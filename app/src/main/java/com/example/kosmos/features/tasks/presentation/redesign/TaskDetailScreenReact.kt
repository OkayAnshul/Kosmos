package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.kosmos.features.tasks.components.ActivityTimeline
import com.example.kosmos.shared.ui.components.PermissionGated
import com.example.kosmos.shared.ui.components.SectionCard
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Task Detail Screen - React Design Implementation (Redesigned)
 *
 * Design Reference: documents/Kosmos/src/app/components/tasks/TaskDetailScreen.tsx
 *
 * Layout: Hero + Grouped SectionCard design
 * - TopAppBar: back arrow, truncated title, Edit icon + MoreVert overflow
 * - HeroCard: large task title, project/status/priority badges
 * - MetaCard: 2-column grid (assignee, due date, created, estimated hours)
 * - DescriptionCard: full description
 * - SubtasksCard: progress bar + subtask rows
 * - DependenciesCard: blocked-by / blocking lists
 * - TimeCard: tracked vs estimate + start/stop timer
 * - ActivityCard: ActivityTimeline component
 * - CommentsCard: list + input row
 *
 * All styling matches React design exactly.
 * NO backend wiring — mock data only.
 */

// ─── Mock data models ────────────────────────────────────────────────────────

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

// ─── Main screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreenReact(
    taskId: String,
    task: TaskDetailData = mockTask,
    activities: List<TaskActivity> = emptyList(),
    comments: List<com.example.kosmos.core.models.TaskComment> = emptyList(),
    currentUserName: String = "You",
    currentUserAvatar: String = "Y",
    currentMember: ProjectMember? = null,
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onMore: () -> Unit = {},
    onSubtaskToggle: (String) -> Unit = {},
    onAddSubtask: () -> Unit = {},
    dependencies: List<Task> = emptyList(),
    dependentTasks: List<Task> = emptyList(),
    onRemoveDependency: (String) -> Unit = {},
    onAddDependency: () -> Unit = {},
    onAddJournalEntry: () -> Unit = {},
    onAddComment: (String) -> Unit = {},
    onStatusClick: () -> Unit = {},
    onAssigneeClick: () -> Unit = {},
    onDeleteTask: () -> Unit = {},
    onStartTimer: () -> Unit = {},
    onStopTimer: () -> Unit = {},
    isTimerRunning: Boolean = false
) {
    val completedSubtasks = task.subtasks.count { it.completed }
    var showDropdown by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ColorTokens.ReactTheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = task.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorTokens.ReactTheme.foreground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ColorTokens.ReactTheme.foreground
                        )
                    }
                },
                actions = {
                    // Edit button
                    PermissionGated(
                        permission = Permission.EDIT_ANY_TASK,
                        currentMember = currentMember,
                        action = "Edit task"
                    ) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit task",
                                tint = ColorTokens.ReactTheme.foreground
                            )
                        }
                    }

                    // Overflow menu
                    Box {
                        IconButton(onClick = { showDropdown = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More actions",
                                tint = ColorTokens.ReactTheme.mutedForeground
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorTokens.ReactTheme.card,
                    scrolledContainerColor = ColorTokens.ReactTheme.card
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {

            // ── Hero Card ──────────────────────────────────────────────────
            item {
                SectionCard {
                    // Large task title
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground,
                        lineHeight = 32.sp
                    )

                    // Badge row: project chip + status + priority
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Project chip (small outlined)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, ColorTokens.ReactTheme.border)
                        ) {
                            Text(
                                text = task.projectName,
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTokens.ReactTheme.mutedForeground,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Status badge (clickable to change)
                        Row(
                            modifier = Modifier.clickable { onStatusClick() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            TaskStatusDetailBadgeReact(status = task.status)
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Change status",
                                tint = ColorTokens.ReactTheme.mutedForeground,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Priority badge
                        TaskPriorityDetailBadgeReact(priority = task.priority, showLabel = true)
                    }
                }
            }

            // ── Meta Card ─────────────────────────────────────────────────
            item {
                SectionCard(title = "DETAILS") {
                    // 2-column layout using two rows of meta items
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
                    ) {
                        // Assignee
                        MetaDetailItem(
                            icon = Icons.Default.Person,
                            label = "Assignee",
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAssigneeClick() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(ColorTokens.ReactTheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = task.assignee.avatar,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = task.assignee.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorTokens.ReactTheme.foreground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Due date
                        MetaDetailItem(
                            icon = Icons.Default.CalendarToday,
                            label = "Due Date",
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = task.dueDate,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = ColorTokens.ReactTheme.foreground
                            )
                        }
                    }

                    HorizontalDivider(color = ColorTokens.ReactTheme.border.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
                    ) {
                        // Created date
                        MetaDetailItem(
                            icon = Icons.Default.AccessTime,
                            label = "Created",
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = task.createdAt,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = ColorTokens.ReactTheme.foreground
                            )
                        }

                        // Estimated hours
                        MetaDetailItem(
                            icon = Icons.Default.Schedule,
                            label = "Estimate",
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = task.timeEstimate,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = ColorTokens.ReactTheme.foreground
                            )
                        }
                    }
                }
            }

            // ── Description Card ──────────────────────────────────────────
            item {
                SectionCard(title = "DESCRIPTION") {
                    if (task.description.isBlank()) {
                        Text(
                            text = "(No description)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    } else {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTokens.ReactTheme.mutedForeground,
                            lineHeight = 22.4.sp
                        )
                    }
                }
            }

            // ── Subtasks Card (always visible) ──────────────────────────
            item {
                SectionCard(title = "SUBTASKS") {
                    // Header with count + add button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (task.subtasks.isNotEmpty()) {
                            Text(
                                text = "$completedSubtasks / ${task.subtasks.size} completed",
                                style = MaterialTheme.typography.labelMedium,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        } else {
                            Text(
                                text = "No subtasks yet",
                                style = MaterialTheme.typography.labelMedium,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
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

                    if (task.subtasks.isNotEmpty()) {
                        // Progress bar
                        LinearProgressIndicator(
                            progress = { completedSubtasks.toFloat() / task.subtasks.size },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = ColorTokens.ReactTheme.primary,
                            trackColor = ColorTokens.ReactTheme.border
                        )

                        // Parent task indicator + subtask rows with connecting line
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            // Parent task reference
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = ColorTokens.ReactTheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorTokens.ReactTheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Subtask rows with indent + connecting dots
                            task.subtasks.forEach { subtask ->
                                Row(
                                    modifier = Modifier.padding(start = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Connecting dot
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (subtask.completed) ColorTokens.ReactTheme.primary
                                                else ColorTokens.ReactTheme.border
                                            )
                                    )
                                    Box(modifier = Modifier.weight(1f)) {
                                        SubtaskItem(
                                            subtask = subtask,
                                            onToggle = { onSubtaskToggle(subtask.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Dependencies Card ─────────────────────────────────────────
            if (dependencies.isNotEmpty() || dependentTasks.isNotEmpty()) {
                item {
                    SectionCard(title = "DEPENDENCIES") {
                        // Header with add button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = ColorTokens.ReactTheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Task Dependencies",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ColorTokens.ReactTheme.mutedForeground
                                )
                            }
                            IconButton(
                                onClick = onAddDependency,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add dependency",
                                    tint = ColorTokens.ReactTheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Blocked-by section
                        if (dependencies.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Blocked by (${dependencies.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                dependencies.forEach { dep ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Block,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = dep.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ColorTokens.ReactTheme.foreground,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        // Status badge for dependency
                                        DependencyStatusBadge(status = dep.status.name)
                                        Spacer(Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { onRemoveDependency(dep.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = ColorTokens.ReactTheme.mutedForeground,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Divider between sections
                        if (dependencies.isNotEmpty() && dependentTasks.isNotEmpty()) {
                            HorizontalDivider(
                                color = ColorTokens.ReactTheme.border.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // Blocking section
                        if (dependentTasks.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Blocking (${dependentTasks.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                dependentTasks.forEach { dep ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = dep.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ColorTokens.ReactTheme.foreground,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        // Status badge for dependent task
                                        DependencyStatusBadge(status = dep.status.name)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Time Tracking Card ────────────────────────────────────────
            item {
                SectionCard(title = "TIME TRACKING") {
                    // Tracked vs estimated row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorTokens.ReactTheme.mutedForeground
                                )
                                Text(
                                    text = task.timeTracked,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorTokens.ReactTheme.foreground
                                )
                            }
                        }

                        // Vertical divider
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
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                            Text(
                                text = task.timeEstimate,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = ColorTokens.ReactTheme.foreground
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Start / Stop timer button
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

            // ── Activity Card ─────────────────────────────────────────────
            item {
                SectionCard(title = "ACTIVITY") {
                    // Header action row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activities.isEmpty()) "No activity yet" else "${activities.size} event(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorTokens.ReactTheme.mutedForeground
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

                    ActivityTimeline(
                        activities = activities,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Comments Card ─────────────────────────────────────────────
            item {
                CommentsCard(
                    comments = comments,
                    currentUserName = currentUserName,
                    currentUserAvatar = currentUserAvatar,
                    onAddComment = onAddComment
                )
            }

            // Bottom spacing
            item { Spacer(Modifier.height(Tokens.Spacing.md)) }
        }
    }
}

// ─── Private helper composables ───────────────────────────────────────────────

/**
 * A single meta detail cell: icon + label + value slot.
 * Used in the 2-column Details card.
 */
@Composable
private fun MetaDetailItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
        value()
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
        // Custom checkbox
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

        Text(
            text = subtask.title,
            fontSize = 14.sp,
            color = if (subtask.completed) ColorTokens.ReactTheme.mutedForeground else ColorTokens.ReactTheme.foreground,
            textDecoration = if (subtask.completed) TextDecoration.LineThrough else null
        )
    }
}

@Composable
private fun DependencyStatusBadge(status: String) {
    val (color, label) = when (status.uppercase()) {
        "DONE", "COMPLETED" -> Color(0xFF34D399) to "Done"
        "IN_PROGRESS" -> Color(0xFF60A5FA) to "In Progress"
        "TODO" -> ColorTokens.ReactTheme.mutedForeground to "To Do"
        else -> ColorTokens.ReactTheme.mutedForeground to status.lowercase().replaceFirstChar { it.uppercase() }
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun CommentsCard(
    comments: List<com.example.kosmos.core.models.TaskComment> = emptyList(),
    currentUserName: String = "You",
    currentUserAvatar: String = "Y",
    onAddComment: (String) -> Unit = {}
) {
    var commentText by remember { mutableStateOf("") }

    SectionCard(title = "COMMENTS") {
        // Section label with count
        Text(
            text = if (comments.isEmpty()) "No comments yet" else "${comments.size} comment(s)",
            style = MaterialTheme.typography.labelSmall,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        // Comment input row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Current user avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUserAvatar,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

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

                if (commentText.isNotBlank()) {
                    Button(
                        onClick = {
                            onAddComment(commentText)
                            commentText = ""
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

        // Existing comments
        if (comments.isNotEmpty()) {
            HorizontalDivider(color = ColorTokens.ReactTheme.border)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                comments.forEach { comment ->
                    CommentItem(comment = comment)
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
            Color(0xFF3F3F46),
            ColorTokens.ReactTheme.foreground,
            "To Do"
        )
        TaskStatusDetail.IN_PROGRESS -> Triple(
            ColorTokens.ReactTheme.primary.copy(alpha = 0.2f),
            ColorTokens.ReactTheme.primary,
            "In Progress"
        )
        TaskStatusDetail.DONE -> Triple(
            Color(0x3310B981),
            Color(0xFF10B981),
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
            Color(0xFF3F3F46),
            ColorTokens.ReactTheme.foreground,
            Icons.Default.Remove,
            "Low"
        )
        TaskPriorityDetail.MEDIUM -> Quadruple(
            Color(0x33F59E0B),
            Color(0xFFF59E0B),
            Icons.Default.Warning,
            "Medium"
        )
        TaskPriorityDetail.HIGH -> Quadruple(
            Color(0x33EF4444),
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

// ─── Helpers ──────────────────────────────────────────────────────────────────

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
