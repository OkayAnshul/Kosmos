package com.example.kosmos.data.repository

import android.util.Log
import com.example.kosmos.core.database.dao.ProjectMemberDao
import com.example.kosmos.core.database.dao.TaskActivityDao
import com.example.kosmos.core.database.dao.TaskDao
import com.example.kosmos.core.database.dao.UserDao
import com.example.kosmos.core.models.ActivityActionType
import com.example.kosmos.core.models.ActivityDescriptionGenerator
import com.example.kosmos.core.models.FieldChange
import com.example.kosmos.core.models.Permission
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.core.validators.PermissionChecker
import com.example.kosmos.core.validators.RoleValidator
import com.example.kosmos.core.exceptions.ConflictException
import com.example.kosmos.data.datasource.SupabaseTaskActivityDataSource
import com.example.kosmos.data.datasource.SupabaseTaskDataSource
import com.example.kosmos.data.sync.SyncRetryHelper
import com.example.kosmos.data.sync.SyncQueueHelper
import com.example.kosmos.core.models.SyncOperation
import com.example.kosmos.core.coroutines.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    private val projectDao: com.example.kosmos.core.database.dao.ProjectDao,
    private val projectMemberDao: ProjectMemberDao,
    private val userDao: UserDao,
    private val taskActivityDao: TaskActivityDao,
    private val supabaseTaskDataSource: SupabaseTaskDataSource,
    private val supabaseTaskActivityDataSource: SupabaseTaskActivityDataSource,
    private val notificationRulesEngine: com.example.kosmos.features.notifications.NotificationRulesEngine,
    private val reminderScheduler: com.example.kosmos.features.notifications.ReminderScheduler,
    private val networkMonitor: com.example.kosmos.shared.utils.NetworkMonitor,  // P0-06 FIX
    private val syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao,  // P0-08 FIX
    private val dispatchers: DispatcherProvider,  // P1-12: Proper threading
    private val fkRetryQueue: com.example.kosmos.data.sync.FKRetryQueue,  // NEW: FK violation retry queue
    private val timeEntryDao: com.example.kosmos.core.database.dao.TimeEntryDao,
    private val taskDependencyDao: com.example.kosmos.core.database.dao.TaskDependencyDao,
    private val supabaseTimeEntryDataSource: com.example.kosmos.data.datasource.SupabaseTimeEntryDataSource,
    private val supabaseDependencyDataSource: com.example.kosmos.data.datasource.SupabaseDependencyDataSource
) {

    /**
     * P0-06 FIX: Expose network connectivity state
     * UI can observe this to show offline banner
     */
    val isOffline: kotlinx.coroutines.flow.StateFlow<Boolean> = networkMonitor.isOffline

    companion object {
        private const val TAG = "TaskRepository"
        // BUG-008 FIX: Use thread-safe DateTimeFormatter instead of SimpleDateFormat
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter
            .ofPattern("MMM dd, yyyy")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }

    /**
     * Format a timestamp to a readable date string (thread-safe)
     * BUG-008 FIX: Uses DateTimeFormatter which is immutable and thread-safe
     */
    private fun formatDate(timestamp: Long): String {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timestamp))
    }

    /**
     * Get all tasks for a chat room (project-scoped to prevent cross-project contamination)
     * @param projectId Project ID
     * @param chatRoomId Chat room ID
     * @return Flow of task list
     */
    fun getTasksForChatRoomFlow(projectId: String, chatRoomId: String): Flow<List<Task>> {
        return taskDao.getTasksForChatRoomFlow(projectId, chatRoomId)
    }

    /**
     * Get tasks by status for a chat room (project-scoped to prevent cross-project contamination)
     * @param projectId Project ID
     * @param chatRoomId Chat room ID
     * @param status Task status to filter by
     * @return Flow of filtered task list
     */
    fun getTasksByStatusFlow(projectId: String, chatRoomId: String, status: TaskStatus): Flow<List<Task>> {
        return taskDao.getTasksByStatusFlow(projectId, chatRoomId, status)
    }

    /**
     * Get all tasks for a project (project-level view)
     * Tasks are independent entities within a project, not nested in chats
     * @param projectId Project ID
     * @return Flow of all project tasks
     */
    fun getTasksForProjectFlow(projectId: String): Flow<List<Task>> {
        return taskDao.getTasksForProjectFlow(projectId)
    }

    /**
     * Get tasks by status for a project
     * @param projectId Project ID
     * @param status Task status to filter by
     * @return Flow of filtered task list
     */
    fun getProjectTasksByStatusFlow(projectId: String, status: TaskStatus): Flow<List<Task>> {
        return taskDao.getProjectTasksByStatusFlow(projectId, status)
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
     * Sync tasks for a user from Supabase to local cache
     * Fetches all active tasks assigned to the user across all their projects
     *
     * CRITICAL: This fixes the bug where tasks are never fetched from Supabase.
     * Call this on app startup, login, or pull-to-refresh.
     *
     * @param userId User ID
     * @return Result indicating success or failure
     */
    @Deprecated(
        message = "Use project-centric sync via InitialSyncManager instead. " +
                  "This only fetches tasks assigned to user, missing other project tasks.",
        replaceWith = ReplaceWith("InitialSyncManager.syncAllData(userId)"),
        level = DeprecationLevel.WARNING
    )
    suspend fun syncUserTasks(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Starting task sync for user: $userId")

            // CRITICAL FIX: Wrap HTTP call in NonCancellable to prevent mid-flight cancellation
            val tasksResult = withContext(NonCancellable) {
                supabaseTaskDataSource.getMyActiveTasks(userId)
            }

            if (tasksResult.isFailure) {
                Log.w(TAG, "Failed to fetch tasks from Supabase", tasksResult.exceptionOrNull())
                return tasksResult.map { }  // Convert to Result<Unit>
            }

            val tasks = tasksResult.getOrNull() ?: emptyList()
            var successCount = 0
            var fkErrorCount = 0
            var cancelledCount = 0

            // Update local cache with FK error handling
            tasks.forEach { task ->
                try {
                    taskDao.insertTask(task)
                    successCount++
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "⚠️ Task insert cancelled for ${task.id}")
                    cancelledCount++
                    // DON'T re-throw - continue with next task
                } catch (e: Exception) {
                    if (com.example.kosmos.data.sync.ForeignKeyErrorHandler.isForeignKeyViolation(e)) {
                        fkErrorCount++
                        // Queue task for retry after users sync completes
                        fkRetryQueue.queueTaskRetry(task)
                        Log.w(TAG, "FK violation for task ${task.id}, queued for retry (assignee: ${task.assignedToId})")
                    } else {
                        throw e
                    }
                }
            }

            if (fkErrorCount > 0 || cancelledCount > 0) {
                Log.w(TAG, "⚠️ Synced $successCount/${tasks.size} tasks ($fkErrorCount FK errors, $cancelledCount cancelled)")
            } else {
                Log.d(TAG, "✅ Synced ${tasks.size} tasks from Supabase")
            }

            Result.success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.w(TAG, "⚠️ Task sync cancelled (partial data saved)")
            Result.success(Unit)  // Return success - partial data is OK
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error in task sync", e)
            Result.failure(e)
        }
    }

    /**
     * Sync all tasks for a specific project from Supabase
     * Useful when entering a project to ensure all tasks are up-to-date
     *
     * INCREMENTAL SYNC: Only fetches tasks modified since last sync (50-90% less data)
     *
     * @param projectId Project ID
     * @param since Optional timestamp (milliseconds) - only fetch tasks updated after this time
     * @return Result indicating success or failure
     */
    suspend fun syncProjectTasks(projectId: String, since: Long? = null): Result<Unit> {
        return try {
            if (since != null) {
                Log.d(TAG, "Starting incremental task sync for project: $projectId (since: $since)")
            } else {
                Log.d(TAG, "Starting full task sync for project: $projectId")
            }

            // Fetch all tasks for the project from Supabase
            val tasksResult = supabaseTaskDataSource.getTasks(
                projectId = projectId,
                limit = 500,  // Reasonable limit for most projects
                before = null,
                since = since  // INCREMENTAL SYNC
            )

            if (tasksResult.isFailure) {
                Log.w(TAG, "Failed to fetch project tasks from Supabase", tasksResult.exceptionOrNull())
                return tasksResult.map { }
            }

            val remoteTasks = tasksResult.getOrNull() ?: emptyList()

            // MERGE remote tasks with local data to prevent overwriting unsynced changes
            // (e.g., comments added locally but not yet synced to Supabase)
            val mergedTasks = remoteTasks.map { remoteTask ->
                val localTask = taskDao.getTaskById(remoteTask.id)
                if (localTask != null) {
                    // Preserve local comments if remote has fewer (local added but not synced yet)
                    val mergedComments = if (localTask.comments.size > remoteTask.comments.size) {
                        localTask.comments
                    } else {
                        remoteTask.comments
                    }
                    // Use remote version for all fields except locally-richer comments
                    // Only overwrite if remote version is newer
                    if (remoteTask.updatedAt >= localTask.updatedAt) {
                        remoteTask.copy(comments = mergedComments)
                    } else {
                        // Local is newer — keep local, but merge any remote fields not in local
                        localTask
                    }
                } else {
                    remoteTask
                }
            }

            // Update local cache with merged data
            taskDao.insertTasks(mergedTasks)

            if (since != null) {
                Log.d(TAG, "✅ Synced ${remoteTasks.size} tasks for project $projectId (incremental)")
            } else {
                Log.d(TAG, "✅ Synced ${remoteTasks.size} tasks for project $projectId (full)")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error syncing project tasks", e)
            Result.failure(e)
        }
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

            // Sync to Supabase (best effort - don't block on failure)
            try {
                val supabaseResult = SyncRetryHelper.retryOnForeignKeyViolation(
                    maxRetries = 3,
                    initialDelayMs = 1000,
                    entityName = "task"
                ) {
                    supabaseTaskDataSource.insertTask(taskWithId)
                }

                if (supabaseResult.isFailure) {
                    val error = supabaseResult.exceptionOrNull()
                    val diagnosticMessage = SyncRetryHelper.getDiagnosticMessage(error, "task")
                    Log.e(TAG, "❌ SUPABASE SYNC FAILED for task")
                    Log.e(TAG, diagnosticMessage, error)
                    // P0-08 FIX: Queue for automatic retry
                    SyncQueueHelper.queueTask(syncQueueDao, taskWithId, SyncOperation.CREATE)
                    Log.d(TAG, "📥 Task queued for retry: $taskId")
                    // Continue anyway - task is saved locally
                } else {
                    Log.d(TAG, "✅ Task synced to Supabase successfully: $taskId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing task to Supabase (possible offline mode)", e)
                // Continue anyway - task is saved locally
            }

            // P0-03 FIX: ALWAYS track activity, even when offline
            // trackActivity() implements offline-first pattern:
            // - Saves to Room immediately (guaranteed to work)
            // - Syncs to Supabase with retry logic (handles FK violations)
            trackActivity(
                task = taskWithId,
                oldTask = null,
                actionType = ActivityActionType.CREATED,
                actorId = creatorId,
                commitMessage = null // No commit message for creation
            )

            // Schedule reminders if task has a due date
            try {
                reminderScheduler.scheduleReminders(taskWithId)
                Log.d(TAG, "✅ Reminders scheduled for new task: ${taskWithId.id}")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to schedule reminders (non-blocking)", e)
            }

            // Update project task count
            projectDao.incrementTaskCount(task.projectId)

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
     * @param actorId User ID performing the update
     * @param commitMessage Optional commit message
     * @return Result indicating success or failure
     */
    suspend fun updateTask(
        task: Task,
        actorId: String,
        commitMessage: String? = null
    ): Result<Unit> {
        return try {
            // H1 FIX: RBAC check for edit permission
            val member = projectMemberDao.getMemberByProjectAndUser(task.projectId, actorId)
                ?: return Result.failure(SecurityException("You are not a member of this project"))

            // Check EDIT_ANY_TASK first, then fall back to EDIT_OWN_TASKS if assignee
            val canEditAny = PermissionChecker.hasPermission(member, Permission.EDIT_ANY_TASK) is PermissionChecker.PermissionResult.Granted
            val canEditOwn = task.assignedToId == actorId &&
                    PermissionChecker.hasPermission(member, Permission.EDIT_OWN_TASKS) is PermissionChecker.PermissionResult.Granted

            if (!canEditAny && !canEditOwn) {
                return Result.failure(
                    PermissionChecker.PermissionDeniedException("You don't have permission to edit this task")
                )
            }

            // Get old task state for change tracking
            val oldTask = taskDao.getTaskById(task.id)

            // P1-11: Check version conflict before updating
            if (oldTask != null && oldTask.version != task.version) {
                // Version mismatch = concurrent edit detected
                throw ConflictException(
                    entityType = "Task",
                    entityId = task.id,
                    localVersion = task.version,
                    serverVersion = oldTask.version,
                    localData = task,
                    serverData = oldTask
                )
            }

            // P1-11: Increment version on successful update
            val updatedTask = task.copy(
                updatedAt = System.currentTimeMillis(),
                version = task.version + 1
            )

            // HYBRID PATTERN: Update Room first (instant UI update)
            taskDao.updateTask(updatedTask)

            // Track activity if there were changes
            if (oldTask != null) {
                val changes = calculateFieldChanges(oldTask, updatedTask)
                if (changes.isNotEmpty()) {
                    trackActivity(
                        task = updatedTask,
                        oldTask = oldTask,
                        actionType = ActivityActionType.UPDATED,
                        actorId = actorId,
                        commitMessage = commitMessage
                    )
                }

                // Reschedule reminders if due date changed
                if (oldTask.dueDate != updatedTask.dueDate) {
                    try {
                        reminderScheduler.rescheduleReminders(updatedTask)
                        Log.d(TAG, "✅ Reminders rescheduled for task: ${updatedTask.id}")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Failed to reschedule reminders (non-blocking)", e)
                    }
                }
            }

            // Sync to Supabase in background
            try {
                val supabaseResult = supabaseTaskDataSource.updateTask(updatedTask)
                if (supabaseResult.isFailure) {
                    val ex = supabaseResult.exceptionOrNull()
                    if (ex is IllegalStateException) throw ex  // Version conflict: propagate to caller
                    Log.w(TAG, "Failed to sync task update to Supabase: ${ex?.message}")
                    // P0-08 FIX: Queue for automatic retry
                    SyncQueueHelper.queueTask(syncQueueDao, updatedTask, SyncOperation.UPDATE)
                    Log.d(TAG, "📥 Task update queued for retry: ${updatedTask.id}")
                }
            } catch (e: IllegalStateException) {
                throw e  // Version conflict: propagate through outer try-catch to ViewModel
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing task update to Supabase (offline mode active)", e)
                // P0-08 FIX: Queue for automatic retry
                SyncQueueHelper.queueTask(syncQueueDao, updatedTask, SyncOperation.UPDATE)
                Log.d(TAG, "📥 Task update queued for retry: ${updatedTask.id}")
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
     * @param actorId User ID performing the status change
     * @param commitMessage Optional commit message
     * @return Result indicating success or failure
     */
    suspend fun updateTaskStatus(
        taskId: String,
        status: TaskStatus,
        actorId: String,
        commitMessage: String? = null
    ): Result<Unit> {
        return try {
            val task = taskDao.getTaskById(taskId)
                ?: return Result.failure(Exception("Task not found"))

            // H1 FIX: RBAC check — assignee can change status via CHANGE_TASK_STATUS,
            // others need EDIT_ANY_TASK
            val isAssignee = task.assignedToId == actorId
            if (!isAssignee) {
                val member = projectMemberDao.getMemberByProjectAndUser(task.projectId, actorId)
                    ?: return Result.failure(SecurityException("You are not a member of this project"))

                val canEditAny = PermissionChecker.hasPermission(member, Permission.EDIT_ANY_TASK) is PermissionChecker.PermissionResult.Granted
                if (!canEditAny) {
                    return Result.failure(
                        PermissionChecker.PermissionDeniedException("You don't have permission to change this task's status")
                    )
                }
            }

            val updatedAt = System.currentTimeMillis()
            val updatedTask = task.copy(
                status = status,
                updatedAt = updatedAt
            )

            // HYBRID PATTERN: Update Room first (instant UI update)
            taskDao.updateTask(updatedTask)

            // Track activity: Status changed
            trackActivity(
                task = updatedTask,
                oldTask = task,
                actionType = ActivityActionType.STATUS_CHANGED,
                actorId = actorId,
                commitMessage = commitMessage
            )

            // Cancel reminders if task is completed
            if (status == TaskStatus.DONE) {
                try {
                    reminderScheduler.cancelReminders(taskId)
                    Log.d(TAG, "✅ Reminders cancelled for completed task: $taskId")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to cancel reminders (non-blocking)", e)
                }
            }

            // Sync to Supabase in background
            try {
                // SCHEMA FIX: Pass version for optimistic locking
                val supabaseResult = supabaseTaskDataSource.updateTaskStatus(
                    taskId = taskId,
                    status = status,
                    updatedAt = updatedAt,
                    currentVersion = task.version  // Add version for conflict detection
                )
                if (supabaseResult.isFailure) {
                    Log.w(TAG, "Failed to sync task status to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
                    // P0-08 FIX: Queue for automatic retry
                    SyncQueueHelper.queueTask(syncQueueDao, updatedTask, SyncOperation.UPDATE)
                    Log.d(TAG, "📥 Task status update queued for retry: $taskId")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing task status to Supabase (offline mode active)", e)
                // P0-08 FIX: Queue for automatic retry
                SyncQueueHelper.queueTask(syncQueueDao, updatedTask, SyncOperation.UPDATE)
                Log.d(TAG, "📥 Task status update queued for retry: $taskId")
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
     * @param commitMessage Optional commit message
     * @return Result indicating success or failure
     */
    suspend fun assignTask(
        taskId: String,
        assigneeUserId: String,
        assignerUserId: String,
        commitMessage: String? = null
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

            // Get assignee's name for display
            val assigneeUser = userDao.getUserById(assigneeUserId)
            val assigneeName = assigneeUser?.displayName ?: assigneeUser?.username

            val updatedTask = task.copy(
                assignedToId = assigneeUserId,
                assignedToName = assigneeName,
                assignedToRole = assignee.role, // Store assignee's role
                updatedAt = System.currentTimeMillis()
            )

            // HYBRID PATTERN: Update Room first (instant UI update)
            taskDao.updateTask(updatedTask)

            // Track activity: Task assigned
            trackActivity(
                task = updatedTask,
                oldTask = task,
                actionType = ActivityActionType.ASSIGNED,
                actorId = assignerUserId,
                commitMessage = commitMessage
            )

            // Sync to Supabase in background
            try {
                val supabaseResult = supabaseTaskDataSource.updateTask(updatedTask)
                if (supabaseResult.isFailure) {
                    Log.w(TAG, "Failed to sync task assignment to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
                    // P0-08 FIX: Queue for automatic retry
                    SyncQueueHelper.queueTask(syncQueueDao, updatedTask, SyncOperation.UPDATE)
                    Log.d(TAG, "📥 Task assignment queued for retry: ${updatedTask.id}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing task assignment to Supabase (offline mode active)", e)
                // P0-08 FIX: Queue for automatic retry
                SyncQueueHelper.queueTask(syncQueueDao, updatedTask, SyncOperation.UPDATE)
                Log.d(TAG, "📥 Task assignment queued for retry: ${updatedTask.id}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning task", e)
            Result.failure(e)
        }
    }

    /**
     * Unassign a task (set assignedToId to null)
     */
    suspend fun unassignTask(taskId: String, actorId: String): Result<Unit> {
        return try {
            val task = taskDao.getTaskById(taskId)
                ?: return Result.failure(Exception("Task not found"))

            val updatedTask = task.copy(
                assignedToId = null,
                assignedToName = null,
                assignedToRole = null,
                updatedAt = System.currentTimeMillis()
            )

            taskDao.updateTask(updatedTask)

            trackActivity(
                task = updatedTask,
                oldTask = task,
                actionType = ActivityActionType.ASSIGNED,
                actorId = actorId,
                commitMessage = "Unassigned task"
            )

            try {
                val supabaseResult = supabaseTaskDataSource.updateTask(updatedTask)
                if (supabaseResult.isFailure) {
                    SyncQueueHelper.queueTask(syncQueueDao, updatedTask, SyncOperation.UPDATE)
                }
            } catch (e: Exception) {
                SyncQueueHelper.queueTask(syncQueueDao, updatedTask, SyncOperation.UPDATE)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unassigning task", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a task
     * Uses hybrid pattern: deletes from Room immediately, then syncs to Supabase
     * @param taskId Task ID
     * @param actorId User ID performing the deletion
     * @return Result indicating success or failure
     */
    suspend fun deleteTask(taskId: String, actorId: String): Result<Unit> {
        return try {
            // Get task before deletion for activity tracking
            val task = taskDao.getTaskById(taskId)

            // H1 FIX: RBAC check for delete permission
            if (task != null) {
                val member = projectMemberDao.getMemberByProjectAndUser(task.projectId, actorId)
                    ?: return Result.failure(SecurityException("You are not a member of this project"))

                val canDeleteAny = PermissionChecker.hasPermission(member, Permission.DELETE_ANY_TASK) is PermissionChecker.PermissionResult.Granted
                val canDeleteOwn = task.createdById == actorId &&
                        PermissionChecker.hasPermission(member, Permission.DELETE_OWN_TASKS) is PermissionChecker.PermissionResult.Granted

                if (!canDeleteAny && !canDeleteOwn) {
                    return Result.failure(
                        PermissionChecker.PermissionDeniedException("You don't have permission to delete this task")
                    )
                }
            }

            // HYBRID PATTERN: Delete from Room first (instant UI update)
            taskDao.deleteTaskById(taskId)

            // Track activity: Task deleted (if task was found)
            if (task != null) {
                trackActivity(
                    task = task,
                    oldTask = null,
                    actionType = ActivityActionType.DELETED,
                    actorId = actorId,
                    commitMessage = null
                )
            }

            // Cancel reminders for deleted task
            try {
                reminderScheduler.cancelReminders(taskId)
                Log.d(TAG, "✅ Reminders cancelled for deleted task: $taskId")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to cancel reminders (non-blocking)", e)
            }

            // Sync to Supabase in background
            try {
                val supabaseResult = supabaseTaskDataSource.deleteTask(taskId)
                if (supabaseResult.isFailure) {
                    Log.w(TAG, "Failed to sync task deletion to Supabase: ${supabaseResult.exceptionOrNull()?.message}")
                    // P0-08 FIX: Queue for automatic retry (if task was found)
                    if (task != null) {
                        SyncQueueHelper.queueTask(syncQueueDao, task, SyncOperation.DELETE)
                        Log.d(TAG, "📥 Task deletion queued for retry: $taskId")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing task deletion to Supabase (offline mode active)", e)
                // P0-08 FIX: Queue for automatic retry (if task was found)
                if (task != null) {
                    SyncQueueHelper.queueTask(syncQueueDao, task, SyncOperation.DELETE)
                    Log.d(TAG, "📥 Task deletion queued for retry: $taskId")
                }
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

    /**
     * Get all tasks assigned to a user across all projects
     * Used for MyTasksScreen cross-project view
     *
     * @param userId User ID
     * @return Flow of all user tasks from all projects
     */
    fun getAllUserTasksFlow(userId: String): Flow<List<Task>> {
        return taskDao.getAllTasksByUserFlow(userId)
    }

    /**
     * Get all subtasks for a parent task
     * Used for TaskDetailScreen subtasks section
     *
     * @param parentTaskId Parent task ID
     * @return Flow of subtasks (tasks with this parentTaskId)
     */
    fun getSubtasksFlow(parentTaskId: String): Flow<List<Task>> {
        return taskDao.getTasksByParentIdFlow(parentTaskId)
    }

    // ============================================================================
    // ACTIVITY TRACKING
    // ============================================================================

    /**
     * Get activity for a task
     * @param taskId Task ID
     * @return Flow of activity list
     */
    fun getActivityForTaskFlow(taskId: String): Flow<List<TaskActivity>> {
        return taskActivityDao.getActivityForTaskFlow(taskId)
    }

    /**
     * Get recent activity for a task (limited)
     * @param taskId Task ID
     * @param limit Number of activities to fetch
     * @return Flow of recent activity
     */
    fun getRecentActivityForTaskFlow(taskId: String, limit: Int = 5): Flow<List<TaskActivity>> {
        return taskActivityDao.getRecentActivityForTaskFlow(taskId, limit)
    }

    /**
     * Get activity for a project
     * @param projectId Project ID
     * @return Flow of activity list
     */
    fun getActivityForProjectFlow(projectId: String): Flow<List<TaskActivity>> {
        return taskActivityDao.getActivityForProjectFlow(projectId)
    }

    /**
     * Track activity for a task change
     *
     * @param task Current task state
     * @param oldTask Previous task state (null for creation)
     * @param actionType Type of action performed
     * @param actorId User ID performing the action
     * @param commitMessage Optional user-provided commit message
     */
    private suspend fun trackActivity(
        task: Task,
        oldTask: Task? = null,
        actionType: ActivityActionType,
        actorId: String,
        commitMessage: String? = null
    ) {
        try {
            // Get actor information
            val actor = userDao.getUserById(actorId)
            val actorName = actor?.displayName ?: actor?.username ?: "Unknown User"

            // Get actor's role in the project
            val member = projectMemberDao.getMemberByProjectAndUser(task.projectId, actorId)
            val actorRole = member?.role?.name

            // Calculate field changes if updating
            val changes = if (oldTask != null) {
                calculateFieldChanges(oldTask, task)
            } else {
                emptyList()
            }

            // Generate auto-description
            val autoDescription = ActivityDescriptionGenerator.generate(
                actionType = actionType,
                changes = changes,
                taskTitle = task.title
            )

            // Create activity record
            val activity = TaskActivity(
                taskId = task.id,
                projectId = task.projectId,
                actorId = actorId,
                actorName = actorName,
                actorRole = actorRole,
                actionType = actionType,
                timestamp = System.currentTimeMillis(),
                changes = changes,
                commitMessage = commitMessage,
                autoDescription = autoDescription
            )

            // OFFLINE-FIRST PATTERN: Save to Room immediately
            // NOTE: No FK on actorId (removed in migration 9→10) so this won't fail
            // even if the user isn't cached locally yet
            taskActivityDao.insertActivity(activity)
            Log.d(TAG, "✅ Activity tracked locally: $actionType for task ${task.id}")

            // Sync to Supabase in background (don't block)
            // Use FK retry to handle race condition where task hasn't synced to Supabase yet
            try {
                val supabaseResult = SyncRetryHelper.retryOnForeignKeyViolation(
                    maxRetries = 3,
                    initialDelayMs = 1000,
                    entityName = "task_activity"
                ) {
                    supabaseTaskActivityDataSource.insertActivity(activity)
                }

                if (supabaseResult.isSuccess) {
                    Log.d(TAG, "✅ Activity synced to Supabase: $actionType")
                } else {
                    Log.w(TAG, "⚠️ Failed to sync activity to Supabase after retries: ${supabaseResult.exceptionOrNull()?.message}")
                    // P0-08 FIX: Queue for automatic retry
                    SyncQueueHelper.queueTaskActivity(syncQueueDao, activity, SyncOperation.CREATE)
                    Log.d(TAG, "📥 Activity queued for retry: ${activity.id}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error syncing activity to Supabase (offline mode active)", e)
                // P0-08 FIX: Queue for automatic retry
                SyncQueueHelper.queueTaskActivity(syncQueueDao, activity, SyncOperation.CREATE)
                Log.d(TAG, "📥 Activity queued for retry: ${activity.id}")
                // Continue anyway - activity is safe locally
            }

            // NOTIFICATION INTEGRATION: Evaluate and send notifications
            // Don't block on notification failures
            try {
                notificationRulesEngine.evaluateAndNotify(activity, task)
                Log.d(TAG, "✅ Notification evaluation complete for activity: $actionType")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error evaluating notifications (non-blocking)", e)
                // Continue anyway - notifications should not break task operations
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error tracking activity for task ${task.id}", e)
            // Don't throw - activity tracking should not break task operations
        }
    }

    /**
     * Calculate field-level changes between old and new task
     *
     * @param oldTask Previous task state
     * @param newTask Current task state
     * @return List of FieldChange objects
     */
    private fun calculateFieldChanges(oldTask: Task, newTask: Task): List<FieldChange> {
        val changes = mutableListOf<FieldChange>()

        // Status change
        if (oldTask.status != newTask.status) {
            changes.add(
                FieldChange(
                    field = "status",
                    fromValue = oldTask.status.name,
                    toValue = newTask.status.name,
                    displayFrom = formatStatus(oldTask.status),
                    displayTo = formatStatus(newTask.status)
                )
            )
        }

        // Priority change
        if (oldTask.priority != newTask.priority) {
            changes.add(
                FieldChange(
                    field = "priority",
                    fromValue = oldTask.priority.name,
                    toValue = newTask.priority.name,
                    displayFrom = formatPriority(oldTask.priority),
                    displayTo = formatPriority(newTask.priority)
                )
            )
        }

        // Assignment change
        if (oldTask.assignedToId != newTask.assignedToId) {
            changes.add(
                FieldChange(
                    field = "assignedTo",
                    fromValue = oldTask.assignedToId,
                    toValue = newTask.assignedToId,
                    displayFrom = oldTask.assignedToName ?: "Unassigned",
                    displayTo = newTask.assignedToName ?: "Unassigned"
                )
            )
        }

        // Title change
        if (oldTask.title != newTask.title) {
            changes.add(
                FieldChange(
                    field = "title",
                    fromValue = oldTask.title,
                    toValue = newTask.title
                )
            )
        }

        // Description change
        if (oldTask.description != newTask.description) {
            changes.add(
                FieldChange(
                    field = "description",
                    fromValue = if (oldTask.description?.isBlank() != false) null else "present",
                    toValue = if (newTask.description?.isBlank() != false) null else "present",
                    displayFrom = if (oldTask.description?.isBlank() != false) "None" else "Present",
                    displayTo = if (newTask.description?.isBlank() != false) "None" else "Present"
                )
            )
        }

        // Due date change
        if (oldTask.dueDate != newTask.dueDate) {
            changes.add(
                FieldChange(
                    field = "dueDate",
                    fromValue = oldTask.dueDate?.toString(),
                    toValue = newTask.dueDate?.toString(),
                    displayFrom = oldTask.dueDate?.let { formatDate(it) },
                    displayTo = newTask.dueDate?.let { formatDate(it) }
                )
            )
        }

        // Tags change
        if (oldTask.tags != newTask.tags) {
            changes.add(
                FieldChange(
                    field = "tags",
                    fromValue = oldTask.tags.joinToString(","),
                    toValue = newTask.tags.joinToString(","),
                    displayFrom = if (oldTask.tags.isEmpty()) "None" else oldTask.tags.joinToString(", "),
                    displayTo = if (newTask.tags.isEmpty()) "None" else newTask.tags.joinToString(", ")
                )
            )
        }

        // Estimated hours change
        if (oldTask.estimatedHours != newTask.estimatedHours) {
            changes.add(
                FieldChange(
                    field = "estimatedHours",
                    fromValue = oldTask.estimatedHours?.toString(),
                    toValue = newTask.estimatedHours?.toString(),
                    displayFrom = oldTask.estimatedHours?.let { "${it}h" },
                    displayTo = newTask.estimatedHours?.let { "${it}h" }
                )
            )
        }

        // Actual hours change
        if (oldTask.actualHours != newTask.actualHours) {
            changes.add(
                FieldChange(
                    field = "actualHours",
                    fromValue = oldTask.actualHours?.toString(),
                    toValue = newTask.actualHours?.toString(),
                    displayFrom = oldTask.actualHours?.let { "${it}h" },
                    displayTo = newTask.actualHours?.let { "${it}h" }
                )
            )
        }

        return changes
    }

    /**
     * Format task status for display
     */
    private fun formatStatus(status: TaskStatus): String {
        return when (status) {
            TaskStatus.TODO -> "To Do"
            TaskStatus.IN_PROGRESS -> "In Progress"
            TaskStatus.DONE -> "Done"
            TaskStatus.CANCELLED -> "Cancelled"
        }
    }

    /**
     * Format task priority for display
     */
    private fun formatPriority(priority: com.example.kosmos.core.models.TaskPriority): String {
        return when (priority) {
            com.example.kosmos.core.models.TaskPriority.LOW -> "Low"
            com.example.kosmos.core.models.TaskPriority.MEDIUM -> "Medium"
            com.example.kosmos.core.models.TaskPriority.HIGH -> "High"
            com.example.kosmos.core.models.TaskPriority.URGENT -> "Urgent"
        }
    }

    /**
     * Get pending tasks count for user (TODO + IN_PROGRESS)
     * Used for bottom navigation badge
     * @param userId User ID
     * @return Flow of pending task count
     */
    fun getPendingTasksCountFlow(userId: String): Flow<Int> {
        return taskDao.getMyActiveTasksFlow(userId).map { tasks ->
            tasks.count { task ->
                task.status == TaskStatus.TODO || task.status == TaskStatus.IN_PROGRESS
            }
        }
    }

    /**
     * Calculate on-time completion rate for a user
     * Compares completedAt vs dueDate for all completed tasks
     * @param userId User ID
     * @return Percentage (0-100) of tasks completed on time, or null if no completed tasks
     */
    suspend fun calculateOnTimeRate(userId: String): Int? {
        return try {
            val completedTasks = taskDao.getCompletedTasksByUser(userId)

            if (completedTasks.isEmpty()) {
                return null // No data yet
            }

            // Count tasks that were completed on or before the due date
            val onTimeTasks = completedTasks.count { task ->
                val dueDate = task.dueDate
                val completedAt = task.updatedAt // Using updatedAt as completion timestamp

                // If task has no due date, consider it "on time"
                // If completed at or before due date, it's on time
                dueDate == null || completedAt <= dueDate
            }

            // Calculate percentage
            val rate = (onTimeTasks * 100) / completedTasks.size
            rate
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating on-time rate for user $userId", e)
            null
        }
    }

    /**
     * Search tasks by title, description, or tags for a specific user
     * Searches local cache first, falls back to Supabase if online
     * @param userId User ID
     * @param query Search query
     * @return Flow of matching tasks
     */
    fun searchTasksByUser(userId: String, query: String): Flow<List<Task>> {
        // If query is blank, return all user tasks
        if (query.isBlank()) {
            return taskDao.getAllTasksByUserFlow(userId)
        }

        // Return local search results
        // Room will reactively update as tasks change
        return taskDao.searchTasksByUser(userId, query)
    }

    /**
     * Search tasks across a project (not filtered by user)
     * Searches local cache first, falls back to Supabase if online
     * @param projectId Project ID
     * @param query Search query
     * @return Flow of matching tasks
     */
    fun searchTasksByProject(projectId: String, query: String): Flow<List<Task>> {
        // If query is blank, return all project tasks
        if (query.isBlank()) {
            return taskDao.getTasksForProjectFlow(projectId)
        }

        // Return local search results
        // Room will reactively update as tasks change
        return taskDao.searchTasksByProject(projectId, query)
    }

    /**
     * P1-05 FIX: Add comment to a task
     * Uses offline-first pattern: updates Room immediately, syncs to Supabase
     *
     * @param taskId Task ID
     * @param authorId Comment author's user ID
     * @param authorName Comment author's display name
     * @param content Comment content
     * @return Result with success or error
     */
    suspend fun addComment(
        taskId: String,
        authorId: String,
        authorName: String,
        content: String
    ): Result<Unit> {
        return try {
            // Step 1: Get current task from Room
            val task = taskDao.getTaskById(taskId)
                ?: return Result.failure(Exception("Task not found"))

            // Step 2: Create new comment
            val newComment = com.example.kosmos.core.models.TaskComment(
                id = java.util.UUID.randomUUID().toString(),
                authorId = authorId,
                authorName = authorName,
                content = content,
                timestamp = System.currentTimeMillis()
            )

            // Step 3: Add comment to task's comment list
            val updatedTask = task.copy(
                comments = task.comments + newComment
            )

            // Step 4: Update Room immediately (offline-first)
            taskDao.updateTask(updatedTask)
            Log.d(TAG, "✅ Comment added to local cache: ${newComment.id}")

            // Step 5: Sync to Supabase (background, won't block)
            val supabaseResult = supabaseTaskDataSource.updateTask(updatedTask)

            if (supabaseResult.isFailure) {
                val error = supabaseResult.exceptionOrNull()
                val diagnosticMessage = SyncRetryHelper.getDiagnosticMessage(error, "task comment")
                Log.e(TAG, "❌ SUPABASE SYNC FAILED for comment")
                Log.e(TAG, diagnosticMessage, error)

                // P1-05 FIX: Queue for automatic retry
                SyncQueueHelper.queueTask(syncQueueDao, updatedTask, SyncOperation.UPDATE)
                Log.d(TAG, "📥 Task with comment queued for retry: $taskId")
            } else {
                Log.d(TAG, "✅ Comment synced to Supabase: ${newComment.id}")
            }

            // Step 6: Track activity
            trackActivity(
                task = updatedTask,
                oldTask = task,
                actionType = ActivityActionType.COMMENT_ADDED,
                actorId = authorId,
                commitMessage = "$authorName added a comment"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add comment to task $taskId", e)
            Result.failure(e)
        }
    }

    /**
     * Add a standalone journal entry (not tied to a field change)
     */
    suspend fun addJournalEntry(taskId: String, projectId: String, actorId: String, message: String): Result<Unit> {
        return try {
            val task = taskDao.getTaskById(taskId)
                ?: return Result.failure(Exception("Task not found"))

            trackActivity(
                task = task,
                oldTask = null,
                actionType = ActivityActionType.JOURNAL_ENTRY,
                actorId = actorId,
                commitMessage = message
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add journal entry for task $taskId", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // TIME TRACKING
    // ========================================================================

    fun getTimeEntriesForTaskFlow(taskId: String): Flow<List<com.example.kosmos.core.models.TimeEntry>> =
        timeEntryDao.getEntriesForTaskFlow(taskId)

    fun getRunningTimerForTaskFlow(taskId: String, userId: String): Flow<com.example.kosmos.core.models.TimeEntry?> =
        timeEntryDao.getRunningTimerForTaskFlow(taskId, userId)

    suspend fun startTimer(taskId: String, projectId: String, userId: String): Result<com.example.kosmos.core.models.TimeEntry> {
        return try {
            val entry = com.example.kosmos.core.models.TimeEntry.createTimer(
                taskId = taskId,
                projectId = projectId,
                userId = userId
            )
            timeEntryDao.insertEntry(entry)
            // Sync to Supabase
            try {
                supabaseTimeEntryDataSource.insertTimeEntry(entry)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync startTimer to Supabase, queuing for retry", e)
                SyncQueueHelper.queueTimeEntry(syncQueueDao, entry, SyncOperation.CREATE)
            }
            Result.success(entry)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start timer for task $taskId", e)
            Result.failure(e)
        }
    }

    suspend fun stopTimer(entryId: String): Result<com.example.kosmos.core.models.TimeEntry> {
        return try {
            val entry = timeEntryDao.getEntryById(entryId)
                ?: return Result.failure(Exception("Time entry not found"))
            val stopped = entry.stop()
            timeEntryDao.updateEntry(stopped)
            try {
                supabaseTimeEntryDataSource.updateTimeEntry(stopped)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync stopTimer to Supabase, queuing for retry", e)
                SyncQueueHelper.queueTimeEntry(syncQueueDao, stopped, SyncOperation.UPDATE)
            }

            // Update task actual hours
            val totalSeconds = timeEntryDao.getTotalTimeForTask(stopped.taskId)
            val totalHours = totalSeconds / 3600f
            val task = taskDao.getTaskById(stopped.taskId)
            if (task != null) {
                taskDao.insertTask(task.copy(actualHours = totalHours, updatedAt = System.currentTimeMillis()))
            }

            Result.success(stopped)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop timer $entryId", e)
            Result.failure(e)
        }
    }

    suspend fun addManualTimeEntry(
        taskId: String,
        projectId: String,
        userId: String,
        startTime: Long,
        endTime: Long,
        description: String? = null
    ): Result<com.example.kosmos.core.models.TimeEntry> {
        return try {
            val entry = com.example.kosmos.core.models.TimeEntry.createManualEntry(
                taskId = taskId,
                projectId = projectId,
                userId = userId,
                startTime = startTime,
                endTime = endTime,
                description = description
            )
            timeEntryDao.insertEntry(entry)
            try {
                supabaseTimeEntryDataSource.insertTimeEntry(entry)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync addManualTimeEntry to Supabase, queuing for retry", e)
                SyncQueueHelper.queueTimeEntry(syncQueueDao, entry, SyncOperation.CREATE)
            }

            // Update task actual hours
            val totalSeconds = timeEntryDao.getTotalTimeForTask(taskId)
            val totalHours = totalSeconds / 3600f
            val task = taskDao.getTaskById(taskId)
            if (task != null) {
                taskDao.insertTask(task.copy(actualHours = totalHours, updatedAt = System.currentTimeMillis()))
            }

            Result.success(entry)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add manual time entry for task $taskId", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTimeEntry(entryId: String): Result<Unit> {
        return try {
            val entry = timeEntryDao.getEntryById(entryId)
            if (entry != null) {
                timeEntryDao.deleteEntryById(entryId)
                try {
                    supabaseTimeEntryDataSource.deleteTimeEntry(entryId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync deleteTimeEntry to Supabase, queuing for retry", e)
                    SyncQueueHelper.queueTimeEntry(syncQueueDao, entry, SyncOperation.DELETE)
                }
                // Recalculate task actual hours
                val totalSeconds = timeEntryDao.getTotalTimeForTask(entry.taskId)
                val totalHours = totalSeconds / 3600f
                val task = taskDao.getTaskById(entry.taskId)
                if (task != null) {
                    taskDao.insertTask(task.copy(actualHours = totalHours, updatedAt = System.currentTimeMillis()))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete time entry $entryId", e)
            Result.failure(e)
        }
    }

    /**
     * Sync time entries for a project from Supabase to local Room cache.
     * Bug M fix: Called by InitialSyncManager so time entries are available on fresh installs.
     *
     * @param projectId Project ID
     * @return Result indicating success or failure
     */
    suspend fun syncTimeEntriesForProject(projectId: String): Result<Unit> {
        return try {
            val result = supabaseTimeEntryDataSource.getEntriesForProject(projectId)
            if (result.isFailure) {
                Log.w(TAG, "Failed to fetch time entries for project $projectId from Supabase", result.exceptionOrNull())
                return result.map { }
            }
            val entries = result.getOrNull() ?: emptyList()
            entries.forEach { entry ->
                try {
                    timeEntryDao.insertEntry(entry)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to cache time entry ${entry.id}", e)
                }
            }
            Log.d(TAG, "✅ Synced ${entries.size} time entries for project $projectId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync time entries for project $projectId", e)
            Result.failure(e)
        }
    }

    // ========================================================================
    // TASK DEPENDENCIES
    // ========================================================================

    fun getDependenciesForTaskFlow(taskId: String): Flow<List<com.example.kosmos.core.models.TaskDependency>> =
        taskDependencyDao.getDependenciesForTaskFlow(taskId)

    fun getDependentTasksFlow(taskId: String): Flow<List<com.example.kosmos.core.models.TaskDependency>> =
        taskDependencyDao.getDependentTasksFlow(taskId)

    suspend fun addDependency(
        taskId: String,
        dependsOnTaskId: String,
        dependencyType: com.example.kosmos.core.models.DependencyType,
        createdBy: String
    ): Result<Unit> {
        return try {
            // Check for circular dependency
            val chain = taskDependencyDao.getDependencyChain(dependsOnTaskId)
            if (taskId in chain || dependsOnTaskId == taskId) {
                return Result.failure(Exception("Circular dependency detected"))
            }

            val dependency = com.example.kosmos.core.models.TaskDependency(
                taskId = taskId,
                dependsOnTaskId = dependsOnTaskId,
                dependencyType = dependencyType,
                createdBy = createdBy
            )
            taskDependencyDao.insertDependency(dependency)
            try { supabaseDependencyDataSource.insertDependency(dependency) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add dependency for task $taskId", e)
            Result.failure(e)
        }
    }

    suspend fun removeDependency(taskId: String, dependsOnTaskId: String): Result<Unit> {
        return try {
            taskDependencyDao.deleteDependencyBetweenTasks(taskId, dependsOnTaskId)
            try { supabaseDependencyDataSource.deleteDependencyBetweenTasks(taskId, dependsOnTaskId) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove dependency", e)
            Result.failure(e)
        }
    }

}