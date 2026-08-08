package com.example.kosmos.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Wizard Step Indicator
 *
 * Horizontal pill-dot step progress bar:
 * - Filled pill = completed step
 * - Ring = current step
 * - Small dot = upcoming step
 *
 * Design: 300ms crossfade animations between states
 */
@Composable
fun WizardStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Tokens.Spacing.lg, vertical = Tokens.Spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (step in 1..totalSteps) {
            val state = when {
                step < currentStep -> StepState.DONE
                step == currentStep -> StepState.CURRENT
                else -> StepState.UPCOMING
            }
            StepPill(step = step, state = state)

            if (step < totalSteps) {
                // Connector line
                val lineColor by animateColorAsState(
                    targetValue = if (step < currentStep)
                        ColorTokens.ReactTheme.primary
                    else
                        ColorTokens.ReactTheme.border,
                    animationSpec = tween(300),
                    label = "connector_$step"
                )
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(1f)
                        .background(lineColor)
                )
            }
        }
    }
}

private enum class StepState { DONE, CURRENT, UPCOMING }

@Composable
private fun StepPill(step: Int, state: StepState) {
    val bgColor by animateColorAsState(
        targetValue = when (state) {
            StepState.DONE -> ColorTokens.ReactTheme.primary
            StepState.CURRENT -> Color.Transparent
            StepState.UPCOMING -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "bg_$step"
    )
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            StepState.DONE -> ColorTokens.ReactTheme.primary
            StepState.CURRENT -> ColorTokens.ReactTheme.primary
            StepState.UPCOMING -> ColorTokens.ReactTheme.border
        },
        animationSpec = tween(300),
        label = "border_$step"
    )
    val size by animateDpAsState(
        targetValue = when (state) {
            StepState.DONE -> 28.dp
            StepState.CURRENT -> 28.dp
            StepState.UPCOMING -> 10.dp
        },
        animationSpec = tween(300),
        label = "size_$step"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(if (state == StepState.UPCOMING) CircleShape else RoundedCornerShape(14.dp))
            .background(bgColor)
            .then(
                if (state != StepState.DONE)
                    Modifier.border(
                        width = 2.dp,
                        color = borderColor,
                        shape = if (state == StepState.UPCOMING) CircleShape else RoundedCornerShape(14.dp)
                    )
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (state != StepState.UPCOMING) {
            Text(
                text = step.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when (state) {
                    StepState.DONE -> ColorTokens.ReactTheme.primaryForeground
                    StepState.CURRENT -> ColorTokens.ReactTheme.primary
                    else -> Color.Transparent
                }
            )
        }
    }
}
