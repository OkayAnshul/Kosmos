package com.example.kosmos.data.datasource
import kotlinx.coroutines.CancellationException

import android.util.Log
import com.example.kosmos.core.models.Milestone
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase Milestone Data Source
 *
 * Handles all Supabase operations for milestones table.
 * Provides sync functionality for offline-first architecture.
 */
@Singleton
class SupabaseMilestoneDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseMilestoneDataSource"
        private const val TABLE_NAME = "milestones"
    }

    // ========================================================================
    // INSERT OPERATIONS
    // ========================================================================

    /**
     * Insert a new milestone into Supabase
     */
    suspend fun insertMilestone(milestone: Milestone): Result<Milestone> {
        return try {
            Log.d(TAG, "Inserting milestone: ${milestone.id}")

            supabase.from(TABLE_NAME).insert(
                mapOf(
                    "id" to milestone.id,
                    "project_id" to milestone.projectId,
                    "name" to milestone.name,
                    "description" to milestone.description,
                    "due_date" to milestone.dueDate,
                    "status" to milestone.status.name.lowercase(),
                    "color" to milestone.color,
                    "sort_order" to milestone.sortOrder,
                    "created_at" to milestone.createdAt,
                    "created_by" to milestone.createdBy,
                    "updated_at" to milestone.updatedAt
                )
            )

            Log.d(TAG, "Successfully inserted milestone: ${milestone.id}")
            Result.success(milestone)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert milestone", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // UPDATE OPERATIONS
    // ========================================================================

    /**
     * Update an existing milestone in Supabase
     */
    suspend fun updateMilestone(milestone: Milestone): Result<Milestone> {
        return try {
            Log.d(TAG, "Updating milestone: ${milestone.id}")

            supabase.from(TABLE_NAME).update(
                mapOf(
                    "name" to milestone.name,
                    "description" to milestone.description,
                    "due_date" to milestone.dueDate,
                    "status" to milestone.status.name.lowercase(),
                    "color" to milestone.color,
                    "sort_order" to milestone.sortOrder,
                    "updated_at" to System.currentTimeMillis()
                )
            ) {
                filter {
                    eq("id", milestone.id)
                }
            }

            Log.d(TAG, "Successfully updated milestone: ${milestone.id}")
            Result.success(milestone)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update milestone", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // DELETE OPERATIONS
    // ========================================================================

    /**
     * Delete a milestone from Supabase
     */
    suspend fun deleteMilestone(milestoneId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting milestone: $milestoneId")

            supabase.from(TABLE_NAME).delete {
                filter {
                    eq("id", milestoneId)
                }
            }

            Log.d(TAG, "Successfully deleted milestone: $milestoneId")
            Result.success(Unit)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete milestone", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // QUERY OPERATIONS
    // ========================================================================

    /**
     * Get all milestones for a specific project
     */
    suspend fun getMilestonesForProject(projectId: String): Result<List<Milestone>> {
        return try {
            Log.d(TAG, "Fetching milestones for project: $projectId")

            val response = supabase.from(TABLE_NAME).select(
                columns = Columns.ALL
            ) {
                filter {
                    eq("project_id", projectId)
                }
                order("sort_order", Order.ASCENDING)
            }.decodeList<Milestone>()

            Log.d(TAG, "Successfully fetched ${response.size} milestones for project: $projectId")
            Result.success(response)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch milestones for project: $projectId", e)
            Result.failure(e)
        }
    }

    /**
     * Get a single milestone by ID
     */
    suspend fun getMilestoneById(milestoneId: String): Result<Milestone> {
        return try {
            Log.d(TAG, "Fetching milestone: $milestoneId")

            val response = supabase.from(TABLE_NAME).select(
                columns = Columns.ALL
            ) {
                filter {
                    eq("id", milestoneId)
                }
                limit(1)
            }.decodeSingle<Milestone>()

            Log.d(TAG, "Successfully fetched milestone: $milestoneId")
            Result.success(response)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch milestone: $milestoneId", e)
            Result.failure(e)
        }
    }
}
