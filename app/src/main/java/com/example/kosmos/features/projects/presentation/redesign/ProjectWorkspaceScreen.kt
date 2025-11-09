package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.chat.presentation.ChatListViewModel
import com.example.kosmos.features.project.presentation.ProjectViewModel
import com.example.kosmos.features.projects.presentation.MembersListScreen
import com.example.kosmos.features.tasks.presentation.redesign.MyTasksScreenWrapper
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Project Workspace Screen - Container with Persistent Bottom Nav
 *
 * This screen provides a workspace for a specific project with:
 * - Persistent bottom navigation (always visible)
 * - 5 tabs: Overview, Chats, Tasks, Members, Activity
 * - No navigation stack buildup - just tab switching
 * - Modern UX like Slack/Discord
 *
 * Architecture:
 * ┌──────────────────────────────┐
 * │      Top App Bar             │
 * ├──────────────────────────────┤
 * │                              │
 * │      Content Area            │
 * │   (switches based on tab)    │
 * │                              │
 * ├──────────────────────────────┤
 * │   Bottom Navigation (Fixed)  │
 * └──────────────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectWorkspaceScreen(
    projectId: String,
    onChatClick: (String) -> Unit,
    onTaskClick: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel(),
    chatListViewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Track selected tab - starts at Overview
    var selectedTab by remember { mutableStateOf(WorkspaceTab.OVERVIEW) }

    // Load project data
    LaunchedEffect(projectId) {
        viewModel.loadProjectMembers(projectId)
        viewModel.loadProjectStats(projectId)
    }

    // Find current project
    val currentProject = remember(uiState.projects, projectId) {
        uiState.projects.find { it.id == projectId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentProject?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(IconSet.Navigation.back, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Project settings */ }) {
                        Icon(IconSet.Action.moreVert, "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            ProjectWorkspaceBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                projectStats = uiState.projectStats[projectId]
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            // Content switches based on selected tab
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() + slideInHorizontally() togetherWith fadeOut() + slideOutHorizontally()
                },
                label = "workspace_content"
            ) { tab ->
                when (tab) {
                    WorkspaceTab.OVERVIEW -> {
                        ProjectDetailsScreenWrapper(
                            projectId = projectId,
                            onViewAllChats = { selectedTab = WorkspaceTab.CHATS },
                            onViewAllTasks = { selectedTab = WorkspaceTab.TASKS },
                            onViewAllMembers = { selectedTab = WorkspaceTab.MEMBERS },
                            onCreateChat = { /* TODO */ },
                            onCreateTask = { /* TODO */ },
                            onInviteMember = { /* TODO */ },
                            onEditProject = { /* TODO */ },
                            onChatClick = onChatClick,
                            onTaskClick = onTaskClick,
                            onBackClick = onBackClick
                        )
                    }

                    WorkspaceTab.CHATS -> {
                        // Chat list for this project
                        com.example.kosmos.features.chat.presentation.redesign.EnhancedChatListScreenWrapper(
                            projectId = projectId,
                            onChatClick = onChatClick,
                            onCreateChat = { /* TODO: Create chat */ },
                            onSearchClick = { /* TODO: Search */ },
                            onProfileClick = { /* TODO: Profile */ },
                            onSettingsClick = { /* TODO: Settings */ },
                            onLogoutClick = { /* TODO: Logout */ },
                            onBackClick = { selectedTab = WorkspaceTab.OVERVIEW }
                        )
                    }

                    WorkspaceTab.TASKS -> {
                        // Task board for this project
                        MyTasksScreenWrapper(
                            onTaskClick = onTaskClick,
                            onBackClick = { selectedTab = WorkspaceTab.OVERVIEW }
                        )
                    }

                    WorkspaceTab.MEMBERS -> {
                        // Members list for this project
                        MembersListScreen(
                            projectId = projectId,
                            onBackClick = { selectedTab = WorkspaceTab.OVERVIEW },
                            onUserClick = { /* TODO: User profile */ },
                            onAddMembersClick = { /* TODO: Add members */ }
                        )
                    }

                    WorkspaceTab.ACTIVITY -> {
                        // Activity feed for this project
                        ProjectActivityScreen(
                            projectId = projectId,
                            onBack = { selectedTab = WorkspaceTab.OVERVIEW }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Navigation Bar for Project Workspace
 * Always visible, allows quick switching between project sections
 */
@Composable
private fun ProjectWorkspaceBottomNav(
    selectedTab: WorkspaceTab,
    onTabSelected: (WorkspaceTab) -> Unit,
    projectStats: com.example.kosmos.core.models.ProjectStats?,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = Tokens.Elevation.level2
    ) {
        NavigationBarItem(
            selected = selectedTab == WorkspaceTab.OVERVIEW,
            onClick = { onTabSelected(WorkspaceTab.OVERVIEW) },
            icon = { Icon(IconSet.Navigation.home, "Overview") },
            label = { Text("Overview") }
        )

        NavigationBarItem(
            selected = selectedTab == WorkspaceTab.CHATS,
            onClick = { onTabSelected(WorkspaceTab.CHATS) },
            icon = {
                BadgedBox(
                    badge = {
                        if ((projectStats?.chatCount ?: 0) > 0) {
                            Badge { Text("${projectStats?.chatCount}") }
                        }
                    }
                ) {
                    Icon(IconSet.Message.chat, "Chats")
                }
            },
            label = { Text("Chats") }
        )

        NavigationBarItem(
            selected = selectedTab == WorkspaceTab.TASKS,
            onClick = { onTabSelected(WorkspaceTab.TASKS) },
            icon = {
                BadgedBox(
                    badge = {
                        if ((projectStats?.taskCount ?: 0) > 0) {
                            Badge { Text("${projectStats?.taskCount}") }
                        }
                    }
                ) {
                    Icon(IconSet.Task.task, "Tasks")
                }
            },
            label = { Text("Tasks") }
        )

        NavigationBarItem(
            selected = selectedTab == WorkspaceTab.MEMBERS,
            onClick = { onTabSelected(WorkspaceTab.MEMBERS) },
            icon = {
                BadgedBox(
                    badge = {
                        if ((projectStats?.memberCount ?: 0) > 0) {
                            Badge { Text("${projectStats?.memberCount}") }
                        }
                    }
                ) {
                    Icon(IconSet.User.group, "Members")
                }
            },
            label = { Text("Members") }
        )

        NavigationBarItem(
            selected = selectedTab == WorkspaceTab.ACTIVITY,
            onClick = { onTabSelected(WorkspaceTab.ACTIVITY) },
            icon = { Icon(IconSet.Time.history, "Activity") },
            label = { Text("Activity") }
        )
    }
}

/**
 * Placeholder Activity Screen
 * TODO: Implement activity feed showing recent project updates
 */
@Composable
private fun ProjectActivityScreen(
    projectId: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                IconSet.Time.history,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Activity Feed",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Coming soon: Recent updates, changes, and notifications",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Workspace Tab Enum
 * Defines the 5 tabs available in the project workspace
 */
enum class WorkspaceTab {
    OVERVIEW,
    CHATS,
    TASKS,
    MEMBERS,
    ACTIVITY
}
