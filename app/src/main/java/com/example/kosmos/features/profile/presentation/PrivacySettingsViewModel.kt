package com.example.kosmos.features.profile.presentation

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Privacy Settings Screen
 * Manages user privacy preferences and settings
 */
@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "privacy_settings",
        Context.MODE_PRIVATE
    )

    private val _uiState = MutableStateFlow(PrivacySettingsUiState())
    val uiState: StateFlow<PrivacySettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                profileVisibility = prefs.getString("profile_visibility", "PUBLIC") ?: "PUBLIC",
                showEmail = prefs.getBoolean("show_email", true),
                showLastSeen = prefs.getBoolean("show_last_seen", true),
                showOnlineStatus = prefs.getBoolean("show_online_status", true),
                allowDirectMessages = prefs.getBoolean("allow_direct_messages", true),
                allowMentions = prefs.getBoolean("allow_mentions", true),
                blockedUsers = emptyList(), // TODO: Load from database
                isLoading = false
            )
        }
    }

    fun updateProfileVisibility(visibility: String) {
        viewModelScope.launch {
            prefs.edit().putString("profile_visibility", visibility).apply()
            _uiState.update { it.copy(profileVisibility = visibility) }
        }
    }

    fun toggleShowEmail(show: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("show_email", show).apply()
            _uiState.update { it.copy(showEmail = show) }
        }
    }

    fun toggleShowLastSeen(show: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("show_last_seen", show).apply()
            _uiState.update { it.copy(showLastSeen = show) }
        }
    }

    fun toggleShowOnlineStatus(show: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("show_online_status", show).apply()
            _uiState.update { it.copy(showOnlineStatus = show) }
        }
    }

    fun toggleAllowDirectMessages(allow: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("allow_direct_messages", allow).apply()
            _uiState.update { it.copy(allowDirectMessages = allow) }
        }
    }

    fun toggleAllowMentions(allow: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("allow_mentions", allow).apply()
            _uiState.update { it.copy(allowMentions = allow) }
        }
    }

    fun requestDataDownload() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingData = true) }
            // TODO: Implement data export functionality
            // This would export user data to a downloadable file
            kotlinx.coroutines.delay(2000) // Simulate download
            _uiState.update { it.copy(isDownloadingData = false, dataDownloadSuccess = true) }
        }
    }

    fun clearDataDownloadStatus() {
        _uiState.update { it.copy(dataDownloadSuccess = false) }
    }
}

/**
 * UI state for Privacy Settings screen
 */
data class PrivacySettingsUiState(
    val profileVisibility: String = "PUBLIC", // PUBLIC, FRIENDS_ONLY, PRIVATE
    val showEmail: Boolean = true,
    val showLastSeen: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val allowDirectMessages: Boolean = true,
    val allowMentions: Boolean = true,
    val blockedUsers: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isDownloadingData: Boolean = false,
    val dataDownloadSuccess: Boolean = false,
    val error: String? = null
)
