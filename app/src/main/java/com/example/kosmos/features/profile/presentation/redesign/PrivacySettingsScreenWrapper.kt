package com.example.kosmos.features.profile.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.profile.presentation.PrivacySettingsViewModel

/**
 * Wrapper for PrivacySettingsScreen with Hilt dependency injection
 *
 * Responsibilities:
 * - Inject PrivacySettingsViewModel via Hilt
 * - Collect UI state from ViewModel
 * - Delegate user actions to ViewModel
 * - Handle navigation
 */
@Composable
fun PrivacySettingsScreenWrapper(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PrivacySettingsScreen(
        profileVisibility = uiState.profileVisibility,
        showEmail = uiState.showEmail,
        showLastSeen = uiState.showLastSeen,
        showOnlineStatus = uiState.showOnlineStatus,
        allowDirectMessages = uiState.allowDirectMessages,
        allowMentions = uiState.allowMentions,
        isDownloadingData = uiState.isDownloadingData,
        blockedUsers = uiState.blockedUsers,
        onUpdateProfileVisibility = viewModel::updateProfileVisibility,
        onToggleShowEmail = viewModel::toggleShowEmail,
        onToggleShowLastSeen = viewModel::toggleShowLastSeen,
        onToggleShowOnlineStatus = viewModel::toggleShowOnlineStatus,
        onToggleAllowDirectMessages = viewModel::toggleAllowDirectMessages,
        onToggleAllowMentions = viewModel::toggleAllowMentions,
        onRequestDataDownload = viewModel::requestDataDownload,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
