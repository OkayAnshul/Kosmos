package com.example.kosmos.shared.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.ColorTokens

/**
 * Progress bar component for showing project completion
 * Stitch design: Color-coded by completion percentage with smooth animation
 */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true,
    height: Int = 8
) {
    // Clamp progress between 0 and 1
    val clampedProgress = progress.coerceIn(0f, 1f)

    // Animate progress changes
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 600),
        label = "progress_animation"
    )

    // Color based on completion
    val progressColor = getProgressColor(clampedProgress)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Progress bar track
        Box(
            modifier = Modifier
                .weight(1f)
                .height(height.dp)
                .clip(RoundedCornerShape(height.dp / 2))
                .background(ColorTokens.ReactTheme.background)
        ) {
            // Progress bar fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(height.dp / 2))
                    .background(progressColor)
            )
        }

        // Percentage text
        if (showPercentage) {
            Text(
                text = "${(clampedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = progressColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 40.dp)
            )
        }
    }
}

/**
 * Get progress color based on completion percentage
 * Green (>80%), Orange (50-80%), Red (<50%)
 */
private fun getProgressColor(progress: Float): Color {
    return when {
        progress >= 0.8f -> ColorTokens.Status.online  // Green (>80%)
        progress >= 0.5f -> ColorTokens.Priority.medium  // Orange (50-80%)
        else -> ColorTokens.ReactTheme.destructive                // Red (<50%)
    }
}

/**
 * Circular progress indicator variant
 */
@Composable
fun CircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Int = 48,
    strokeWidth: Int = 4
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 600),
        label = "circular_progress_animation"
    )
    val progressColor = getProgressColor(clampedProgress)

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        androidx.compose.material3.CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = ColorTokens.ReactTheme.background,
            strokeWidth = strokeWidth.dp,
        )

        // Progress circle
        androidx.compose.material3.CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = progressColor,
            strokeWidth = strokeWidth.dp,
        )

        // Percentage text
        Text(
            text = "${(clampedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = progressColor,
            fontWeight = FontWeight.Bold
        )
    }
}
