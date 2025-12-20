package com.example.kosmos.shared.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.ColorTokens

/**
 * Offline mode banner for critical screens
 * Stitch design: Orange/yellow banner with sync icon
 *
 * Usage: Add ONLY to Chat, Tasks, and Projects screens
 */
@Composable
fun OfflineModeBanner(
    isOffline: Boolean,
    isSyncing: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = if (isSyncing) {
                ColorTokens.ReactTheme.primary
            } else {
                ColorTokens.Priority.medium
            },
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Icon(
                    imageVector = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudOff,
                    contentDescription = if (isSyncing) "Syncing" else "Offline",
                    tint = ColorTokens.ReactTheme.primaryForeground,
                    modifier = Modifier.size(20.dp)
                )

                // Text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSyncing) "Syncing Changes" else "Offline Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = ColorTokens.ReactTheme.primaryForeground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isSyncing) {
                            "Uploading cached data..."
                        } else {
                            "Changes saved locally"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTokens.ReactTheme.primaryForeground.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

/**
 * Minimal variant - single line banner
 */
@Composable
fun OfflineModeBannerCompact(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = ColorTokens.Priority.medium,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline",
                    tint = ColorTokens.ReactTheme.primaryForeground,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = "Offline Mode - Changes saved locally",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.primaryForeground,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
