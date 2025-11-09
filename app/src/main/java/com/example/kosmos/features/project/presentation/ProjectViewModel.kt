package com.example.kosmos.features.project.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.ProjectStats
import com.example.kosmos.core.models.ProjectStatus
import com.example.kosmos.core.models.User
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    // Track active stat observation jobs to prevent duplicates
    private val statsJobs = mutableMapOf<String, Job>()

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

    /**
     * Get a project by ID from current loaded projects
     * @param projectId Project ID
     * @return Project or null if not found
     */
    fun getProjectById(projectId: String): Project? {
        return _uiState.value.projects.find { it.id == projectId }
    }

    /**
     * Archive a project (change status to ARCHIVED)
     * Requires ARCHIVE_PROJECT permission
     * @param projectId Project ID to archive
     */
    fun archiveProject(projectId: String) {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    val result = projectRepository.updateProjectStatus(
                        projectId = projectId,
                        status = ProjectStatus.ARCHIVED,
                        userId = user.id
                    )

                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Project archived successfully"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to archive project: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Error archiving project: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Unarchive a project (change status to ACTIVE)
     * Requires ARCHIVE_PROJECT permission
     * @param projectId Project ID to unarchive
     */
    fun unarchiveProject(projectId: String) {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    val result = projectRepository.updateProjectStatus(
                        projectId = projectId,
                        status = ProjectStatus.ACTIVE,
                        userId = user.id
                    )

                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            successMessage = "Project restored successfully"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to restore project: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Error restoring project: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Update project details (name, description, and status)
     * Requires EDIT_PROJECT permission
     * @param projectId Project ID
     * @param name New project name
     * @param description New project description
     * @param status New project status (optional)
     */
    fun updateProjectDetails(
        projectId: String,
        name: String,
        description: String,
        status: com.example.kosmos.core.models.ProjectStatus? = null
    ) {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(isUpdating = true)

                    val project = _uiState.value.projects.find { it.id == projectId }
                        ?: return@launch

                    val updatedProject = project.copy(
                        name = name,
                        description = description,
                        status = status ?: project.status
                    )

                    val result = projectRepository.updateProject(updatedProject, user.id)

                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            successMessage = "Project updated successfully"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = "Failed to update project: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        error = "Error updating project: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Observe statistics for all user projects in real-time
     * Uses Flow to automatically update when any project data changes
     */
    fun loadAllProjectStats() {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(isLoadingStats = true)

                    // Observe all projects and update stats when they change
                    projectRepository.getUserProjectsFlow(user.id).collect { projects ->
                        val statsMap = mutableMapOf<String, ProjectStats>()

                        // For each project, get its stats
                        projects.forEach { project ->
                            val stats = projectRepository.getProjectStats(project.id)
                            statsMap[project.id] = stats
                        }

                        _uiState.value = _uiState.value.copy(
                            projectStats = statsMap,
                            isLoadingStats = false
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingStats = false,
                        error = "Failed to load project stats: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Observe statistics for a specific project in real-time
     * Uses Flow to automatically update when project data changes
     * Prevents duplicate observations by cancelling existing job if present
     * @param projectId Project ID
     */
    fun loadProjectStats(projectId: String) {
        // Cancel existing job for this project if any
        statsJobs[projectId]?.cancel()

        // Create new observation job
        val job = viewModelScope.launch {
            try {
                // Use Flow for real-time updates instead of one-time query
                projectRepository.getProjectStatsFlow(projectId).collect { stats ->
                    val updatedStats = _uiState.value.projectStats.toMutableMap()
                    updatedStats[projectId] = stats

                    _uiState.value = _uiState.value.copy(
                        projectStats = updatedStats
                    )
                }
            } catch (e: Exception) {
                // Only log if it's not a cancellation
                if (e !is kotlinx.coroutines.CancellationException) {
                    android.util.Log.e("ProjectViewModel", "Failed to observe stats for project $projectId", e)
                }
            }
        }

        // Store the job
        statsJobs[projectId] = job
    }

    /**
     * Get stats for a specific project from current state
     * @param projectId Project ID
     * @return ProjectStats or null if not loaded
     */
    fun getProjectStats(projectId: String): ProjectStats? {
        return _uiState.value.projectStats[projectId]
    }
}

/**
 * UI state for project management
 */
data class ProjectUiState(
    val projects: List<Project> = emptyList(),
    val currentProjectMembers: List<ProjectMember> = emptyList(),
    val projectStats: Map<String, ProjectStats> = emptyMap(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val isAddingMember: Boolean = false,
    val isLoadingStats: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
