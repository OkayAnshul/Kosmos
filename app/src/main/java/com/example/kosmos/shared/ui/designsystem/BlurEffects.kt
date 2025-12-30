package com.example.kosmos.shared.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Blur Effects & Glassmorphism Utilities
 *
 * Provides modifier extensions for creating glassmorphism effects
 * used in Better Blur design system (auth screens).
 *
 * Note: Blur effects work best on Android 12+ (API 31+).
 * On older versions, effects will gracefully degrade to solid backgrounds.
 */

/**
 * Glassmorphism Card Effect
 *
 * Creates a frosted glass appearance with:
 * - Semi-transparent background
 * - Subtle blur effect
 * - Thin border
 * - Rounded corners
 *
 * @param cornerRadius Corner radius for rounded shape
 * @param backgroundColor Semi-transparent background color
 * @param borderColor Border color (usually white with low opacity)
 * @param borderWidth Border width
 * @param blurRadius Blur effect radius (requires API 31+)
 *
 * Example: Box(modifier = Modifier.glassmorphism())
 */
fun Modifier.glassmorphism(
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = Color(0x33FFFFFF), // 20% white
    borderColor: Color = Color(0x1AFFFFFF),     // 10% white
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 20.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        color = backgroundColor,
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = borderWidth,
        color = borderColor,
        shape = RoundedCornerShape(cornerRadius)
    )
    .blur(
        radius = blurRadius,
        edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded
    )

/**
 * Glassmorphism Input Field
 *
 * Optimized glassmorphism for input fields:
 * - Slightly more transparent
 * - Tighter blur
 * - Smaller corner radius
 *
 * Example: TextField(modifier = Modifier.glass morphismInput())
 */
fun Modifier.glassmorphismInput(
    cornerRadius: Dp = 12.dp,
    backgroundColor: Color = Color(0x1AFFFFFF), // 10% white
    borderColor: Color = Color(0x0DFFFFFF),     // 5% white
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        color = backgroundColor,
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = borderWidth,
        color = borderColor,
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Glassmorphism Button
 *
 * Glassmorphism effect for buttons with more opacity for better contrast
 *
 * Example: Button(modifier = Modifier.glassmorphismButton())
 */
fun Modifier.glassmorphismButton(
    cornerRadius: Dp = 12.dp,
    backgroundColor: Color = Color(0x4DFFFFFF), // 30% white
    borderColor: Color = Color(0x33FFFFFF),     // 20% white
    borderWidth: Dp = 1.5.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        color = backgroundColor,
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = borderWidth,
        color = borderColor,
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Frosted Overlay
 *
 * Creates a frosted glass overlay effect for modals/dialogs
 * with backdrop blur
 *
 * Example: Box(modifier = Modifier.frostedOverlay())
 */
fun Modifier.frostedOverlay(
    backgroundColor: Color = Color(0x80000000), // 50% black
    blurRadius: Dp = 25.dp
): Modifier = this
    .background(backgroundColor)
    .blur(
        radius = blurRadius,
        edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Rectangle
    )

/**
 * Subtle Blur Background
 *
 * Adds a subtle blur effect to any background
 * Useful for creating depth and layering
 *
 * Example: Image(modifier = Modifier.subtleBlur())
 */
fun Modifier.subtleBlur(
    blurRadius: Dp = 10.dp
): Modifier = this
    .blur(
        radius = blurRadius,
        edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded
    )
