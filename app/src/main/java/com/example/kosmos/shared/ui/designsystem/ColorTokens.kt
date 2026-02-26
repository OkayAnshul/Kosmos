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
 *
 * React Design Reference: documents/Kosmos/src/styles/theme.css
 * Maps React CSS variables to Android Color tokens
 */
object ColorTokens {

    /**
     * React Design System Colors
     * Mapped from documents/Kosmos/src/styles/theme.css
     * PRIMARY colors for Android implementation to match React design EXACTLY
     */
    object ReactTheme {
        // Primary colors
        val primary = Color(0xFF7C3AED)            // --primary: #7C3AED (purple)
        val primaryForeground = Color(0xFFFFFFFF)  // --primary-foreground: #FFFFFF

        // Background colors (dark mode focused)
        val background = Color(0xFF0F0F14)         // --background: #0F0F14 (very dark gray)
        val foreground = Color(0xFFE8E8ED)         // --foreground: #E8E8ED (light gray text)

        // Surface/Card colors
        val card = Color(0xFF18181D)               // --card: #18181D (dark card bg)
        val cardForeground = Color(0xFFE8E8ED)     // --card-foreground: #E8E8ED

        // Secondary colors
        val secondary = Color(0xFF1F1F27)          // --secondary: #1F1F27 (darker gray)
        val secondaryForeground = Color(0xFFE8E8ED) // --secondary-foreground: #E8E8ED

        // Muted colors
        val muted = Color(0xFF2A2A32)              // --muted: #2A2A32 (medium gray)
        val mutedForeground = Color(0xFF9CA3AF)    // --muted-foreground: #9CA3AF (matches theme.css)

        // Accent colors
        val accent = Color(0xFF7C3AED)             // --accent: #7C3AED (same as primary)
        val accentForeground = Color(0xFFFFFFFF)   // --accent-foreground: #FFFFFF

        // Destructive/Error colors
        val destructive = Color(0xFFEF4444)        // --destructive: #EF4444 (red)
        val destructiveForeground = Color(0xFFFFFFFF) // --destructive-foreground: #FFFFFF

        // Border/Outline colors
        val border = Color(0xFF2A2A32)             // --border: #2A2A32 (same as muted)
        val input = Color(0xFF1F1F27)              // --input: #1F1F27 (same as secondary)
        val ring = Color(0xFF7C3AED)               // --ring: #7C3AED (focus ring, same as primary)
    }

    /**
     * Primary Brand Colors - Midnight Plum Palette
     * Main brand identity colors for key actions and branding
     * Deep purple theme optimized for neumorphic shadows
     */
    object Primary {
        // Light mode
        val light = Color(0xFF6200EA)              // Deep Purple 600 (Midnight Plum)
        val lightContainer = Color(0xFFE1BEE7)     // Purple 100 container
        val onLight = Color(0xFFFFFFFF)            // White text on primary
        val onLightContainer = Color(0xFF4527A0)   // Deep Purple 800 text on container

        // Dark mode
        val dark = Color(0xFFB388FF)               // Light Purple 200 for dark theme
        val darkContainer = Color(0xFF4527A0)      // Deep Purple 800 container
        val onDark = Color(0xFF000000)             // Black text on dark primary
        val onDarkContainer = Color(0xFFE1BEE7)    // Purple 100 text on dark container
    }

    /**
     * Secondary/Accent Colors - Lavender Complement
     * Supporting colors for secondary actions and accents
     * Complementary lavender tones for Midnight Plum palette
     */
    object Secondary {
        // Light mode
        val light = Color(0xFFCE93D8)              // Purple 200 (Lavender)
        val lightContainer = Color(0xFFF3E5F5)     // Purple 50 container
        val onLight = Color(0xFF000000)            // Black text on secondary
        val onLightContainer = Color(0xFF6A1B9A)   // Purple 800 text on container

        // Dark mode
        val dark = Color(0xFFCE93D8)               // Purple 200 for dark
        val darkContainer = Color(0xFF6A1B9A)      // Purple 800 container
        val onDark = Color(0xFF000000)             // Black text on dark secondary
        val onDarkContainer = Color(0xFFF3E5F5)    // Purple 50 text on dark container
    }

    /**
     * Surface Colors - Neumorphic Optimized
     * Backgrounds for cards, sheets, and elevated content
     * Subtle purple tints for better shadow visibility
     */
    object Surface {
        // Light mode - Cool white with purple undertones
        val light = Color(0xFFFDFAFF)              // Cool white with subtle purple tint
        val lightVariant = Color(0xFFF5F5F7)       // Light cool gray
        val lightElevated = Color(0xFFFFFFFF)      // Pure white for elevated cards
        val lightHighest = Color(0xFFECEFF1)       // Highest elevation

        // Dark mode - Navy-purple tones
        val dark = Color(0xFF1A1625)               // Deep navy-purple (not pure black)
        val darkVariant = Color(0xFF2C2438)        // Lighter navy-purple
        val darkElevated = Color(0xFF332D45)       // Elevated surface with purple
        val darkHighest = Color(0xFF3F3852)        // Highest elevation

        // On surface text
        val onLight = Color(0xFF1C1B1F)            // Primary text on light
        val onLightVariant = Color(0xFF49454F)     // Secondary text on light
        val onDark = Color(0xFFE6E1E5)             // Primary text on dark
        val onDarkVariant = Color(0xFFCAC4D0)      // Secondary text on dark
    }

    /**
     * Background Colors - Neumorphic Optimized
     * Screen backgrounds with purple tints for depth perception
     */
    object Background {
        val light = Color(0xFFFAFAFA)              // Near white, neutral base
        val dark = Color(0xFF0D0A1F)               // Nearly black with purple tint
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
     * Gradient Colors - Midnight Plum Palette
     * For headers, cards, and decorative elements
     * Purple-themed gradients optimized for neumorphism
     */
    object Gradient {
        // Primary gradient - Deep purple to bright purple
        val primaryStart = Color(0xFF6200EA)       // Deep Purple 600
        val primaryEnd = Color(0xFF9C27B0)         // Purple 700

        // Secondary gradient - Lavender tones
        val secondaryStart = Color(0xFFCE93D8)     // Purple 200
        val secondaryEnd = Color(0xFFE1BEE7)       // Purple 100

        // Success gradient (keep existing - works well)
        val successStart = Color(0xFF4CAF50)
        val successEnd = Color(0xFF388E3C)

        // Surface gradient - Cool gray with subtle purple
        val surfaceStart = Color(0xFFF5F5F7)
        val surfaceEnd = Color(0xFFFFFFFF)

        // Shimmer gradient (for loading states) - purple tinted
        val shimmerStart = Color(0xFFE8E5EC)       // Light purple-gray
        val shimmerMiddle = Color(0xFFF5F5F7)      // Cool white
        val shimmerEnd = Color(0xFFE8E5EC)         // Light purple-gray

        // Shimmer dark - navy-purple tones
        val shimmerStartDark = Color(0xFF2C2438)
        val shimmerMiddleDark = Color(0xFF3F3852)
        val shimmerEndDark = Color(0xFF2C2438)
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

    /**
     * Progress Colors
     * Visual feedback for completion percentages
     */
    object Progress {
        val high = Color(0xFF4CAF50)       // Green (>80% completion)
        val medium = Color(0xFFFFA726)     // Orange (50-80% completion)
        val low = Color(0xFFF44336)        // Red (<50% completion)
        val background = Color(0xFFE0E0E0) // Progress bar background
    }

    /**
     * Offline/Sync Banner Colors
     * Status indicators for offline mode and syncing states
     */
    object Banner {
        val offline = Color(0xFFFFA726)         // Orange for offline mode
        val syncing = Color(0xFF2196F3)         // Blue for syncing
        val onOffline = Color(0xFF000000)       // Text on offline banner
        val onSyncing = Color(0xFFFFFFFF)       // Text on syncing banner
    }

    /**
     * Stitch Design System Colors
     * Navy-themed colors matching reference designs exactly
     * Reference: UI-redesign-reference/stitch/
     */
    object Stitch {
        val backgroundPrimary = Color(0xFF101322)    // Very dark navy (#101322)
        val surface = Color(0xFF1C2136)              // Surface color (#1c2136 to #1e2235)
        val surfaceVariant = Color(0xFF1E2235)       // Variant surface (#1e2235)
        val cardBackground = Color(0xFF1C2136)       // Card backgrounds (same as surface)
        val primary = Color(0xFF1132D4)              // Bright blue (#1132d4)
        val textPrimary = Color(0xFFFFFFFF)          // White text
        val textSecondary = Color(0xFF929BC9)        // Muted text (#929bc9)
        val textOnPrimary = Color(0xFFFFFFFF)        // White text on colored backgrounds
        val border = Color(0xFF323B67)               // Border color (#323b67)

        // Status colors (keeping existing for consistency)
        val success = Color(0xFF4CAF50)              // Green for success/completion
        val warning = Color(0xFFFFA726)              // Orange for warnings/medium priority
        val error = Color(0xFFF44336)                // Red for errors/high priority/destructive actions
    }
}
