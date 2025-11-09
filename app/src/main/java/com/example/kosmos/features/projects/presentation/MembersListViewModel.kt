package com.example.kosmos.features.projects.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.User
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Members List Screen
 * Manages project member viewing and role management
 */
@HiltViewModel
class MembersListViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MembersListUiState())
    val uiState: StateFlow<MembersListUiState> = _uiState.asStateFlow()

    fun loadMembers(projectId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Get current user
                val currentUser = authRepository.getCurrentUser()

                // Sync members from Supabase first
                projectRepository.syncProjectMembers(projectId)

                // Load project members
                val members = projectRepository.getProjectMembers(projectId)

                // Find current user's role
                val currentUserRole = members.find { it.userId == currentUser?.id }?.role ?: ProjectRole.MEMBER

                // Load user details for each member
                val membersWithUsers = members.mapNotNull { member ->
                    val user = userRepository.getUserById(member.userId)
                    if (user != null) {
                        MemberWithUser(member, user)
                    } else null
                }

                _uiState.update {
                    it.copy(
                        members = membersWithUsers,
                        filteredMembers = membersWithUsers,
                        currentUserRole = currentUserRole,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error loading members: ${e.message}"
                    )
                }
            }
        }
    }

    fun filterByRole(role: ProjectRole?) {
        _uiState.update { state ->
            val filtered = if (role == null) {
                state.members
            } else {
                state.members.filter { it.member.role == role }
            }
            state.copy(
                selectedRoleFilter = role,
                filteredMembers = filtered
            )
        }
    }

    fun searchMembers(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                if (state.selectedRoleFilter != null) {
                    state.members.filter { it.member.role == state.selectedRoleFilter }
                } else {
                    state.members
                }
            } else {
                state.members.filter { memberWithUser ->
                    val user = memberWithUser.user
                    user.displayName.contains(query, ignoreCase = true) ||
                    user.username.contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true)
                }.let { results ->
                    if (state.selectedRoleFilter != null) {
                        results.filter { it.member.role == state.selectedRoleFilter }
                    } else {
                        results
                    }
                }
            }
            state.copy(
                searchQuery = query,
                filteredMembers = filtered
            )
        }
    }

    fun changeRole(projectId: String, memberId: String, newRole: ProjectRole) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUpdating = true, error = null) }

                // Get current user ID
                val currentUserId = authRepository.getCurrentUser()?.id
                    ?: return@launch _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Not authenticated"
                        )
                    }

                // Find the actual user ID from the member object
                val memberToChange = _uiState.value.members.find { it.member.id == memberId }
                val userIdToChange = memberToChange?.user?.id
                    ?: return@launch _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Member not found"
                        )
                    }

                val result = projectRepository.changeRole(projectId, userIdToChange, newRole, currentUserId)

                if (result.isSuccess) {
                    // Reload members to get updated data
                    loadMembers(projectId)
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            successMessage = "Role updated successfully"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Failed to update role: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Error updating role: ${e.message}"
                    )
                }
            }
        }
    }

    fun removeMember(projectId: String, memberId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUpdating = true, error = null) }

                // Get current user ID
                val currentUserId = authRepository.getCurrentUser()?.id
                    ?: return@launch _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Not authenticated"
                        )
                    }

                // Find the actual user ID from the member object
                val memberToRemove = _uiState.value.members.find { it.member.id == memberId }
                val userIdToRemove = memberToRemove?.user?.id
                    ?: return@launch _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Member not found"
                        )
                    }

                val result = projectRepository.removeMember(projectId, userIdToRemove, currentUserId)

                if (result.isSuccess) {
                    // Reload members to reflect removal
                    loadMembers(projectId)
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            successMessage = "Member removed successfully"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Failed to remove member: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Error removing member: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }
}

/**
 * Data class combining ProjectMember with User details
 */
data class MemberWithUser(
    val member: ProjectMember,
    val user: User
)

/**
 * UI state for Members List screen
 */
data class MembersListUiState(
    val members: List<MemberWithUser> = emptyList(),
    val filteredMembers: List<MemberWithUser> = emptyList(),
    val currentUserRole: ProjectRole = ProjectRole.MEMBER,
    val selectedRoleFilter: ProjectRole? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)
