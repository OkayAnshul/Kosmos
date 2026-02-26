package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.TimeEntry
import kotlinx.coroutines.flow.Flow

/**
 * Time Entry DAO
 *
 * Room Data Access Object for time_entries table.
 * Provides queries for time tracking functionality.
 *
 * Features:
 * - Get time entries for task/project/user
 * - Find running timers
 * - Calculate total time
 * - CRUD operations with Flow support
 */
@Dao
interface TimeEntryDao {

    // ========================================================================
    // QUERY METHODS (with Flow for reactive updates)
    // ========================================================================

    /**
     * Get all time entries for a specific task
     * Ordered by start time (newest first)
     *
     * @param taskId The task ID
     * @return Flow emitting list of time entries
     */
    @Query("SELECT * FROM time_entries WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getEntriesForTaskFlow(taskId: String): Flow<List<TimeEntry>>

    /**
     * Get recent time entries for a task (limited)
     * Useful for showing last N entries in UI
     *
     * @param taskId The task ID
     * @param limit Maximum number of entries to return
     * @return Flow emitting list of time entries
     */
    @Query("SELECT * FROM time_entries WHERE taskId = :taskId ORDER BY startTime DESC LIMIT :limit")
    fun getRecentEntriesForTaskFlow(taskId: String, limit: Int = 5): Flow<List<TimeEntry>>

    /**
     * Get all time entries for a specific user
     * Ordered by start time (newest first)
     *
     * @param userId The user ID
     * @return Flow emitting list of time entries
     */
    @Query("SELECT * FROM time_entries WHERE userId = :userId ORDER BY startTime DESC")
    fun getEntriesForUserFlow(userId: String): Flow<List<TimeEntry>>

    /**
     * Get all time entries for a specific project
     * Useful for project-wide time reports
     *
     * @param projectId The project ID
     * @return Flow emitting list of time entries
     */
    @Query("SELECT * FROM time_entries WHERE projectId = :projectId ORDER BY startTime DESC")
    fun getEntriesForProjectFlow(projectId: String): Flow<List<TimeEntry>>

    /**
     * Get all running timers for a specific user
     * A running timer has endTime = NULL
     *
     * @param userId The user ID
     * @return Flow emitting list of running time entries
     */
    @Query("SELECT * FROM time_entries WHERE userId = :userId AND endTime IS NULL ORDER BY startTime DESC")
    fun getRunningTimersFlow(userId: String): Flow<List<TimeEntry>>

    /**
     * Get running timer for a specific task and user
     * Returns null if no timer is running
     *
     * @param taskId The task ID
     * @param userId The user ID
     * @return Flow emitting running time entry or null
     */
    @Query("SELECT * FROM time_entries WHERE taskId = :taskId AND userId = :userId AND endTime IS NULL LIMIT 1")
    fun getRunningTimerForTaskFlow(taskId: String, userId: String): Flow<TimeEntry?>

    /**
     * Get a single time entry by ID
     *
     * @param entryId The time entry ID
     * @return The time entry or null
     */
    @Query("SELECT * FROM time_entries WHERE id = :entryId")
    suspend fun getEntryById(entryId: String): TimeEntry?

    /**
     * Get total time spent on a task (in seconds)
     * Sums all durationSeconds for completed entries
     *
     * @param taskId The task ID
     * @return Total duration in seconds
     */
    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM time_entries WHERE taskId = :taskId AND durationSeconds IS NOT NULL")
    suspend fun getTotalTimeForTask(taskId: String): Int

    /**
     * Get total billable time for a task (in seconds)
     * Sums durationSeconds where isBillable = true
     *
     * @param taskId The task ID
     * @return Total billable duration in seconds
     */
    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM time_entries WHERE taskId = :taskId AND isBillable = 1 AND durationSeconds IS NOT NULL")
    suspend fun getBillableTimeForTask(taskId: String): Int

    /**
     * Get total time for a project (in seconds)
     *
     * @param projectId The project ID
     * @return Total duration in seconds
     */
    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM time_entries WHERE projectId = :projectId AND durationSeconds IS NOT NULL")
    suspend fun getTotalTimeForProject(projectId: String): Int

    /**
     * Check if a user has any running timers
     *
     * @param userId The user ID
     * @return True if user has at least one running timer
     */
    @Query("SELECT COUNT(*) > 0 FROM time_entries WHERE userId = :userId AND endTime IS NULL")
    suspend fun hasRunningTimer(userId: String): Boolean

    // ========================================================================
    // INSERT METHODS
    // ========================================================================

    /**
     * Insert a new time entry
     * Replace on conflict (same ID)
     *
     * @param entry The time entry to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimeEntry)

    /**
     * Insert multiple time entries
     *
     * @param entries List of time entries
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<TimeEntry>)

    // ========================================================================
    // UPDATE METHODS
    // ========================================================================

    /**
     * Update an existing time entry
     *
     * @param entry The time entry to update
     */
    @Update
    suspend fun updateEntry(entry: TimeEntry)

    /**
     * Update multiple time entries
     *
     * @param entries List of time entries
     */
    @Update
    suspend fun updateEntries(entries: List<TimeEntry>)

    // ========================================================================
    // DELETE METHODS
    // ========================================================================

    /**
     * Delete a time entry by ID
     *
     * @param entryId The time entry ID to delete
     * @return Number of rows deleted (0 or 1)
     */
    @Query("DELETE FROM time_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: String): Int

    /**
     * Delete a time entry
     *
     * @param entry The time entry to delete
     */
    @Delete
    suspend fun deleteEntry(entry: TimeEntry)

    /**
     * Delete all time entries for a task
     * Used when task is deleted (cascade should handle this, but kept for manual cleanup)
     *
     * @param taskId The task ID
     * @return Number of entries deleted
     */
    @Query("DELETE FROM time_entries WHERE taskId = :taskId")
    suspend fun deleteEntriesForTask(taskId: String): Int

    /**
     * Delete all time entries for a user
     *
     * @param userId The user ID
     * @return Number of entries deleted
     */
    @Query("DELETE FROM time_entries WHERE userId = :userId")
    suspend fun deleteEntriesForUser(userId: String): Int

    /**
     * Delete all time entries (for testing/cleanup)
     *
     * @return Number of entries deleted
     */
    @Query("DELETE FROM time_entries")
    suspend fun deleteAllEntries(): Int
}
