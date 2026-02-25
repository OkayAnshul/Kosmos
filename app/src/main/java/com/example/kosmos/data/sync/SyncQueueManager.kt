package com.example.kosmos.data.sync

import android.util.Log
import com.example.kosmos.core.database.dao.SyncQueueDao
import com.example.kosmos.core.database.dao.TaskDao
import com.example.kosmos.core.models.*
import com.example.kosmos.data.datasource.*
import com.example.kosmos.shared.utils.NetworkMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Sync Queue Manager (P0-08 FIX)
 *
 * Orchestrates automatic retry of failed Supabase operations.
 *
 * Pattern:
 * 1. Repositories queue failed operations to sync_queue table
 * 2. NetworkMonitor detects network return (isOffline: false)
 * 3. SyncQueueManager queries pending items
 * 4. Retries each item with exponential backoff
 * 5. On success: removes from queue
 * 6. On failure: increments retry count, waits for next attempt
 *
 * @property syncQueueDao DAO for sync queue persistence
 * @property networkMonitor Network connectivity monitor
 * @property scope Coroutine scope for background operations
 */
@Singleton
class SyncQueueManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val taskDao: TaskDao,
    private val networkMonitor: NetworkMonitor,
    private val supabaseTaskDataSource: SupabaseTaskDataSource,
    private val supabaseTaskActivityDataSource: SupabaseTaskActivityDataSource,
    private val supabaseProjectDataSource: SupabaseProjectDataSource,
    private val supabaseProjectMemberDataSource: SupabaseProjectMemberDataSource,
    private val supabaseMessageDataSource: SupabaseMessageDataSource,
    private val supabaseChatDataSource: SupabaseChatDataSource,
    private val supabaseUserDataSource: SupabaseUserDataSource,
    private val supabaseProjectInviteDataSource: SupabaseProjectInviteDataSource,
    private val supabaseUserConnectionDataSource: SupabaseUserConnectionDataSource,
    private val supabaseProjectJoinRequestDataSource: SupabaseProjectJoinRequestDataSource,
    private val supabaseTimeEntryDataSource: com.example.kosmos.data.datasource.SupabaseTimeEntryDataSource,
    private val supabaseDependencyDataSource: com.example.kosmos.data.datasource.SupabaseDependencyDataSource,
    @ApplicationScope private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "SyncQueueManager"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // Track if we're currently syncing to avoid duplicate work
    private var isSyncing = false

    init {
        // Start observing network state changes
        observeNetworkState()
    }

    /**
     * Observe network state and trigger sync when online
     */
    private fun observeNetworkState() {
        scope.launch {
            networkMonitor.isOffline.collectLatest { offline ->
                if (!offline && !isSyncing) {
                    Log.d(TAG, "📡 Network available - starting sync queue processing")
                    processSyncQueue()
                }
            }
        }
    }

    /**
     * Process all pending items in sync queue
     * Called automatically when network returns
     */
    suspend fun processSyncQueue() {
        if (isSyncing) {
            Log.d(TAG, "⏭️ Already syncing - skipping duplicate sync")
            return
        }

        isSyncing = true
        try {
            val pendingItems = syncQueueDao.getItemsReadyToRetry()

            if (pendingItems.isEmpty()) {
                Log.d(TAG, "✅ Sync queue empty - nothing to process")
                return
            }

            Log.d(TAG, "🔄 Processing ${pendingItems.size} pending sync items")

            pendingItems.forEach { item ->
                if (item.shouldRetryNow()) {
                    retryItem(item)
                    // Small delay between items to avoid overwhelming server
                    delay(100)
                }
            }

            Log.d(TAG, "✅ Sync queue processing complete")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing sync queue", e)
        } finally {
            isSyncing = false
        }
    }

    /**
     * Retry a single sync queue item
     * Deserializes entity, calls appropriate data source, handles result
     */
    private suspend fun retryItem(item: SyncQueueItem) {
        Log.d(TAG, "🔄 Retrying ${item.operation} ${item.entityType} (attempt ${item.retryCount + 1}/${item.maxRetries})")

        try {
            val result = when (item.entityType) {
                SyncEntityType.TASK -> retryTaskOperation(item)
                SyncEntityType.TASK_ACTIVITY -> retryTaskActivityOperation(item)
                SyncEntityType.PROJECT -> retryProjectOperation(item)
                SyncEntityType.PROJECT_MEMBER -> retryProjectMemberOperation(item)
                SyncEntityType.MESSAGE -> retryMessageOperation(item)
                SyncEntityType.CHAT_ROOM -> retryChatRoomOperation(item)
                SyncEntityType.USER -> retryUserOperation(item)
                SyncEntityType.VOICE_MESSAGE -> Result.failure(Exception("Voice message sync not implemented"))
                SyncEntityType.ACTION_ITEM -> Result.failure(Exception("Action item sync not implemented"))
                SyncEntityType.PROJECT_INVITE -> retryProjectInviteOperation(item)
                SyncEntityType.USER_CONNECTION -> retryUserConnectionOperation(item)
                SyncEntityType.JOIN_REQUEST -> retryJoinRequestOperation(item)
                SyncEntityType.TIME_ENTRY -> retryTimeEntryOperation(item)
                SyncEntityType.TASK_DEPENDENCY -> retryTaskDependencyOperation(item)
            }

            if (result.isSuccess) {
                // Success - remove from queue
                syncQueueDao.deleteById(item.id)
                Log.d(TAG, "✅ Sync successful - removed ${item.entityType} from queue")
            } else {
                // Failure - increment retry count
                val error = result.exceptionOrNull()
                val updatedItem = item.copy(
                    retryCount = item.retryCount + 1,
                    lastAttemptTimestamp = System.currentTimeMillis(),
                    lastErrorMessage = error?.message,
                    lastErrorCode = if (error is io.github.jan.supabase.postgrest.exception.PostgrestRestException) error.code else null
                )

                syncQueueDao.update(updatedItem)

                if (updatedItem.hasExceededMaxRetries()) {
                    Log.e(TAG, "❌ Max retries exceeded for ${item.entityType} - giving up")
                } else {
                    val nextDelay = updatedItem.getNextRetryDelayMs()
                    Log.w(TAG, "⚠️ Sync failed - will retry in ${nextDelay}ms (attempt ${updatedItem.retryCount}/${item.maxRetries})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrying ${item.entityType}", e)

            // Update retry count even on exception
            val updatedItem = item.copy(
                retryCount = item.retryCount + 1,
                lastAttemptTimestamp = System.currentTimeMillis(),
                lastErrorMessage = e.message
            )
            syncQueueDao.update(updatedItem)
        }
    }

    /**
     * Retry task operation (CREATE/UPDATE/DELETE)
     */
    private suspend fun retryTaskOperation(item: SyncQueueItem): Result<Unit> {
        return try {
            when (item.operation) {
                SyncOperation.CREATE -> {
                    val task = json.decodeFromString<Task>(item.entityJson)
                    supabaseTaskDataSource.insertTask(task).map { }
                }
                SyncOperation.UPDATE -> {
                    // Always retry with latest local state, not stale queued JSON
                    val freshTask = taskDao.getTaskById(item.entityId)
                        ?: return Result.failure(Exception("Task ${item.entityId} no longer exists locally — removing from queue"))
                    supabaseTaskDataSource.updateTask(freshTask).map { }
                }
                SyncOperation.DELETE -> {
                    supabaseTaskDataSource.deleteTask(item.entityId).map { }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retry task activity operation
     */
    private suspend fun retryTaskActivityOperation(item: SyncQueueItem): Result<Unit> {
        return try {
            when (item.operation) {
                SyncOperation.CREATE, SyncOperation.UPDATE -> {
                    val activity = json.decodeFromString<TaskActivity>(item.entityJson)
                    supabaseTaskActivityDataSource.insertActivity(activity).map { }
                }
                SyncOperation.DELETE -> {
                    supabaseTaskActivityDataSource.deleteActivity(item.entityId).map { }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retry project operation
     */
    private suspend fun retryProjectOperation(item: SyncQueueItem): Result<Unit> {
        return try {
            when (item.operation) {
                SyncOperation.CREATE, SyncOperation.UPDATE -> {
                    val project = json.decodeFromString<Project>(item.entityJson)
                    supabaseProjectDataSource.insert(project).map { }
                }
                SyncOperation.DELETE -> {
                    supabaseProjectDataSource.delete(item.entityId).map { }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retry project member operation
     */
    private suspend fun retryProjectMemberOperation(item: SyncQueueItem): Result<Unit> {
        return try {
            when (item.operation) {
                SyncOperation.CREATE, SyncOperation.UPDATE -> {
                    val member = json.decodeFromString<ProjectMember>(item.entityJson)
                    supabaseProjectMemberDataSource.insert(member).map { }
                }
                SyncOperation.DELETE -> {
                    // For DELETE, decode the member to get projectId and userId
                    val member = json.decodeFromString<ProjectMember>(item.entityJson)
                    supabaseProjectMemberDataSource.removeMember(member.projectId, member.userId).map { }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retry message operation
     */
    private suspend fun retryMessageOperation(item: SyncQueueItem): Result<Unit> {
        return try {
            when (item.operation) {
                SyncOperation.CREATE, SyncOperation.UPDATE -> {
                    val message = json.decodeFromString<Message>(item.entityJson)
                    supabaseMessageDataSource.insertMessage(message).map { }
                }
                SyncOperation.DELETE -> {
                    supabaseMessageDataSource.deleteMessage(item.entityId).map { }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retry chat room operation
     */
    private suspend fun retryChatRoomOperation(item: SyncQueueItem): Result<Unit> {
        return try {
            when (item.operation) {
                SyncOperation.CREATE, SyncOperation.UPDATE -> {
                    val chatRoom = json.decodeFromString<ChatRoom>(item.entityJson)
                    supabaseChatDataSource.insertChatRoom(chatRoom).map { }
                }
                SyncOperation.DELETE -> {
                    supabaseChatDataSource.deleteChatRoom(item.entityId).map { }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retry user operation
     */
    private suspend fun retryUserOperation(item: SyncQueueItem): Result<Unit> {
        return try {
            when (item.operation) {
                SyncOperation.CREATE -> {
                    val user = json.decodeFromString<User>(item.entityJson)
                    supabaseUserDataSource.insert(user).map { }
                }
                SyncOperation.UPDATE -> {
                    val user = json.decodeFromString<User>(item.entityJson)
                    supabaseUserDataSource.update(user).map { }
                }
                SyncOperation.DELETE -> {
                    supabaseUserDataSource.delete(item.entityId).map { }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun retryProjectInviteOperation(item: SyncQueueItem): Result<Unit> {
        return try {
            when (item.operation) {
                SyncOperation.CREATE -> {
                    val invite = json.decodeFromString<ProjectInvite>(item.entityJson)
                    supabaseProjectInviteDataSource.createInvite(invite).map { }
                }
                SyncOperation.UPDATE -> {
                    val invite = json.decodeFromString<ProjectInvite>(item.entityJson)
                    supabaseProjectInviteDataSource.updateStatus(invite.id, invite.status, invite.respondedAt ?: System.currentTimeMillis()).map { }
                }
                SyncOperation.DELETE -> {
                    supabaseProjectInviteDataSource.cancelInvite(item.entityId).map { }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun retryUserConnectionOperation(item: SyncQueueItem): Result<Unit> {
        return try {
            when (item.operation) {
                SyncOperation.CREATE -> {
                    val connection = json.decodeFromString<UserConnection>(item.entityJson)
                    supabaseUserConnectionDataSource.createConnection(connection).map { }
                }
                SyncOperation.UPDATE -> {
                    val connection = json.decodeFromString<UserConnection>(item.entityJson)
                    supabaseUserConnectionDataSource.updateStatus(connection.id, connection.status, connection.respondedAt ?: System.currentTimeMillis()).map { }
                }
                SyncOperation.DELETE -> {
                    supabaseUserConnectionDataSource.removeConnection(item.entityId).map { }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun retryJoinRequestOperation(item: SyncQueueItem): Result<Unit> {
        return try {
            when (item.operation) {
                SyncOperation.CREATE -> {
                    val request = json.decodeFromString<ProjectJoinRequest>(item.entityJson)
                    supabaseProjectJoinRequestDataSource.createRequest(request).map { }
                }
                SyncOperation.UPDATE -> {
                    val request = json.decodeFromString<ProjectJoinRequest>(item.entityJson)
                    supabaseProjectJoinRequestDataSource.updateStatus(request.id, request.status, request.reviewedBy, request.respondedAt ?: System.currentTimeMillis()).map { }
                }
                SyncOperation.DELETE -> {
                    supabaseProjectJoinRequestDataSource.cancelRequest(item.entityId).map { }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Manually trigger sync queue processing
     * Useful for testing or forced sync
     */
    suspend fun forceSyncNow() {
        Log.d(TAG, "🔄 Manual sync triggered")
        processSyncQueue()
    }

    /**
     * Get pending sync queue count
     */
    suspend fun getPendingCount(): Int {
        return syncQueueDao.getPendingCount()
    }

    private suspend fun retryTaskDependencyOperation(item: SyncQueueItem): Result<Unit> {
        return try {
            val dependency = json.decodeFromString<TaskDependency>(item.entityJson)
            when (item.operation) {
                SyncOperation.CREATE, SyncOperation.UPDATE -> supabaseDependencyDataSource.insertDependency(dependency).map { }
                SyncOperation.DELETE -> supabaseDependencyDataSource.deleteDependency(dependency.id).map { }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear all failed items (exceeded max retries)
     * Call this periodically to clean up permanently failed items
     */
    private suspend fun retryTimeEntryOperation(item: SyncQueueItem): Result<Unit> {
        return try {
            val timeEntry = json.decodeFromString<com.example.kosmos.core.models.TimeEntry>(item.entityJson)
            when (item.operation) {
                SyncOperation.CREATE -> supabaseTimeEntryDataSource.insertTimeEntry(timeEntry).map { }
                SyncOperation.UPDATE -> supabaseTimeEntryDataSource.updateTimeEntry(timeEntry).map { }
                SyncOperation.DELETE -> supabaseTimeEntryDataSource.deleteTimeEntry(timeEntry.id).map { }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cleanupFailedItems() {
        try {
            val failedItems = syncQueueDao.getFailedItems()
            if (failedItems.isNotEmpty()) {
                Log.w(TAG, "🧹 Cleaning up ${failedItems.size} permanently failed items")
                syncQueueDao.deleteAllFailed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up failed items", e)
        }
    }
}

/**
 * Coroutine scope qualifier for SyncQueueManager
 * Ensures background operations survive configuration changes
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope
