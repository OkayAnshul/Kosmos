package com.example.kosmos.shared.ui.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Color Tokens for Kosmos App
 *
 * Semantic color system following Material Design 3 guidelines.
 * All colors are defined with light and dark mode variants.
 *
 * Usage: Reference colors by semantic meaning, not by specific values.
 * Example: Use ColorTokens.Primary instead of hardcoded blue
 */
object ColorTokens {

    /**
     * Primary Brand Colors
     * Main brand identity colors for key actions and branding
     */
    object Primary {
        // Light mode
        val light = Color(0xFF2196F3)              // Modern Blue
        val lightContainer = Color(0xFFE3F2FD)     // Light blue container
        val onLight = Color(0xFFFFFFFF)            // Text on primary
        val onLightContainer = Color(0xFF0D47A1)   // Text on primary container

        // Dark mode
        val dark = Color(0xFF90CAF9)               // Light blue for dark theme
        val darkContainer = Color(0xFF1565C0)      // Dark blue container
        val onDark = Color(0xFF000000)             // Text on dark primary
        val onDarkContainer = Color(0xFFE3F2FD)    // Text on dark primary container
    }

    /**
     * Secondary/Accent Colors
     * Supporting colors for secondary actions and accents
     */
    object Secondary {
        // Light mode
        val light = Color(0xFF03DAC6)              // Teal
        val lightContainer = Color(0xFFE0F7F5)     // Light teal container
        val onLight = Color(0xFF000000)            // Text on secondary
        val onLightContainer = Color(0xFF00695C)   // Text on secondary container

        // Dark mode
        val dark = Color(0xFF80CBC4)               // Light teal for dark
        val darkContainer = Color(0xFF00796B)      // Dark teal container
        val onDark = Color(0xFF000000)             // Text on dark secondary
        val onDarkContainer = Color(0xFFE0F7F5)    // Text on dark secondary container
    }

    /**
     * Surface Colors
     * Backgrounds for cards, sheets, and elevated content
     */
    object Surface {
        // Light mode
        val light = Color(0xFFFFFBFE)              // Main background
        val lightVariant = Color(0xFFF5F5F5)       // Slightly darker surface
        val lightElevated = Color(0xFFFFFFFF)      // Elevated surface (cards)
        val lightHighest = Color(0xFFECEFF1)       // Highest elevation

        // Dark mode
        val dark = Color(0xFF121212)               // True black for OLED
        val darkVariant = Color(0xFF1E1E1E)        // Slightly lighter
        val darkElevated = Color(0xFF2C2C2C)       // Elevated surface
        val darkHighest = Color(0xFF383838)        // Highest elevation

        // On surface text
        val onLight = Color(0xFF1C1B1F)            // Primary text on light
        val onLightVariant = Color(0xFF49454F)     // Secondary text on light
        val onDark = Color(0xFFE6E1E5)             // Primary text on dark
        val onDarkVariant = Color(0xFFCAC4D0)      // Secondary text on dark
    }

    /**
     * Background Colors
     * Screen backgrounds
     */
    object Background {
        val light = Color(0xFFFFFBFE)              // Light background
        val dark = Color(0xFF121212)               // Dark background (OLED black)
        val onLight = Color(0xFF1C1B1F)            // Text on light background
        val onDark = Color(0xFFE6E1E5)             // Text on dark background
    }

    /**
     * Error/Destructive Colors
     * For errors, warnings, and destructive actions
     */
    object Error {
        val light = Color(0xFFB00020)              // Error red
        val lightContainer = Color(0xFFFFDAD6)     // Error container
        val onLight = Color(0xFFFFFFFF)            // Text on error
        val onLightContainer = Color(0xFF93000A)   // Text on error container

        val dark = Color(0xFFFFB4AB)               // Error red for dark
        val darkContainer = Color(0xFF93000A)      // Error container dark
        val onDark = Color(0xFF690005)             // Text on dark error
        val onDarkContainer = Color(0xFFFFDAD6)    // Text on dark error container
    }

    /**
     * Success Colors
     * For confirmations and completed states
     */
    object Success {
        val light = Color(0xFF4CAF50)              // Success green
        val lightContainer = Color(0xFFE8F5E9)     // Success container
        val onLight = Color(0xFFFFFFFF)            // Text on success
        val onLightContainer = Color(0xFF1B5E20)   // Text on success container

        val dark = Color(0xFF81C784)               // Success green for dark
        val darkContainer = Color(0xFF2E7D32)      // Success container dark
        val onDark = Color(0xFF003300)             // Text on dark success
        val onDarkContainer = Color(0xFFE8F5E9)    // Text on dark success container
    }

    /**
     * Warning Colors
     * For caution states and pending actions
     */
    object Warning {
        val light = Color(0xFFFF9800)              // Warning orange
        val lightContainer = Color(0xFFFFE0B2)     // Warning container
        val onLight = Color(0xFF000000)            // Text on warning
        val onLightContainer = Color(0xFFE65100)   // Text on warning container

        val dark = Color(0xFFFFB74D)               // Warning orange for dark
        val darkContainer = Color(0xFFF57C00)      // Warning container dark
        val onDark = Color(0xFF4E2700)             // Text on dark warning
        val onDarkContainer = Color(0xFFFFE0B2)    // Text on dark warning container
    }

    /**
     * Info Colors
     * For informational messages and hints
     */
    object Info {
        val light = Color(0xFF2196F3)              // Info blue
        val lightContainer = Color(0xFFE3F2FD)     // Info container
        val onLight = Color(0xFFFFFFFF)            // Text on info
        val onLightContainer = Color(0xFF0D47A1)   // Text on info container

        val dark = Color(0xFF64B5F6)               // Info blue for dark
        val darkContainer = Color(0xFF1976D2)      // Info container dark
        val onDark = Color(0xFF001D36)             // Text on dark info
        val onDarkContainer = Color(0xFFE3F2FD)    // Text on dark info container
    }

    /**
     * Outline/Border Colors
     * For borders, dividers, and outlines
     */
    object Outline {
        val light = Color(0xFF79747E)              // Outline for light mode
        val lightVariant = Color(0xFFCAC4D0)       // Lighter outline
        val dark = Color(0xFF938F99)               // Outline for dark mode
        val darkVariant = Color(0xFF49454F)        // Darker outline
    }

    /**
     * Status Colors
     * For user presence and connection status
     */
    object Status {
        // Online/Available
        val online = Color(0xFF4CAF50)             // Green
        val onlineContainer = Color(0xFFE8F5E9)

        // Away/Idle
        val away = Color(0xFFFF9800)               // Orange
        val awayContainer = Color(0xFFFFE0B2)

        // Busy/Do Not Disturb
        val busy = Color(0xFFF44336)               // Red
        val busyContainer = Color(0xFFFFEBEE)

        // Offline
        val offline = Color(0xFF9E9E9E)            // Gray
        val offlineContainer = Color(0xFFF5F5F5)
    }

    /**
     * Priority Colors
     * For task and message priority levels
     */
    object Priority {
        // Urgent/Critical (Red)
        val urgent = Color(0xFFD32F2F)
        val urgentContainer = Color(0xFFFFEBEE)
        val onUrgent = Color(0xFFFFFFFF)

        // High (Orange)
        val high = Color(0xFFFF6F00)
        val highContainer = Color(0xFFFFE0B2)
        val onHigh = Color(0xFF000000)

        // Medium (Yellow)
        val medium = Color(0xFFFBC02D)
        val mediumContainer = Color(0xFFFFF9C4)
        val onMedium = Color(0xFF000000)

        // Low (Blue)
        val low = Color(0xFF1976D2)
        val lowContainer = Color(0xFFE3F2FD)
        val onLow = Color(0xFFFFFFFF)

        // No priority (Gray)
        val none = Color(0xFF757575)
        val noneContainer = Color(0xFFEEEEEE)
        val onNone = Color(0xFFFFFFFF)
    }

    /**
     * Message Bubble Colors
     * For chat message styling
     */
    object Message {
        // Sent messages (user's own messages)
        val sentLight = Color(0xFF2196F3)          // Primary blue
        val sentLightContainer = Color(0xFFE3F2FD) // Light blue
        val onSentLight = Color(0xFFFFFFFF)
        val onSentLightContainer = Color(0xFF0D47A1)

        val sentDark = Color(0xFF1565C0)           // Darker blue for dark mode
        val sentDarkContainer = Color(0xFF0D47A1)
        val onSentDark = Color(0xFFFFFFFF)
        val onSentDarkContainer = Color(0xFFE3F2FD)

        // Received messages (others' messages)
        val receivedLight = Color(0xFFECEFF1)      // Light gray
        val onReceivedLight = Color(0xFF1C1B1F)

        val receivedDark = Color(0xFF2C2C2C)       // Dark gray
        val onReceivedDark = Color(0xFFE6E1E5)

        // System messages
        val systemLight = Color(0xFFFFF9C4)        // Light yellow
        val onSystemLight = Color(0xFF000000)

        val systemDark = Color(0xFF5D4E00)         // Dark yellow
        val onSystemDark = Color(0xFFFFFFFF)
    }

    /**
     * Reaction Colors
     * For emoji reactions and interaction feedback
     */
    object Reaction {
        val backgroundLight = Color(0xFFE3F2FD)    // Light blue
        val backgroundDark = Color(0xFF1E3A5F)     // Dark blue
        val onBackgroundLight = Color(0xFF0D47A1)
        val onBackgroundDark = Color(0xFF90CAF9)

        // User's own reaction
        val selectedLight = Color(0xFF2196F3)
        val selectedDark = Color(0xFF64B5F6)
        val onSelectedLight = Color(0xFFFFFFFF)
        val onSelectedDark = Color(0xFF000000)
    }

    /**
     * Badge Colors
     * For notification badges and counters
     */
    object Badge {
        val light = Color(0xFFD32F2F)              // Red badge
        val lightContainer = Color(0xFFFFEBEE)
        val onLight = Color(0xFFFFFFFF)

        val dark = Color(0xFFEF5350)               // Light red for dark
        val darkContainer = Color(0xFFB71C1C)
        val onDark = Color(0xFFFFFFFF)
    }

    /**
     * Scrim/Overlay Colors
     * For modal backdrops and overlays
     */
    object Scrim {
        val light = Color(0xFF000000)              // Black scrim
        val dark = Color(0xFF000000)               // Black scrim (same for both)

        // Alpha values (multiply with scrim color)
        const val alphaLight = 0.32f               // 32% opacity in light mode
        const val alphaDark = 0.50f                // 50% opacity in dark mode
    }

    /**
     * Gradient Colors
     * For headers, cards, and decorative elements
     */
    object Gradient {
        // Primary gradient
        val primaryStart = Color(0xFF2196F3)
        val primaryEnd = Color(0xFF1976D2)

        // Secondary gradient
        val secondaryStart = Color(0xFF03DAC6)
        val secondaryEnd = Color(0xFF018786)

        // Success gradient
        val successStart = Color(0xFF4CAF50)
        val successEnd = Color(0xFF388E3C)

        // Shimmer gradient (for loading states)
        val shimmerStart = Color(0xFFE0E0E0)
        val shimmerMiddle = Color(0xFFF5F5F5)
        val shimmerEnd = Color(0xFFE0E0E0)

        // Shimmer dark
        val shimmerStartDark = Color(0xFF2C2C2C)
        val shimmerMiddleDark = Color(0xFF383838)
        val shimmerEndDark = Color(0xFF2C2C2C)
    }

    /**
     * Task Status Colors
     * For task board columns and status indicators
     */
    object TaskStatus {
        // To Do
        val todo = Color(0xFF757575)
        val todoContainer = Color(0xFFEEEEEE)
        val onTodo = Color(0xFFFFFFFF)

        // In Progress
        val inProgress = Color(0xFF2196F3)
        val inProgressContainer = Color(0xFFE3F2FD)
        val onInProgress = Color(0xFFFFFFFF)

        // Done/Completed
        val done = Color(0xFF4CAF50)
        val doneContainer = Color(0xFFE8F5E9)
        val onDone = Color(0xFFFFFFFF)

        // Cancelled
        val cancelled = Color(0xFF9E9E9E)
        val cancelledContainer = Color(0xFFF5F5F5)
        val onCancelled = Color(0xFFFFFFFF)
    }

    /**
     * Chart/Data Visualization Colors
     * For future analytics and metrics
     */
    object Chart {
        val blue = Color(0xFF2196F3)
        val green = Color(0xFF4CAF50)
        val orange = Color(0xFFFF9800)
        val red = Color(0xFFF44336)
        val purple = Color(0xFF9C27B0)
        val teal = Color(0xFF009688)
        val pink = Color(0xFFE91E63)
        val yellow = Color(0xFFFFEB3B)
    }

    /**
     * Shadow Colors
     * For elevation and depth
     */
    object Shadow {
        val light = Color(0xFF000000)              // Black shadow
        val dark = Color(0xFF000000)               // Black shadow (same)

        // Alpha values
        const val alphaLight = 0.08f               // Light shadow
        const val alphaMedium = 0.12f              // Medium shadow
        const val alphaHeavy = 0.16f               // Heavy shadow
    }

    /**
     * Ripple/Interaction Colors
     * For touch feedback and interactions
     */
    object Interaction {
        val rippleLight = Color(0xFF000000)
        val rippleDark = Color(0xFFFFFFFF)

        const val rippleAlphaLight = 0.12f
        const val rippleAlphaDark = 0.10f

        const val hoverAlpha = 0.08f
        const val pressedAlpha = 0.12f
        const val selectedAlpha = 0.16f
    }
}
