package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import java.text.SimpleDateFormat
import java.util.*

/**
 * Project Activity Screen - React Design Implementation
 *
 * Shows timeline of project activities:
 * - Task updates (created, status changed, assigned)
 * - Member updates (joined, role changed, left)
 * - Chat updates (new chat created, message milestones)
 * - Project updates (name changed, settings updated)
 *
 * Design matches React theme with timeline visualization
 */

enum class ProjectActivityType {
    TASK_CREATED,
    TASK_STATUS_CHANGED,
    TASK_ASSIGNED,
    MEMBER_JOINED,
    MEMBER_ROLE_CHANGED,
    MEMBER_LEFT,
    CHAT_CREATED,
    PROJECT_UPDATED
}

data class ProjectActivityItem(
    val id: String,
    val type: ProjectActivityType,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val title: String,
    val description: String,
    val timestamp: Long
)

@Composable
fun ProjectActivityScreenReact(
    activities: List<ProjectActivityItem> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Use Column with custom top bar (NO Scaffold - avoids nesting with MainActivity's Scaffold)
    Column(modifier = modifier.fillMaxSize()) {
        // Custom top bar
        Surface(
            color = ColorTokens.ReactTheme.card,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = ColorTokens.ReactTheme.border,
                    shape = RectangleShape
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Activity",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    )
                    Text(
                        text = "${activities.size} activities",
                        fontSize = 14.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }
        }

        // Content area - respects bottom nav automatically
        if (activities.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorTokens.ReactTheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ColorTokens.ReactTheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = ColorTokens.ReactTheme.mutedForeground,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "No activity yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    )
                    Text(
                        text = "Activity will appear here",
                        fontSize = 14.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }
        } else {
            // Activity timeline
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorTokens.ReactTheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(activities, key = { it.id }) { activity ->
                    ActivityTimelineItem(activity = activity)
                }
            }
        }
    }
}

@Composable
private fun ActivityTimelineItem(activity: ProjectActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline indicator (left side)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getActivityColor(activity.type)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getActivityIcon(activity.type),
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.primaryForeground,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Timeline line (vertical connector)
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(ColorTokens.ReactTheme.border)
            )
        }

        // Activity content
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = ColorTokens.ReactTheme.card
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = ColorTokens.ReactTheme.border
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(Tokens.CornerRadius.md)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header: user name + timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User avatar
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ColorTokens.ReactTheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activity.userAvatar,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorTokens.ReactTheme.primaryForeground
                            )
                        }

                        Text(
                            text = activity.userName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorTokens.ReactTheme.foreground
                        )
                    }

                    Text(
                        text = formatActivityTime(activity.timestamp),
                        fontSize = 12.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }

                // Title
                Text(
                    text = activity.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTokens.ReactTheme.foreground
                )

                // Description
                if (activity.description.isNotBlank()) {
                    Text(
                        text = activity.description,
                        fontSize = 14.sp,
                        color = ColorTokens.ReactTheme.mutedForeground,
                        lineHeight = 19.6.sp
                    )
                }
            }
        }
    }
}

private fun getActivityIcon(type: ProjectActivityType): ImageVector {
    return when (type) {
        ProjectActivityType.TASK_CREATED -> Icons.Default.Add
        ProjectActivityType.TASK_STATUS_CHANGED -> Icons.Default.Update
        ProjectActivityType.TASK_ASSIGNED -> Icons.Default.Assignment
        ProjectActivityType.MEMBER_JOINED -> Icons.Default.PersonAdd
        ProjectActivityType.MEMBER_ROLE_CHANGED -> Icons.Default.AdminPanelSettings
        ProjectActivityType.MEMBER_LEFT -> Icons.Default.PersonRemove
        ProjectActivityType.CHAT_CREATED -> Icons.Default.Chat
        ProjectActivityType.PROJECT_UPDATED -> Icons.Default.Edit
    }
}

private fun getActivityColor(type: ProjectActivityType): androidx.compose.ui.graphics.Color {
    return when (type) {
        ProjectActivityType.TASK_CREATED -> ColorTokens.Success.dark
        ProjectActivityType.TASK_STATUS_CHANGED -> ColorTokens.ReactTheme.primary
        ProjectActivityType.TASK_ASSIGNED -> ColorTokens.ReactTheme.primary
        ProjectActivityType.MEMBER_JOINED -> ColorTokens.Success.dark
        ProjectActivityType.MEMBER_ROLE_CHANGED -> ColorTokens.ReactTheme.primary
        ProjectActivityType.MEMBER_LEFT -> ColorTokens.ReactTheme.destructive
        ProjectActivityType.CHAT_CREATED -> ColorTokens.ReactTheme.primary
        ProjectActivityType.PROJECT_UPDATED -> ColorTokens.ReactTheme.primary
    }
}

private fun formatActivityTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
