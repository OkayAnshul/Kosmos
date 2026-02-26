package com.example.kosmos.features.users.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.users.presentation.UserProfileViewModel

/**
 * Wrapper for UserProfileScreen with Hilt dependency injection
 *
 * Responsibilities:
 * - Inject UserProfileViewModel via Hilt
 * - Collect UI state from ViewModel
 * - Handle chat creation and navigation
 * - Delegate user actions to ViewModel
 */
@Composable
fun UserProfileScreenWrapper(
    userId: String,
    projectId: String,
    onNavigateBack: () -> Unit,
    onStartChat: (String, String) -> Unit, // Navigate to chat with (userId, chatRoomId)
    modifier: Modifier = Modifier,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load user when screen opens
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    // Navigate to chat when created
    LaunchedEffect(uiState.createdChatRoomId) {
        uiState.createdChatRoomId?.let { chatRoomId ->
            onStartChat(userId, chatRoomId)
        }
    }

    // Determine if this is the current user's own profile
    val isCurrentUser = remember(uiState.user, userId) {
        uiState.user?.id == userId
    }

    UserProfileScreen(
        user = uiState.user,
        isCurrentUser = isCurrentUser,
        sharedProjectCount = uiState.sharedProjectCount,
        onTimeRate = uiState.onTimeRate,
        onStartChat = {
            viewModel.createOrGetDirectChat(projectId, userId)
        },
        onNavigateBack = onNavigateBack,
        isLoading = uiState.isLoading,
        error = uiState.error,
        onRetry = { viewModel.loadUser(userId) },
        modifier = modifier
    )
}
