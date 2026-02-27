package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.features.tasks.components.CommitMessageDialog
import com.example.kosmos.features.tasks.components.TaskPickerBottomSheet
import com.example.kosmos.features.tasks.presentation.TaskDetailViewModel
import com.example.kosmos.shared.ui.components.UserPickerDialog
import com.example.kosmos.shared.ui.components.task.TaskFormatUtils
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Wrapper for TaskDetailScreenReact — the unified task management hub.
 *
 * Uses TaskDetailViewModel for all task operations including:
 * - Status changes (via StatusPickerDialog)
 * - Assignment changes (via UserPickerDialog)
 * - Delete (via confirmation dialog)
 * - Commit message dialogs for significant changes
 * - Subtask toggling, comments, time tracking, dependencies
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreenReactWrapper(
    taskId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
    // Keep repository access for flows not yet in ViewModel (time entries, dependencies)
    taskRepository: TaskRepository = hiltViewModel<TaskDetailDataViewModel>().taskRepository,
    projectRepository: ProjectRepository = hiltViewModel<TaskDetailDataViewModel>().projectRepository,
    authRepository: AuthRepository = hiltViewModel<TaskDetailDataViewModel>().authRepository
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Collect ViewModel state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load task via ViewModel
    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    // Handle delete success
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            onBack()
        }
    }

    // Show errors as snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // Local dialog state
    var showManagementSheet by remember { mutableStateOf(false) }
    var showStatusPicker by remember { mutableStateOf(false) }
    var showUserPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddSubtask by remember { mutableStateOf(false) }
    var showTimeTracker by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }
    var showJournalDialog by remember { mutableStateOf(false) }
    var showDependencyPicker by remember { mutableStateOf(false) }

    // Get current user
    val currentUser = authRepository.getCurrentUser()

    // Flows not yet in ViewModel: time entries, dependencies
    val task = uiState.task

    val activities by taskRepository.getRecentActivityForTaskFlow(taskId, limit = 10)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val timeEntries by taskRepository.getTimeEntriesForTaskFlow(taskId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val runningTimer by if (currentUser != null) {
        taskRepository.getRunningTimerForTaskFlow(taskId, currentUser.id)
            .collectAsStateWithLifecycle(initialValue = null)
    } else {
        remember { mutableStateOf(null) }
    }

    val dependencies by taskRepository.getDependenciesForTaskFlow(taskId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val dependentTasks by taskRepository.getDependentTasksFlow(taskId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val dependencyTasks by remember(dependencies) {
        if (dependencies.isEmpty()) flowOf(emptyList())
        else combine(
            dependencies.map { dep -> taskRepository.getTaskByIdFlow(dep.dependsOnTaskId) }
        ) { tasks -> tasks.filterNotNull() }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val dependentTaskObjects by remember(dependentTasks) {
        if (dependentTasks.isEmpty()) flowOf(emptyList())
        else combine(
            dependentTasks.map { dep -> taskRepository.getTaskByIdFlow(dep.taskId) }
        ) { tasks -> tasks.filterNotNull() }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // Tasks in same project for dependency picker (exclude self and already-added deps)
    val existingDepIds = remember(dependencies) { dependencies.map { it.dependsOnTaskId }.toSet() }
    val projectTasksForPicker by if (task != null) {
        taskRepository.getTasksForProjectFlow(task.projectId)
            .collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    val availableForDependency = remember(projectTasksForPicker, taskId, existingDepIds) {
        projectTasksForPicker.filter { it.id != taskId && !existingDepIds.contains(it.id) }
    }

    val project by if (task != null) {
        projectRepository.getProjectFlow(task.projectId)
            .collectAsStateWithLifecycle(initialValue = null)
    } else {
        remember { mutableStateOf(null) }
    }

    // Comment handler
    val onAddComment: (String) -> Unit = { content ->
        viewModel.addComment(content)
    }

    // Subtask toggle handler
    val onSubtaskToggle: (String) -> Unit = { subtaskId ->
        viewModel.toggleSubtask(subtaskId)
    }

    // Map to UI model when data is loaded
    if (task != null) {
        val taskDetailData = TaskDetailData(
            id = task.id,
            title = task.title,
            status = when (task.status) {
                TaskStatus.TODO -> TaskStatusDetail.TODO
                TaskStatus.IN_PROGRESS -> TaskStatusDetail.IN_PROGRESS
                TaskStatus.DONE -> TaskStatusDetail.DONE
                TaskStatus.CANCELLED -> TaskStatusDetail.DONE
            },
            priority = when (task.priority) {
                TaskPriority.LOW -> TaskPriorityDetail.LOW
                TaskPriority.MEDIUM -> TaskPriorityDetail.MEDIUM
                TaskPriority.HIGH -> TaskPriorityDetail.HIGH
                TaskPriority.URGENT -> TaskPriorityDetail.HIGH
            },
            description = task.description ?: "No description",
            dueDate = task.dueDate?.let { formatDate(it) } ?: "No due date",
            assignee = TaskAssignee(
                name = task.assignedToName ?: "Unassigned",
                avatar = task.assignedToName?.firstOrNull()?.toString() ?: "?"
            ),
            projectName = project?.name ?: "Unknown Project",
            createdAt = formatDate(task.createdAt),
            subtasks = uiState.subtasks.map { subtask ->
                Subtask(
                    id = subtask.id,
                    title = subtask.title,
                    completed = subtask.status == TaskStatus.DONE
                )
            },
            timeTracked = formatHours(task.actualHours ?: 0f),
            timeEstimate = formatHours(task.estimatedHours ?: 0f),
            activity = emptyList()
        )

        Box(modifier = Modifier.fillMaxSize()) {
            TaskDetailScreenReact(
                taskId = taskId,
                task = taskDetailData,
                activities = activities,
                comments = task.comments,
                currentUserName = currentUser?.displayName ?: "Unknown",
                currentUserAvatar = currentUser?.displayName?.firstOrNull()?.toString() ?: "?",
                onBack = onBack,
                onEdit = onEdit,
                onMore = { showManagementSheet = true },
                onSubtaskToggle = onSubtaskToggle,
                onAddSubtask = { showAddSubtask = true },
                onAddJournalEntry = { showJournalDialog = true },
                dependencies = dependencyTasks,
                dependentTasks = dependentTaskObjects,
                onRemoveDependency = { depTaskId ->
                    coroutineScope.launch {
                        taskRepository.removeDependency(taskId, depTaskId)
                    }
                },
                onAddDependency = { showDependencyPicker = true },
                onAddComment = onAddComment,
                // New inline action callbacks
                onStatusClick = { showStatusPicker = true },
                onAssigneeClick = { showUserPicker = true },
                onDeleteTask = { showDeleteConfirm = true },
                onStartTimer = {
                    if (currentUser != null) {
                        coroutineScope.launch {
                            taskRepository.startTimer(taskId, task.projectId, currentUser.id)
                        }
                    }
                },
                onStopTimer = {
                    if (runningTimer != null) {
                        coroutineScope.launch {
                            taskRepository.stopTimer(runningTimer!!.id)
                        }
                    }
                },
                isTimerRunning = runningTimer != null
            )

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Journal Entry Dialog
        if (showJournalDialog) {
            CommitMessageDialog(
                isVisible = true,
                changes = emptyList(),
                standaloneMode = true,
                onConfirm = { message ->
                    if (message != null && currentUser != null) {
                        coroutineScope.launch {
                            taskRepository.addJournalEntry(
                                taskId = taskId,
                                projectId = task.projectId,
                                actorId = currentUser.id,
                                message = message
                            )
                        }
                    }
                    showJournalDialog = false
                },
                onDismiss = { showJournalDialog = false }
            )
        }

        // Dependency Picker
        if (showDependencyPicker) {
            TaskPickerBottomSheet(
                tasks = availableForDependency,
                onTaskSelected = { selectedTask ->
                    coroutineScope.launch {
                        taskRepository.addDependency(
                            taskId = taskId,
                            dependsOnTaskId = selectedTask.id,
                            dependencyType = com.example.kosmos.core.models.DependencyType.BLOCKED_BY,
                            createdBy = currentUser?.id ?: ""
                        )
                    }
                    showDependencyPicker = false
                },
                onDismiss = { showDependencyPicker = false }
            )
        }

        // Task Management Bottom Sheet
        if (showManagementSheet) {
            TaskManagementBottomSheet(
                task = task,
                onUpdateStatus = { showStatusPicker = true },
                onAssignUser = { showUserPicker = true },
                onEditDetails = { onEdit() },
                onViewTimeTracking = {
                    showManagementSheet = false
                    showTimeTracker = true
                },
                onDeleteTask = { showDeleteConfirm = true },
                onDismiss = { showManagementSheet = false }
            )
        }

        // Status Picker Dialog
        if (showStatusPicker) {
            StatusPickerDialog(
                currentStatus = task.status,
                onStatusSelected = { newStatus ->
                    viewModel.requestStatusChange(newStatus)
                    showStatusPicker = false
                },
                onDismiss = { showStatusPicker = false }
            )
        }

        // User Picker Dialog
        if (showUserPicker) {
            UserPickerDialog(
                users = uiState.availableUsers,
                title = "Assign Task",
                onUserSelected = { user ->
                    viewModel.assignUser(user)
                    showUserPicker = false
                },
                onDismiss = { showUserPicker = false }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = {
                    Text(
                        text = "Delete Task?",
                        style = TypographyTokens.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ColorTokens.ReactTheme.foreground
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete this task? This action cannot be undone.",
                        style = TypographyTokens.typography.bodyMedium,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTask()
                            showDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorTokens.ReactTheme.destructive
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = ColorTokens.ReactTheme.card
            )
        }

        // Commit Message Dialog (from ViewModel state)
        if (uiState.showCommitDialog) {
            CommitMessageDialog(
                isVisible = true,
                changes = uiState.pendingChanges,
                onConfirm = { commitMessage ->
                    viewModel.confirmWithCommitMessage(commitMessage)
                },
                onDismiss = {
                    viewModel.dismissCommitDialog()
                }
            )
        }

        // Add Subtask Sheet
        if (showAddSubtask) {
            QuickTaskCreationSheetWrapper(
                projectId = task.projectId,
                chatRoomId = task.chatRoomId,
                parentTaskId = taskId,
                onDismiss = { showAddSubtask = false },
                onCreate = { _ -> showAddSubtask = false }
            )
        }

        // Time Tracker Bottom Sheet
        if (showTimeTracker) {
            ModalBottomSheet(
                onDismissRequest = { showTimeTracker = false },
                containerColor = ColorTokens.ReactTheme.card
            ) {
                com.example.kosmos.features.tasks.components.TimeTrackerWidget(
                    task = task,
                    runningTimer = runningTimer,
                    timeEntries = timeEntries,
                    onStartTimer = {
                        if (currentUser != null) {
                            coroutineScope.launch {
                                taskRepository.startTimer(taskId, task.projectId, currentUser.id)
                            }
                        }
                    },
                    onStopTimer = {
                        if (runningTimer != null) {
                            coroutineScope.launch {
                                taskRepository.stopTimer(runningTimer!!.id)
                            }
                        }
                    },
                    onAddManualEntry = { showManualEntry = true },
                    onViewAllEntries = { },
                    onDeleteEntry = { entry ->
                        coroutineScope.launch {
                            taskRepository.deleteTimeEntry(entry.id)
                        }
                    }
                )
            }
        }

        // Manual Time Entry Dialog
        if (showManualEntry) {
            com.example.kosmos.features.tasks.components.AddManualTimeEntryDialog(
                isVisible = true,
                onDismiss = { showManualEntry = false },
                onConfirm = { startTime, endTime, description, _, _ ->
                    if (currentUser != null) {
                        coroutineScope.launch {
                            taskRepository.addManualTimeEntry(
                                taskId = taskId,
                                projectId = task.projectId,
                                userId = currentUser.id,
                                startTime = startTime,
                                endTime = endTime,
                                description = description
                            )
                            showManualEntry = false
                        }
                    }
                }
            )
        }
    } else {
        // Loading indicator
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Status Picker Dialog — harvested from TaskManagementScreen
 */
@Composable
private fun StatusPickerDialog(
    currentStatus: TaskStatus,
    onStatusSelected: (TaskStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Status",
                style = TypographyTokens.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
                TaskStatus.values().forEach { status ->
                    Surface(
                        onClick = { onStatusSelected(status) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (status == currentStatus) {
                            ColorTokens.ReactTheme.primary.copy(alpha = 0.1f)
                        } else {
                            ColorTokens.ReactTheme.card
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(Tokens.Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = TaskFormatUtils.getStatusLabel(status),
                                style = TypographyTokens.typography.bodyMedium,
                                color = TaskFormatUtils.getStatusColor(status)
                            )
                            if (status == currentStatus) {
                                Icon(
                                    imageVector = IconSet.Action.check,
                                    contentDescription = null,
                                    tint = ColorTokens.ReactTheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = ColorTokens.ReactTheme.card
    )
}

/**
 * Helper ViewModel to inject repositories
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class TaskDetailDataViewModel @Inject constructor(
    val taskRepository: TaskRepository,
    val projectRepository: ProjectRepository,
    val authRepository: AuthRepository
) : androidx.lifecycle.ViewModel()

/**
 * Format timestamp to "MMM dd, yyyy" format
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Format hours to human-readable string (e.g., "4h 30m")
 */
private fun formatHours(hours: Float): String {
    if (hours == 0f) return "0h"
    val h = hours.toInt()
    val m = ((hours - h) * 60).toInt()
    return when {
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}
