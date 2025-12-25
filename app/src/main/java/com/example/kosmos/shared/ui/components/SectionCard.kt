package com.example.kosmos.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Section Card
 *
 * Standard card wrapper with optional section title header.
 * Used in creation wizards, edit screens, and detail screens.
 *
 * Design:
 * - Background: #18181D (card)
 * - Border: 1px solid #2A2A32
 * - Border radius: 12dp
 * - Section title: labelMedium, uppercase, muted color
 * - Inner padding: 16dp
 */
@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
    ) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.padding(horizontal = Tokens.Spacing.xs)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = ColorTokens.ReactTheme.card
            ),
            border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
                content = content
            )
        }
    }
}
