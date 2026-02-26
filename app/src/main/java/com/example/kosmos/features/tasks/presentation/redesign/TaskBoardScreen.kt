package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.features.tasks.presentation.TaskUiState
import com.example.kosmos.features.users.presentation.components.UserAvatar
import com.example.kosmos.shared.ui.designsystem.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Stitch Design TaskBoard Screen (Kanban Layout)
 *
 * Features:
 * - Horizontal scrolling columns (TO DO / IN PROGRESS / DONE)
 * - Navy backgrounds matching Stitch reference
 * - Priority badges (colored)
 * - Search bar at top
 * - My Tasks / All Tasks tabs
 * - Offline mode banner (minimal)
 * - Assignee avatars
 * - Due dates with calendar icons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBoardScreen(
    projectId: String,
    projectName: String,
    teamInfo: String,
    currentUserDisplayName: String,
    currentUserPhotoUrl: String?,
    chatRoomId: String? = null,
    uiState: TaskUiState,
    isOffline: Boolean = false,
    onTaskClick: (Task) -> Unit,
    onCreateTask: () -> Unit,
    onCreateTaskWithStatus: (TaskStatus) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onNavigateBack: () -> Unit,
    onTaskStatusChange: (Task, TaskStatus) -> Unit = { _, _ -> },
    onEditTask: (Task) -> Unit = {},
    onDeleteTask: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL_TASKS) }

    // Filter tasks by search query
    val filteredTasks = remember(uiState.tasks, searchQuery, selectedFilter) {
        uiState.tasks.filter { task ->
            val matchesSearch = if (searchQuery.isBlank()) true
            else task.title.contains(searchQuery, ignoreCase = true) ||
                    (task.description?.contains(searchQuery, ignoreCase = true) == true)

            val matchesFilter = when (selectedFilter) {
                TaskFilter.ALL_TASKS -> true
                TaskFilter.MY_TASKS -> task.assignedToId == uiState.currentUserId
            }

            matchesSearch && matchesFilter
        }
    }

    // Group tasks by status for Kanban columns
    val todoTasks = filteredTasks.filter { it.status == TaskStatus.TODO }
    val inProgressTasks = filteredTasks.filter { it.status == TaskStatus.IN_PROGRESS }
    val doneTasks = filteredTasks.filter { it.status == TaskStatus.DONE }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            projectName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ColorTokens.ReactTheme.foreground
                        )
                        Text(
                            teamInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = ColorTokens.ReactTheme.foreground
                        )
                    }
                },
                actions = {
                    // Current user avatar
                    UserAvatar(
                        photoUrl = currentUserPhotoUrl,
                        displayName = currentUserDisplayName,
                        isOnline = !isOffline,
                        modifier = Modifier.padding(end = 8.dp),
                        size = 40.dp,
                        showOnlineIndicator = false
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorTokens.ReactTheme.card
                )
            )

            // Offline Mode Banner (minimal)
            if (isOffline) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorTokens.Banner.offline
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            "Offline",
                            modifier = Modifier.size(16.dp),
                            tint = ColorTokens.Banner.onOffline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Offline mode • Changes saved locally",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.Banner.onOffline,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearchQueryChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("task_board_search_field"),
                placeholder = {
                    Text(
                        "Search tasks...",
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        "Search",
                        tint = ColorTokens.ReactTheme.mutedForeground
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                "Clear",
                                tint = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ColorTokens.ReactTheme.foreground,
                    unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                    focusedContainerColor = ColorTokens.ReactTheme.card,
                    unfocusedContainerColor = ColorTokens.ReactTheme.card,
                    focusedBorderColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.2f)
                )
            )

            // My Tasks / All Tasks Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == TaskFilter.MY_TASKS,
                    onClick = {
                        selectedFilter = TaskFilter.MY_TASKS
                        onFilterChange(TaskFilter.MY_TASKS)
                    },
                    label = { Text("My Tasks") },
                    modifier = Modifier.weight(1f).testTag("filter_chip_my_tasks"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColorTokens.ReactTheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = ColorTokens.ReactTheme.card,
                        labelColor = ColorTokens.ReactTheme.mutedForeground
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFilter == TaskFilter.MY_TASKS,
                        borderColor = if (selectedFilter == TaskFilter.MY_TASKS)
                            ColorTokens.ReactTheme.primary
                        else ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                        selectedBorderColor = ColorTokens.ReactTheme.primary
                    )
                )

                FilterChip(
                    selected = selectedFilter == TaskFilter.ALL_TASKS,
                    onClick = {
                        selectedFilter = TaskFilter.ALL_TASKS
                        onFilterChange(TaskFilter.ALL_TASKS)
                    },
                    label = { Text("All Tasks") },
                    modifier = Modifier.weight(1f).testTag("filter_chip_all_tasks"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColorTokens.ReactTheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = ColorTokens.ReactTheme.card,
                        labelColor = ColorTokens.ReactTheme.mutedForeground
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFilter == TaskFilter.ALL_TASKS,
                        borderColor = if (selectedFilter == TaskFilter.ALL_TASKS)
                            ColorTokens.ReactTheme.primary
                        else ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                        selectedBorderColor = ColorTokens.ReactTheme.primary
                    )
                )
            }

            // Kanban Columns (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TO DO Column
                KanbanColumn(
                    title = "TO DO",
                    count = todoTasks.size,
                    tasks = todoTasks,
                    statusColor = ColorTokens.ReactTheme.mutedForeground,
                    columnTag = "column_todo",
                    onTaskClick = onTaskClick,
                    onTaskStatusChange = onTaskStatusChange,
                    onEditTask = onEditTask,
                    onDeleteTask = onDeleteTask
                )

                // IN PROGRESS Column
                KanbanColumn(
                    title = "IN PROGRESS",
                    count = inProgressTasks.size,
                    tasks = inProgressTasks,
                    statusColor = ColorTokens.ReactTheme.primary,
                    columnTag = "column_in_progress",
                    onTaskClick = onTaskClick,
                    onTaskStatusChange = onTaskStatusChange,
                    onEditTask = onEditTask,
                    onDeleteTask = onDeleteTask
                )

                // DONE Column
                KanbanColumn(
                    title = "DONE",
                    count = doneTasks.size,
                    tasks = doneTasks,
                    statusColor = ColorTokens.Status.online,
                    columnTag = "column_done",
                    onTaskClick = onTaskClick,
                    onTaskStatusChange = onTaskStatusChange,
                    onEditTask = onEditTask,
                    onDeleteTask = onDeleteTask
                )
            }
        }

        // FAB for creating tasks
        FloatingActionButton(
            onClick = onCreateTask,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("create_task_fab"),
            containerColor = ColorTokens.ReactTheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, "Add Task")
        }

        // Loading Indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = ColorTokens.ReactTheme.primary
                )
            }
        }
    }
}

/**
 * Kanban Column Component
 */
@Composable
private fun KanbanColumn(
    title: String,
    count: Int,
    tasks: List<Task>,
    statusColor: Color,
    columnTag: String = "",
    onTaskClick: (Task) -> Unit,
    onTaskStatusChange: (Task, TaskStatus) -> Unit = { _, _ -> },
    onEditTask: (Task) -> Unit = {},
    onDeleteTask: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .then(if (columnTag.isNotEmpty()) Modifier.testTag(columnTag) else Modifier)
    ) {
        // Column Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ColorTokens.ReactTheme.foreground
                )
            }
            Surface(
                shape = CircleShape,
                color = ColorTokens.ReactTheme.card
            ) {
                Text(
                    count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorTokens.ReactTheme.mutedForeground,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Task Cards
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onClick = { onTaskClick(task) },
                    onStatusChange = { newStatus -> onTaskStatusChange(task, newStatus) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task) }
                )
            }
        }
    }
}

/**
 * Task Card Component (Stitch Design)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("task_card_${task.id}")
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = ColorTokens.ReactTheme.card
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top row: Priority Badge + 3-dot menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Badge
                    if (task.priority != TaskPriority.MEDIUM) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (task.priority) {
                                TaskPriority.URGENT -> ColorTokens.Priority.urgent.copy(alpha = 0.2f)
                                TaskPriority.HIGH -> ColorTokens.Priority.high.copy(alpha = 0.2f)
                                TaskPriority.LOW -> ColorTokens.Priority.low.copy(alpha = 0.2f)
                                else -> ColorTokens.Priority.medium.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                when (task.priority) {
                                    TaskPriority.URGENT -> "URGENT"
                                    TaskPriority.HIGH -> "HIGH"
                                    TaskPriority.LOW -> "LOW"
                                    else -> "MEDIUM"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (task.priority) {
                                    TaskPriority.URGENT -> ColorTokens.Priority.urgent
                                    TaskPriority.HIGH -> ColorTokens.Priority.high
                                    TaskPriority.LOW -> ColorTokens.Priority.low
                                    else -> ColorTokens.Priority.medium
                                }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier)
                    }

                    // 3-dot menu trigger
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = ColorTokens.ReactTheme.mutedForeground,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Task Title
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Task Description (if available)
                if (!task.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer: Assignee + Due Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Assignee Avatar (placeholder)
                    if (task.assignedToId != null) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ColorTokens.ReactTheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "A",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(24.dp))
                    }

                    // Due Date
                    if (task.dueDate != null) {
                        val isOverdue = task.dueDate < System.currentTimeMillis() && task.status != TaskStatus.DONE
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                "Due date",
                                modifier = Modifier.size(14.dp),
                                tint = if (isOverdue) ColorTokens.Error.light else ColorTokens.ReactTheme.mutedForeground
                            )
                            Text(
                                SimpleDateFormat("MMM dd", Locale.getDefault())
                                    .format(Date(task.dueDate)),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) ColorTokens.Error.light else ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                    }
                }
            }
        }

        // 3-dot dropdown menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit Task") },
                onClick = { showMenu = false; onEdit() },
                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp), tint = ColorTokens.ReactTheme.mutedForeground) }
            )
            HorizontalDivider(color = ColorTokens.ReactTheme.border.copy(alpha = 0.3f))
            DropdownMenuItem(
                text = { Text("Move to TODO") },
                onClick = { showMenu = false; onStatusChange(TaskStatus.TODO) },
                leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, null, modifier = Modifier.size(18.dp), tint = ColorTokens.ReactTheme.mutedForeground) },
                enabled = task.status != TaskStatus.TODO
            )
            DropdownMenuItem(
                text = { Text("Move to In Progress") },
                onClick = { showMenu = false; onStatusChange(TaskStatus.IN_PROGRESS) },
                leadingIcon = { Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(18.dp), tint = ColorTokens.ReactTheme.primary) },
                enabled = task.status != TaskStatus.IN_PROGRESS
            )
            DropdownMenuItem(
                text = { Text("Move to Done") },
                onClick = { showMenu = false; onStatusChange(TaskStatus.DONE) },
                leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp), tint = ColorTokens.Status.online) },
                enabled = task.status != TaskStatus.DONE
            )
            HorizontalDivider(color = ColorTokens.ReactTheme.border.copy(alpha = 0.3f))
            DropdownMenuItem(
                text = { Text("Delete Task", color = ColorTokens.ReactTheme.destructive) },
                onClick = { showMenu = false; onDelete() },
                leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = ColorTokens.ReactTheme.destructive) }
            )
        }
    }
}

/**
 * Task Filter Enum
 */
enum class TaskFilter {
    MY_TASKS,
    ALL_TASKS
}
