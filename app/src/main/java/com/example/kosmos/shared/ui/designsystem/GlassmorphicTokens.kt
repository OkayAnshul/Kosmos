package com.example.kosmos.shared.ui.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphic Design Tokens
 *
 * Complete glassmorphism system based on 2026 UI trends:
 * - Frosted glass effect with translucent panels
 * - Vibrant gradient backgrounds
 * - Soft shadows and blur effects
 * - Material You 3.0 motion physics
 *
 * References:
 * - https://www.designstudiouiux.com/blog/what-is-glassmorphism-ui-trend/
 * - https://www.techqware.com/blog/material-you-30-the-new-ui-era-for-android-apps
 * - https://lomatechnology.com/blog/motion-ui-trends-2026/2911
 */
object GlassmorphicTokens {

    /**
     * Glass Surface Properties
     * Used for frosted glass card backgrounds
     */
    object Glass {
        // Opacity levels for different importance
        const val alphaPrimary = 0.85f           // Main content cards (projects, tasks)
        const val alphaSecondary = 0.90f         // Secondary panels
        const val alphaHover = 0.88f             // Hover state (slightly more opaque)
        const val alphaPressed = 0.92f           // Pressed state (even more opaque)

        // Blur radius (backdrop filter blur)
        val blurPrimary = 10.dp                  // Standard glass blur
        val blurSecondary = 6.dp                 // Subtle blur
        val blurIntense = 16.dp                  // Intense blur for modals

        // Border properties
        val borderWidth = 1.dp
        const val borderAlpha = 0.3f             // White border alpha
        val borderColor = Color.White.copy(alpha = borderAlpha)

        // Shadow properties (soft, subtle shadows for depth)
        val shadowElevation = 2.dp
        const val shadowAlpha = 0.12f            // Soft shadow for glassmorphism
        val shadowColor = Color.Black.copy(alpha = shadowAlpha)
    }

    /**
     * Gradient Backgrounds
     * Vibrant gradients visible through glass layers
     * Midnight Plum theme with purple tones
     */
    object GradientBackground {
        // Primary gradient (for main screens)
        val primaryStart = Color(0xFFF5F3FF)     // Very light purple
        val primaryMiddle = Color(0xFFE8E1FF)    // Light purple
        val primaryEnd = Color(0xFFD4C6FF)       // Soft purple

        // Card gradient (subtle for glassmorphic cards)
        val cardStart = Color(0xFFFDFAFF)        // Near white with purple tint
        val cardEnd = Color(0xFFF5F3FF)          // Very light purple

        // Accent gradient (for buttons, FABs)
        val accentStart = ColorTokens.Primary.light      // #6200EA
        val accentMiddle = Color(0xFF7C4DFF)    // Purple A200
        val accentEnd = ColorTokens.Primary.light        // #9C27B0

        // Dark mode gradients
        val darkStart = Color(0xFF1A1625)        // Deep navy-purple
        val darkMiddle = Color(0xFF2C2438)       // Lighter navy-purple
        val darkEnd = Color(0xFF3F3852)          // Medium purple-gray
    }

    /**
     * Animation Tokens - Material You 3.0 Motion Physics
     * Spring-based animations with natural bounce
     */
    object Animation {
        // Spring specs for different use cases
        fun <T> springDefault() = spring<T>(
            dampingRatio = Spring.DampingRatioMediumBouncy,  // Natural bounce
            stiffness = Spring.StiffnessMedium
        )

        fun <T> springGentle() = spring<T>(
            dampingRatio = Spring.DampingRatioLowBouncy,     // More bounce
            stiffness = Spring.StiffnessLow
        )

        fun <T> springSnappy() = spring<T>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh                  // Faster response
        )

        // Duration values for non-spring animations
        const val durationFast = 150              // Quick micro-interactions
        const val durationMedium = 250            // Standard transitions
        const val durationSlow = 350              // Deliberate animations

        // Delay for staggered animations
        const val staggerDelay = 50L              // 50ms delay between items

        // Scale values for press feedback
        const val scalePressed = 0.98f            // Subtle press scale
        const val scaleHover = 1.02f              // Subtle hover scale (web/tablet)
        const val scaleNormal = 1.0f

        // Elevation changes for press feedback
        fun elevationPressed(base: Dp) = base / 2  // Half elevation when pressed
        fun elevationHover(base: Dp) = base * 1.2f // 20% more elevation on hover
    }

    /**
     * Micro-Interaction Tokens
     * Subtle feedback for user actions (2026 trend)
     */
    object MicroInteraction {
        // Haptic feedback settings
        const val hapticEnabled = true            // Enable haptic on button press
        const val hapticStrength = 0.5f           // Medium strength

        // Icon morphing settings
        const val iconMorphDuration = 300         // Icon state transition
        const val iconRotation = 180f             // Icon rotation degrees

        // Ripple effect
        const val rippleAlpha = 0.16f             // Ripple overlay alpha
        val rippleColor = ColorTokens.Primary.light.copy(alpha = rippleAlpha)

        // Glow effect for focused elements
        val glowColor = ColorTokens.Primary.light.copy(alpha = 0.4f)
        val glowRadius = 8.dp
    }

    /**
     * Depth Layers - Material You 3.0
     * Layered surfaces for visual hierarchy
     */
    object DepthLayer {
        // Elevation levels
        val level0 = 0.dp                         // Background
        val level1 = 1.dp                         // Cards on background
        val level2 = 3.dp                         // Elevated cards
        val level3 = 6.dp                         // Modals, dialogs
        val level4 = 8.dp                         // Floating action button
        val level5 = 12.dp                        // Navigation bar

        // Z-index for overlays
        const val zIndexBase = 0f
        const val zIndexCard = 1f
        const val zIndexModal = 10f
        const val zIndexTooltip = 20f
        const val zIndexSnackbar = 30f
    }

    /**
     * Typography Enhancement
     * Variable font weights (Material You 3.0)
     */
    object Typography {
        // Dynamic font weights for expressive design
        const val weightThin = 100
        const val weightExtraLight = 200
        const val weightLight = 300
        const val weightRegular = 400
        const val weightMedium = 500
        const val weightSemiBold = 600
        const val weightBold = 700
        const val weightExtraBold = 800
        const val weightBlack = 900

        // Letter spacing for glass text
        const val letterSpacingTight = -0.5f
        const val letterSpacingNormal = 0f
        const val letterSpacingLoose = 0.5f
    }

    /**
     * Whitespace Tokens
     * Generous spacing for minimal design (2026 trend)
     */
    object Whitespace {
        val xxs = 4.dp
        val xs = 8.dp
        val sm = 12.dp
        val md = 16.dp
        val lg = 24.dp
        val xl = 32.dp                            // Between primary cards
        val xxl = 48.dp                           // Between major sections
        val xxxl = 64.dp                          // Screen padding (tablet)
    }

    /**
     * Corner Radius
     * Consistent rounding for glassmorphic surfaces
     */
    object CornerRadius {
        val xs = 8.dp                             // Small elements
        val sm = 12.dp                            // Buttons, inputs
        val md = 16.dp                            // Cards
        val lg = 20.dp                            // Large cards
        val xl = 24.dp                            // Modals, dialogs
        val xxl = 32.dp                           // Full-screen sheets
        val pill = 9999.dp                        // Fully rounded (search bars)
    }

    /**
     * Accessibility Enhancements
     * WCAG AA compliance with glassmorphism
     */
    object Accessibility {
        // Minimum contrast ratio for text on glass
        const val minContrastRatio = 4.5f        // WCAG AA standard

        // Touch target sizes (48dp minimum for Android)
        val minTouchTarget = 48.dp

        // Text sizes for readability on glass
        val minBodyTextSize = 14.dp              // sp equivalent
        val minLabelTextSize = 12.dp

        // Border enhancement for low vision
        val highVisibilityBorder = 2.dp
        val highVisibilityBorderColor = ColorTokens.Primary.light
    }

    /**
     * Loading States
     * Skeleton screens and shimmer effects
     */
    object Loading {
        // Shimmer animation
        const val shimmerDuration = 1200          // Shimmer cycle duration
        const val shimmerAlpha = 0.3f             // Shimmer highlight alpha

        // Skeleton background
        val skeletonBackground = Color(0xFFE8E5EC)  // Light purple-gray
        val skeletonHighlight = Color(0xFFF5F5F7)   // Cool white

        // Progress indicator
        val progressTrackColor = Color(0xFFE0E0E0)
        val progressIndicatorColor = ColorTokens.Primary.light
    }
}
