package com.example.kosmos.data.datasource

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
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting task: ${task.title}", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing task in Supabase
     * @param task Task with updated fields
     * @return Result with Unit or error
     */
    suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            val updates = mapOf(
                "title" to task.title,
                "description" to task.description,
                "status" to task.status.name,
                "priority" to task.priority.name,
                "assigned_to_id" to task.assignedToId,
                "assigned_to_name" to task.assignedToName,
                "assigned_to_role" to task.assignedToRole?.name,
                "due_date" to task.dueDate,
                "tags" to task.tags,
                "updated_at" to task.updatedAt,
                "estimated_hours" to task.estimatedHours,
                "actual_hours" to task.actualHours
            )

            supabase.from(TABLE_NAME)
                .update(updates) {
                    filter {
                        eq("id", task.id)
                    }
                }

            Log.d(TAG, "Task updated successfully: id=${task.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task: id=${task.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Update task status only
     * Optimized for quick status changes
     * @param taskId Task ID
     * @param status New status
     * @param updatedAt Update timestamp
     * @return Result with Unit or error
     */
    suspend fun updateTaskStatus(
        taskId: String,
        status: TaskStatus,
        updatedAt: Long
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to status.name,
                "updated_at" to updatedAt
            )

            supabase.from(TABLE_NAME)
                .update(updates) {
                    filter {
                        eq("id", taskId)
                    }
                }

            Log.d(TAG, "Task status updated: id=$taskId, status=${status.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task status: id=$taskId", e)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task: id=$taskId", e)
            Result.failure(e)
        }
    }

    /**
     * Get tasks for a project with pagination
     * @param projectId Project ID
     * @param limit Maximum number of tasks to fetch
     * @param before Timestamp cursor for pagination (fetch tasks before this time)
     * @return Result with list of tasks or error
     */
    suspend fun getTasks(
        projectId: String,
        limit: Int = DEFAULT_LIMIT,
        before: Long? = null
    ): Result<List<Task>> {
        return try {
            val tasks = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        eq("project_id", projectId)
                        before?.let { gte("created_at", it) }
                    }
                    limit(limit.toLong())
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Task>()

            // Client-side sorting by created_at descending (as per Phase 1A pattern)
            val sortedTasks = tasks.sortedByDescending { it.createdAt }

            Log.d(TAG, "Fetched ${sortedTasks.size} tasks for project: $projectId")
            Result.success(sortedTasks)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks by status: ${status.name}", e)
            Result.failure(e)
        }
    }
}
