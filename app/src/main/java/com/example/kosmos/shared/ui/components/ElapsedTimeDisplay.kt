package com.example.kosmos.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import kotlinx.coroutines.delay

/**
 * ElapsedTimeDisplay Component
 *
 * Displays real-time elapsed time since task creation with auto-updates
 * every 60 seconds to minimize recompositions.
 *
 * Features:
 * - Auto-start from task creation time (createdAt timestamp)
 * - Live updates every 60 seconds
 * - Formatted display: "2d 5h", "3h 45m", "25m", "Just now"
 * - Optional progress indicator if estimated hours set
 * - Color-coded progress: Green (0-80%), Orange (80-100%), Red (>100%)
 */
@Composable
fun ElapsedTimeDisplay(
    createdAt: Long,
    estimatedHours: Float? = null,
    actualHours: Float? = null,
    modifier: Modifier = Modifier
) {
    // Real-time update every minute
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L) // 60 seconds
            currentTime = System.currentTimeMillis()
        }
    }

    val elapsedMillis = currentTime - createdAt
    val elapsedFormatted = formatElapsedTime(elapsedMillis)
    val elapsedHours = elapsedMillis / (1000f * 60f * 60f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        color = ColorTokens.ReactTheme.card,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            // Header
            Text(
                text = "Time Tracking",
                style = TypographyTokens.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ColorTokens.ReactTheme.foreground
            )

            // Elapsed Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Elapsed Time",
                        style = TypographyTokens.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                    Text(
                        text = elapsedFormatted,
                        style = TypographyTokens.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ColorTokens.ReactTheme.foreground
                    )
                    Text(
                        text = "Since creation",
                        style = TypographyTokens.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f)
                    )
                }

                // Show elapsed hours as decimal for reference
                Text(
                    text = "${String.format("%.1f", elapsedHours)}h",
                    style = TypographyTokens.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            // Estimated Hours (if set)
            if (estimatedHours != null && estimatedHours > 0f) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimated",
                        style = TypographyTokens.typography.bodyMedium,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                    Text(
                        text = "${String.format("%.1f", estimatedHours)}h",
                        style = TypographyTokens.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = ColorTokens.ReactTheme.foreground
                    )
                }
            }

            // Actual Hours (if set)
            if (actualHours != null && actualHours > 0f) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Actual",
                        style = TypographyTokens.typography.bodyMedium,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                    Text(
                        text = "${String.format("%.1f", actualHours)}h",
                        style = TypographyTokens.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = ColorTokens.ReactTheme.foreground
                    )
                }
            }

            // Progress Bar (if both estimated & actual set)
            if (estimatedHours != null && estimatedHours > 0f && actualHours != null && actualHours > 0f) {
                val progress = (actualHours / estimatedHours).coerceIn(0f, 1.2f)
                val progressPercentage = (progress * 100).toInt()

                val progressColor = when {
                    progress > 1f -> ColorTokens.ReactTheme.destructive
                    progress >= 0.8f -> ColorTokens.Priority.medium
                    else -> ColorTokens.Status.online
                }

                val progressText = when {
                    progress > 1f -> "${progressPercentage}% over budget"
                    else -> "${progressPercentage}% complete"
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progress",
                            style = TypographyTokens.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                        Text(
                            text = progressText,
                            style = TypographyTokens.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = progressColor
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = progressColor,
                        trackColor = ColorTokens.ReactTheme.card.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

/**
 * Format elapsed time in human-readable format
 *
 * Examples:
 * - < 1 minute: "Just now"
 * - 1-59 minutes: "25m"
 * - 1-23 hours: "3h 45m"
 * - 1+ days: "2d 5h"
 */
private fun formatElapsedTime(elapsedMillis: Long): String {
    val minutes = (elapsedMillis / (1000 * 60)).toInt()
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "Just now"
    }
}
