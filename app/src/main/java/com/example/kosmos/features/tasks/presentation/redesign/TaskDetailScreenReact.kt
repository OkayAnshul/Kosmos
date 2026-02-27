package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.core.models.Permission
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.core.models.TaskDependency
import com.example.kosmos.features.tasks.components.ActivityTimeline
import com.example.kosmos.shared.ui.components.PermissionGated
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Task Detail Screen - React Design Implementation
 *
 * Design Reference: documents/Kosmos/src/app/components/tasks/TaskDetailScreen.tsx
 *
 * Features:
 * - View-only comprehensive task information
 * - Top app bar with back + edit buttons
 * - Title section with project name, title, status, priority
 * - Meta info grid (due date, assignee)
 * - Description card
 * - Subtasks with checkboxes and progress counter
 * - Time tracking (tracked vs estimate)
 * - Activity timeline with avatars
 * - Comments section with input
 *
 * All styling matches React design exactly:
 * - Colors from ColorTokens.ReactTheme.*
 * - Cards with border and shadow (rounded-xl, 12dp)
 * - Meta grid: 2 columns with icon labels
 * - Subtasks: checkbox + strikethrough on completed
 * - Activity: avatar + timeline connector
 * - NO backend wiring (mock data only)
 */

// Mock data models
data class TaskDetailData(
    val id: String,
    val title: String,
    val status: TaskStatusDetail,
    val priority: TaskPriorityDetail,
    val description: String,
    val dueDate: String,
    val assignee: TaskAssignee,
    val projectName: String,
    val createdAt: String,
    val subtasks: List<Subtask>,
    val timeTracked: String,
    val timeEstimate: String,
    val activity: List<ActivityItem>
)

data class TaskAssignee(
    val name: String,
    val avatar: String
)

data class Subtask(
    val id: String,
    val title: String,
    val completed: Boolean
)

data class ActivityItem(
    val id: String,
    val user: String,
    val action: String,
    val time: String
)

enum class TaskStatusDetail {
    TODO, IN_PROGRESS, DONE
}

enum class TaskPriorityDetail {
    LOW, MEDIUM, HIGH
}

// Mock task data for testing
private val mockTask = TaskDetailData(
    id = "1",
    title = "Design new onboarding flow for mobile app",
    status = TaskStatusDetail.IN_PROGRESS,
    priority = TaskPriorityDetail.HIGH,
    description = "Create a comprehensive onboarding experience that guides new users through the key features of the mobile application. Focus on clarity and engagement.",
    dueDate = "Jan 15, 2026",
    assignee = TaskAssignee(name = "Alice Chen", avatar = "A"),
    projectName = "Mobile App Redesign",
    createdAt = "Jan 8, 2026",
    subtasks = listOf(
        Subtask(id = "1", title = "Design welcome screen", completed = true),
        Subtask(id = "2", title = "Create feature tour screens", completed = true),
        Subtask(id = "3", title = "Design skip/continue interactions", completed = false),
        Subtask(id = "4", title = "Add animations and transitions", completed = false)
    ),
    timeTracked = "4h 30m",
    timeEstimate = "8h",
    activity = listOf(
        ActivityItem(id = "1", user = "Alice", action = "changed status to In Progress", time = "2 hours ago"),
        ActivityItem(id = "2", user = "Bob", action = "added a comment", time = "5 hours ago"),
        ActivityItem(id = "3", user = "Alice", action = "completed subtask \"Design welcome screen\"", time = "1 day ago")
    )
)

@Composable
fun TaskDetailScreenReact(
    taskId: String,
    task: TaskDetailData = mockTask, // Accept task as parameter, default to mock
    activities: List<TaskActivity> = emptyList(), // Real activity data for ActivityTimeline component
    comments: List<com.example.kosmos.core.models.TaskComment> = emptyList(), // P1-05 FIX: Real comments from task
    currentUserName: String = "You",
    currentUserAvatar: String = "Y",
    currentMember: ProjectMember? = null,
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onMore: () -> Unit = {}, // Callback for More Actions button
    onSubtaskToggle: (String) -> Unit = {}, // Callback for subtask completion toggle
    onAddSubtask: () -> Unit = {},
    dependencies: List<Task> = emptyList(), // Tasks this task depends on
    dependentTasks: List<Task> = emptyList(), // Tasks that depend on this task
    onRemoveDependency: (String) -> Unit = {},
    onAddDependency: () -> Unit = {},
    onAddJournalEntry: () -> Unit = {},
    onAddComment: (String) -> Unit = {},
    // Inline action callbacks
    onStatusClick: () -> Unit = {},
    onAssigneeClick: () -> Unit = {},
    onDeleteTask: () -> Unit = {},
    onStartTimer: () -> Unit = {},
    onStopTimer: () -> Unit = {},
    isTimerRunning: Boolean = false
) {

    val completedSubtasks = task.subtasks.count { it.completed }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            currentMember = currentMember,
            onBack = onBack,
            onEdit = onEdit,
            onMore = onMore,
            onDeleteTask = onDeleteTask
        )

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Title Section
            item {
                TitleSection(
                    projectName = task.projectName,
                    title = task.title,
                    status = task.status,
                    priority = task.priority,
                    onStatusClick = onStatusClick
                )
            }

            // Meta Info Grid
            item {
                MetaInfoGrid(
                    dueDate = task.dueDate,
                    assignee = task.assignee,
                    onAssigneeClick = onAssigneeClick
                )
            }

            // Description
            item {
                DescriptionCard(description = task.description)
            }

            // Subtasks
            item {
                SubtasksCard(
                    subtasks = task.subtasks,
                    completedCount = completedSubtasks,
                    totalCount = task.subtasks.size,
                    onSubtaskToggle = onSubtaskToggle,
                    onAddSubtask = onAddSubtask
                )
            }

            // Time Tracking
            item {
                TimeTrackingCard(
                    timeTracked = task.timeTracked,
                    timeEstimate = task.timeEstimate,
                    onStartTimer = onStartTimer,
                    onStopTimer = onStopTimer,
                    isTimerRunning = isTimerRunning
                )
            }

            // Dependencies (always shown so user can add)
            item {
                DependenciesCard(
                    dependencies = dependencies,
                    dependentTasks = dependentTasks,
                    onRemoveDependency = onRemoveDependency,
                    onAddDependency = onAddDependency
                )
            }

            // Activity Timeline (using enhanced component with commit messages)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ColorTokens.ReactTheme.card),
                    border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
                    shape = RoundedCornerShape(Tokens.CornerRadius.md),
                    elevation = CardDefaults.cardElevation(defaultElevation = Tokens.Elevation.level1)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Activity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorTokens.ReactTheme.foreground
                            )
                            TextButton(onClick = onAddJournalEntry) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = ColorTokens.ReactTheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Add Entry",
                                    fontSize = 14.sp,
                                    color = ColorTokens.ReactTheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        ActivityTimeline(
                            activities = activities,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Comments Section - P1-05 FIX: Pass real comments and callback
            item {
                CommentsCard(
                    comments = comments,
                    currentUserName = currentUserName,
                    currentUserAvatar = currentUserAvatar,
                    onAddComment = onAddComment
                )
            }
        }
    }
}

@Composable
private fun TopAppBar(
    currentMember: ProjectMember? = null,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onMore: () -> Unit = {},
    onDeleteTask: () -> Unit = {}
) {
    var showDropdown by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ColorTokens.ReactTheme.card,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = ColorTokens.ReactTheme.foreground,
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = ColorTokens.ReactTheme.foreground,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showDropdown = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More actions",
                            tint = ColorTokens.ReactTheme.mutedForeground,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        PermissionGated(
                            permission = Permission.EDIT_ANY_TASK,
                            currentMember = currentMember,
                            action = "Edit task"
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Task") },
                                onClick = {
                                    showDropdown = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = ColorTokens.ReactTheme.mutedForeground
                                    )
                                }
                            )
                        }
                        HorizontalDivider(color = ColorTokens.ReactTheme.border.copy(alpha = 0.3f))
                        PermissionGated(
                            permission = Permission.DELETE_ANY_TASK,
                            currentMember = currentMember,
                            action = "Delete task"
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete Task",
                                        color = ColorTokens.ReactTheme.destructive
                                    )
                                },
                                onClick = {
                                    showDropdown = false
                                    onDeleteTask()
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
            }
        }
    }

    HorizontalDivider(color = ColorTokens.ReactTheme.border, thickness = 1.dp)
}

@Composable
private fun TitleSection(
    projectName: String,
    title: String,
    status: TaskStatusDetail,
    priority: TaskPriorityDetail,
    onStatusClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Project name
        Text(
            text = projectName,
            fontSize = 14.sp,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        // Task title
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground,
            lineHeight = 31.2.sp  // 1.3 line height
        )

        // Status + Priority badges
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clickable status badge
            Row(
                modifier = Modifier
                    .clickable { onStatusClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TaskStatusDetailBadgeReact(status = status)
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Change status",
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(16.dp)
                )
            }
            TaskPriorityDetailBadgeReact(priority = priority, showLabel = true)
        }
    }
}

@Composable
private fun MetaInfoGrid(
    dueDate: String,
    assignee: TaskAssignee,
    onAssigneeClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Due Date
        MetaCard(
            icon = Icons.Default.CalendarToday,
            label = "Due Date",
            value = {
                Text(
                    text = dueDate,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTokens.ReactTheme.foreground
                )
            },
            modifier = Modifier.weight(1f)
        )

        // Assignee (clickable)
        MetaCard(
            icon = Icons.Default.Person,
            label = "Assignee",
            value = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(ColorTokens.ReactTheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = assignee.avatar,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }

                    Text(
                        text = assignee.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTokens.ReactTheme.foreground
                    )

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change assignee",
                        tint = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.size(14.dp)
                    )
                }
            },
            modifier = Modifier
                .weight(1f)
                .clickable { onAssigneeClick() }
        )
    }
}

@Composable
private fun MetaCard(
    icon: ImageVector,
    label: String,
    value: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        ),
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = Tokens.Elevation.level1)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Icon + label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            // Value
            value()
        }
    }
}

@Composable
private fun DescriptionCard(description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        ),
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = Tokens.Elevation.level1)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Description",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorTokens.ReactTheme.foreground
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = ColorTokens.ReactTheme.mutedForeground,
                lineHeight = 22.4.sp  // 1.6 line height
            )
        }
    }
}

@Composable
private fun SubtasksCard(
    subtasks: List<Subtask>,
    completedCount: Int,
    totalCount: Int,
    onSubtaskToggle: (String) -> Unit = {},
    onAddSubtask: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        ),
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = Tokens.Elevation.level1)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtasks",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$completedCount/$totalCount",
                        fontSize = 14.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                    IconButton(
                        onClick = onAddSubtask,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Subtask",
                            tint = ColorTokens.ReactTheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Subtask list
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subtasks.forEach { subtask ->
                    SubtaskItem(
                        subtask = subtask,
                        onToggle = { onSubtaskToggle(subtask.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtaskItem(
    subtask: Subtask,
    onToggle: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = 2.dp,
                    color = if (subtask.completed) ColorTokens.ReactTheme.primary else ColorTokens.ReactTheme.mutedForeground,
                    shape = RoundedCornerShape(4.dp)
                )
                .background(
                    color = if (subtask.completed) ColorTokens.ReactTheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (subtask.completed) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.primaryForeground,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Title
        Text(
            text = subtask.title,
            fontSize = 14.sp,
            color = if (subtask.completed) ColorTokens.ReactTheme.mutedForeground else ColorTokens.ReactTheme.foreground,
            textDecoration = if (subtask.completed) TextDecoration.LineThrough else null
        )
    }
}

@Composable
private fun TimeTrackingCard(
    timeTracked: String,
    timeEstimate: String,
    onStartTimer: () -> Unit = {},
    onStopTimer: () -> Unit = {},
    isTimerRunning: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        ),
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = Tokens.Elevation.level1)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Time Tracking",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorTokens.ReactTheme.foreground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tracked time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = "Tracked",
                            fontSize = 12.sp,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                        Text(
                            text = timeTracked,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorTokens.ReactTheme.foreground
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(ColorTokens.ReactTheme.border)
                )

                // Estimate
                Column {
                    Text(
                        text = "Estimate",
                        fontSize = 12.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                    Text(
                        text = timeEstimate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTokens.ReactTheme.foreground
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Timer button
                if (isTimerRunning) {
                    FilledTonalButton(
                        onClick = onStopTimer,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ColorTokens.ReactTheme.destructive.copy(alpha = 0.15f),
                            contentColor = ColorTokens.ReactTheme.destructive
                        )
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Stop", fontSize = 13.sp)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onStartTimer,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.15f),
                            contentColor = ColorTokens.ReactTheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Start", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityTimelineCard(activity: List<ActivityItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        ),
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = Tokens.Elevation.level1)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorTokens.ReactTheme.foreground
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                activity.forEachIndexed { index, item ->
                    ActivityTimelineItem(
                        item = item,
                        showConnector = index < activity.size - 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTimelineItem(
    item: ActivityItem,
    showConnector: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar + connector
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ColorTokens.ReactTheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.user.first().toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(ColorTokens.ReactTheme.border)
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row {
                Text(
                    text = item.user,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = " ${item.action}",
                    fontSize = 14.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }
            Text(
                text = item.time,
                fontSize = 12.sp,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
    }
}

@Composable
private fun CommentsCard(
    comments: List<com.example.kosmos.core.models.TaskComment> = emptyList(),
    currentUserName: String = "You",
    currentUserAvatar: String = "Y",
    onAddComment: (String) -> Unit = {}
) {
    // P1-05 FIX: Make CommentsCard functional with real input
    var commentText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        ),
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = Tokens.Elevation.level1)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Comments (${comments.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
            }

            // Comment input
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // User avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1)),  // Indigo color from React
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentUserAvatar,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                // Input field
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = {
                            Text(
                                text = "Add a comment...",
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ColorTokens.ReactTheme.secondary,
                            unfocusedContainerColor = ColorTokens.ReactTheme.secondary,
                            focusedBorderColor = ColorTokens.ReactTheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        maxLines = 3
                    )

                    // Post button
                    if (commentText.isNotBlank()) {
                        Button(
                            onClick = {
                                onAddComment(commentText)
                                commentText = "" // Clear input after posting
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorTokens.ReactTheme.primary
                            )
                        ) {
                            Text("Post Comment")
                        }
                    }
                }
            }

            // Display existing comments
            if (comments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = ColorTokens.ReactTheme.border)
                Spacer(modifier = Modifier.height(8.dp))

                comments.forEach { comment ->
                    CommentItem(comment = comment)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: com.example.kosmos.core.models.TaskComment) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Author avatar
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF6366F1)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.authorName.firstOrNull()?.toString() ?: "?",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        // Comment content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.authorName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = formatCommentTime(comment.timestamp),
                    fontSize = 12.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.content,
                fontSize = 14.sp,
                color = ColorTokens.ReactTheme.foreground.copy(alpha = 0.9f)
            )
        }
    }
}

private fun formatCommentTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}

@Composable
private fun TaskStatusDetailBadgeReact(status: TaskStatusDetail) {
    val (bgColor, textColor, label) = when (status) {
        TaskStatusDetail.TODO -> Triple(
            Color(0xFF3F3F46),  // Gray
            ColorTokens.ReactTheme.foreground,
            "To Do"
        )
        TaskStatusDetail.IN_PROGRESS -> Triple(
            ColorTokens.ReactTheme.primary.copy(alpha = 0.2f),
            ColorTokens.ReactTheme.primary,
            "In Progress"
        )
        TaskStatusDetail.DONE -> Triple(
            Color(0x3310B981),  // Emerald with 20% alpha
            Color(0xFF10B981),  // Emerald
            "Done"
        )
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun TaskPriorityDetailBadgeReact(
    priority: TaskPriorityDetail,
    showLabel: Boolean = false
) {
    val (bgColor, textColor, icon, label) = when (priority) {
        TaskPriorityDetail.LOW -> Quadruple(
            Color(0xFF3F3F46),  // Gray
            ColorTokens.ReactTheme.foreground,
            Icons.Default.Remove,
            "Low"
        )
        TaskPriorityDetail.MEDIUM -> Quadruple(
            Color(0x33F59E0B),  // Amber with 20% alpha
            Color(0xFFF59E0B),  // Amber
            Icons.Default.Warning,
            "Medium"
        )
        TaskPriorityDetail.HIGH -> Quadruple(
            Color(0x33EF4444),  // Red with 20% alpha
            ColorTokens.ReactTheme.destructive,
            Icons.Default.ArrowUpward,
            "High"
        )
    }

    Row(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(12.dp)
        )
        if (showLabel) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

/**
 * Dependencies Card - shows blocking/blocked-by tasks
 */
@Composable
private fun DependenciesCard(
    dependencies: List<Task>,
    dependentTasks: List<Task>,
    onRemoveDependency: (String) -> Unit,
    onAddDependency: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorTokens.ReactTheme.card),
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AccountTree, null, tint = ColorTokens.ReactTheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Dependencies", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = ColorTokens.ReactTheme.foreground, modifier = Modifier.weight(1f))
                IconButton(onClick = onAddDependency, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, "Add dependency", tint = ColorTokens.ReactTheme.primary, modifier = Modifier.size(18.dp))
                }
            }

            if (dependencies.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Blocked by", style = MaterialTheme.typography.labelSmall, color = ColorTokens.ReactTheme.mutedForeground)
                Spacer(Modifier.height(4.dp))
                dependencies.forEach { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Block, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(task.title, style = MaterialTheme.typography.bodySmall, color = ColorTokens.ReactTheme.foreground, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { onRemoveDependency(task.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Remove", tint = ColorTokens.ReactTheme.mutedForeground, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            if (dependentTasks.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Blocking", style = MaterialTheme.typography.labelSmall, color = ColorTokens.ReactTheme.mutedForeground)
                Spacer(Modifier.height(4.dp))
                dependentTasks.forEach { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(task.title, style = MaterialTheme.typography.bodySmall, color = ColorTokens.ReactTheme.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// Helper data class for quadruple
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
