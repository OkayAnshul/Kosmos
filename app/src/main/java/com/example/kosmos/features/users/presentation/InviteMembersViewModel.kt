package com.example.kosmos.features.users.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.User
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Invite Members Screen
 * Handles bulk member invitation with multi-select functionality
 */
@HiltViewModel
class InviteMembersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
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

    private var currentProjectId: String? = null

    init {
        // Search users as query changes
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 2) {
                    searchUsers(query)
                } else {
                    _uiState.value = _uiState.value.copy(users = emptyList())
                }
            }
            .launchIn(viewModelScope)
    }

    fun setProjectId(projectId: String) {
        currentProjectId = projectId
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

    /**
     * Load existing project members to prevent duplicate invitations
     */
    fun loadExistingMembers() {
        val projectId = currentProjectId ?: return

        viewModelScope.launch {
            try {
                projectRepository.getProjectMembersFlow(projectId).collect { members ->
                    _uiState.value = _uiState.value.copy(
                        existingMemberIds = members.map { it.userId }.toSet()
                    )
                }
            } catch (e: Exception) {
                Log.e("InviteMembersVM", "Failed to load existing members: ${e.message}")
            }
        }
    }

    /**
     * Search for users to invite
     * Fetches FRESH data from Supabase (no stale cache)
     */
    private fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Search directly from Supabase for fresh, real-time data
                val result = userRepository.searchUsersFromSupabase(
                    query = query,
                    excludeIds = emptyList(), // Don't exclude anyone, we mark existing members in UI
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

    /**
     * Invite all selected members to the project
     */
    fun inviteMembers() {
        val projectId = currentProjectId
        if (projectId == null) {
            _uiState.value = _uiState.value.copy(error = "No project selected")
            return
        }

        val usersToInvite = _selectedUsers.value.filter { user ->
            // Don't invite existing members
            user.id !in _uiState.value.existingMemberIds
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

                val currentUserId = authRepository.getCurrentUser()?.id
                if (currentUserId == null) {
                    _uiState.value = _uiState.value.copy(
                        isInviting = false,
                        error = "You must be logged in to invite members"
                    )
                    return@launch
                }

                val role = _selectedRole.value
                var successCount = 0
                var failedCount = 0

                // Invite each user
                for (user in usersToInvite) {
                    val result = projectRepository.addMember(
                        projectId = projectId,
                        userId = user.id,
                        role = role,
                        invitedBy = currentUserId
                    )

                    if (result.isSuccess) {
                        successCount++
                        Log.d("InviteMembersVM", "Successfully invited ${user.displayName}")
                    } else {
                        failedCount++
                        Log.e("InviteMembersVM", "Failed to invite ${user.displayName}: ${result.exceptionOrNull()?.message}")
                    }
                }

                // Show result
                if (failedCount == 0) {
                    // All successful
                    _uiState.value = _uiState.value.copy(
                        isInviting = false,
                        invitationSuccess = true
                    )
                    _selectedUsers.value = emptyList()
                } else {
                    // Some failed
                    _uiState.value = _uiState.value.copy(
                        isInviting = false,
                        error = "Invited $successCount member(s). $failedCount failed."
                    )
                }

            } catch (e: Exception) {
                Log.e("InviteMembersVM", "Exception inviting members: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isInviting = false,
                    error = "Failed to invite members: ${e.message}"
                )
            }
        }
    }
}

data class InviteMembersUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val existingMemberIds: Set<String> = emptySet(),
    val isInviting: Boolean = false,
    val invitationSuccess: Boolean = false,
    val error: String? = null
)
