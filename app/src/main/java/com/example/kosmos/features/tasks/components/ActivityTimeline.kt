package com.example.kosmos.features.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.features.users.presentation.components.UserAvatar
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Activity Timeline Component
 *
 * Displays a chronological list of task activities with:
 * - Actor avatar and name
 * - Auto-generated description
 * - Optional commit message (styled card)
 * - Field changes (before → after)
 * - Relative timestamps
 * - Load more pagination
 *
 * Pattern: Git-style activity log for task history
 */
@Composable
fun ActivityTimeline(
    activities: List<TaskActivity>,
    onLoadMore: (() -> Unit)? = null,
    hasMore: Boolean = false,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Activity",
                style = TypographyTokens.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ColorTokens.ReactTheme.foreground
            )

            if (activities.isNotEmpty()) {
                Text(
                    text = "${activities.size} ${if (activities.size == 1) "entry" else "entries"}",
                    style = TypographyTokens.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }
        }

        // Timeline items
        if (activities.isEmpty()) {
            // Empty state
            EmptyActivityState()
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                activities.sortedByDescending { it.timestamp }.forEach { activity ->
                    ActivityTimelineItem(
                        activity = activity,
                        isLast = activity == activities.lastOrNull()
                    )
                }

                // Load more button
                if (hasMore) {
                    LoadMoreButton(
                        onClick = { onLoadMore?.invoke() },
                        isLoading = isLoading
                    )
                }
            }
        }
    }
}

/**
 * Activity Timeline Item
 *
 * Individual activity entry with:
 * - Vertical timeline connector
 * - Actor avatar
 * - Description and timestamp
 * - Optional commit message
 * - Field changes display
 */
@Composable
private fun ActivityTimelineItem(
    activity: TaskActivity,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Tokens.Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        // Timeline column (avatar + vertical line)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Actor avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ColorTokens.ReactTheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                // Use first letter of actor name
                Text(
                    text = activity.actorName.firstOrNull()?.uppercase() ?: "?",
                    style = TypographyTokens.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ColorTokens.ReactTheme.primary
                )
            }

            // Vertical connector line (if not last item)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp)
                        .background(ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.2f))
                )
            }
        }

        // Content column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) Tokens.Spacing.sm else 0.dp),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
        ) {
            // Actor name and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.actorName,
                    style = TypographyTokens.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ColorTokens.ReactTheme.foreground,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = formatRelativeTime(activity.timestamp),
                    style = TypographyTokens.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            // Auto-generated description
            Text(
                text = activity.autoDescription,
                style = TypographyTokens.typography.bodyMedium,
                color = ColorTokens.ReactTheme.mutedForeground
            )

            // Field changes (if any)
            if (activity.changes.isNotEmpty()) {
                FieldChangesDisplay(
                    changes = activity.changes,
                    modifier = Modifier.padding(top = Tokens.Spacing.xxs)
                )
            }

            // Commit message (if provided)
            if (!activity.commitMessage.isNullOrBlank()) {
                CommitMessageCard(
                    message = activity.commitMessage,
                    modifier = Modifier.padding(top = Tokens.Spacing.xs)
                )
            }
        }
    }
}

/**
 * Field Changes Display
 *
 * Shows before → after changes for each modified field
 */
@Composable
private fun FieldChangesDisplay(
    changes: List<com.example.kosmos.core.models.FieldChange>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = ColorTokens.ReactTheme.card.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(Tokens.Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
        ) {
            changes.forEach { change ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Field name
                    Text(
                        text = formatFieldName(change.field),
                        style = TypographyTokens.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.width(80.dp)
                    )

                    // From value
                    Text(
                        text = change.getFormattedFromValue(),
                        style = TypographyTokens.typography.bodySmall,
                        color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )

                    // Arrow
                    Icon(
                        imageVector = IconSet.Direction.arrowForward,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.size(16.dp)
                    )

                    // To value
                    Text(
                        text = change.getFormattedToValue(),
                        style = TypographyTokens.typography.bodySmall,
                        color = ColorTokens.Status.online.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Commit Message Card
 *
 * Displays user-provided commit message in a styled card
 */
@Composable
private fun CommitMessageCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ColorTokens.ReactTheme.primary.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = ColorTokens.ReactTheme.primary.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(Tokens.Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = IconSet.Message.chatBubble,
                contentDescription = null,
                tint = ColorTokens.ReactTheme.primary,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = message,
                style = TypographyTokens.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = ColorTokens.ReactTheme.foreground,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Empty Activity State
 *
 * Shown when there are no activities yet
 */
@Composable
private fun EmptyActivityState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Tokens.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        Icon(
            imageVector = IconSet.Time.schedule,
            contentDescription = null,
            tint = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = "No activity yet",
            style = TypographyTokens.typography.titleMedium,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Text(
            text = "Changes to this task will appear here",
            style = TypographyTokens.typography.bodySmall,
            color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.7f)
        )
    }
}

/**
 * Load More Button
 *
 * Button to load older activities
 */
@Composable
private fun LoadMoreButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .padding(Tokens.Spacing.md)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = ColorTokens.ReactTheme.primary
            )
            Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
        }
        Text(
            text = if (isLoading) "Loading..." else "Load more",
            style = TypographyTokens.typography.labelLarge
        )
    }
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Format field name for display
 */
private fun formatFieldName(field: String): String {
    return when (field) {
        "status" -> "Status"
        "priority" -> "Priority"
        "assignedTo" -> "Assigned"
        "title" -> "Title"
        "description" -> "Description"
        "dueDate" -> "Due Date"
        "tags" -> "Tags"
        "estimatedHours" -> "Estimate"
        "actualHours" -> "Actual"
        else -> field.replaceFirstChar { it.uppercase() }
    }
}

/**
 * Format timestamp to relative time
 * Examples: "Just now", "5m ago", "2h ago", "Yesterday", "Mar 15"
 */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 172_800_000 -> "Yesterday"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
