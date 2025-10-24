package com.example.kosmos.features.chat.presentation

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onNavigateToChat: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        // Any initialization logic if needed
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Kosmos",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
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
                                // TODO: Navigate to profile
                            },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showUserMenu = false
                                // TODO: Navigate to settings
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
                viewModel.createNewChatRoom(name, description, selectedUserIds)
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = true
                    ) {
                        items(uiState.messages.reversed()) { message ->
                            MessageBubble(
                                message = message,
                                isCurrentUser = message.senderId == viewModel.currentUser?.uid
                            )
                        }
                    }
                }
            }
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
    // TODO: Implement create chat dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Chat") },
        text = { Text("Create chat dialog implementation needed") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean
) {
    // TODO: Implement message bubble with proper styling
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCurrentUser) 32.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            if (!isCurrentUser) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrentUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
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