package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.features.auth.presentation.AuthViewModel
import com.example.kosmos.features.project.presentation.ProjectViewModel
import com.example.kosmos.features.tasks.presentation.TaskViewModel
import com.example.kosmos.shared.utils.NetworkMonitor
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

/**
 * Entry point for accessing NetworkMonitor
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TaskNetworkMonitorEntryPoint {
    fun networkMonitor(): NetworkMonitor
}

/**
 * Wrapper for TaskBoardScreen with Hilt dependency injection
 *
 * Responsibilities:
 * - Inject TaskViewModel, ProjectViewModel, AuthViewModel via Hilt
 * - Collect UI state from ViewModels
 * - Provide real project and user data
 * - Monitor network status for offline indicator
 * - Handle error display via SnackBar
 * - Delegate user actions to ViewModel
 */
@Composable
fun TaskBoardScreenWrapper(
    projectId: String,
    chatRoomId: String? = null,
    onTaskClick: (String) -> Unit,
    onEditTask: (String) -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    taskViewModel: TaskViewModel = hiltViewModel(),
    projectViewModel: ProjectViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val networkMonitor = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TaskNetworkMonitorEntryPoint::class.java
        ).networkMonitor()
    }

    val uiState by taskViewModel.uiState.collectAsStateWithLifecycle()
    val projectUiState by projectViewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val isOffline by networkMonitor.isOffline.collectAsState()

    val currentUser = authUiState.currentUser

    // Get project data
    val project = remember(projectId, projectUiState.projects) {
        projectUiState.projects.find { it.id == projectId }
    }

    // Calculate team info
    val teamInfo = remember(project) {
        val memberCount = project?.memberCount ?: 0
        if (memberCount > 0) "$memberCount members" else "No members"
    }

    // Load tasks when screen is first composed
    LaunchedEffect(projectId, chatRoomId) {
        if (chatRoomId != null) {
            taskViewModel.loadTasks(chatRoomId)
        } else {
            taskViewModel.loadTasksForProject(projectId)
        }
    }

    // Error handling
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            taskViewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TaskBoardScreen(
        projectId = projectId,
        projectName = project?.name ?: "Unknown Project",
        teamInfo = teamInfo,
        currentUserDisplayName = currentUser?.displayName ?: "User",
        currentUserPhotoUrl = currentUser?.photoUrl,
        chatRoomId = chatRoomId,
        uiState = uiState,
        isOffline = isOffline,
        onTaskClick = { task -> onTaskClick(task.id) },
        onCreateTask = { taskViewModel.showCreateTaskDialog() },
        onCreateTaskWithStatus = { status ->
            taskViewModel.updateCreateTaskStatus(status)
            taskViewModel.showCreateTaskDialog()
        },
        onSearchQueryChange = { query ->
            taskViewModel.searchTasks(query)
        },
        onFilterChange = { filter ->
            val viewModelFilter = if (filter == TaskFilter.MY_TASKS) {
                com.example.kosmos.features.tasks.presentation.TaskFilter.MY_TASKS
            } else {
                com.example.kosmos.features.tasks.presentation.TaskFilter.ALL
            }
            taskViewModel.filterTasks(viewModelFilter)
        },
            onTaskStatusChange = { task, newStatus ->
                coroutineScope.launch {
                    taskViewModel.updateTaskStatus(task.id, newStatus)
                }
            },
            onEditTask = { task -> onEditTask(task.id) },
            onDeleteTask = { task ->
                coroutineScope.launch {
                    taskViewModel.deleteTask(task.id)
                }
            },
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )

        // Snackbar for error messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Show task creation sheet when requested
        if (uiState.showCreateTaskDialog) {
            QuickTaskCreationSheetWrapper(
                projectId = projectId,
                chatRoomId = chatRoomId,
                onDismiss = { taskViewModel.hideCreateTaskDialog() },
                onCreate = { taskId ->
                    taskViewModel.hideCreateTaskDialog()
                    // Optionally navigate to the created task
                    // onTaskClick(taskId)
                }
            )
        }
    }
}
