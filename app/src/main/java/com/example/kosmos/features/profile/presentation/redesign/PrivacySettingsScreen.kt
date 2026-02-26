package com.example.kosmos.features.profile.presentation.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.*

/**
 * Stitch Design PrivacySettingsScreen
 *
 * Features:
 * - Navy background matching Stitch design
 * - Profile visibility selector (Public/Friends/Private)
 * - Toggle switches for visibility preferences
 * - Communication preferences
 * - Data management (download data)
 * - Blocked users section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    profileVisibility: String,
    showEmail: Boolean,
    showLastSeen: Boolean,
    showOnlineStatus: Boolean,
    allowDirectMessages: Boolean,
    allowMentions: Boolean,
    isDownloadingData: Boolean,
    blockedUsers: List<String>,
    onUpdateProfileVisibility: (String) -> Unit,
    onToggleShowEmail: (Boolean) -> Unit,
    onToggleShowLastSeen: (Boolean) -> Unit,
    onToggleShowOnlineStatus: (Boolean) -> Unit,
    onToggleAllowDirectMessages: (Boolean) -> Unit,
    onToggleAllowMentions: (Boolean) -> Unit,
    onRequestDataDownload: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = { Text("Privacy Settings", color = ColorTokens.ReactTheme.foreground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = ColorTokens.ReactTheme.foreground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorTokens.ReactTheme.card
                )
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Profile Visibility Section
                PrivacySectionHeader(
                    icon = Icons.Default.Visibility,
                    title = "PROFILE VISIBILITY"
                )

                // Visibility Options
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    VisibilityOption(
                        title = "Public",
                        description = "Anyone can see your profile",
                        selected = profileVisibility == "PUBLIC",
                        onClick = { onUpdateProfileVisibility("PUBLIC") }
                    )
                    VisibilityOption(
                        title = "Friends Only",
                        description = "Only people in your projects can see your profile",
                        selected = profileVisibility == "FRIENDS_ONLY",
                        onClick = { onUpdateProfileVisibility("FRIENDS_ONLY") }
                    )
                    VisibilityOption(
                        title = "Private",
                        description = "Only you can see your profile",
                        selected = profileVisibility == "PRIVATE",
                        onClick = { onUpdateProfileVisibility("PRIVATE") }
                    )
                }

                // What Others Can See Section
                PrivacySectionHeader(
                    icon = Icons.Default.RemoveRedEye,
                    title = "WHAT OTHERS CAN SEE"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ColorTokens.ReactTheme.card
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PrivacyToggle(
                            label = "Show Email Address",
                            description = "Let others see your email on your profile",
                            checked = showEmail,
                            onCheckedChange = onToggleShowEmail
                        )

                        Divider(
                            color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        PrivacyToggle(
                            label = "Show Last Seen",
                            description = "Let others see when you were last active",
                            checked = showLastSeen,
                            onCheckedChange = onToggleShowLastSeen
                        )

                        Divider(
                            color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        PrivacyToggle(
                            label = "Show Online Status",
                            description = "Show a green dot when you're online",
                            checked = showOnlineStatus,
                            onCheckedChange = onToggleShowOnlineStatus
                        )
                    }
                }

                // Communication Preferences Section
                PrivacySectionHeader(
                    icon = Icons.Default.Chat,
                    title = "COMMUNICATION PREFERENCES"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ColorTokens.ReactTheme.card
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PrivacyToggle(
                            label = "Allow Direct Messages",
                            description = "Let others send you direct messages",
                            checked = allowDirectMessages,
                            onCheckedChange = onToggleAllowDirectMessages
                        )

                        Divider(
                            color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        PrivacyToggle(
                            label = "Allow Mentions",
                            description = "Let others mention you in messages",
                            checked = allowMentions,
                            onCheckedChange = onToggleAllowMentions
                        )
                    }
                }

                // Data Management Section
                PrivacySectionHeader(
                    icon = Icons.Default.Storage,
                    title = "DATA MANAGEMENT"
                )

                Button(
                    onClick = onRequestDataDownload,
                    enabled = !isDownloadingData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorTokens.ReactTheme.card,
                        contentColor = ColorTokens.ReactTheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isDownloadingData) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = ColorTokens.ReactTheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Preparing Download...")
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Download My Data", fontWeight = FontWeight.Medium)
                    }
                }

                Text(
                    text = "Request a download of all your data including messages, tasks, and profile information.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.8f)
                )

                // Blocked Users Section
                PrivacySectionHeader(
                    icon = Icons.Default.Block,
                    title = "BLOCKED USERS"
                )

                if (blockedUsers.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ColorTokens.ReactTheme.card
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = "",
                                modifier = Modifier.size(48.dp),
                                tint = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "No blocked users",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorTokens.ReactTheme.mutedForeground,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "You haven't blocked anyone yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PrivacySectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = "",
            tint = ColorTokens.ReactTheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ColorTokens.ReactTheme.mutedForeground
        )
    }
}

@Composable
private fun VisibilityOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
            } else {
                ColorTokens.ReactTheme.card
            }
        ),
        border = if (selected) {
            BorderStroke(2.dp, ColorTokens.ReactTheme.primary)
        } else {
            BorderStroke(1.dp, ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.2f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) {
                        ColorTokens.ReactTheme.primary
                    } else {
                        ColorTokens.ReactTheme.foreground
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = ColorTokens.ReactTheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PrivacyToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = ColorTokens.ReactTheme.foreground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ColorTokens.ReactTheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f)
            )
        )
    }
}
