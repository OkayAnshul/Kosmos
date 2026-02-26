package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.FieldChange
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.core.exceptions.ConflictException
import com.example.kosmos.core.sync.ConflictChoice
import com.example.kosmos.core.sync.ConflictResolution
import com.example.kosmos.core.sync.FieldConflict
import com.example.kosmos.core.sync.TaskConflictResolver
import com.example.kosmos.features.tasks.components.CommitMessageDialog
import com.example.kosmos.features.tasks.components.ConflictResolutionDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Wrapper for TaskEditScreenReact that connects to the backend.
 *
 * This wrapper:
 * - Loads existing task data (if editing)
 * - Loads list of projects and users for dropdowns
 * - Maps domain models to UI models
 * - Handles save/update/delete operations
 * - Maintains the exact React design UI
 */
@Composable
fun TaskEditScreenReactWrapper(
    taskId: String? = null,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    taskRepository: TaskRepository = hiltViewModel<TaskEditDataViewModel>().taskRepository,
    projectRepository: ProjectRepository = hiltViewModel<TaskEditDataViewModel>().projectRepository,
    userRepository: UserRepository = hiltViewModel<TaskEditDataViewModel>().userRepository,
    authRepository: AuthRepository = hiltViewModel<TaskEditDataViewModel>().authRepository
) {
    val isNewTask = taskId == null
    val coroutineScope = rememberCoroutineScope()

    // Snackbar for error/success messages
    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Commit dialog state
    var showCommitDialog by remember { mutableStateOf(false) }
    var pendingChanges by remember { mutableStateOf<List<FieldChange>>(emptyList()) }
    var pendingTaskData by remember { mutableStateOf<Task?>(null) }

    // Conflict resolution state
    val conflictResolver = remember { TaskConflictResolver() }
    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictFields by remember { mutableStateOf<List<FieldConflict>>(emptyList()) }
    var conflictLocalTask by remember { mutableStateOf<Task?>(null) }
    var conflictServerTask by remember { mutableStateOf<Task?>(null) }

    // Show error snackbar when error occurs
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            errorMessage = null
        }
    }

    // Get current user
    val currentUser = authRepository.getCurrentUser()

    // Load existing task (if editing)
    val task by if (taskId != null) {
        taskRepository.getTaskByIdFlow(taskId)
            .collectAsStateWithLifecycle(initialValue = null)
    } else {
        remember { mutableStateOf(null) }
    }

    // Load user's projects for dropdown
    val projects by if (currentUser != null) {
        projectRepository.getUserProjectsFlow(currentUser.id)
            .collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    // Load all users for assignee dropdown
    val users by userRepository.getAllUsersFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Load tasks for parent task dropdown (exclude current task and tasks that already have parents)
    val allTasks by if (currentUser != null) {
        taskRepository.getAllUserTasksFlow(currentUser.id)
            .collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    // Prepare initial form data
    val initialData = if (task != null) {
        TaskEditFormData(
            title = task!!.title,
            description = task!!.description ?: "",
            status = when (task!!.status) {
                TaskStatus.TODO -> TaskStatusEdit.TODO
                TaskStatus.IN_PROGRESS -> TaskStatusEdit.IN_PROGRESS
                TaskStatus.DONE -> TaskStatusEdit.DONE
                TaskStatus.CANCELLED -> TaskStatusEdit.DONE
            },
            priority = when (task!!.priority) {
                TaskPriority.LOW -> TaskPriorityEdit.LOW
                TaskPriority.MEDIUM -> TaskPriorityEdit.MEDIUM
                TaskPriority.HIGH -> TaskPriorityEdit.HIGH
                TaskPriority.URGENT -> TaskPriorityEdit.HIGH
            },
            dueDate = task!!.dueDate?.let { formatDateForInput(it) } ?: "",
            projectId = task!!.projectId,
            projectName = projects.find { it.id == task!!.projectId }?.name ?: "",
            assigneeId = task!!.assignedToId,
            assigneeName = task!!.assignedToName,
            parentTaskId = task!!.parentTaskId,
            parentTaskTitle = allTasks.find { it.id == task!!.parentTaskId }?.title,
            tags = task!!.tags,
            estimatedHours = task!!.estimatedHours
        )
    } else {
        // Default values for new task (use first project if available)
        TaskEditFormData(
            projectId = projects.firstOrNull()?.id ?: "",
            projectName = projects.firstOrNull()?.name ?: ""
        )
    }

    // Prepare project list (id, name pairs)
    val projectList = projects.map { it.id to it.name }

    // Prepare assignee list (id, name pairs)
    val assigneeList = users.map { it.id to it.username }

    // Prepare available tasks for parent task picker (exclude current task and tasks already having parents)
    val availableTasksList = allTasks
        .filter { it.id != taskId && it.parentTaskId == null }
        .map { it.id to it.title }

    // Function to calculate field changes for commit message
    fun calculateFieldChanges(oldTask: Task, newTask: Task): List<FieldChange> {
        val changes = mutableListOf<FieldChange>()

        if (oldTask.title != newTask.title) {
            changes.add(FieldChange("title", oldTask.title, newTask.title))
        }
        if (oldTask.description != newTask.description) {
            changes.add(FieldChange("description", oldTask.description ?: "", newTask.description ?: ""))
        }
        if (oldTask.status != newTask.status) {
            changes.add(FieldChange("status", oldTask.status.name, newTask.status.name))
        }
        if (oldTask.priority != newTask.priority) {
            changes.add(FieldChange("priority", oldTask.priority.name, newTask.priority.name))
        }
        if (oldTask.assignedToId != newTask.assignedToId) {
            changes.add(FieldChange(
                "assignee",
                oldTask.assignedToName ?: "Unassigned",
                newTask.assignedToName ?: "Unassigned"
            ))
        }
        if (oldTask.dueDate != newTask.dueDate) {
            changes.add(FieldChange(
                "dueDate",
                oldTask.dueDate?.let { formatDateForInput(it) } ?: "No date",
                newTask.dueDate?.let { formatDateForInput(it) } ?: "No date"
            ))
        }

        return changes
    }

    // Build FieldConflicts for all fields that differ between local and server task
    fun buildBasicConflicts(localTask: Task, serverTask: Task): List<FieldConflict> {
        val conflicts = mutableListOf<FieldConflict>()
        fun check(field: String, lv: Any?, sv: Any?) {
            if (lv != sv) conflicts.add(FieldConflict(field, lv, sv, localTask.updatedAt, serverTask.updatedAt))
        }
        check("title", localTask.title, serverTask.title)
        check("description", localTask.description, serverTask.description)
        check("status", localTask.status, serverTask.status)
        check("priority", localTask.priority, serverTask.priority)
        check("assignedToId", localTask.assignedToId, serverTask.assignedToId)
        check("dueDate", localTask.dueDate, serverTask.dueDate)
        check("tags", localTask.tags, serverTask.tags)
        check("estimatedHours", localTask.estimatedHours, serverTask.estimatedHours)
        check("actualHours", localTask.actualHours, serverTask.actualHours)
        return conflicts
    }

    // Function to save task with optional commit message
    suspend fun saveTaskWithCommit(taskData: Task, commitMessage: String?) {
        try {
            if (isNewTask) {
                taskRepository.createTask(taskData, currentUser!!.id)
            } else {
                taskRepository.updateTask(taskData, currentUser!!.id, commitMessage)
            }
            onSaveSuccess()
        } catch (e: ConflictException) {
            val localTask = e.localData as? Task ?: taskData
            val serverTask = e.serverData as? Task ?: taskData
            // Build field-level conflicts using resolver; fall back to RequiresUserInput for all changed fields
            val resolution = conflictResolver.resolve(localTask, serverTask, currentUser?.id ?: "")
            conflictFields = when (resolution) {
                is ConflictResolution.RequiresUserInput -> resolution.conflicts
                else -> buildBasicConflicts(localTask, serverTask)
            }
            conflictLocalTask = localTask
            conflictServerTask = serverTask
            showConflictDialog = true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e("TaskEditWrapper", "Failed to save task", e)
            errorMessage = "Failed to save task: ${e.message ?: "Unknown error"}"
        }
    }

    // Handle save operation
    val handleSave: (TaskEditFormData) -> Unit = { formData ->
        if (currentUser != null) {
            val taskData = Task(
                id = taskId ?: java.util.UUID.randomUUID().toString(),
                title = formData.title,
                description = formData.description,
                status = when (formData.status) {
                    TaskStatusEdit.TODO -> TaskStatus.TODO
                    TaskStatusEdit.IN_PROGRESS -> TaskStatus.IN_PROGRESS
                    TaskStatusEdit.DONE -> TaskStatus.DONE
                },
                priority = when (formData.priority) {
                    TaskPriorityEdit.LOW -> TaskPriority.LOW
                    TaskPriorityEdit.MEDIUM -> TaskPriority.MEDIUM
                    TaskPriorityEdit.HIGH -> TaskPriority.HIGH
                },
                projectId = formData.projectId,
                dueDate = if (formData.dueDate.isNotEmpty()) {
                    parseDateFromInput(formData.dueDate)
                } else null,
                assignedToId = formData.assigneeId,
                assignedToName = formData.assigneeName,
                parentTaskId = formData.parentTaskId,
                tags = formData.tags,
                estimatedHours = formData.estimatedHours,
                actualHours = task?.actualHours,
                version = task?.version ?: 1,
                createdById = currentUser.id,
                createdByName = currentUser.username,
                createdAt = task?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            if (!isNewTask && task != null) {
                // Calculate changes for existing task
                val changes = calculateFieldChanges(task!!, taskData)

                // Define significant fields (status, priority, assignee, dueDate)
                val significantChanges = changes.filter { change ->
                    change.field in listOf("status", "priority", "assignee", "dueDate")
                }

                if (significantChanges.isNotEmpty()) {
                    // Show commit dialog for significant changes
                    pendingChanges = changes
                    pendingTaskData = taskData
                    showCommitDialog = true
                } else {
                    // No significant changes or only minor edits, save directly
                    coroutineScope.launch {
                        saveTaskWithCommit(taskData, commitMessage = null)
                    }
                }
            } else {
                // New task - no commit message needed
                coroutineScope.launch {
                    saveTaskWithCommit(taskData, commitMessage = null)
                }
            }
        }
    }

    // Delete confirmation
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val handleDelete: () -> Unit = {
        showDeleteConfirmation = true
    }

    val confirmDelete: () -> Unit = {
        if (taskId != null && currentUser != null) {
            coroutineScope.launch {
                try {
                    taskRepository.deleteTask(taskId, currentUser.id)
                    onBack()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    android.util.Log.e("TaskEditWrapper", "Failed to delete task", e)
                    errorMessage = "Failed to delete task: ${e.message ?: "Unknown error"}"
                }
            }
        }
        showDeleteConfirmation = false
    }

    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { androidx.compose.material3.Text("Delete Task") },
            text = { androidx.compose.material3.Text("Are you sure you want to delete this task? This action cannot be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = confirmDelete) {
                    androidx.compose.material3.Text("Delete", color = com.example.kosmos.shared.ui.designsystem.ColorTokens.Error.light)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteConfirmation = false }) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    // Scaffold wrapper for snackbar support
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        // Guard: Don't render edit form until task data is loaded (prevents blank form race condition)
        if (!isNewTask && task == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
        TaskEditScreenReact(
            taskId = taskId,
            initialData = initialData,
            projects = projectList,
            assignees = assigneeList,
            availableTasks = availableTasksList,
            onBack = onBack,
            onSave = handleSave,
            onDelete = handleDelete
        )

        // Commit Message Dialog
        if (showCommitDialog) {
            CommitMessageDialog(
                isVisible = true,
                changes = pendingChanges,
                onConfirm = { commitMessage ->
                    showCommitDialog = false
                    coroutineScope.launch {
                        saveTaskWithCommit(pendingTaskData!!, commitMessage)
                    }
                },
                onDismiss = {
                    showCommitDialog = false
                    pendingChanges = emptyList()
                    pendingTaskData = null
                }
            )
        }

        // Conflict Resolution Dialog — shown when a version conflict is detected during save
        if (showConflictDialog && conflictFields.isNotEmpty()) {
            ConflictResolutionDialog(
                isVisible = true,
                conflicts = conflictFields,
                onResolve = { choices ->
                    val resolved = conflictResolver.applyUserChoices(
                        conflictLocalTask ?: return@ConflictResolutionDialog,
                        choices
                    )
                    // Force-save: bump version past the server version so it wins
                    val forceSaveTask = resolved.copy(
                        version = (conflictServerTask?.version ?: resolved.version) + 1,
                        updatedAt = System.currentTimeMillis()
                    )
                    showConflictDialog = false
                    conflictFields = emptyList()
                    coroutineScope.launch { saveTaskWithCommit(forceSaveTask, commitMessage = null) }
                },
                onDismiss = {
                    showConflictDialog = false
                    conflictFields = emptyList()
                    conflictLocalTask = null
                    conflictServerTask = null
                }
            )
        }
        } // end else (task loaded or new task)
    }
}

/**
 * Helper ViewModel to inject repositories
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class TaskEditDataViewModel @Inject constructor(
    val taskRepository: TaskRepository,
    val projectRepository: ProjectRepository,
    val userRepository: UserRepository,
    val authRepository: AuthRepository
) : androidx.lifecycle.ViewModel()

/**
 * Format timestamp to "yyyy-MM-dd" format for date input
 */
private fun formatDateForInput(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Parse "yyyy-MM-dd" date string to timestamp
 */
private fun parseDateFromInput(dateString: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.parse(dateString)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        System.currentTimeMillis()
    }
}
