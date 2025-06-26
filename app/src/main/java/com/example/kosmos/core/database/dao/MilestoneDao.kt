package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.Milestone
import com.example.kosmos.core.models.MilestoneStatus
import kotlinx.coroutines.flow.Flow

/**
 * Milestone DAO
 *
 * Room Data Access Object for milestones table.
 * Provides queries for milestone management.
 *
 * Features:
 * - Get milestones for a project
 * - Filter by status
 * - Sort by due date or custom order
 * - CRUD operations with Flow support
 */
@Dao
interface MilestoneDao {

    // ========================================================================
    // QUERY METHODS (with Flow for reactive updates)
    // ========================================================================

    /**
     * Get all milestones for a specific project
     * Ordered by sort_order ascending
     *
     * @param projectId The project ID
     * @return Flow emitting list of milestones
     */
    @Query("SELECT * FROM milestones WHERE projectId = :projectId ORDER BY sortOrder ASC")
    fun getMilestonesForProjectFlow(projectId: String): Flow<List<Milestone>>

    /**
     * Get active milestones for a project
     *
     * @param projectId The project ID
     * @return Flow emitting list of active milestones
     */
    @Query("SELECT * FROM milestones WHERE projectId = :projectId AND status = 'ACTIVE' ORDER BY sortOrder ASC")
    fun getActiveMilestonesFlow(projectId: String): Flow<List<Milestone>>

    /**
     * Get completed milestones for a project
     *
     * @param projectId The project ID
     * @return Flow emitting list of completed milestones
     */
    @Query("SELECT * FROM milestones WHERE projectId = :projectId AND status = 'COMPLETED' ORDER BY sortOrder ASC")
    fun getCompletedMilestonesFlow(projectId: String): Flow<List<Milestone>>

    /**
     * Get milestones ordered by due date
     *
     * @param projectId The project ID
     * @return Flow emitting list of milestones ordered by due date
     */
    @Query("SELECT * FROM milestones WHERE projectId = :projectId ORDER BY dueDate ASC")
    fun getMilestonesByDueDateFlow(projectId: String): Flow<List<Milestone>>

    /**
     * Get overdue milestones
     * Milestones with due date in the past and status = ACTIVE
     *
     * @param projectId The project ID
     * @param currentTime Current timestamp
     * @return Flow emitting list of overdue milestones
     */
    @Query("SELECT * FROM milestones WHERE projectId = :projectId AND status = 'ACTIVE' AND dueDate < :currentTime ORDER BY dueDate ASC")
    fun getOverdueMilestonesFlow(projectId: String, currentTime: Long): Flow<List<Milestone>>

    /**
     * Get a single milestone by ID
     *
     * @param milestoneId The milestone ID
     * @return Flow emitting milestone or null
     */
    @Query("SELECT * FROM milestones WHERE id = :milestoneId")
    fun getMilestoneFlow(milestoneId: String): Flow<Milestone?>

    /**
     * Get a single milestone by ID (non-reactive)
     *
     * @param milestoneId The milestone ID
     * @return The milestone or null
     */
    @Query("SELECT * FROM milestones WHERE id = :milestoneId")
    suspend fun getMilestoneById(milestoneId: String): Milestone?

    /**
     * Get milestones by status
     *
     * @param projectId The project ID
     * @param status The milestone status
     * @return Flow emitting list of milestones
     */
    @Query("SELECT * FROM milestones WHERE projectId = :projectId AND status = :status ORDER BY sortOrder ASC")
    fun getMilestonesByStatusFlow(projectId: String, status: MilestoneStatus): Flow<List<Milestone>>

    /**
     * Count milestones in a project
     *
     * @param projectId The project ID
     * @return Number of milestones
     */
    @Query("SELECT COUNT(*) FROM milestones WHERE projectId = :projectId")
    suspend fun countMilestones(projectId: String): Int

    /**
     * Count active milestones
     *
     * @param projectId The project ID
     * @return Number of active milestones
     */
    @Query("SELECT COUNT(*) FROM milestones WHERE projectId = :projectId AND status = 'ACTIVE'")
    suspend fun countActiveMilestones(projectId: String): Int

    // ========================================================================
    // INSERT METHODS
    // ========================================================================

    /**
     * Insert a new milestone
     * Replace on conflict (same ID)
     *
     * @param milestone The milestone to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: Milestone)

    /**
     * Insert multiple milestones
     *
     * @param milestones List of milestones
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestones(milestones: List<Milestone>)

    // ========================================================================
    // UPDATE METHODS
    // ========================================================================

    /**
     * Update an existing milestone
     *
     * @param milestone The milestone to update
     */
    @Update
    suspend fun updateMilestone(milestone: Milestone)

    /**
     * Update multiple milestones
     *
     * @param milestones List of milestones
     */
    @Update
    suspend fun updateMilestones(milestones: List<Milestone>)

    /**
     * Update milestone status
     *
     * @param milestoneId The milestone ID
     * @param status The new status
     * @return Number of rows updated
     */
    @Query("UPDATE milestones SET status = :status, updatedAt = :updatedAt WHERE id = :milestoneId")
    suspend fun updateMilestoneStatus(milestoneId: String, status: MilestoneStatus, updatedAt: Long = System.currentTimeMillis()): Int

    /**
     * Update milestone sort order
     *
     * @param milestoneId The milestone ID
     * @param sortOrder The new sort order
     * @return Number of rows updated
     */
    @Query("UPDATE milestones SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :milestoneId")
    suspend fun updateMilestoneSortOrder(milestoneId: String, sortOrder: Int, updatedAt: Long = System.currentTimeMillis()): Int

    // ========================================================================
    // DELETE METHODS
    // ========================================================================

    /**
     * Delete a milestone by ID
     *
     * @param milestoneId The milestone ID
     * @return Number of rows deleted (0 or 1)
     */
    @Query("DELETE FROM milestones WHERE id = :milestoneId")
    suspend fun deleteMilestoneById(milestoneId: String): Int

    /**
     * Delete a milestone
     *
     * @param milestone The milestone to delete
     */
    @Delete
    suspend fun deleteMilestone(milestone: Milestone)

    /**
     * Delete all milestones for a project
     *
     * @param projectId The project ID
     * @return Number of milestones deleted
     */
    @Query("DELETE FROM milestones WHERE projectId = :projectId")
    suspend fun deleteMilestonesForProject(projectId: String): Int

    /**
     * Delete all milestones (for testing/cleanup)
     *
     * @return Number of milestones deleted
     */
    @Query("DELETE FROM milestones")
    suspend fun deleteAllMilestones(): Int
}
