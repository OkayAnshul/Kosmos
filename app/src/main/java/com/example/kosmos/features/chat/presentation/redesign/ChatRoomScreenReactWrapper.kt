package com.example.kosmos.features.chat.presentation.redesign

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.MessageType
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.features.chat.presentation.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Wrapper for ChatRoomScreenReact that connects to the backend via ChatViewModel.
 *
 * H3 FIX: Replaced ChatRoomDataViewModel with ChatViewModel to get:
 * - Real-time subscriptions (C4)
 * - Reply support (H4)
 * - Reactions, edit/delete, search, pagination
 * - Task link data (H2 — partially; uses message content for now)
 */
@Composable
fun ChatRoomScreenReactWrapper(
    chatRoomId: String,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    projectRepository: ProjectRepository = hiltViewModel<ProjectRepoHolder>().projectRepository
) {
    // Load chat via ChatViewModel (starts realtime, loads messages, loads chat room)
    LaunchedEffect(chatRoomId) {
        chatViewModel.loadChat(chatRoomId)
    }

    // Mark messages as read when viewing
    LaunchedEffect(chatRoomId) {
        chatViewModel.markMessagesAsRead()
    }

    // Cleanup realtime on dispose
    DisposableEffect(chatRoomId) {
        onDispose {
            // ChatViewModel handles cleanup via onCleared, but we can also explicitly stop
        }
    }

    val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()

    // Load project details for header
    val project by if (chatUiState.chatRoom != null) {
        projectRepository.getProjectFlow(chatUiState.chatRoom!!.projectId)
            .collectAsStateWithLifecycle(initialValue = null)
    } else {
        remember { mutableStateOf(null) }
    }

    val currentUser = chatViewModel.currentUser

    // Map Message → MessageData
    val messageDataList = chatUiState.messages.map { message ->
        MessageData(
            id = message.id,
            type = when (message.type) {
                MessageType.TEXT -> MessageTypeReact.USER
                MessageType.VOICE -> MessageTypeReact.USER
                MessageType.IMAGE -> MessageTypeReact.USER
                MessageType.FILE -> MessageTypeReact.USER
                MessageType.SYSTEM -> MessageTypeReact.SYSTEM
                MessageType.TASK_CREATED -> MessageTypeReact.TASK
            },
            sender = if (message.type != MessageType.SYSTEM) {
                MessageSender(
                    name = message.senderName,
                    avatar = message.senderName.firstOrNull()?.uppercase() ?: "?"
                )
            } else null,
            content = message.content,
            timestamp = formatTimestamp(message.timestamp),
            isOwn = currentUser?.id == message.senderId,
            taskLink = if (message.type == MessageType.TASK_CREATED && message.taskIds.isNotEmpty()) {
                TaskLink(
                    title = message.content,
                    status = "In Progress"
                )
            } else null,
            reactions = message.reactions.entries.groupBy { it.value }.map { (emoji, entries) ->
                ReactionData(emoji = emoji, count = entries.size)
            },
            readStatus = when {
                message.readBy.isNotEmpty() -> ReadStatus.READ
                else -> ReadStatus.SENT
            }
        )
    }

    // Map reply state from ChatViewModel to ReplyTo for the React screen
    val replyTo = chatUiState.replyingToMessage?.let { msg ->
        ReplyTo(
            sender = msg.senderName,
            message = msg.content
        )
    }

    // Handle send message via ChatViewModel (supports reply automatically)
    val handleSendMessage: (String) -> Unit = { content ->
        if (content.isNotBlank()) {
            chatViewModel.sendMessage(content)
        }
    }

    ChatRoomScreenReact(
        chatId = chatRoomId,
        chatName = chatUiState.chatRoom?.name ?: "Loading...",
        projectName = project?.name ?: "",
        memberCount = chatUiState.chatRoom?.participantIds?.size ?: 0,
        messages = messageDataList,
        replyTo = replyTo,
        onReplyToChange = { newReplyTo ->
            if (newReplyTo == null) {
                chatViewModel.cancelReply()
            }
        },
        onSendMessage = handleSendMessage,
        onBack = onBack,
        onReaction = { messageId, emoji ->
            chatViewModel.toggleReaction(messageId, emoji)
        },
        onEditMessage = { messageId, newContent ->
            val msg = chatUiState.messages.find { it.id == messageId }
            if (msg != null) {
                chatViewModel.showMessageContextMenu(msg)
                chatViewModel.editMessage(newContent)
            }
        },
        onReplyToMessage = { messageId ->
            val msg = chatUiState.messages.find { it.id == messageId }
            if (msg != null) {
                chatViewModel.showReplyTo(msg)
            }
        }
    )
}

/**
 * Helper ViewModel to inject ProjectRepository
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class ProjectRepoHolder @javax.inject.Inject constructor(
    val projectRepository: ProjectRepository
) : androidx.lifecycle.ViewModel()

/**
 * Format timestamp to display format
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "just now"
        diff < 86400_000 -> {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
        isSameDay(timestamp, now) -> "Today"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
