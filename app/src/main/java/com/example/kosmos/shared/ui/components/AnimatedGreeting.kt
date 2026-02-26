package com.example.kosmos.shared.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import com.example.kosmos.shared.ui.designsystem.ColorTokens
/**
 * Time periods for greeting messages
 */
enum class TimePeriod {
    MORNING, AFTERNOON, EVENING, NIGHT
}

/**
 * Get current time period based on hour of day
 */
private fun getTimePeriod(): TimePeriod {
    val hour = LocalDateTime.now().hour
    return when {
        hour in 5..11 -> TimePeriod.MORNING
        hour in 12..16 -> TimePeriod.AFTERNOON
        hour in 17..20 -> TimePeriod.EVENING
        else -> TimePeriod.NIGHT
    }
}

/**
 * Get random greeting message for the given time period
 */
private fun getGreetingForPeriod(period: TimePeriod): String {
    return when (period) {
        TimePeriod.MORNING -> listOf(
            "Good morning",
            "Rise and shine",
            "Good to see you early",
            "Morning, let's build"
        ).random()

        TimePeriod.AFTERNOON -> listOf(
            "Good afternoon",
            "Welcome back",
            "Keep going",
            "Afternoon focus time"
        ).random()

        TimePeriod.EVENING -> listOf(
            "Good evening",
            "Evening session",
            "Finishing strong",
            "Let's wrap up"
        ).random()

        TimePeriod.NIGHT -> listOf(
            "Burning the midnight oil",
            "Night owl mode",
            "Late night productivity",
            "Still working, respect"
        ).random()
    }
}

/**
 * Build complete greeting message with username
 */
private fun buildGreetingMessage(username: String): String {
    val greeting = getGreetingForPeriod(getTimePeriod())
    return "$greeting, $username!"
}

/**
 * Animated Greeting Component
 *
 * Displays time-based greeting with username (e.g., "Good morning, Anshul!")
 * - Fade in on appear with slide animation
 * - Auto-hides after 3 seconds with elegant fade-out
 * - Random greeting variations based on time of day
 * - Minimal, elegant design
 */
@Composable
fun AnimatedGreeting(
    username: String,
    modifier: Modifier = Modifier,
    textColor: Color = ColorTokens.ReactTheme.foreground,
    greetingDurationMs: Int = 3000,
    fadeOutDurationMs: Int = 1200,  // Increased to match relaxed animation timing
    onHideComplete: (() -> Unit)? = null
) {
    // Return early if username is null or empty
    if (username.isEmpty()) return

    // State management
    var visible by remember { mutableStateOf(false) }
    var shouldShow by remember { mutableStateOf(true) }

    // Generate greeting message (cached per username)
    val displayMessage by remember(username) {
        mutableStateOf(buildGreetingMessage(username))
    }

    // Auto-hide timing sequence
    LaunchedEffect(username) {
        // Reset state
        visible = true
        shouldShow = true

        // Hold for specified duration
        delay(greetingDurationMs.toLong())

        // Start fade-out
        visible = false
    }

    // Handle fade-out completion
    LaunchedEffect(visible) {
        if (!visible) {
            // Wait for fade-out animation to complete
            delay(fadeOutDurationMs.toLong())
            shouldShow = false
            onHideComplete?.invoke()
        }
    }

    // Don't render if hidden
    if (!shouldShow) return

    // Relaxed, calm fade in/out animation (1200ms for smooth, gentle feel)
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = LinearOutSlowInEasing  // Smoother, more relaxed easing
        ),
        label = "fade"
    )

    // Gentle scale animation with minimal bounce (more calm)
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,  // Less dramatic scale (0.85 instead of 0.7)
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,  // No bounce for calm effect
            stiffness = Spring.StiffnessVeryLow  // Very slow, relaxed movement
        ),
        label = "scale"
    )

    // Subtle slide in animation (reduced distance for calm effect)
    val offsetX by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,  // Reduced from 50f to 30f for gentler slide
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,  // No bounce
            stiffness = Spring.StiffnessVeryLow  // Very slow, smooth slide
        ),
        label = "slideX"
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                translationX = offsetX
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Display complete greeting message
        Text(
            text = displayMessage,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            color = textColor
        )
    }
}

// Removed WavingHand - too distracting

/**
 * Waving Hand Emoji Animation
 * Subtle rotate animation
 */
@Composable
private fun WavingHand() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Text(
        text = " 👋",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
            .graphicsLayer {
                rotationZ = rotation
            }
    )
}
