package com.example.kosmos.data.datasource
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

import android.util.Log
import com.example.kosmos.core.models.TimeEntry
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase Time Entry Data Source
 *
 * Handles all Supabase operations for time_entries table.
 * Provides sync functionality for offline-first architecture.
 *
 * Features:
 * - Insert/update/delete time entries
 * - Query entries by task/project/user
 * - Find running timers
 * - Calculate time totals
 */
@Singleton
class SupabaseTimeEntryDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseTimeEntryDataSource"
        private const val TABLE_NAME = "time_entries"
        private const val DEFAULT_LIMIT = 100
    }

    // ========================================================================
    // INSERT OPERATIONS
    // ========================================================================

    /**
     * Insert a new time entry into Supabase
     *
     * @param entry The time entry to insert
     * @return Result with inserted entry or error
     */
    suspend fun insertTimeEntry(entry: TimeEntry): Result<TimeEntry> {
        return try {
            Log.d(TAG, "Inserting time entry: ${entry.id}")

            supabase.from(TABLE_NAME).insert(buildJsonObject {
                put("id", entry.id)
                put("task_id", entry.taskId)
                put("project_id", entry.projectId)
                put("user_id", entry.userId)
                put("start_time", entry.startTime)
                entry.endTime?.let { put("end_time", it) }
                entry.durationSeconds?.let { put("duration_seconds", it) }
                entry.description?.let { put("description", it) }
                put("is_billable", entry.isBillable)
                entry.hourlyRate?.let { put("hourly_rate", it) }
                put("is_manual", entry.isManual)
                put("created_at", entry.createdAt)
                put("updated_at", entry.updatedAt)
            })

            Log.d(TAG, "Successfully inserted time entry: ${entry.id}")
            Result.success(entry)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert time entry", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // UPDATE OPERATIONS
    // ========================================================================

    /**
     * Update an existing time entry in Supabase
     *
     * @param entry The time entry to update
     * @return Result with updated entry or error
     */
    suspend fun updateTimeEntry(entry: TimeEntry): Result<TimeEntry> {
        return try {
            Log.d(TAG, "Updating time entry: ${entry.id}")

            supabase.from(TABLE_NAME).update(buildJsonObject {
                put("task_id", entry.taskId)
                put("project_id", entry.projectId)
                put("user_id", entry.userId)
                put("start_time", entry.startTime)
                entry.endTime?.let { put("end_time", it) } ?: run { put("end_time", JsonNull) }
                entry.durationSeconds?.let { put("duration_seconds", it) } ?: run { put("duration_seconds", JsonNull) }
                entry.description?.let { put("description", it) } ?: run { put("description", JsonNull) }
                put("is_billable", entry.isBillable)
                entry.hourlyRate?.let { put("hourly_rate", it) } ?: run { put("hourly_rate", JsonNull) }
                put("is_manual", entry.isManual)
                put("updated_at", System.currentTimeMillis())
            }) {
                filter {
                    eq("id", entry.id)
                }
            }

            Log.d(TAG, "Successfully updated time entry: ${entry.id}")
            Result.success(entry)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update time entry", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // DELETE OPERATIONS
    // ========================================================================

    /**
     * Delete a time entry from Supabase
     *
     * @param entryId The time entry ID to delete
     * @return Result with success or error
     */
    suspend fun deleteTimeEntry(entryId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting time entry: $entryId")

            supabase.from(TABLE_NAME).delete {
                filter {
                    eq("id", entryId)
                }
            }

            Log.d(TAG, "Successfully deleted time entry: $entryId")
            Result.success(Unit)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete time entry", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // QUERY OPERATIONS
    // ========================================================================

    /**
     * Get all time entries for a specific task
     *
     * @param taskId The task ID
     * @param limit Maximum number of entries to return
     * @return Result with list of time entries or error
     */
    suspend fun getEntriesForTask(
        taskId: String,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<TimeEntry>> {
        return try {
            Log.d(TAG, "Fetching time entries for task: $taskId")

            val response = supabase.from(TABLE_NAME).select(
                columns = Columns.ALL
            ) {
                filter {
                    eq("task_id", taskId)
                }
                order("start_time", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<TimeEntry>()

            Log.d(TAG, "Successfully fetched ${response.size} time entries for task: $taskId")
            Result.success(response)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch time entries for task: $taskId", e)
            Result.failure(e)
        }
    }

    /**
     * Get all time entries for a specific user
     *
     * @param userId The user ID
     * @param limit Maximum number of entries to return
     * @return Result with list of time entries or error
     */
    suspend fun getEntriesForUser(
        userId: String,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<TimeEntry>> {
        return try {
            Log.d(TAG, "Fetching time entries for user: $userId")

            val response = supabase.from(TABLE_NAME).select(
                columns = Columns.ALL
            ) {
                filter {
                    eq("user_id", userId)
                }
                order("start_time", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<TimeEntry>()

            Log.d(TAG, "Successfully fetched ${response.size} time entries for user: $userId")
            Result.success(response)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch time entries for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Get all time entries for a specific project
     *
     * @param projectId The project ID
     * @param limit Maximum number of entries to return
     * @return Result with list of time entries or error
     */
    suspend fun getEntriesForProject(
        projectId: String,
        limit: Int = DEFAULT_LIMIT
    ): Result<List<TimeEntry>> {
        return try {
            Log.d(TAG, "Fetching time entries for project: $projectId")

            val response = supabase.from(TABLE_NAME).select(
                columns = Columns.ALL
            ) {
                filter {
                    eq("project_id", projectId)
                }
                order("start_time", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<TimeEntry>()

            Log.d(TAG, "Successfully fetched ${response.size} time entries for project: $projectId")
            Result.success(response)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch time entries for project: $projectId", e)
            Result.failure(e)
        }
    }

    /**
     * Get running timers for a specific user
     * A running timer has end_time = NULL
     *
     * @param userId The user ID
     * @return Result with list of running time entries or error
     */
    suspend fun getRunningTimers(userId: String): Result<List<TimeEntry>> {
        return try {
            Log.d(TAG, "Fetching running timers for user: $userId")

            val response = supabase.from(TABLE_NAME).select(
                columns = Columns.ALL
            ) {
                filter {
                    eq("user_id", userId)
                    eq("end_time", "null")
                }
                order("start_time", Order.DESCENDING)
            }.decodeList<TimeEntry>()

            Log.d(TAG, "Successfully fetched ${response.size} running timers for user: $userId")
            Result.success(response)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch running timers for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Get a single time entry by ID
     *
     * @param entryId The time entry ID
     * @return Result with time entry or error
     */
    suspend fun getEntryById(entryId: String): Result<TimeEntry> {
        return try {
            Log.d(TAG, "Fetching time entry: $entryId")

            val response = supabase.from(TABLE_NAME).select(
                columns = Columns.ALL
            ) {
                filter {
                    eq("id", entryId)
                }
                limit(1)
            }.decodeSingle<TimeEntry>()

            Log.d(TAG, "Successfully fetched time entry: $entryId")
            Result.success(response)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch time entry: $entryId", e)
            Result.failure(e)
        }
    }
}
