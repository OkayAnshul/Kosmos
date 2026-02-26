package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.Task
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Task Management Bottom Sheet
 *
 * Animated modal bottom sheet with 4 management options:
 * 1. Update Status - Change task status
 * 2. Assign Task - Assign or reassign to team member
 * 3. Edit Details - Navigate to full edit screen
 * 4. Time Tracking - View/update time tracking info
 *
 * Animation: Slide up from bottom (300ms) with Material 3 default
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagementBottomSheet(
    task: Task,
    onUpdateStatus: () -> Unit,
    onAssignUser: () -> Unit,
    onEditDetails: () -> Unit,
    onViewTimeTracking: () -> Unit,
    onDeleteTask: () -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = ColorTokens.ReactTheme.card,
        contentColor = ColorTokens.ReactTheme.foreground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
        ) {
            // Header
            Column(
                modifier = Modifier.padding(horizontal = Tokens.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
            ) {
                Text(
                    text = "Task Management",
                    style = TypographyTokens.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = task.title,
                    style = TypographyTokens.typography.bodyMedium,
                    color = ColorTokens.ReactTheme.mutedForeground,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(Tokens.Spacing.md))

            // Option 1: Update Status
            ManagementOptionItem(
                icon = IconSet.Action.sync,
                label = "Update Status",
                description = "Change task status (To Do, In Progress, Done)",
                onClick = {
                    onUpdateStatus()
                    onDismiss()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Tokens.Spacing.md),
                color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.2f)
            )

            // Option 2: Assign Task
            ManagementOptionItem(
                icon = IconSet.User.personAdd,
                label = "Assign Task",
                description = "Assign or reassign to team member",
                onClick = {
                    onAssignUser()
                    onDismiss()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Tokens.Spacing.md),
                color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.2f)
            )

            // Option 3: Edit Details
            ManagementOptionItem(
                icon = IconSet.Action.edit,
                label = "Edit Details",
                description = "Edit description, tags, subtasks, and more",
                onClick = {
                    onEditDetails()
                    onDismiss()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Tokens.Spacing.md),
                color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.2f)
            )

            // Option 4: Time Tracking
            ManagementOptionItem(
                icon = IconSet.Time.schedule,
                label = "Time Tracking",
                description = "View elapsed time and update estimates",
                onClick = {
                    onViewTimeTracking()
                    onDismiss()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Tokens.Spacing.md),
                color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.2f)
            )

            // Option 5: Delete Task (destructive)
            Surface(
                onClick = {
                    onDeleteTask()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                color = ColorTokens.ReactTheme.card
            ) {
                Row(
                    modifier = Modifier.padding(Tokens.Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = IconSet.Action.delete,
                            contentDescription = null,
                            tint = ColorTokens.ReactTheme.destructive,
                            modifier = Modifier.padding(Tokens.Spacing.sm)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
                    ) {
                        Text(
                            text = "Delete Task",
                            style = TypographyTokens.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = ColorTokens.ReactTheme.destructive
                        )
                        Text(
                            text = "Permanently delete this task",
                            style = TypographyTokens.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                    Icon(
                        imageVector = IconSet.Direction.chevronRight,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Management Option Item
 *
 * Individual option in the bottom sheet with icon, label, and description
 */
@Composable
private fun ManagementOptionItem(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = ColorTokens.ReactTheme.card
    ) {
        Row(
            modifier = Modifier.padding(Tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Icon
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = ColorTokens.ReactTheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.primary,
                    modifier = Modifier.padding(Tokens.Spacing.sm)
                )
            }

            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
            ) {
                Text(
                    text = label,
                    style = TypographyTokens.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = description,
                    style = TypographyTokens.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            // Chevron icon
            Icon(
                imageVector = IconSet.Direction.chevronRight,
                contentDescription = null,
                tint = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
