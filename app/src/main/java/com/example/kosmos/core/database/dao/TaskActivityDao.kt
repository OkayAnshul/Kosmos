package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.ActivityActionType
import com.example.kosmos.core.models.TaskActivity
import kotlinx.coroutines.flow.Flow

/**
 * Task Activity DAO
 *
 * Manages local storage of task activity records.
 * Provides queries for activity timelines, logs, and user history.
 */
@Dao
interface TaskActivityDao {

    // ============================================================================
    // SINGLE ACTIVITY QUERIES
    // ============================================================================

    /**
     * Get activity by ID
     */
    @Query("SELECT * FROM task_activity WHERE id = :activityId")
    suspend fun getActivityById(activityId: String): TaskActivity?

    /**
     * Get activity by ID (Flow)
     */
    @Query("SELECT * FROM task_activity WHERE id = :activityId")
    fun getActivityByIdFlow(activityId: String): Flow<TaskActivity?>

    // ============================================================================
    // TASK-LEVEL ACTIVITY QUERIES
    // ============================================================================

    /**
     * Get all activity for a specific task (newest first)
     * Used for: Task detail screen activity timeline
     */
    @Query("SELECT * FROM task_activity WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getActivityForTaskFlow(taskId: String): Flow<List<TaskActivity>>

    /**
     * Get limited activity for a task (for preview/timeline)
     * Used for: Showing last N activities in task detail
     */
    @Query("SELECT * FROM task_activity WHERE taskId = :taskId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentActivityForTaskFlow(taskId: String, limit: Int = 5): Flow<List<TaskActivity>>

    /**
     * Get activity for a task (suspend)
     */
    @Query("SELECT * FROM task_activity WHERE taskId = :taskId ORDER BY timestamp DESC")
    suspend fun getActivityForTask(taskId: String): List<TaskActivity>

    // ============================================================================
    // PROJECT-LEVEL ACTIVITY QUERIES
    // ============================================================================

    /**
     * Get all activity for a project (newest first)
     * Used for: Project-wide activity log screen
     */
    @Query("SELECT * FROM task_activity WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getActivityForProjectFlow(projectId: String): Flow<List<TaskActivity>>

    /**
     * Get paginated activity for a project
     * Used for: Activity log with pagination
     */
    @Query("SELECT * FROM task_activity WHERE projectId = :projectId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getActivityForProjectPaginated(
        projectId: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<TaskActivity>

    /**
     * Get activity before a specific timestamp (for pagination)
     */
    @Query("SELECT * FROM task_activity WHERE projectId = :projectId AND timestamp < :beforeTimestamp ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getActivityBeforeTimestamp(
        projectId: String,
        beforeTimestamp: Long,
        limit: Int = 100
    ): List<TaskActivity>

    // ============================================================================
    // USER ACTIVITY QUERIES
    // ============================================================================

    /**
     * Get all activity by a specific user
     * Used for: User profile activity history
     */
    @Query("SELECT * FROM task_activity WHERE actorId = :userId ORDER BY timestamp DESC")
    fun getActivityForUserFlow(userId: String): Flow<List<TaskActivity>>

    /**
     * Get recent activity by a user in a project
     */
    @Query("SELECT * FROM task_activity WHERE actorId = :userId AND projectId = :projectId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentActivityForUserInProject(
        userId: String,
        projectId: String,
        limit: Int = 50
    ): List<TaskActivity>

    // ============================================================================
    // FILTERED QUERIES
    // ============================================================================

    /**
     * Get activity by action type for a project
     * Used for: Activity log filtering
     */
    @Query("SELECT * FROM task_activity WHERE projectId = :projectId AND actionType = :actionType ORDER BY timestamp DESC")
    fun getActivityByActionTypeFlow(
        projectId: String,
        actionType: ActivityActionType
    ): Flow<List<TaskActivity>>

    /**
     * Search activity by commit message
     * Used for: Activity log search
     */
    @Query("SELECT * FROM task_activity WHERE projectId = :projectId AND commitMessage LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC")
    fun searchActivityByCommitMessage(
        projectId: String,
        searchQuery: String
    ): Flow<List<TaskActivity>>

    // ============================================================================
    // INSERT/UPDATE/DELETE OPERATIONS
    // ============================================================================

    /**
     * Insert single activity
     * Used when tracking a task change
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: TaskActivity)

    /**
     * Insert multiple activities (bulk import/sync)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<TaskActivity>)

    /**
     * Update activity (rare - activities should be immutable)
     */
    @Update
    suspend fun updateActivity(activity: TaskActivity)

    /**
     * Delete activity by ID
     * Used for: Cleanup, moderation by admins
     */
    @Query("DELETE FROM task_activity WHERE id = :activityId")
    suspend fun deleteActivityById(activityId: String)

    /**
     * Delete all activity for a task
     * Used when: Task is permanently deleted
     */
    @Query("DELETE FROM task_activity WHERE taskId = :taskId")
    suspend fun deleteActivityForTask(taskId: String)

    /**
     * Delete all activity for a project
     * Used when: Project is deleted
     */
    @Query("DELETE FROM task_activity WHERE projectId = :projectId")
    suspend fun deleteActivityForProject(projectId: String)

    // ============================================================================
    // STATISTICS QUERIES
    // ============================================================================

    /**
     * Get activity count for a task
     */
    @Query("SELECT COUNT(*) FROM task_activity WHERE taskId = :taskId")
    suspend fun getActivityCountForTask(taskId: String): Int

    /**
     * Get activity count for a project
     */
    @Query("SELECT COUNT(*) FROM task_activity WHERE projectId = :projectId")
    suspend fun getActivityCountForProject(projectId: String): Int

    /**
     * Get activity count for a project (Flow)
     */
    @Query("SELECT COUNT(*) FROM task_activity WHERE projectId = :projectId")
    fun getActivityCountForProjectFlow(projectId: String): Flow<Int>

    /**
     * Get activity count by user in a project
     */
    @Query("SELECT COUNT(*) FROM task_activity WHERE actorId = :userId AND projectId = :projectId")
    suspend fun getActivityCountByUserInProject(userId: String, projectId: String): Int
}
