package com.example.kosmos.shared.ui.layouts

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Swipeable Layout Components
 *
 * Gesture-based interactions for mobile power users.
 * Swipe actions for quick operations without menus.
 *
 * Components:
 * - SwipeableLayout: Generic swipeable container
 * - SwipeToDelete: Swipe left to delete
 * - SwipeToArchive: Swipe right to archive
 * - SwipeActions: Customizable swipe actions
 */

/**
 * Swipe Direction
 */
enum class SwipeDirection {
    LEFT,
    RIGHT,
    NONE
}

/**
 * Swipe Action Configuration
 *
 * @param icon Action icon
 * @param label Action label
 * @param color Background color when swiping
 * @param onAction Action handler
 */
data class SwipeAction(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onAction: () -> Unit
)

/**
 * Swipeable Layout
 *
 * Generic container with swipe gesture detection
 * Supports left and right swipe actions
 *
 * @param modifier Modifier
 * @param leftAction Optional left swipe action
 * @param rightAction Optional right swipe action
 * @param threshold Swipe threshold to trigger action (default: 56dp)
 * @param maxSwipeDistance Maximum swipe distance (default: 2x threshold)
 * @param onSwipeComplete Callback when swipe completes
 * @param content Swipeable content
 */
@Composable
fun SwipeableLayout(
    modifier: Modifier = Modifier,
    leftAction: SwipeAction? = null,
    rightAction: SwipeAction? = null,
    threshold: Float = Tokens.Gesture.swipeThreshold.value,
    maxSwipeDistance: Float = threshold * 2,
    onSwipeComplete: ((SwipeDirection) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var swipeDirection by remember { mutableStateOf(SwipeDirection.NONE) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val absOffset = offsetX.absoluteValue
                        if (absOffset > threshold) {
                            // Swipe completed
                            val direction = if (offsetX > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT

                            when (direction) {
                                SwipeDirection.RIGHT -> rightAction?.onAction?.invoke()
                                SwipeDirection.LEFT -> leftAction?.onAction?.invoke()
                                SwipeDirection.NONE -> {}
                            }

                            onSwipeComplete?.invoke(direction)
                        }
                        // Reset position with animation
                        offsetX = 0f
                        swipeDirection = SwipeDirection.NONE
                    },
                    onDragCancel = {
                        offsetX = 0f
                        swipeDirection = SwipeDirection.NONE
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = offsetX + dragAmount

                        // Limit swipe distance
                        offsetX = when {
                            newOffset > 0 && rightAction != null -> newOffset.coerceAtMost(maxSwipeDistance)
                            newOffset < 0 && leftAction != null -> newOffset.coerceAtLeast(-maxSwipeDistance)
                            else -> 0f
                        }

                        swipeDirection = when {
                            offsetX > 0 -> SwipeDirection.RIGHT
                            offsetX < 0 -> SwipeDirection.LEFT
                            else -> SwipeDirection.NONE
                        }
                    }
                )
            }
    ) {
        // Background action indicators
        if (offsetX > 0 && rightAction != null) {
            // Right swipe background (left side visible)
            SwipeBackground(
                action = rightAction,
                progress = (offsetX / threshold).coerceAtMost(1f),
                alignment = Alignment.CenterStart
            )
        } else if (offsetX < 0 && leftAction != null) {
            // Left swipe background (right side visible)
            SwipeBackground(
                action = leftAction,
                progress = (offsetX.absoluteValue / threshold).coerceAtMost(1f),
                alignment = Alignment.CenterEnd
            )
        }

        // Content with offset
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

/**
 * Swipe Background
 *
 * Background shown during swipe gesture
 * Fades in as swipe progresses
 *
 * @param action Swipe action configuration
 * @param progress Swipe progress (0.0 to 1.0)
 * @param alignment Content alignment
 */
@Composable
private fun SwipeBackground(
    action: SwipeAction,
    progress: Float,
    alignment: Alignment
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .alpha(progress * 0.9f),
        color = action.color
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Tokens.Spacing.md),
            contentAlignment = alignment
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (alignment == Alignment.CenterStart) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = action.label,
                        style = TypographyTokens.Custom.chipLabel,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = action.label,
                        style = TypographyTokens.Custom.chipLabel,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * Swipe to Delete
 *
 * Common pattern: swipe left to delete
 * Shows red background with delete icon
 *
 * @param onDelete Delete handler
 * @param modifier Modifier
 * @param deleteLabel Delete label (default: "Delete")
 * @param deleteIcon Delete icon
 * @param content Swipeable content
 */
@Composable
fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    deleteLabel: String = "Delete",
    deleteIcon: ImageVector = com.example.kosmos.shared.ui.designsystem.IconSet.Action.delete,
    content: @Composable () -> Unit
) {
    SwipeableLayout(
        modifier = modifier,
        leftAction = SwipeAction(
            icon = deleteIcon,
            label = deleteLabel,
            color = MaterialTheme.colorScheme.error,
            onAction = onDelete
        ),
        content = content
    )
}

/**
 * Swipe to Archive
 *
 * Common pattern: swipe right to archive
 * Shows blue background with archive icon
 *
 * @param onArchive Archive handler
 * @param modifier Modifier
 * @param archiveLabel Archive label (default: "Archive")
 * @param archiveIcon Archive icon
 * @param content Swipeable content
 */
@Composable
fun SwipeToArchive(
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
    archiveLabel: String = "Archive",
    archiveIcon: ImageVector = com.example.kosmos.shared.ui.designsystem.IconSet.Message.archive,
    content: @Composable () -> Unit
) {
    SwipeableLayout(
        modifier = modifier,
        rightAction = SwipeAction(
            icon = archiveIcon,
            label = archiveLabel,
            color = MaterialTheme.colorScheme.primaryContainer,
            onAction = onArchive
        ),
        content = content
    )
}

/**
 * Swipe Actions (Both Directions)
 *
 * Full swipe configuration with both left and right actions
 * Common pattern for list items with multiple quick actions
 *
 * @param onSwipeLeft Left swipe handler (e.g., delete)
 * @param onSwipeRight Right swipe handler (e.g., archive)
 * @param modifier Modifier
 * @param leftIcon Left action icon
 * @param leftLabel Left action label
 * @param leftColor Left action color
 * @param rightIcon Right action icon
 * @param rightLabel Right action label
 * @param rightColor Right action color
 * @param content Swipeable content
 */
@Composable
fun SwipeActions(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    leftIcon: ImageVector = com.example.kosmos.shared.ui.designsystem.IconSet.Action.delete,
    leftLabel: String = "Delete",
    leftColor: Color = MaterialTheme.colorScheme.error,
    rightIcon: ImageVector = com.example.kosmos.shared.ui.designsystem.IconSet.Message.archive,
    rightLabel: String = "Archive",
    rightColor: Color = MaterialTheme.colorScheme.primaryContainer,
    content: @Composable () -> Unit
) {
    SwipeableLayout(
        modifier = modifier,
        leftAction = SwipeAction(
            icon = leftIcon,
            label = leftLabel,
            color = leftColor,
            onAction = onSwipeLeft
        ),
        rightAction = SwipeAction(
            icon = rightIcon,
            label = rightLabel,
            color = rightColor,
            onAction = onSwipeRight
        ),
        content = content
    )
}

/**
 * Swipe to Complete (Task)
 *
 * Common pattern for task lists: swipe right to mark as done
 * Shows green background with checkmark
 *
 * @param onComplete Complete handler
 * @param modifier Modifier
 * @param completeLabel Complete label (default: "Done")
 * @param content Swipeable content
 */
@Composable
fun SwipeToComplete(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    completeLabel: String = "Done",
    content: @Composable () -> Unit
) {
    SwipeableLayout(
        modifier = modifier,
        rightAction = SwipeAction(
            icon = com.example.kosmos.shared.ui.designsystem.IconSet.Action.done,
            label = completeLabel,
            color = MaterialTheme.colorScheme.primary,
            onAction = onComplete
        ),
        content = content
    )
}

/**
 * Dismissible Item
 *
 * Item that can be swiped away completely
 * Animates out when dismissed
 *
 * @param onDismiss Dismiss handler
 * @param modifier Modifier
 * @param dismissDirection Direction to swipe for dismiss
 * @param dismissLabel Dismiss label
 * @param dismissIcon Dismiss icon
 * @param dismissColor Dismiss background color
 * @param content Item content
 */
@Composable
fun DismissibleItem(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissDirection: SwipeDirection = SwipeDirection.LEFT,
    dismissLabel: String = "Dismiss",
    dismissIcon: ImageVector = com.example.kosmos.shared.ui.designsystem.IconSet.Navigation.close,
    dismissColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit
) {
    var isDismissed by remember { mutableStateOf(false) }

    if (!isDismissed) {
        SwipeableLayout(
            modifier = modifier,
            leftAction = if (dismissDirection == SwipeDirection.LEFT) {
                SwipeAction(
                    icon = dismissIcon,
                    label = dismissLabel,
                    color = dismissColor,
                    onAction = {
                        isDismissed = true
                        onDismiss()
                    }
                )
            } else null,
            rightAction = if (dismissDirection == SwipeDirection.RIGHT) {
                SwipeAction(
                    icon = dismissIcon,
                    label = dismissLabel,
                    color = dismissColor,
                    onAction = {
                        isDismissed = true
                        onDismiss()
                    }
                )
            } else null,
            content = content
        )
    }
}

/**
 * Swipe Gesture Helper
 *
 * Standalone function to detect swipe gestures
 * Can be used with any composable
 *
 * @param threshold Swipe distance threshold
 * @param onSwipeLeft Left swipe callback
 * @param onSwipeRight Right swipe callback
 */
fun Modifier.swipeGesture(
    threshold: Float = Tokens.Gesture.swipeThreshold.value,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
): Modifier = this.pointerInput(Unit) {
    var offsetX = 0f

    detectHorizontalDragGestures(
        onDragEnd = {
            if (offsetX.absoluteValue > threshold) {
                when {
                    offsetX > 0 -> onSwipeRight?.invoke()
                    offsetX < 0 -> onSwipeLeft?.invoke()
                }
            }
            offsetX = 0f
        },
        onDragCancel = {
            offsetX = 0f
        },
        onHorizontalDrag = { change, dragAmount ->
            change.consume()
            offsetX += dragAmount
        }
    )
}
