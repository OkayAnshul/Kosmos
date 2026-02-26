package com.example.kosmos.features.project.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectCategory
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.ProjectStats
import com.example.kosmos.core.models.ProjectStatus
import com.example.kosmos.core.models.User
import com.example.kosmos.core.feedback.UserFeedbackManager
import com.example.kosmos.core.feedback.safeCall
import com.example.kosmos.core.validators.PermissionChecker
import com.example.kosmos.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import com.example.kosmos.data.repository.ProjectCreationData
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.data.sync.InitialSyncManager
import com.example.kosmos.shared.utils.ValidationUtils
import com.example.kosmos.shared.utils.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data class representing a selected member for project creation wizard
 * @param user User details
 * @param role Role to assign (MANAGER or MEMBER, ADMIN is automatic for creator)
 */
data class SelectedMember(
    val user: User,
    val role: ProjectRole = ProjectRole.MEMBER
)

/**
 * ViewModel for project management with RBAC
 * Handles project creation, member management, and permission checks
 */
@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val initialSyncManager: InitialSyncManager,
    private val userConnectionRepository: com.example.kosmos.data.repository.UserConnectionRepository,
    private val feedbackManager: UserFeedbackManager
) : ViewModel() {
    companion object {
        private const val TAG = "ProjectViewModel"
    }

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    // Track active stat observation jobs to prevent duplicates
    private val statsJobs = mutableMapOf<String, Job>()

    // Debounced search query flow
    private val _searchQuery = MutableStateFlow("")

    // Helper property to get current user (gets fresh value each time)
    private val currentUser: User?
        get() = authRepository.getCurrentUser()

    init {
        // Observe auth state reactively instead of capturing value
        viewModelScope.launch {
            authRepository.userFlow
                .filterNotNull()  // Wait until user is loaded
                .collect { user ->
                    // User is now authenticated and loaded
                    Log.d(TAG, "User authenticated: ${user.username}")
                    // REMOVED: syncProjectsFromSupabase() - Sync now handled by InitialSyncManager in MainActivity
                    loadUserProjects()
                    // Preload users for project creation wizard
                    loadAllUsers()
                    loadRecentCollaborators()
                    loadConnections()
                }
        }

        // Setup debounced search
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // 300ms debounce for better UX
                .distinctUntilChanged()
                .collect { query ->
                    _uiState.value = _uiState.value.copy(searchQuery = query)
                }
        }
    }

    /**
     * Sync projects from Supabase to local cache
     *
     * @Deprecated Use InitialSyncManager in MainActivity instead.
     * ViewModels should ONLY observe Flows, never trigger syncs.
     * Syncing in viewModelScope causes cancellations when navigating away.
     */
    @Deprecated(
        message = "Use InitialSyncManager.syncAllData() instead. ViewModels should not trigger syncs.",
        level = DeprecationLevel.WARNING
    )
    private fun syncProjectsFromSupabase() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    val result = projectRepository.syncUserProjects(user.id)
                    if (result.isFailure) {
                        Log.w(TAG, "Failed to sync projects from Supabase", result.exceptionOrNull())
                    }
                } else {
                    Log.w(TAG, "Cannot sync projects: user not authenticated")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Error syncing projects", e)
            }
        }
    }

    private var projectsJob: Job? = null

    /**
     * Load all projects for the current user
     */
    private fun loadUserProjects() {
        // Cancel previous job if exists
        projectsJob?.cancel()
        projectsJob = viewModelScope.launch {
            try {
                // Get current user directly from authRepository (reactive)
                val user = authRepository.getCurrentUser()
                if (user == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Please sign in to view projects"
                    )
                    return@launch
                }

                projectRepository.getUserProjectsFlow(user.id)
                    .catch { e ->
                        // Handle Flow errors gracefully
                        Log.e(TAG, "Error loading projects", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load projects: ${e.message}"
                        )
                    }
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5000),  // Stop after 5s of no subscribers
                        initialValue = emptyList()
                    )
                    .collect { projects ->
                        Log.d(TAG, "📦 Projects updated: ${projects.size} projects")
                        _uiState.value = _uiState.value.copy(
                            projects = projects,
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Error in loadUserProjects", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load projects: ${e.message}"
                )
            }
        }
    }

    /**
     * Refresh projects by restarting the Flow collection
     */
    fun refreshProjects() {
        loadUserProjects()
    }

    /**
     * Create a new project
     * @param name Project name
     * @param description Project description
     */
    fun createProject(name: String, description: String) {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                if (user == null) {
                    _uiState.value = _uiState.value.copy(error = "Please sign in to create projects")
                    return@launch
                }

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
                if (e is CancellationException) throw e
                if (e is PermissionChecker.PermissionDeniedException) {
                    feedbackManager.permissionDenied("create project", e.message ?: "Permission denied")
                    _uiState.value = _uiState.value.copy(isCreating = false)
                    return@launch
                }
                val msg = ErrorMapper.mapError(e, "create project")
                feedbackManager.error(msg)
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    error = msg
                )
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
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    error = ErrorMapper.mapError(e, "load members")
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
                    if (e is CancellationException) throw e
                    if (e is PermissionChecker.PermissionDeniedException) {
                        feedbackManager.permissionDenied("add member", e.message ?: "Permission denied")
                        _uiState.value = _uiState.value.copy(isAddingMember = false)
                        return@launch
                    }
                    val msg = ErrorMapper.mapError(e, "add member")
                    feedbackManager.error(msg)
                    _uiState.value = _uiState.value.copy(
                        isAddingMember = false,
                        error = msg
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
                    if (e is CancellationException) throw e
                    if (e is PermissionChecker.PermissionDeniedException) {
                        feedbackManager.permissionDenied("remove member", e.message ?: "Permission denied")
                        return@launch
                    }
                    val msg = ErrorMapper.mapError(e, "remove member")
                    feedbackManager.error(msg)
                    _uiState.value = _uiState.value.copy(error = msg)
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
                    if (e is CancellationException) throw e
                    if (e is PermissionChecker.PermissionDeniedException) {
                        feedbackManager.permissionDenied("change role", e.message ?: "Permission denied")
                        return@launch
                    }
                    val msg = ErrorMapper.mapError(e, "change role")
                    feedbackManager.error(msg)
                    _uiState.value = _uiState.value.copy(error = msg)
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
                    if (e is CancellationException) throw e
                    if (e is PermissionChecker.PermissionDeniedException) {
                        feedbackManager.permissionDenied("archive project", e.message ?: "Permission denied")
                        return@launch
                    }
                    val msg = ErrorMapper.mapError(e, "archive project")
                    feedbackManager.error(msg)
                    _uiState.value = _uiState.value.copy(error = msg)
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
                    if (e is CancellationException) throw e
                    if (e is PermissionChecker.PermissionDeniedException) {
                        feedbackManager.permissionDenied("restore project", e.message ?: "Permission denied")
                        return@launch
                    }
                    val msg = ErrorMapper.mapError(e, "restore project")
                    feedbackManager.error(msg)
                    _uiState.value = _uiState.value.copy(error = msg)
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
                    if (e is CancellationException) throw e
                    if (e is PermissionChecker.PermissionDeniedException) {
                        feedbackManager.permissionDenied("update project", e.message ?: "Permission denied")
                        _uiState.value = _uiState.value.copy(isUpdating = false)
                        return@launch
                    }
                    val msg = ErrorMapper.mapError(e, "update project")
                    feedbackManager.error(msg)
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        error = msg
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
                    projectRepository.getUserProjectsFlow(user.id)
                        .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),  // Stop after 5s of no subscribers
                            initialValue = emptyList()
                        )
                        .collect { projects ->
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
                    if (e is CancellationException) throw e
                    _uiState.value = _uiState.value.copy(
                        isLoadingStats = false,
                        error = ErrorMapper.mapError(e, "load project stats")
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
     * Sync project data (members, tasks, chats) from Supabase into Room.
     * Called when entering the project workspace so tab content is populated.
     */
    fun syncProjectData(projectId: String) {
        val userId = currentUser?.id ?: return
        viewModelScope.launch {
            initialSyncManager.syncProjectData(projectId, userId)
        }
    }

    /**
     * Get stats for a specific project from current state
     * @param projectId Project ID
     * @return ProjectStats or null if not loaded
     */
    fun getProjectStats(projectId: String): ProjectStats? {
        return _uiState.value.projectStats[projectId]
    }

    /**
     * Delete a project permanently
     * Requires DELETE_PROJECT permission
     * @param projectId Project ID to delete
     */
    fun deleteProject(projectId: String) {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(isUpdating = true)

                    val result = projectRepository.deleteProject(
                        projectId = projectId,
                        userId = user.id
                    )

                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            successMessage = "Project deleted successfully"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = "Failed to delete project: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    if (e is PermissionChecker.PermissionDeniedException) {
                        feedbackManager.permissionDenied("delete project", e.message ?: "Permission denied")
                        _uiState.value = _uiState.value.copy(isUpdating = false)
                        return@launch
                    }
                    val msg = ErrorMapper.mapError(e, "delete project")
                    feedbackManager.error(msg)
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        error = msg
                    )
                }
            }
        }
    }

    /**
     * Filter projects by status
     * @param filter Filter option (ALL, ACTIVE, ARCHIVED)
     */
    fun filterProjects(filter: ProjectFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }

    /**
     * Sort projects by selected option
     * @param option Sort option (NAME, ACTIVITY, MEMBERS, TASKS)
     */
    fun sortProjects(option: ProjectSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
    }

    /**
     * Search projects by name or description
     * @param query Search query
     * Will trigger debounced search automatically (300ms delay)
     */
    fun searchProjects(query: String) {
        _searchQuery.value = query
    }

    /**
     * Get filtered and sorted projects based on current UI state
     * @return List of projects after applying filters, search, and sorting
     */
    fun getFilteredProjects(): List<Project> {
        val state = _uiState.value
        var filteredProjects = state.projects

        // Apply status filter
        filteredProjects = when (state.activeFilter) {
            ProjectFilter.ALL -> filteredProjects
            ProjectFilter.ACTIVE -> filteredProjects.filter { it.status == ProjectStatus.ACTIVE }
            ProjectFilter.ARCHIVED -> filteredProjects.filter { it.status == ProjectStatus.ARCHIVED }
        }

        // Apply search query
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.trim().lowercase()
            filteredProjects = filteredProjects.filter { project ->
                project.name.lowercase().contains(query) ||
                project.description.lowercase().contains(query)
            }
        }

        // Apply sorting
        filteredProjects = when (state.sortOption) {
            ProjectSortOption.NAME -> filteredProjects.sortedBy { it.name.lowercase() }
            ProjectSortOption.ACTIVITY -> filteredProjects.sortedByDescending { it.updatedAt ?: it.createdAt }
            ProjectSortOption.MEMBERS -> {
                // Sort by member count (requires stats)
                filteredProjects.sortedByDescending { project ->
                    state.projectStats[project.id]?.memberCount ?: 0
                }
            }
            ProjectSortOption.TASKS -> {
                // Sort by task count (requires stats)
                filteredProjects.sortedByDescending { project ->
                    state.projectStats[project.id]?.taskCount ?: 0
                }
            }
        }

        return filteredProjects
    }

    // ============================================================
    // PROJECT CREATION WIZARD METHODS
    // ============================================================

    /**
     * Get current user ID
     * @return Current user ID or empty string if not authenticated
     */
    fun getCurrentUserId(): String {
        return currentUser?.id ?: ""
    }

    fun getCurrentUserName(): String {
        val user = currentUser
        return user?.displayName?.takeIf { it.isNotBlank() } ?: user?.username ?: ""
    }

    /**
     * Set the current wizard step
     * @param step Step number (1-3)
     */
    fun setWizardStep(step: Int) {
        _uiState.value = _uiState.value.copy(wizardStep = step)
    }

    /**
     * Update project creation data from wizard
     * @param data Project creation data
     */
    fun updateProjectData(data: ProjectCreationData) {
        _uiState.value = _uiState.value.copy(
            projectCreationData = data,
            validationErrors = validateProjectData(data)
        )
    }

    /**
     * Add a member to the selection for project creation
     * @param user User to add
     * @param role Role to assign (default MEMBER)
     */
    fun addMemberToSelection(user: User, role: ProjectRole = ProjectRole.MEMBER) {
        val currentMembers = _uiState.value.selectedMembers

        // Check if user is already selected
        if (currentMembers.any { it.user.id == user.id }) {
            return
        }

        // Don't allow adding creator as they're automatically ADMIN
        if (user.id == currentUser?.id) {
            return
        }

        val newMember = SelectedMember(user, role)
        _uiState.value = _uiState.value.copy(
            selectedMembers = currentMembers + newMember
        )
    }

    /**
     * Remove a member from the selection
     * @param userId User ID to remove
     */
    fun removeMemberFromSelection(userId: String) {
        val currentMembers = _uiState.value.selectedMembers
        _uiState.value = _uiState.value.copy(
            selectedMembers = currentMembers.filter { it.user.id != userId }
        )
    }

    /**
     * Update a member's role in the selection
     * @param userId User ID
     * @param newRole New role to assign
     */
    fun updateMemberRole(userId: String, newRole: ProjectRole) {
        val currentMembers = _uiState.value.selectedMembers
        _uiState.value = _uiState.value.copy(
            selectedMembers = currentMembers.map { member ->
                if (member.user.id == userId) {
                    member.copy(role = newRole)
                } else {
                    member
                }
            }
        )
    }

    /**
     * Load recent collaborators for the current user
     */
    fun loadRecentCollaborators() {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    val collaborators = userRepository.getRecentCollaborators(user.id, limit = 10)
                    _uiState.value = _uiState.value.copy(
                        recentCollaborators = collaborators
                    )
                } catch (e: Exception) {
                    // Silently fail - not critical
                    _uiState.value = _uiState.value.copy(
                        recentCollaborators = emptyList()
                    )
                }
            }
        }
    }

    /**
     * Load accepted connections for current user (shown first in Step2)
     */
    fun loadConnections() {
        currentUser?.let { user ->
            viewModelScope.launch {
                safeCall(feedbackManager, tag = "ProjectViewModel", action = "load connections") {
                    val connectionIds = userConnectionRepository.getAcceptedIds(user.id)
                    val connectionUsers = connectionIds.mapNotNull { id ->
                        userRepository.getUserById(id)
                    }
                    _uiState.value = _uiState.value.copy(connectionUsers = connectionUsers)
                }
            }
        }
    }

    /**
     * Load all users for member selection
     * Fetches directly from Supabase instead of relying on Room cache
     */
    fun loadAllUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingUsers = true, userLoadError = null)

            val result = userRepository.getAllUsersFromSupabase()

            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(
                    allUsers = result.getOrNull() ?: emptyList(),
                    isLoadingUsers = false,
                    userLoadError = null
                )
            } else {
                _uiState.value.copy(
                    allUsers = emptyList(),
                    isLoadingUsers = false,
                    userLoadError = result.exceptionOrNull()?.message ?: "Failed to load users"
                )
            }
        }
    }

    /**
     * Search users with debounce
     * Fetches search results directly from Supabase
     * @param query Search query (username, name, or email)
     */
    private var searchJob: Job? = null

    fun searchUsers(query: String) {
        _uiState.value = _uiState.value.copy(userSearchQuery = query)

        // Debounce search (300ms)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)

            _uiState.value = _uiState.value.copy(isLoadingUsers = true, userLoadError = null)

            val result = if (query.length < 2) {
                // Load all users if query too short
                userRepository.getAllUsersFromSupabase()
            } else {
                // Search users from Supabase
                userRepository.searchUsersFromSupabase(query, limit = 50)
            }

            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(
                    allUsers = result.getOrNull() ?: emptyList(),
                    isLoadingUsers = false,
                    userLoadError = null
                )
            } else {
                _uiState.value.copy(
                    allUsers = emptyList(),
                    isLoadingUsers = false,
                    userLoadError = result.exceptionOrNull()?.message ?: "Failed to search users"
                )
            }
        }
    }

    /**
     * Validate current wizard step
     * @return true if current step is valid
     */
    fun validateCurrentStep(): Boolean {
        val state = _uiState.value
        return when (state.wizardStep) {
            1 -> {
                // Step 1: Validate project data
                val errors = state.projectCreationData?.let { validateProjectData(it) } ?: mapOf("name" to "Project data is required")
                _uiState.value = _uiState.value.copy(validationErrors = errors)
                errors.isEmpty()
            }
            2 -> {
                // Step 2: Members are optional, always valid
                true
            }
            3 -> {
                // Step 3: Review, always valid
                true
            }
            else -> false
        }
    }

    /**
     * Create project with wizard data
     */
    fun createProjectWithWizardData() {
        val state = _uiState.value
        val projectData = state.projectCreationData

        if (projectData == null) {
            _uiState.value = _uiState.value.copy(
                error = "Project data is missing"
            )
            return
        }

        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(isCreatingProject = true)

                    // Set ownerId to current user's ID before creating project
                    val projectDataWithOwner = projectData.copy(ownerId = user.id)

                    // Convert selected members to (userId, role) pairs
                    val initialMembers = state.selectedMembers.map { member ->
                        Pair(member.user.id, member.role)
                    }

                    val result = projectRepository.createProjectWithMembers(
                        projectData = projectDataWithOwner,
                        initialMembers = initialMembers
                    )

                    if (result.isSuccess) {
                        // REMOVED: Sync call - InitialSyncManager will handle sync
                        // Project creation updates Supabase, Room Flow will update automatically
                        // If needed, realtime listeners will also pick up the change
                        Log.d("ProjectViewModel", "✅ Project created successfully")

                        _uiState.value = _uiState.value.copy(
                            isCreatingProject = false,
                            successMessage = "Project created successfully with ${initialMembers.size} members",
                            // Don't reset wizard yet - let UI handle it after showing success
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isCreatingProject = false,
                            error = "Failed to create project: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    if (e is PermissionChecker.PermissionDeniedException) {
                        feedbackManager.permissionDenied("create project", e.message ?: "Permission denied")
                        _uiState.value = _uiState.value.copy(isCreatingProject = false)
                        return@launch
                    }
                    val msg = ErrorMapper.mapError(e, "create project")
                    feedbackManager.error(msg)
                    _uiState.value = _uiState.value.copy(
                        isCreatingProject = false,
                        error = msg
                    )
                }
            }
        }
    }

    /**
     * Reset wizard state to initial values
     */
    fun resetWizard() {
        _uiState.value = _uiState.value.copy(
            wizardStep = 1,
            projectCreationData = null,
            selectedMembers = emptyList(),
            userSearchQuery = "",
            validationErrors = emptyMap(),
            isCreatingProject = false
        )
    }

    /**
     * Validate project data and return errors
     * @param data Project creation data
     * @return Map of field names to error messages
     */
    private fun validateProjectData(data: ProjectCreationData): Map<String, String> {
        // Use centralized ValidationUtils for consistency
        return ValidationUtils.validateProjectData(
            name = data.name,
            description = data.description ?: "",
            category = data.category?.name ?: "OTHER",
            deadline = data.deadline,
            websiteUrl = data.websiteUrl,
            githubUrl = data.githubUrl,
            projectMotive = data.projectMotive,
            techStack = data.techStack,
            tags = data.tags,
            businessModel = data.businessModel,
            targetAudience = data.targetAudience,
            industryTags = data.industryTags
        )
    }

    // Keep these helper methods for backward compatibility
    private fun isValidUrl(url: String): Boolean {
        return ValidationUtils.isValidUrlFormat(url)
    }

    private fun isValidGitHubUrl(url: String): Boolean {
        return ValidationUtils.isValidGitHubUrlFormat(url)
    }

    // Old validation method (replaced by ValidationUtils.validateProjectData)
    private fun validateProjectDataOld(data: ProjectCreationData): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        // Name validation
        when {
            data.name.isBlank() -> errors["name"] = "Project name is required"
            data.name.length < 3 -> errors["name"] = "Name must be at least 3 characters"
            data.name.length > 100 -> errors["name"] = "Name must be less than 100 characters"
        }

        // URL validations
        data.githubUrl?.let { url ->
            if (url.isNotBlank() && !isValidGitHubUrl(url)) {
                errors["githubUrl"] = "Invalid GitHub URL"
            }
        }

        data.websiteUrl?.let { url ->
            if (url.isNotBlank() && !isValidUrl(url)) {
                errors["websiteUrl"] = "Invalid URL format"
            }
        }

        // Deadline validation
        data.deadline?.let { deadline ->
            if (deadline <= System.currentTimeMillis()) {
                errors["deadline"] = "Deadline must be in the future"
            }
        }

        // Category-specific validations
        when (data.category) {
            ProjectCategory.TECH -> {
                if (data.techStack.isNullOrEmpty()) {
                    errors["techStack"] = "Please select at least one technology"
                }
            }
            ProjectCategory.SOCIAL -> {
                if (data.projectMotive.isNullOrBlank()) {
                    errors["projectMotive"] = "Please describe the project's social impact"
                }
            }
            ProjectCategory.BUSINESS -> {
                if (data.businessModel.isNullOrBlank()) {
                    errors["businessModel"] = "Business model is required"
                }
            }
            ProjectCategory.OTHER -> {
                // No specific requirements for OTHER category
            }
        }

        return errors
    }
}

/**
 * Filter options for project list
 */
enum class ProjectFilter {
    ALL,
    ACTIVE,
    ARCHIVED
}

/**
 * Sort options for project list
 */
enum class ProjectSortOption {
    NAME,
    ACTIVITY,
    MEMBERS,
    TASKS
}

/**
 * UI state for project management
 */
data class ProjectUiState(
    // Existing fields
    val projects: List<Project> = emptyList(),
    val currentProjectMembers: List<ProjectMember> = emptyList(),
    val projectStats: Map<String, ProjectStats> = emptyMap(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val isAddingMember: Boolean = false,
    val isLoadingStats: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val activeFilter: ProjectFilter = ProjectFilter.ALL,
    val sortOption: ProjectSortOption = ProjectSortOption.ACTIVITY,
    val searchQuery: String = "",

    // ========================================================================
    // PROJECT CREATION WIZARD FIELDS (Added 2026-01-06)
    // ========================================================================

    /**
     * Current wizard step (1 = Details, 2 = Members, 3 = Review)
     */
    val wizardStep: Int = 1,

    /**
     * Project data being created in the wizard
     */
    val projectCreationData: ProjectCreationData? = null,

    /**
     * Members selected to add to the project
     */
    val selectedMembers: List<SelectedMember> = emptyList(),

    /**
     * Recent collaborators for quick member selection
     */
    val recentCollaborators: List<User> = emptyList(),

    /**
     * Connected users for quick member selection (shown first)
     */
    val connectionUsers: List<User> = emptyList(),

    /**
     * All available users for member selection
     */
    val allUsers: List<User> = emptyList(),

    /**
     * User search query for filtering members
     */
    val userSearchQuery: String = "",

    /**
     * Whether users are being loaded/searched
     */
    val isLoadingUsers: Boolean = false,

    /**
     * Error message from user loading/search
     */
    val userLoadError: String? = null,

    /**
     * Validation errors map (field name → error message)
     */
    val validationErrors: Map<String, String> = emptyMap(),

    /**
     * Whether project is being created (for wizard loading state)
     */
    val isCreatingProject: Boolean = false
)
