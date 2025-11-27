package com.example.kosmos.features.users.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.User
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ProjectInviteRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.UserConnectionRepository
import com.example.kosmos.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

@HiltViewModel
class InviteMembersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val projectInviteRepository: ProjectInviteRepository,
    private val userConnectionRepository: UserConnectionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow(InviteMembersUiState())
    val uiState: StateFlow<InviteMembersUiState> = _uiState.asStateFlow()

    private val _selectedUsers = MutableStateFlow<List<User>>(emptyList())
    val selectedUsers: StateFlow<List<User>> = _selectedUsers.asStateFlow()

    private val _selectedRole = MutableStateFlow(ProjectRole.MEMBER)
    val selectedRole: StateFlow<ProjectRole> = _selectedRole.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0 = Connections, 1 = Search
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _connections = MutableStateFlow<List<User>>(emptyList())
    val connections: StateFlow<List<User>> = _connections.asStateFlow()

    private var currentProjectId: String? = null

    init {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 2) {
                    searchUsers(query)
                } else if (query.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(users = emptyList(), searchHint = "Type at least 2 characters to search")
                } else {
                    _uiState.value = _uiState.value.copy(users = emptyList(), searchHint = null)
                }
            }
            .launchIn(viewModelScope)
    }

    fun setProjectId(projectId: String) {
        currentProjectId = projectId
        loadExistingMembers()
        loadPendingInvites()
        loadConnections()
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.value = _uiState.value.copy(users = emptyList())
    }

    fun setRole(role: ProjectRole) {
        _selectedRole.value = role
    }

    fun toggleUserSelection(user: User) {
        val currentSelection = _selectedUsers.value
        _selectedUsers.value = if (user in currentSelection) {
            currentSelection - user
        } else {
            currentSelection + user
        }
    }

    fun clearSelection() {
        _selectedUsers.value = emptyList()
    }

    private fun loadExistingMembers() {
        val projectId = currentProjectId ?: return
        viewModelScope.launch {
            try {
                projectRepository.getProjectMembersFlow(projectId).collect { members ->
                    _uiState.value = _uiState.value.copy(
                        existingMemberIds = members.map { it.userId }.toSet()
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("InviteMembersVM", "Failed to load existing members: ${e.message}")
            }
        }
    }

    private fun loadPendingInvites() {
        val projectId = currentProjectId ?: return
        viewModelScope.launch {
            try {
                projectInviteRepository.getProjectInvitesFlow(projectId).collect { invites ->
                    val pendingIds = invites
                        .filter { it.status == com.example.kosmos.core.models.InviteStatus.PENDING }
                        .map { it.inviteeId }
                        .toSet()
                    _uiState.value = _uiState.value.copy(pendingInviteUserIds = pendingIds)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("InviteMembersVM", "Failed to load pending invites: ${e.message}")
            }
        }
    }

    private fun loadConnections() {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUser()?.id ?: return@launch
                userConnectionRepository.getAcceptedConnectionsFlow(currentUserId).collect { connectionsList ->
                    // Get user details for each connection
                    val connectedUserIds = connectionsList.map { conn ->
                        if (conn.requesterId == currentUserId) conn.addresseeId else conn.requesterId
                    }
                    val users = connectedUserIds.mapNotNull { userId ->
                        userRepository.getUserById(userId)
                    }
                    _connections.value = users
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("InviteMembersVM", "Failed to load connections: ${e.message}")
            }
        }
    }

    private fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null, searchHint = null)

                val result = userRepository.searchUsersFromSupabase(
                    query = query,
                    excludeIds = emptyList(),
                    limit = 50
                )

                if (result.isSuccess) {
                    val users = result.getOrNull() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        users = users,
                        isLoading = false
                    )
                } else {
                    val error = result.exceptionOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to search users: ${error?.message}"
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("InviteMembersVM", "Failed to search users: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to search users: ${e.message}"
                )
            }
        }
    }

    fun retrySearch() {
        if (_searchQuery.value.length >= 2) {
            searchUsers(_searchQuery.value)
        }
    }

    fun inviteMembers() {
        val projectId = currentProjectId
        if (projectId == null) {
            _uiState.value = _uiState.value.copy(error = "No project selected")
            return
        }

        val usersToInvite = _selectedUsers.value.filter { user ->
            user.id !in _uiState.value.existingMemberIds &&
            user.id !in _uiState.value.pendingInviteUserIds
        }

        if (usersToInvite.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No new members to invite")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isInviting = true,
                    error = null,
                    invitationSuccess = false
                )

                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isInviting = false,
                        error = "You must be logged in to invite members"
                    )
                    return@launch
                }

                // Get project name for notification
                val project = projectRepository.getProject(projectId)
                val projectName = project?.name ?: ""

                val role = _selectedRole.value
                var successCount = 0
                var failedCount = 0

                for (user in usersToInvite) {
                    val result = projectInviteRepository.sendInvite(
                        projectId = projectId,
                        inviteeId = user.id,
                        inviterId = currentUser.id,
                        role = role.name,
                        projectName = projectName,
                        inviterName = currentUser.displayName
                    )

                    if (result.isSuccess) {
                        successCount++
                        Log.d("InviteMembersVM", "Invite sent to ${user.displayName}")
                    } else {
                        failedCount++
                        Log.e("InviteMembersVM", "Failed to invite ${user.displayName}: ${result.exceptionOrNull()?.message}")
                    }
                }

                if (failedCount == 0) {
                    _uiState.value = _uiState.value.copy(
                        isInviting = false,
                        invitationSuccess = true
                    )
                    _selectedUsers.value = emptyList()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isInviting = false,
                        error = "Sent $successCount invite(s). $failedCount failed."
                    )
                }

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("InviteMembersVM", "Exception inviting members: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isInviting = false,
                    error = "Failed to send invites: ${e.message}"
                )
            }
        }
    }
}

data class InviteMembersUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val existingMemberIds: Set<String> = emptySet(),
    val pendingInviteUserIds: Set<String> = emptySet(),
    val isInviting: Boolean = false,
    val invitationSuccess: Boolean = false,
    val error: String? = null,
    val searchHint: String? = null
)
