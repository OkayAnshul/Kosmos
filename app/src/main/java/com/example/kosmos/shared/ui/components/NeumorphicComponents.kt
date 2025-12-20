package com.example.kosmos.shared.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Gradients
import com.example.kosmos.shared.ui.designsystem.NeumorphicEffects

/**
 * Neumorphic Components Library
 *
 * Reusable Material 3 components with neumorphic styling following the Midnight Plum design system.
 * All components use enhanced shadows and purple-tinted surfaces for depth perception.
 *
 * Design Principles:
 * - Strategic accent usage (not full interface coverage)
 * - Accessibility first (WCAG AA compliance)
 * - Interactive feedback with animations
 * - Consistent with Material Design 3
 *
 * @see NeumorphicEffects for shadow modifiers
 * @see ColorTokens for Midnight Plum color palette
 * @see Gradients.MidnightPlum for gradient definitions
 */

/**
 * Neumorphic Card
 *
 * A Material 3 Card with enhanced neumorphic shadows and optional gradient background.
 * Supports press states with animated elevation changes.
 *
 * Features:
 * - Enhanced shadows for depth perception
 * - Optional gradient background
 * - Animated press states
 * - Customizable corner radius and elevation
 *
 * @param modifier Modifier to be applied to the card
 * @param onClick Optional click handler - enables press animation
 * @param enabled Whether the card is enabled for interaction
 * @param shape Shape of the card (default: 16dp rounded corners)
 * @param elevation Base elevation in dp (animates on press)
 * @param useGradient Whether to apply gradient background
 * @param gradient Custom gradient (defaults to MidnightPlum.cardBackground)
 * @param content Content to be displayed inside the card
 */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 4.dp,
    useGradient: Boolean = false,
    gradient: Brush = Gradients.MidnightPlum.cardBackground,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate elevation on press (4dp -> 2dp)
    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) elevation / 2 else elevation,
        animationSpec = spring(),
        label = "card_elevation"
    )

    // Animate scale on press (1.0 -> 0.98)
    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(),
        label = "card_scale"
    )

    Card(
        onClick = onClick ?: {},
        modifier = modifier
            .scale(animatedScale)
            .shadow(
                elevation = animatedElevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (isPressed) 0.30f else 0.22f),
                spotColor = Color.Black.copy(alpha = if (isPressed) 0.38f else 0.28f)
            )
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                shape
            ),
        enabled = enabled,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (useGradient) Color.Transparent else ColorTokens.Surface.light
        ),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = if (useGradient) {
                Modifier.background(gradient)
            } else {
                Modifier
            }
        ) {
            content()
        }
    }
}

/**
 * Neumorphic Button
 *
 * A Material 3 Button with neumorphic styling and animated press states.
 * Uses Midnight Plum primary colors with enhanced shadows.
 *
 * Features:
 * - Animated press feedback (elevation + scale)
 * - Enhanced neumorphic shadows
 * - Optional gradient background
 * - Full Material 3 Button API support
 *
 * @param onClick Click handler
 * @param modifier Modifier to be applied to the button
 * @param enabled Whether the button is enabled
 * @param shape Shape of the button (default: 12dp rounded corners)
 * @param elevation Base elevation in dp
 * @param useGradient Whether to apply primary gradient background
 * @param contentPadding Padding around button content
 * @param content Button content (typically Text)
 */
@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    elevation: Dp = 4.dp,
    useGradient: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate elevation on press (4dp -> 1dp for pressed effect)
    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else elevation,
        animationSpec = spring(),
        label = "button_elevation"
    )

    // Animate scale on press (1.0 -> 0.98)
    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(),
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .scale(animatedScale)
            .shadow(
                elevation = animatedElevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (isPressed) 0.30f else 0.22f),
                spotColor = Color.Black.copy(alpha = if (isPressed) 0.38f else 0.28f)
            )
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = if (isPressed) 0.1f else 0.3f)),
                shape
            ),
        enabled = enabled,
        shape = shape,
        colors = if (useGradient) {
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = ColorTokens.Primary.light,
                contentColor = Color.White
            )
        },
        contentPadding = contentPadding,
        interactionSource = interactionSource
    ) {
        if (useGradient) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gradients.MidnightPlum.primary),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

/**
 * Neumorphic TextField
 *
 * An OutlinedTextField with neumorphic styling optimized for input.
 * Uses subtle shadows and purple-tinted surfaces.
 *
 * Features:
 * - Neumorphic input field styling
 * - Focus state animations
 * - Midnight Plum color scheme
 * - Full OutlinedTextField API support
 *
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param modifier Modifier to be applied to the text field
 * @param enabled Whether the text field is enabled
 * @param readOnly Whether the text field is read-only
 * @param textStyle Style for input text
 * @param label Optional label composable
 * @param placeholder Optional placeholder composable
 * @param leadingIcon Optional leading icon
 * @param trailingIcon Optional trailing icon
 * @param supportingText Optional supporting text below field
 * @param isError Whether to show error state
 * @param visualTransformation Visual transformation for text (e.g., password)
 * @param keyboardOptions Keyboard configuration options
 * @param keyboardActions Keyboard action handlers
 * @param singleLine Whether text field is single line
 * @param maxLines Maximum number of lines
 * @param minLines Minimum number of lines
 * @param shape Shape of the text field
 */
@Composable
fun NeumorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.20f)
            ),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
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
        minLines = minLines,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ColorTokens.Surface.light,
            unfocusedContainerColor = ColorTokens.Surface.light,
            disabledContainerColor = ColorTokens.Surface.lightVariant,
            focusedBorderColor = ColorTokens.Primary.light,
            unfocusedBorderColor = ColorTokens.Outline.light.copy(alpha = 0.5f),
            focusedLabelColor = ColorTokens.Primary.light,
            unfocusedLabelColor = ColorTokens.Surface.onLightVariant,
            cursorColor = ColorTokens.Primary.light
        )
    )
}

/**
 * Neumorphic Search Bar
 *
 * A specialized neumorphic text field optimized for search functionality.
 * Features a search icon and rounded pill shape.
 *
 * Features:
 * - Pill-shaped design (28dp radius)
 * - Built-in search icon
 * - Neumorphic shadows
 * - Optional trailing icon (e.g., clear button)
 *
 * @param query Current search query
 * @param onQueryChange Callback when query changes
 * @param modifier Modifier to be applied to the search bar
 * @param placeholder Placeholder text when empty
 * @param enabled Whether the search bar is enabled
 * @param trailingIcon Optional trailing icon (e.g., clear/filter button)
 * @param onSearch Callback when search is submitted (e.g., keyboard done)
 */
@Composable
fun NeumorphicSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    onSearch: ((String) -> Unit)? = null
) {
    NeumorphicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorTokens.Surface.onLightVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = ColorTokens.Primary.light
            )
        },
        trailingIcon = trailingIcon,
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch?.invoke(query) }
        )
    )
}

/**
 * Animated Elevation Helper
 *
 * Provides animated elevation state for custom neumorphic components.
 * Useful when building custom interactive elements.
 *
 * @param baseElevation Base elevation when not pressed
 * @param pressedElevation Elevation when pressed
 * @param isPressed Current pressed state
 * @param animationSpec Animation specification for elevation changes
 * @return Animated elevation value
 */
@Composable
fun animatedNeumorphicElevation(
    baseElevation: Dp = 4.dp,
    pressedElevation: Dp = 1.dp,
    isPressed: Boolean,
    animationSpec: AnimationSpec<Dp> = spring()
): Dp {
    return animateDpAsState(
        targetValue = if (isPressed) pressedElevation else baseElevation,
        animationSpec = animationSpec,
        label = "neumorphic_elevation"
    ).value
}

/**
 * Animated Scale Helper
 *
 * Provides animated scale state for custom neumorphic components.
 * Creates subtle press feedback effect.
 *
 * @param baseScale Base scale when not pressed (typically 1.0f)
 * @param pressedScale Scale when pressed (typically 0.98f)
 * @param isPressed Current pressed state
 * @param animationSpec Animation specification for scale changes
 * @return Animated scale value
 */
@Composable
fun animatedNeumorphicScale(
    baseScale: Float = 1f,
    pressedScale: Float = 0.98f,
    isPressed: Boolean,
    animationSpec: AnimationSpec<Float> = spring()
): Float {
    return androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) pressedScale else baseScale,
        animationSpec = animationSpec,
        label = "neumorphic_scale"
    ).value
}

/**
 * Neumorphic Surface
 *
 * A simple Surface with neumorphic styling.
 * Good for creating custom neumorphic layouts.
 *
 * @param modifier Modifier to be applied to the surface
 * @param shape Shape of the surface
 * @param elevation Elevation in dp
 * @param color Background color
 * @param contentColor Content color for text/icons
 * @param border Optional border
 * @param content Content to be displayed
 */
@Composable
fun NeumorphicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 4.dp,
    color: Color = ColorTokens.Surface.light,
    contentColor: Color = ColorTokens.Surface.onLight,
    border: BorderStroke? = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.28f)
            )
            .then(
                if (border != null) {
                    Modifier.border(border, shape)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = color,
        contentColor = contentColor
    ) {
        content()
    }
}
