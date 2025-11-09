package com.example.kosmos.data.datasource

import android.util.Log
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for project member operations using Supabase Postgrest
 * Handles CRUD operations and real-time subscriptions for project members
 */
@Singleton
class SupabaseProjectMemberDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseProjectMemberDataSource"
        private const val TABLE_NAME = "project_members"
    }

    /**
     * Add a new member to a project
     * @param member ProjectMember to insert
     * @return Result with inserted member or error
     */
    suspend fun insert(member: ProjectMember): Result<ProjectMember> {
        return try {
            supabase.from(TABLE_NAME)
                .insert(member)
            Result.success(member)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting project member", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing project member
     * @param member ProjectMember to update
     * @return Result with updated member or error
     */
    suspend fun update(member: ProjectMember): Result<ProjectMember> {
        return try {
            supabase.from(TABLE_NAME).update(member) {
                filter {
                    eq("id", member.id)
                }
            }
            Result.success(member)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating project member", e)
            Result.failure(e)
        }
    }

    /**
     * Get a member by project and user ID
     * @param projectId Project ID
     * @param userId User ID
     * @return Result with member or error
     */
    suspend fun getMemberByProjectAndUser(projectId: String, userId: String): Result<ProjectMember?> {
        return try {
            val member = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("project_id", projectId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<ProjectMember>()
            Result.success(member)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching member by project and user", e)
            Result.failure(e)
        }
    }

    /**
     * Get all active members of a project
     * @param projectId Project ID
     * @return Result with list of members or error
     */
    suspend fun getProjectMembers(projectId: String): Result<List<ProjectMember>> {
        return try {
            val members = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("project_id", projectId)
                        eq("is_active", true)
                    }
                }
                .decodeList<ProjectMember>()
                .sortedBy { it.joinedAt }
            Result.success(members)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching project members", e)
            Result.failure(e)
        }
    }

    /**
     * Get members by role in a project
     * @param projectId Project ID
     * @param role Role to filter by
     * @return Result with list of members or error
     */
    suspend fun getMembersByRole(projectId: String, role: ProjectRole): Result<List<ProjectMember>> {
        return try {
            val members = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("project_id", projectId)
                        eq("role", role.name)
                        eq("is_active", true)
                    }
                }
                .decodeList<ProjectMember>()
            Result.success(members)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching members by role", e)
            Result.failure(e)
        }
    }

    /**
     * Get all projects a user is a member of
     * @param userId User ID
     * @return Result with list of memberships or error
     */
    suspend fun getUserMemberships(userId: String): Result<List<ProjectMember>> {
        return try {
            val memberships = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_active", true)
                    }
                }
                .decodeList<ProjectMember>()
                .sortedByDescending { it.lastActivityAt }
            Result.success(memberships)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user memberships", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a member from a project
     * @param projectId Project ID
     * @param userId User ID
     * @return Result with Unit or error
     */
    suspend fun removeMember(projectId: String, userId: String): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME).delete {
                filter {
                    eq("project_id", projectId)
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing member", e)
            Result.failure(e)
        }
    }

    /**
     * Update a member's role
     * @param memberId Member ID
     * @param newRole New role
     * @return Result with Unit or error
     */
    suspend fun updateRole(memberId: String, newRole: ProjectRole): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME).update({
                set("role", newRole.name)
            }) {
                filter {
                    eq("id", memberId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating member role", e)
            Result.failure(e)
        }
    }

    /**
     * Update member's active status
     * @param memberId Member ID
     * @param isActive Active status
     * @return Result with Unit or error
     */
    suspend fun updateStatus(memberId: String, isActive: Boolean): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME).update({
                set("is_active", isActive)
            }) {
                filter {
                    eq("id", memberId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating member status", e)
            Result.failure(e)
        }
    }

    /**
     * Update member's last activity timestamp
     * @param projectId Project ID
     * @param userId User ID
     * @param timestamp Activity timestamp
     * @return Result with Unit or error
     */
    suspend fun updateLastActivity(projectId: String, userId: String, timestamp: Long): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME).update({
                set("last_activity_at", timestamp)
            }) {
                filter {
                    eq("project_id", projectId)
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last activity", e)
            Result.failure(e)
        }
    }

    /**
     * Get count of active members in a project
     * @param projectId Project ID
     * @return Result with member count or error
     */
    suspend fun getActiveMemberCount(projectId: String): Result<Int> {
        return try {
            val members = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("project_id", projectId)
                        eq("is_active", true)
                    }
                }
                .decodeList<ProjectMember>()
            Result.success(members.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching member count", e)
            Result.failure(e)
        }
    }

    /**
     * Subscribe to real-time member changes for a project
     * TODO: Implement real-time subscriptions in Phase 2
     * @param projectId Project ID to monitor
     * @return Flow of PostgresAction events
     */
    // fun observeProjectMembers(projectId: String): Flow<PostgresAction> {
    //     // Real-time subscriptions to be implemented in Phase 2
    //     return flowOf()
    // }
}
