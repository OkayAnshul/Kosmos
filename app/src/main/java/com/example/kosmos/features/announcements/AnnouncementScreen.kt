package com.example.kosmos.features.announcements

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

@Composable
fun AnnouncementScreen(
    announcement: Announcement,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = ColorTokens.ReactTheme.card,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
                ) {
                    // Type icon in a coloured circle
                    val (icon, iconColor) = announcementIconAndColor(announcement.type)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(iconColor.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Title
                    Text(
                        text = announcement.title,
                        style = TypographyTokens.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ColorTokens.ReactTheme.foreground
                    )

                    // Body
                    Text(
                        text = announcement.body,
                        style = TypographyTokens.typography.bodyMedium,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )

                    Spacer(modifier = Modifier.height(Tokens.Spacing.xs))

                    // Optional CTA button
                    if (!announcement.ctaUrl.isNullOrBlank() && !announcement.ctaLabel.isNullOrBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(announcement.ctaUrl))
                                runCatching { context.startActivity(intent) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorTokens.ReactTheme.primary
                            )
                        ) {
                            Text(announcement.ctaLabel)
                        }
                    }

                    // Dismiss button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp
                        )
                    ) {
                        Text(
                            text = "Got it",
                            color = ColorTokens.ReactTheme.foreground
                        )
                    }
                }
            }
        }
    }
}

private fun announcementIconAndColor(type: String): Pair<ImageVector, Color> = when (type) {
    "warning" -> Pair(Icons.Filled.Warning, Color(0xFFF59E0B))
    "feature" -> Pair(Icons.Filled.Star,    Color(0xFF7C3AED))
    else      -> Pair(Icons.Filled.Info,    Color(0xFF3B82F6))   // "info" and anything else
}
