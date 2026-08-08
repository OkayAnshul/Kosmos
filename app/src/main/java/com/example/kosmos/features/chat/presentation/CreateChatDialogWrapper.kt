package com.example.kosmos.features.chat.presentation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Wrapper for CreateChatDialog that connects to backend
 *
 * This wrapper:
 * - Loads project members via ChatListViewModel
 * - Creates chat via ChatListViewModel
 * - Handles success/error states
 * - Dismisses on success (parent navigation handled outside)
 */
@Composable
fun CreateChatDialogWrapper(
    projectId: String,
    onDismiss: () -> Unit,
    onChatCreated: (String) -> Unit, // Returns empty string (no chatRoomId available from ViewModel)
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load project members when dialog opens
    LaunchedEffect(projectId) {
        viewModel.loadProjectMembers(projectId)
    }

    CreateChatScreen(
        projectMembers = uiState.projectMembers,
        isLoading = uiState.isCreatingChat,
        error = uiState.error,
        onDismiss = onDismiss,
        onCreate = { chatName, selectedUserIds ->
            // For direct chats, use recipient's display name instead of "Direct Chat"
            val resolvedName = chatName ?: run {
                if (selectedUserIds.size == 1) {
                    uiState.projectMembers
                        .find { it.id == selectedUserIds.first() }
                        ?.let { it.displayName.takeIf { n -> n.isNotBlank() } ?: it.username }
                        ?: "Direct Chat"
                } else "Group Chat"
            }
            viewModel.createNewChatRoom(
                name = resolvedName,
                description = "",
                selectedUserIds = selectedUserIds,
                projectId = projectId
            )
        }
    )

    // Handle chat creation success
    LaunchedEffect(uiState.lastCreatedChatRoomId) {
        uiState.lastCreatedChatRoomId?.let { chatRoomId ->
            // Clear the state first to prevent re-triggering
            viewModel.clearLastCreatedChatRoom()
            // Notify caller with the actual chat room ID
            onChatCreated(chatRoomId)
        }
    }

    // Clear errors when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearError()
        }
    }
}
