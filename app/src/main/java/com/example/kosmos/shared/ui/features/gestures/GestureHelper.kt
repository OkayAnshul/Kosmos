package com.example.kosmos.shared.ui.features.gestures

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.kosmos.shared.ui.designsystem.Tokens
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Gesture Detection Helpers for Kosmos App
 *
 * Advanced gesture support for mobile power users:
 * - Long-press for context menus
 * - Double-tap for quick actions
 * - Swipe gestures with haptic feedback
 * - Multi-touch support
 *
 * All gestures include haptic feedback for better UX.
 */

/**
 * Gesture Configuration
 */
object GestureConfig {
    val SWIPE_THRESHOLD = Tokens.Gesture.swipeThreshold.value
    val LONG_PRESS_DELAY = Tokens.Gesture.longPressDelay
    val DOUBLE_TAP_DELAY = Tokens.Gesture.doubleTapDelay
    const val SWIPE_VELOCITY_THRESHOLD = Tokens.Gesture.swipeVelocityThreshold
}

/**
 * Swipe Direction
 */
enum class SwipeDirection {
    LEFT, RIGHT, UP, DOWN, NONE
}

/**
 * Gesture Event
 */
sealed class GestureEvent {
    object Tap : GestureEvent()
    object DoubleTap : GestureEvent()
    object LongPress : GestureEvent()
    data class Swipe(val direction: SwipeDirection) : GestureEvent()
}

/**
 * Enhanced Click Modifier
 *
 * Combines click, double-click, and long-press with haptic feedback
 *
 * @param onClick Single tap handler
 * @param onDoubleTap Double tap handler
 * @param onLongPress Long press handler
 * @param hapticFeedback Whether to provide haptic feedback
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.enhancedClick(
    onClick: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    hapticFeedback: Boolean = true
): Modifier {
    val haptic = LocalHapticFeedback.current

    return this.combinedClickable(
        onClick = {
            if (hapticFeedback) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick?.invoke()
        },
        onDoubleClick = if (onDoubleTap != null) {
            {
                if (hapticFeedback) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onDoubleTap()
            }
        } else null,
        onLongClick = if (onLongPress != null) {
            {
                if (hapticFeedback) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onLongPress()
            }
        } else null
    )
}

/**
 * Swipe Gesture Detector
 *
 * Detects horizontal and vertical swipes with velocity
 *
 * @param onSwipeLeft Left swipe handler
 * @param onSwipeRight Right swipe handler
 * @param onSwipeUp Up swipe handler
 * @param onSwipeDown Down swipe handler
 * @param threshold Minimum distance to trigger swipe
 * @param hapticFeedback Whether to provide haptic feedback
 */
@Composable
fun Modifier.swipeGesture(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
    threshold: Float = GestureConfig.SWIPE_THRESHOLD,
    hapticFeedback: Boolean = true
): Modifier {
    val haptic = LocalHapticFeedback.current

    return this.pointerInput(Unit) {
        detectDragGestures(
            onDragEnd = {},
            onDragCancel = {},
            onDrag = { change, dragAmount ->
                change.consume()

                val (x, y) = dragAmount
                val absX = abs(x)
                val absY = abs(y)

                // Determine dominant direction
                if (absX > absY && absX > threshold) {
                    // Horizontal swipe
                    if (x > 0 && onSwipeRight != null) {
                        if (hapticFeedback) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onSwipeRight()
                    } else if (x < 0 && onSwipeLeft != null) {
                        if (hapticFeedback) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onSwipeLeft()
                    }
                } else if (absY > absX && absY > threshold) {
                    // Vertical swipe
                    if (y > 0 && onSwipeDown != null) {
                        if (hapticFeedback) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onSwipeDown()
                    } else if (y < 0 && onSwipeUp != null) {
                        if (hapticFeedback) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onSwipeUp()
                    }
                }
            }
        )
    }
}

/**
 * Long Press Detector
 *
 * Detects long press with custom delay
 *
 * @param onLongPress Long press handler
 * @param delayMillis Long press delay
 * @param hapticFeedback Whether to provide haptic feedback
 */
@Composable
fun Modifier.longPressGesture(
    onLongPress: () -> Unit,
    delayMillis: Long = GestureConfig.LONG_PRESS_DELAY,
    hapticFeedback: Boolean = true
): Modifier {
    val haptic = LocalHapticFeedback.current

    return this.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = {
                if (hapticFeedback) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onLongPress()
            }
        )
    }
}

/**
 * Double Tap Detector
 *
 * Detects double tap within specified time window
 *
 * @param onDoubleTap Double tap handler
 * @param hapticFeedback Whether to provide haptic feedback
 */
@Composable
fun Modifier.doubleTapGesture(
    onDoubleTap: () -> Unit,
    hapticFeedback: Boolean = true
): Modifier {
    val haptic = LocalHapticFeedback.current

    return this.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = {
                if (hapticFeedback) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onDoubleTap()
            }
        )
    }
}

/**
 * Pull to Refresh Gesture
 *
 * Detects vertical drag for pull-to-refresh
 *
 * @param onRefresh Refresh handler
 * @param threshold Pull distance threshold
 */
@Composable
fun Modifier.pullToRefreshGesture(
    onRefresh: () -> Unit,
    threshold: Float = Tokens.Scroll.pullToRefreshThreshold.value
): Modifier {
    var offsetY by remember { mutableStateOf(0f) }

    return this.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragEnd = {
                if (offsetY > threshold) {
                    onRefresh()
                }
                offsetY = 0f
            },
            onDragCancel = {
                offsetY = 0f
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                if (dragAmount > 0) { // Only pull down
                    offsetY += dragAmount
                }
            }
        )
    }
}

/**
 * Haptic Feedback Helper
 *
 * Provides consistent haptic feedback across the app
 */
object HapticFeedbackHelper {
    fun HapticFeedback.tap() {
        performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun HapticFeedback.longPress() {
        performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun HapticFeedback.success() {
        performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun HapticFeedback.error() {
        performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun HapticFeedback.selection() {
        performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

/**
 * Gesture State
 *
 * Tracks current gesture state
 */
@Composable
fun rememberGestureState(): GestureState {
    return remember { GestureState() }
}

class GestureState {
    var currentGesture by mutableStateOf<GestureEvent?>(null)
        private set

    var isLongPressing by mutableStateOf(false)
        private set

    var isSwiping by mutableStateOf(false)
        private set

    fun onGesture(event: GestureEvent) {
        currentGesture = event
        when (event) {
            is GestureEvent.LongPress -> isLongPressing = true
            is GestureEvent.Swipe -> isSwiping = true
            else -> {}
        }
    }

    fun reset() {
        currentGesture = null
        isLongPressing = false
        isSwiping = false
    }
}

/**
 * Multi-Gesture Detector
 *
 * Combines all gesture types into one modifier
 *
 * @param onClick Tap handler
 * @param onDoubleTap Double tap handler
 * @param onLongPress Long press handler
 * @param onSwipeLeft Left swipe handler
 * @param onSwipeRight Right swipe handler
 * @param onSwipeUp Up swipe handler
 * @param onSwipeDown Down swipe handler
 * @param hapticFeedback Whether to provide haptic feedback
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.multiGesture(
    onClick: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
    hapticFeedback: Boolean = true
): Modifier {
    var modifier = this

    // Add click gestures
    modifier = modifier.enhancedClick(
        onClick = onClick,
        onDoubleTap = onDoubleTap,
        onLongPress = onLongPress,
        hapticFeedback = hapticFeedback
    )

    // Add swipe gestures if any are defined
    if (onSwipeLeft != null || onSwipeRight != null || onSwipeUp != null || onSwipeDown != null) {
        modifier = modifier.swipeGesture(
            onSwipeLeft = onSwipeLeft,
            onSwipeRight = onSwipeRight,
            onSwipeUp = onSwipeUp,
            onSwipeDown = onSwipeDown,
            hapticFeedback = hapticFeedback
        )
    }

    return modifier
}

/**
 * Message Quick React Gesture
 *
 * Specialized gesture for chat messages:
 * - Tap: Select/open message
 * - Double tap: Quick react with last emoji
 * - Long press: Show context menu
 *
 * @param onClick Message tap handler
 * @param onQuickReact Quick react handler
 * @param onShowMenu Show context menu handler
 */
@Composable
fun Modifier.messageGesture(
    onClick: () -> Unit,
    onQuickReact: () -> Unit,
    onShowMenu: () -> Unit
): Modifier {
    return this.enhancedClick(
        onClick = onClick,
        onDoubleTap = onQuickReact,
        onLongPress = onShowMenu,
        hapticFeedback = true
    )
}

/**
 * Task Quick Complete Gesture
 *
 * Specialized gesture for task items:
 * - Tap: Open task details
 * - Swipe right: Mark as complete
 * - Long press: Show task menu
 *
 * @param onClick Task tap handler
 * @param onComplete Mark complete handler
 * @param onShowMenu Show menu handler
 */
@Composable
fun Modifier.taskGesture(
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onShowMenu: () -> Unit
): Modifier {
    return this.multiGesture(
        onClick = onClick,
        onLongPress = onShowMenu,
        onSwipeRight = onComplete,
        hapticFeedback = true
    )
}
