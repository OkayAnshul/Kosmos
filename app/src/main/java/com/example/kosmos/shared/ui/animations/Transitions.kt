package com.example.kosmos.shared.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Animation and Transition Utilities
 *
 * Consistent animations across the app using Material Motion standards.
 * All animations target 60fps for smooth performance.
 *
 * Components:
 * - Screen transitions
 * - List item animations
 * - FAB animations
 * - Loading animations
 * - Success/error feedback animations
 */

/**
 * Animation Specs
 *
 * Pre-configured animation specifications for consistency
 */
object AnimationSpecs {
    /**
     * Standard easing for most animations
     */
    val standardEasing = tween<Float>(
        durationMillis = Tokens.Duration.normal,
        easing = FastOutSlowInEasing
    )

    /**
     * Fast animations for quick feedback
     */
    val fastEasing = tween<Float>(
        durationMillis = Tokens.Duration.fast,
        easing = LinearOutSlowInEasing
    )

    /**
     * Emphasized easing for important transitions
     */
    val emphasizedEasing = tween<Float>(
        durationMillis = Tokens.Duration.medium,
        easing = EaseInOutCubic
    )

    /**
     * Spring animation for bouncy effects
     */
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Snappy spring for quick responses
     */
    val snappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * Screen Transition Animations
 */
object ScreenTransitions {
    /**
     * Slide in from right (forward navigation)
     */
    fun slideInFromRight(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(Tokens.Duration.medium)
        ) + fadeIn(animationSpec = tween(Tokens.Duration.normal))
    }

    /**
     * Slide out to left (forward navigation)
     */
    fun slideOutToLeft(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(Tokens.Duration.medium)
        ) + fadeOut(animationSpec = tween(Tokens.Duration.normal))
    }

    /**
     * Slide in from left (back navigation)
     */
    fun slideInFromLeft(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(Tokens.Duration.medium)
        ) + fadeIn(animationSpec = tween(Tokens.Duration.normal))
    }

    /**
     * Slide out to right (back navigation)
     */
    fun slideOutToRight(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(Tokens.Duration.medium)
        ) + fadeOut(animationSpec = tween(Tokens.Duration.normal))
    }

    /**
     * Fade through (cross-fade between screens)
     */
    fun fadeThroughEnter(): EnterTransition {
        return fadeIn(animationSpec = tween(Tokens.Duration.medium)) +
                scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(Tokens.Duration.medium)
                )
    }

    fun fadeThroughExit(): ExitTransition {
        return fadeOut(animationSpec = tween(Tokens.Duration.normal)) +
                scaleOut(
                    targetScale = 1.05f,
                    animationSpec = tween(Tokens.Duration.normal)
                )
    }

    /**
     * Bottom sheet enter/exit
     */
    fun bottomSheetEnter(): EnterTransition {
        return slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(Tokens.Duration.bottomSheetExpand)
        ) + fadeIn(animationSpec = tween(Tokens.Duration.fadeIn))
    }

    fun bottomSheetExit(): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(Tokens.Duration.bottomSheetCollapse)
        ) + fadeOut(animationSpec = tween(Tokens.Duration.fadeOut))
    }
}

/**
 * List Item Animations
 */
object ListAnimations {
    /**
     * Fade in + slide up for new list items
     */
    fun itemEnter(index: Int = 0): EnterTransition {
        val delay = (index * 50).coerceAtMost(300) // Stagger up to 300ms

        return fadeIn(
            animationSpec = tween(
                durationMillis = Tokens.Duration.listItemAnimation,
                delayMillis = delay
            )
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(
                durationMillis = Tokens.Duration.listItemAnimation,
                delayMillis = delay
            )
        )
    }

    /**
     * Fade out + slide down for removed items
     */
    fun itemExit(): ExitTransition {
        return fadeOut(
            animationSpec = tween(Tokens.Duration.listItemAnimation)
        ) + slideOutVertically(
            targetOffsetY = { -it / 4 },
            animationSpec = tween(Tokens.Duration.listItemAnimation)
        ) + shrinkVertically(
            animationSpec = tween(Tokens.Duration.listItemAnimation)
        )
    }

    /**
     * Scale animation for item selection
     */
    @Composable
    fun rememberItemScaleAnimation(selected: Boolean): Float {
        val scale by animateFloatAsState(
            targetValue = if (selected) 0.95f else 1f,
            animationSpec = AnimationSpecs.snappySpring,
            label = "item_scale"
        )
        return scale
    }
}

/**
 * FAB Animations
 */
object FABAnimations {
    /**
     * FAB expand animation (to extended FAB)
     */
    @Composable
    fun rememberExpandAnimation(expanded: Boolean): Float {
        return animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = AnimationSpecs.emphasizedEasing,
            label = "fab_expand"
        ).value
    }

    /**
     * FAB rotation for morph animations
     */
    @Composable
    fun rememberRotationAnimation(rotated: Boolean): Float {
        return animateFloatAsState(
            targetValue = if (rotated) 45f else 0f,
            animationSpec = AnimationSpecs.emphasizedEasing,
            label = "fab_rotation"
        ).value
    }

    /**
     * FAB hide/show based on scroll
     */
    @Composable
    fun rememberScrollAnimation(visible: Boolean): Float {
        return animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = AnimationSpecs.standardEasing,
            label = "fab_scroll"
        ).value
    }
}

/**
 * Loading Animations
 */
object LoadingAnimations {
    /**
     * Infinite rotation for loading spinners
     */
    @Composable
    fun rememberInfiniteRotation(): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "loading_rotation")
        return infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        ).value
    }

    /**
     * Pulsing animation for loading indicators
     */
    @Composable
    fun rememberPulseAnimation(): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")
        return infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        ).value
    }

    /**
     * Shimmer animation for skeleton loaders
     */
    @Composable
    fun rememberShimmerAnimation(): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        return infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        ).value
    }
}

/**
 * Feedback Animations
 */
object FeedbackAnimations {
    /**
     * Success checkmark animation
     */
    @Composable
    fun rememberSuccessAnimation(): Float {
        var animationPlayed by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            animationPlayed = true
        }

        return animateFloatAsState(
            targetValue = if (animationPlayed) 1f else 0f,
            animationSpec = tween(
                durationMillis = Tokens.Duration.medium,
                easing = OvershootInterpolator(2f).toEasing()
            ),
            label = "success"
        ).value
    }

    /**
     * Error shake animation
     */
    @Composable
    fun rememberShakeAnimation(trigger: Boolean): Float {
        var animationPlayed by remember { mutableStateOf(false) }

        LaunchedEffect(trigger) {
            if (trigger) {
                animationPlayed = true
                kotlinx.coroutines.delay(500)
                animationPlayed = false
            }
        }

        return animateFloatAsState(
            targetValue = if (animationPlayed) 1f else 0f,
            animationSpec = tween(
                durationMillis = 500,
                easing = LinearEasing
            ),
            label = "shake"
        ).value
    }

    /**
     * Badge appear animation
     */
    @Composable
    fun rememberBadgeAnimation(visible: Boolean): Float {
        return animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = AnimationSpecs.springSpec,
            label = "badge"
        ).value
    }
}

/**
 * Modifier Extensions for Animations
 */

/**
 * Shake modifier for error feedback
 */
fun Modifier.shake(shakeProgress: Float): Modifier {
    val offset = (shakeProgress * 10 * kotlin.math.sin(shakeProgress * kotlin.math.PI * 6)).toFloat()
    return this.graphicsLayer {
        translationX = offset
    }
}

/**
 * Pulse modifier for attention
 */
fun Modifier.pulse(pulseAlpha: Float): Modifier {
    return this.graphicsLayer {
        alpha = pulseAlpha
        scaleX = 0.9f + (pulseAlpha * 0.1f)
        scaleY = 0.9f + (pulseAlpha * 0.1f)
    }
}

/**
 * Scale modifier for selections
 */
fun Modifier.animatedScale(scale: Float): Modifier {
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Rotation modifier
 */
fun Modifier.animatedRotation(rotation: Float): Modifier {
    return this.graphicsLayer {
        rotationZ = rotation
    }
}

/**
 * Helper function to convert Android Interpolator to Compose Easing
 */
private fun android.view.animation.Interpolator.toEasing(): Easing {
    return Easing { fraction -> this.getInterpolation(fraction) }
}

/**
 * Overshoot Interpolator for bouncy animations
 */
private class OvershootInterpolator(private val tension: Float = 2f) :
    android.view.animation.Interpolator {
    override fun getInterpolation(input: Float): Float {
        var t = input
        t -= 1.0f
        return t * t * ((tension + 1) * t + tension) + 1.0f
    }
}

/**
 * Anticipate Overshoot Interpolator
 */
private class AnticipateOvershootInterpolator(private val tension: Float = 2f) :
    android.view.animation.Interpolator {
    override fun getInterpolation(input: Float): Float {
        return if (input < 0.5f) {
            0.5f * anticipate(input * 2.0f, tension)
        } else {
            0.5f * (overshoot(input * 2.0f - 2.0f, tension) + 2.0f)
        }
    }

    private fun anticipate(t: Float, s: Float): Float {
        return t * t * ((s + 1) * t - s)
    }

    private fun overshoot(t: Float, s: Float): Float {
        return t * t * ((s + 1) * t + s)
    }
}

/**
 * Crossfade between composables
 */
@Composable
fun <T> AnimatedContent(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedContentScope.(T) -> Unit
) {
    androidx.compose.animation.AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(Tokens.Duration.normal)) togetherWith
                    fadeOut(animationSpec = tween(Tokens.Duration.normal))
        },
        label = "animated_content",
        content = content
    )
}
