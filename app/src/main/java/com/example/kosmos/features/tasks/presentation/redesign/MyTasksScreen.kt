package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kosmos.features.tasks.components.*
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.layouts.ListState
import com.example.kosmos.shared.ui.layouts.RefreshableStatefulList

/**
 * My Tasks Screen - Cross-Project Task Hub
 *
 * Features:
 * - View all tasks across projects
 * - Switch between list and board view
 * - Filter by status, priority, project
 * - Sort by due date, priority, created date
 * - Pull-to-refresh
 * - Quick actions (swipe, long-press)
 * - Task creation
 *
 * Power user features:
 * - Swipe to complete/delete
 * - Quick filters
 * - Keyboard shortcuts (future)
 * - Batch operations (future)
 */

/**
 * View Mode
 */
enum class TaskViewMode {
    LIST, BOARD
}

/**
 * Sort Option
 */
enum class TaskSortOption {
    DUE_DATE, PRIORITY, CREATED, UPDATED
}

/**
 * My Tasks Screen
 *
 * @param tasksState Tasks state (loading, success, error)
 * @param viewMode Current view mode (list or board)
 * @param onViewModeChange View mode change handler
 * @param selectedStatus Selected status filter
 * @param selectedPriority Selected priority filter
 * @param sortOption Current sort option
 * @param onStatusFilterChange Status filter change handler
 * @param onPriorityFilterChange Priority filter change handler
 * @param onSortChange Sort option change handler
 * @param onTaskClick Task click handler
 * @param onTaskStatusChange Task status change handler
 * @param onTaskEdit Task edit handler
 * @param onTaskDelete Task delete handler
 * @param onCreateTask Create task handler
 * @param onRefresh Refresh handler
 * @param isRefreshing Whether refreshing
 * @param onBackClick Back navigation handler
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksScreen(
    tasksState: ListState<TaskItem>,
    viewMode: TaskViewMode,
    onViewModeChange: (TaskViewMode) -> Unit,
    selectedStatus: TaskStatus?,
    selectedPriority: TaskPriority?,
    sortOption: TaskSortOption,
    onStatusFilterChange: (TaskStatus?) -> Unit,
    onPriorityFilterChange: (TaskPriority?) -> Unit,
    onSortChange: (TaskSortOption) -> Unit,
    onTaskClick: (String) -> Unit,
    onTaskStatusChange: (String, TaskStatus) -> Unit,
    onTaskEdit: (String) -> Unit,
    onTaskDelete: (String) -> Unit,
    onCreateTask: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean = false,
    onBackClick: () -> Unit
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Tasks") },
                navigationIcon = {
                    IconButtonStandard(
                        icon = IconSet.Navigation.back,
                        onClick = onBackClick,
                        contentDescription = "Back"
                    )
                },
                actions = {
                    // View mode toggle
                    IconButtonStandard(
                        icon = if (viewMode == TaskViewMode.LIST)
                            IconSet.Task.board
                        else
                            IconSet.Task.list,
                        onClick = {
                            onViewModeChange(
                                if (viewMode == TaskViewMode.LIST)
                                    TaskViewMode.BOARD
                                else
                                    TaskViewMode.LIST
                            )
                        },
                        contentDescription = "Toggle view"
                    )

                    // Filter button
                    IconButtonStandard(
                        icon = IconSet.Action.filter,
                        onClick = { showFilterSheet = true },
                        contentDescription = "Filter"
                    )

                    // Sort menu
                    Box {
                        IconButtonStandard(
                            icon = IconSet.Action.sort,
                            onClick = { showSortMenu = true },
                            contentDescription = "Sort"
                        )

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            TaskSortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option.name.replace('_', ' ')
                                                .lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                        )
                                    },
                                    onClick = {
                                        onSortChange(option)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOption == option) {
                                            Icon(IconSet.Status.checkmark, null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FABStandard(
                icon = IconSet.Action.add,
                onClick = onCreateTask,
                contentDescription = "Create task"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Active filters indicator
            if (selectedStatus != null || selectedPriority != null) {
                ActiveFiltersChip(
                    statusFilter = selectedStatus,
                    priorityFilter = selectedPriority,
                    onClearFilters = {
                        onStatusFilterChange(null)
                        onPriorityFilterChange(null)
                    },
                    modifier = Modifier.padding(horizontal = Tokens.Spacing.md)
                )
            }

            // Content - Animated transition between list and board
            AnimatedContent(
                targetState = viewMode,
                label = "view_mode_transition"
            ) { mode ->
                when (mode) {
                    TaskViewMode.LIST -> {
                        TaskListView(
                            tasksState = tasksState,
                            onTaskClick = onTaskClick,
                            onTaskStatusChange = onTaskStatusChange,
                            onTaskEdit = onTaskEdit,
                            onTaskDelete = onTaskDelete,
                            onRefresh = onRefresh,
                            isRefreshing = isRefreshing,
                            onCreateTask = onCreateTask
                        )
                    }
                    TaskViewMode.BOARD -> {
                        TaskBoardView(
                            tasksState = tasksState,
                            onTaskClick = onTaskClick,
                            onTaskStatusChange = onTaskStatusChange,
                            onRefresh = onRefresh,
                            isRefreshing = isRefreshing
                        )
                    }
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                Text(
                    text = "Filter Tasks",
                    style = MaterialTheme.typography.titleLarge
                )

                TaskFilterChips(
                    selectedStatus = selectedStatus,
                    selectedPriority = selectedPriority,
                    onStatusSelected = onStatusFilterChange,
                    onPrioritySelected = onPriorityFilterChange
                )

                Spacer(modifier = Modifier.height(Tokens.Spacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                ) {
                    SecondaryButton(
                        text = "Clear All",
                        onClick = {
                            onStatusFilterChange(null)
                            onPriorityFilterChange(null)
                            showFilterSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Apply",
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Tokens.Spacing.md))
            }
        }
    }
}

/**
 * Task List View
 */
@Composable
private fun TaskListView(
    tasksState: ListState<TaskItem>,
    onTaskClick: (String) -> Unit,
    onTaskStatusChange: (String, TaskStatus) -> Unit,
    onTaskEdit: (String) -> Unit,
    onTaskDelete: (String) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onCreateTask: () -> Unit
) {
    RefreshableStatefulList(
        state = tasksState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        emptyTitle = "No tasks yet",
        emptyMessage = "Create your first task to get started",
        emptyActionLabel = "Create Task",
        onEmptyAction = onCreateTask,
        errorTitle = "Failed to load tasks",
        onRetry = onRefresh
    ) { tasks ->
        items(
            items = tasks,
            key = { it.id }
        ) { task ->
            EnhancedTaskCard(
                task = task,
                onClick = { onTaskClick(task.id) },
                onStatusChange = { newStatus ->
                    onTaskStatusChange(task.id, newStatus)
                },
                onDelete = { onTaskDelete(task.id) },
                onEdit = { onTaskEdit(task.id) },
                showProject = true,
                modifier = Modifier.padding(
                    horizontal = Tokens.Spacing.md,
                    vertical = Tokens.Spacing.xs
                )
            )
        }

        item {
            Spacer(modifier = Modifier.height(Tokens.Spacing.xxl))
        }
    }
}

/**
 * Task Board View (Kanban)
 */
@Composable
private fun TaskBoardView(
    tasksState: ListState<TaskItem>,
    onTaskClick: (String) -> Unit,
    onTaskStatusChange: (String, TaskStatus) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean
) {
    when (tasksState) {
        is ListState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        }
        is ListState.Success -> {
            val tasks = tasksState.data

            // Group tasks by status
            val tasksByStatus = tasks.groupBy { it.status }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Tokens.Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                // TODO column
                TaskColumn(
                    title = "To Do",
                    status = TaskStatus.TODO,
                    tasks = tasksByStatus[TaskStatus.TODO] ?: emptyList(),
                    onTaskClick = onTaskClick,
                    modifier = Modifier.weight(1f)
                )

                // IN_PROGRESS column
                TaskColumn(
                    title = "In Progress",
                    status = TaskStatus.IN_PROGRESS,
                    tasks = tasksByStatus[TaskStatus.IN_PROGRESS] ?: emptyList(),
                    onTaskClick = onTaskClick,
                    modifier = Modifier.weight(1f)
                )

                // DONE column
                TaskColumn(
                    title = "Done",
                    status = TaskStatus.DONE,
                    tasks = tasksByStatus[TaskStatus.DONE] ?: emptyList(),
                    onTaskClick = onTaskClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        is ListState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ErrorState(
                    title = "Failed to load tasks",
                    message = tasksState.message,
                    onRetry = onRefresh
                )
            }
        }
    }
}

/**
 * Task Column (for Board View)
 */
@Composable
private fun TaskColumn(
    title: String,
    status: TaskStatus,
    tasks: List<TaskItem>,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
    ) {
        // Column header
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Badge {
                    Text(tasks.size.toString())
                }
            }
        }

        // Tasks
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
        ) {
            items(
                items = tasks,
                key = { it.id }
            ) { task ->
                BoardTaskCard(
                    task = task,
                    onClick = { onTaskClick(task.id) }
                )
            }
        }
    }
}

/**
 * Active Filters Chip
 */
@Composable
private fun ActiveFiltersChip(
    statusFilter: TaskStatus?,
    priorityFilter: TaskPriority?,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Tokens.Spacing.xs),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = IconSet.Action.filter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Tokens.Size.iconSmall)
                )

                val filterText = buildString {
                    if (statusFilter != null) {
                        append(statusFilter.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
                    }
                    if (priorityFilter != null) {
                        if (statusFilter != null) append(" • ")
                        append(priorityFilter.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }

                Text(
                    text = filterText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            IconButtonStandard(
                icon = IconSet.Navigation.close,
                onClick = onClearFilters,
                contentDescription = "Clear filters"
            )
        }
    }
}
