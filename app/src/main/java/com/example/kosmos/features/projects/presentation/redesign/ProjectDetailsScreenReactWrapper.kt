package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectStatus
import com.example.kosmos.data.repository.TaskRepository
import kotlinx.serialization.json.Json
import com.example.kosmos.features.project.presentation.ProjectViewModel
import com.example.kosmos.features.projects.components.EditProjectDialog
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Wrapper for ProjectDetailsScreenReact that connects to the backend.
 *
 * This wrapper:
 * - Injects ProjectViewModel via Hilt
 * - Loads project details and stats from the database
 * - Loads recent activity for the project
 * - Maps domain models to UI models
 * - Handles edit/delete project dialog
 * - Wires up navigation callbacks for clickable stat cards
 * - Maintains the exact React design UI
 *
 * Navigation:
 * - Stats cards navigate to respective tabs (Chats, Tasks, Members)
 * - "View All" in Recent Activity navigates to Activity tab
 * - Tab switching is handled by parent ProjectWorkspaceScreen
 */
@Composable
fun ProjectDetailsScreenReactWrapper(
    projectId: String,
    onBack: () -> Unit,
    onNewTask: () -> Unit,  // Navigate to task creation
    onNewChat: () -> Unit,  // Navigate to chat creation
    onViewChats: () -> Unit = {},  // Navigate to Chats tab
    onViewTasks: () -> Unit = {},  // Navigate to Tasks tab
    onViewMembers: () -> Unit,  // Navigate to Members tab
    onViewActivity: () -> Unit = {},  // Navigate to Activity tab
    onEditProject: ((String) -> Unit)? = null,  // Navigate to full-screen edit
    onShowMoreMenu: () -> Unit = {},  // Show more menu (edit/delete) - optional for now
    viewModel: ProjectViewModel = hiltViewModel(),
    taskRepository: TaskRepository = hiltViewModel<ProjectDetailsActivityViewModel>().taskRepository
) {
    // State for edit dialog
    var showEditDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Get project from ViewModel state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val project = uiState.projects.find { it.id == projectId }
    val stats = uiState.projectStats[projectId]

    // Handle edit project
    val handleSaveProject: (String, String, ProjectStatus) -> Unit = { name, description, status ->
        coroutineScope.launch {
            viewModel.updateProjectDetails(
                projectId = projectId,
                name = name,
                description = description,
                status = status
            )
            showEditDialog = false

            // Check for error from ViewModel state
            if (uiState.error != null) {
                snackbarHostState.showSnackbar(
                    message = uiState.error ?: "Failed to update project",
                    duration = SnackbarDuration.Short
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = "Project updated successfully",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Handle delete project
    val handleDeleteProject: () -> Unit = {
        coroutineScope.launch {
            viewModel.deleteProject(projectId)
            showEditDialog = false
            snackbarHostState.showSnackbar(
                message = "Project deleted",
                duration = SnackbarDuration.Short
            )
            // Navigate back after deletion
            onBack()
        }
    }

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

    // Load project stats when projectId changes
    LaunchedEffect(projectId) {
        viewModel.loadProjectStats(projectId)
    }

    // Load recent activities for the project (limit to 5 most recent)
    val activities by taskRepository.getActivityForProjectFlow(projectId)
        .map { activityList ->
            activityList
                .sortedByDescending { it.timestamp }
                .take(5)
                .map { activity ->
                    ActivityItem(
                        user = activity.actorName,
                        action = activity.commitMessage ?: activity.autoDescription,
                        time = formatActivityTime(activity.timestamp)
                    )
                }
        }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Map domain Project model to ProjectData UI model
    val projectData = if (project != null) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val parseTags: (String?) -> List<String> = { raw ->
            raw?.let {
                try { Json.decodeFromString<List<String>>(it) }
                catch (_: Exception) { it.split(",").map { t -> t.trim() }.filter { t -> t.isNotEmpty() } }
            } ?: emptyList()
        }
        ProjectData(
            id = project.id,
            name = project.name,
            description = project.description,
            status = if (project.status == ProjectStatus.ARCHIVED) "Archived" else "Active",
            memberCount = stats?.memberCount ?: 0,
            chatCount = stats?.chatCount ?: 0,
            taskCount = stats?.taskCount ?: 0,
            completedTasks = stats?.completedTaskCount ?: 0,
            lastActivity = formatLastActivity(project.updatedAt ?: project.createdAt),
            category = project.category.getDisplayName(),
            deadline = project.deadline?.let { dateFormat.format(Date(it)) },
            visibility = project.visibility.name.lowercase().replaceFirstChar { it.uppercase() },
            createdAt = dateFormat.format(Date(project.createdAt)),
            githubUrl = project.githubUrl,
            websiteUrl = project.websiteUrl,
            projectMotive = project.projectMotive,
            techStack = project.techStack?.let { parseTags(it).joinToString(", ") } ?: project.techStack,
            businessModel = project.businessModel,
            targetAudience = project.targetAudience,
            tags = parseTags(project.tags),
            openSourceLicense = project.openSourceLicense,
            industryTags = parseTags(project.industryTags)
        )
    } else {
        // Fallback if project not found (shouldn't happen, but safe handling)
        ProjectData(
            id = projectId,
            name = "Loading...",
            description = "",
            status = "Active",
            memberCount = 0,
            chatCount = 0,
            taskCount = 0,
            completedTasks = 0,
            lastActivity = "Just now"
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ProjectDetailsScreenReact(
            projectId = projectId,
            project = projectData,
            activities = activities,
            onBack = onBack,
            onNewTask = onNewTask,
            onNewChat = onNewChat,
            onViewChats = onViewChats,  // Navigate to Chats tab
            onViewTasks = onViewTasks,  // Navigate to Tasks tab
            onViewMembers = onViewMembers,  // Navigate to Members tab
            onViewActivity = onViewActivity,  // Navigate to Activity tab
            onMoreMenu = {
                if (onEditProject != null) {
                    onEditProject(projectId)
                } else {
                    showEditDialog = true
                }
            }
        )

        // Snackbar for error/success messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Edit/Delete Project Dialog
        if (showEditDialog && project != null) {
            EditProjectDialog(
                project = project,
                onDismiss = { showEditDialog = false },
                onSave = handleSaveProject,
                onDelete = handleDeleteProject,
                isLoading = uiState.isUpdating
            )
        }
    }
}

/**
 * Helper ViewModel to inject TaskRepository
 * (Since we can't inject repositories directly in Composables)
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class ProjectDetailsActivityViewModel @Inject constructor(
    val taskRepository: TaskRepository
) : androidx.lifecycle.ViewModel()

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

/**
 * Format activity timestamp to relative time string (e.g., "2 hours ago")
 */
private fun formatActivityTime(timestamp: Long): String {
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
