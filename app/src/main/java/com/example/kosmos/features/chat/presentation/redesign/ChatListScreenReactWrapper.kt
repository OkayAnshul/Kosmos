package com.example.kosmos.features.chat.presentation.redesign

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ChatRepository
import com.example.kosmos.data.repository.ProjectRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Wrapper for ChatListScreenReact that connects to the backend.
 *
 * This wrapper:
 * - Loads chat rooms for the current user in a specific project
 * - Loads project name for display
 * - Manages search query and filter state
 * - Maps domain models to UI models (ChatRoom → ChatListItemData)
 * - Maintains the exact React design UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreenReactWrapper(
    projectId: String,
    onChatClick: (String) -> Unit,
    onCreateChat: () -> Unit,
    chatRepository: ChatRepository = hiltViewModel<ChatListDataViewModel>().chatRepository,
    projectRepository: ProjectRepository = hiltViewModel<ChatListDataViewModel>().projectRepository,
    authRepository: AuthRepository = hiltViewModel<ChatListDataViewModel>().authRepository
) {
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FilterType.ALL) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Get current user
    val currentUser = authRepository.getCurrentUser()

    // Load chat rooms for this project
    val chatRooms by if (currentUser != null) {
        chatRepository.getChatRoomsForProject(currentUser.id, projectId)
            .collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    // Load the project to get its name
    val project by projectRepository.getProjectFlow(projectId)
        .collectAsStateWithLifecycle(initialValue = null)

    val projectName = project?.name ?: "Loading..."

    // Map ChatRoom → ChatListItemData with unread counts
    // Collect unread counts for all chat rooms
    val chatListDataWithUnread = chatRooms.map { chatRoom ->
        val unreadCount = if (currentUser != null) {
            chatRepository.getUnreadCountFlow(chatRoom.id, currentUser.id)
                .collectAsStateWithLifecycle(initialValue = 0).value
        } else {
            0
        }

        ChatListItemData(
            id = chatRoom.id,
            name = chatRoom.name,
            lastMessage = formatLastMessage(chatRoom.lastMessage),
            timestamp = formatTimestamp(chatRoom.lastMessageTimestamp),
            unreadCount = unreadCount,
            isPinned = chatRoom.isPinned,
            projectName = projectName
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                chatRepository.syncProjectChatRooms(projectId)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        ChatListScreenReact(
            chats = chatListDataWithUnread,
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            filter = filter,
            onFilterChange = { filter = it },
            onChatClick = onChatClick,
            onCreateChat = onCreateChat
        )
    }
}

/**
 * Helper ViewModel to inject repositories
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class ChatListDataViewModel @Inject constructor(
    val chatRepository: ChatRepository,
    val projectRepository: ProjectRepository,
    val authRepository: AuthRepository
) : androidx.lifecycle.ViewModel()

/**
 * Format last message for display (truncate if needed)
 */
private fun formatLastMessage(message: String): String {
    return if (message.isEmpty()) {
        "No messages yet"
    } else {
        message
    }
}

/**
 * Format timestamp to relative time (e.g., "2m ago", "1h ago", "2d ago")
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "just now" // < 1 minute
        diff < 3600_000 -> "${diff / 60_000}m ago" // < 1 hour
        diff < 86400_000 -> "${diff / 3600_000}h ago" // < 1 day
        diff < 604800_000 -> "${diff / 86400_000}d ago" // < 1 week
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
