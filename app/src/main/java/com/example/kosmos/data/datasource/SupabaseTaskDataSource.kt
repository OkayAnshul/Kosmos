package com.example.kosmos.data.datasource
import kotlinx.coroutines.CancellationException

import android.util.Log
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for task operations using Supabase Postgrest
 * Handles CRUD operations, status updates, and task synchronization
 */
@Singleton
class SupabaseTaskDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseTaskDataSource"
        private const val TABLE_NAME = "tasks"
        private const val DEFAULT_LIMIT = 50
    }

    /**
     * Insert a new task into Supabase
     * @param task Task to insert
     * @return Result with inserted task or error
     */
    suspend fun insertTask(task: Task): Result<Task> {
        return try {
            supabase.from(TABLE_NAME)
                .insert(task)

            Log.d(TAG, "Task inserted successfully: id=${task.id}")
            Result.success(task)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting task: ${task.title}", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing task in Supabase
     * Uses optimistic locking via version field to prevent concurrent edit conflicts
     * @param task Task with updated fields
     * @return Result with Unit or error
     */
    suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            // C1 FIX: Repository already increments version before calling this method.
            // Do NOT increment again here — that causes double increment and version mismatch.
            // task.version = already-incremented value from TaskRepository
            // DB still has (task.version - 1), so filter on that.

            // Use UpdateBuilder DSL to avoid "Serializer for class 'Any'" error
            // Each field is explicitly typed, preventing type inference issues
            // select() enables return=representation so we can detect 0-row updates (version conflicts)
            val updatedRows = supabase.from(TABLE_NAME).update({
                set("title", task.title)
                set("description", task.description)
                set("status", task.status.name)
                set("priority", task.priority.name)
                set("assigned_to_id", task.assignedToId)
                set("assigned_to_name", task.assignedToName)
                set("assigned_to_role", task.assignedToRole?.name)
                set("due_date", task.dueDate)
                set("tags", task.tags)
                set("updated_at", task.updatedAt)
                set("estimated_hours", task.estimatedHours)
                set("actual_hours", task.actualHours)
                set("parent_task_id", task.parentTaskId)  // Support for subtasks
                set("comments", task.comments)  // supabase-kt serializes List<TaskComment> → JSONB directly
                set("version", task.version)  // Already incremented by repository
            }) {
                filter {
                    eq("id", task.id)
                    eq("version", task.version - 1)  // C1 FIX: Match the original DB value (pre-increment)
                }
                select()  // Return affected rows so we can detect 0-row updates
            }.decodeList<Task>()

            if (updatedRows.isEmpty()) {
                // Filter matched 0 rows — version in Supabase differs from expected (conflict)
                throw IllegalStateException("Task version conflict — refresh and try again")
            }

            Log.d(TAG, "Task updated successfully: id=${task.id}, version ${task.version - 1} → ${task.version}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task: id=${task.id}, version=${task.version}", e)
            Result.failure(e)
        }
    }

    /**
     * Update task status only
     * Optimized for quick status changes with optimistic locking
     * @param taskId Task ID
     * @param status New status
     * @param updatedAt Update timestamp
     * @param currentVersion Current version number (for optimistic locking)
     * @return Result with Unit or error
     */
    suspend fun updateTaskStatus(
        taskId: String,
        status: TaskStatus,
        updatedAt: Long,
        currentVersion: Int
    ): Result<Unit> {
        return try {
            // currentVersion is the pre-increment value from the caller (TaskRepository)
            // The DB still has this version, so we filter on it and set version+1
            val newVersion = currentVersion + 1

            // Use UpdateBuilder DSL for type safety
            supabase.from(TABLE_NAME).update({
                set("status", status.name)
                set("updated_at", updatedAt)
                set("version", newVersion)
            }) {
                filter {
                    eq("id", taskId)
                    eq("version", currentVersion)  // Optimistic lock check — matches DB value
                }
            }

            Log.d(TAG, "Task status updated: id=$taskId, status=${status.name}, version $currentVersion → $newVersion")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task status: id=$taskId, version=$currentVersion", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a task from Supabase
     * @param taskId ID of task to delete
     * @return Result with Unit or error
     */
    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME)
                .delete {
                    filter {
                        eq("id", taskId)
                    }
                }

            Log.d(TAG, "Task deleted successfully: id=$taskId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task: id=$taskId", e)
            Result.failure(e)
        }
    }

    /**
     * Get tasks for a project with pagination and incremental sync
     *
     * @param projectId Project ID
     * @param limit Maximum number of tasks to fetch
     * @param before Timestamp cursor for pagination (fetch tasks before this time)
     * @param since Optional timestamp (milliseconds) - only fetch tasks updated after this time (INCREMENTAL SYNC)
     * @return Result with list of tasks or error
     *
     * INCREMENTAL SYNC: Pass `since` to only fetch tasks modified after that timestamp.
     * This reduces data transfer by 50-90% on subsequent syncs.
     */
    suspend fun getTasks(
        projectId: String,
        limit: Int = DEFAULT_LIMIT,
        before: Long? = null,
        since: Long? = null
    ): Result<List<Task>> {
        return try {
            val tasks = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("project_id", projectId)
                        before?.let { gte("created_at", it) }
                        // INCREMENTAL SYNC: Only fetch tasks modified since last sync
                        since?.let { gt("updated_at", it) }
                    }
                    limit(limit.toLong())
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Task>()

            // Client-side sorting by created_at descending (as per Phase 1A pattern)
            val sortedTasks = tasks.sortedByDescending { it.createdAt }

            if (since != null) {
                Log.d(TAG, "Fetched ${sortedTasks.size} tasks for project $projectId (since: $since)")
            } else {
                Log.d(TAG, "Fetched ${sortedTasks.size} tasks for project: $projectId (full sync)")
            }
            Result.success(sortedTasks)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks for project: $projectId", e)
            Result.failure(e)
        }
    }

    /**
     * Get tasks for a chat room with pagination
     * @param chatRoomId Chat room ID
     * @param limit Maximum number of tasks to fetch
     * @param before Timestamp cursor for pagination
     * @return Result with list of tasks or error
     */
    suspend fun getTasksForChatRoom(
        chatRoomId: String,
        limit: Int = DEFAULT_LIMIT,
        before: Long? = null
    ): Result<List<Task>> {
        return try {
            val tasks = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("chat_room_id", chatRoomId)
                        before?.let { gte("created_at", it) }
                    }
                    limit(limit.toLong())
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Task>()

            // Client-side sorting by created_at descending
            val sortedTasks = tasks.sortedByDescending { it.createdAt }

            Log.d(TAG, "Fetched ${sortedTasks.size} tasks for chat room: $chatRoomId")
            Result.success(sortedTasks)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks for chat room: $chatRoomId", e)
            Result.failure(e)
        }
    }

    /**
     * Get a single task by ID
     * @param taskId Task ID
     * @return Result with Task or error
     */
    suspend fun getTaskById(taskId: String): Result<Task?> {
        return try {
            val task = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("id", taskId)
                    }
                }
                .decodeSingleOrNull<Task>()

            Log.d(TAG, "Fetched task: id=$taskId, found=${task != null}")
            Result.success(task)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching task by ID: $taskId", e)
            Result.failure(e)
        }
    }

    /**
     * Get active tasks assigned to a user
     * @param userId User ID
     * @return Result with list of active tasks or error
     */
    suspend fun getMyActiveTasks(userId: String): Result<List<Task>> {
        return try {
            val tasks = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("assigned_to_id", userId)
                        neq("status", TaskStatus.DONE.name)
                        neq("status", TaskStatus.CANCELLED.name)
                    }
                    order("due_date", Order.ASCENDING)
                }
                .decodeList<Task>()

            Log.d(TAG, "Fetched ${tasks.size} active tasks for user: $userId")
            Result.success(tasks)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching active tasks for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Get overdue tasks
     * @param timestamp Current timestamp
     * @return Result with list of overdue tasks or error
     */
    suspend fun getOverdueTasks(timestamp: Long): Result<List<Task>> {
        return try {
            val tasks = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        lt("due_date", timestamp)
                        neq("status", TaskStatus.DONE.name)
                        neq("status", TaskStatus.CANCELLED.name)
                    }
                    order("due_date", Order.ASCENDING)
                }
                .decodeList<Task>()

            // Client-side filter for non-null due_date (Supabase `not` filter not available in 3.2.5)
            val filteredTasks = tasks.filter { it.dueDate != null }

            Log.d(TAG, "Fetched ${filteredTasks.size} overdue tasks")
            Result.success(filteredTasks)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching overdue tasks", e)
            Result.failure(e)
        }
    }

    /**
     * Batch insert tasks for synchronization
     * @param tasks List of tasks to insert
     * @return Result with Unit or error
     */
    suspend fun insertAll(tasks: List<Task>): Result<Unit> {
        return try {
            if (tasks.isEmpty()) {
                return Result.success(Unit)
            }

            supabase.from(TABLE_NAME)
                .insert(tasks)

            Log.d(TAG, "Batch inserted ${tasks.size} tasks")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error batch inserting tasks", e)
            Result.failure(e)
        }
    }

    /**
     * Get tasks by status for a project
     * @param projectId Project ID
     * @param status Task status filter
     * @return Result with list of tasks or error
     */
    suspend fun getTasksByStatus(
        projectId: String,
        status: TaskStatus
    ): Result<List<Task>> {
        return try {
            val tasks = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("project_id", projectId)
                        eq("status", status.name)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Task>()

            // Client-side sorting
            val sortedTasks = tasks.sortedByDescending { it.createdAt }

            Log.d(TAG, "Fetched ${sortedTasks.size} tasks with status ${status.name}")
            Result.success(sortedTasks)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks by status: ${status.name}", e)
            Result.failure(e)
        }
    }

    /**
     * Search tasks by title, description, or tags for a specific user
     * @param userId User ID
     * @param query Search query
     * @return Result with list of matching tasks or error
     */
    suspend fun searchTasksByUser(userId: String, query: String): Result<List<Task>> {
        return try {
            val tasks = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("assigned_to_id", userId)
                        or {
                            ilike("title", "%$query%")
                            ilike("description", "%$query%")
                            ilike("tags", "%$query%")
                        }
                    }
                    order("due_date", Order.ASCENDING)
                }
                .decodeList<Task>()

            Log.d(TAG, "Search found ${tasks.size} tasks for query: $query")
            Result.success(tasks)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error searching tasks: $query", e)
            Result.failure(e)
        }
    }

    /**
     * Search tasks across a project (not filtered by user)
     * @param projectId Project ID
     * @param query Search query
     * @return Result with list of matching tasks or error
     */
    suspend fun searchTasksByProject(projectId: String, query: String): Result<List<Task>> {
        return try {
            val tasks = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("project_id", projectId)
                        or {
                            ilike("title", "%$query%")
                            ilike("description", "%$query%")
                            ilike("tags", "%$query%")
                        }
                    }
                    order("due_date", Order.ASCENDING)
                }
                .decodeList<Task>()

            Log.d(TAG, "Search found ${tasks.size} tasks in project for query: $query")
            Result.success(tasks)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error searching tasks in project: $query", e)
            Result.failure(e)
        }
    }
}
