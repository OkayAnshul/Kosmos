package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.kosmos.features.projects.components.*
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.layouts.ListState
import com.example.kosmos.shared.ui.layouts.RefreshableStatefulList

/**
 * Project List Screen
 *
 * Features:
 * - View all projects
 * - Filter by status (active, archived)
 * - Sort by name, activity, members
 * - Pull-to-refresh
 * - Quick actions (swipe)
 * - Create new project
 *
 * Power user features:
 * - Swipe to archive/edit
 * - Quick stats view
 * - Activity indicators
 */

/**
 * Project Filter
 */
enum class ProjectFilter {
    ALL, ACTIVE, ARCHIVED
}

/**
 * Project Sort Option
 */
enum class ProjectSortOption {
    NAME, ACTIVITY, MEMBERS, TASKS
}

/**
 * Project List Screen
 *
 * @param projectsState Projects state
 * @param selectedFilter Selected filter
 * @param sortOption Current sort option
 * @param onFilterSelected Filter selection handler
 * @param onSortChange Sort change handler
 * @param onProjectClick Project click handler
 * @param onArchiveProject Archive project handler
 * @param onEditProject Edit project handler
 * @param onCreateProject Create project handler
 * @param onRefresh Refresh handler
 * @param isRefreshing Whether refreshing
 * @param onBackClick Back navigation handler
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    projectsState: ListState<ProjectItem>,
    selectedFilter: ProjectFilter,
    sortOption: ProjectSortOption,
    onFilterSelected: (ProjectFilter) -> Unit,
    onSortChange: (ProjectSortOption) -> Unit,
    onProjectClick: (String) -> Unit,
    onArchiveProject: (String) -> Unit,
    onEditProject: (String) -> Unit,
    onCreateProject: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean = false,
    onBackClick: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects") },
                // No back button - this is the home/landing screen
                actions = {
                    // Sort menu
                    Box {
                        IconButtonStandard(
                            icon = IconSet.Action.sort,
                            onClick = { showSortMenu = true },
                            contentDescription = "Sort"
                        )

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            ProjectSortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option.name.lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                        )
                                    },
                                    onClick = {
                                        onSortChange(option)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sortOption == option) {
                                            Icon(IconSet.Status.checkmark, null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FABStandard(
                icon = IconSet.Action.add,
                onClick = onCreateProject,
                contentDescription = "Create project"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips
            ProjectFilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected,
                modifier = Modifier.padding(horizontal = Tokens.Spacing.md)
            )

            // Project list
            RefreshableStatefulList(
                state = projectsState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                emptyTitle = "No projects yet",
                emptyMessage = "Create your first project to get started",
                emptyActionLabel = "Create Project",
                onEmptyAction = onCreateProject,
                errorTitle = "Failed to load projects",
                onRetry = onRefresh
            ) { projects ->
                items(
                    items = projects,
                    key = { it.id }
                ) { project ->
                    EnhancedProjectCard(
                        project = project,
                        onClick = { onProjectClick(project.id) },
                        onArchive = { onArchiveProject(project.id) },
                        onEdit = { onEditProject(project.id) },
                        modifier = Modifier.padding(
                            horizontal = Tokens.Spacing.md,
                            vertical = Tokens.Spacing.xs
                        )
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(Tokens.Spacing.xxl))
                }
            }
        }
    }
}

/**
 * Project Filter Chips
 */
@Composable
private fun ProjectFilterChips(
    selectedFilter: ProjectFilter,
    onFilterSelected: (ProjectFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    ChipGroup(
        chips = ProjectFilter.values().map {
            it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
        },
        selectedChips = setOf(
            selectedFilter.name.lowercase().replaceFirstChar { c -> c.uppercase() }
        ),
        onChipClick = { filterName ->
            val filter = ProjectFilter.values().find {
                it.name.lowercase().replaceFirstChar { c -> c.uppercase() } == filterName
            }
            filter?.let { onFilterSelected(it) }
        },
        modifier = modifier,
        multiSelect = false
    )
}
