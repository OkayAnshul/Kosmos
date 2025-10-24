package com.example.kosmos.data.repository

import com.example.kosmos.core.database.dao.TaskDao
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling task operations
 * Manages task CRUD operations and status updates
 */
@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {

    /**
     * Get all tasks for a chat room
     * @param chatRoomId Chat room ID
     * @return Flow of task list
     */
    fun getTasksForChatRoomFlow(chatRoomId: String): Flow<List<Task>> {
        return taskDao.getTasksForChatRoomFlow(chatRoomId)
    }

    /**
     * Get tasks by status for a chat room
     * @param chatRoomId Chat room ID
     * @param status Task status to filter by
     * @return Flow of filtered task list
     */
    fun getTasksByStatusFlow(chatRoomId: String, status: TaskStatus): Flow<List<Task>> {
        return taskDao.getTasksByStatusFlow(chatRoomId, status)
    }

    /**
     * Get active tasks assigned to a user
     * @param userId User ID
     * @return Flow of user's active tasks
     */
    fun getMyActiveTasksFlow(userId: String): Flow<List<Task>> {
        return taskDao.getMyActiveTasksFlow(userId)
    }

    /**
     * Get a specific task by ID
     * @param taskId Task ID
     * @return Flow of Task or null
     */
    fun getTaskByIdFlow(taskId: String): Flow<Task?> {
        return taskDao.getTaskByIdFlow(taskId)
    }

    /**
     * Create a new task
     * @param task Task to create
     * @return Result with task ID or error
     */
    suspend fun createTask(task: Task): Result<String> {
        return try {
            val taskId = if (task.id.isBlank()) {
                java.util.UUID.randomUUID().toString()
            } else {
                task.id
            }

            val taskWithId = task.copy(
                id = taskId,
                createdAt = System.currentTimeMillis()
            )

            taskDao.insertTask(taskWithId)
            Result.success(taskId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing task
     * @param task Task to update
     * @return Result indicating success or failure
     */
    suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            taskDao.updateTask(task)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update task status
     * @param taskId Task ID
     * @param status New status
     * @return Result indicating success or failure
     */
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit> {
        return try {
            val task = taskDao.getTaskById(taskId)
                ?: return Result.failure(Exception("Task not found"))

            val updatedTask = task.copy(status = status)
            taskDao.updateTask(updatedTask)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Assign a task to a user
     * @param taskId Task ID
     * @param userId User ID to assign to
     * @return Result indicating success or failure
     */
    suspend fun assignTask(taskId: String, userId: String): Result<Unit> {
        return try {
            val task = taskDao.getTaskById(taskId)
                ?: return Result.failure(Exception("Task not found"))

            val updatedTask = task.copy(assignedToId = userId)
            taskDao.updateTask(updatedTask)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a task
     * @param taskId Task ID
     * @return Result indicating success or failure
     */
    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            taskDao.deleteTaskById(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get overdue tasks
     * @return List of overdue tasks
     */
    suspend fun getOverdueTasks(): List<Task> {
        return try {
            val currentTime = System.currentTimeMillis()
            taskDao.getOverdueTasks(currentTime)
        } catch (e: Exception) {
            emptyList()
        }
    }
}