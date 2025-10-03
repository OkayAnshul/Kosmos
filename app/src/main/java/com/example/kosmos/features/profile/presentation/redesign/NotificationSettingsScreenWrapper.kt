package com.example.kosmos.features.profile.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.profile.presentation.NotificationSettingsViewModel

/**
 * Wrapper for NotificationSettingsScreen with Hilt dependency injection
 *
 * Responsibilities:
 * - Inject NotificationSettingsViewModel via Hilt
 * - Collect UI state from ViewModel
 * - Delegate user actions to ViewModel
 * - Handle navigation
 */
@Composable
fun NotificationSettingsScreenWrapper(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationSettingsScreen(
        allNotificationsEnabled = uiState.allNotificationsEnabled,
        messageNotifications = uiState.messageNotifications,
        taskNotifications = uiState.taskNotifications,
        projectUpdateNotifications = uiState.projectUpdateNotifications,
        mentionNotifications = uiState.mentionNotifications,
        mentionsOnlyMode = uiState.mentionsOnlyMode,
        soundEnabled = uiState.soundEnabled,
        vibrationEnabled = uiState.vibrationEnabled,
        dndEnabled = uiState.dndEnabled,
        dndStartHour = uiState.dndStartHour,
        dndStartMinute = uiState.dndStartMinute,
        dndEndHour = uiState.dndEndHour,
        dndEndMinute = uiState.dndEndMinute,
        onToggleAllNotifications = viewModel::toggleAllNotifications,
        onToggleMessageNotifications = viewModel::toggleMessageNotifications,
        onToggleTaskNotifications = viewModel::toggleTaskNotifications,
        onToggleProjectUpdateNotifications = viewModel::toggleProjectUpdateNotifications,
        onToggleMentionNotifications = viewModel::toggleMentionNotifications,
        onToggleMentionsOnlyMode = viewModel::toggleMentionsOnlyMode,
        onToggleSound = viewModel::toggleSound,
        onToggleVibration = viewModel::toggleVibration,
        onToggleDoNotDisturb = viewModel::toggleDoNotDisturb,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
