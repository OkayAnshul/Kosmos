package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.tasks.presentation.TaskDetailViewModel

/**
 * Wrapper for TaskDetailScreen with Hilt dependency injection
 *
 * Responsibilities:
 * - Inject TaskDetailViewModel via Hilt
 * - Collect UI state from ViewModel
 * - Load task when screen opens
 * - Handle delete success navigation
 * - Wire offline detection (TODO: Add NetworkMonitor)
 */
@Composable
fun TaskDetailScreenWrapper(
    taskId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load task when screen opens
    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    // Navigate back when task is deleted
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            onNavigateBack()
        }
    }

    // TODO: Wire network monitor for offline detection
    val isOffline = false // Placeholder

    TaskDetailScreen(
        task = uiState.task,
        assignedUser = uiState.assignedUser,
        currentUserId = uiState.currentUserId,
        availableUsers = uiState.availableUsers,
        subtasks = uiState.subtasks,
        isLoading = uiState.isLoading,
        isUpdating = uiState.isUpdating,
        isOffline = isOffline,
        error = uiState.error,
        onStatusChange = viewModel::requestStatusChange,
        onPriorityChange = viewModel::requestPriorityChange,
        onDescriptionChange = viewModel::requestDescriptionChange,
        onAssignUser = { user -> viewModel.requestAssignmentChange(user.id, user.username) },
        onTagsUpdated = viewModel::requestTagsChange,
        onEstimatedHoursChange = viewModel::requestEstimatedHoursChange,
        onActualHoursChange = viewModel::requestActualHoursChange,
        onToggleSubtask = viewModel::toggleSubtask,
        onAddComment = viewModel::addComment,
        onDeleteTask = viewModel::deleteTask,
        onNavigateBack = onNavigateBack,
        // Commit dialog parameters
        showCommitDialog = uiState.showCommitDialog,
        pendingChanges = uiState.pendingChanges,
        onCommitConfirm = viewModel::confirmWithCommitMessage,
        onCommitDismiss = viewModel::dismissCommitDialog,
        onDontAskAgain = viewModel::setDontAskCommitMessage,
        modifier = modifier
    )
}
