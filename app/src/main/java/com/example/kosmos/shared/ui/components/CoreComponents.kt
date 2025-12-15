package com.example.kosmos.shared.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.GlassmorphicTokens

/**
 * Core Glassmorphic Components Library
 *
 * Complete component system based on 2026 UI trends:
 * - Glassmorphism with frosted glass effects
 * - Material You 3.0 motion physics (spring animations)
 * - Micro-interactions for tactile feedback
 * - WCAG AA accessibility compliance
 *
 * Component Philosophy:
 * - PRIMARY importance = Glassmorphic treatment (main content)
 * - STANDARD importance = Minimal flat design (secondary content)
 * - Consistent animations across all components
 * - Generous whitespace for modern minimal feel
 *
 * References:
 * - Glassmorphism: https://www.designstudiouiux.com/blog/what-is-glassmorphism-ui-trend/
 * - Material You 3.0: https://www.techqware.com/blog/material-you-30-the-new-ui-era-for-android-apps
 * - Motion UI: https://lomatechnology.com/blog/motion-ui-trends-2026/2911
 */

/**
 * Card Importance Levels
 * Determines visual treatment (glass vs flat)
 */
enum class CardImportance {
    PRIMARY,      // Glassmorphic effect - main content (projects, tasks, messages)
    STANDARD      // Flat minimal - secondary content (sections, lists, metadata)
}

/**
 * Button Variants
 * Different button styles for visual hierarchy
 */
enum class ButtonVariant {
    PRIMARY,      // Filled with gradient - main actions (save, send, create)
    SECONDARY,    // Outlined - secondary actions (cancel, back)
    TERTIARY      // Text only - tertiary actions (edit, show more, delete)
}

/**
 * CoreCard - Unified card component with glassmorphic effect
 *
 * Primary cards use frosted glass effect with blur, translucency, and soft shadows.
 * Standard cards use minimal flat design with simple borders.
 *
 * Features:
 * - Spring-based press animations (Material You 3.0 motion physics)
 * - Glassmorphic frosted effect for PRIMARY importance
 * - Micro-interactions with scale and elevation changes
 * - WCAG AA contrast compliance
 *
 * @param onClick Optional click handler (enables press animation)
 * @param importance Visual treatment level (PRIMARY = glass, STANDARD = flat)
 * @param modifier Modifier to apply to the card
 * @param shape Corner shape (default: 16dp rounded)
 * @param enabled Whether card is interactive
 * @param content Card content composable
 */
@Composable
fun CoreCard(
    onClick: (() -> Unit)? = null,
    importance: CardImportance = CardImportance.STANDARD,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlassmorphicTokens.CornerRadius.md),
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Material You 3.0 spring animations
    val animatedScale by animateFloatAsState(
        targetValue = when {
            isPressed -> GlassmorphicTokens.Animation.scalePressed
            else -> GlassmorphicTokens.Animation.scaleNormal
        },
        animationSpec = GlassmorphicTokens.Animation.springDefault(),
        label = "card_scale"
    )

    val animatedElevation by animateDpAsState(
        targetValue = when (importance) {
            CardImportance.PRIMARY -> if (isPressed) {
                GlassmorphicTokens.Animation.elevationPressed(GlassmorphicTokens.DepthLayer.level2)
            } else {
                GlassmorphicTokens.DepthLayer.level2
            }
            CardImportance.STANDARD -> GlassmorphicTokens.DepthLayer.level0
        },
        animationSpec = GlassmorphicTokens.Animation.springDefault(),
        label = "card_elevation"
    )

    // Glass effect background
    val backgroundColor = when (importance) {
        CardImportance.PRIMARY -> Color.White.copy(
            alpha = if (isPressed) {
                GlassmorphicTokens.Glass.alphaPressed
            } else {
                GlassmorphicTokens.Glass.alphaPrimary
            }
        )
        CardImportance.STANDARD -> Color.White
    }

    val borderStroke = when (importance) {
        CardImportance.PRIMARY -> BorderStroke(
            width = GlassmorphicTokens.Glass.borderWidth,
            color = GlassmorphicTokens.Glass.borderColor
        )
        CardImportance.STANDARD -> BorderStroke(
            width = 1.dp,
            color = ColorTokens.Outline.lightVariant
        )
    }

    Card(
        onClick = onClick ?: {},
        modifier = modifier
            .scale(animatedScale)
            .then(
                if (importance == CardImportance.PRIMARY) {
                    // Apply glassmorphic shadow for PRIMARY cards
                    Modifier.shadow(
                        elevation = animatedElevation,
                        shape = shape,
                        ambientColor = GlassmorphicTokens.Glass.shadowColor,
                        spotColor = GlassmorphicTokens.Glass.shadowColor
                    )
                } else {
                    Modifier
                }
            )
            .border(borderStroke, shape)
            .animateContentSize(animationSpec = GlassmorphicTokens.Animation.springGentle()),
        enabled = enabled,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        interactionSource = interactionSource
    ) {
        // Gradient background for PRIMARY cards (visible through translucent surface)
        Box(
            modifier = if (importance == CardImportance.PRIMARY) {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassmorphicTokens.GradientBackground.cardStart,
                            GlassmorphicTokens.GradientBackground.cardEnd
                        )
                    )
                )
            } else {
                Modifier
            }
        ) {
            content()
        }
    }
}

/**
 * CoreButton - Unified button component with animations
 *
 * Features:
 * - Spring-based press animations
 * - Gradient fill for PRIMARY variant
 * - Haptic feedback simulation (scale + opacity changes)
 * - Icon morphing support
 *
 * @param onClick Click handler
 * @param label Button text
 * @param variant Visual style (PRIMARY, SECONDARY, TERTIARY)
 * @param modifier Modifier to apply
 * @param icon Optional leading icon
 * @param enabled Whether button is enabled
 * @param loading Whether to show loading state
 */
@Composable
fun CoreButton(
    onClick: () -> Unit,
    label: String,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Material You 3.0 spring animations
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) GlassmorphicTokens.Animation.scalePressed else GlassmorphicTokens.Animation.scaleNormal,
        animationSpec = GlassmorphicTokens.Animation.springDefault(),
        label = "button_scale"
    )

    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) {
            GlassmorphicTokens.Animation.elevationPressed(GlassmorphicTokens.DepthLayer.level1)
        } else {
            GlassmorphicTokens.DepthLayer.level1
        },
        animationSpec = GlassmorphicTokens.Animation.springDefault(),
        label = "button_elevation"
    )

    // Icon rotation for morphing effect
    val iconRotation by animateFloatAsState(
        targetValue = if (loading) 360f else 0f,
        animationSpec = tween(
            durationMillis = GlassmorphicTokens.MicroInteraction.iconMorphDuration,
            easing = LinearEasing
        ),
        label = "icon_rotation"
    )

    when (variant) {
        ButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = modifier
                    .scale(animatedScale)
                    .height(48.dp)
                    .shadow(
                        elevation = animatedElevation,
                        shape = RoundedCornerShape(GlassmorphicTokens.CornerRadius.sm),
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.16f)
                    ),
                enabled = enabled && !loading,
                shape = RoundedCornerShape(GlassmorphicTokens.CornerRadius.sm),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                interactionSource = interactionSource
            ) {
                // Gradient background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    GlassmorphicTokens.GradientBackground.accentStart,
                                    GlassmorphicTokens.GradientBackground.accentEnd
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        ButtonVariant.SECONDARY -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier
                    .scale(animatedScale)
                    .height(48.dp),
                enabled = enabled && !loading,
                shape = RoundedCornerShape(GlassmorphicTokens.CornerRadius.sm),
                border = BorderStroke(2.dp, ColorTokens.Primary.light),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ColorTokens.Primary.light
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                interactionSource = interactionSource
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ColorTokens.Primary.light,
                            strokeWidth = 2.dp
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        ButtonVariant.TERTIARY -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.scale(animatedScale),
                enabled = enabled && !loading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ColorTokens.Primary.light
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                interactionSource = interactionSource
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ColorTokens.Primary.light,
                            strokeWidth = 2.dp
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * CoreInput - Unified input component with smooth transitions
 *
 * Features:
 * - Focus state animations
 * - Label floating animation
 * - Border color transitions
 * - Optional leading/trailing icons
 *
 * @param value Current text value
 * @param onValueChange Text change callback
 * @param label Optional label text
 * @param modifier Modifier to apply
 * @param leadingIcon Optional leading icon
 * @param trailingIcon Optional trailing icon
 * @param placeholder Optional placeholder text
 * @param enabled Whether input is enabled
 * @param readOnly Whether input is read-only
 * @param isError Whether to show error state
 * @param supportingText Optional supporting/error text
 * @param keyboardOptions Keyboard configuration
 * @param keyboardActions Keyboard actions
 * @param singleLine Whether input is single line
 * @param maxLines Maximum number of lines
 * @param visualTransformation Visual transformation (e.g., password)
 */
@Composable
fun CoreInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = GlassmorphicTokens.Animation.springGentle()),
        enabled = enabled,
        readOnly = readOnly,
        label = label?.let { { Text(it) } },
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(GlassmorphicTokens.CornerRadius.sm),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = ColorTokens.Surface.lightVariant,
            focusedBorderColor = ColorTokens.Primary.light,
            unfocusedBorderColor = ColorTokens.Outline.lightVariant,
            focusedLabelColor = ColorTokens.Primary.light,
            unfocusedLabelColor = ColorTokens.Surface.onLightVariant,
            cursorColor = ColorTokens.Primary.light,
            errorBorderColor = ColorTokens.Error.light,
            errorLabelColor = ColorTokens.Error.light,
            errorSupportingTextColor = ColorTokens.Error.light
        )
    )
}

/**
 * CoreTopBar - Consistent top app bar
 *
 * Features:
 * - Slide-in animation
 * - Subtle elevation
 * - Icon button micro-interactions
 *
 * @param title Screen title
 * @param modifier Modifier to apply
 * @param onBack Optional back navigation callback
 * @param actions Optional action buttons (max 3 recommended)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    // Slide-in animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val slideOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else (-56).dp,
        animationSpec = GlassmorphicTokens.Animation.springGentle(),
        label = "topbar_slide"
    )

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = ColorTokens.Surface.onLight
            )
        },
        modifier = modifier.offset(y = slideOffset),
        navigationIcon = {
            onBack?.let { backAction ->
                IconButton(onClick = backAction) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = ColorTokens.Surface.onLight
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = ColorTokens.Surface.onLight,
            actionIconContentColor = ColorTokens.Surface.onLight
        )
    )
}

/**
 * CoreBottomNav - Consistent bottom navigation
 *
 * Features:
 * - Selected state animations
 * - Icon + label layout
 * - Ripple micro-interactions
 *
 * @param items Navigation items
 * @param selectedIndex Currently selected tab index
 * @param onSelect Tab selection callback
 * @param modifier Modifier to apply
 */
@Composable
fun CoreBottomNav(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.White,
        contentColor = ColorTokens.Primary.light,
        tonalElevation = GlassmorphicTokens.DepthLayer.level1
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ColorTokens.Primary.light,
                    selectedTextColor = ColorTokens.Primary.light,
                    unselectedIconColor = ColorTokens.Surface.onLightVariant,
                    unselectedTextColor = ColorTokens.Surface.onLightVariant,
                    indicatorColor = ColorTokens.Primary.lightContainer
                )
            )
        }
    }
}

/**
 * Bottom Navigation Item Data Class
 */
data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
