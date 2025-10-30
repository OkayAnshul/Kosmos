package com.example.kosmos.features.chat.presentation

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.kosmos.core.models.ChatRoom
import com.example.kosmos.core.models.Message
import com.example.kosmos.core.models.MessageType
import com.example.kosmos.core.models.VoiceMessage
import com.example.kosmos.core.models.User
import kotlinx.coroutines.Job
import java.io.File
import java.util.UUID

// Main Chat List Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    projectId: String,
    onNavigateToChat: (String) -> Unit,
    onLogout: () -> Unit,
    onBackToProjects: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToUserSearch: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load chat rooms for this project
    LaunchedEffect(projectId) {
        viewModel.loadChatRooms(projectId)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Project Chats",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackToProjects) {
                    Icon(Icons.Default.ArrowBack, "Back to Projects")
                }
            },
            actions = {
                // User Search Button
                IconButton(onClick = onNavigateToUserSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Find Users")
                }

                // Create Chat Button
                IconButton(onClick = { viewModel.showCreateChatDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Chat")
                }

                Box {
                    var showUserMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = { showUserMenu = true }) {
                        val currentUser = uiState.currentUser
                        if (currentUser?.photoUrl?.isNotEmpty() == true) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(currentUser.photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    }

                    DropdownMenu(
                        expanded = showUserMenu,
                        onDismissRequest = { showUserMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Profile") },
                            onClick = {
                                showUserMenu = false
                                onNavigateToProfile()
                            },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showUserMenu = false
                                onNavigateToSettings()
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                showUserMenu = false
                                viewModel.logout()
                                onLogout()
                            },
                            leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) }
                        )
                    }
                }
            }
        )

        // Content
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.chatRooms.isEmpty() -> {
                EmptyChatListContent(
                    onCreateNewChat = { viewModel.showCreateChatDialog() }
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.chatRooms) { chatRoom ->
                        ChatRoomItem(
                            chatRoom = chatRoom,
                            onClick = { onNavigateToChat(chatRoom.id) }
                        )
                    }
                }
            }
        }
    }

    // Create Chat Dialog
    if (uiState.showCreateChatDialog) {
        CreateChatDialog(
            onDismiss = { viewModel.hideCreateChatDialog() },
            onCreate = { name, description, selectedUserIds ->
                viewModel.createNewChatRoom(name, description, selectedUserIds, projectId)
            },
            searchResults = uiState.searchResults,
            onSearchUsers = { query -> viewModel.searchUsers(query) },
            isCreating = uiState.isCreatingChat
        )
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error message
        }
    }
}

// Main Chat Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatRoomId: String,
    onNavigateBack: () -> Unit,
    onNavigateToTasks: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(chatRoomId) {
        viewModel.loadChat(chatRoomId)
    }

    LaunchedEffect(Unit) {
        viewModel.markMessagesAsRead()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = uiState.chatRoom?.name ?: "Loading...",
                    fontWeight = FontWeight.Medium
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Navigate to Task Board
                IconButton(onClick = onNavigateToTasks) {
                    Icon(Icons.Default.Task, contentDescription = "View Tasks")
                }
                IconButton(onClick = { /* TODO: Open chat settings */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }
        )

        // Messages
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.messages.isEmpty() -> {
                    EmptyMessagesContent()
                }
                else -> {
                    val listState = rememberLazyListState()

                    // Detect when user scrolls to top to load more messages
                    LaunchedEffect(listState) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                            .collect { visibleItems ->
                                val lastVisibleItem = visibleItems.lastOrNull()
                                if (lastVisibleItem != null &&
                                    lastVisibleItem.index >= uiState.messages.size - 1 &&
                                    uiState.hasMoreMessages &&
                                    !uiState.isLoadingMore
                                ) {
                                    viewModel.loadOlderMessages()
                                }
                            }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = true
                    ) {
                        items(uiState.messages.reversed()) { message ->
                            MessageBubble(
                                message = message,
                                isCurrentUser = message.senderId == viewModel.currentUser?.id,
                                currentUserId = viewModel.currentUser?.id ?: "",
                                onLongPress = { viewModel.showMessageContextMenu(message) },
                                onReactionClick = { emoji ->
                                    viewModel.toggleReaction(message.id, emoji)
                                }
                            )
                        }

                        // Loading indicator at the top when loading more messages
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Typing Indicator
        if (uiState.typingUsers.isNotEmpty()) {
            TypingIndicator(
                typingCount = uiState.typingUsers.size
            )
        }

        // Message Input
        MessageInput(
            messageText = uiState.messageText,
            onMessageTextChange = viewModel::updateMessageText,
            onSendMessage = viewModel::sendMessage,
            isRecording = uiState.isRecording,
            onStartRecording = viewModel::startVoiceRecording,
            onStopRecording = viewModel::stopVoiceRecording,
            isSendingVoice = uiState.isSendingVoice
        )
    }

    // Message Context Menu Bottom Sheet
    uiState.selectedMessage?.let { selectedMessage ->
        if (uiState.showMessageContextMenu) {
            MessageContextMenuBottomSheet(
                message = selectedMessage,
                isCurrentUser = selectedMessage.senderId == viewModel.currentUser?.id,
                onDismiss = { viewModel.hideMessageContextMenu() },
                onEdit = { viewModel.showEditDialog() },
                onDelete = { viewModel.showDeleteDialog() },
                onReact = { viewModel.showReactionPicker() }
            )
        }

        // Edit Message Dialog
        if (uiState.showEditDialog) {
            EditMessageDialog(
                message = selectedMessage,
                onDismiss = { viewModel.hideEditDialog() },
                onConfirm = { newContent ->
                    viewModel.editMessage(newContent)
                }
            )
        }

        // Delete Message Dialog
        if (uiState.showDeleteDialog) {
            DeleteMessageDialog(
                onDismiss = { viewModel.hideDeleteDialog() },
                onConfirm = { viewModel.deleteMessage() }
            )
        }

        // Reaction Picker Dialog
        if (uiState.showReactionPicker) {
            ReactionPickerDialog(
                onDismiss = { viewModel.hideReactionPicker() },
                onReactionSelected = { emoji ->
                    viewModel.toggleReaction(selectedMessage.id, emoji)
                    viewModel.hideReactionPicker()
                }
            )
        }
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error message
            viewModel.clearError()
        }
    }
}

// Placeholder implementations for UI components
@Composable
private fun ChatRoomItem(
    chatRoom: ChatRoom,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chatRoom.name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chatRoom.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chatRoom.lastMessage.ifEmpty { "No messages yet" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyChatListContent(
    onCreateNewChat: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No chats yet",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Start a conversation by creating a new chat",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onCreateNewChat) {
                Text("Create New Chat")
            }
        }
    }
}

@Composable
private fun CreateChatDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, List<String>) -> Unit,
    searchResults: List<User>,
    onSearchUsers: (String) -> Unit,
    isCreating: Boolean
) {
    var chatName by remember { mutableStateOf("") }
    var chatDescription by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var roomType by remember { mutableStateOf("GENERAL") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Create New Chat Room") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Chat name input
                OutlinedTextField(
                    value = chatName,
                    onValueChange = {
                        chatName = it
                        nameError = false
                    },
                    label = { Text("Room Name *") },
                    placeholder = { Text("e.g., General Discussion") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Room name is required") }
                    } else null,
                    enabled = !isCreating,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description input
                OutlinedTextField(
                    value = chatDescription,
                    onValueChange = { chatDescription = it },
                    label = { Text("Description") },
                    placeholder = { Text("What is this room for?") },
                    enabled = !isCreating,
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Room type selector
                Text(
                    text = "Room Type:",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("GENERAL", "CHANNEL", "ANNOUNCEMENTS").forEach { type ->
                        FilterChip(
                            selected = roomType == type,
                            onClick = { if (!isCreating) roomType = type },
                            label = {
                                Text(
                                    text = type.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }

                // User search for participants
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.length >= 2) {
                            onSearchUsers(it)
                        }
                    },
                    label = { Text("Add Participants") },
                    placeholder = { Text("Search users...") },
                    enabled = !isCreating,
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, "Search")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Search results
                if (searchResults.isNotEmpty() && searchQuery.length >= 2) {
                    Text(
                        text = "Select users:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(
                        modifier = Modifier.heightIn(max = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        searchResults.take(5).forEach { user ->
                            val isSelected = selectedUsers.any { it.id == user.id }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedUsers = if (isSelected) {
                                        selectedUsers.filter { it.id != user.id }
                                    } else {
                                        selectedUsers + user
                                    }
                                },
                                label = { Text(user.displayName) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Selected users
                if (selectedUsers.isNotEmpty()) {
                    Text(
                        text = "Selected (${selectedUsers.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        selectedUsers.forEach { user ->
                            AssistChip(
                                onClick = {
                                    selectedUsers = selectedUsers.filter { it.id != user.id }
                                },
                                label = { Text(user.displayName, style = MaterialTheme.typography.bodySmall) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }

                if (isCreating) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (chatName.isBlank()) {
                        nameError = true
                    } else {
                        val participantIds = selectedUsers.map { it.id }
                        onCreate(chatName.trim(), chatDescription.trim(), participantIds)
                    }
                },
                enabled = !isCreating && chatName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    currentUserId: String,
    onLongPress: () -> Unit = {},
    onReactionClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrentUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentUser)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show metadata (timestamp, edited, read receipts)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edited indicator
                    if (message.isEdited) {
                        Text(
                            text = "Edited",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrentUser)
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Read receipts (only for current user's messages)
                    if (isCurrentUser) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(message.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            ReadReceiptIndicator(
                                isSent = true,
                                isDelivered = message.readBy.isNotEmpty(),
                                isRead = message.readBy.isNotEmpty(),
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Reaction bar
        if (message.reactions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            ReactionBar(
                reactions = message.reactions,
                currentUserId = currentUserId,
                onReactionClick = onReactionClick,
                modifier = Modifier.align(if (isCurrentUser) Alignment.End else Alignment.Start)
            )
        }
    }
}

@Composable
private fun MessageInput(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    isSendingVoice: Boolean
) {
    // TODO: Implement message input with voice recording
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (messageText.isNotBlank()) {
                IconButton(
                    onClick = {
                        onSendMessage(messageText)
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            } else {
                IconButton(
                    onClick = if (isRecording) onStopRecording else onStartRecording,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else Color.Transparent
                    )
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMessagesContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Message,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No messages yet",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Start the conversation by sending a message",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Message Context Menu Bottom Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageContextMenuBottomSheet(
    message: Message,
    isCurrentUser: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReact: () -> Unit
) {
    var showSheet by remember { mutableStateOf(true) }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                onDismiss()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                // React option (available for all messages)
                ListItem(
                    headlineContent = { Text("React") },
                    leadingContent = {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        showSheet = false
                        onReact()
                    }
                )

                // Edit option (only for own messages)
                if (isCurrentUser && message.type == MessageType.TEXT) {
                    ListItem(
                        headlineContent = { Text("Edit") },
                        leadingContent = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            showSheet = false
                            onEdit()
                        }
                    )
                }

                // Delete option (only for own messages)
                if (isCurrentUser) {
                    ListItem(
                        headlineContent = { Text("Delete") },
                        leadingContent = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        modifier = Modifier.clickable {
                            showSheet = false
                            onDelete()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// Edit Message Dialog
@Composable
private fun EditMessageDialog(
    message: Message,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var editedText by remember { mutableStateOf(message.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Message") },
        text = {
            OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type your message...") },
                maxLines = 5
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (editedText.isNotBlank() && editedText != message.content) {
                        onConfirm(editedText)
                        onDismiss()
                    }
                },
                enabled = editedText.isNotBlank() && editedText != message.content
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Delete Message Dialog
@Composable
private fun DeleteMessageDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Message?") },
        text = { Text("This message will be permanently deleted. This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Reaction Picker Dialog
@Composable
private fun ReactionPickerDialog(
    onDismiss: () -> Unit,
    onReactionSelected: (String) -> Unit
) {
    val commonEmojis = listOf(
        "👍", "❤️", "😂", "😮", "😢", "🙏",
        "🎉", "🔥", "👏", "💯", "🤔", "😍"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "React to message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Emoji grid
                val rows = commonEmojis.chunked(6)
                rows.forEach { rowEmojis ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowEmojis.forEach { emoji ->
                            TextButton(
                                onClick = {
                                    onReactionSelected(emoji)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

// Reaction Bar Component
@Composable
private fun ReactionBar(
    reactions: Map<String, String>,
    currentUserId: String,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group reactions by emoji
    val reactionGroups = reactions.values.groupingBy { it }.eachCount()

    Row(
        modifier = modifier
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        reactionGroups.forEach { (emoji, count) ->
            val isCurrentUserReaction = reactions[currentUserId] == emoji

            Surface(
                modifier = Modifier.clickable { onReactionClick(emoji) },
                shape = RoundedCornerShape(12.dp),
                color = if (isCurrentUserReaction)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                border = if (isCurrentUserReaction)
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                else
                    null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (count > 1) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrentUserReaction)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Read Receipt Indicator Component
@Composable
private fun ReadReceiptIndicator(
    isSent: Boolean,
    isDelivered: Boolean,
    isRead: Boolean,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val icon = when {
        isRead -> "✓✓"  // Double check - read
        isDelivered -> "✓✓"  // Double check - delivered
        isSent -> "✓"  // Single check - sent
        else -> ""
    }

    val color = when {
        isRead -> MaterialTheme.colorScheme.primary  // Blue for read
        else -> tint  // Gray for sent/delivered
    }

    if (icon.isNotEmpty()) {
        Text(
            text = icon,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = if (isRead) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// Typing Indicator Component
@Composable
private fun TypingIndicator(
    typingCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Animated typing dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "typing")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_alpha_$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                CircleShape
                            )
                    )
                }
            }

            Text(
                text = if (typingCount == 1) "Someone is typing..." else "$typingCount people are typing...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}