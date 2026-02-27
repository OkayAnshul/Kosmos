package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.project.presentation.CreateProjectDialog
import com.example.kosmos.features.project.presentation.ProjectViewModel
import com.example.kosmos.features.projects.components.EditProjectDialog
import com.example.kosmos.features.projects.components.ProjectCreationWizard
import com.example.kosmos.shared.ui.layouts.ListState
import com.example.kosmos.shared.ui.mappers.ProjectDataMapper
import com.example.kosmos.shared.ui.mappers.ProjectDataMapper.toProjectItem
import com.example.kosmos.shared.ui.mappers.StateMapper
import com.example.kosmos.shared.utils.NetworkMonitor
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.kosmos.shared.ui.mappers.ProjectFilter as MapperProjectFilter
import com.example.kosmos.shared.ui.mappers.ProjectSortOption as MapperProjectSortOption

/**
 * Wrapper composable that connects ProjectListScreen to ProjectViewModel
 * Handles data mapping and state transformations
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkMonitorEntryPoint {
    fun networkMonitor(): NetworkMonitor
}

@Composable
fun ProjectListScreenWrapper(
    onProjectClick: (String) -> Unit,
    onCreateProject: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel(),
    username: String? = null,
    unreadNotificationCount: Int = 0,
    onNotificationsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val networkMonitor = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            NetworkMonitorEntryPoint::class.java
        ).networkMonitor()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOffline by networkMonitor.isOffline.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Track local UI state
    var isRefreshing by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<com.example.kosmos.core.models.Project?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Initialize default filter if needed
    LaunchedEffect(Unit) {
        if (uiState.activeFilter == com.example.kosmos.features.project.presentation.ProjectFilter.ALL) {
            viewModel.filterProjects(com.example.kosmos.features.project.presentation.ProjectFilter.ACTIVE)
        }
    }

    // Load project stats when projects change
    LaunchedEffect(uiState.projects) {
        if (uiState.projects.isNotEmpty() && uiState.projectStats.isEmpty()) {
            viewModel.loadAllProjectStats()
        }
    }

    // Get filtered and sorted projects from ViewModel
    val filteredDomainProjects = remember(uiState.projects, uiState.activeFilter, uiState.sortOption, uiState.searchQuery, uiState.projectStats) {
        viewModel.getFilteredProjects()
    }

    // Convert domain projects to UI projects with real stats
    val projectItems = remember(filteredDomainProjects, uiState.projectStats) {
        filteredDomainProjects.map { project ->
            val stats = uiState.projectStats[project.id]
            project.toProjectItem(
                memberCount = stats?.memberCount ?: 0,
                chatCount = stats?.chatCount ?: 0,
                taskCount = stats?.taskCount ?: 0,
                completedTaskCount = stats?.completedTaskCount ?: 0,
                unreadChatCount = stats?.unreadChatCount ?: 0,
                pendingTaskCount = stats?.pendingTaskCount ?: 0,
                lastActivityTimestamp = stats?.lastActivityTime
            )
        }
    }

    // Convert to ListState
    val projectsState = remember(uiState.isLoading, projectItems, uiState.error) {
        StateMapper.toListState(
            isLoading = uiState.isLoading,
            data = projectItems,
            error = uiState.error
        )
    }

    // Map ViewModel filter/sort to UI filter/sort
    val selectedFilter = remember(uiState.activeFilter) {
        when (uiState.activeFilter) {
            com.example.kosmos.features.project.presentation.ProjectFilter.ALL -> ProjectFilter.ALL
            com.example.kosmos.features.project.presentation.ProjectFilter.ACTIVE -> ProjectFilter.ACTIVE
            com.example.kosmos.features.project.presentation.ProjectFilter.ARCHIVED -> ProjectFilter.ARCHIVED
        }
    }

    val sortOption = remember(uiState.sortOption) {
        when (uiState.sortOption) {
            com.example.kosmos.features.project.presentation.ProjectSortOption.NAME -> ProjectSortOption.NAME
            com.example.kosmos.features.project.presentation.ProjectSortOption.ACTIVITY -> ProjectSortOption.ACTIVITY
            com.example.kosmos.features.project.presentation.ProjectSortOption.MEMBERS -> ProjectSortOption.MEMBERS
            com.example.kosmos.features.project.presentation.ProjectSortOption.TASKS -> ProjectSortOption.TASKS
        }
    }

    ProjectListScreen(
        projectsState = projectsState,
        selectedFilter = selectedFilter,
        sortOption = sortOption,
        searchQuery = uiState.searchQuery,
        isOffline = isOffline,
        onFilterSelected = { filter ->
            val viewModelFilter = when (filter) {
                ProjectFilter.ALL -> com.example.kosmos.features.project.presentation.ProjectFilter.ALL
                ProjectFilter.ACTIVE -> com.example.kosmos.features.project.presentation.ProjectFilter.ACTIVE
                ProjectFilter.ARCHIVED -> com.example.kosmos.features.project.presentation.ProjectFilter.ARCHIVED
            }
            viewModel.filterProjects(viewModelFilter)
        },
        onSortChange = { newSortOption ->
            val viewModelSort = when (newSortOption) {
                ProjectSortOption.NAME -> com.example.kosmos.features.project.presentation.ProjectSortOption.NAME
                ProjectSortOption.ACTIVITY -> com.example.kosmos.features.project.presentation.ProjectSortOption.ACTIVITY
                ProjectSortOption.MEMBERS -> com.example.kosmos.features.project.presentation.ProjectSortOption.MEMBERS
                ProjectSortOption.TASKS -> com.example.kosmos.features.project.presentation.ProjectSortOption.TASKS
            }
            viewModel.sortProjects(viewModelSort)
        },
        onSearchQueryChange = { query ->
            viewModel.searchProjects(query)
        },
        onProjectClick = onProjectClick,
        onArchiveProject = { projectId ->
            val project = uiState.projects.find { it.id == projectId }
            if (project != null) {
                if (project.status == com.example.kosmos.core.models.ProjectStatus.ARCHIVED) {
                    viewModel.unarchiveProject(projectId)
                } else {
                    viewModel.archiveProject(projectId)
                }
            }
        },
        onEditProject = { projectId ->
            val project = uiState.projects.find { it.id == projectId }
            editingProject = project
        },
        onCreateProject = { showCreateDialog = true },
        onRefresh = {
            isRefreshing = true
            // Reload stats for all projects
            coroutineScope.launch {
                viewModel.loadAllProjectStats()
                delay(500) // Brief delay for UX
                isRefreshing = false
            }
        },
        isRefreshing = isRefreshing,
        onBackClick = onBackClick,
        username = username,
        unreadNotificationCount = unreadNotificationCount,
        onNotificationsClick = onNotificationsClick
    )

    // Show create project wizard (new multi-step wizard)
    if (showCreateDialog) {
        ProjectCreationWizard(
            isOpen = true,
            currentStep = uiState.wizardStep,
            projectData = uiState.projectCreationData,
            selectedMembers = uiState.selectedMembers,
            recentCollaborators = uiState.recentCollaborators,
            allUsers = uiState.allUsers,
            userSearchQuery = uiState.userSearchQuery,
            validationErrors = uiState.validationErrors,
            isCreating = uiState.isCreatingProject,
            currentUserId = viewModel.getCurrentUserId(), // From ViewModel
            currentUserName = username ?: "User",
            onStepChange = viewModel::setWizardStep,
            onProjectDataUpdate = viewModel::updateProjectData,
            onAddMember = viewModel::addMemberToSelection,
            onRemoveMember = viewModel::removeMemberFromSelection,
            onUpdateMemberRole = viewModel::updateMemberRole,
            onSearchQueryChange = viewModel::searchUsers,
            onCreate = viewModel::createProjectWithWizardData,
            onDismiss = {
                showCreateDialog = false
                viewModel.resetWizard()
            }
        )
    }

    // Show edit project dialog if a project is being edited
    editingProject?.let { project ->
        EditProjectDialog(
            project = project,
            onDismiss = { editingProject = null },
            onSave = { name, description, status ->
                viewModel.updateProjectDetails(project.id, name, description, status)
                editingProject = null
            },
            onDelete = {
                viewModel.deleteProject(project.id)
                editingProject = null
            },
            isLoading = uiState.isUpdating
        )
    }
}
