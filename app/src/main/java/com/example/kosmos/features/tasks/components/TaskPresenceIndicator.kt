package com.example.kosmos.features.tasks.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.data.realtime.TaskViewer
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Task Presence Indicator
 *
 * Shows who's currently viewing a task with:
 * - Stacked avatars (max 3 visible)
 * - "+N more" indicator for additional viewers
 * - Online status dots
 * - Smooth animations
 *
 * Usage:
 * ```kotlin
 * TaskPresenceIndicator(
 *     viewers = listOf(
 *         TaskViewer("1", "John Doe", null),
 *         TaskViewer("2", "Jane Smith", null)
 *     )
 * )
 * ```
 */
@Composable
fun TaskPresenceIndicator(
    viewers: List<TaskViewer>,
    modifier: Modifier = Modifier,
    maxVisibleAvatars: Int = 3
) {
    AnimatedVisibility(
        visible = viewers.isNotEmpty(),
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
            modifier = Modifier
                .background(
                    color = ColorTokens.ReactTheme.card.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.medium
                )
                .border(
                    width = 1.dp,
                    color = ColorTokens.ReactTheme.card,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(horizontal = Tokens.Spacing.sm, vertical = Tokens.Spacing.xs)
        ) {
            // Online indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ColorTokens.Status.online)
            )

            // "Viewing now" label
            Text(
                text = "Viewing now:",
                style = TypographyTokens.typography.labelSmall,
                color = ColorTokens.ReactTheme.mutedForeground,
                fontWeight = FontWeight.Medium
            )

            // Stacked avatars
            Row(
                horizontalArrangement = Arrangement.spacedBy((-8).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val visibleViewers = viewers.take(maxVisibleAvatars)
                val remainingCount = (viewers.size - maxVisibleAvatars).coerceAtLeast(0)

                visibleViewers.forEachIndexed { index, viewer ->
                    ViewerAvatar(
                        viewer = viewer,
                        zIndex = visibleViewers.size - index
                    )
                }

                // "+N more" indicator
                if (remainingCount > 0) {
                    RemainingViewersIndicator(count = remainingCount)
                }
            }
        }
    }
}

/**
 * Viewer Avatar
 *
 * Circular avatar with user initial and colored border
 */
@Composable
private fun ViewerAvatar(
    viewer: TaskViewer,
    zIndex: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(32.dp)
            .border(
                width = 2.dp,
                color = ColorTokens.ReactTheme.card,
                shape = CircleShape
            ),
        shape = CircleShape,
        color = getViewerColor(viewer.userId),
        shadowElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = getInitial(viewer.userName),
                style = TypographyTokens.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Remaining Viewers Indicator
 *
 * Shows "+N" count for additional viewers
 */
@Composable
private fun RemainingViewersIndicator(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(32.dp)
            .border(
                width = 2.dp,
                color = ColorTokens.ReactTheme.card,
                shape = CircleShape
            ),
        shape = CircleShape,
        color = ColorTokens.ReactTheme.card,
        shadowElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "+$count",
                style = TypographyTokens.typography.labelSmall,
                color = ColorTokens.ReactTheme.mutedForeground,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Get user initial from name
 */
private fun getInitial(name: String): String {
    return name.trim()
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(1)
        .joinToString("")
        .ifEmpty { "?" }
}

/**
 * Get consistent color for viewer based on userId
 * Uses hash to generate consistent color per user
 */
private fun getViewerColor(userId: String): Color {
    val colors = listOf(
        ColorTokens.ReactTheme.primary,
        Color(0xFF6366F1), // Indigo
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFF06B6D4), // Cyan
        Color(0xFF3B82F6), // Blue
        Color(0xFFEF4444), // Red
        Color(0xFFF97316)  // Orange
    )

    val hash = userId.hashCode()
    val index = (hash % colors.size).let { if (it < 0) it + colors.size else it }
    return colors[index]
}
