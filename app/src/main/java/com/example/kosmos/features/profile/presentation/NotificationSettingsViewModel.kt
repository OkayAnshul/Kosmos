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
 * ViewModel for Notification Settings Screen
 * Manages user notification preferences and settings
 */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "notification_settings",
        Context.MODE_PRIVATE
    )

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                allNotificationsEnabled = prefs.getBoolean("all_notifications", true),
                messageNotifications = prefs.getBoolean("message_notifications", true),
                taskNotifications = prefs.getBoolean("task_notifications", true),
                projectUpdateNotifications = prefs.getBoolean("project_updates", true),
                mentionNotifications = prefs.getBoolean("mention_notifications", true),
                mentionsOnlyMode = prefs.getBoolean("mentions_only_mode", false),
                soundEnabled = prefs.getBoolean("sound_enabled", true),
                vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
                dndEnabled = prefs.getBoolean("dnd_enabled", false),
                dndStartHour = prefs.getInt("dnd_start_hour", 22),
                dndStartMinute = prefs.getInt("dnd_start_minute", 0),
                dndEndHour = prefs.getInt("dnd_end_hour", 8),
                dndEndMinute = prefs.getInt("dnd_end_minute", 0),
                isLoading = false
            )
        }
    }

    fun toggleAllNotifications(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("all_notifications", enabled).apply()
            _uiState.update { it.copy(allNotificationsEnabled = enabled) }
        }
    }

    fun toggleMessageNotifications(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("message_notifications", enabled).apply()
            _uiState.update { it.copy(messageNotifications = enabled) }
        }
    }

    fun toggleTaskNotifications(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("task_notifications", enabled).apply()
            _uiState.update { it.copy(taskNotifications = enabled) }
        }
    }

    fun toggleProjectUpdateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("project_updates", enabled).apply()
            _uiState.update { it.copy(projectUpdateNotifications = enabled) }
        }
    }

    fun toggleMentionNotifications(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("mention_notifications", enabled).apply()
            _uiState.update { it.copy(mentionNotifications = enabled) }
        }
    }

    fun toggleMentionsOnlyMode(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("mentions_only_mode", enabled).apply()
            _uiState.update { it.copy(mentionsOnlyMode = enabled) }
        }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("sound_enabled", enabled).apply()
            _uiState.update { it.copy(soundEnabled = enabled) }
        }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("vibration_enabled", enabled).apply()
            _uiState.update { it.copy(vibrationEnabled = enabled) }
        }
    }

    fun toggleDoNotDisturb(enabled: Boolean) {
        viewModelScope.launch {
            prefs.edit().putBoolean("dnd_enabled", enabled).apply()
            _uiState.update { it.copy(dndEnabled = enabled) }
        }
    }

    fun updateDndStartTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefs.edit()
                .putInt("dnd_start_hour", hour)
                .putInt("dnd_start_minute", minute)
                .apply()
            _uiState.update {
                it.copy(
                    dndStartHour = hour,
                    dndStartMinute = minute
                )
            }
        }
    }

    fun updateDndEndTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefs.edit()
                .putInt("dnd_end_hour", hour)
                .putInt("dnd_end_minute", minute)
                .apply()
            _uiState.update {
                it.copy(
                    dndEndHour = hour,
                    dndEndMinute = minute
                )
            }
        }
    }
}

/**
 * UI state for Notification Settings screen
 */
data class NotificationSettingsUiState(
    val allNotificationsEnabled: Boolean = true,
    val messageNotifications: Boolean = true,
    val taskNotifications: Boolean = true,
    val projectUpdateNotifications: Boolean = true,
    val mentionNotifications: Boolean = true,
    val mentionsOnlyMode: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val dndEnabled: Boolean = false,
    val dndStartHour: Int = 22,
    val dndStartMinute: Int = 0,
    val dndEndHour: Int = 8,
    val dndEndMinute: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)
