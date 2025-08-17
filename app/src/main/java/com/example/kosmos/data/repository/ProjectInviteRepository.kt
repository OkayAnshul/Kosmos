package com.example.kosmos.data.repository

import android.util.Log
import com.example.kosmos.core.database.dao.ProjectInviteDao
import com.example.kosmos.core.database.dao.ProjectMemberDao
import com.example.kosmos.core.database.dao.SyncQueueDao
import com.example.kosmos.core.models.InviteStatus
import com.example.kosmos.core.models.ProjectInvite
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.SyncOperation
import com.example.kosmos.data.datasource.SupabaseProjectInviteDataSource
import com.example.kosmos.data.sync.SyncQueueHelper
import com.example.kosmos.features.notifications.SupabaseNotificationService
import com.example.kosmos.shared.utils.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class ProjectInviteRepository @Inject constructor(
    private val inviteDao: ProjectInviteDao,
    private val projectMemberDao: ProjectMemberDao,
    private val supabaseDataSource: SupabaseProjectInviteDataSource,
    private val projectRepository: ProjectRepository,
    private val notificationService: SupabaseNotificationService,
    private val networkMonitor: NetworkMonitor,
    private val syncQueueDao: SyncQueueDao
) {
    companion object {
        private const val TAG = "ProjectInviteRepo"
    }

    fun getProjectInvitesFlow(projectId: String): Flow<List<ProjectInvite>> =
        inviteDao.getByProjectFlow(projectId)

    fun getPendingForUserFlow(userId: String): Flow<List<ProjectInvite>> =
        inviteDao.getPendingForUserFlow(userId)

    suspend fun sendInvite(
        projectId: String,
        inviteeId: String,
        inviterId: String,
        role: String = "MEMBER",
        message: String? = null,
        projectName: String = "",
        inviterName: String = ""
    ): Result<ProjectInvite> {
        return try {
            // Check if already a member
            val existingMember = projectMemberDao.getMemberByProjectAndUser(projectId, inviteeId)
            if (existingMember != null) {
                return Result.failure(IllegalStateException("User is already a member"))
            }

            // Check for existing pending invite
            val existingInvite = inviteDao.getPendingInvite(projectId, inviteeId)
            if (existingInvite != null) {
                return Result.failure(IllegalStateException("Invite already pending"))
            }

            val invite = ProjectInvite(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                inviteeId = inviteeId,
                inviterId = inviterId,
                role = role,
                message = message
            )

            // Save locally first (offline-first)
            inviteDao.insert(invite)

            // Sync to Supabase
            if (!networkMonitor.isOffline.value) {
                val result = supabaseDataSource.createInvite(invite)
                if (result.isFailure) {
                    SyncQueueHelper.queueProjectInvite(syncQueueDao, invite, SyncOperation.CREATE)
                }
            } else {
                SyncQueueHelper.queueProjectInvite(syncQueueDao, invite, SyncOperation.CREATE)
            }

            // Send notification to invitee
            try {
                notificationService.sendNotification(
                    userId = inviteeId,
                    title = "Project Invite",
                    body = "$inviterName invited you to $projectName",
                    type = "project_invite",
                    data = mapOf(
                        "invite_id" to invite.id,
                        "project_id" to projectId,
                        "project_name" to projectName,
                        "inviter_name" to inviterName,
                        "role" to role
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Failed to send invite notification", e)
            }

            Result.success(invite)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error sending invite", e)
            Result.failure(e)
        }
    }

    suspend fun acceptInvite(inviteId: String, userId: String): Result<Unit> {
        return try {
            val invite = inviteDao.getById(inviteId)
                ?: return Result.failure(IllegalArgumentException("Invite not found"))

            if (invite.status != InviteStatus.PENDING) {
                return Result.failure(IllegalStateException("Invite is no longer pending"))
            }

            // Update invite status locally
            val now = System.currentTimeMillis()
            inviteDao.updateStatus(inviteId, InviteStatus.ACCEPTED.name, now)

            // Bug K fix: use try-catch instead of isOffline check (exceptions can occur even when online)
            val updatedInvite = invite.copy(status = InviteStatus.ACCEPTED, respondedAt = now)
            try {
                val result = supabaseDataSource.updateStatus(inviteId, InviteStatus.ACCEPTED, now)
                if (result.isFailure) {
                    Log.w(TAG, "Failed to sync invite acceptance to Supabase, queuing", result.exceptionOrNull())
                    SyncQueueHelper.queueProjectInvite(syncQueueDao, updatedInvite, SyncOperation.UPDATE)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Exception syncing invite acceptance to Supabase, queuing", e)
                SyncQueueHelper.queueProjectInvite(syncQueueDao, updatedInvite, SyncOperation.UPDATE)
            }

            // Create the actual member via ProjectRepository (bypass approval since invite was accepted)
            val role = try { ProjectRole.valueOf(invite.role) } catch (_: Exception) { ProjectRole.MEMBER }
            projectRepository.addMember(
                projectId = invite.projectId,
                userId = invite.inviteeId,
                role = role,
                invitedBy = invite.inviterId,
                bypassApproval = true
            )

            // Sync projects so the new project appears in the invitee's list immediately
            // Also syncs the real member_count from Supabase trigger, subsumes local incrementMemberCount drift
            try {
                projectRepository.syncUserProjects(userId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Failed to sync projects after accepting invite", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error accepting invite", e)
            Result.failure(e)
        }
    }

    suspend fun declineInvite(inviteId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            inviteDao.updateStatus(inviteId, InviteStatus.DECLINED.name, now)

            // Bug K fix: use try-catch instead of isOffline check
            val invite = inviteDao.getById(inviteId)
            try {
                val result = supabaseDataSource.updateStatus(inviteId, InviteStatus.DECLINED, now)
                if (result.isFailure) {
                    Log.w(TAG, "Failed to sync invite decline to Supabase, queuing", result.exceptionOrNull())
                    invite?.let { SyncQueueHelper.queueProjectInvite(syncQueueDao, it.copy(status = InviteStatus.DECLINED, respondedAt = now), SyncOperation.UPDATE) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Exception syncing invite decline to Supabase, queuing", e)
                invite?.let { SyncQueueHelper.queueProjectInvite(syncQueueDao, it.copy(status = InviteStatus.DECLINED, respondedAt = now), SyncOperation.UPDATE) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error declining invite", e)
            Result.failure(e)
        }
    }

    suspend fun cancelInvite(inviteId: String): Result<Unit> {
        return try {
            inviteDao.deleteById(inviteId)

            // Bug K fix: use try-catch instead of isOffline check
            try {
                supabaseDataSource.cancelInvite(inviteId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Failed to sync invite cancellation to Supabase (ephemeral — not queued)", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error cancelling invite", e)
            Result.failure(e)
        }
    }

    suspend fun expireOldInvites() {
        try {
            val count = inviteDao.expireOldInvites()
            if (count > 0) {
                Log.d(TAG, "Expired $count old invites")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error expiring old invites", e)
        }
    }

    suspend fun syncFromSupabase(projectId: String) {
        try {
            val result = supabaseDataSource.getProjectInvites(projectId)
            if (result.isSuccess) {
                inviteDao.insertAll(result.getOrThrow())
            }
            expireOldInvites()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error syncing invites from Supabase", e)
        }
    }

    suspend fun syncPendingForUser(userId: String) {
        try {
            val result = supabaseDataSource.getPendingForUser(userId)
            if (result.isSuccess) {
                inviteDao.insertAll(result.getOrThrow())
            }
            expireOldInvites()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Error syncing pending invites from Supabase", e)
        }
    }
}
