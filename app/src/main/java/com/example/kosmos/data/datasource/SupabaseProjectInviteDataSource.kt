package com.example.kosmos.data.datasource
import kotlinx.coroutines.CancellationException

import android.util.Log
import com.example.kosmos.core.models.InviteStatus
import com.example.kosmos.core.models.ProjectInvite
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseProjectInviteDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseProjectInviteDS"
        private const val TABLE_NAME = "project_invites"
    }

    suspend fun createInvite(invite: ProjectInvite): Result<ProjectInvite> {
        return try {
            supabase.from(TABLE_NAME).insert(invite)
            Log.d(TAG, "Invite created: ${invite.id}")
            Result.success(invite)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error creating invite", e)
            Result.failure(e)
        }
    }

    suspend fun getProjectInvites(projectId: String): Result<List<ProjectInvite>> {
        return try {
            val invites = supabase.from(TABLE_NAME).select {
                filter { eq("project_id", projectId) }
                order("created_at", Order.DESCENDING)
            }.decodeList<ProjectInvite>()
            Result.success(invites)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching project invites", e)
            Result.failure(e)
        }
    }

    suspend fun getPendingForUser(userId: String): Result<List<ProjectInvite>> {
        return try {
            val invites = supabase.from(TABLE_NAME).select {
                filter {
                    eq("invitee_id", userId)
                    eq("status", InviteStatus.PENDING.name)
                }
                order("created_at", Order.DESCENDING)
            }.decodeList<ProjectInvite>()
            Result.success(invites)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching pending invites for user", e)
            Result.failure(e)
        }
    }

    suspend fun updateStatus(inviteId: String, status: InviteStatus, respondedAt: Long = System.currentTimeMillis()): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME).update({
                set("status", status.name)
                set("responded_at", respondedAt)
            }) {
                filter { eq("id", inviteId) }
            }
            Log.d(TAG, "Invite $inviteId updated to $status")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating invite status", e)
            Result.failure(e)
        }
    }

    suspend fun cancelInvite(inviteId: String): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME).delete {
                filter { eq("id", inviteId) }
            }
            Log.d(TAG, "Invite $inviteId cancelled")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling invite", e)
            Result.failure(e)
        }
    }
}
