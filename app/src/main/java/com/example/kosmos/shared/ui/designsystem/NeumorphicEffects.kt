package com.example.kosmos.shared.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neumorphic design effects for Kosmos project creation wizard
 *
 * Neumorphism is a design trend that combines:
 * - Soft shadows (both light and dark)
 * - Subtle gradients
 * - Border highlights
 * - Depth perception through layering
 *
 * Usage: Apply these modifiers to cards, buttons, and containers
 * for a modern, tactile UI feel while maintaining Material 3 foundation
 */
object NeumorphicEffects {

    /**
     * Default neumorphic card modifier - Enhanced for Midnight Plum
     * Creates soft shadow effect with subtle border
     * Shadow alphas increased for better depth perception
     *
     * @param cornerRadius Corner radius for the card (default: 16dp)
     * @param elevation Shadow elevation (default: 4dp)
     * @return Modifier with neumorphic styling
     */
    fun cardModifier(
        cornerRadius: Dp = 16.dp,
        elevation: Dp = 4.dp
    ): Modifier {
        return Modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.22f),  // Increased from 0.08f
                spotColor = Color.Black.copy(alpha = 0.28f)      // Increased from 0.12f
            )
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                RoundedCornerShape(cornerRadius)
            )
    }

    /**
     * Neumorphic surface with gradient background
     * Creates a subtle gradient for depth perception
     *
     * @param isLightMode Whether in light mode (default: true)
     * @return Modifier with gradient background
     */
    fun Modifier.neumorphicSurface(isLightMode: Boolean = true): Modifier {
        val gradient = if (isLightMode) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF5F5F5), // Slightly lighter at top
                    Color(0xFFFFFFFF)  // White at bottom
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF2C2C2C), // Slightly lighter at top
                    Color(0xFF1E1E1E)  // Darker at bottom
                )
            )
        }

        return this.background(brush = gradient)
    }

    /**
     * Pressed/depressed neumorphic effect - Enhanced
     * Creates inset shadow for button press states
     * Deeper shadows for better tactile feedback
     *
     * @param cornerRadius Corner radius (default: 12dp)
     * @return Modifier with inset effect
     */
    fun pressedModifier(cornerRadius: Dp = 12.dp): Modifier {
        return Modifier
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.30f),  // Increased from 0.15f
                spotColor = Color.Black.copy(alpha = 0.38f)      // Increased from 0.20f
            )
            .border(
                BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
                RoundedCornerShape(cornerRadius)
            )
    }

    /**
     * Elevated neumorphic effect - Enhanced
     * For floating action buttons or important cards
     * Stronger shadows for prominent elevation
     *
     * @param cornerRadius Corner radius (default: 16dp)
     * @param elevation Shadow elevation (default: 8dp)
     * @return Modifier with elevated styling
     */
    fun elevatedModifier(
        cornerRadius: Dp = 16.dp,
        elevation: Dp = 8.dp
    ): Modifier {
        return Modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.26f),  // Increased from 0.12f
                spotColor = Color.Black.copy(alpha = 0.34f)      // Increased from 0.18f
            )
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                RoundedCornerShape(cornerRadius)
            )
    }

    /**
     * Flat neumorphic effect - Enhanced
     * Minimal shadow for subtle depth
     * Slightly increased for better visibility
     *
     * @param cornerRadius Corner radius (default: 12dp)
     * @return Modifier with flat styling
     */
    fun flatModifier(cornerRadius: Dp = 12.dp): Modifier {
        return Modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.16f),  // Increased from 0.04f
                spotColor = Color.Black.copy(alpha = 0.20f)      // Increased from 0.06f
            )
            .border(
                BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                RoundedCornerShape(cornerRadius)
            )
    }

    /**
     * Wizard step card modifier
     * Special styling for wizard step cards
     *
     * @param isActive Whether this step is active
     * @param cornerRadius Corner radius (default: 20.dp)
     * @return Modifier with step card styling
     */
    fun wizardStepCard(
        isActive: Boolean,
        cornerRadius: Dp = 20.dp
    ): Modifier {
        return if (isActive) {
            elevatedModifier(cornerRadius, elevation = 6.dp)
        } else {
            flatModifier(cornerRadius)
        }
    }

    /**
     * Input field neumorphic modifier
     * For text fields and input components
     *
     * @param cornerRadius Corner radius (default: 12.dp)
     * @return Modifier with input field styling
     */
    fun inputFieldModifier(cornerRadius: Dp = 12.dp): Modifier {
        return Modifier
            .shadow(
                elevation = 0.dp, // No external shadow
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
                RoundedCornerShape(cornerRadius)
            )
    }

    /**
     * Create a vertical gradient brush
     * Utility for custom gradient backgrounds
     *
     * @param topColor Color at top
     * @param bottomColor Color at bottom
     * @return Gradient brush
     */
    fun verticalGradient(
        topColor: Color,
        bottomColor: Color
    ): Brush {
        return Brush.verticalGradient(
            colors = listOf(topColor, bottomColor)
        )
    }

    /**
     * Create a subtle highlight gradient
     * For top borders and highlights
     *
     * @param baseColor Base color to highlight
     * @param intensity Highlight intensity (0f - 1f, default: 0.2f)
     * @return Gradient brush
     */
    fun highlightGradient(
        baseColor: Color,
        intensity: Float = 0.2f
    ): Brush {
        val highlightColor = baseColor.copy(
            alpha = baseColor.alpha,
            red = (baseColor.red + intensity).coerceAtMost(1f),
            green = (baseColor.green + intensity).coerceAtMost(1f),
            blue = (baseColor.blue + intensity).coerceAtMost(1f)
        )

        return Brush.verticalGradient(
            colors = listOf(highlightColor, baseColor)
        )
    }
}
