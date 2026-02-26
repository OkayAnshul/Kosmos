package com.example.kosmos.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.ProjectStatus
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.shared.ui.designsystem.ColorTokens

/**
 * Status badge component for showing project/task status
 * Stitch design: Color-coded chips with optional icons
 */

enum class BadgeSize {
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * Project status badge
 */
@Composable
fun ProjectStatusBadge(
    status: ProjectStatus,
    modifier: Modifier = Modifier,
    size: BadgeSize = BadgeSize.MEDIUM,
    showIcon: Boolean = false
) {
    val (color, text, icon) = when (status) {
        ProjectStatus.ACTIVE -> Triple(
            ColorTokens.Status.online,
            "Active",
            Icons.Default.CheckCircle
        )
        ProjectStatus.ARCHIVED -> Triple(
            ColorTokens.ReactTheme.mutedForeground,
            "Archived",
            Icons.Default.Archive
        )
        ProjectStatus.COMPLETED -> Triple(
            ColorTokens.ReactTheme.primary,
            "Completed",
            Icons.Default.Done
        )
        ProjectStatus.ON_HOLD -> Triple(
            ColorTokens.Priority.medium,
            "On Hold",
            Icons.Default.Pause
        )
    }

    StatusBadge(
        text = text,
        color = color,
        icon = if (showIcon) icon else null,
        size = size,
        modifier = modifier
    )
}

/**
 * Task status badge
 */
@Composable
fun TaskStatusBadge(
    status: TaskStatus,
    modifier: Modifier = Modifier,
    size: BadgeSize = BadgeSize.SMALL,
    showIcon: Boolean = false
) {
    val (color, text, icon) = when (status) {
        TaskStatus.TODO -> Triple(
            ColorTokens.ReactTheme.mutedForeground,
            "To Do",
            Icons.Default.RadioButtonUnchecked
        )
        TaskStatus.IN_PROGRESS -> Triple(
            ColorTokens.ReactTheme.primary,
            "In Progress",
            Icons.Default.PlayArrow
        )
        TaskStatus.DONE -> Triple(
            ColorTokens.Status.online,
            "Done",
            Icons.Default.CheckCircle
        )
        TaskStatus.CANCELLED -> Triple(
            ColorTokens.ReactTheme.mutedForeground,
            "Cancelled",
            Icons.Default.Cancel
        )
    }

    StatusBadge(
        text = text,
        color = color,
        icon = if (showIcon) icon else null,
        size = size,
        modifier = modifier
    )
}

/**
 * Task priority badge
 */
@Composable
fun TaskPriorityBadge(
    priority: TaskPriority,
    modifier: Modifier = Modifier,
    size: BadgeSize = BadgeSize.SMALL,
    showIcon: Boolean = false
) {
    val (color, text, icon) = when (priority) {
        TaskPriority.URGENT -> Triple(
            Color(0xFFD32F2F),  // Dark red for urgent
            "URGENT",
            Icons.Default.LocalFireDepartment
        )
        TaskPriority.HIGH -> Triple(
            ColorTokens.ReactTheme.destructive,
            "HIGH",
            Icons.Default.PriorityHigh
        )
        TaskPriority.MEDIUM -> Triple(
            ColorTokens.Priority.medium,
            "MEDIUM",
            Icons.Default.Remove
        )
        TaskPriority.LOW -> Triple(
            ColorTokens.ReactTheme.primary,
            "LOW",
            Icons.Default.ArrowDownward
        )
    }

    StatusBadge(
        text = text,
        color = color,
        icon = if (showIcon) icon else null,
        size = size,
        modifier = modifier
    )
}

/**
 * Generic status badge component
 */
@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    size: BadgeSize = BadgeSize.MEDIUM
) {
    val (paddingHorizontal, paddingVertical, cornerRadius, textStyle, iconSize) = when (size) {
        BadgeSize.SMALL -> Tuple5(6.dp, 2.dp, 4.dp, MaterialTheme.typography.labelSmall, 12.dp)
        BadgeSize.MEDIUM -> Tuple5(8.dp, 4.dp, 6.dp, MaterialTheme.typography.labelMedium, 14.dp)
        BadgeSize.LARGE -> Tuple5(12.dp, 6.dp, 8.dp, MaterialTheme.typography.labelLarge, 16.dp)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = paddingHorizontal, vertical = paddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Optional icon
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(iconSize)
                )
            }

            // Text
            Text(
                text = text,
                style = textStyle,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Custom status badge with custom text and color
 */
@Composable
fun CustomStatusBadge(
    text: String,
    color: Color = ColorTokens.ReactTheme.primary,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    size: BadgeSize = BadgeSize.MEDIUM
) {
    StatusBadge(
        text = text,
        color = color,
        icon = icon,
        size = size,
        modifier = modifier
    )
}

/**
 * Syncing status badge
 */
@Composable
fun SyncingBadge(
    modifier: Modifier = Modifier,
    size: BadgeSize = BadgeSize.SMALL
) {
    StatusBadge(
        text = "Syncing",
        color = ColorTokens.ReactTheme.primary,
        icon = Icons.Default.Sync,
        size = size,
        modifier = modifier
    )
}

/**
 * Helper data class for tuple of 5 elements
 */
private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
