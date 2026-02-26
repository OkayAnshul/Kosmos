package com.example.kosmos.features.connections.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.ConnectionStatus
import com.example.kosmos.core.models.ProjectInvite
import com.example.kosmos.core.models.User
import com.example.kosmos.core.models.UserConnection
import com.example.kosmos.core.feedback.UserFeedbackManager
import com.example.kosmos.core.feedback.safeCall
import com.example.kosmos.data.realtime.ConnectionEvent
import com.example.kosmos.data.realtime.SupabaseRealtimeManager
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.NotificationRepository
import com.example.kosmos.data.repository.ProjectInviteRepository
import com.example.kosmos.data.repository.UserConnectionRepository
import com.example.kosmos.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val userConnectionRepository: UserConnectionRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val realtimeManager: SupabaseRealtimeManager,
    private val projectInviteRepository: ProjectInviteRepository,
    private val notificationRepository: NotificationRepository,
    private val feedbackManager: UserFeedbackManager
) : ViewModel() {

    companion object {
        private const val TAG = "ConnectionsViewModel"
    }

    private val _uiState = MutableStateFlow(ConnectionsUiState())
    val uiState: StateFlow<ConnectionsUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            currentUserId = authRepository.getCurrentUser()?.id
            currentUserId?.let { userId ->
                // Sync from Supabase first so Room has fresh data
                safeCall(feedbackManager, tag = TAG, action = "sync connections") {
                    userConnectionRepository.syncFromSupabase(userId)
                }
                safeCall(feedbackManager, tag = TAG, action = "sync pending invites") {
                    projectInviteRepository.syncPendingForUser(userId)
                }
                loadAll(userId)

                // Subscribe to real-time connection events
                realtimeManager.subscribeToUserConnections(userId)

                // Re-sync when real-time events arrive (Room flows will auto-update UI)
                viewModelScope.launch {
                    realtimeManager.connectionEvents.collect { event ->
                        Log.d(TAG, "Real-time connection event: $event")
                        // Room dao is already updated by RealtimeManager,
                        // and loadAll() collects Room flows, so UI updates automatically
                    }
                }
            }
        }
    }

    private fun loadAll(userId: String) {
        loadAcceptedConnections(userId)
        loadPendingRequests(userId)
        loadPendingInvites(userId)
    }

    private fun loadPendingInvites(userId: String) {
        viewModelScope.launch {
            projectInviteRepository.getPendingForUserFlow(userId).collect { invites ->
                _uiState.update { it.copy(pendingInvites = invites) }
            }
        }
    }

    private fun loadAcceptedConnections(userId: String) {
        viewModelScope.launch {
            userConnectionRepository.getAcceptedConnectionsFlow(userId).collect { connections ->
                val usersWithConnections = connections.mapNotNull { conn ->
                    val otherId = if (conn.requesterId == userId) conn.addresseeId else conn.requesterId
                    val user = userRepository.getUserById(otherId)
                        ?: userRepository.getUserByIdFromSupabase(otherId)
                            .getOrNull()
                            ?.also { userRepository.cacheUser(it) }
                    if (user != null) ConnectionWithUser(conn, user) else null
                }
                _uiState.update { it.copy(acceptedConnections = usersWithConnections, isLoading = false) }
            }
        }
    }

    private fun loadPendingRequests(userId: String) {
        viewModelScope.launch {
            userConnectionRepository.getPendingRequestsFlow(userId).collect { connections ->
                val usersWithConnections = connections.map { conn ->
                    // Try local cache first, then fetch from Supabase if missing
                    val user = userRepository.getUserById(conn.requesterId)
                        ?: userRepository.getUserByIdFromSupabase(conn.requesterId)
                            .getOrNull()
                            ?.also { userRepository.cacheUser(it) }
                        ?: User(id = conn.requesterId, displayName = "Loading...")
                    ConnectionWithUser(conn, user)
                }
                _uiState.update { it.copy(pendingRequests = usersWithConnections) }
            }
        }
    }

    fun acceptConnection(connectionId: String) {
        viewModelScope.launch {
            val result = userConnectionRepository.acceptConnection(connectionId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to accept connection") }
                Log.e(TAG, "Failed to accept connection", result.exceptionOrNull())
            } else {
                // Delete the notification so it doesn't reappear on the Notifications tab
                val myId = currentUserId
                if (myId != null) {
                    safeCall(feedbackManager, tag = TAG, action = "delete connection notification") {
                        notificationRepository.deleteNotificationByDataField(myId, "connection_id", connectionId)
                    }
                }
            }
        }
    }

    fun declineConnection(connectionId: String) {
        viewModelScope.launch {
            val result = userConnectionRepository.declineConnection(connectionId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to decline connection") }
                Log.e(TAG, "Failed to decline connection", result.exceptionOrNull())
            } else {
                // Delete the notification so it doesn't reappear on the Notifications tab
                val myId = currentUserId
                if (myId != null) {
                    safeCall(feedbackManager, tag = TAG, action = "delete connection notification") {
                        notificationRepository.deleteNotificationByDataField(myId, "connection_id", connectionId)
                    }
                }
            }
        }
    }

    fun removeConnection(connectionId: String) {
        viewModelScope.launch {
            val result = userConnectionRepository.removeConnection(connectionId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to remove connection") }
                Log.e(TAG, "Failed to remove connection", result.exceptionOrNull())
            }
        }
    }

    fun acceptInvite(inviteId: String) {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            val result = projectInviteRepository.acceptInvite(inviteId, userId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to accept invite") }
                Log.e(TAG, "Failed to accept invite", result.exceptionOrNull())
            } else {
                safeCall(feedbackManager, tag = TAG, action = "delete invite notification") {
                    notificationRepository.deleteNotificationByDataField(userId, "invite_id", inviteId)
                }
            }
        }
    }

    fun declineInvite(inviteId: String) {
        viewModelScope.launch {
            val result = projectInviteRepository.declineInvite(inviteId)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to decline invite") }
                Log.e(TAG, "Failed to decline invite", result.exceptionOrNull())
            } else {
                val myId = currentUserId
                if (myId != null) {
                    safeCall(feedbackManager, tag = TAG, action = "delete invite notification") {
                        notificationRepository.deleteNotificationByDataField(myId, "invite_id", inviteId)
                    }
                }
            }
        }
    }

    fun refresh() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                safeCall(feedbackManager, tag = TAG, action = "sync connections on refresh") {
                    userConnectionRepository.syncFromSupabase(userId)
                }
                loadAll(userId)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class ConnectionWithUser(
    val connection: UserConnection,
    val user: User
)

data class ConnectionsUiState(
    val acceptedConnections: List<ConnectionWithUser> = emptyList(),
    val pendingRequests: List<ConnectionWithUser> = emptyList(),
    val pendingInvites: List<ProjectInvite> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val selectedTab: Int get() = 0
}
