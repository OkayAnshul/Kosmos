package com.example.kosmos.features.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.feedback.UserFeedbackManager
import com.example.kosmos.core.feedback.safeCall
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.NotificationRepository
import com.example.kosmos.data.repository.ProjectInviteRepository
import com.example.kosmos.data.repository.UserConnectionRepository
import com.example.kosmos.data.repository.ProjectJoinRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Notification List Screen
 *
 * Manages notification list state, loading, marking as read, and deletion.
 */
@HiltViewModel
class NotificationListViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository,
    private val projectInviteRepository: ProjectInviteRepository,
    private val userConnectionRepository: UserConnectionRepository,
    private val projectJoinRequestRepository: ProjectJoinRequestRepository,
    private val feedbackManager: UserFeedbackManager
) : ViewModel() {

    private val TAG = "NotificationListViewModel"

    private val _uiState = MutableStateFlow(NotificationListUiState())
    val uiState: StateFlow<NotificationListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Sync connections + invites from Supabase so Accept/Decline can find records in Room
            val userId = authRepository.getCurrentUser()?.id
            if (userId != null) {
                safeCall(feedbackManager, tag = "NotificationListViewModel", action = "sync connections") {
                    userConnectionRepository.syncFromSupabase(userId)
                }
                safeCall(feedbackManager, tag = "NotificationListViewModel", action = "sync pending invites") {
                    projectInviteRepository.syncPendingForUser(userId)
                }
                safeCall(feedbackManager, tag = "NotificationListViewModel", action = "sync join requests") {
                    projectJoinRequestRepository.syncForUser(userId)
                }
            }
            loadNotifications()
        }
    }

    /**
     * Load notifications for current user
     */
    fun loadNotifications() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                Log.w(TAG, "Cannot load notifications: No user logged in")
                _uiState.update { it.copy(error = "Not logged in") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = notificationRepository.getNotifications(
                userId = currentUser.id,
                limit = 100
            )

            result.fold(
                onSuccess = { notifications ->
                    _uiState.update {
                        it.copy(
                            notifications = notifications,
                            isLoading = false,
                            error = null
                        )
                    }
                    Log.d(TAG, "✅ Loaded ${notifications.size} notifications")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load notifications"
                        )
                    }
                    Log.e(TAG, "❌ Failed to load notifications", error)
                }
            )
        }
    }

    /**
     * Mark notification as read
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            val result = notificationRepository.markAsRead(notificationId)

            result.fold(
                onSuccess = {
                    // Update local state
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { notification ->
                                if (notification.id == notificationId) {
                                    notification.copy(isRead = true)
                                } else {
                                    notification
                                }
                            }
                        )
                    }
                    Log.d(TAG, "✅ Marked notification as read: $notificationId")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to mark notification as read", error)
                    _uiState.update { it.copy(error = "Failed to mark as read") }
                }
            )
        }
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                Log.w(TAG, "Cannot mark all as read: No user logged in")
                return@launch
            }

            val result = notificationRepository.markAllAsRead(currentUser.id)

            result.fold(
                onSuccess = {
                    // Update local state
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map { it.copy(isRead = true) }
                        )
                    }
                    Log.d(TAG, "✅ Marked all notifications as read")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to mark all as read", error)
                    _uiState.update { it.copy(error = "Failed to mark all as read") }
                }
            )
        }
    }

    /**
     * Delete a notification
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            val result = notificationRepository.deleteNotification(notificationId)

            result.fold(
                onSuccess = {
                    // Remove from local state
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.filter { it.id != notificationId }
                        )
                    }
                    Log.d(TAG, "✅ Deleted notification: $notificationId")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to delete notification", error)
                    _uiState.update { it.copy(error = "Failed to delete notification") }
                }
            )
        }
    }

    /**
     * Clear all read notifications
     */
    fun clearAllRead() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                Log.w(TAG, "Cannot clear notifications: No user logged in")
                return@launch
            }

            val result = notificationRepository.deleteAllRead(currentUser.id)

            result.fold(
                onSuccess = {
                    // Remove read notifications from local state
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.filter { !it.isRead }
                        )
                    }
                    Log.d(TAG, "✅ Cleared all read notifications")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to clear read notifications", error)
                    _uiState.update { it.copy(error = "Failed to clear notifications") }
                }
            )
        }
    }

    /**
     * Clear all notifications
     */
    fun clearAll() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                Log.w(TAG, "Cannot clear all: No user logged in")
                return@launch
            }

            val result = notificationRepository.deleteAll(currentUser.id)

            result.fold(
                onSuccess = {
                    // Clear local state
                    _uiState.update { state ->
                        state.copy(notifications = emptyList())
                    }
                    Log.d(TAG, "✅ Cleared all notifications")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to clear all notifications", error)
                    _uiState.update { it.copy(error = "Failed to clear all") }
                }
            )
        }
    }

    /**
     * Refresh notifications
     */
    fun refresh() {
        loadNotifications()
    }

    // --- Invite / Connection / Join Request actions ---

    fun acceptInvite(inviteId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.id ?: return@launch
            val result = projectInviteRepository.acceptInvite(inviteId, userId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to accept invite: ${result.exceptionOrNull()?.message}") }
            } else {
                // Delete from Supabase so it doesn't reappear on next load
                val notif = _uiState.value.notifications.find { it.data["invite_id"] == inviteId }
                if (notif != null) {
                    safeCall(feedbackManager, tag = "NotificationListViewModel", action = "delete invite notification") {
                        notificationRepository.deleteNotification(notif.id!!)
                    }
                }
                _uiState.update { state ->
                    state.copy(
                        notifications = state.notifications.filter { it.data["invite_id"] != inviteId },
                        successMessage = "You've joined the project!"
                    )
                }
            }
        }
    }

    fun declineInvite(inviteId: String) {
        viewModelScope.launch {
            val result = projectInviteRepository.declineInvite(inviteId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to decline invite") }
            } else {
                loadNotifications()
            }
        }
    }

    fun acceptConnection(connectionId: String) {
        viewModelScope.launch {
            val result = userConnectionRepository.acceptConnection(connectionId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to accept connection") }
            } else {
                // Delete from Supabase so it doesn't reappear on next load
                val notif = _uiState.value.notifications.find { it.data["connection_id"] == connectionId }
                if (notif != null) {
                    safeCall(feedbackManager, tag = "NotificationListViewModel", action = "delete connection notification") {
                        notificationRepository.deleteNotification(notif.id!!)
                    }
                }
                _uiState.update { state ->
                    state.copy(
                        notifications = state.notifications.filter { it.data["connection_id"] != connectionId },
                        successMessage = "Connection accepted!"
                    )
                }
            }
        }
    }

    fun declineConnection(connectionId: String) {
        viewModelScope.launch {
            val result = userConnectionRepository.declineConnection(connectionId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to decline connection") }
            } else {
                loadNotifications()
            }
        }
    }

    fun approveJoinRequest(requestId: String) {
        viewModelScope.launch {
            val reviewerId = authRepository.getCurrentUser()?.id ?: return@launch
            val result = projectJoinRequestRepository.approveRequest(requestId, reviewerId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to approve request: ${result.exceptionOrNull()?.message}") }
            } else {
                loadNotifications()
            }
        }
    }

    fun rejectJoinRequest(requestId: String) {
        viewModelScope.launch {
            val reviewerId = authRepository.getCurrentUser()?.id ?: return@launch
            val result = projectJoinRequestRepository.rejectRequest(requestId, reviewerId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to reject request") }
            } else {
                loadNotifications()
            }
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

/**
 * UI State for Notification List Screen
 */
data class NotificationListUiState(
    val notifications: List<SupabaseNotification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
) {
    val unreadCount: Int
        get() = notifications.count { !it.isRead }

    val hasNotifications: Boolean
        get() = notifications.isNotEmpty()
}
