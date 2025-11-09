package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.project.presentation.CreateProjectDialog
import com.example.kosmos.features.project.presentation.ProjectViewModel
import com.example.kosmos.features.projects.components.EditProjectDialog
import com.example.kosmos.shared.ui.layouts.ListState
import com.example.kosmos.shared.ui.mappers.ProjectDataMapper
import com.example.kosmos.shared.ui.mappers.ProjectDataMapper.toProjectItem
import com.example.kosmos.shared.ui.mappers.StateMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.kosmos.shared.ui.mappers.ProjectFilter as MapperProjectFilter
import com.example.kosmos.shared.ui.mappers.ProjectSortOption as MapperProjectSortOption

/**
 * Wrapper composable that connects ProjectListScreen to ProjectViewModel
 * Handles data mapping and state transformations
 */
@Composable
fun ProjectListScreenWrapper(
    onProjectClick: (String) -> Unit,
    onCreateProject: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // Track filter and sort options
    var selectedFilter by remember { mutableStateOf(ProjectFilter.ACTIVE) }
    var sortOption by remember { mutableStateOf(ProjectSortOption.ACTIVITY) }
    var isRefreshing by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<com.example.kosmos.core.models.Project?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Load project stats when projects change
    LaunchedEffect(uiState.projects) {
        if (uiState.projects.isNotEmpty() && uiState.projectStats.isEmpty()) {
            viewModel.loadAllProjectStats()
        }
    }

    // Convert domain projects to UI projects with real stats
    val projectItems = remember(uiState.projects, uiState.projectStats) {
        uiState.projects.map { project ->
            val stats = uiState.projectStats[project.id]
            project.toProjectItem(
                memberCount = stats?.memberCount ?: 0,
                chatCount = stats?.chatCount ?: 0,
                taskCount = stats?.taskCount ?: 0,
                completedTaskCount = stats?.completedTaskCount ?: 0,
                unreadChatCount = stats?.unreadChatCount ?: 0,
                pendingTaskCount = stats?.pendingTaskCount ?: 0,
                lastActivityTimestamp = stats?.lastActivityTime // Pass Long directly for timestamp formatting
            )
        }
    }

    // Apply filter
    val filteredProjects = remember(projectItems, selectedFilter) {
        val mapperFilter = when (selectedFilter) {
            ProjectFilter.ALL -> MapperProjectFilter.ALL
            ProjectFilter.ACTIVE -> MapperProjectFilter.ACTIVE
            ProjectFilter.ARCHIVED -> MapperProjectFilter.ARCHIVED
        }
        ProjectDataMapper.filterProjects(projectItems, mapperFilter)
    }

    // Apply sorting
    val sortedProjects = remember(filteredProjects, sortOption) {
        val mapperSort = when (sortOption) {
            ProjectSortOption.NAME -> MapperProjectSortOption.NAME
            ProjectSortOption.ACTIVITY -> MapperProjectSortOption.ACTIVITY
            ProjectSortOption.MEMBERS -> MapperProjectSortOption.MEMBERS
            ProjectSortOption.TASKS -> MapperProjectSortOption.TASKS
        }
        ProjectDataMapper.sortProjects(filteredProjects, mapperSort)
    }

    // Convert to ListState
    val projectsState = remember(uiState.isLoading, sortedProjects, uiState.error) {
        StateMapper.toListState(
            isLoading = uiState.isLoading,
            data = sortedProjects,
            error = uiState.error
        )
    }

    ProjectListScreen(
        projectsState = projectsState,
        selectedFilter = selectedFilter,
        sortOption = sortOption,
        onFilterSelected = { filter ->
            selectedFilter = filter
        },
        onSortChange = { newSortOption ->
            sortOption = newSortOption
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
        onBackClick = onBackClick
    )

    // Show create project dialog
    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description ->
                viewModel.createProject(name, description)
                showCreateDialog = false
            },
            isCreating = uiState.isCreating
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
            isLoading = uiState.isUpdating
        )
    }
}
