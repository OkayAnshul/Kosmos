package com.example.kosmos.features.profile.presentation.redesign

import androidx.compose.foundation.background
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
 * Stitch Design NotificationSettingsScreen
 *
 * Features:
 * - Navy background matching Stitch design
 * - Master toggle for all notifications
 * - Notification type toggles (messages, tasks, projects, mentions)
 * - Mentions-only mode
 * - Sound & vibration toggles
 * - Do Not Disturb with time schedule
 * - Info card at bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    allNotificationsEnabled: Boolean,
    messageNotifications: Boolean,
    taskNotifications: Boolean,
    projectUpdateNotifications: Boolean,
    mentionNotifications: Boolean,
    mentionsOnlyMode: Boolean,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    dndEnabled: Boolean,
    dndStartHour: Int,
    dndStartMinute: Int,
    dndEndHour: Int,
    dndEndMinute: Int,
    onToggleAllNotifications: (Boolean) -> Unit,
    onToggleMessageNotifications: (Boolean) -> Unit,
    onToggleTaskNotifications: (Boolean) -> Unit,
    onToggleProjectUpdateNotifications: (Boolean) -> Unit,
    onToggleMentionNotifications: (Boolean) -> Unit,
    onToggleMentionsOnlyMode: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleDoNotDisturb: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = { Text("Notification Settings", color = ColorTokens.ReactTheme.foreground) },
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
                // Master Toggle Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (allNotificationsEnabled) {
                            ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
                        } else {
                            ColorTokens.ReactTheme.card
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "All Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorTokens.ReactTheme.foreground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (allNotificationsEnabled) {
                                    "You'll receive all notifications"
                                } else {
                                    "All notifications are disabled"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                        Switch(
                            checked = allNotificationsEnabled,
                            onCheckedChange = onToggleAllNotifications,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ColorTokens.ReactTheme.primary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                if (allNotificationsEnabled) {
                    // Notification Types Section
                    NotificationSectionHeader(
                        icon = Icons.Default.Notifications,
                        title = "NOTIFICATION TYPES"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ColorTokens.ReactTheme.card
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            NotificationToggle(
                                label = "Message Notifications",
                                description = "Get notified when you receive new messages",
                                icon = Icons.Default.Message,
                                checked = messageNotifications,
                                onCheckedChange = onToggleMessageNotifications,
                                enabled = allNotificationsEnabled
                            )

                            Divider(
                                color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            NotificationToggle(
                                label = "Task Notifications",
                                description = "Get notified about task assignments and updates",
                                icon = Icons.Default.Assignment,
                                checked = taskNotifications,
                                onCheckedChange = onToggleTaskNotifications,
                                enabled = allNotificationsEnabled
                            )

                            Divider(
                                color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            NotificationToggle(
                                label = "Project Updates",
                                description = "Get notified when projects are updated",
                                icon = Icons.Default.Folder,
                                checked = projectUpdateNotifications,
                                onCheckedChange = onToggleProjectUpdateNotifications,
                                enabled = allNotificationsEnabled
                            )

                            Divider(
                                color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            NotificationToggle(
                                label = "Mentions",
                                description = "Get notified when someone mentions you",
                                icon = Icons.Default.AlternateEmail,
                                checked = mentionNotifications,
                                onCheckedChange = onToggleMentionNotifications,
                                enabled = allNotificationsEnabled
                            )
                        }
                    }

                    // Special Modes Section
                    NotificationSectionHeader(
                        icon = Icons.Default.FilterAlt,
                        title = "SPECIAL MODES"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (mentionsOnlyMode) {
                                ColorTokens.ReactTheme.primary.copy(alpha = 0.1f)
                            } else {
                                ColorTokens.ReactTheme.card
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PersonPin,
                                        contentDescription = "",
                                        modifier = Modifier.size(20.dp),
                                        tint = ColorTokens.ReactTheme.primary
                                    )
                                    Text(
                                        text = "Mentions Only Mode",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = ColorTokens.ReactTheme.foreground
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Only notify me when I'm mentioned or assigned",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorTokens.ReactTheme.mutedForeground
                                )
                            }
                            Switch(
                                checked = mentionsOnlyMode,
                                onCheckedChange = onToggleMentionsOnlyMode,
                                enabled = allNotificationsEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ColorTokens.ReactTheme.primary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    // Sound & Vibration Section
                    NotificationSectionHeader(
                        icon = Icons.Default.VolumeUp,
                        title = "SOUND & VIBRATION"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ColorTokens.ReactTheme.card
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            NotificationToggle(
                                label = "Sound",
                                description = "Play sound for notifications",
                                icon = Icons.Default.MusicNote,
                                checked = soundEnabled,
                                onCheckedChange = onToggleSound,
                                enabled = allNotificationsEnabled
                            )

                            Divider(
                                color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            NotificationToggle(
                                label = "Vibration",
                                description = "Vibrate for notifications",
                                icon = Icons.Default.Vibration,
                                checked = vibrationEnabled,
                                onCheckedChange = onToggleVibration,
                                enabled = allNotificationsEnabled
                            )
                        }
                    }

                    // Do Not Disturb Section
                    NotificationSectionHeader(
                        icon = Icons.Default.DoNotDisturb,
                        title = "DO NOT DISTURB"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (dndEnabled) {
                                ColorTokens.ReactTheme.primary.copy(alpha = 0.1f)
                            } else {
                                ColorTokens.ReactTheme.card
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enable Do Not Disturb",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = ColorTokens.ReactTheme.foreground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Silence notifications during specific hours",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ColorTokens.ReactTheme.mutedForeground
                                    )
                                }
                                Switch(
                                    checked = dndEnabled,
                                    onCheckedChange = onToggleDoNotDisturb,
                                    enabled = allNotificationsEnabled,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ColorTokens.ReactTheme.primary,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            if (dndEnabled) {
                                Divider(color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.1f))

                                // DND Schedule
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Start Time
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Start Time",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ColorTokens.ReactTheme.mutedForeground
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { showStartTimePicker = true },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = ColorTokens.ReactTheme.card,
                                                contentColor = ColorTokens.ReactTheme.foreground
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.AccessTime,
                                                contentDescription = "",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = String.format("%02d:%02d", dndStartHour, dndStartMinute),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = "to",
                                        modifier = Modifier.size(24.dp),
                                        tint = ColorTokens.ReactTheme.mutedForeground
                                    )

                                    // End Time
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "End Time",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ColorTokens.ReactTheme.mutedForeground
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { showEndTimePicker = true },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = ColorTokens.ReactTheme.card,
                                                contentColor = ColorTokens.ReactTheme.foreground
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.AccessTime,
                                                contentDescription = "",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = String.format("%02d:%02d", dndEndHour, dndEndMinute),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ColorTokens.ReactTheme.card.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "",
                            tint = ColorTokens.ReactTheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Notification settings apply to all your projects. You can also configure per-project notifications in project settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Time Pickers (simplified - TODO: Implement proper time picker dialogs)
    if (showStartTimePicker) {
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Set Start Time", color = ColorTokens.ReactTheme.foreground) },
            text = {
                Text(
                    "Time picker would be implemented here.\nCurrently: ${String.format("%02d:%02d", dndStartHour, dndStartMinute)}",
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            },
            confirmButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("OK", color = ColorTokens.ReactTheme.primary)
                }
            },
            containerColor = ColorTokens.ReactTheme.card
        )
    }

    if (showEndTimePicker) {
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Set End Time", color = ColorTokens.ReactTheme.foreground) },
            text = {
                Text(
                    "Time picker would be implemented here.\nCurrently: ${String.format("%02d:%02d", dndEndHour, dndEndMinute)}",
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            },
            confirmButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("OK", color = ColorTokens.ReactTheme.primary)
                }
            },
            containerColor = ColorTokens.ReactTheme.card
        )
    }
}

@Composable
private fun NotificationSectionHeader(
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
private fun NotificationToggle(
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = "",
                modifier = Modifier.size(20.dp),
                tint = if (enabled && checked) {
                    ColorTokens.ReactTheme.primary
                } else {
                    ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)
                }
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) {
                        ColorTokens.ReactTheme.foreground
                    } else {
                        ColorTokens.ReactTheme.foreground.copy(alpha = 0.5f)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground.copy(
                        alpha = if (enabled) 1f else 0.5f
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ColorTokens.ReactTheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f)
            )
        )
    }
}
