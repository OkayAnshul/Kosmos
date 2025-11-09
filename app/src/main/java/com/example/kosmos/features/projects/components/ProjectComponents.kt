package com.example.kosmos.features.projects.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.layouts.SwipeActions
import com.example.kosmos.shared.ui.utils.toRelativeTime

/**
 * Enhanced Project Components
 *
 * Components for project management with:
 * - Project cards with stats
 * - Member avatars
 * - Activity indicators
 * - Quick actions
 * - Progress indicators
 */

/**
 * Enhanced Project Card
 *
 * Project card with swipe actions and stats
 *
 * @param project Project item data
 * @param onClick Project click handler
 * @param onArchive Archive handler
 * @param onEdit Edit handler
 * @param modifier Modifier
 */
@Composable
fun EnhancedProjectCard(
    project: ProjectItem,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeActions(
        onSwipeLeft = onArchive,
        onSwipeRight = onEdit,
        leftIcon = IconSet.Message.archive,
        leftLabel = if (project.isArchived) "Unarchive" else "Archive",
        leftColor = ColorTokens.Primary.light,
        rightIcon = IconSet.Action.edit,
        rightLabel = "Edit",
        rightColor = ColorTokens.Primary.light
    ) {
        ProjectCardContent(
            project = project,
            onClick = onClick,
            modifier = modifier
        )
    }
}

/**
 * Project Card Content
 */
@Composable
private fun ProjectCardContent(
    project: ProjectItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Tokens.Elevation.level2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
                ) {
                    // Project name
                    Text(
                        text = project.name,
                        style = TypographyTokens.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Project description
                    if (!project.description.isNullOrBlank()) {
                        Text(
                            text = project.description,
                            style = TypographyTokens.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status indicator
                if (project.hasUnread || project.isActive) {
                    Box {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = if (project.hasUnread)
                                ColorTokens.Status.online
                            else
                                ColorTokens.TaskStatus.inProgress
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                // Members
                StatItem(
                    icon = IconSet.User.profile,
                    value = project.memberCount.toString(),
                    label = "members"
                )

                // Chats
                StatItem(
                    icon = IconSet.Navigation.chats,
                    value = project.chatCount.toString(),
                    label = "chats",
                    hasBadge = project.unreadChatCount > 0,
                    badgeValue = project.unreadChatCount
                )

                // Tasks - Show total count, no badge (badge shows pending in Overview tab)
                StatItem(
                    icon = IconSet.Navigation.tasks,
                    value = project.taskCount.toString(),
                    label = "tasks"
                )
            }

            // Progress bar (if tasks exist)
            if (project.taskCount > 0) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progress",
                            style = TypographyTokens.Custom.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${project.completedTaskCount}/${project.taskCount}",
                            style = TypographyTokens.Custom.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = {
                            if (project.taskCount > 0)
                                project.completedTaskCount.toFloat() / project.taskCount
                            else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Last activity
            if (project.lastActivityTimestamp != null) {
                Text(
                    text = "Last activity: ${project.lastActivityTimestamp.toRelativeTime()}",
                    style = TypographyTokens.Custom.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Stat Item
 */
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    hasBadge: Boolean = false,
    badgeValue: Int = 0
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Tokens.Size.iconSmall)
        )

        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = TypographyTokens.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (hasBadge && badgeValue > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = if (badgeValue > 99) "99+" else badgeValue.toString(),
                            style = TypographyTokens.Custom.badgeNumber
                        )
                    }
                }
            }

            Text(
                text = label,
                style = TypographyTokens.Custom.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Compact Project Card (for lists)
 */
@Composable
fun CompactProjectCard(
    project: ProjectItem,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = IconSet.Navigation.projects,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(Tokens.Size.iconMedium)
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
            ) {
                Text(
                    text = project.name,
                    style = TypographyTokens.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                ) {
                    Text(
                        text = "${project.memberCount} members",
                        style = TypographyTokens.Custom.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "•",
                        style = TypographyTokens.Custom.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${project.taskCount} tasks",
                        style = TypographyTokens.Custom.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Unread indicator
            if (project.hasUnread) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = (project.unreadChatCount + project.pendingTaskCount).toString(),
                        style = TypographyTokens.Custom.badgeNumber
                    )
                }
            }

            Icon(
                imageVector = IconSet.Direction.right,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Tokens.Size.iconSmall)
            )
        }
    }
}

/**
 * Project Member Avatar Group
 */
@Composable
fun ProjectMemberAvatars(
    members: List<ProjectMember>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-8).dp)
    ) {
        members.take(maxVisible).forEach { member ->
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = member.initials,
                        style = TypographyTokens.Custom.caption,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Overflow indicator
        if (members.size > maxVisible) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+${members.size - maxVisible}",
                        style = TypographyTokens.Custom.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Project Activity Item
 */
@Composable
fun ProjectActivityItem(
    activity: ProjectActivity,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Tokens.Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
    ) {
        // Activity icon
        Surface(
            shape = MaterialTheme.shapes.small,
            color = activity.iconColor.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = activity.icon,
                    contentDescription = null,
                    tint = activity.iconColor,
                    modifier = Modifier.size(Tokens.Size.iconMedium)
                )
            }
        }

        // Activity info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
        ) {
            Text(
                text = activity.message,
                style = TypographyTokens.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = activity.timestamp,
                style = TypographyTokens.Custom.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Project Item Data Class
 */
data class ProjectItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val memberCount: Int,
    val chatCount: Int,
    val taskCount: Int,
    val completedTaskCount: Int,
    val unreadChatCount: Int = 0,
    val pendingTaskCount: Int = 0,
    val hasUnread: Boolean = unreadChatCount > 0 || pendingTaskCount > 0,
    val isActive: Boolean = false,
    val isArchived: Boolean = false,
    val lastActivityTimestamp: Long? = null, // Changed from String to Long for timestamp formatting
    val createdAt: Long
)

/**
 * Project Member
 */
data class ProjectMember(
    val id: String,
    val name: String,
    val initials: String,
    val role: String? = null,
    val isOnline: Boolean = false
)

/**
 * Project Activity
 */
data class ProjectActivity(
    val id: String,
    val message: String,
    val timestamp: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: androidx.compose.ui.graphics.Color,
    val userId: String? = null
)
