package com.example.kosmos.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Kosmos Card Component
 *
 * Replicates the React ProjectCard.tsx design:
 * - Background: --card (#18181D)
 * - Border: 1px solid --border (#2A2A32)
 * - Border radius: rounded-xl (12dp)
 * - Shadow: 0 2px 8px rgba(0,0,0,0.3)
 * - Padding: p-4 (16dp)
 * - Hover: border-primary/20, shadow-lg
 *
 * Design Reference: documents/Kosmos/src/app/components/ProjectCard.tsx line 25-28
 */
@Composable
fun KosmosCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = ColorTokens.ReactTheme.card  // --card: #18181D
    )
    val cardBorder = BorderStroke(
        width = 1.dp,
        color = ColorTokens.ReactTheme.border  // --border: #2A2A32
    )
    val cardShape = androidx.compose.foundation.shape.RoundedCornerShape(
        Tokens.CornerRadius.md  // rounded-xl: 12dp
    )
    val cardElevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp  // No shadow - clean AMOLED look
    )
    val cardContent: @Composable () -> Unit = {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(Tokens.Spacing.md)  // p-4: 16dp
        ) {
            content()
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            colors = cardColors,
            border = cardBorder,
            shape = cardShape,
            elevation = cardElevation,
            content = { cardContent() }
        )
    } else {
        Card(
            modifier = modifier,
            colors = cardColors,
            border = cardBorder,
            shape = cardShape,
            elevation = cardElevation,
            content = { cardContent() }
        )
    }
}
