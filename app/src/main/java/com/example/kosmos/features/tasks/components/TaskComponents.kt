package com.example.kosmos.features.tasks.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.features.gestures.enhancedClick
import com.example.kosmos.shared.ui.layouts.SwipeActions

/**
 * Enhanced Task Components
 *
 * Components for task management with:
 * - Task cards with status indicators
 * - Priority badges
 * - Quick actions (swipe, long-press)
 * - Board view cards
 * - Task filters
 * - Status chips
 */

/**
 * Task Status Enum
 */
enum class TaskStatus {
    TODO, IN_PROGRESS, DONE, CANCELLED
}

/**
 * Task Priority Enum
 */
enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}

/**
 * Enhanced Task Card
 *
 * Task card with swipe actions and quick interactions
 *
 * @param task Task item data
 * @param onClick Task click handler
 * @param onStatusChange Status change handler
 * @param onDelete Delete handler
 * @param onEdit Edit handler
 * @param modifier Modifier
 * @param showProject Whether to show project name
 */
@Composable
fun EnhancedTaskCard(
    task: TaskItem,
    onClick: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    showProject: Boolean = false
) {
    SwipeActions(
        onSwipeLeft = onDelete,
        onSwipeRight = {
            when (task.status) {
                TaskStatus.TODO -> onStatusChange(TaskStatus.IN_PROGRESS)
                TaskStatus.IN_PROGRESS -> onStatusChange(TaskStatus.DONE)
                TaskStatus.DONE -> onStatusChange(TaskStatus.TODO)
                TaskStatus.CANCELLED -> onStatusChange(TaskStatus.TODO)
            }
        },
        leftIcon = IconSet.Action.delete,
        leftLabel = "Delete",
        leftColor = ColorTokens.Error.light,
        rightIcon = when (task.status) {
            TaskStatus.TODO -> Icons.Filled.PlayArrow
            TaskStatus.IN_PROGRESS -> IconSet.Status.checkmark
            TaskStatus.DONE -> Icons.AutoMirrored.Filled.Undo
            TaskStatus.CANCELLED -> Icons.AutoMirrored.Filled.Undo
        },
        rightLabel = when (task.status) {
            TaskStatus.TODO -> "Start"
            TaskStatus.IN_PROGRESS -> "Complete"
            TaskStatus.DONE -> "Reopen"
            TaskStatus.CANCELLED -> "Reopen"
        },
        rightColor = ColorTokens.Primary.light
    ) {
        TaskCardContent(
            task = task,
            onClick = onClick,
            onEdit = onEdit,
            modifier = modifier,
            showProject = showProject
        )
    }
}

/**
 * Task Card Content
 */
@Composable
private fun TaskCardContent(
    task: TaskItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    showProject: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Tokens.Elevation.level1
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority badge
                PriorityBadge(priority = task.priority)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status chip
                    StatusChip(status = task.status)

                    // Edit button
                    IconButtonStandard(
                        icon = IconSet.Action.edit,
                        onClick = onEdit,
                        contentDescription = "Edit task"
                    )
                }
            }

            // Task title
            Text(
                text = task.title,
                style = TypographyTokens.typography.titleMedium,
                color = if (task.status == TaskStatus.DONE)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.status == TaskStatus.DONE)
                    TextDecoration.LineThrough
                else
                    null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Task description (if present)
            if (!task.description.isNullOrBlank()) {
                Text(
                    text = task.description,
                    style = TypographyTokens.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Project name (if shown)
                if (showProject && task.projectName != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = IconSet.Navigation.projects,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Tokens.Size.iconSmall)
                        )
                        Text(
                            text = task.projectName,
                            style = TypographyTokens.Custom.caption,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Due date (if present)
                if (task.dueDate != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = IconSet.Time.calendar,
                            contentDescription = null,
                            tint = if (task.isOverdue)
                                ColorTokens.Error.light
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Tokens.Size.iconSmall)
                        )
                        Text(
                            text = task.dueDate,
                            style = TypographyTokens.Custom.caption,
                            color = if (task.isOverdue)
                                ColorTokens.Error.light
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Assignee count (if present)
                if (task.assigneeCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = IconSet.User.profile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Tokens.Size.iconSmall)
                        )
                        Text(
                            text = task.assigneeCount.toString(),
                            style = TypographyTokens.Custom.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Board Task Card (Compact for Kanban)
 *
 * Simplified task card for board view
 *
 * @param task Task item
 * @param onClick Click handler
 * @param modifier Modifier
 */
@Composable
fun BoardTaskCard(
    task: TaskItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Tokens.Elevation.level1
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
        ) {
            // Priority indicator
            if (task.priority != TaskPriority.LOW) {
                PriorityBadge(priority = task.priority, compact = true)
            }

            // Task title
            Text(
                text = task.title,
                style = TypographyTokens.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Bottom info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Due date
                if (task.dueDate != null) {
                    Text(
                        text = task.dueDate,
                        style = TypographyTokens.Custom.caption,
                        color = if (task.isOverdue)
                            ColorTokens.Error.light
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Assignee indicator
                if (task.assigneeCount > 0) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = task.assigneeCount.toString(),
                                style = TypographyTokens.Custom.caption,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Priority Badge
 */
@Composable
fun PriorityBadge(
    priority: TaskPriority,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val (color, label) = when (priority) {
        TaskPriority.URGENT -> ColorTokens.Priority.urgent to "Urgent"
        TaskPriority.HIGH -> ColorTokens.Priority.high to "High"
        TaskPriority.MEDIUM -> ColorTokens.Priority.medium to "Medium"
        TaskPriority.LOW -> ColorTokens.Priority.low to "Low"
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) Tokens.Spacing.xxs else Tokens.Spacing.xs,
                vertical = Tokens.Spacing.xxs
            ),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority dot
            Surface(
                modifier = Modifier.size(6.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = color
            ) {}

            // Priority label
            if (!compact) {
                Text(
                    text = label,
                    style = TypographyTokens.Custom.caption,
                    color = color
                )
            }
        }
    }
}

/**
 * Status Chip
 */
@Composable
fun StatusChip(
    status: TaskStatus,
    modifier: Modifier = Modifier
) {
    val (color, icon, label) = when (status) {
        TaskStatus.TODO -> Triple(
            ColorTokens.TaskStatus.todo,
            IconSet.Task.radioButtonUnchecked,
            "To Do"
        )
        TaskStatus.IN_PROGRESS -> Triple(
            ColorTokens.TaskStatus.inProgress,
            IconSet.Task.task,
            "In Progress"
        )
        TaskStatus.DONE -> Triple(
            ColorTokens.TaskStatus.done,
            IconSet.Task.checkCircle,
            "Done"
        )
        TaskStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            IconSet.Navigation.close,
            "Cancelled"
        )
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Tokens.Spacing.xs,
                vertical = Tokens.Spacing.xxs
            ),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(Tokens.Size.iconSmall)
            )
            Text(
                text = label,
                style = TypographyTokens.Custom.caption,
                color = color
            )
        }
    }
}

/**
 * Task Filter Chips
 */
@Composable
fun TaskFilterChips(
    selectedStatus: TaskStatus?,
    selectedPriority: TaskPriority?,
    onStatusSelected: (TaskStatus?) -> Unit,
    onPrioritySelected: (TaskPriority?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
    ) {
        // Status filters
        Text(
            text = "Status",
            style = TypographyTokens.Custom.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Tokens.Spacing.xs)
        )

        ChipGroup(
            chips = listOf("All") + TaskStatus.values().map { it.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() } },
            selectedChips = setOf(
                if (selectedStatus == null) "All"
                else selectedStatus.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() }
            ),
            onChipClick = { statusName ->
                if (statusName == "All") {
                    onStatusSelected(null)
                } else {
                    val status = TaskStatus.values().find {
                        it.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() } == statusName
                    }
                    onStatusSelected(status)
                }
            },
            multiSelect = false
        )

        // Priority filters
        Text(
            text = "Priority",
            style = TypographyTokens.Custom.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Tokens.Spacing.xs)
        )

        ChipGroup(
            chips = listOf("All") + TaskPriority.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
            selectedChips = setOf(
                if (selectedPriority == null) "All"
                else selectedPriority.name.lowercase().replaceFirstChar { c -> c.uppercase() }
            ),
            onChipClick = { priorityName ->
                if (priorityName == "All") {
                    onPrioritySelected(null)
                } else {
                    val priority = TaskPriority.values().find {
                        it.name.lowercase().replaceFirstChar { c -> c.uppercase() } == priorityName
                    }
                    onPrioritySelected(priority)
                }
            },
            multiSelect = false
        )
    }
}

/**
 * Task Item Data Class
 */
data class TaskItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: TaskStatus,
    val priority: TaskPriority,
    val dueDate: String? = null,
    val isOverdue: Boolean = false,
    val projectId: String? = null,
    val projectName: String? = null,
    val assigneeCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
