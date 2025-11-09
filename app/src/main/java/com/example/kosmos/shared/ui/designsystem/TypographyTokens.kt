package com.example.kosmos.shared.ui.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography Tokens for Kosmos App
 *
 * Mobile-optimized type scale following Material Design 3 guidelines.
 * All font sizes are designed for readability on mobile screens.
 *
 * Type Scale Hierarchy:
 * - Display: Large, impactful text (headlines, empty states)
 * - Headline: Section headers, screen titles
 * - Title: Subsection headers, card titles
 * - Body: Main content text
 * - Label: UI labels, buttons, chips
 */
object TypographyTokens {

    /**
     * Default font family
     * Can be replaced with custom fonts later
     */
    val defaultFontFamily = FontFamily.Default

    /**
     * Material Design 3 Typography Scale
     * Optimized for mobile readability
     */
    val typography = Typography(
        // Display styles - Large, impactful text
        displayLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),

        // Headline styles - Section headers, screen titles
        headlineLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),

        // Title styles - Card titles, subsection headers
        titleLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),

        // Body styles - Main content text
        bodyLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),

        // Label styles - Buttons, chips, UI labels
        labelLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )

    /**
     * Custom Text Styles for Specific Use Cases
     * These complement the Material 3 typography scale
     */
    object Custom {
        /**
         * Message Bubble Text
         * Optimized for chat message readability
         */
        val messageBubbleText = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.25.sp
        )

        /**
         * Message Timestamp
         * Small, subtle timestamps
         */
        val messageTimestamp = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.4.sp
        )

        /**
         * Message Sender Name
         * Above message bubbles
         */
        val messageSender = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.2.sp
        )

        /**
         * Task Title
         * Bold, prominent task titles
         */
        val taskTitle = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        )

        /**
         * Task Description
         * Secondary text for task details
         */
        val taskDescription = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.25.sp
        )

        /**
         * Task Metadata
         * Small labels for due date, assignee, etc.
         */
        val taskMetadata = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        )

        /**
         * Project Card Title
         * Prominent project names
         */
        val projectTitle = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        )

        /**
         * Project Description
         * Secondary text for project details
         */
        val projectDescription = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        )

        /**
         * User Display Name
         * User names in lists and profiles
         */
        val userDisplayName = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        )

        /**
         * User Email/Username
         * Secondary user information
         */
        val userSecondaryInfo = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        )

        /**
         * Badge Number
         * Numbers in notification badges
         */
        val badgeNumber = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp
        )

        /**
         * Chip Label
         * Text in chips (tags, filters)
         */
        val chipLabel = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.1.sp
        )

        /**
         * Button Text
         * Text buttons and icon button labels
         */
        val buttonText = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        )

        /**
         * Bottom Nav Label
         * Bottom navigation text
         */
        val bottomNavLabel = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )

        /**
         * Empty State Title
         * Large, prominent empty state headings
         */
        val emptyStateTitle = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.15.sp
        )

        /**
         * Empty State Description
         * Supportive text for empty states
         */
        val emptyStateDescription = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        )

        /**
         * Section Header
         * Headers for grouped content
         */
        val sectionHeader = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        )

        /**
         * Caption
         * Small captions and hints
         */
        val caption = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        )

        /**
         * Overline
         * Small all-caps labels
         */
        val overline = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.5.sp
        )

        /**
         * Tab Label
         * Tab text in tab rows
         */
        val tabLabel = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        )

        /**
         * Search Field
         * Text in search input
         */
        val searchField = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        )

        /**
         * Input Field
         * Text in form fields
         */
        val inputField = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        )

        /**
         * Input Label
         * Labels for form fields
         */
        val inputLabel = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        )

        /**
         * Helper Text
         * Helper/error text below inputs
         */
        val helperText = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        )

        /**
         * Snackbar Text
         * Text in snackbar messages
         */
        val snackbarText = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        )

        /**
         * Dialog Title
         * Titles in dialogs and bottom sheets
         */
        val dialogTitle = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.15.sp
        )

        /**
         * Dialog Content
         * Body text in dialogs
         */
        val dialogContent = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        )
    }

    /**
     * Font Weight Presets
     * Quick access to font weights
     */
    object Weight {
        val light = FontWeight.Light        // 300
        val normal = FontWeight.Normal      // 400
        val medium = FontWeight.Medium      // 500
        val semiBold = FontWeight.SemiBold  // 600
        val bold = FontWeight.Bold          // 700
        val extraBold = FontWeight.ExtraBold // 800
    }

    /**
     * Line Height Multipliers
     * For dynamic line height calculations
     */
    object LineHeight {
        const val tight = 1.2f     // Compact text
        const val normal = 1.5f    // Default line height
        const val relaxed = 1.75f  // Comfortable reading
    }

    /**
     * Letter Spacing Presets
     * Common letter spacing values
     */
    object LetterSpacing {
        val none = 0.sp
        val tight = 0.1.sp
        val normal = 0.25.sp
        val loose = 0.5.sp
        val extraLoose = 1.5.sp  // For overlines/all-caps
    }
}
