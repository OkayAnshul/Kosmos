package com.example.kosmos.data.repository

import android.util.Log
import com.example.kosmos.core.database.dao.ProjectJoinRequestDao
import com.example.kosmos.core.database.dao.ProjectMemberDao
import com.example.kosmos.core.database.dao.SyncQueueDao
import com.example.kosmos.core.models.JoinRequestStatus
import com.example.kosmos.core.models.ProjectJoinRequest
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.SyncOperation
import com.example.kosmos.data.datasource.SupabaseProjectJoinRequestDataSource
import com.example.kosmos.data.sync.SyncQueueHelper
import com.example.kosmos.features.notifications.SupabaseNotificationService
import com.example.kosmos.shared.utils.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class ProjectJoinRequestRepository @Inject constructor(
    private val joinRequestDao: ProjectJoinRequestDao,
    private val projectMemberDao: ProjectMemberDao,
    private val supabaseDataSource: SupabaseProjectJoinRequestDataSource,
    private val projectRepository: ProjectRepository,
    private val notificationService: SupabaseNotificationService,
    private val networkMonitor: NetworkMonitor,
    private val syncQueueDao: SyncQueueDao
) {
    companion object {
        private const val TAG = "JoinRequestRepo"
    }

    fun getRequestsForProjectFlow(projectId: String): Flow<List<ProjectJoinRequest>> =
        joinRequestDao.getByProjectFlow(projectId)

    fun getMyRequestsFlow(userId: String): Flow<List<ProjectJoinRequest>> =
        joinRequestDao.getByRequesterFlow(userId)

    suspend fun getMyRequestForProject(userId: String, projectId: String): ProjectJoinRequest? =
        joinRequestDao.getExisting(projectId, userId)

    suspend fun requestToJoin(
        projectId: String,
        requesterId: String,
        message: String? = null,
        projectName: String = "",
        requesterName: String = ""
    ): Result<ProjectJoinRequest> {
        return try {
            // Check if already a member
            val existingMember = projectMemberDao.getMemberByProjectAndUser(projectId, requesterId)
            if (existingMember != null) {
                return Result.failure(IllegalStateException("Already a member"))
            }

            // Check for existing request
            val existingRequest = joinRequestDao.getExisting(projectId, requesterId)
            if (existingRequest != null && existingRequest.status == JoinRequestStatus.PENDING) {
                return Result.failure(IllegalStateException("Request already pending"))
            }

            val request = ProjectJoinRequest(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                requesterId = requesterId,
                message = message
            )

            joinRequestDao.insert(request)

            if (!networkMonitor.isOffline.value) {
                val result = supabaseDataSource.createRequest(request)
                if (result.isFailure) {
                    SyncQueueHelper.queueJoinRequest(syncQueueDao, request, SyncOperation.CREATE)
                }
            } else {
                SyncQueueHelper.queueJoinRequest(syncQueueDao, request, SyncOperation.CREATE)
            }

            // Notify project admins/managers
            try {
                val adminMembers = projectMemberDao.getMembersByRoleSync(projectId, "ADMIN") +
                    projectMemberDao.getMembersByRoleSync(projectId, "MANAGER")
                val adminIds = adminMembers.map { it.userId }

                notificationService.sendNotificationToMultiple(
                    userIds = adminIds,
                    title = "Join Request",
                    body = "$requesterName wants to join $projectName",
                    type = "join_request",
                    data = mapOf(
                        "request_id" to request.id,
                        "project_id" to projectId,
                        "project_name" to projectName,
                        "requester_id" to requesterId,
                        "requester_name" to requesterName,
                        "message" to (message ?: "")
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Failed to send join request notification", e)
            }

            Result.success(request)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error requesting to join", e)
            Result.failure(e)
        }
    }

    suspend fun approveRequest(requestId: String, reviewerId: String): Result<Unit> {
        return try {
            val request = joinRequestDao.getById(requestId)
                ?: return Result.failure(IllegalArgumentException("Request not found"))

            if (request.status != JoinRequestStatus.PENDING) {
                return Result.failure(IllegalStateException("Request is no longer pending"))
            }

            // Check if already a member (from invite acceptance perhaps)
            val existingMember = projectMemberDao.getMemberByProjectAndUser(request.projectId, request.requesterId)
            if (existingMember != null) {
                // Already a member — just update request status
                val now = System.currentTimeMillis()
                joinRequestDao.updateStatus(requestId, JoinRequestStatus.APPROVED.name, reviewerId, now)
                // Bug K fix: try-catch instead of isOffline check
                try {
                    supabaseDataSource.updateStatus(requestId, JoinRequestStatus.APPROVED, reviewerId, now)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Failed to sync quick-approve status, queuing", e)
                    val updated = request.copy(status = JoinRequestStatus.APPROVED, reviewedBy = reviewerId, respondedAt = now)
                    SyncQueueHelper.queueJoinRequest(syncQueueDao, updated, SyncOperation.UPDATE)
                }
                return Result.success(Unit)
            }

            val now = System.currentTimeMillis()
            joinRequestDao.updateStatus(requestId, JoinRequestStatus.APPROVED.name, reviewerId, now)

            // Bug K fix: try-catch instead of isOffline check
            val updatedRequest = request.copy(status = JoinRequestStatus.APPROVED, reviewedBy = reviewerId, respondedAt = now)
            try {
                val result = supabaseDataSource.updateStatus(requestId, JoinRequestStatus.APPROVED, reviewerId, now)
                if (result.isFailure) {
                    Log.w(TAG, "Failed to sync approve to Supabase, queuing", result.exceptionOrNull())
                    SyncQueueHelper.queueJoinRequest(syncQueueDao, updatedRequest, SyncOperation.UPDATE)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Exception syncing approve to Supabase, queuing", e)
                SyncQueueHelper.queueJoinRequest(syncQueueDao, updatedRequest, SyncOperation.UPDATE)
            }

            // Create member
            projectRepository.addMember(
                projectId = request.projectId,
                userId = request.requesterId,
                role = ProjectRole.MEMBER,
                invitedBy = reviewerId,
                bypassApproval = true
            )

            // Notify requester
            try {
                notificationService.sendNotification(
                    userId = request.requesterId,
                    title = "Request Approved",
                    body = "Your request to join was approved",
                    type = "join_approved",
                    data = mapOf(
                        "project_id" to request.projectId
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Failed to send approval notification", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error approving request", e)
            Result.failure(e)
        }
    }

    suspend fun rejectRequest(requestId: String, reviewerId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            joinRequestDao.updateStatus(requestId, JoinRequestStatus.REJECTED.name, reviewerId, now)

            // Bug K fix: try-catch instead of isOffline check
            try {
                supabaseDataSource.updateStatus(requestId, JoinRequestStatus.REJECTED, reviewerId, now)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Failed to sync rejection to Supabase (no queue needed for rejection)", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error rejecting request", e)
            Result.failure(e)
        }
    }

    suspend fun cancelRequest(requestId: String): Result<Unit> {
        return try {
            joinRequestDao.deleteById(requestId)

            // Bug K fix: try-catch instead of isOffline check
            try {
                supabaseDataSource.cancelRequest(requestId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Failed to sync request cancellation to Supabase (ephemeral — not queued)", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error cancelling request", e)
            Result.failure(e)
        }
    }

    suspend fun syncFromSupabase(projectId: String) {
        try {
            val result = supabaseDataSource.getByProject(projectId)
            if (result.isSuccess) {
                joinRequestDao.insertAll(result.getOrThrow())
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error syncing join requests from Supabase", e)
        }
    }

    suspend fun syncForUser(userId: String) {
        try {
            val result = supabaseDataSource.getByRequester(userId)
            if (result.isSuccess) {
                joinRequestDao.insertAll(result.getOrThrow())
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error syncing user's join requests from Supabase", e)
        }
    }
}
