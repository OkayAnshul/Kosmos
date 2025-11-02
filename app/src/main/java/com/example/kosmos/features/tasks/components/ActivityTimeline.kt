package com.example.kosmos.features.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity Timeline Component — Professional Design
 *
 * Minimal vertical timeline with:
 * - Colored action icons (type-based)
 * - Actor name (bold) + action description inline
 * - Field changes as compact pills
 * - Commit messages as subtle quote blocks
 * - Relative timestamps right-aligned
 */
@Composable
fun ActivityTimeline(
    activities: List<TaskActivity>,
    onLoadMore: (() -> Unit)? = null,
    hasMore: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (activities.isEmpty()) {
        EmptyActivityState(modifier)
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        val sorted = activities.sortedByDescending { it.timestamp }
        sorted.forEachIndexed { index, activity ->
            ActivityTimelineItem(
                activity = activity,
                isLast = index == sorted.lastIndex
            )
        }

        if (hasMore) {
            TextButton(
                onClick = { onLoadMore?.invoke() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Tokens.Spacing.sm)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = ColorTokens.ReactTheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = if (isLoading) "Loading..." else "Load older activity",
                    style = TypographyTokens.typography.labelMedium,
                    color = ColorTokens.ReactTheme.primary
                )
            }
        }
    }
}

@Composable
private fun ActivityTimelineItem(
    activity: TaskActivity,
    isLast: Boolean
) {
    val (icon, iconColor) = getActivityIcon(activity.actionType.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Tokens.Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline rail: icon dot + connector line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Connector line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(40.dp)
                        .background(ColorTokens.ReactTheme.border)
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 8.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Main line: "Alice changed status to In Progress" + timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = ColorTokens.ReactTheme.foreground)) {
                            append(activity.actorName)
                        }
                        append(" ")
                        withStyle(SpanStyle(color = ColorTokens.ReactTheme.mutedForeground)) {
                            append(activity.autoDescription)
                        }
                    },
                    style = TypographyTokens.typography.bodySmall,
                    modifier = Modifier.weight(1f, fill = false),
                    lineHeight = TypographyTokens.typography.bodySmall.lineHeight
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = formatRelativeTime(activity.timestamp),
                    style = TypographyTokens.typography.labelSmall,
                    color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.7f)
                )
            }

            // Field changes as compact row
            if (activity.changes.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activity.changes.take(3).forEach { change ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ColorTokens.ReactTheme.secondary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatFieldName(change.field),
                                    style = TypographyTokens.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorTokens.ReactTheme.mutedForeground
                                )
                                Text(
                                    text = change.getFormattedFromValue(),
                                    style = TypographyTokens.typography.labelSmall,
                                    color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "\u2192",
                                    style = TypographyTokens.typography.labelSmall,
                                    color = ColorTokens.ReactTheme.mutedForeground
                                )
                                Text(
                                    text = change.getFormattedToValue(),
                                    style = TypographyTokens.typography.labelSmall,
                                    color = ColorTokens.Status.online.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                    if (activity.changes.size > 3) {
                        Text(
                            text = "+${activity.changes.size - 3}",
                            style = TypographyTokens.typography.labelSmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }
            }

            // Commit message as subtle quote
            if (!activity.commitMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(IntrinsicSize.Min)
                            .background(ColorTokens.ReactTheme.primary.copy(alpha = 0.4f))
                    )
                    Text(
                        text = activity.commitMessage,
                        style = TypographyTokens.typography.bodySmall,
                        color = ColorTokens.ReactTheme.foreground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Get icon and color for activity type
 */
private fun getActivityIcon(action: String): Pair<ImageVector, Color> {
    return when {
        action.contains("status", ignoreCase = true) -> Icons.Default.SwapHoriz to Color(0xFF60A5FA) // blue
        action.contains("priority", ignoreCase = true) -> Icons.Default.Flag to Color(0xFFFBBF24) // amber
        action.contains("assign", ignoreCase = true) -> Icons.Default.PersonAdd to Color(0xFF34D399) // green
        action.contains("comment", ignoreCase = true) -> Icons.Default.ChatBubbleOutline to Color(0xFFA78BFA) // purple
        action.contains("created", ignoreCase = true) -> Icons.Default.Add to Color(0xFF34D399) // green
        action.contains("subtask", ignoreCase = true) -> Icons.Default.CheckCircle to Color(0xFF34D399) // green
        action.contains("due", ignoreCase = true) || action.contains("date", ignoreCase = true) -> Icons.Default.CalendarToday to Color(0xFFFB923C) // orange
        action.contains("delete", ignoreCase = true) -> Icons.Default.Delete to Color(0xFFEF4444) // red
        action.contains("tag", ignoreCase = true) -> Icons.Default.Sell to Color(0xFF60A5FA) // blue
        action.contains("description", ignoreCase = true) || action.contains("title", ignoreCase = true) -> Icons.Default.Edit to Color(0xFF94A3B8) // slate
        action.contains("time", ignoreCase = true) || action.contains("hour", ignoreCase = true) -> Icons.Default.Schedule to Color(0xFFFBBF24) // amber
        else -> Icons.Default.FiberManualRecord to ColorTokens.ReactTheme.mutedForeground
    }
}

@Composable
private fun EmptyActivityState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Tokens.Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Timeline,
            contentDescription = null,
            tint = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.4f),
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = "No activity yet",
            style = TypographyTokens.typography.bodyMedium,
            color = ColorTokens.ReactTheme.mutedForeground
        )
    }
}

private fun formatFieldName(field: String): String = when (field) {
    "status" -> "Status"
    "priority" -> "Priority"
    "assignedTo" -> "Assigned"
    "title" -> "Title"
    "description" -> "Desc"
    "dueDate" -> "Due"
    "tags" -> "Tags"
    "estimatedHours" -> "Est."
    "actualHours" -> "Actual"
    else -> field.replaceFirstChar { it.uppercase() }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 172_800_000 -> "1d"
        diff < 604_800_000 -> "${diff / 86_400_000}d"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
