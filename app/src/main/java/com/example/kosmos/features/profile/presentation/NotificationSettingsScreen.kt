package com.example.kosmos.features.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Notification Settings Screen
 * Allows users to manage their notification preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Master Toggle Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.allNotificationsEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
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
                        Text(
                            text = "All Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.allNotificationsEnabled) {
                                "You'll receive all notifications"
                            } else {
                                "All notifications are disabled"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.allNotificationsEnabled,
                        onCheckedChange = viewModel::toggleAllNotifications
                    )
                }
            }

            if (uiState.allNotificationsEnabled) {
                HorizontalDivider()

                // Notification Types Section
                NotificationSectionHeader(
                    icon = Icons.Default.Notifications,
                    title = "Notification Types"
                )

                NotificationToggle(
                    label = "Message Notifications",
                    description = "Get notified when you receive new messages",
                    icon = Icons.Default.Message,
                    checked = uiState.messageNotifications,
                    onCheckedChange = viewModel::toggleMessageNotifications,
                    enabled = uiState.allNotificationsEnabled
                )

                NotificationToggle(
                    label = "Task Notifications",
                    description = "Get notified about task assignments and updates",
                    icon = Icons.Default.Assignment,
                    checked = uiState.taskNotifications,
                    onCheckedChange = viewModel::toggleTaskNotifications,
                    enabled = uiState.allNotificationsEnabled
                )

                NotificationToggle(
                    label = "Project Updates",
                    description = "Get notified when projects are updated",
                    icon = Icons.Default.Folder,
                    checked = uiState.projectUpdateNotifications,
                    onCheckedChange = viewModel::toggleProjectUpdateNotifications,
                    enabled = uiState.allNotificationsEnabled
                )

                NotificationToggle(
                    label = "Mentions",
                    description = "Get notified when someone mentions you",
                    icon = Icons.Default.AlternateEmail,
                    checked = uiState.mentionNotifications,
                    onCheckedChange = viewModel::toggleMentionNotifications,
                    enabled = uiState.allNotificationsEnabled
                )

                HorizontalDivider()

                // Special Modes Section
                NotificationSectionHeader(
                    icon = Icons.Default.FilterAlt,
                    title = "Special Modes"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.mentionsOnlyMode) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
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
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Mentions Only Mode",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Only notify me when I'm mentioned or assigned",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.mentionsOnlyMode,
                            onCheckedChange = viewModel::toggleMentionsOnlyMode,
                            enabled = uiState.allNotificationsEnabled
                        )
                    }
                }

                HorizontalDivider()

                // Sound & Vibration Section
                NotificationSectionHeader(
                    icon = Icons.Default.VolumeUp,
                    title = "Sound & Vibration"
                )

                NotificationToggle(
                    label = "Sound",
                    description = "Play sound for notifications",
                    icon = Icons.Default.MusicNote,
                    checked = uiState.soundEnabled,
                    onCheckedChange = viewModel::toggleSound,
                    enabled = uiState.allNotificationsEnabled
                )

                NotificationToggle(
                    label = "Vibration",
                    description = "Vibrate for notifications",
                    icon = Icons.Default.Vibration,
                    checked = uiState.vibrationEnabled,
                    onCheckedChange = viewModel::toggleVibration,
                    enabled = uiState.allNotificationsEnabled
                )

                HorizontalDivider()

                // Do Not Disturb Section
                NotificationSectionHeader(
                    icon = Icons.Default.DoNotDisturb,
                    title = "Do Not Disturb"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.dndEnabled) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Silence notifications during specific hours",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.dndEnabled,
                                onCheckedChange = viewModel::toggleDoNotDisturb,
                                enabled = uiState.allNotificationsEnabled
                            )
                        }

                        if (uiState.dndEnabled) {
                            HorizontalDivider()

                            // DND Schedule
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Start Time
                                Column {
                                    Text(
                                        text = "Start Time",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { showStartTimePicker = true }
                                    ) {
                                        Icon(
                                            Icons.Default.AccessTime,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = String.format(
                                                "%02d:%02d",
                                                uiState.dndStartHour,
                                                uiState.dndStartMinute
                                            )
                                        )
                                    }
                                }

                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "to",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // End Time
                                Column {
                                    Text(
                                        text = "End Time",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { showEndTimePicker = true }
                                    ) {
                                        Icon(
                                            Icons.Default.AccessTime,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = String.format(
                                                "%02d:%02d",
                                                uiState.dndEndHour,
                                                uiState.dndEndMinute
                                            )
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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Notification settings apply to all your projects. You can also configure per-project notifications in project settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Time Pickers (simplified - would use actual TimePicker in production)
    // TODO: Implement proper time picker dialogs
    if (showStartTimePicker) {
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Set Start Time") },
            text = {
                Text("Time picker would be implemented here.\nCurrently: ${String.format("%02d:%02d", uiState.dndStartHour, uiState.dndStartMinute)}")
            },
            confirmButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showEndTimePicker) {
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Set End Time") },
            text = {
                Text("Time picker would be implemented here.\nCurrently: ${String.format("%02d:%02d", uiState.dndEndHour, uiState.dndEndMinute)}")
            },
            confirmButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("OK")
                }
            }
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
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
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (enabled && checked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.5f
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
