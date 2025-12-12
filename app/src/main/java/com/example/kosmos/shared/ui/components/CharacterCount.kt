package com.example.kosmos.shared.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.kosmos.shared.ui.designsystem.ColorTokens

/**
 * Inline character count indicator for text fields.
 * Shown in muted color, turns orange at 85%, red when over limit.
 *
 * Usage: Place immediately below an OutlinedTextField, aligned to end.
 * Also enforce the limit in the onValueChange lambda:
 *   onValueChange = { if (it.length <= MAX) value = it }
 */
@Composable
fun CharacterCount(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier
) {
    val isOverLimit = current > max
    val isNearLimit = current > (max * 0.85).toInt()
    Text(
        text = "$current / $max",
        style = MaterialTheme.typography.labelSmall,
        color = when {
            isOverLimit -> Color(0xFFF44336)  // error red
            isNearLimit -> Color(0xFFFFA726)  // warning orange
            else -> ColorTokens.ReactTheme.mutedForeground
        },
        modifier = modifier
    )
}
