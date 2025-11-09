package com.example.kosmos.shared.ui.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design Tokens for Kosmos App
 *
 * Centralized design constants following Material Design 3 guidelines
 * optimized for mobile-first, power-user experience.
 *
 * All spacing follows 4dp grid system for consistency.
 */
object Tokens {

    /**
     * Spacing Scale (4dp grid)
     * Use these for margins, padding, gaps between elements
     */
    object Spacing {
        val xxs: Dp = 4.dp    // Inner chip padding, icon margins
        val xs: Dp = 8.dp     // Component internal spacing
        val sm: Dp = 12.dp    // Small gaps between related items
        val md: Dp = 16.dp    // Standard spacing (default, most common)
        val lg: Dp = 24.dp    // Section spacing
        val xl: Dp = 32.dp    // Major section breaks
        val xxl: Dp = 48.dp   // Screen top/bottom padding
    }

    /**
     * Touch Target Sizes
     * Following Material Design accessibility guidelines
     */
    object TouchTarget {
        val minimum: Dp = 48.dp           // Absolute minimum for accessibility
        val recommended: Dp = 56.dp       // Recommended for primary actions
        val comfortable: Dp = 64.dp       // Extra comfortable for important actions
        val iconButton: Dp = 48.dp        // Standard icon button size
        val fab: Dp = 56.dp               // Standard FAB size
        val fabMini: Dp = 40.dp           // Mini FAB size
        val listItemMinHeight: Dp = 56.dp // Minimum list item height
    }

    /**
     * Component Sizing
     * Standard sizes for common UI elements
     */
    object Size {
        // Avatar sizes
        val avatarSmall: Dp = 24.dp
        val avatarMedium: Dp = 40.dp
        val avatarLarge: Dp = 56.dp
        val avatarXLarge: Dp = 80.dp
        val avatarXXLarge: Dp = 120.dp

        // Icon sizes
        val iconSmall: Dp = 16.dp
        val iconMedium: Dp = 24.dp
        val iconLarge: Dp = 32.dp

        // Chip sizes
        val chipHeight: Dp = 32.dp

        // Badge sizes
        val badgeSmall: Dp = 16.dp
        val badgeMedium: Dp = 20.dp

        // Online status indicator
        val statusDot: Dp = 8.dp
        val statusDotWithBorder: Dp = 12.dp

        // Progress indicators
        val progressSmall: Dp = 16.dp
        val progressMedium: Dp = 24.dp
        val progressLarge: Dp = 48.dp

        // Divider
        val dividerThickness: Dp = 1.dp

        // Message bubble tail
        val messageTail: Dp = 8.dp
    }

    /**
     * Elevation Levels
     * Material Design 3 elevation scale
     */
    object Elevation {
        val level0: Dp = 0.dp    // Flat, no shadow
        val level1: Dp = 1.dp    // Subtle raise (cards)
        val level2: Dp = 3.dp    // Medium raise (buttons, chips)
        val level3: Dp = 6.dp    // Higher raise (FAB, modal)
        val level4: Dp = 8.dp    // Navigation drawer
        val level5: Dp = 12.dp   // Dialog, picker
    }

    /**
     * Border Radius / Corner Rounding
     * Consistent rounded corners throughout the app
     */
    object CornerRadius {
        val none: Dp = 0.dp
        val xs: Dp = 4.dp      // Small elements (badges, chips)
        val sm: Dp = 8.dp      // Buttons, small cards
        val md: Dp = 12.dp     // Standard cards, dialogs
        val lg: Dp = 16.dp     // Large cards, bottom sheets
        val xl: Dp = 24.dp     // Extra large cards
        val xxl: Dp = 28.dp    // Rounded sections
        val full: Dp = 9999.dp // Fully rounded (pills, avatars)
    }

    /**
     * Animation Durations
     * Consistent timing for all animations
     */
    object Duration {
        // Standard Material Motion durations
        const val instant: Int = 0          // No animation
        const val fast: Int = 100           // Very quick feedback (ripple, small changes)
        const val normal: Int = 200         // Standard animations (most transitions)
        const val medium: Int = 300         // Moderate animations (sheet expansion)
        const val slow: Int = 400           // Slower animations (enter/exit)
        const val slowest: Int = 500        // Complex animations (multi-step)

        // Specific animation durations
        const val ripple: Int = 150
        const val buttonPress: Int = 100
        const val fadeIn: Int = 200
        const val fadeOut: Int = 150
        const val slideIn: Int = 300
        const val slideOut: Int = 250
        const val bottomSheetExpand: Int = 300
        const val bottomSheetCollapse: Int = 250
        const val dialogEnter: Int = 200
        const val dialogExit: Int = 150
        const val listItemAnimation: Int = 150
        const val sharedElement: Int = 300
    }

    /**
     * Animation Easing Curves
     * Standard easing for natural motion
     */
    object Easing {
        // Material Design standard easing
        const val standardAccelerate = 0.4f   // Start slow, accelerate
        const val standardDecelerate = 0.0f   // Start fast, decelerate
        const val emphasized = 0.2f           // Material 3 emphasized easing

        // Cubic bezier curves (for custom animations if needed)
        // Standard: cubic-bezier(0.4, 0.0, 0.2, 1)
        // Decelerate: cubic-bezier(0.0, 0.0, 0.2, 1)
        // Accelerate: cubic-bezier(0.4, 0.0, 1, 1)
        // Sharp: cubic-bezier(0.4, 0.0, 0.6, 1)
    }

    /**
     * Z-Index Layers
     * Stacking order for overlapping elements
     */
    object ZIndex {
        const val background = 0f
        const val content = 1f
        const val elevated = 2f
        const val sticky = 3f
        const val fab = 4f
        const val bottomNav = 5f
        const val snackbar = 6f
        const val bottomSheet = 7f
        const val dialog = 8f
        const val tooltip = 9f
        const val notification = 10f
    }

    /**
     * Opacity Levels
     * Standard opacity values for disabled, inactive states
     */
    object Opacity {
        const val full = 1.0f
        const val high = 0.87f        // Primary text
        const val medium = 0.60f      // Secondary text
        const val disabled = 0.38f    // Disabled state
        const val divider = 0.12f     // Dividers
        const val backdrop = 0.32f    // Modal backdrop
        const val hover = 0.08f       // Hover overlay
        const val pressed = 0.12f     // Pressed state overlay
        const val selected = 0.16f    // Selected state overlay
    }

    /**
     * Border Widths
     * Standard border/stroke widths
     */
    object BorderWidth {
        val thin: Dp = 1.dp       // Standard border
        val medium: Dp = 2.dp     // Emphasized border
        val thick: Dp = 4.dp      // Priority indicator, selected state
    }

    /**
     * Content Widths
     * Maximum widths for content readability
     */
    object ContentWidth {
        val maxMessageBubble: Dp = 280.dp    // Max width for message bubbles
        val maxDialogWidth: Dp = 560.dp      // Max width for dialogs
        val maxCardWidth: Dp = 400.dp        // Max width for standalone cards
    }

    /**
     * Gesture Thresholds
     * Distances and velocities for gesture detection
     */
    object Gesture {
        val swipeThreshold: Dp = 56.dp        // Minimum swipe distance to trigger action
        val longPressDelay: Long = 500        // Milliseconds for long press
        val doubleTapDelay: Long = 300        // Max time between taps for double-tap
        const val swipeVelocityThreshold: Float = 1000f  // Minimum velocity (px/s) for swipe
    }

    /**
     * Scroll Behavior
     * Thresholds for scroll-based UI changes
     */
    object Scroll {
        val fabHideThreshold: Dp = 100.dp     // Scroll distance to hide FAB
        val headerCollapseThreshold: Dp = 56.dp  // Scroll to collapse header
        val pullToRefreshThreshold: Dp = 80.dp   // Pull distance to trigger refresh
    }

    /**
     * Message Grouping
     * Timing for message grouping logic
     */
    object MessageGrouping {
        const val timeWindowMinutes: Long = 5  // Group messages within 5 minutes
        const val timeWindowMillis: Long = 300_000  // 5 minutes in milliseconds
    }

    /**
     * Performance Thresholds
     * Target metrics for performance optimization
     */
    object Performance {
        const val targetFps: Int = 60
        const val maxFrameTimeMs: Long = 16  // ~60fps
        const val optimisticUiDelayMs: Long = 100  // Max delay for optimistic UI
        const val imageFadeInMs: Int = 200   // Image placeholder fade duration
        const val listBufferItems: Int = 20  // Items to buffer before/after viewport
    }
}
