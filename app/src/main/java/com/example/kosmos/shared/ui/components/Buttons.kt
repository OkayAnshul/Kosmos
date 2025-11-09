package com.example.kosmos.shared.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Button Components for Kosmos App
 *
 * Touch-optimized button variants following Material Design 3 guidelines.
 * All buttons meet minimum 48dp touch target requirement.
 *
 * Components:
 * - PrimaryButton: Main CTA button (filled)
 * - SecondaryButton: Secondary actions (outlined)
 * - TextButton: Low emphasis actions
 * - IconButtonStandard: Icon-only button
 * - LoadingButton: Button with loading state
 * - FABStandard: Floating action button
 */

/**
 * Primary Button - Main Call to Action
 *
 * Filled button for primary actions (Send, Create, Save, etc.)
 * Minimum touch target: 48dp height
 *
 * @param text Button label
 * @param onClick Click handler
 * @param modifier Modifier
 * @param enabled Whether button is enabled
 * @param icon Optional leading icon
 * @param fullWidth Whether button fills available width
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fullWidth: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .then(
                if (fullWidth) Modifier.fillMaxWidth()
                else Modifier
            )
            .heightIn(min = Tokens.TouchTarget.minimum),
        enabled = enabled,
        contentPadding = PaddingValues(
            horizontal = Tokens.Spacing.md,
            vertical = Tokens.Spacing.sm
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Tokens.Size.iconMedium)
            )
            Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
        }
        Text(
            text = text,
            style = TypographyTokens.Custom.buttonText
        )
    }
}

/**
 * Secondary Button - Secondary Actions
 *
 * Outlined button for secondary actions (Cancel, Discard, etc.)
 * Minimum touch target: 48dp height
 *
 * @param text Button label
 * @param onClick Click handler
 * @param modifier Modifier
 * @param enabled Whether button is enabled
 * @param icon Optional leading icon
 * @param fullWidth Whether button fills available width
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fullWidth: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .then(
                if (fullWidth) Modifier.fillMaxWidth()
                else Modifier
            )
            .heightIn(min = Tokens.TouchTarget.minimum),
        enabled = enabled,
        contentPadding = PaddingValues(
            horizontal = Tokens.Spacing.md,
            vertical = Tokens.Spacing.sm
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Tokens.Size.iconMedium)
            )
            Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
        }
        Text(
            text = text,
            style = TypographyTokens.Custom.buttonText
        )
    }
}

/**
 * Text Button - Low Emphasis Actions
 *
 * Text-only button for tertiary actions (Skip, Learn More, etc.)
 * Minimum touch target: 48dp height
 *
 * @param text Button label
 * @param onClick Click handler
 * @param modifier Modifier
 * @param enabled Whether button is enabled
 * @param icon Optional leading icon
 */
@Composable
fun TextButtonStandard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Tokens.TouchTarget.minimum),
        enabled = enabled,
        contentPadding = PaddingValues(
            horizontal = Tokens.Spacing.md,
            vertical = Tokens.Spacing.sm
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Tokens.Size.iconMedium)
            )
            Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
        }
        Text(
            text = text,
            style = TypographyTokens.Custom.buttonText
        )
    }
}

/**
 * Icon Button - Icon-Only Action
 *
 * Circular icon button for toolbar actions
 * Fixed size: 48x48dp (touch target compliant)
 *
 * @param icon Icon to display
 * @param onClick Click handler
 * @param contentDescription Accessibility description
 * @param modifier Modifier
 * @param enabled Whether button is enabled
 */
@Composable
fun IconButtonStandard(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(Tokens.TouchTarget.iconButton),
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(Tokens.Size.iconMedium)
        )
    }
}

/**
 * Loading Button - Button with Loading State
 *
 * Button that shows loading indicator when isLoading is true
 * Useful for async operations (submitting forms, network requests)
 *
 * @param text Button label
 * @param onClick Click handler
 * @param isLoading Whether button is in loading state
 * @param modifier Modifier
 * @param enabled Whether button is enabled (auto-disabled when loading)
 * @param fullWidth Whether button fills available width
 */
@Composable
fun LoadingButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .then(
                if (fullWidth) Modifier.fillMaxWidth()
                else Modifier
            )
            .heightIn(min = Tokens.TouchTarget.minimum),
        enabled = enabled && !isLoading,
        contentPadding = PaddingValues(
            horizontal = Tokens.Spacing.md,
            vertical = Tokens.Spacing.sm
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Tokens.Size.iconMedium),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
        }
        Text(
            text = if (isLoading) "Loading..." else text,
            style = TypographyTokens.Custom.buttonText
        )
    }
}

/**
 * FAB Standard - Floating Action Button
 *
 * Standard FAB for primary screen actions
 * Size: 56x56dp (Material 3 standard)
 *
 * @param icon Icon to display
 * @param onClick Click handler
 * @param contentDescription Accessibility description
 * @param modifier Modifier
 * @param expanded Whether FAB should show text (Extended FAB)
 * @param text Text for extended FAB
 */
@Composable
fun FABStandard(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    text: String? = null
) {
    if (expanded && text != null) {
        // Extended FAB with text
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription
                )
            },
            text = {
                Text(
                    text = text,
                    style = TypographyTokens.Custom.buttonText
                )
            }
        )
    } else {
        // Standard FAB
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier.size(Tokens.TouchTarget.fab)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription
            )
        }
    }
}

/**
 * FAB Mini - Small Floating Action Button
 *
 * Smaller FAB for secondary actions
 * Size: 40x40dp
 *
 * @param icon Icon to display
 * @param onClick Click handler
 * @param contentDescription Accessibility description
 * @param modifier Modifier
 */
@Composable
fun FABMini(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(Tokens.TouchTarget.fabMini)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(Tokens.Size.iconSmall)
        )
    }
}

/**
 * Destructive Button - For Delete/Remove Actions
 *
 * Red-colored button for destructive actions
 * Shows warning color to indicate danger
 *
 * @param text Button label
 * @param onClick Click handler
 * @param modifier Modifier
 * @param enabled Whether button is enabled
 * @param fullWidth Whether button fills available width
 */
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .then(
                if (fullWidth) Modifier.fillMaxWidth()
                else Modifier
            )
            .heightIn(min = Tokens.TouchTarget.minimum),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        contentPadding = PaddingValues(
            horizontal = Tokens.Spacing.md,
            vertical = Tokens.Spacing.sm
        )
    ) {
        Text(
            text = text,
            style = TypographyTokens.Custom.buttonText
        )
    }
}

/**
 * Toggle Button Group - Radio Button Alternative
 *
 * Segmented buttons for mutually exclusive options
 * Better UX than radio buttons on mobile
 *
 * @param options List of options
 * @param selectedIndex Currently selected index
 * @param onSelectionChange Selection change handler
 * @param modifier Modifier
 */
@Composable
fun ToggleButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(Tokens.TouchTarget.minimum),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
    ) {
        options.forEachIndexed { index, option ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onSelectionChange(index) },
                label = {
                    Text(
                        text = option,
                        style = TypographyTokens.Custom.chipLabel
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Button Group - Horizontal Button Layout
 *
 * Common pattern for action buttons (OK/Cancel, Yes/No, etc.)
 *
 * @param primaryText Primary button text
 * @param onPrimaryClick Primary button click handler
 * @param secondaryText Secondary button text
 * @param onSecondaryClick Secondary button click handler
 * @param modifier Modifier
 * @param primaryEnabled Primary button enabled state
 * @param secondaryEnabled Secondary button enabled state
 */
@Composable
fun ButtonGroup(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryEnabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SecondaryButton(
            text = secondaryText,
            onClick = onSecondaryClick,
            enabled = secondaryEnabled,
            modifier = Modifier.weight(1f)
        )
        PrimaryButton(
            text = primaryText,
            onClick = onPrimaryClick,
            enabled = primaryEnabled,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Pill Button - Fully Rounded Button
 *
 * Decorative rounded button for tags, filters
 *
 * @param text Button text
 * @param onClick Click handler
 * @param modifier Modifier
 * @param selected Whether button is in selected state
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = TypographyTokens.Custom.chipLabel
            )
        },
        modifier = modifier
    )
}
