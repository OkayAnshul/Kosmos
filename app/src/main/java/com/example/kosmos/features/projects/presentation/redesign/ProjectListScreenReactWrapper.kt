package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.project.presentation.ProjectViewModel
import com.example.kosmos.features.project.presentation.ProjectFilter
import com.example.kosmos.core.models.ProjectStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Wrapper for ProjectListScreenReact that connects to the backend.
 *
 * This wrapper:
 * - Injects ProjectViewModel via Hilt
 * - Collects real project data from the database
 * - Maps domain models to UI models
 * - Handles all backend operations (create, edit, archive, etc.)
 * - Maintains the exact React design UI
 */
@Composable
fun ProjectListScreenReactWrapper(
    onProjectClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    notificationBadgeCount: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // Handle success messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }

    // Trigger stats loading when projects are loaded
    LaunchedEffect(uiState.projects) {
        if (uiState.projects.isNotEmpty() && uiState.projectStats.isEmpty()) {
            viewModel.loadAllProjectStats()
        }
    }

    // Handle refresh with proper loading state
    val onRefresh: () -> Unit = {
        coroutineScope.launch {
            isRefreshing = true
            viewModel.refreshProjects()
            viewModel.loadAllProjectStats()
            delay(500) // Minimum visual feedback
            isRefreshing = false
        }
    }

    // Use filtered projects from ViewModel (applies both search and filter)
    val filteredProjects = viewModel.getFilteredProjects()

    // Map domain Project models to ProjectCardData for React UI
    val projectCards = filteredProjects.map { project ->
        val stats = uiState.projectStats[project.id]
        ProjectCardData(
            id = project.id,
            name = project.name,
            description = project.description,
            status = if (project.status == ProjectStatus.ARCHIVED) "Archived" else "Active",
            memberCount = stats?.memberCount ?: 0,
            chatCount = stats?.chatCount ?: 0,
            taskCount = stats?.taskCount ?: 0,
            completedTasks = stats?.completedTaskCount ?: 0,
            lastActivity = formatLastActivity(project.updatedAt ?: project.createdAt),
            accentColor = project.color
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        ProjectListScreenReact(
            projects = projectCards,
            onProjectClick = onProjectClick,
            onNotificationsClick = onNotificationsClick,
            onMenuClick = onMenuClick,
            onCreateProject = { showCreateDialog = true },
            searchQuery = uiState.searchQuery,
            onSearchChange = { viewModel.searchProjects(it) },
            activeFilter = when {
                uiState.activeFilter.name == "ALL" -> "All"
                uiState.activeFilter.name == "ACTIVE" -> "Active"
                uiState.activeFilter.name == "ARCHIVED" -> "Archived"
                else -> "All"
            },
            onFilterChange = { filter ->
                val projectFilter = when (filter) {
                    "Active" -> ProjectFilter.ACTIVE
                    "Archived" -> ProjectFilter.ARCHIVED
                    else -> ProjectFilter.ALL
                }
                viewModel.filterProjects(projectFilter)
            },
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            notificationBadgeCount = notificationBadgeCount
        )

        // Snackbar for error/success messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Create Project Wizard (full 3-step flow with members)
    if (showCreateDialog) {
        com.example.kosmos.features.projects.components.ProjectCreationWizard(
            isOpen = true,
            currentStep = uiState.wizardStep,
            projectData = uiState.projectCreationData,
            selectedMembers = uiState.selectedMembers,
            recentCollaborators = uiState.recentCollaborators,
            connectionUsers = uiState.connectionUsers,
            allUsers = uiState.allUsers,
            userSearchQuery = uiState.userSearchQuery,
            validationErrors = uiState.validationErrors,
            isCreating = uiState.isCreatingProject,
            currentUserId = viewModel.getCurrentUserId(),
            currentUserName = viewModel.getCurrentUserName(),
            onStepChange = { step -> viewModel.setWizardStep(step) },
            onProjectDataUpdate = { data -> viewModel.updateProjectData(data) },
            onAddMember = { user, role -> viewModel.addMemberToSelection(user, role) },
            onRemoveMember = { userId -> viewModel.removeMemberFromSelection(userId) },
            onUpdateMemberRole = { userId, role -> viewModel.updateMemberRole(userId, role) },
            onSearchQueryChange = { query -> viewModel.searchUsers(query) },
            onCreate = { viewModel.createProjectWithWizardData() },
            onDismiss = {
                viewModel.resetWizard()
                showCreateDialog = false
            }
        )
    }
}

/**
 * Format timestamp to relative time string (e.g., "2 hours ago")
 */
private fun formatLastActivity(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
