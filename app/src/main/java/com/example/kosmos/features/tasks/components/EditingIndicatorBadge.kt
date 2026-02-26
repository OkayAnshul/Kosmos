package com.example.kosmos.features.tasks.components
import androidx.compose.ui.graphics.Color

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Editing Indicator Badge
 *
 * Shows "Being edited by [name]" badge on form fields.
 * Used to warn users before overwriting someone else's edits.
 *
 * Usage:
 * ```kotlin
 * OutlinedTextField(
 *     value = description,
 *     onValueChange = { ... },
 *     label = {
 *         Row {
 *             Text("Description")
 *             EditingIndicatorBadge(
 *                 isVisible = editingUsers["description"] != null,
 *                 userName = editingUsers["description"]?.userName
 *             )
 *         }
 *     }
 * )
 * ```
 */
@Composable
fun EditingIndicatorBadge(
    isVisible: Boolean,
    userName: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && userName != null,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 }),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs),
            modifier = Modifier
                .background(
                    color = ColorTokens.Priority.medium,
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = Tokens.Spacing.xs, vertical = 2.dp)
        ) {
            // Editing icon
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Being edited",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )

            // User name
            Text(
                text = "Editing by ${userName}",
                style = TypographyTokens.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            // Pulsing dot indicator
            PulsingDot()
        }
    }
}

/**
 * Pulsing Dot Indicator
 *
 * Animated dot to show active editing
 */
@Composable
private fun PulsingDot(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(ColorTokens.Priority.medium)
    )
}

/**
 * Editing Warning Badge (for larger display)
 *
 * Shows a more prominent warning when user tries to edit a field
 * that someone else is currently editing.
 */
@Composable
fun EditingWarningBadge(
    isVisible: Boolean,
    userName: String?,
    onProceedAnyway: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && userName != null,
        enter = fadeIn() + slideInHorizontally(),
        exit = fadeOut() + slideOutHorizontally(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = ColorTokens.Priority.medium,
                    shape = MaterialTheme.shapes.small
                )
                .padding(Tokens.Spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Being edited",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
            ) {
                Text(
                    text = "$userName is editing this field",
                    style = TypographyTokens.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your changes may overwrite theirs",
                    style = TypographyTokens.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
