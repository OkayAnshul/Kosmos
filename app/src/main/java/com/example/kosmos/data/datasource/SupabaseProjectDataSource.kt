package com.example.kosmos.data.datasource

import android.util.Log
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectStatus
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
 * Data source for project operations using Supabase Postgrest
 * Handles CRUD operations and real-time subscriptions for projects
 */
@Singleton
class SupabaseProjectDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseProjectDataSource"
        private const val TABLE_NAME = "projects"
    }

    /**
     * Insert a new project into Supabase
     * @param project Project to insert
     * @return Result with inserted project or error
     */
    suspend fun insert(project: Project): Result<Project> {
        return try {
            supabase.from(TABLE_NAME)
                .insert(project)
            Result.success(project)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting project", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing project in Supabase
     * @param project Project to update
     * @return Result with updated project or error
     */
    suspend fun update(project: Project): Result<Project> {
        return try {
            supabase.from(TABLE_NAME).update(project) {
                filter {
                    eq("id", project.id)
                }
            }
            Result.success(project)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating project", e)
            Result.failure(e)
        }
    }

    /**
     * Get a project by ID
     * @param projectId Project ID to fetch
     * @return Result with project or error
     */
    suspend fun getById(projectId: String): Result<Project> {
        return try {
            val project = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("id", projectId)
                    }
                }
                .decodeSingle<Project>()
            Result.success(project)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching project by ID", e)
            Result.failure(e)
        }
    }

    /**
     * Get all projects owned by a user
     * @param userId User ID
     * @return Result with list of projects or error
     */
    suspend fun getProjectsByOwner(userId: String): Result<List<Project>> {
        return try {
            val projects = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("ownerId", userId)
                    }
                }
                .decodeList<Project>()
                .sortedByDescending { it.updatedAt }
            Result.success(projects)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching projects by owner", e)
            Result.failure(e)
        }
    }

    /**
     * Get projects by status
     * @param status Project status
     * @return Result with list of projects or error
     */
    suspend fun getProjectsByStatus(status: ProjectStatus): Result<List<Project>> {
        return try {
            val projects = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("status", status.name)
                    }
                }
                .decodeList<Project>()
                .sortedByDescending { it.updatedAt }
            Result.success(projects)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching projects by status", e)
            Result.failure(e)
        }
    }

    /**
     * Search projects by name or description
     * @param query Search query
     * @return Result with list of matching projects or error
     */
    suspend fun searchProjects(query: String): Result<List<Project>> {
        return try {
            // Note: For production, implement full-text search using Supabase FTS
            // For now, we'll fetch all and filter client-side
            val projects = supabase.from(TABLE_NAME)
                .select()
                .decodeList<Project>()
                .filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
                }
                .sortedByDescending { it.updatedAt }
            Result.success(projects)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching projects", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a project
     * @param projectId Project ID to delete
     * @return Result with Unit or error
     */
    suspend fun delete(projectId: String): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME).delete {
                filter {
                    eq("id", projectId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting project", e)
            Result.failure(e)
        }
    }

    /**
     * Update project status
     * @param projectId Project ID
     * @param status New status
     * @return Result with Unit or error
     */
    suspend fun updateStatus(projectId: String, status: ProjectStatus): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME).update({
                set("status", status.name)
                set("updated_at", System.currentTimeMillis())
            }) {
                filter {
                    eq("id", projectId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating project status", e)
            Result.failure(e)
        }
    }

    /**
     * Subscribe to real-time project changes for a specific project
     * TODO: Implement real-time subscriptions in Phase 2
     * @param projectId Project ID to monitor
     * @return Flow of Project or null
     */
    // fun observeProject(projectId: String): Flow<Project?> {
    //     // Real-time subscriptions to be implemented in Phase 2
    //     return flowOf(null)
    // }
}
