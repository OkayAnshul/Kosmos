package com.example.kosmos.features.chat.presentation.redesign

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.kosmos.features.chat.components.*
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.layouts.ScreenScaffoldStandard
import kotlinx.coroutines.launch

/**
 * Enhanced Chat Screen - Redesigned
 *
 * Complete chat interface with power user features:
 * - Message grouping (same sender within 5 min)
 * - Inline reactions
 * - Read receipts
 * - Typing indicators
 * - Sticky date headers
 * - Jump to bottom FAB (when scrolled up)
 * - Message context menu (long-press)
 * - Quick react (double-tap)
 * - Pull-to-refresh
 *
 * Performance:
 * - Efficient message grouping
 * - Virtual scrolling
 * - Optimistic UI updates
 */

/**
 * Enhanced Chat Screen
 *
 * @param chatRoomId Chat room ID
 * @param chatRoomName Chat room name
 * @param messages List of messages
 * @param currentUserId Current user ID
 * @param typingUsers List of users currently typing
 * @param onBackClick Back navigation handler
 * @param onSendMessage Send message handler
 * @param onEditMessage Edit message handler
 * @param onDeleteMessage Delete message handler
 * @param onReactToMessage React to message handler
 * @param onMessageClick Message click handler
 * @param onLoadOlderMessages Load more messages handler
 * @param onNavigateToTaskBoard Navigate to task board handler
 * @param isLoading Whether messages are loading
 * @param isLoadingMore Whether loading more messages
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedChatScreen(
    chatRoomId: String,
    chatRoomName: String,
    messages: List<ChatMessage>,
    currentUserId: String,
    typingUsers: List<String> = emptyList(),
    replyingToMessage: ChatMessage? = null,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onEditMessage: (String, String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onReactToMessage: (String, String) -> Unit,
    onReplyToMessage: (ChatMessage) -> Unit = {},
    onCancelReply: () -> Unit = {},
    onMessageClick: (ChatMessage) -> Unit = {},
    onLoadOlderMessages: () -> Unit = {},
    onNavigateToTaskBoard: () -> Unit = {},
    isLoading: Boolean = false,
    isLoadingMore: Boolean = false
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Message input state
    var messageText by remember { mutableStateOf(TextFieldValue("")) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }

    // Context menu state
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Snackbar for feedback
    val snackbarHostState = remember { SnackbarHostState() }

    // Show jump to bottom FAB when scrolled up
    val showJumpToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 5
        }
    }

    // Group messages
    val groupedMessages = remember(messages) {
        groupMessages(messages, currentUserId)
    }

    // Detect when near top for pagination
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex < 3 && !isLoadingMore) {
            onLoadOlderMessages()
        }
    }

    ScreenScaffoldStandard(
        title = chatRoomName,
        onNavigationClick = onBackClick,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButtonStandard(
                icon = IconSet.Action.search,
                onClick = { /* TODO: Search in chat */ },
                contentDescription = "Search"
            )
            IconButtonStandard(
                icon = IconSet.Task.board,
                onClick = onNavigateToTaskBoard,
                contentDescription = "Task Board"
            )
            IconButtonStandard(
                icon = IconSet.Action.moreVert,
                onClick = { /* TODO: Chat settings */ },
                contentDescription = "More"
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                LoadingIndicator(
                    message = "Loading messages...",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Messages list
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            reverseLayout = true, // Latest messages at bottom
                            contentPadding = PaddingValues(vertical = Tokens.Spacing.sm)
                        ) {
                            // Typing indicator at bottom
                            if (typingUsers.isNotEmpty()) {
                                item {
                                    TypingIndicator(typingUsers = typingUsers)
                                }
                            }

                            // Messages
                            items(
                                items = groupedMessages,
                                key = { it.message.id }
                            ) { groupedMessage ->
                                EnhancedMessageBubble(
                                    text = groupedMessage.message.content,
                                    isSentByCurrentUser = groupedMessage.message.senderId == currentUserId,
                                    timestamp = groupedMessage.message.formattedTime,
                                    senderName = if (groupedMessage.message.senderId != currentUserId)
                                        groupedMessage.message.senderName
                                    else null,
                                    isFirstInGroup = groupedMessage.isFirstInGroup,
                                    isLastInGroup = groupedMessage.isLastInGroup,
                                    isEdited = groupedMessage.message.isEdited,
                                    reactions = groupedMessage.message.reactions.map {
                                        MessageReaction(
                                            emoji = it.emoji,
                                            count = it.count,
                                            reactedByCurrentUser = it.userIds.contains(currentUserId)
                                        )
                                    },
                                    readBy = groupedMessage.message.readBy.size,
                                    totalRecipients = 1, // TODO: Get from chat room
                                    replyToSenderName = groupedMessage.message.replyToSenderName,
                                    onMessageClick = {
                                        onMessageClick(groupedMessage.message)
                                    },
                                    onQuickReact = {
                                        // Double-tap: quick react with last used emoji
                                        onReactToMessage(groupedMessage.message.id, "👍")
                                    },
                                    onShowMenu = {
                                        selectedMessage = groupedMessage.message
                                        showContextMenu = true
                                    },
                                    onReactionClick = { emoji ->
                                        onReactToMessage(groupedMessage.message.id, emoji)
                                    },
                                    onReplyClick = {
                                        // TODO: Scroll to original message
                                    }
                                )

                                // Date divider (show for first message of the day)
                                if (groupedMessage.showDateDivider) {
                                    DateDivider(date = groupedMessage.message.formattedDate)
                                }
                            }

                            // Loading more indicator at top
                            if (isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Tokens.Spacing.md),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(Tokens.Size.progressMedium)
                                        )
                                    }
                                }
                            }
                        }

                        // Jump to bottom FAB
                        if (showJumpToBottom) {
                            JumpToBottomFAB(
                                unreadCount = 0, // TODO: Calculate unread
                                onClick = {
                                    scope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(Tokens.Spacing.md)
                            )
                        }
                    }

                    // Reply preview bar (if replying)
                    if (replyingToMessage != null) {
                        ReplyPreviewBar(
                            replyToSenderName = replyingToMessage.senderName,
                            replyToContent = replyingToMessage.content,
                            onCancelReply = onCancelReply
                        )
                    }

                    // Message input
                    MessageInput(
                        text = messageText,
                        onTextChange = { messageText = it },
                        onSendClick = {
                            if (messageText.text.isNotBlank()) {
                                editingMessageId?.let { msgId ->
                                    onEditMessage(msgId, messageText.text)
                                    editingMessageId = null
                                } ?: run {
                                    onSendMessage(messageText.text)
                                }
                                messageText = TextFieldValue("")
                            }
                        },
                        isEditing = editingMessageId != null,
                        onCancelEdit = {
                            editingMessageId = null
                            messageText = TextFieldValue("")
                        }
                    )
                }
            }

            // Context Menu Bottom Sheet
            if (showContextMenu && selectedMessage != null) {
                selectedMessage?.let { message ->
                    MessageContextMenuBottomSheet(
                        message = message,
                        isSentByCurrentUser = message.senderId == currentUserId,
                    onDismiss = {
                        showContextMenu = false
                        selectedMessage = null
                    },
                    onReact = {
                        showContextMenu = false
                        showReactionPicker = true
                    },
                    onEdit = {
                        showContextMenu = false
                        editingMessageId = message.id
                        messageText = TextFieldValue(message.content)
                    },
                    onDelete = {
                        showContextMenu = false
                        showDeleteDialog = true
                    },
                    onCopy = {
                        message?.let {
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Message", it.content)
                            clipboardManager.setPrimaryClip(clip)

                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Message copied to clipboard",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                        showContextMenu = false
                    },
                    onReply = {
                        message?.let { onReplyToMessage(it) }
                        showContextMenu = false
                    }
                )
                }
            }

            // Reaction Picker
            if (showReactionPicker && selectedMessage != null) {
                selectedMessage?.let { message ->
                    ReactionPickerBottomSheet(
                        onDismiss = {
                            showReactionPicker = false
                            selectedMessage = null
                        },
                        onReactionSelected = { emoji ->
                            onReactToMessage(message.id, emoji)
                            showReactionPicker = false
                            selectedMessage = null
                        }
                    )
                }
            }

            // Delete Confirmation
            if (showDeleteDialog && selectedMessage != null) {
                selectedMessage?.let { message ->
                    ConfirmationDialog(
                        title = "Delete Message",
                        message = "Are you sure you want to delete this message? This cannot be undone.",
                        confirmText = "Delete",
                        dismissText = "Cancel",
                        onConfirm = {
                            onDeleteMessage(message.id)
                            showDeleteDialog = false
                            selectedMessage = null
                        },
                    onDismiss = {
                        showDeleteDialog = false
                        selectedMessage = null
                    },
                    isDestructive = true,
                    icon = IconSet.Action.delete
                )
                }
            }
        }
    }
}

/**
 * Message Input Component
 */
@Composable
private fun MessageInput(
    text: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    isEditing: Boolean = false,
    onCancelEdit: () -> Unit = {}
) {
    Column {
        // Edit mode indicator
        if (isEditing) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = IconSet.Action.edit,
                            contentDescription = null,
                            modifier = Modifier.size(Tokens.Size.iconSmall),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Editing message",
                            style = TypographyTokens.Custom.caption,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    IconButtonStandard(
                        icon = IconSet.Navigation.close,
                        onClick = onCancelEdit,
                        contentDescription = "Cancel editing"
                    )
                }
            }
        }

        HorizontalDivider()

        // Input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.sm),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message...") },
                maxLines = 3,
                shape = MaterialTheme.shapes.large
            )

            FloatingActionButton(
                onClick = onSendClick,
                modifier = Modifier.size(Tokens.TouchTarget.fab),
                containerColor = if (text.text.isNotBlank())
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = IconSet.Message.send,
                    contentDescription = "Send"
                )
            }
        }
    }
}

/**
 * Message Context Menu Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageContextMenuBottomSheet(
    message: ChatMessage,
    isSentByCurrentUser: Boolean,
    onDismiss: () -> Unit,
    onReact: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onReply: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Tokens.Spacing.xl)
        ) {
            Text(
                text = "Message Actions",
                style = TypographyTokens.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm)
            )

            HorizontalDivider()

            ListItemStandard(
                text = "React",
                onClick = onReact,
                leadingIcon = IconSet.Message.reaction
            )

            ListItemStandard(
                text = "Reply",
                onClick = onReply,
                leadingIcon = IconSet.Message.reply
            )

            if (isSentByCurrentUser) {
                ListItemStandard(
                    text = "Edit",
                    onClick = onEdit,
                    leadingIcon = IconSet.Action.edit
                )
            }

            ListItemStandard(
                text = "Copy",
                onClick = onCopy,
                leadingIcon = IconSet.Action.copy
            )

            if (isSentByCurrentUser) {
                ListDivider()

                ListItemStandard(
                    text = "Delete",
                    onClick = onDelete,
                    leadingIcon = IconSet.Action.delete
                )
            }
        }
    }
}

/**
 * Reaction Picker Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReactionPickerBottomSheet(
    onDismiss: () -> Unit,
    onReactionSelected: (String) -> Unit
) {
    val commonEmojis = listOf(
        "👍", "❤️", "😂", "😮", "😢", "🙏",
        "🔥", "🎉", "✅", "👏", "💯", "🚀"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md)
                .padding(bottom = Tokens.Spacing.xl)
        ) {
            Text(
                text = "React with",
                style = TypographyTokens.typography.titleMedium,
                modifier = Modifier.padding(bottom = Tokens.Spacing.md)
            )

            // Emoji grid
            Column(
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                commonEmojis.chunked(6).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                    ) {
                        row.forEach { emoji ->
                            Surface(
                                onClick = { onReactionSelected(emoji) },
                                modifier = Modifier
                                    .size(Tokens.TouchTarget.recommended)
                                    .weight(1f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = emoji,
                                        style = TypographyTokens.typography.headlineSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data Classes
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val senderId: String,
    val senderName: String,
    val timestamp: Long,
    val formattedTime: String,
    val formattedDate: String,
    val isEdited: Boolean = false,
    val reactions: List<Reaction> = emptyList(),
    val readBy: List<String> = emptyList(),
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null
)

data class Reaction(
    val emoji: String,
    val userIds: List<String>,
    val count: Int
)

data class GroupedMessage(
    val message: ChatMessage,
    val isFirstInGroup: Boolean,
    val isLastInGroup: Boolean,
    val showDateDivider: Boolean
)

/**
 * Helper function to group messages
 */
private fun groupMessages(messages: List<ChatMessage>, currentUserId: String): List<GroupedMessage> {
    if (messages.isEmpty()) return emptyList()

    val grouped = mutableListOf<GroupedMessage>()
    val groupTimeWindow = 5 * 60 * 1000 // 5 minutes

    messages.forEachIndexed { index, message ->
        val prevMessage = messages.getOrNull(index + 1) // Reversed list
        val nextMessage = messages.getOrNull(index - 1)

        val isFirstInGroup = prevMessage == null ||
                prevMessage.senderId != message.senderId ||
                (message.timestamp - prevMessage.timestamp) > groupTimeWindow

        val isLastInGroup = nextMessage == null ||
                nextMessage.senderId != message.senderId ||
                (nextMessage.timestamp - message.timestamp) > groupTimeWindow

        val showDateDivider = prevMessage == null ||
                message.formattedDate != prevMessage.formattedDate

        grouped.add(
            GroupedMessage(
                message = message,
                isFirstInGroup = isFirstInGroup,
                isLastInGroup = isLastInGroup,
                showDateDivider = showDateDivider
            )
        )
    }

    return grouped
}
