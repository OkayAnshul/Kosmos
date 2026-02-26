package com.example.kosmos.data.repository

import android.util.Log
import com.example.kosmos.core.database.dao.TaskActivityDao
import com.example.kosmos.core.models.ActivityActionType
import com.example.kosmos.core.models.SyncEntityType
import com.example.kosmos.core.models.SyncOperation
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.data.datasource.SupabaseTaskActivityDataSource
import com.example.kosmos.data.sync.SyncQueueHelper
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Task Activity Repository (P0-03 FIX)
 *
 * Implements offline-first pattern for task activity tracking:
 * 1. Save activity to Room immediately (works offline)
 * 2. Queue Supabase sync in background (happens when online)
 * 3. Activity history preserved even if sync fails
 *
 * This ensures task changes are ALWAYS logged, regardless of network status.
 */
@Singleton
class TaskActivityRepository @Inject constructor(
    private val taskActivityDao: TaskActivityDao,
    private val supabaseTaskActivityDataSource: SupabaseTaskActivityDataSource,
    private val syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao
) {
    companion object {
        private const val TAG = "TaskActivityRepository"
    }

    /**
     * Track activity (P0-03 FIX: Offline-first pattern)
     *
     * CRITICAL: This method MUST succeed even when offline.
     * - Saves to Room immediately (guaranteed success)
     * - Syncs to Supabase in background (best effort)
     *
     * @param activity TaskActivity to track
     * @return Result indicating success or failure
     */
    suspend fun trackActivity(activity: TaskActivity): Result<Unit> {
        return try {
            // Step 1: Save to Room immediately (offline-first)
            // This MUST succeed - activity history is critical
            taskActivityDao.insertActivity(activity)
            Log.d(TAG, "✅ Activity tracked locally: ${activity.actionType} on task ${activity.taskId}")

            // Step 2: Sync to Supabase in background (best effort)
            // Don't fail if this fails - Room cache is the source of truth
            try {
                val supabaseResult = supabaseTaskActivityDataSource.insertActivity(activity)
                if (supabaseResult.isSuccess) {
                    Log.d(TAG, "✅ Activity synced to Supabase: ${activity.id}")
                } else {
                    Log.w(TAG, "⚠️ Failed to sync activity to Supabase (will retry later)", supabaseResult.exceptionOrNull())
                    // Bug E fix: Queue for retry
                    SyncQueueHelper.queueTaskActivity(syncQueueDao, activity, SyncOperation.CREATE)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "⚠️ Error syncing activity to Supabase (offline mode?)", e)
                // Bug E fix: Queue for retry
                SyncQueueHelper.queueTaskActivity(syncQueueDao, activity, SyncOperation.CREATE)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ CRITICAL: Failed to track activity locally", e)
            Result.failure(e)
        }
    }

    /**
     * Track multiple activities in bulk (e.g., initial sync)
     *
     * @param activities List of activities to track
     * @return Result indicating success or failure
     */
    suspend fun trackActivities(activities: List<TaskActivity>): Result<Unit> {
        return try {
            // Step 1: Save all to Room
            taskActivityDao.insertActivities(activities)
            Log.d(TAG, "✅ ${activities.size} activities tracked locally")

            // Step 2: Sync to Supabase (best effort)
            try {
                supabaseTaskActivityDataSource.insertActivities(activities)
                Log.d(TAG, "✅ ${activities.size} activities synced to Supabase")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "⚠️ Error syncing ${activities.size} activities to Supabase", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to track activities", e)
            Result.failure(e)
        }
    }

    /**
     * Get activity for a specific task (Flow - reactive updates)
     *
     * @param taskId Task ID
     * @return Flow of activity list
     */
    fun getActivityForTaskFlow(taskId: String): Flow<List<TaskActivity>> {
        return taskActivityDao.getActivityForTaskFlow(taskId)
    }

    /**
     * Get recent activity for a task (limited)
     *
     * @param taskId Task ID
     * @param limit Maximum number of activities
     * @return Flow of recent activities
     */
    fun getRecentActivityForTaskFlow(taskId: String, limit: Int = 5): Flow<List<TaskActivity>> {
        return taskActivityDao.getRecentActivityForTaskFlow(taskId, limit)
    }

    /**
     * Get activity for a task (suspend - one-time fetch)
     *
     * @param taskId Task ID
     * @return List of activities
     */
    suspend fun getActivityForTask(taskId: String): List<TaskActivity> {
        return taskActivityDao.getActivityForTask(taskId)
    }

    /**
     * Get activity for a project (Flow - reactive updates)
     *
     * @param projectId Project ID
     * @return Flow of activity list
     */
    fun getActivityForProjectFlow(projectId: String): Flow<List<TaskActivity>> {
        return taskActivityDao.getActivityForProjectFlow(projectId)
    }

    /**
     * Get paginated activity for a project
     *
     * @param projectId Project ID
     * @param limit Page size
     * @param offset Page offset
     * @return List of activities
     */
    suspend fun getActivityForProjectPaginated(
        projectId: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<TaskActivity> {
        return taskActivityDao.getActivityForProjectPaginated(projectId, limit, offset)
    }

    /**
     * Get activity before a specific timestamp (for infinite scroll pagination)
     *
     * @param projectId Project ID
     * @param beforeTimestamp Timestamp threshold
     * @param limit Maximum number of activities
     * @return List of activities
     */
    suspend fun getActivityBeforeTimestamp(
        projectId: String,
        beforeTimestamp: Long,
        limit: Int = 100
    ): List<TaskActivity> {
        return taskActivityDao.getActivityBeforeTimestamp(projectId, beforeTimestamp, limit)
    }

    /**
     * Get activity by a specific user (Flow)
     *
     * @param userId User ID
     * @return Flow of activity list
     */
    fun getActivityForUserFlow(userId: String): Flow<List<TaskActivity>> {
        return taskActivityDao.getActivityForUserFlow(userId)
    }

    /**
     * Get recent activity by user in a project
     *
     * @param userId User ID
     * @param projectId Project ID
     * @param limit Maximum number of activities
     * @return List of activities
     */
    suspend fun getRecentActivityForUserInProject(
        userId: String,
        projectId: String,
        limit: Int = 50
    ): List<TaskActivity> {
        return taskActivityDao.getRecentActivityForUserInProject(userId, projectId, limit)
    }

    /**
     * Get activity by action type (Flow)
     *
     * @param projectId Project ID
     * @param actionType Action type filter
     * @return Flow of filtered activities
     */
    fun getActivityByActionTypeFlow(
        projectId: String,
        actionType: ActivityActionType
    ): Flow<List<TaskActivity>> {
        return taskActivityDao.getActivityByActionTypeFlow(projectId, actionType)
    }

    /**
     * Search activities by commit message (Flow)
     *
     * @param projectId Project ID
     * @param searchQuery Search query
     * @return Flow of matching activities
     */
    fun searchActivityByCommitMessage(
        projectId: String,
        searchQuery: String
    ): Flow<List<TaskActivity>> {
        return taskActivityDao.searchActivityByCommitMessage(projectId, searchQuery)
    }

    /**
     * Delete activity by ID (admin only)
     *
     * @param activityId Activity ID
     * @return Result indicating success or failure
     */
    suspend fun deleteActivity(activityId: String): Result<Unit> {
        return try {
            taskActivityDao.deleteActivityById(activityId)

            // Try to delete from Supabase (best effort)
            try {
                supabaseTaskActivityDataSource.deleteActivity(activityId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "⚠️ Failed to delete activity from Supabase", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to delete activity", e)
            Result.failure(e)
        }
    }

    /**
     * Delete all activity for a task
     *
     * @param taskId Task ID
     * @return Result indicating success or failure
     */
    suspend fun deleteActivityForTask(taskId: String): Result<Unit> {
        return try {
            taskActivityDao.deleteActivityForTask(taskId)

            // Try to delete from Supabase (best effort)
            try {
                supabaseTaskActivityDataSource.deleteActivitiesForTask(taskId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "⚠️ Failed to delete task activities from Supabase", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to delete task activities", e)
            Result.failure(e)
        }
    }

    /**
     * Delete all activity for a project
     *
     * @param projectId Project ID
     * @return Result indicating success or failure
     */
    suspend fun deleteActivityForProject(projectId: String): Result<Unit> {
        return try {
            taskActivityDao.deleteActivityForProject(projectId)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to delete project activities", e)
            Result.failure(e)
        }
    }

    /**
     * Get activity count for a task
     *
     * @param taskId Task ID
     * @return Activity count
     */
    suspend fun getActivityCountForTask(taskId: String): Int {
        return taskActivityDao.getActivityCountForTask(taskId)
    }

    /**
     * Get activity count for a project
     *
     * @param projectId Project ID
     * @return Activity count
     */
    suspend fun getActivityCountForProject(projectId: String): Int {
        return taskActivityDao.getActivityCountForProject(projectId)
    }

    /**
     * Get activity count for a project (Flow - reactive)
     *
     * @param projectId Project ID
     * @return Flow of activity count
     */
    fun getActivityCountForProjectFlow(projectId: String): Flow<Int> {
        return taskActivityDao.getActivityCountForProjectFlow(projectId)
    }

    /**
     * Get activity count by user in a project
     *
     * @param userId User ID
     * @param projectId Project ID
     * @return Activity count
     */
    suspend fun getActivityCountByUserInProject(userId: String, projectId: String): Int {
        return taskActivityDao.getActivityCountByUserInProject(userId, projectId)
    }

    /**
     * Sync pending activities to Supabase
     * Queries the sync queue for TASK_ACTIVITY items and retries each one.
     * Called by SyncManager when network becomes available.
     */
    suspend fun syncPendingActivities(): Result<Unit> {
        return try {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val pendingItems = syncQueueDao.getAllPendingItems()
                .filter { it.entityType == SyncEntityType.TASK_ACTIVITY }

            if (pendingItems.isEmpty()) {
                Log.d(TAG, "No pending activities to sync")
                return Result.success(Unit)
            }

            Log.d(TAG, "Syncing ${pendingItems.size} pending activities")
            var successCount = 0
            var failureCount = 0

            pendingItems.forEach { item ->
                try {
                    val activity = json.decodeFromString<TaskActivity>(item.entityJson)
                    val result = supabaseTaskActivityDataSource.insertActivity(activity)
                    if (result.isSuccess) {
                        syncQueueDao.deleteById(item.id)
                        successCount++
                        Log.d(TAG, "✅ Synced pending activity: ${activity.id}")
                    } else {
                        val updated = item.copy(retryCount = item.retryCount + 1)
                        syncQueueDao.update(updated)
                        failureCount++
                        Log.w(TAG, "⚠️ Failed to sync activity ${activity.id}, retry ${updated.retryCount}/${item.maxRetries}")
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    val updated = item.copy(retryCount = item.retryCount + 1)
                    syncQueueDao.update(updated)
                    failureCount++
                    Log.w(TAG, "⚠️ Error syncing activity ${item.entityId}", e)
                }
            }

            Log.d(TAG, "✅ Sync complete: $successCount synced, $failureCount failed")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Error syncing pending activities", e)
            Result.failure(e)
        }
    }
}
