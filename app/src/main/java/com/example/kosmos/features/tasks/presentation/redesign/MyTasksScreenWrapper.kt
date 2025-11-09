package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.tasks.presentation.TaskViewModel
import com.example.kosmos.shared.ui.layouts.ListState
import com.example.kosmos.shared.ui.mappers.StateMapper
import com.example.kosmos.shared.ui.mappers.TaskDataMapper
import com.example.kosmos.shared.ui.mappers.TaskDataMapper.toTaskItem
import com.example.kosmos.shared.ui.mappers.TaskDataMapper.toUIStatus
import com.example.kosmos.shared.ui.mappers.TaskDataMapper.toUIPriority
import com.example.kosmos.shared.ui.mappers.TaskDataMapper.toDomainStatus
import com.example.kosmos.features.tasks.components.TaskStatus
import com.example.kosmos.features.tasks.components.TaskPriority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.kosmos.shared.ui.mappers.TaskSortOption as MapperTaskSortOption

/**
 * Wrapper composable that connects MyTasksScreen to TaskViewModel
 * Handles data mapping and state transformations
 *
 * Note: This is a cross-project task view, so we need to aggregate tasks from all projects
 */
@Composable
fun MyTasksScreenWrapper(
    onTaskClick: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel(),
    projectViewModel: com.example.kosmos.features.project.presentation.ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val projectUiState by projectViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // Track view mode, filters, and sort option
    var viewMode by remember { mutableStateOf(TaskViewMode.LIST) }
    var selectedStatus by remember { mutableStateOf<TaskStatus?>(null) }
    var selectedPriority by remember { mutableStateOf<TaskPriority?>(null) }
    var sortOption by remember { mutableStateOf(TaskSortOption.DUE_DATE) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showEditTaskDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<String?>(null) }

    // Load all user tasks on first composition
    LaunchedEffect(Unit) {
        viewModel.loadAllUserTasks()
    }

    // Convert domain tasks to UI tasks with project name lookup
    val taskItems = remember(uiState.tasks, projectUiState.projects) {
        uiState.tasks.map { task ->
            val projectName = projectUiState.projects.find { it.id == task.projectId }?.name
            task.toTaskItem(
                projectName = projectName
            )
        }
    }

    // Apply filters
    val filteredTasks = remember(taskItems, selectedStatus, selectedPriority) {
        TaskDataMapper.filterTasks(taskItems, selectedStatus, selectedPriority)
    }

    // Apply sorting
    val sortedTasks = remember(filteredTasks, sortOption) {
        val mapperSort = when (sortOption) {
            TaskSortOption.DUE_DATE -> MapperTaskSortOption.DUE_DATE
            TaskSortOption.PRIORITY -> MapperTaskSortOption.PRIORITY
            TaskSortOption.CREATED -> MapperTaskSortOption.CREATED
            TaskSortOption.UPDATED -> MapperTaskSortOption.UPDATED
        }
        TaskDataMapper.sortTasks(filteredTasks, mapperSort)
    }

    // Convert to ListState
    val tasksState = remember(uiState.isLoading, sortedTasks, uiState.error) {
        StateMapper.toListState(
            isLoading = uiState.isLoading,
            data = sortedTasks,
            error = uiState.error
        )
    }

    MyTasksScreen(
        tasksState = tasksState,
        viewMode = viewMode,
        onViewModeChange = { newMode ->
            viewMode = newMode
        },
        selectedStatus = selectedStatus,
        selectedPriority = selectedPriority,
        sortOption = sortOption,
        onStatusFilterChange = { status ->
            selectedStatus = status
        },
        onPriorityFilterChange = { priority ->
            selectedPriority = priority
        },
        onSortChange = { newSortOption ->
            sortOption = newSortOption
        },
        onTaskClick = onTaskClick,
        onTaskStatusChange = { taskId, newStatus ->
            // Find the task and update it
            val task = uiState.tasks.find { it.id == taskId }
            if (task != null) {
                viewModel.updateTaskStatus(taskId, newStatus.toDomainStatus())
            }
        },
        onTaskEdit = { taskId ->
            val task = uiState.tasks.find { it.id == taskId }
            if (task != null) {
                viewModel.showEditTaskDialog(task)
                showEditTaskDialog = true
            }
        },
        onTaskDelete = { taskId ->
            taskToDelete = taskId
            showDeleteConfirmation = true
        },
        onCreateTask = {
            showCreateTaskDialog = true
        },
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                viewModel.loadAllUserTasks()
                delay(500) // Brief delay for UX
                isRefreshing = false
            }
        },
        isRefreshing = isRefreshing,
        onBackClick = onBackClick
    )

    // Create Task Dialog - Show project selection first
    if (showCreateTaskDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTaskDialog = false },
            title = { Text("Create Task") },
            text = { Text("Task creation from My Tasks view requires selecting a project. Please create tasks from within a specific project instead.") },
            confirmButton = {
                TextButton(onClick = { showCreateTaskDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Edit Task Dialog - Placeholder for now
    if (showEditTaskDialog && uiState.editingTask != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.hideEditTaskDialog()
                showEditTaskDialog = false
            },
            title = { Text("Edit Task") },
            text = { Text("Task editing UI will be added in the next phase. For now, you can change task status by clicking on the task card.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.hideEditTaskDialog()
                    showEditTaskDialog = false
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation && taskToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                taskToDelete = null
            },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskToDelete?.let { viewModel.deleteTask(it) }
                        showDeleteConfirmation = false
                        taskToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        taskToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
