package com.example.kosmos.data.repository

import android.util.Log
import com.example.kosmos.core.database.dao.SyncQueueDao
import com.example.kosmos.core.database.dao.UserConnectionDao
import com.example.kosmos.core.models.ConnectionStatus
import com.example.kosmos.core.models.SyncOperation
import com.example.kosmos.core.models.UserConnection
import com.example.kosmos.data.datasource.SupabaseUserConnectionDataSource
import com.example.kosmos.data.sync.SyncQueueHelper
import com.example.kosmos.features.notifications.SupabaseNotificationService
import com.example.kosmos.shared.utils.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class UserConnectionRepository @Inject constructor(
    private val connectionDao: UserConnectionDao,
    private val supabaseDataSource: SupabaseUserConnectionDataSource,
    private val notificationService: SupabaseNotificationService,
    private val networkMonitor: NetworkMonitor,
    private val syncQueueDao: SyncQueueDao
) {
    companion object {
        private const val TAG = "UserConnectionRepo"
    }

    fun getAcceptedConnectionsFlow(userId: String): Flow<List<UserConnection>> =
        connectionDao.getAcceptedFlow(userId)

    fun getPendingRequestsFlow(userId: String): Flow<List<UserConnection>> =
        connectionDao.getPendingRequestsFlow(userId)

    fun getForUserFlow(userId: String): Flow<List<UserConnection>> =
        connectionDao.getForUserFlow(userId)

    suspend fun getConnectionStatus(currentUserId: String, otherUserId: String): ConnectionStatus? {
        val connection = connectionDao.getBetween(currentUserId, otherUserId)
        return connection?.status
    }

    suspend fun getConnectionBetween(userA: String, userB: String): UserConnection? =
        connectionDao.getBetween(userA, userB)

    suspend fun getAcceptedIds(userId: String): List<String> =
        connectionDao.getAcceptedIds(userId)

    suspend fun sendRequest(
        requesterId: String,
        addresseeId: String,
        requesterName: String = ""
    ): Result<UserConnection> {
        return try {
            if (requesterId == addresseeId) {
                return Result.failure(IllegalArgumentException("Cannot connect with yourself"))
            }

            // Check for existing connection
            val existing = connectionDao.getBetween(requesterId, addresseeId)
            if (existing != null) {
                return when (existing.status) {
                    ConnectionStatus.ACCEPTED -> Result.failure(IllegalStateException("Already connected"))
                    ConnectionStatus.PENDING -> Result.failure(IllegalStateException("Request already pending"))
                    ConnectionStatus.BLOCKED -> Result.failure(IllegalStateException("User is blocked"))
                    ConnectionStatus.DECLINED -> {
                        // Allow re-request after decline
                        connectionDao.deleteById(existing.id)
                        if (!networkMonitor.isOffline.value) {
                            supabaseDataSource.removeConnection(existing.id)
                        }
                        // Fall through to create new request
                        createNewRequest(requesterId, addresseeId, requesterName)
                    }
                }
            }

            createNewRequest(requesterId, addresseeId, requesterName)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error sending connection request", e)
            Result.failure(e)
        }
    }

    private suspend fun createNewRequest(
        requesterId: String,
        addresseeId: String,
        requesterName: String
    ): Result<UserConnection> {
        val connection = UserConnection(
            id = UUID.randomUUID().toString(),
            requesterId = requesterId,
            addresseeId = addresseeId
        )

        connectionDao.insert(connection)

        if (!networkMonitor.isOffline.value) {
            val result = supabaseDataSource.createConnection(connection)
            if (result.isFailure) {
                SyncQueueHelper.queueUserConnection(syncQueueDao, connection, SyncOperation.CREATE)
            }
        } else {
            SyncQueueHelper.queueUserConnection(syncQueueDao, connection, SyncOperation.CREATE)
        }

        try {
            notificationService.sendNotification(
                userId = addresseeId,
                title = "Connection Request",
                body = "$requesterName wants to connect with you",
                type = "connection_request",
                data = mapOf(
                    "connection_id" to connection.id,
                    "requester_id" to requesterId,
                    "requester_name" to requesterName
                )
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Failed to send connection notification", e)
        }

        return Result.success(connection)
    }

    suspend fun acceptConnection(connectionId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            connectionDao.updateStatus(connectionId, ConnectionStatus.ACCEPTED.name, now)

            // Bug K fix: try-catch instead of isOffline check
            try {
                val result = supabaseDataSource.updateStatus(connectionId, ConnectionStatus.ACCEPTED, now)
                if (result.isFailure) {
                    val conn = connectionDao.getById(connectionId)
                    conn?.let { SyncQueueHelper.queueUserConnection(syncQueueDao, it.copy(status = ConnectionStatus.ACCEPTED, respondedAt = now), SyncOperation.UPDATE) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Failed to sync acceptConnection to Supabase, queuing", e)
                val conn = connectionDao.getById(connectionId)
                conn?.let { SyncQueueHelper.queueUserConnection(syncQueueDao, it.copy(status = ConnectionStatus.ACCEPTED, respondedAt = now), SyncOperation.UPDATE) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error accepting connection", e)
            Result.failure(e)
        }
    }

    suspend fun declineConnection(connectionId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            connectionDao.updateStatus(connectionId, ConnectionStatus.DECLINED.name, now)

            // Bug K fix: try-catch instead of isOffline check
            try {
                val result = supabaseDataSource.updateStatus(connectionId, ConnectionStatus.DECLINED, now)
                if (result.isFailure) {
                    val conn = connectionDao.getById(connectionId)
                    conn?.let { SyncQueueHelper.queueUserConnection(syncQueueDao, it.copy(status = ConnectionStatus.DECLINED, respondedAt = now), SyncOperation.UPDATE) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Failed to sync declineConnection to Supabase, queuing", e)
                val conn = connectionDao.getById(connectionId)
                conn?.let { SyncQueueHelper.queueUserConnection(syncQueueDao, it.copy(status = ConnectionStatus.DECLINED, respondedAt = now), SyncOperation.UPDATE) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error declining connection", e)
            Result.failure(e)
        }
    }

    suspend fun removeConnection(connectionId: String): Result<Unit> {
        return try {
            connectionDao.deleteById(connectionId)

            // Bug K fix: try-catch instead of isOffline check (no queue — entity deleted locally)
            try {
                supabaseDataSource.removeConnection(connectionId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Failed to sync removeConnection to Supabase (ephemeral — not queued)", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error removing connection", e)
            Result.failure(e)
        }
    }

    suspend fun blockUser(currentUserId: String, blockedUserId: String): Result<Unit> {
        return try {
            val existing = connectionDao.getBetween(currentUserId, blockedUserId)
            if (existing != null) {
                val now = System.currentTimeMillis()
                connectionDao.updateStatus(existing.id, ConnectionStatus.BLOCKED.name, now)
                // Bug K fix: try-catch instead of isOffline check
                try {
                    supabaseDataSource.updateStatus(existing.id, ConnectionStatus.BLOCKED, now)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Failed to sync blockUser (update) to Supabase, queuing", e)
                    SyncQueueHelper.queueUserConnection(syncQueueDao, existing.copy(status = ConnectionStatus.BLOCKED, respondedAt = now), SyncOperation.UPDATE)
                }
            } else {
                // Create a BLOCKED connection entry
                val connection = UserConnection(
                    id = UUID.randomUUID().toString(),
                    requesterId = currentUserId,
                    addresseeId = blockedUserId,
                    status = ConnectionStatus.BLOCKED
                )
                connectionDao.insert(connection)
                // Bug K fix: try-catch instead of isOffline check
                try {
                    supabaseDataSource.createConnection(connection)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Failed to sync blockUser (create) to Supabase, queuing", e)
                    SyncQueueHelper.queueUserConnection(syncQueueDao, connection, SyncOperation.CREATE)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error blocking user", e)
            Result.failure(e)
        }
    }

    suspend fun syncFromSupabase(userId: String) {
        try {
            val result = supabaseDataSource.getForUser(userId)
            if (result.isSuccess) {
                connectionDao.insertAll(result.getOrThrow())
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error syncing connections from Supabase", e)
        }
    }
}
