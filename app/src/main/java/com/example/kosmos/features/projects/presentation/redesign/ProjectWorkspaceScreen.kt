package com.example.kosmos.features.projects.presentation.redesign

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.kosmos.features.chat.presentation.ChatListViewModel
import com.example.kosmos.features.project.presentation.ProjectViewModel
import com.example.kosmos.features.projects.presentation.redesign.MembersListScreenWrapper
import com.example.kosmos.features.tasks.presentation.redesign.MyTasksScreenReactWrapper
import com.example.kosmos.features.tasks.presentation.redesign.ProjectTasksScreenReactWrapper
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Project Workspace Screen - Container with Bottom Nav + Swipeable Content
 *
 * This screen provides a workspace for a specific project with:
 * - Full-screen swipeable content (HorizontalPager) - swipe left/right to navigate
 * - Bottom navigation bar with React design styling
 * - 5 tabs: Overview, Chats, Tasks, Members, Activity
 * - Badge indicators showing counts for Chats, Tasks, and Members
 * - Modern UX like Slack/Discord with gesture navigation
 *
 * Architecture:
 * ┌──────────────────────────────────────────┐
 * │                                          │
 * │      Swipeable Content Area              │
 * │   (swipe left/right to navigate)         │
 * │      Tap nav item or swipe               │
 * │                                          │
 * ├──────────────────────────────────────────┤
 * │   Bottom Navigation (Fixed)              │
 * │  [Overview][Chats][Tasks][Members][Activity] │
 * │  Styled with React theme colors          │
 * └──────────────────────────────────────────┘
 *
 * Styling (React Design):
 * - Nav container: ColorTokens.ReactTheme.card (#18181D)
 * - Top border: ColorTokens.ReactTheme.border (#2A2A32), 1.dp
 * - Selected icon/text: ColorTokens.ReactTheme.primary (#7C3AED)
 * - Unselected icon/text: ColorTokens.ReactTheme.mutedForeground (#9CA3AF)
 * - Badge: Primary color with alpha background
 *
 * Notes:
 * - Back navigation: System back button or in-content back buttons
 * - Project menu: Available in Overview tab content
 * - Gesture navigation: Swipe left/right OR tap bottom nav items
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectWorkspaceScreen(
    projectId: String,
    onChatClick: (String) -> Unit,
    onTaskClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onCreateChat: () -> Unit,
    onCreateTask: () -> Unit,
    onInviteMembers: () -> Unit,
    onEditProject: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel(),
    chatListViewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Pager state for swipeable tabs
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { WorkspaceTab.values().size }
    )
    val coroutineScope = rememberCoroutineScope()

    // Load project data
    LaunchedEffect(projectId) {
        viewModel.syncProjectData(projectId)
        viewModel.loadProjectMembers(projectId)
        viewModel.loadProjectStats(projectId)
    }

    // Find current project
    val currentProject = remember(uiState.projects, projectId) {
        uiState.projects.find { it.id == projectId }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Swipeable Content (full screen - no top bar)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (WorkspaceTab.values()[page]) {
                WorkspaceTab.OVERVIEW -> {
                    // Use new React design wrapper
                    ProjectDetailsScreenReactWrapper(
                        projectId = projectId,
                        onBack = onBackClick,
                        onNewTask = onCreateTask,
                        onNewChat = onCreateChat,
                        onEditProject = { _ -> onEditProject() },
                        onViewChats = {
                            Log.d("ProjectWorkspace", "onViewChats called - switching to CHATS")
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(WorkspaceTab.CHATS.ordinal)
                            }
                        },
                        onViewTasks = {
                            Log.d("ProjectWorkspace", "onViewTasks called - switching to TASKS")
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(WorkspaceTab.TASKS.ordinal)
                            }
                        },
                        onViewMembers = {
                            Log.d("ProjectWorkspace", "onViewMembers called - switching to MEMBERS")
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(WorkspaceTab.MEMBERS.ordinal)
                            }
                        },
                        onViewActivity = {
                            Log.d("ProjectWorkspace", "onViewActivity called - switching to ACTIVITY")
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(WorkspaceTab.ACTIVITY.ordinal)
                            }
                        }
                    )
                }

                WorkspaceTab.CHATS -> {
                    // Chat list for this project using React design
                    com.example.kosmos.features.chat.presentation.redesign.ChatListScreenReactWrapper(
                        projectId = projectId,
                        onChatClick = onChatClick,
                        onCreateChat = onCreateChat
                    )
                }

                WorkspaceTab.TASKS -> {
                    // Use project-scoped tasks wrapper (shows ALL tasks in this project)
                    ProjectTasksScreenReactWrapper(
                        projectId = projectId,
                        onTaskClick = onTaskClick,
                        onTaskEdit = onTaskClick,
                        onCreateTask = onCreateTask
                    )
                }

                WorkspaceTab.MEMBERS -> {
                    // Members list for this project (React theme)
                    MembersListScreenReactWrapper(
                        projectId = projectId,
                        onMemberClick = onUserClick,
                        onAddMembersClick = onInviteMembers
                    )
                }

                WorkspaceTab.ACTIVITY -> {
                    // Activity feed for this project (React theme)
                    ProjectActivityScreenReactWrapper(
                        projectId = projectId
                    )
                }
            }
        }

        // Bottom Navigation Bar (React Design)
        ProjectWorkspaceBottomNav(
            selectedTabIndex = pagerState.currentPage,
            onTabSelected = { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(index)
                }
            },
            projectStats = uiState.projectStats[projectId]
        )
    }
}

// ============================================================================
// ARCHIVED COMPONENT: UnifiedWorkspaceTopBar
// ============================================================================
// This component was removed to maximize screen space for content.
// Kept here for reference in case top bar needs to be restored.
// Archived: 2026-01-22
// ============================================================================

/**
 * [ARCHIVED] Unified Workspace Top Bar
 *
 * A single, smart top app bar that handles all workspace screens:
 * - Dynamically shows screen name based on selected tab
 * - Handles loading states (shows "Loading..." when project not loaded)
 * - Optional menu button (pass onMenuClick for screens that need it)
 * - Maintains React design consistency
 *
 * Screen Titles:
 * - OVERVIEW → "Project Details: <Project Name>"
 * - CHATS → "Chats"
 * - TASKS → "Tasks"
 * - MEMBERS → "Members"
 * - ACTIVITY → "Activity"
 *
 * @param selectedTab Current workspace tab
 * @param projectName Project name (null when loading)
 * @param isLoading Whether project is still loading
 * @param onBackClick Back button click handler
 * @param onMenuClick Optional menu button click handler (null = no menu button)
 */
@Suppress("UNUSED")
@Composable
private fun UnifiedWorkspaceTopBar_ARCHIVED(
    selectedTab: WorkspaceTab,
    projectName: String?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null
) {
    // Generate dynamic title based on selected tab
    val title = when {
        isLoading -> "Loading..."
        selectedTab == WorkspaceTab.OVERVIEW && projectName != null -> "Project Details: $projectName"
        selectedTab == WorkspaceTab.CHATS -> "Chats"
        selectedTab == WorkspaceTab.TASKS -> "Tasks"
        selectedTab == WorkspaceTab.MEMBERS -> "Members"
        selectedTab == WorkspaceTab.ACTIVITY -> "Activity"
        else -> projectName ?: "Loading..."
    }

    Surface(
        color = ColorTokens.ReactTheme.card,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = ColorTokens.ReactTheme.border,
                shape = RectangleShape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = IconSet.Navigation.back,
                    contentDescription = "Back",
                    tint = ColorTokens.ReactTheme.foreground,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Dynamic title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // Optional menu button (or spacer for alignment)
            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = IconSet.Action.moreVert,
                        contentDescription = "More options",
                        tint = ColorTokens.ReactTheme.foreground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                // Spacer for visual alignment when no menu button
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

/**
 * Bottom Navigation Bar for Project Workspace
 * Professional Material 3 design with React theme colors
 */
@Composable
private fun ProjectWorkspaceBottomNav(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    projectStats: com.example.kosmos.core.models.ProjectStats?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ColorTokens.ReactTheme.card,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            // Top border separator
            HorizontalDivider(
                thickness = 1.dp,
                color = ColorTokens.ReactTheme.border
            )

            // Navigation Bar with React theme styling (compact width)
            NavigationBar(
                containerColor = ColorTokens.ReactTheme.card,
                contentColor = ColorTokens.ReactTheme.foreground,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp) // Horizontal padding for narrower width
            ) {
                WorkspaceTab.values().forEachIndexed { index, tab ->
                    val selected = selectedTabIndex == index

                    NavigationBarItem(
                        selected = selected,
                        onClick = { onTabSelected(index) },
                        icon = {
                            // Get count for badge
                            val count = when (tab) {
                                WorkspaceTab.CHATS -> projectStats?.chatCount ?: 0
                                WorkspaceTab.TASKS -> projectStats?.taskCount ?: 0
                                WorkspaceTab.MEMBERS -> projectStats?.memberCount ?: 0
                                else -> 0
                            }

                            if (count > 0) {
                                // Icon with badge
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = ColorTokens.ReactTheme.primary,
                                            contentColor = ColorTokens.ReactTheme.primaryForeground
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = when (tab) {
                                            WorkspaceTab.OVERVIEW -> IconSet.Navigation.home
                                            WorkspaceTab.CHATS -> IconSet.Message.chat
                                            WorkspaceTab.TASKS -> IconSet.Task.task
                                            WorkspaceTab.MEMBERS -> IconSet.User.group
                                            WorkspaceTab.ACTIVITY -> IconSet.Time.history
                                        },
                                        contentDescription = tab.displayName,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                // Icon without badge
                                Icon(
                                    imageVector = when (tab) {
                                        WorkspaceTab.OVERVIEW -> IconSet.Navigation.home
                                        WorkspaceTab.CHATS -> IconSet.Message.chat
                                        WorkspaceTab.TASKS -> IconSet.Task.task
                                        WorkspaceTab.MEMBERS -> IconSet.User.group
                                        WorkspaceTab.ACTIVITY -> IconSet.Time.history
                                    },
                                    contentDescription = tab.displayName,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorTokens.ReactTheme.primary,
                            selectedTextColor = ColorTokens.ReactTheme.primary,
                            unselectedIconColor = ColorTokens.ReactTheme.mutedForeground,
                            unselectedTextColor = ColorTokens.ReactTheme.mutedForeground,
                            indicatorColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
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
                contentDescription = "",
                modifier = Modifier.size(64.dp),
                tint = ColorTokens.ReactTheme.primary
            )
            Text(
                "Activity Feed",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Coming soon: Recent updates, changes, and notifications",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
    }
}

/**
 * Workspace Tab Enum
 * Defines the 5 tabs available in the project workspace
 */
enum class WorkspaceTab(val displayName: String) {
    OVERVIEW("Overview"),
    CHATS("Chats"),
    TASKS("Tasks"),
    MEMBERS("Members"),
    ACTIVITY("Activity")
}
