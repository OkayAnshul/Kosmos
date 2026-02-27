package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.kosmos.features.projects.components.*
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.GlassmorphicTokens
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
 * @param searchQuery Current search query
 * @param onFilterSelected Filter selection handler
 * @param onSortChange Sort change handler
 * @param onSearchQueryChange Search query change handler
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
    searchQuery: String = "",
    isOffline: Boolean = false,
    onFilterSelected: (ProjectFilter) -> Unit,
    onSortChange: (ProjectSortOption) -> Unit,
    onSearchQueryChange: (String) -> Unit = {},
    onProjectClick: (String) -> Unit,
    onArchiveProject: (String) -> Unit,
    onEditProject: (String) -> Unit,
    onCreateProject: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean = false,
    onBackClick: () -> Unit,
    username: String? = null,
    unreadNotificationCount: Int = 0,
    onNotificationsClick: () -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var shouldShowGreeting by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            CoreTopBar(
                title = if (username != null && shouldShowGreeting) "" else "Projects",
                actions = {
                    // Notification bell with badge
                    androidx.compose.material3.BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                androidx.compose.material3.Badge {
                                    androidx.compose.material3.Text(
                                        text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString()
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onNotificationsClick) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    }

                    // Sort menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = IconSet.Action.sort,
                                contentDescription = "Sort"
                            )
                        }

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
            CoreButton(
                onClick = onCreateProject,
                label = "Create",
                icon = IconSet.Action.add,
                variant = ButtonVariant.PRIMARY
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassmorphicTokens.GradientBackground.primaryStart,
                            GlassmorphicTokens.GradientBackground.primaryMiddle,
                            GlassmorphicTokens.GradientBackground.primaryEnd
                        )
                    )
                )
                .padding(padding)
        ) {
            // Greeting animation (if applicable)
            if (username != null && shouldShowGreeting) {
                AnimatedGreeting(
                    username = username,
                    textColor = ColorTokens.Surface.onLight,
                    onHideComplete = { shouldShowGreeting = false },
                    modifier = Modifier.padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm)
                )
            }

            // Offline mode banner (minimal - only for critical screens)
            OfflineModeBannerCompact(isOffline = isOffline)

            // Search bar
            CoreInput(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = "Search projects...",
                leadingIcon = {
                    Icon(
                        imageVector = IconSet.Action.search,
                        contentDescription = "Search"
                    )
                },
                modifier = Modifier.padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm)
            )

            // Filter chips
            ProjectFilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected,
                modifier = Modifier.padding(horizontal = Tokens.Spacing.md)
            )

            // Project list with glassmorphic cards
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
                            horizontal = GlassmorphicTokens.Whitespace.md,
                            vertical = GlassmorphicTokens.Whitespace.xs
                        )
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(GlassmorphicTokens.Whitespace.xl))
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
