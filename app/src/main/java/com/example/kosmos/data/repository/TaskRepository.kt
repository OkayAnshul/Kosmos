package com.example.kosmos.data.repository

import android.util.Log
import com.example.kosmos.core.database.dao.ProjectMemberDao
import com.example.kosmos.core.database.dao.TaskDao
import com.example.kosmos.core.models.Permission
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.core.validators.PermissionChecker
import com.example.kosmos.core.validators.RoleValidator
import com.example.kosmos.data.datasource.SupabaseTaskDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling task operations with RBAC enforcement
 * Manages task CRUD operations, status updates, and Supabase synchronization
 * Uses hybrid pattern: Room-first for instant UI, then Supabase sync
 */
@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val projectMemberDao: ProjectMemberDao,
    private val supabaseTaskDataSource: SupabaseTaskDataSource
) {

    companion object {
        private const val TAG = "TaskRepository"
    }

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
     * Requires CREATE_TASKS permission
     * Uses hybrid pattern: saves to Room immediately, then syncs to Supabase
     *
     * @param task Task to create
     * @param creatorId User ID creating the task
     * @return Result with task ID or error
     */
    suspend fun createTask(task: Task, creatorId: String): Result<String> {
        return try {
            // Check permission
            val member = projectMemberDao.getMemberByProjectAndUser(task.projectId, creatorId)
                ?: return Result.failure(SecurityException("You are not a member of this project"))

            val permissionResult = PermissionChecker.hasPermission(member, Permission.CREATE_TASKS)
            if (permissionResult !is PermissionChecker.PermissionResult.Granted) {
                return Result.failure(
                    PermissionChecker.PermissionDeniedException(
                        permissionResult.getDeniedReason() ?: "Permission denied"
                    )
                )
            }

            val taskId = if (task.id.isBlank()) {
                java.util.UUID.randomUUID().toString()
            } else {
                task.id
            }

            val taskWithId = task.copy(
                id = taskId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                createdById = creatorId,
                createdByRole = member.role // Store creator's role
            )

            // HYBRID PATTERN: Save to Room first (instant UI update)
            taskDao.insertTask(taskWithId)

            // Sync to Supabase in background (don't block UI)
            try {
                val supabaseResult = supabaseTaskDataSource.insertTask(taskWithId)
                if (supabaseResult.isFailure) {
                    val error = supabaseResult.exceptionOrNull()
                    Log.e(TAG, "❌ SUPABASE SYNC FAILED for task", error)
                    Log.e(TAG, "Possible causes: RLS policies blocking insert, network error, auth token expired")
                    Log.e(TAG, "Task saved locally only. Check Supabase RLS policies and network connection.")
                    // Continue anyway - task is saved locally
                } else {
                    Log.d(TAG, "✅ Task synced to Supabase successfully: $taskId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing task to Supabase (possible offline mode)", e)
                // Continue anyway - task is saved locally
            }

            Result.success(taskId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating task", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing task
     * Uses hybrid pattern: updates Room immediately, then syncs to Supabase
     * @param task Task to update
     * @return Result indicating success or failure
     */
    suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            val updatedTask = task.copy(updatedAt = System.currentTimeMillis())

            // HYBRID PATTERN: Update Room first (instant UI update)
            taskDao.updateTask(updatedTask)

            // Sync to Supabase in background
            try {
                val supabaseResult = supabaseTaskDataSource.updateTask(updatedTask)
                if (supabaseResult.isFailure) {
                    Log.w(TAG, "Failed to sync task update to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing task update to Supabase (offline mode active)", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task", e)
            Result.failure(e)
        }
    }

    /**
     * Update task status
     * Uses hybrid pattern: updates Room immediately, then syncs to Supabase
     * @param taskId Task ID
     * @param status New status
     * @return Result indicating success or failure
     */
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit> {
        return try {
            val task = taskDao.getTaskById(taskId)
                ?: return Result.failure(Exception("Task not found"))

            val updatedAt = System.currentTimeMillis()
            val updatedTask = task.copy(
                status = status,
                updatedAt = updatedAt
            )

            // HYBRID PATTERN: Update Room first (instant UI update)
            taskDao.updateTask(updatedTask)

            // Sync to Supabase in background
            try {
                val supabaseResult = supabaseTaskDataSource.updateTaskStatus(taskId, status, updatedAt)
                if (supabaseResult.isFailure) {
                    Log.w(TAG, "Failed to sync task status to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing task status to Supabase (offline mode active)", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task status", e)
            Result.failure(e)
        }
    }

    /**
     * Assign a task to a user
     * Requires ASSIGN_TASKS permission and role validation
     *
     * @param taskId Task ID
     * @param assigneeUserId User ID to assign to
     * @param assignerUserId User ID performing the assignment
     * @return Result indicating success or failure
     */
    suspend fun assignTask(
        taskId: String,
        assigneeUserId: String,
        assignerUserId: String
    ): Result<Unit> {
        return try {
            val task = taskDao.getTaskById(taskId)
                ?: return Result.failure(Exception("Task not found"))

            // Get both members
            val assigner = projectMemberDao.getMemberByProjectAndUser(task.projectId, assignerUserId)
                ?: return Result.failure(SecurityException("You are not a member of this project"))

            val assignee = projectMemberDao.getMemberByProjectAndUser(task.projectId, assigneeUserId)
                ?: return Result.failure(IllegalArgumentException("Assignee is not a member of this project"))

            // Check permission
            val permissionResult = PermissionChecker.hasPermission(assigner, Permission.ASSIGN_TASKS)
            if (permissionResult !is PermissionChecker.PermissionResult.Granted) {
                return Result.failure(
                    PermissionChecker.PermissionDeniedException(
                        permissionResult.getDeniedReason() ?: "Permission denied"
                    )
                )
            }

            // Validate role hierarchy - can only assign to equal or lower roles
            val roleValidation = RoleValidator.canAssignTask(assigner.role, assignee.role)
            if (roleValidation !is RoleValidator.ValidationResult.Success) {
                return Result.failure(
                    SecurityException(roleValidation.getErrorMessage() ?: "Cannot assign to this role")
                )
            }

            val updatedTask = task.copy(
                assignedToId = assigneeUserId,
                assignedToRole = assignee.role, // Store assignee's role
                updatedAt = System.currentTimeMillis()
            )

            // HYBRID PATTERN: Update Room first (instant UI update)
            taskDao.updateTask(updatedTask)

            // Sync to Supabase in background
            try {
                val supabaseResult = supabaseTaskDataSource.updateTask(updatedTask)
                if (supabaseResult.isFailure) {
                    Log.w(TAG, "Failed to sync task assignment to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing task assignment to Supabase (offline mode active)", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning task", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a task
     * Uses hybrid pattern: deletes from Room immediately, then syncs to Supabase
     * @param taskId Task ID
     * @return Result indicating success or failure
     */
    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            // HYBRID PATTERN: Delete from Room first (instant UI update)
            taskDao.deleteTaskById(taskId)

            // Sync to Supabase in background
            try {
                val supabaseResult = supabaseTaskDataSource.deleteTask(taskId)
                if (supabaseResult.isFailure) {
                    Log.w(TAG, "Failed to sync task deletion to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing task deletion to Supabase (offline mode active)", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task", e)
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

    /**
     * Load more tasks from Supabase with pagination
     * Similar to ChatRepository.loadMoreMessages pattern
     * @param projectId Project ID
     * @param before Timestamp cursor (null for initial load)
     * @return Result with list of tasks
     */
    suspend fun loadMoreTasks(
        projectId: String,
        before: Long? = null
    ): Result<List<Task>> {
        return try {
            // Fetch from Supabase
            val supabaseResult = supabaseTaskDataSource.getTasks(projectId, limit = 50, before = before)

            if (supabaseResult.isSuccess) {
                val tasks = supabaseResult.getOrNull() ?: emptyList()

                // Cache in Room for offline access
                if (tasks.isNotEmpty()) {
                    taskDao.insertTasks(tasks)
                }

                Result.success(tasks)
            } else {
                // Fall back to Room cache - get all tasks (no pagination in cache)
                Log.w(TAG, "Failed to load tasks from Supabase, using cache", supabaseResult.exceptionOrNull())
                // Note: This returns empty list since we don't have a getTasksForProject method in DAO
                // Tasks are cached via syncTasksForChatRoom() method
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading more tasks", e)
            Result.failure(e)
        }
    }

    /**
     * Sync tasks from Supabase for a chat room
     * @param chatRoomId Chat room ID
     * @return Result with list of synced tasks
     */
    suspend fun syncTasksForChatRoom(chatRoomId: String): Result<List<Task>> {
        return try {
            val supabaseResult = supabaseTaskDataSource.getTasksForChatRoom(chatRoomId)

            if (supabaseResult.isSuccess) {
                val tasks = supabaseResult.getOrNull() ?: emptyList()

                // Update Room cache
                if (tasks.isNotEmpty()) {
                    taskDao.insertTasks(tasks)
                }

                Log.d(TAG, "Synced ${tasks.size} tasks for chat room: $chatRoomId")
                Result.success(tasks)
            } else {
                Log.w(TAG, "Failed to sync tasks from Supabase", supabaseResult.exceptionOrNull())
                Result.failure(supabaseResult.exceptionOrNull() ?: Exception("Sync failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tasks", e)
            Result.failure(e)
        }
    }

    /**
     * Get a task by ID with Supabase sync
     * First checks Room, then fetches from Supabase if needed
     * @param taskId Task ID
     * @return Result with Task or null
     */
    suspend fun getTaskByIdWithSync(taskId: String): Result<Task?> {
        return try {
            // Try local first
            val localTask = taskDao.getTaskById(taskId)

            // Fetch from Supabase in background
            try {
                val supabaseResult = supabaseTaskDataSource.getTaskById(taskId)
                if (supabaseResult.isSuccess) {
                    val remoteTask = supabaseResult.getOrNull()
                    if (remoteTask != null) {
                        // Update cache
                        taskDao.insertTask(remoteTask)
                        return Result.success(remoteTask)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching task from Supabase, using cache", e)
            }

            Result.success(localTask)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting task by ID", e)
            Result.failure(e)
        }
    }
}