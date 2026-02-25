package com.example.kosmos.data.datasource
import kotlinx.coroutines.CancellationException

import android.util.Log
import com.example.kosmos.core.models.JoinRequestStatus
import com.example.kosmos.core.models.ProjectJoinRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseProjectJoinRequestDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseJoinReqDS"
        private const val TABLE_NAME = "project_join_requests"
    }

    suspend fun createRequest(request: ProjectJoinRequest): Result<ProjectJoinRequest> {
        return try {
            supabase.from(TABLE_NAME).insert(request)
            Log.d(TAG, "Join request created: ${request.id}")
            Result.success(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error creating join request", e)
            Result.failure(e)
        }
    }

    suspend fun getByProject(projectId: String): Result<List<ProjectJoinRequest>> {
        return try {
            val requests = supabase.from(TABLE_NAME).select {
                filter { eq("project_id", projectId) }
                order("created_at", Order.DESCENDING)
            }.decodeList<ProjectJoinRequest>()
            Result.success(requests)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching join requests for project", e)
            Result.failure(e)
        }
    }

    suspend fun getByRequester(userId: String): Result<List<ProjectJoinRequest>> {
        return try {
            val requests = supabase.from(TABLE_NAME).select {
                filter { eq("requester_id", userId) }
                order("created_at", Order.DESCENDING)
            }.decodeList<ProjectJoinRequest>()
            Result.success(requests)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching join requests by requester", e)
            Result.failure(e)
        }
    }

    suspend fun updateStatus(
        requestId: String,
        status: JoinRequestStatus,
        reviewedBy: String? = null,
        respondedAt: Long = System.currentTimeMillis()
    ): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME).update({
                set("status", status.name)
                set("reviewed_by", reviewedBy)
                set("responded_at", respondedAt)
            }) {
                filter { eq("id", requestId) }
            }
            Log.d(TAG, "Join request $requestId updated to $status")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating join request status", e)
            Result.failure(e)
        }
    }

    suspend fun cancelRequest(requestId: String): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME).delete {
                filter { eq("id", requestId) }
            }
            Log.d(TAG, "Join request $requestId cancelled")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling join request", e)
            Result.failure(e)
        }
    }
}
