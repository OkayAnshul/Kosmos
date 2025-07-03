package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.TaskDependency
import kotlinx.coroutines.flow.Flow

/**
 * Task Dependency DAO
 *
 * Room Data Access Object for task_dependencies table.
 * Provides queries for dependency management.
 *
 * Features:
 * - Get dependencies for a task
 * - Get blocking/blocked tasks
 * - Check for circular dependencies
 * - CRUD operations with Flow support
 */
@Dao
interface TaskDependencyDao {

    // ========================================================================
    // QUERY METHODS (with Flow for reactive updates)
    // ========================================================================

    /**
     * Get all dependencies for a specific task
     * Returns tasks that this task depends on
     *
     * @param taskId The task ID
     * @return Flow emitting list of dependencies
     */
    @Query("SELECT * FROM task_dependencies WHERE taskId = :taskId")
    fun getDependenciesForTaskFlow(taskId: String): Flow<List<TaskDependency>>

    /**
     * Get all tasks that depend on this task
     * Returns tasks that are blocked by this task
     *
     * @param taskId The task ID
     * @return Flow emitting list of dependencies
     */
    @Query("SELECT * FROM task_dependencies WHERE dependsOnTaskId = :taskId")
    fun getDependentTasksFlow(taskId: String): Flow<List<TaskDependency>>

    /**
     * Get all blocking dependencies for a task
     * Returns tasks that must complete before this task can start
     *
     * @param taskId The task ID
     * @return Flow emitting list of blocking dependencies
     */
    @Query("SELECT * FROM task_dependencies WHERE taskId = :taskId AND dependencyType = 'BLOCKS'")
    fun getBlockingDependenciesFlow(taskId: String): Flow<List<TaskDependency>>

    /**
     * Get all tasks blocked by this task
     * Returns tasks that cannot start until this task is complete
     *
     * @param taskId The task ID
     * @return Flow emitting list of blocked tasks
     */
    @Query("SELECT * FROM task_dependencies WHERE dependsOnTaskId = :taskId AND dependencyType = 'BLOCKS'")
    fun getBlockedTasksFlow(taskId: String): Flow<List<TaskDependency>>

    /**
     * Get all related dependencies for a task
     *
     * @param taskId The task ID
     * @return Flow emitting list of related dependencies
     */
    @Query("SELECT * FROM task_dependencies WHERE (taskId = :taskId OR dependsOnTaskId = :taskId) AND dependencyType = 'RELATED_TO'")
    fun getRelatedDependenciesFlow(taskId: String): Flow<List<TaskDependency>>

    /**
     * Get a specific dependency by task IDs
     *
     * @param taskId The task ID
     * @param dependsOnTaskId The dependency task ID
     * @return The dependency or null
     */
    @Query("SELECT * FROM task_dependencies WHERE taskId = :taskId AND dependsOnTaskId = :dependsOnTaskId LIMIT 1")
    suspend fun getDependency(taskId: String, dependsOnTaskId: String): TaskDependency?

    /**
     * Check if a dependency exists
     *
     * @param taskId The task ID
     * @param dependsOnTaskId The dependency task ID
     * @return True if dependency exists
     */
    @Query("SELECT COUNT(*) > 0 FROM task_dependencies WHERE taskId = :taskId AND dependsOnTaskId = :dependsOnTaskId")
    suspend fun dependencyExists(taskId: String, dependsOnTaskId: String): Boolean

    /**
     * Get all dependencies in a project (for validation)
     *
     * @return List of all dependencies
     */
    @Query("SELECT * FROM task_dependencies")
    suspend fun getAllDependencies(): List<TaskDependency>

    /**
     * Get dependency path for cycle detection
     * Returns all tasks that a given task transitively depends on
     *
     * Note: This is a simplified query. Full cycle detection is done in DependencyValidator.
     *
     * @param taskId The task ID
     * @return List of task IDs in the dependency chain
     */
    @Query("""
        WITH RECURSIVE dependency_chain AS (
            SELECT dependsOnTaskId as task_id, 0 as depth
            FROM task_dependencies
            WHERE taskId = :taskId AND dependencyType = 'BLOCKS'

            UNION ALL

            SELECT td.dependsOnTaskId, dc.depth + 1
            FROM task_dependencies td
            INNER JOIN dependency_chain dc ON td.taskId = dc.task_id
            WHERE td.dependencyType = 'BLOCKS' AND dc.depth < 10
        )
        SELECT DISTINCT task_id FROM dependency_chain
    """)
    suspend fun getDependencyChain(taskId: String): List<String>

    // ========================================================================
    // INSERT METHODS
    // ========================================================================

    /**
     * Insert a new dependency
     * Replace on conflict (same task_id and depends_on_task_id)
     *
     * @param dependency The dependency to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDependency(dependency: TaskDependency)

    /**
     * Insert multiple dependencies
     *
     * @param dependencies List of dependencies
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDependencies(dependencies: List<TaskDependency>)

    // ========================================================================
    // DELETE METHODS
    // ========================================================================

    /**
     * Delete a specific dependency by ID
     *
     * @param dependencyId The dependency ID
     * @return Number of rows deleted (0 or 1)
     */
    @Query("DELETE FROM task_dependencies WHERE id = :dependencyId")
    suspend fun deleteDependencyById(dependencyId: String): Int

    /**
     * Delete a dependency
     *
     * @param dependency The dependency to delete
     */
    @Delete
    suspend fun deleteDependency(dependency: TaskDependency)

    /**
     * Delete a specific dependency between two tasks
     *
     * @param taskId The task ID
     * @param dependsOnTaskId The dependency task ID
     * @return Number of rows deleted
     */
    @Query("DELETE FROM task_dependencies WHERE taskId = :taskId AND dependsOnTaskId = :dependsOnTaskId")
    suspend fun deleteDependencyBetweenTasks(taskId: String, dependsOnTaskId: String): Int

    /**
     * Delete all dependencies for a task (when task is deleted)
     *
     * @param taskId The task ID
     * @return Number of dependencies deleted
     */
    @Query("DELETE FROM task_dependencies WHERE taskId = :taskId OR dependsOnTaskId = :taskId")
    suspend fun deleteDependenciesForTask(taskId: String): Int

    /**
     * Delete all dependencies (for testing/cleanup)
     *
     * @return Number of dependencies deleted
     */
    @Query("DELETE FROM task_dependencies")
    suspend fun deleteAllDependencies(): Int
}
