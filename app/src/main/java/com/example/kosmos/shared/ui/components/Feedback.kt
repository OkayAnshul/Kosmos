package com.example.kosmos.shared.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import kotlinx.coroutines.delay

/**
 * Feedback Components for Kosmos App
 *
 * Loading indicators, empty states, error messages, and snackbars.
 * Provide clear feedback for all user actions and system states.
 *
 * Components:
 * - LoadingIndicator: Circular progress with message
 * - EmptyState: No content placeholder with CTA
 * - ErrorState: Error message with retry
 * - SnackbarStandard: Toast-like notifications
 * - ProgressBar: Linear progress indicator
 * - LoadingDots: Animated dots (typing indicator)
 */

/**
 * Loading Indicator - Centered Progress
 *
 * Full-width centered loading indicator with optional message
 *
 * @param modifier Modifier
 * @param message Optional loading message
 * @param size Progress indicator size
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
    size: ProgressSize = ProgressSize.MEDIUM
) {
    val indicatorSize = when (size) {
        ProgressSize.SMALL -> Tokens.Size.progressSmall
        ProgressSize.MEDIUM -> Tokens.Size.progressMedium
        ProgressSize.LARGE -> Tokens.Size.progressLarge
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Tokens.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(indicatorSize)
        )
        if (message != null) {
            Text(
                text = message,
                style = TypographyTokens.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class ProgressSize {
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * Empty State - No Content Placeholder
 *
 * Shows when list/view has no content
 * Includes icon, message, and optional action button
 *
 * @param title Empty state title
 * @param modifier Modifier
 * @param message Optional description
 * @param icon Optional icon
 * @param actionLabel Optional action button text
 * @param onActionClick Optional action button handler
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = IconSet.Feedback.empty,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Tokens.Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Tokens.Size.avatarXLarge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Text(
            text = title,
            style = TypographyTokens.Custom.emptyStateTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (message != null) {
            Text(
                text = message,
                style = TypographyTokens.Custom.emptyStateDescription,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(Tokens.Spacing.sm))
            PrimaryButton(
                text = actionLabel,
                onClick = onActionClick
            )
        }
    }
}

/**
 * Error State - Error Message with Retry
 *
 * Shows when operation fails
 * Includes error message and retry button
 *
 * @param title Error title
 * @param modifier Modifier
 * @param message Optional error details
 * @param onRetry Optional retry handler
 * @param retryLabel Retry button text
 */
@Composable
fun ErrorState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "Retry"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Tokens.Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
    ) {
        Icon(
            imageVector = IconSet.Feedback.error,
            contentDescription = null,
            modifier = Modifier.size(Tokens.Size.avatarXLarge),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = title,
            style = TypographyTokens.Custom.emptyStateTitle,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        if (message != null) {
            Text(
                text = message,
                style = TypographyTokens.Custom.emptyStateDescription,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(Tokens.Spacing.sm))
            PrimaryButton(
                text = retryLabel,
                onClick = onRetry,
                icon = IconSet.Action.refresh
            )
        }
    }
}

/**
 * Success State - Success Message
 *
 * Shows when operation succeeds
 * Auto-dismisses after delay
 *
 * @param title Success title
 * @param modifier Modifier
 * @param message Optional success details
 * @param onDismiss Optional dismiss handler
 * @param autoDismiss Whether to auto-dismiss
 * @param dismissDelay Auto-dismiss delay in milliseconds
 */
@Composable
fun SuccessState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    onDismiss: (() -> Unit)? = null,
    autoDismiss: Boolean = true,
    dismissDelay: Long = 2000L
) {
    LaunchedEffect(autoDismiss) {
        if (autoDismiss && onDismiss != null) {
            delay(dismissDelay)
            onDismiss()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Tokens.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
    ) {
        Icon(
            imageVector = IconSet.Feedback.success,
            contentDescription = null,
            modifier = Modifier.size(Tokens.Size.avatarLarge),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = title,
            style = TypographyTokens.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (message != null) {
            Text(
                text = message,
                style = TypographyTokens.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Loading Dots - Animated Typing Indicator
 *
 * Three animated dots for typing/loading states
 * Commonly used in chat for "user is typing"
 *
 * @param modifier Modifier
 * @param dotSize Size of each dot
 */
@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    dotSize: androidx.compose.ui.unit.Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    @Composable
    fun animateDotAlpha(delay: Int): Float {
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delay),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_alpha_$delay"
        )
        return alpha
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Surface(
                modifier = Modifier
                    .size(dotSize)
                    .alpha(animateDotAlpha(index * 200)),
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) {}
        }
    }
}

/**
 * Linear Progress Bar
 *
 * Progress bar with optional label and percentage
 *
 * @param progress Progress value (0.0 to 1.0)
 * @param modifier Modifier
 * @param label Optional progress label
 * @param showPercentage Whether to show percentage
 */
@Composable
fun ProgressBarLinear(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    showPercentage: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
    ) {
        if (label != null || showPercentage) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = TypographyTokens.Custom.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showPercentage) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = TypographyTokens.Custom.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Indeterminate Progress Bar
 *
 * Progress bar with unknown completion (indeterminate state)
 *
 * @param modifier Modifier
 * @param label Optional label
 */
@Composable
fun ProgressBarIndeterminate(
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
    ) {
        if (label != null) {
            Text(
                text = label,
                style = TypographyTokens.Custom.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Network Error Banner
 *
 * Persistent banner for offline/connection issues
 * Sticks to top of screen
 *
 * @param message Error message
 * @param modifier Modifier
 * @param onRetry Optional retry handler
 */
@Composable
fun NetworkErrorBanner(
    message: String = "No internet connection",
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = Tokens.Elevation.level2
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = IconSet.Feedback.offline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = TypographyTokens.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            if (onRetry != null) {
                TextButtonStandard(
                    text = "Retry",
                    onClick = onRetry
                )
            }
        }
    }
}

/**
 * Info Banner
 *
 * Informational banner for tips, updates, announcements
 *
 * @param message Info message
 * @param modifier Modifier
 * @param onDismiss Optional dismiss handler
 * @param icon Optional icon
 */
@Composable
fun InfoBanner(
    message: String,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    icon: ImageVector = IconSet.Status.info
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = Tokens.Elevation.level1
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = message,
                    style = TypographyTokens.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (onDismiss != null) {
                IconButtonStandard(
                    icon = IconSet.Navigation.close,
                    onClick = onDismiss,
                    contentDescription = "Dismiss"
                )
            }
        }
    }
}

/**
 * Skeleton Loader - Placeholder for Loading Content
 *
 * Animated placeholder while content loads
 * Shows approximate layout structure
 *
 * @param modifier Modifier
 */
@Composable
fun SkeletonLoader(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
    ) {
        Box(modifier = Modifier.fillMaxSize())
    }
}

/**
 * Message Skeleton - Skeleton for Message List
 *
 * Placeholder for chat messages while loading
 *
 * @param modifier Modifier
 */
@Composable
fun MessageSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Tokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (it % 2 == 0) Arrangement.Start else Arrangement.End
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.7f),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                ) {
                    SkeletonLoader(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(16.dp)
                    )
                    SkeletonLoader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    )
                }
            }
        }
    }
}

/**
 * Snackbar Host State Extension
 * Helper to show snackbars easily
 */
suspend fun SnackbarHostState.showSuccess(
    message: String,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult {
    return showSnackbar(
        message = message,
        actionLabel = actionLabel,
        duration = duration
    )
}

suspend fun SnackbarHostState.showError(
    message: String,
    actionLabel: String? = "Retry",
    duration: SnackbarDuration = SnackbarDuration.Long
): SnackbarResult {
    return showSnackbar(
        message = message,
        actionLabel = actionLabel,
        duration = duration
    )
}
