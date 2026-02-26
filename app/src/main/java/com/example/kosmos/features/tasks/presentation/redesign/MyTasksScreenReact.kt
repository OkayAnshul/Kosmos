package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.shared.ui.components.KosmosCard
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * My Tasks Screen - React Design Implementation
 *
 * Design Reference: documents/Kosmos/src/app/components/tasks/MyTasksScreen.tsx
 * DESIGN-ONLY: Uses mock data, NO backend wiring
 *
 * Features:
 * - View toggle: List ↔ Kanban (board view)
 * - Filter chips: All | Active | Completed
 * - List view: Vertical task cards with full info
 * - Kanban view: 3 columns (To Do | In Progress | Done)
 * - FAB: Create new task button (bottom-right)
 * - Task cards with:
 *   - Status indicator bar (left edge, 4px wide)
 *   - Title + project name
 *   - Status badge + priority badge
 *   - Due date + assignee avatar
 *
 * Colors from theme.css + component styles
 */

enum class TaskViewModeReact {
    LIST, KANBAN
}

enum class TaskFilterReact {
    ALL, TODO, IN_PROGRESS, DONE
}

enum class TaskStatusReact {
    TODO, IN_PROGRESS, DONE
}

enum class TaskPriorityReact {
    LOW, MEDIUM, HIGH
}

// Task data model (from React line 5-16)
data class TaskData(
    val id: String,
    val title: String,
    val status: TaskStatusReact,
    val priority: TaskPriorityReact,
    val dueDate: String? = null,
    val assignee: Assignee? = null,
    val projectName: String? = null
)

data class Assignee(
    val name: String,
    val avatar: String
)

// Mock tasks (from React line 6-61)
private val mockTasks = listOf(
    TaskData(
        id = "1",
        title = "Design new onboarding flow for mobile app",
        status = TaskStatusReact.IN_PROGRESS,
        priority = TaskPriorityReact.HIGH,
        dueDate = "Jan 15",
        assignee = Assignee("Alice", "A"),
        projectName = "Mobile App Redesign"
    ),
    TaskData(
        id = "2",
        title = "Update API documentation",
        status = TaskStatusReact.TODO,
        priority = TaskPriorityReact.MEDIUM,
        dueDate = "Jan 18",
        assignee = Assignee("Bob", "B"),
        projectName = "API Documentation"
    ),
    TaskData(
        id = "3",
        title = "Fix login page responsiveness",
        status = TaskStatusReact.IN_PROGRESS,
        priority = TaskPriorityReact.HIGH,
        dueDate = "Jan 12",
        assignee = Assignee("Carol", "C"),
        projectName = "Customer Portal v2"
    ),
    TaskData(
        id = "4",
        title = "Review pull requests",
        status = TaskStatusReact.TODO,
        priority = TaskPriorityReact.LOW,
        dueDate = "Jan 16",
        assignee = Assignee("David", "D"),
        projectName = "Website Performance"
    ),
    TaskData(
        id = "5",
        title = "Write unit tests for auth module",
        status = TaskStatusReact.DONE,
        priority = TaskPriorityReact.MEDIUM,
        dueDate = "Jan 10",
        assignee = Assignee("Eve", "E"),
        projectName = "Customer Portal v2"
    ),
    TaskData(
        id = "6",
        title = "Setup CI/CD pipeline",
        status = TaskStatusReact.DONE,
        priority = TaskPriorityReact.HIGH,
        dueDate = "Jan 8",
        assignee = Assignee("Frank", "F"),
        projectName = "Website Performance"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksScreenReact(
    tasks: List<TaskData> = mockTasks, // Accept tasks as parameter, default to mock
    viewMode: TaskViewModeReact = TaskViewModeReact.LIST,
    onViewModeChange: (TaskViewModeReact) -> Unit = {},
    filter: TaskFilterReact = TaskFilterReact.ALL,
    onFilterChange: (TaskFilterReact) -> Unit = {},
    onTaskClick: (String) -> Unit = {},
    onTaskEdit: (String) -> Unit = {},
    onTaskDelete: (String) -> Unit = {},
    onTaskStatusChange: (String) -> Unit = {},
    onTaskAssign: (String) -> Unit = {},
    onTaskAddTimeEntry: (String) -> Unit = {},
    onCreateTask: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter logic (line 74-78)
    val filteredTasks = tasks.filter { task ->
        val matchesFilter = when (filter) {
            TaskFilterReact.TODO -> task.status == TaskStatusReact.TODO
            TaskFilterReact.IN_PROGRESS -> task.status == TaskStatusReact.IN_PROGRESS
            TaskFilterReact.DONE -> task.status == TaskStatusReact.DONE
            TaskFilterReact.ALL -> true
        }
        val matchesSearch = searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    // Group by status for Kanban (line 80-82)
    val todoTasks = filteredTasks.filter { it.status == TaskStatusReact.TODO }
    val inProgressTasks = filteredTasks.filter { it.status == TaskStatusReact.IN_PROGRESS }
    val doneTasks = filteredTasks.filter { it.status == TaskStatusReact.DONE }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar (line 87-140)
            TopAppBar(
                viewMode = viewMode,
                onViewModeChange = onViewModeChange,
                filter = filter,
                onFilterChange = onFilterChange
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search tasks...", color = ColorTokens.ReactTheme.mutedForeground) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = ColorTokens.ReactTheme.mutedForeground) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear", tint = ColorTokens.ReactTheme.mutedForeground)
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
                    unfocusedBorderColor = ColorTokens.ReactTheme.border
                )
            )

            // Content (line 143-178)
            if (viewMode == TaskViewModeReact.LIST) {
                // List View (line 144-154)
                ListView(
                    tasks = filteredTasks,
                    onTaskClick = onTaskClick,
                    onTaskEdit = onTaskEdit,
                    onTaskDelete = onTaskDelete,
                    onTaskStatusChange = onTaskStatusChange,
                    onTaskAssign = onTaskAssign,
                    onTaskAddTimeEntry = onTaskAddTimeEntry
                )
            } else {
                // Kanban View (line 156-177)
                KanbanView(
                    todoTasks = todoTasks,
                    inProgressTasks = inProgressTasks,
                    doneTasks = doneTasks,
                    onTaskClick = onTaskClick,
                    onTaskEdit = onTaskEdit,
                    onTaskDelete = onTaskDelete,
                    onTaskStatusChange = onTaskStatusChange,
                    onTaskAssign = onTaskAssign,
                    onTaskAddTimeEntry = onTaskAddTimeEntry
                )
            }
        }

        // FAB (line 181-186)
        FloatingActionButton(
            onClick = onCreateTask,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)  // bottom-6 right-6
                .size(56.dp)  // w-14 h-14
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.5f)
                ),
            containerColor = ColorTokens.ReactTheme.primary,
            contentColor = ColorTokens.ReactTheme.primaryForeground
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create task",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Top App Bar (line 87-140)
@Composable
private fun TopAppBar(
    viewMode: TaskViewModeReact,
    onViewModeChange: (TaskViewModeReact) -> Unit,
    filter: TaskFilterReact,
    onFilterChange: (TaskFilterReact) -> Unit
) {
    Surface(
        color = ColorTokens.ReactTheme.card,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = ColorTokens.ReactTheme.border,
                shape = RectangleShape
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)  // px-4 py-3
        ) {
            // Title + View Toggle + Filter button (line 89-116)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),  // mb-3
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Tasks",
                    fontSize = 20.sp,  // 1.25rem
                    fontWeight = FontWeight.SemiBold,  // 600
                    color = ColorTokens.ReactTheme.foreground
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)  // gap-2
                ) {
                    // View Toggle (line 93-110)
                    ViewToggle(
                        viewMode = viewMode,
                        onViewModeChange = onViewModeChange
                    )
                    // Filter button (line 112-114)
                    var showFilterDialog by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = ColorTokens.ReactTheme.foreground,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Filter Dialog
                    if (showFilterDialog) {
                        TaskFilterDialog(
                            currentFilter = filter,
                            onFilterSelected = { newFilter ->
                                onFilterChange(newFilter)
                                showFilterDialog = false
                            },
                            onDismiss = { showFilterDialog = false }
                        )
                    }
                }
            }

            // Filter Chips (line 119-138)
            FilterChips(
                activeFilter = filter,
                onFilterChange = onFilterChange
            )
        }
    }
}

// View Toggle Component (line 93-110)
@Composable
private fun ViewToggle(
    viewMode: TaskViewModeReact,
    onViewModeChange: (TaskViewModeReact) -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = ColorTokens.ReactTheme.secondary,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(2.dp)  // p-0.5
    ) {
        // List button
        IconButton(
            onClick = { onViewModeChange(TaskViewModeReact.LIST) },
            modifier = Modifier
                .size(32.dp)
                .then(
                    if (viewMode == TaskViewModeReact.LIST)
                        Modifier
                            .background(ColorTokens.ReactTheme.card, RoundedCornerShape(6.dp))
                            .shadow(1.dp, RoundedCornerShape(6.dp))
                    else Modifier
                )
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "List view",
                tint = if (viewMode == TaskViewModeReact.LIST)
                    ColorTokens.ReactTheme.primary
                else
                    ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.size(18.dp)
            )
        }
        // Kanban button
        IconButton(
            onClick = { onViewModeChange(TaskViewModeReact.KANBAN) },
            modifier = Modifier
                .size(32.dp)
                .then(
                    if (viewMode == TaskViewModeReact.KANBAN)
                        Modifier
                            .background(ColorTokens.ReactTheme.card, RoundedCornerShape(6.dp))
                            .shadow(1.dp, RoundedCornerShape(6.dp))
                    else Modifier
                )
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = "Kanban view",
                tint = if (viewMode == TaskViewModeReact.KANBAN)
                    ColorTokens.ReactTheme.primary
                else
                    ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Filter Chips (line 119-138)
@Composable
private fun FilterChips(
    activeFilter: TaskFilterReact,
    onFilterChange: (TaskFilterReact) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),  // gap-2
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf(
            TaskFilterReact.ALL to "All",
            TaskFilterReact.TODO to "To Do",
            TaskFilterReact.IN_PROGRESS to "In Progress",
            TaskFilterReact.DONE to "Done"
        ).forEach { (filterValue, label) ->
            FilterChip(
                selected = activeFilter == filterValue,
                onClick = { onFilterChange(filterValue) },
                label = {
                    Text(
                        text = label,
                        fontSize = 14.sp,  // 0.875rem
                        fontWeight = FontWeight.Medium  // 500
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ColorTokens.ReactTheme.primary,
                    selectedLabelColor = ColorTokens.ReactTheme.primaryForeground,
                    containerColor = ColorTokens.ReactTheme.secondary,
                    labelColor = ColorTokens.ReactTheme.foreground
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// List View (line 144-154) - NOW WITH PROJECT GROUPING
@Composable
private fun ListView(
    tasks: List<TaskData>,
    onTaskClick: (String) -> Unit,
    onTaskEdit: (String) -> Unit,
    onTaskDelete: (String) -> Unit,
    onTaskStatusChange: (String) -> Unit = {},
    onTaskAssign: (String) -> Unit = {},
    onTaskAddTimeEntry: (String) -> Unit = {}
) {
    // Group tasks by project
    val tasksByProject = tasks.groupBy { it.projectName ?: "No Project" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),  // px-4 py-4
        verticalArrangement = Arrangement.spacedBy(20.dp)  // space between project groups
    ) {
        tasksByProject.forEach { (projectName, projectTasks) ->
            // Project Header
            item(key = "header_$projectName") {
                ProjectGroupHeader(
                    projectName = projectName,
                    taskCount = projectTasks.size
                )
            }

            // Tasks for this project
            items(
                items = projectTasks,
                key = { task -> task.id }
            ) { task ->
                TaskCard(
                    task = task,
                    onClick = { onTaskClick(task.id) },
                    onEdit = { onTaskEdit(task.id) },
                    onDelete = { onTaskDelete(task.id) },
                    onStatusChange = { onTaskStatusChange(task.id) },
                    onAssign = { onTaskAssign(task.id) },
                    onAddTimeEntry = { onTaskAddTimeEntry(task.id) },
                    compact = false,
                    showProjectName = false  // Hide project name - already in header
                )
            }

            // Spacer between project groups
            item(key = "spacer_$projectName") {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// Project Group Header - NEW
@Composable
private fun ProjectGroupHeader(
    projectName: String,
    taskCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Project icon
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ColorTokens.ReactTheme.primary)
            )

            Text(
                text = projectName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorTokens.ReactTheme.foreground
            )
        }

        // Task count badge
        Surface(
            color = ColorTokens.ReactTheme.secondary,
            shape = CircleShape
        ) {
            Text(
                text = "$taskCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// Kanban View (line 156-177)
@Composable
private fun KanbanView(
    todoTasks: List<TaskData>,
    inProgressTasks: List<TaskData>,
    doneTasks: List<TaskData>,
    onTaskClick: (String) -> Unit,
    onTaskEdit: (String) -> Unit,
    onTaskDelete: (String) -> Unit,
    onTaskStatusChange: (String) -> Unit = {},
    onTaskAssign: (String) -> Unit = {},
    onTaskAddTimeEntry: (String) -> Unit = {}
) {
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),  // gap-3
        contentPadding = PaddingValues(16.dp)  // px-4 py-4
    ) {
        item {
            KanbanColumn(
                title = "To Do",
                status = TaskStatusReact.TODO,
                tasks = todoTasks,
                onTaskClick = onTaskClick,
                onTaskEdit = onTaskEdit,
                onTaskDelete = onTaskDelete,
                onTaskStatusChange = onTaskStatusChange,
                onTaskAssign = onTaskAssign,
                onTaskAddTimeEntry = onTaskAddTimeEntry
            )
        }
        item {
            KanbanColumn(
                title = "In Progress",
                status = TaskStatusReact.IN_PROGRESS,
                tasks = inProgressTasks,
                onTaskClick = onTaskClick,
                onTaskEdit = onTaskEdit,
                onTaskDelete = onTaskDelete,
                onTaskStatusChange = onTaskStatusChange,
                onTaskAssign = onTaskAssign,
                onTaskAddTimeEntry = onTaskAddTimeEntry
            )
        }
        item {
            KanbanColumn(
                title = "Done",
                status = TaskStatusReact.DONE,
                tasks = doneTasks,
                onTaskClick = onTaskClick,
                onTaskEdit = onTaskEdit,
                onTaskDelete = onTaskDelete,
                onTaskStatusChange = onTaskStatusChange,
                onTaskAssign = onTaskAssign,
                onTaskAddTimeEntry = onTaskAddTimeEntry
            )
        }
    }
}

// Kanban Column (from KanbanColumn.tsx line 12-47)
@Composable
private fun KanbanColumn(
    title: String,
    status: TaskStatusReact,
    tasks: List<TaskData>,
    onTaskClick: (String) -> Unit,
    onTaskEdit: (String) -> Unit,
    onTaskDelete: (String) -> Unit,
    onTaskStatusChange: (String) -> Unit = {},
    onTaskAssign: (String) -> Unit = {},
    onTaskAddTimeEntry: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.width(288.dp)  // w-72 (18rem = 288px)
    ) {
        // Column Header (line 15-25)
        KosmosCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)  // mb-3
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,  // 0.9375rem
                    fontWeight = FontWeight.SemiBold,  // 600
                    color = ColorTokens.ReactTheme.foreground
                )
                Surface(
                    color = ColorTokens.ReactTheme.secondary,
                    shape = CircleShape,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tasks.size.toString(),
                        fontSize = 12.sp,  // 0.75rem
                        fontWeight = FontWeight.Medium,  // 500
                        color = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Tasks (line 28-45)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),  // space-y-2
            modifier = Modifier.fillMaxWidth()
        ) {
            if (tasks.isNotEmpty()) {
                tasks.forEach { task ->
                    TaskCard(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        onEdit = { onTaskEdit(task.id) },
                        onDelete = { onTaskDelete(task.id) },
                        onStatusChange = { onTaskStatusChange(task.id) },
                        onAssign = { onTaskAssign(task.id) },
                        onAddTimeEntry = { onTaskAddTimeEntry(task.id) },
                        compact = true,
                        showProjectName = true  // Keep showing project name in Kanban view
                    )
                }
            } else {
                // Empty state (line 38-43)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = ColorTokens.ReactTheme.border,
                            shape = RoundedCornerShape(Tokens.CornerRadius.md)
                        )
                        .background(
                            color = ColorTokens.ReactTheme.card.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(Tokens.CornerRadius.md)
                        )
                        .padding(24.dp),  // p-6
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks",
                        fontSize = 14.sp,  // 0.875rem
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }
        }
    }
}

// Task Card (from TaskCard.tsx line 24-98)
@Composable
private fun TaskCard(
    task: TaskData,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onStatusChange: () -> Unit = {},
    onAssign: () -> Unit = {},
    onAddTimeEntry: () -> Unit = {},
    compact: Boolean = false,
    showProjectName: Boolean = true  // NEW: Control project name visibility
) {
    val statusColor = getStatusColor(task.status)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.CornerRadius.md))
    ) {
        // Status indicator bar (line 34-37)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)  // w-1 (4px)
                .background(statusColor)
                .align(Alignment.CenterStart)
        )

        // Card content - non-clickable card, navigation handled via Box overlay
        KosmosCard(
            modifier = Modifier.padding(start = 0.dp)
        ) {
            // Menu state hoisted above clickable area
            var showTaskMenu by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                // Clickable background for card navigation — behind everything
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { onClick() }
                )

                // Non-clickable content layer
                Column(
                    modifier = Modifier.padding(start = 8.dp)  // pl-2
                ) {
                    // Header (line 41-56)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),  // mb-2
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = task.title,
                            fontSize = if (compact) 14.sp else 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorTokens.ReactTheme.foreground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // More menu — sits on top of clickable background, consumes its own taps
                        Box {
                            IconButton(
                                onClick = { showTaskMenu = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = ColorTokens.ReactTheme.mutedForeground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showTaskMenu,
                                onDismissRequest = { showTaskMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Task") },
                                    onClick = {
                                        showTaskMenu = false
                                        onEdit()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = ColorTokens.ReactTheme.mutedForeground)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Change Status") },
                                    onClick = {
                                        showTaskMenu = false
                                        onStatusChange()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp), tint = ColorTokens.ReactTheme.mutedForeground)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Assign to...") },
                                    onClick = {
                                        showTaskMenu = false
                                        onAssign()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = ColorTokens.ReactTheme.mutedForeground)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add Time Entry") },
                                    onClick = {
                                        showTaskMenu = false
                                        onAddTimeEntry()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp), tint = ColorTokens.ReactTheme.mutedForeground)
                                    }
                                )
                                HorizontalDivider(color = ColorTokens.ReactTheme.border.copy(alpha = 0.3f))
                                DropdownMenuItem(
                                    text = { Text("Delete Task", color = ColorTokens.ReactTheme.destructive) },
                                    onClick = {
                                        showTaskMenu = false
                                        onDelete()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = ColorTokens.ReactTheme.destructive
                                        )
                                    }
                                )
                            }
                        }
                    }

                // Project name (line 59-63) - only show if showProjectName=true
                if (task.projectName != null && showProjectName && !compact) {
                    Text(
                        text = task.projectName,
                        fontSize = 12.sp,  // 0.75rem
                        color = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.padding(bottom = 8.dp)  // mb-2
                    )
                }

                // Badges row (line 66-69)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),  // gap-2
                    modifier = Modifier.padding(bottom = 8.dp)  // mb-2
                ) {
                    TaskStatusBadge(status = task.status)
                    PriorityBadge(priority = task.priority)
                }

                // Footer (line 72-95)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Due date (line 73-79)
                    if (task.dueDate != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),  // gap-1.5
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = ColorTokens.ReactTheme.mutedForeground,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = task.dueDate,
                                fontSize = 12.sp,  // 0.75rem
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Assignee avatar (line 82-94)
                    if (task.assignee != null) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)  // w-6 h-6
                                .clip(CircleShape)
                                .background(ColorTokens.ReactTheme.primary)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = CircleShape,
                                    ambientColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.4f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = task.assignee.name.first().toString(),
                                fontSize = 10.sp,  // 0.625rem
                                fontWeight = FontWeight.Medium,  // 500
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

// Task Status Badge (from TaskStatusBadge.tsx line 7-46)
@Composable
private fun TaskStatusBadge(
    status: TaskStatusReact
) {
    data class BadgeStyle(val bgColor: Color, val textColor: Color, val borderColor: Color, val label: String)

    val style = when (status) {
        TaskStatusReact.TODO -> BadgeStyle(
            bgColor = ColorTokens.ReactTheme.muted,
            textColor = ColorTokens.ReactTheme.mutedForeground,
            borderColor = ColorTokens.ReactTheme.muted,
            label = "To Do"
        )
        TaskStatusReact.IN_PROGRESS -> BadgeStyle(
            bgColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.2f),
            textColor = ColorTokens.ReactTheme.primary,
            borderColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.3f),
            label = "In Progress"
        )
        TaskStatusReact.DONE -> BadgeStyle(
            bgColor = Color(0xFF10B981).copy(alpha = 0.2f),
            textColor = Color(0xFF34D399),
            borderColor = Color(0xFF10B981).copy(alpha = 0.3f),
            label = "Done"
        )
    }

    Surface(
        color = style.bgColor,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, style.borderColor),
        modifier = Modifier
    ) {
        Text(
            text = style.label,
            fontSize = 12.sp,  // 0.75rem
            fontWeight = FontWeight.Medium,  // 500
            color = style.textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)  // px-2.5 py-0.5
        )
    }
}

// Priority Badge (from PriorityBadge.tsx line 10-53)
@Composable
private fun PriorityBadge(
    priority: TaskPriorityReact,
    showLabel: Boolean = false
) {
    val (icon, color, label) = when (priority) {
        TaskPriorityReact.LOW -> Triple(Icons.Default.Remove, Color(0xFF6B7280), "Low")  // Minus icon, gray
        TaskPriorityReact.MEDIUM -> Triple(Icons.Default.Warning, Color(0xFFF59E0B), "Medium")  // Alert, amber
        TaskPriorityReact.HIGH -> Triple(Icons.Default.ArrowUpward, Color(0xFFEF4444), "High")  // Arrow up, red
    }

    Surface(
        color = color.copy(alpha = 0.15f),  // ${color}15
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),  // gap-1
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)  // px-2 py-0.5
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            if (showLabel) {
                Text(
                    text = label,
                    fontSize = 12.sp,  // 0.75rem
                    fontWeight = FontWeight.Medium,  // 500
                    color = color
                )
            }
        }
    }
}

// Helper: Get status color (from TaskStatusBadge.tsx line 44-46)
private fun getStatusColor(status: TaskStatusReact): Color {
    return when (status) {
        TaskStatusReact.TODO -> Color(0xFF6B7280)  // Gray
        TaskStatusReact.IN_PROGRESS -> Color(0xFF7C3AED)  // Purple (primary)
        TaskStatusReact.DONE -> Color(0xFF10B981)  // Emerald green
    }
}

// Task Filter Dialog
@Composable
private fun TaskFilterDialog(
    currentFilter: TaskFilterReact,
    onFilterSelected: (TaskFilterReact) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Filter Tasks",
                style = MaterialTheme.typography.titleLarge,
                color = ColorTokens.ReactTheme.foreground
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilterReact.entries.forEach { filter ->
                    val label = when (filter) {
                        TaskFilterReact.ALL -> "All Tasks"
                        TaskFilterReact.TODO -> "To Do"
                        TaskFilterReact.IN_PROGRESS -> "In Progress"
                        TaskFilterReact.DONE -> "Done"
                    }

                    Surface(
                        onClick = { onFilterSelected(filter) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (filter == currentFilter)
                            ColorTokens.ReactTheme.primary.copy(alpha = 0.1f)
                        else
                            ColorTokens.ReactTheme.card
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (filter == currentFilter)
                                    ColorTokens.ReactTheme.primary
                                else
                                    ColorTokens.ReactTheme.foreground
                            )
                            if (filter == currentFilter) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
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
                Text("Close", color = ColorTokens.ReactTheme.primary)
            }
        },
        containerColor = ColorTokens.ReactTheme.card,
        tonalElevation = 8.dp
    )
}
