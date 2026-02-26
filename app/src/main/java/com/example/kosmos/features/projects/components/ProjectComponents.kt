package com.example.kosmos.features.projects.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.kosmos.core.models.ProjectStatus
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.components.CoreCard
import com.example.kosmos.shared.ui.components.CardImportance
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
 * Project Card Content with Glassmorphic Design
 */
@Composable
private fun ProjectCardContent(
    project: ProjectItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CoreCard(
        onClick = onClick,
        importance = CardImportance.PRIMARY,
        modifier = modifier.fillMaxWidth()
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
                        color = ColorTokens.Surface.onLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Project description
                    if (!project.description.isNullOrBlank()) {
                        Text(
                            text = project.description,
                            style = TypographyTokens.typography.bodyMedium,
                            color = ColorTokens.Surface.onLightVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status badge
                ProjectStatusBadge(
                    status = when {
                        project.isArchived -> ProjectStatus.ARCHIVED
                        project.isActive -> ProjectStatus.ACTIVE
                        else -> ProjectStatus.ON_HOLD
                    },
                    size = com.example.kosmos.shared.ui.components.BadgeSize.SMALL
                )
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

            // Progress bar (if tasks exist) - Using Stitch ProgressBar component
            if (project.taskCount > 0) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
                ) {
                    Text(
                        text = "Task Progress",
                        style = TypographyTokens.Custom.caption,
                        color = ColorTokens.Surface.onLightVariant
                    )

                    ProgressBar(
                        progress = if (project.taskCount > 0)
                            project.completedTaskCount.toFloat() / project.taskCount
                        else 0f,
                        showPercentage = true,
                        height = 8
                    )
                }
            }

            // Last activity
            if (project.lastActivityTimestamp != null) {
                Text(
                    text = "Last activity: ${project.lastActivityTimestamp.toRelativeTime()}",
                    style = TypographyTokens.Custom.caption,
                    color = ColorTokens.Surface.onLightVariant
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
            contentDescription = "",
            tint = ColorTokens.Primary.light,
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
                    color = ColorTokens.Surface.onLight
                )

                if (hasBadge && badgeValue > 0) {
                    Badge(
                        containerColor = ColorTokens.ReactTheme.primary
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
                color = ColorTokens.Surface.onLightVariant
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
        color = ColorTokens.ReactTheme.card,
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
                color = ColorTokens.ReactTheme.secondary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = IconSet.Navigation.projects,
                        contentDescription = "",
                        tint = ColorTokens.ReactTheme.primaryForeground,
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
                    color = ColorTokens.ReactTheme.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                ) {
                    Text(
                        text = "${project.memberCount} members",
                        style = TypographyTokens.Custom.caption,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )

                    Text(
                        text = "•",
                        style = TypographyTokens.Custom.caption,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )

                    Text(
                        text = "${project.taskCount} tasks",
                        style = TypographyTokens.Custom.caption,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }

            // Unread indicator
            if (project.hasUnread) {
                Badge(
                    containerColor = ColorTokens.ReactTheme.primary
                ) {
                    Text(
                        text = (project.unreadChatCount + project.pendingTaskCount).toString(),
                        style = TypographyTokens.Custom.badgeNumber
                    )
                }
            }

            Icon(
                imageVector = IconSet.Direction.right,
                contentDescription = "",
                tint = ColorTokens.ReactTheme.mutedForeground,
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
                color = ColorTokens.ReactTheme.secondary,
                modifier = Modifier.size(32.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = ColorTokens.ReactTheme.card
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = member.initials,
                        style = TypographyTokens.Custom.caption,
                        color = ColorTokens.ReactTheme.primaryForeground
                    )
                }
            }
        }

        // Overflow indicator
        if (members.size > maxVisible) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = ColorTokens.ReactTheme.secondary,
                modifier = Modifier.size(32.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = ColorTokens.ReactTheme.card
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+${members.size - maxVisible}",
                        style = TypographyTokens.Custom.caption,
                        color = ColorTokens.ReactTheme.mutedForeground
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
                    contentDescription = "",
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
                color = ColorTokens.ReactTheme.foreground
            )

            Text(
                text = activity.timestamp,
                style = TypographyTokens.Custom.caption,
                color = ColorTokens.ReactTheme.mutedForeground
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
