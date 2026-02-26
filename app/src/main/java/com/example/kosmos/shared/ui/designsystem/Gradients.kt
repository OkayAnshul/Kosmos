package com.example.kosmos.shared.ui.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

/**
 * Gradient Definitions for Kosmos App
 *
 * Pre-defined gradients for consistent visual effects across the app.
 * Supports both Better Blur auth design and Stitch main app design.
 */
object Gradients {

    /**
     * Better Blur Design System Gradients
     * For auth screens (Login/SignUp) with glassmorphism effect
     */
    object BetterBlur {
        /**
         * Main auth background gradient
         * Dark blue → Dark teal vertical gradient
         */
        val authBackground = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A237E),  // Deep blue (top)
                Color(0xFF004D40)   // Dark teal (bottom)
            )
        )

        /**
         * Alternative auth background
         * Navy → Deep purple
         */
        val authBackgroundAlt = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D47A1),  // Navy blue
                Color(0xFF4A148C)   // Deep purple
            )
        )

        /**
         * Card overlay gradient for glassmorphism
         * Subtle white gradient for blur effect
         */
        val glassOverlay = Brush.verticalGradient(
            colors = listOf(
                Color(0x33FFFFFF),  // 20% white (top)
                Color(0x1AFFFFFF)   // 10% white (bottom)
            )
        )

        /**
         * Logo card background
         * Vibrant blue gradient for logo container
         */
        val logoCard = Brush.linearGradient(
            colors = listOf(
                Color(0xFF2196F3),  // Material Blue
                Color(0xFF1976D2)   // Darker Blue
            ),
            start = Offset(0f, 0f),
            end = Offset.Infinite
        )
    }

    /**
     * Stitch Design System Gradients
     * For main app screens with navy theme
     */
    object Stitch {
        /**
         * Card shimmer effect
         * Subtle gradient for loading states
         */
        val shimmer = Brush.linearGradient(
            colors = listOf(
                Color(0x1AFFFFFF),  // Transparent
                Color(0x33FFFFFF),  // 20% white
                Color(0x1AFFFFFF)   // Transparent
            ),
            start = Offset(-200f, -200f),
            end = Offset(200f, 200f)
        )

        /**
         * Card background gradient
         * Subtle depth for elevated cards
         */
        val cardBackground = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2E3347),  // Lighter navy (top)
                Color(0xFF252A3A)   // Darker navy (bottom)
            )
        )

        /**
         * Status badge gradient
         * For active/syncing status indicators
         */
        val statusActive = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF4CAF50),  // Green
                Color(0xFF66BB6A)   // Light green
            )
        )

        /**
         * Progress bar gradient
         * For task completion indicators
         */
        val progressHigh = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF4CAF50),  // Green
                Color(0xFF81C784)   // Lighter green
            )
        )

        val progressMedium = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFFA726),  // Orange
                Color(0xFFFFB74D)   // Lighter orange
            )
        )

        val progressLow = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFF44336),  // Red
                Color(0xFFE57373)   // Lighter red
            )
        )
    }

    /**
     * Common Gradients
     * Used across both design systems
     */
    object Common {
        /**
         * Primary brand gradient - Midnight Plum
         * Deep purple to bright purple
         */
        val primary = Brush.linearGradient(
            colors = listOf(
                Color(0xFF6200EA),  // Deep Purple 600
                Color(0xFF9C27B0)   // Purple 700
            )
        )

        /**
         * Success gradient
         * For successful actions and confirmations
         */
        val success = Brush.linearGradient(
            colors = listOf(
                Color(0xFF4CAF50),  // Green
                Color(0xFF388E3C)   // Darker green
            )
        )

        /**
         * Error gradient
         * For errors and destructive actions
         */
        val error = Brush.linearGradient(
            colors = listOf(
                Color(0xFFF44336),  // Red
                Color(0xFFD32F2F)   // Darker red
            )
        )

        /**
         * Scrim gradient
         * For modal overlays and dialogs
         */
        val scrim = Brush.verticalGradient(
            colors = listOf(
                Color(0x00000000),  // Transparent (top)
                Color(0xAA000000)   // 67% black (bottom)
            )
        )
    }

    /**
     * Midnight Plum Neumorphic Gradients
     * Specialized gradients for neumorphic design system
     */
    object MidnightPlum {
        /**
         * Primary neumorphic gradient
         * Deep purple to bright purple - for cards and buttons
         */
        val primary = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF6200EA),  // Deep Purple 600 (top)
                Color(0xFF9C27B0)   // Purple 700 (bottom)
            )
        )

        /**
         * Secondary lavender gradient
         * Soft purple tones for accents
         */
        val secondary = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFCE93D8),  // Purple 200 (top)
                Color(0xFFE1BEE7)   // Purple 100 (bottom)
            )
        )

        /**
         * Surface gradient for neumorphic cards
         * Subtle cool gray with purple undertones
         */
        val surface = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF5F5F7),  // Light cool gray (top)
                Color(0xFFFFFFFF)   // Pure white (bottom)
            )
        )

        /**
         * Card background with subtle purple tint
         * For elevated neumorphic elements
         */
        val cardBackground = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFDFAFF),  // Cool white with purple tint (top)
                Color(0xFFFAFAFA)   // Near white (bottom)
            )
        )

        /**
         * Dark mode surface gradient
         * Navy-purple tones for dark theme
         */
        val surfaceDark = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2C2438),  // Lighter navy-purple (top)
                Color(0xFF1A1625)   // Deep navy-purple (bottom)
            )
        )

        /**
         * Shimmer loading effect - purple tinted
         * For skeleton screens and loading states
         */
        val shimmer = Brush.linearGradient(
            colors = listOf(
                Color(0xFFE8E5EC),  // Light purple-gray
                Color(0xFFF5F5F7),  // Cool white
                Color(0xFFE8E5EC)   // Light purple-gray
            ),
            start = Offset(-200f, -200f),
            end = Offset(200f, 200f)
        )

        /**
         * Shimmer dark mode
         * Navy-purple shimmer for dark theme
         */
        val shimmerDark = Brush.linearGradient(
            colors = listOf(
                Color(0xFF2C2438),  // Navy-purple
                Color(0xFF3F3852),  // Lighter navy-purple
                Color(0xFF2C2438)   // Navy-purple
            ),
            start = Offset(-200f, -200f),
            end = Offset(200f, 200f)
        )
    }
}
