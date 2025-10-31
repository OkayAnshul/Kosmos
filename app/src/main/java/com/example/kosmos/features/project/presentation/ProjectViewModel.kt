package com.example.kosmos.features.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.Project
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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for project management with RBAC
 * Handles project creation, member management, and permission checks
 */
@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    private val currentUser = authRepository.getCurrentUser()

    init {
        if (currentUser != null) {
            loadUserProjects()
        }
    }

    /**
     * Load all projects for the current user
     */
    private fun loadUserProjects() {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    projectRepository.getUserProjectsFlow(user.id).collect { projects ->
                        _uiState.value = _uiState.value.copy(
                            projects = projects,
                            isLoading = false
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load projects: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Create a new project
     * @param name Project name
     * @param description Project description
     */
    fun createProject(name: String, description: String) {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(isCreating = true)

                    val result = projectRepository.createProject(
                        name = name,
                        description = description,
                        ownerId = user.id
                    )

                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            isCreating = false,
                            successMessage = "Project created successfully"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isCreating = false,
                            error = "Failed to create project: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = "Error creating project: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Load members for a specific project
     * @param projectId Project ID
     */
    fun loadProjectMembers(projectId: String) {
        viewModelScope.launch {
            try {
                projectRepository.getProjectMembersFlow(projectId).collect { members ->
                    _uiState.value = _uiState.value.copy(
                        currentProjectMembers = members
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load members: ${e.message}"
                )
            }
        }
    }

    /**
     * Add a member to a project
     * @param projectId Project ID
     * @param userId User to add
     * @param role Role to assign
     */
    fun addMember(projectId: String, userId: String, role: ProjectRole) {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(isAddingMember = true)

                    val result = projectRepository.addMember(
                        projectId = projectId,
                        userId = userId,
                        role = role,
                        invitedBy = user.id
                    )

                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            isAddingMember = false,
                            successMessage = "Member added successfully"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isAddingMember = false,
                            error = "Failed to add member: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isAddingMember = false,
                        error = "Error adding member: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Remove a member from a project
     * @param projectId Project ID
     * @param userId User to remove
     */
    fun removeMember(projectId: String, userId: String) {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    val result = projectRepository.removeMember(
                        projectId = projectId,
                        userIdToRemove = userId,
                        removedBy = user.id
                    )

                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Member removed successfully"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to remove member: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Error removing member: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Change a member's role
     * @param projectId Project ID
     * @param userId User whose role to change
     * @param newRole New role
     */
    fun changeRole(projectId: String, userId: String, newRole: ProjectRole) {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    val result = projectRepository.changeRole(
                        projectId = projectId,
                        userIdToChange = userId,
                        newRole = newRole,
                        changedBy = user.id
                    )

                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Role changed successfully"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to change role: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Error changing role: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Get user by ID
     * Used to load user data for project members
     * @param userId User ID
     * @return User object or null if not found
     */
    suspend fun getUserById(userId: String): User? {
        return try {
            userRepository.getUserById(userId)
        } catch (e: Exception) {
            android.util.Log.e("ProjectViewModel", "Failed to load user: ${e.message}")
            null
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

/**
 * UI state for project management
 */
data class ProjectUiState(
    val projects: List<Project> = emptyList(),
    val currentProjectMembers: List<ProjectMember> = emptyList(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val isAddingMember: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
