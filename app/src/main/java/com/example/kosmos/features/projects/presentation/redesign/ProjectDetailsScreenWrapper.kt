package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.project.presentation.ProjectViewModel
import com.example.kosmos.features.projects.components.EditProjectDialog
import com.example.kosmos.features.tasks.presentation.redesign.QuickTaskCreationSheetWrapper
import com.example.kosmos.shared.ui.mappers.ProjectDataMapper
import com.example.kosmos.shared.ui.mappers.ProjectDataMapper.toProjectItem
import com.example.kosmos.shared.ui.mappers.ProjectDataMapper.toUIProjectMember
import com.example.kosmos.features.projects.components.ProjectActivity
import com.example.kosmos.shared.ui.designsystem.IconSet
import androidx.compose.ui.graphics.Color

/**
 * Wrapper composable that connects ProjectDetailsScreen to ProjectViewModel
 * Handles data mapping and state transformations
 */
@Composable
fun ProjectDetailsScreenWrapper(
    projectId: String,
    onViewAllChats: () -> Unit,
    onViewAllTasks: () -> Unit,
    onViewAllMembers: () -> Unit,
    onCreateChat: () -> Unit,
    onCreateTask: () -> Unit,
    onInviteMember: () -> Unit,
    onEditProject: () -> Unit,
    onChatClick: (String) -> Unit = {},
    onTaskClick: (String) -> Unit = {},
    onBackClick: () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load full user details for project members
    var projectMembersWithUsers by remember { mutableStateOf<List<com.example.kosmos.core.models.User>>(emptyList()) }

    LaunchedEffect(uiState.currentProjectMembers) {
        val users = uiState.currentProjectMembers.mapNotNull { member ->
            viewModel.getUserById(member.userId)
        }
        projectMembersWithUsers = users
    }

    // Track selected tab
    var selectedTab by remember { mutableStateOf(ProjectTab.OVERVIEW) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCreateTaskSheet by remember { mutableStateOf(false) }
    var showCreateChatDialog by remember { mutableStateOf(false) }

    // Load project details and stats
    LaunchedEffect(projectId) {
        viewModel.loadProjectMembers(projectId)
        viewModel.loadProjectStats(projectId)
    }

    // Find the current project
    val currentProject = remember(uiState.projects, projectId) {
        uiState.projects.find { it.id == projectId }
    }

    // Convert project to ProjectItem with real stats
    val projectItem = remember(currentProject, uiState.projectStats) {
        val stats = uiState.projectStats[projectId]
        currentProject?.toProjectItem(
            memberCount = stats?.memberCount ?: uiState.currentProjectMembers.size,
            chatCount = stats?.chatCount ?: 0,
            taskCount = stats?.taskCount ?: 0,
            completedTaskCount = stats?.completedTaskCount ?: 0,
            unreadChatCount = stats?.unreadChatCount ?: 0,
            pendingTaskCount = stats?.pendingTaskCount ?: 0,
            lastActivityTimestamp = stats?.lastActivityTime
        ) ?: com.example.kosmos.features.projects.components.ProjectItem(
            id = projectId,
            name = "Loading...",
            description = null,
            memberCount = 0,
            chatCount = 0,
            taskCount = 0,
            completedTaskCount = 0,
            createdAt = System.currentTimeMillis()
        )
    }

    // Convert project members to UI members
    val members = remember(uiState.currentProjectMembers) {
        uiState.currentProjectMembers.map { member ->
            member.toUIProjectMember(isOnline = false) // TODO: Implement online status
        }
    }

    // For now, show empty lists for Chats and Tasks tabs
    // They will be populated when switching to those tabs
    // Using dedicated navigation routes maintains separation of concerns
    val chatRooms = emptyList<com.example.kosmos.features.chat.presentation.redesign.ChatRoomItem>()
    val tasks = emptyList<com.example.kosmos.features.tasks.components.TaskItem>()

    // TODO: Load actual activity data
    val recentActivity = remember {
        emptyList<ProjectActivity>()
    }

    if (currentProject == null && !uiState.isLoading) {
        // Show error state if project not found
        androidx.compose.material3.Text("Project not found")
        return
    }

    ProjectDetailsScreen(
        project = projectItem,
        members = members,
        recentActivity = recentActivity,
        chatRooms = chatRooms,
        tasks = tasks,
        selectedTab = selectedTab,
        onTabSelected = { tab ->
            // When user taps bottom nav, navigate to dedicated screens
            when (tab) {
                ProjectTab.CHATS -> onViewAllChats()
                ProjectTab.TASKS -> onViewAllTasks()
                ProjectTab.MEMBERS -> onViewAllMembers()
                else -> selectedTab = tab // Switch tab for Overview and Activity
            }
        },
        // Quick action buttons navigate to full-featured screens
        onViewAllChats = onViewAllChats,
        onViewAllTasks = onViewAllTasks,
        onViewAllMembers = onViewAllMembers,
        onCreateChat = { showCreateChatDialog = true },
        onCreateTask = { showCreateTaskSheet = true },
        onInviteMember = onInviteMember,
        onEditProject = { showEditDialog = true },
        onChatClick = onChatClick,
        onTaskClick = onTaskClick,
        onArchiveProject = {
            currentProject?.let {
                if (it.status == com.example.kosmos.core.models.ProjectStatus.ARCHIVED) {
                    viewModel.unarchiveProject(projectId)
                } else {
                    viewModel.archiveProject(projectId)
                }
            }
        },
        onBackClick = onBackClick
    )

    // Edit Project Dialog
    if (showEditDialog && currentProject != null) {
        EditProjectDialog(
            project = currentProject,
            onDismiss = { showEditDialog = false },
            onSave = { name, description, status ->
                viewModel.updateProjectDetails(projectId, name, description, status)
                showEditDialog = false
            },
            isLoading = uiState.isUpdating
        )
    }

    // Create Chat Dialog
    if (showCreateChatDialog && currentProject != null && projectMembersWithUsers.isNotEmpty()) {
        com.example.kosmos.features.chat.presentation.CreateChatDialog(
            projectMembers = projectMembersWithUsers,
            onDismiss = { showCreateChatDialog = false },
            onCreate = { chatName, userIds ->
                // Create chat room with selected users
                // For now, just dismiss - actual creation handled by onCreateChat callback
                onCreateChat()
                showCreateChatDialog = false
            }
        )
    }

    // Create Task Sheet - Tasks can be created at project level without chat room
    if (showCreateTaskSheet) {
        QuickTaskCreationSheetWrapper(
            chatRoomId = null, // Project-level task, no chat room required
            projectId = projectId,
            onDismiss = { showCreateTaskSheet = false },
            onCreate = { taskId ->
                // Task created successfully
                onTaskClick(taskId)
                showCreateTaskSheet = false
            }
        )
    }
}
