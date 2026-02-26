package com.example.kosmos.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.DoNotDisturbSettings
import com.example.kosmos.core.models.NotificationSettings
import com.example.kosmos.core.models.UserSettings
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * ViewModel for Notification Settings Screen
 * Manages user notification preferences and settings
 * Now persists to Supabase database instead of SharedPreferences
 */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    private val currentUserId: String?
        get() = authRepository.getCurrentUser()?.id

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Wait for AuthRepository's async init to complete if needed
            val userId = currentUserId ?: run {
                val user = authRepository.userFlow.filterNotNull().first()
                user.id
            }

            _uiState.update { it.copy(isLoading = true) }

            val result = userRepository.getUserSettings(userId)

            if (result.isSuccess) {
                val settings = result.getOrNull() ?: UserSettings()
                val notif = settings.notifications

                _uiState.update {
                    it.copy(
                        allNotificationsEnabled = notif.enabled,
                        messageNotifications = notif.messages,
                        taskNotifications = notif.tasks,
                        projectUpdateNotifications = notif.projectUpdates,
                        mentionNotifications = notif.mentions,
                        mentionsOnlyMode = notif.mentionsOnlyMode,
                        soundEnabled = notif.sound,
                        vibrationEnabled = notif.vibration,
                        dndEnabled = notif.dnd.enabled,
                        dndStartHour = notif.dnd.startHour,
                        dndStartMinute = notif.dnd.startMinute,
                        dndEndHour = notif.dnd.endHour,
                        dndEndMinute = notif.dnd.endMinute,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    private suspend fun saveSettings() {
        val userId = currentUserId ?: return

        // P1-03 FIX: Add try/catch for error handling
        try {
            // Get current settings from database to preserve privacy settings
            val currentSettings = userRepository.getUserSettings(userId).getOrNull() ?: UserSettings()

            // Get current UI state
            val state = _uiState.value

            // Create updated settings object (preserve privacy, update notifications)
            val updatedSettings = currentSettings.copy(
                notifications = NotificationSettings(
                    enabled = state.allNotificationsEnabled,
                    messages = state.messageNotifications,
                    tasks = state.taskNotifications,
                    projectUpdates = state.projectUpdateNotifications,
                    mentions = state.mentionNotifications,
                    mentionsOnlyMode = state.mentionsOnlyMode,
                    sound = state.soundEnabled,
                    vibration = state.vibrationEnabled,
                    dnd = DoNotDisturbSettings(
                        enabled = state.dndEnabled,
                        startHour = state.dndStartHour,
                        startMinute = state.dndStartMinute,
                        endHour = state.dndEndHour,
                        endMinute = state.dndEndMinute
                    )
                )
            )

            // Save to database
            val result = userRepository.updateUserSettings(userId, updatedSettings)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(error = "Failed to save settings: ${result.exceptionOrNull()?.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _uiState.update {
                it.copy(error = "Failed to save notification settings: ${e.message}")
            }
        }
    }

    fun toggleAllNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(allNotificationsEnabled = enabled) }
            saveSettings()
        }
    }

    fun toggleMessageNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(messageNotifications = enabled) }
            saveSettings()
        }
    }

    fun toggleTaskNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(taskNotifications = enabled) }
            saveSettings()
        }
    }

    fun toggleProjectUpdateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(projectUpdateNotifications = enabled) }
            saveSettings()
        }
    }

    fun toggleMentionNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(mentionNotifications = enabled) }
            saveSettings()
        }
    }

    fun toggleMentionsOnlyMode(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(mentionsOnlyMode = enabled) }
            saveSettings()
        }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(soundEnabled = enabled) }
            saveSettings()
        }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(vibrationEnabled = enabled) }
            saveSettings()
        }
    }

    fun toggleDoNotDisturb(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(dndEnabled = enabled) }
            saveSettings()
        }
    }

    fun updateDndStartTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    dndStartHour = hour,
                    dndStartMinute = minute
                )
            }
            saveSettings()
        }
    }

    fun updateDndEndTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    dndEndHour = hour,
                    dndEndMinute = minute
                )
            }
            saveSettings()
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
