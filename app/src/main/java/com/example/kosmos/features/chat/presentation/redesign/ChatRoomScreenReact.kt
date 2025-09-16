package com.example.kosmos.features.chat.presentation.redesign

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Chat Room Screen - React Design Implementation
 *
 * Design Reference: documents/Kosmos/src/app/components/chat/ChatRoomScreen.tsx
 *                   documents/Kosmos/src/app/components/chat/MessageBubble.tsx
 *                   documents/Kosmos/src/app/components/chat/MessageInput.tsx
 *
 * Features:
 * - Chat room with message list and input
 * - Top app bar with chat name, project, member count
 * - Three message types: user, system, task-linked
 * - Message grouping (hide avatar/name for consecutive messages)
 * - Message reactions (emoji + count)
 * - Reply preview in input area
 * - Send button (only shows when text entered)
 *
 * All styling matches React design exactly:
 * - Colors from ColorTokens.ReactTheme.*
 * - User messages: card bg, border, rounded-xl
 * - System messages: centered, secondary bg, pill shape
 * - Task messages: primary/10 bg, primary border, task icon
 * - Avatars: 32dp circle, primary/indigo bg
 * - Input: secondary bg, rounded-xl, focus ring
 * - NO backend wiring (mock data only)
 */

// Data models
enum class MessageTypeReact {
    USER, SYSTEM, TASK
}

enum class ReadStatus { SENT, DELIVERED, READ }

data class MessageData(
    val id: String,
    val type: MessageTypeReact,
    val sender: MessageSender? = null,
    val content: String,
    val timestamp: String,
    val isOwn: Boolean = false,
    val taskLink: TaskLink? = null,
    val reactions: List<ReactionData> = emptyList(),
    val readStatus: ReadStatus = ReadStatus.SENT
)

data class MessageSender(
    val name: String,
    val avatar: String
)

data class TaskLink(
    val title: String,
    val status: String
)

data class ReactionData(
    val emoji: String,
    val count: Int
)

data class ReplyTo(
    val sender: String,
    val message: String
)

@Composable
fun ChatRoomScreenReact(
    chatId: String,
    chatName: String = "Loading...",
    projectName: String = "",
    memberCount: Int = 0,
    messages: List<MessageData> = mockMessages,
    replyTo: ReplyTo? = null,
    onReplyToChange: (ReplyTo?) -> Unit = {},
    onSendMessage: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onViewMembers: () -> Unit = {},
    onSearchMessages: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onChatSettings: () -> Unit = {},
    onLeaveChat: () -> Unit = {},
    onReaction: (messageId: String, emoji: String) -> Unit = { _, _ -> },
    onEditMessage: (messageId: String, newContent: String) -> Unit = { _, _ -> },
    onReplyToMessage: (messageId: String) -> Unit = {}
) {
    // Message grouping (matches React logic)
    val messagesWithGrouping = remember(messages) {
        messages.mapIndexed { idx, msg ->
            val prevMsg = if (idx > 0) messages[idx - 1] else null
            val showAvatar = prevMsg == null ||
                    prevMsg.type != msg.type ||
                    prevMsg.sender?.name != msg.sender?.name ||
                    msg.type == MessageTypeReact.SYSTEM
            val showName = showAvatar && msg.type != MessageTypeReact.SYSTEM

            msg to Pair(showAvatar, showName)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            chatName = chatName,
            projectName = projectName,
            memberCount = memberCount,
            onBack = onBack,
            onViewMembers = onViewMembers,
            onSearchMessages = onSearchMessages,
            onToggleMute = onToggleMute,
            onChatSettings = onChatSettings,
            onLeaveChat = onLeaveChat
        )

        // Messages (reverseLayout = true displays newest at bottom)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,  // Display messages bottom-to-top (newest at bottom)
            contentPadding = PaddingValues(16.dp)
        ) {
            items(messagesWithGrouping) { (message, grouping) ->
                val (showAvatar, showName) = grouping
                MessageBubble(
                    message = message,
                    showAvatar = showAvatar,
                    showName = showName,
                    onReaction = { emoji -> onReaction(message.id, emoji) },
                    onEditMessage = { newContent -> onEditMessage(message.id, newContent) },
                    onReplyToMessage = { onReplyToMessage(message.id) }
                )
            }
        }

        // Message Input
        MessageInput(
            replyTo = replyTo,
            onCancelReply = { onReplyToChange(null) },
            onSend = { message ->
                onSendMessage(message)
                onReplyToChange(null)
            }
        )
    }
}

// Mock data for testing
private val mockMessages = listOf(
    MessageData(
        id = "1",
        type = MessageTypeReact.SYSTEM,
        content = "Alice Chen created this chat",
        timestamp = "Jan 8"
    ),
    MessageData(
        id = "2",
        type = MessageTypeReact.USER,
                sender = MessageSender(name = "Alice Chen", avatar = "A"),
                content = "Hey team! I started working on the new onboarding flow designs. Would love to get your feedback.",
                timestamp = "10:30 AM",
                isOwn = false
            ),
            MessageData(
                id = "3",
                type = MessageTypeReact.USER,
                sender = MessageSender(name = "Bob Smith", avatar = "B"),
                content = "Sounds great! When can we expect the first draft?",
                timestamp = "10:32 AM",
                isOwn = false
            ),
            MessageData(
                id = "4",
                type = MessageTypeReact.USER,
                sender = MessageSender(name = "Alice Chen", avatar = "A"),
                content = "I should have something ready by end of day tomorrow.",
                timestamp = "10:35 AM",
                isOwn = false
            ),
            MessageData(
                id = "5",
                type = MessageTypeReact.TASK,
                sender = MessageSender(name = "Alice Chen", avatar = "A"),
                content = "Created this task to track the design work",
                timestamp = "10:36 AM",
                isOwn = false,
                taskLink = TaskLink(
                    title = "Design new onboarding flow for mobile app",
                    status = "In Progress"
                )
            ),
            MessageData(
                id = "6",
                type = MessageTypeReact.SYSTEM,
                content = "Today",
                timestamp = ""
            ),
            MessageData(
                id = "7",
                type = MessageTypeReact.USER,
                sender = MessageSender(name = "Carol Davis", avatar = "C"),
                content = "I reviewed the initial concepts. Looking good so far!",
                timestamp = "9:15 AM",
                isOwn = false,
                reactions = listOf(ReactionData(emoji = "👍", count = 2))
            ),
            MessageData(
                id = "8",
                type = MessageTypeReact.USER,
                sender = MessageSender(name = "You", avatar = "Y"),
                content = "I think we should use the purple variant for the primary buttons",
                timestamp = "9:45 AM",
                isOwn = true
    )
)

@Composable
private fun TopAppBar(
    chatName: String,
    projectName: String,
    memberCount: Int,
    onBack: () -> Unit,
    onViewMembers: () -> Unit,
    onSearchMessages: () -> Unit,
    onToggleMute: () -> Unit,
    onChatSettings: () -> Unit,
    onLeaveChat: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ColorTokens.ReactTheme.card,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back + chat info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = ColorTokens.ReactTheme.foreground,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = chatName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$projectName · $memberCount members",
                        fontSize = 12.sp,
                        color = ColorTokens.ReactTheme.mutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: More button with menu
            var showChatMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { showChatMenu = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = ColorTokens.ReactTheme.foreground,
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = showChatMenu,
                    onDismissRequest = { showChatMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("View Members") },
                        onClick = {
                            onViewMembers()
                            showChatMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.People, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Search Messages") },
                        onClick = {
                            onSearchMessages()
                            showChatMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mute Notifications") },
                        onClick = {
                            onToggleMute()
                            showChatMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Chat Settings") },
                        onClick = {
                            onChatSettings()
                            showChatMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Leave Chat", color = ColorTokens.Error.light) },
                        onClick = {
                            onLeaveChat()
                            showChatMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = ColorTokens.Error.light
                            )
                        }
                    )
                }
            }
        }
    }

    Divider(color = ColorTokens.ReactTheme.border, thickness = 1.dp)
}

@Composable
private fun MessageBubble(
    message: MessageData,
    showAvatar: Boolean,
    showName: Boolean,
    onReaction: (String) -> Unit = {},
    onEditMessage: (String) -> Unit = {},
    onReplyToMessage: () -> Unit = {}
) {
    when (message.type) {
        MessageTypeReact.SYSTEM -> SystemMessage(message)
        MessageTypeReact.TASK -> TaskMessage(message, showAvatar, showName)
        MessageTypeReact.USER -> UserMessage(
            message = message,
            showAvatar = showAvatar,
            showName = showName,
            onReaction = onReaction,
            onEditMessage = onEditMessage,
            onReplyToMessage = onReplyToMessage
        )
    }
}

@Composable
private fun SystemMessage(message: MessageData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = ColorTokens.ReactTheme.secondary,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = message.content,
                fontSize = 12.sp,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
    }
}

@Composable
private fun TaskMessage(
    message: MessageData,
    showAvatar: Boolean,
    showName: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar or spacer
        if (showAvatar) {
            Avatar(
                avatar = message.sender?.avatar ?: "",
                isOwn = message.isOwn
            )
        } else {
            Spacer(modifier = Modifier.width(32.dp))
        }

        // Message content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Sender name
            if (showName && message.sender != null) {
                Text(
                    text = message.sender.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
            }

            // Task card
            if (message.taskLink != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorTokens.ReactTheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(Tokens.CornerRadius.md),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = ColorTokens.ReactTheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Task header
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckBox,
                                contentDescription = null,
                                tint = ColorTokens.ReactTheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = message.taskLink.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorTokens.ReactTheme.primary
                                )
                                Text(
                                    text = message.taskLink.status,
                                    fontSize = 12.sp,
                                    color = ColorTokens.ReactTheme.mutedForeground
                                )
                            }
                        }

                        // Message content
                        Text(
                            text = message.content,
                            fontSize = 14.sp,
                            color = ColorTokens.ReactTheme.foreground,
                            lineHeight = 21.sp  // 1.5 line height
                        )
                    }
                }
            }

            // Timestamp
            Text(
                text = message.timestamp,
                fontSize = 12.sp,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserMessage(
    message: MessageData,
    showAvatar: Boolean,
    showName: Boolean,
    onReaction: (String) -> Unit = {},
    onEditMessage: (String) -> Unit = {},
    onReplyToMessage: () -> Unit = {}
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(message.content) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar or spacer
        if (showAvatar) {
            Avatar(
                avatar = message.sender?.avatar ?: "",
                isOwn = message.isOwn
            )
        } else {
            Spacer(modifier = Modifier.width(32.dp))
        }

        // Message content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Sender name
            if (showName && message.sender != null) {
                Text(
                    text = message.sender.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
            }

            // Message bubble with long-press menu
            Box {
                Surface(
                    color = ColorTokens.ReactTheme.card,
                    shape = RoundedCornerShape(Tokens.CornerRadius.md),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = ColorTokens.ReactTheme.border
                    ),
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { showContextMenu = true }
                    )
                ) {
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        color = ColorTokens.ReactTheme.foreground,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // Context menu
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    // Quick reactions
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("👍", "❤️", "😄", "🎉", "👀").forEach { emoji ->
                            Surface(
                                onClick = {
                                    onReaction(emoji)
                                    showContextMenu = false
                                },
                                shape = CircleShape,
                                color = ColorTokens.ReactTheme.secondary
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = ColorTokens.ReactTheme.border)
                    DropdownMenuItem(
                        text = { Text("Reply") },
                        onClick = {
                            onReplyToMessage()
                            showContextMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Reply, null) }
                    )
                    if (message.isOwn) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                editText = message.content
                                showEditDialog = true
                                showContextMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                    }
                }
            }

            // Timestamp + read status + reactions row
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.timestamp,
                    fontSize = 12.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )

                // Read receipt checkmarks for own messages
                if (message.isOwn) {
                    Icon(
                        imageVector = when (message.readStatus) {
                            ReadStatus.READ -> Icons.Filled.DoneAll
                            ReadStatus.DELIVERED -> Icons.Filled.DoneAll
                            ReadStatus.SENT -> Icons.Filled.Done
                        },
                        contentDescription = message.readStatus.name.lowercase(),
                        modifier = Modifier.size(14.dp),
                        tint = if (message.readStatus == ReadStatus.READ)
                            ColorTokens.ReactTheme.primary
                        else
                            ColorTokens.ReactTheme.mutedForeground
                    )
                }

                if (message.reactions.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        message.reactions.forEach { reaction ->
                            Surface(
                                onClick = { onReaction(reaction.emoji) },
                                shape = RoundedCornerShape(12.dp),
                                color = ColorTokens.ReactTheme.secondary
                            ) {
                                Text(
                                    text = "${reaction.emoji} ${reaction.count}",
                                    fontSize = 12.sp,
                                    color = ColorTokens.ReactTheme.foreground,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit message dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Message", color = ColorTokens.ReactTheme.foreground) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorTokens.ReactTheme.card,
                        unfocusedContainerColor = ColorTokens.ReactTheme.card,
                        focusedBorderColor = ColorTokens.ReactTheme.primary,
                        unfocusedBorderColor = ColorTokens.ReactTheme.border,
                        focusedTextColor = ColorTokens.ReactTheme.foreground,
                        unfocusedTextColor = ColorTokens.ReactTheme.foreground
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEditMessage(editText)
                        showEditDialog = false
                    },
                    enabled = editText.isNotBlank() && editText != message.content
                ) {
                    Text("Save", color = ColorTokens.ReactTheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = ColorTokens.ReactTheme.mutedForeground)
                }
            },
            containerColor = ColorTokens.ReactTheme.card
        )
    }
}

@Composable
private fun Avatar(
    avatar: String,
    isOwn: Boolean
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                if (isOwn) Color(0xFF6366F1)  // Indigo for own messages
                else ColorTokens.ReactTheme.primary  // Purple for others
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = avatar,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun MessageInput(
    replyTo: ReplyTo?,
    onCancelReply: () -> Unit,
    onSend: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    val handleSend = {
        if (message.trim().isNotEmpty()) {
            onSend(message)
            message = ""
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ColorTokens.ReactTheme.card,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            Divider(color = ColorTokens.ReactTheme.border, thickness = 1.dp)

            // Reply preview
            if (replyTo != null) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                ) {
                    Surface(
                        color = ColorTokens.ReactTheme.secondary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 3dp primary accent bar
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(48.dp)
                                    .background(
                                        ColorTokens.ReactTheme.primary,
                                        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Replying to ${replyTo.sender}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ColorTokens.ReactTheme.primary
                                    )
                                    Text(
                                        text = replyTo.message,
                                        fontSize = 14.sp,
                                        color = ColorTokens.ReactTheme.mutedForeground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = onCancelReply,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel reply",
                                        tint = ColorTokens.ReactTheme.mutedForeground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = ColorTokens.ReactTheme.border, thickness = 1.dp)
            }

            // Input area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Text input
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .background(
                            color = ColorTokens.ReactTheme.secondary,
                            shape = RoundedCornerShape(Tokens.CornerRadius.md)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 15.sp,
                            color = ColorTokens.ReactTheme.foreground
                        ),
                        decorationBox = { innerTextField ->
                            if (message.isEmpty()) {
                                Text(
                                    text = "Type a message...",
                                    fontSize = 15.sp,
                                    color = ColorTokens.ReactTheme.mutedForeground
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // Send button (only when message is not empty)
                if (message.trim().isNotEmpty()) {
                    IconButton(
                        onClick = handleSend,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = ColorTokens.ReactTheme.primary,
                                shape = RoundedCornerShape(Tokens.CornerRadius.md)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = ColorTokens.ReactTheme.primaryForeground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
