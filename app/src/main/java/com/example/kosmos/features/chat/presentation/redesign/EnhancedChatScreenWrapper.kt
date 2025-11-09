package com.example.kosmos.features.chat.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.chat.presentation.ChatViewModel
import com.example.kosmos.shared.ui.mappers.ChatDataMapper
import com.example.kosmos.shared.ui.mappers.ChatDataMapper.toChatMessage
import java.util.concurrent.TimeUnit

/**
 * Wrapper composable that connects EnhancedChatScreen to ChatViewModel
 * Handles data mapping and state transformations
 */
@Composable
fun EnhancedChatScreenWrapper(
    chatRoomId: String,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Load chat when screen opens
    LaunchedEffect(chatRoomId) {
        viewModel.loadChat(chatRoomId)
        viewModel.markMessagesAsRead()
    }

    // Mark messages as read when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            viewModel.markMessagesAsRead()
        }
    }

    // Convert domain models to UI models
    val currentUserId = viewModel.currentUser?.id ?: ""

    // Build user map for reaction display
    val userMap = remember(uiState.messages) {
        uiState.messages
            .map { it.senderId to it.senderName }
            .toMap()
    }

    // Build message map for reply lookups
    val messageMap = remember(uiState.messages) {
        uiState.messages.associateBy { it.id }
    }

    // Convert messages to ChatMessage format
    val chatMessages = remember(uiState.messages, currentUserId, userMap, messageMap) {
        uiState.messages.map { message ->
            message.toChatMessage(currentUserId, userMap, messageMap)
        }
    }

    // Convert replying message to ChatMessage if present
    val replyingToMessage = remember(uiState.replyingToMessage, currentUserId, userMap, messageMap) {
        uiState.replyingToMessage?.toChatMessage(currentUserId, userMap, messageMap)
    }

    // Group messages by date for dividers
    val groupedMessages = remember(chatMessages) {
        groupMessagesByDate(chatMessages)
    }

    // Extract typing users (convert Set<String> to List<String>)
    val typingUsersList = remember(uiState.typingUsers) {
        uiState.typingUsers.toList()
    }

    EnhancedChatScreen(
        chatRoomId = chatRoomId,
        chatRoomName = uiState.chatRoom?.name ?: "Loading...",
        messages = chatMessages,
        currentUserId = currentUserId,
        typingUsers = typingUsersList,
        replyingToMessage = replyingToMessage,
        onSendMessage = { text ->
            viewModel.sendMessage(text)
        },
        onEditMessage = { messageId, newText ->
            // Find and set the message, then edit it
            val message = uiState.messages.find { it.id == messageId }
            if (message != null) {
                viewModel.showMessageContextMenu(message)
                viewModel.showEditDialog()
                viewModel.editMessage(newText)
            }
        },
        onDeleteMessage = { messageId ->
            // Find and set the message, then delete it
            val message = uiState.messages.find { it.id == messageId }
            if (message != null) {
                viewModel.showMessageContextMenu(message)
                viewModel.showDeleteDialog()
                viewModel.deleteMessage()
            }
        },
        onReactToMessage = { messageId, emoji ->
            viewModel.toggleReaction(messageId, emoji)
        },
        onReplyToMessage = { chatMessage ->
            // Convert ChatMessage back to Message for ViewModel
            val message = uiState.messages.find { it.id == chatMessage.id }
            if (message != null) {
                viewModel.showReplyTo(message)
            }
        },
        onCancelReply = {
            viewModel.cancelReply()
        },
        onLoadOlderMessages = {
            viewModel.loadOlderMessages()
        },
        onBackClick = onBackClick
    )
}

/**
 * Group messages by date for date dividers
 */
private fun groupMessagesByDate(messages: List<ChatMessage>): Map<String, List<ChatMessage>> {
    return messages.groupBy { it.formattedDate }
}

/**
 * Determine if a message should show sender name
 * (first message in group or different sender)
 */
private fun shouldShowSenderName(
    currentMessage: ChatMessage,
    previousMessage: ChatMessage?,
    currentUserId: String
): Boolean {
    // Don't show for current user's messages
    if (currentMessage.senderId == currentUserId) return false

    // Show if first message
    if (previousMessage == null) return true

    // Show if different sender
    if (currentMessage.senderId != previousMessage.senderId) return true

    // Show if time gap > 5 minutes
    val timeDiff = currentMessage.timestamp - previousMessage.timestamp
    if (timeDiff > TimeUnit.MINUTES.toMillis(5)) return true

    return false
}

/**
 * Determine grouping flags for message bubble styling
 */
private data class MessageGrouping(
    val isFirstInGroup: Boolean,
    val isLastInGroup: Boolean
)

private fun getMessageGrouping(
    index: Int,
    messages: List<ChatMessage>,
    currentUserId: String
): MessageGrouping {
    val currentMessage = messages[index]
    val previousMessage = messages.getOrNull(index - 1)
    val nextMessage = messages.getOrNull(index + 1)

    val isFirstInGroup = previousMessage == null ||
            previousMessage.senderId != currentMessage.senderId ||
            (currentMessage.timestamp - previousMessage.timestamp) > TimeUnit.MINUTES.toMillis(5)

    val isLastInGroup = nextMessage == null ||
            nextMessage.senderId != currentMessage.senderId ||
            (nextMessage.timestamp - currentMessage.timestamp) > TimeUnit.MINUTES.toMillis(5)

    return MessageGrouping(isFirstInGroup, isLastInGroup)
}
