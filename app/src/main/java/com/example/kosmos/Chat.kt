package com.example.kosmos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.AuthRepository
import com.example.kosmos.ChatRepository
import com.example.kosmos.UserRepository
import com.example.kosmos.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
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
import com.example.kosmos.models.ChatRoom
import com.example.kosmos.models.Message
import com.example.kosmos.models.MessageType
import com.example.kosmos.models.VoiceMessage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val currentUser = authRepository.getCurrentUser()

    init {
        if (currentUser != null) {
            loadChatRooms()
            loadCurrentUser()
        }
    }

    private fun loadCurrentUser() {
        currentUser?.let { firebaseUser ->
            viewModelScope.launch {
                try {
                    val user = userRepository.getUserById(firebaseUser.uid)
                    _uiState.value = _uiState.value.copy(currentUser = user)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load user profile"
                    )
                }
            }
        }
    }

    private fun loadChatRooms() {
        currentUser?.let { firebaseUser ->
            viewModelScope.launch {
                try {
                    chatRepository.getChatRoomsFlow(firebaseUser.uid).collect { chatRooms ->
                        _uiState.value = _uiState.value.copy(
                            chatRooms = chatRooms,
                            isLoading = false
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load chat rooms: ${e.message}"
                    )
                }
            }
        }
    }

    fun createNewChatRoom(name: String, description: String, selectedUserIds: List<String>) {
        currentUser?.let { firebaseUser ->
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(isCreatingChat = true)

                    val participantIds = selectedUserIds + firebaseUser.uid
                    val chatRoom = ChatRoom(
                        name = name,
                        description = description,
                        participantIds = participantIds,
                        createdBy = firebaseUser.uid,
                        createdAt = System.currentTimeMillis()
                    )

                    val result = chatRepository.createChatRoom(chatRoom)
                    result.fold(
                        onSuccess = { chatRoomId ->
                            _uiState.value = _uiState.value.copy(
                                isCreatingChat = false,
                                showCreateChatDialog = false
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isCreatingChat = false,
                                error = "Failed to create chat room: ${exception.message}"
                            )
                        }
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isCreatingChat = false,
                        error = "Failed to create chat room: ${e.message}"
                    )
                }
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }

        viewModelScope.launch {
            try {
                val users = userRepository.searchUsers(query)
                _uiState.value = _uiState.value.copy(searchResults = users)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to search users: ${e.message}"
                )
            }
        }
    }

    fun showCreateChatDialog() {
        _uiState.value = _uiState.value.copy(showCreateChatDialog = true)
    }

    fun hideCreateChatDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateChatDialog = false,
            searchResults = emptyList()
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

data class ChatListUiState(
    val isLoading: Boolean = true,
    val chatRooms: List<ChatRoom> = emptyList(),
    val currentUser: User? = null,
    val showCreateChatDialog: Boolean = false,
    val isCreatingChat: Boolean = false,
    val searchResults: List<User> = emptyList(),
    val error: String? = null
)


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
                    var showMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Profile") },
                            onClick = {
                                showMenu = false
                                // Navigate to profile
                            },
                            leadingIcon = { Icon(Icons.Default.Person, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showMenu = false
                                // Navigate to settings
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                showMenu = false
                                viewModel.logout()
                                onLogout()
                            },
                            leadingIcon = { Icon(Icons.Default.ExitToApp, null) }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Error message
        if (uiState.error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Chat rooms list
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
                    onCreateChat = { viewModel.showCreateChatDialog() }
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
                            onClick = { onNavigateToChat(chatRoom.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Create chat dialog
    if (uiState.showCreateChatDialog) {
        CreateChatDialog(
            onDismiss = { viewModel.hideCreateChatDialog() },
            onCreateChat = viewModel::createNewChatRoom,
            onSearchUsers = viewModel::searchUsers,
            searchResults = uiState.searchResults,
            isCreating = uiState.isCreatingChat
        )
    }
}

@Composable
private fun ChatRoomItem(
    chatRoom: ChatRoom,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chat room avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (chatRoom.imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(chatRoom.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = chatRoom.name.take(2).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Chat info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chatRoom.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chatRoom.isTaskBoardEnabled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Task,
                            contentDescription = "Task Board Enabled",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = chatRoom.description.ifBlank { "${chatRoom.participantIds.size} participants" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Timestamp and status
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatTimestamp(chatRoom.lastMessageTimestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Unread indicator (placeholder)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent) // Will be implemented with actual unread count
                )
            }
        }
    }
}

@Composable
private fun EmptyChatListContent(
    onCreateChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Chat,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Conversations Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start your first conversation with colleagues",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCreateChat,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Chat Room")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateChatDialog(
    onDismiss: () -> Unit,
    onCreateChat: (String, String, List<String>) -> Unit,
    onSearchUsers: (String) -> Unit,
    searchResults: List<com.example.kosmos.models.User>,
    isCreating: Boolean,
    modifier: Modifier = Modifier
) {
    var chatName by remember { mutableStateOf("") }
    var chatDescription by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUsers by remember { mutableStateOf<Set<String>>(emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Chat Room") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = chatName,
                    onValueChange = { chatName = it },
                    label = { Text("Chat Room Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = chatDescription,
                    onValueChange = { chatDescription = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Add Participants",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onSearchUsers(it)
                    },
                    label = { Text("Search users...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (searchResults.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                    ) {
                        items(searchResults) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedUsers = if (user.id in selectedUsers) {
                                            selectedUsers - user.id
                                        } else {
                                            selectedUsers + user.id
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = user.id in selectedUsers,
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = user.displayName,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateChat(chatName, chatDescription, selectedUsers.toList())
                },
                enabled = chatName.isNotBlank() && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator()
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now" // Less than 1 minute
        diff < 3600_000 -> "${diff / 60_000}m" // Less than 1 hour
        diff < 86400_000 -> "${diff / 3600_000}h" // Less than 1 day
        diff < 604800_000 -> {
            val days = diff / 86400_000
            "${days}d"
        } // Less than 1 week
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val currentUser = authRepository.getCurrentUser()
    private var currentChatRoomId: String = ""

    fun loadChat(chatRoomId: String) {
        currentChatRoomId = chatRoomId
        loadChatRoom(chatRoomId)
        loadMessages(chatRoomId)
    }

    private fun loadChatRoom(chatRoomId: String) {
        viewModelScope.launch {
            try {
                // This will be implemented when we add the flow method to ChatRepository
                // For now, we'll create a basic implementation
                _uiState.value = _uiState.value.copy(
                    chatRoom = ChatRoom(id = chatRoomId, name = "Loading...")
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load chat room"
                )
            }
        }
    }

    private fun loadMessages(chatRoomId: String) {
        viewModelScope.launch {
            try {
                chatRepository.getMessagesFlow(chatRoomId).collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load messages: ${e.message}"
                )
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || currentUser == null) return

        viewModelScope.launch {
            try {
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    chatRoomId = currentChatRoomId,
                    senderId = currentUser.uid,
                    senderName = currentUser.displayName ?: "Unknown",
                    senderPhotoUrl = currentUser.photoUrl?.toString(),
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.TEXT
                )

                chatRepository.sendMessage(message)
                _uiState.value = _uiState.value.copy(messageText = "")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send message: ${e.message}"
                )
            }
        }
    }

    fun sendVoiceMessage(audioFile: File, duration: Long) {
        if (currentUser == null) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSendingVoice = true)

                val voiceMessageId = UUID.randomUUID().toString()
                val messageId = UUID.randomUUID().toString()

                val voiceMessage = VoiceMessage(
                    id = voiceMessageId,
                    messageId = messageId,
                    duration = duration,
                    isTranscribing = true
                )

                // Upload voice message
                val uploadResult = voiceRepository.uploadVoiceMessage(audioFile, voiceMessage)
                uploadResult.fold(
                    onSuccess = { uploadedVoiceMessageId ->
                        // Create text message with voice attachment
                        val message = Message(
                            id = messageId,
                            chatRoomId = currentChatRoomId,
                            senderId = currentUser.uid,
                            senderName = currentUser.displayName ?: "Unknown",
                            senderPhotoUrl = currentUser.photoUrl?.toString(),
                            content = "🎤 Voice message",
                            timestamp = System.currentTimeMillis(),
                            type = MessageType.VOICE,
                            voiceMessageId = uploadedVoiceMessageId
                        )

                        chatRepository.sendMessage(message)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to send voice message: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send voice message: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isSendingVoice = false)
            }
        }
    }

    fun updateMessageText(text: String) {
        _uiState.value = _uiState.value.copy(messageText = text)
    }

    fun startVoiceRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true)
    }

    fun stopVoiceRecording() {
        _uiState.value = _uiState.value.copy(isRecording = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun loadOlderMessages() {
        val oldestMessage = _uiState.value.messages.lastOrNull()
        if (oldestMessage != null && !_uiState.value.isLoadingMore) {
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(isLoadingMore = true)
                    val olderMessages = chatRepository.loadOlderMessages(
                        currentChatRoomId,
                        oldestMessage.timestamp
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        hasMoreMessages = olderMessages.isNotEmpty()
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = "Failed to load older messages"
                    )
                }
            }
        }
    }

    fun markMessagesAsRead() {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    _uiState.value.messages
                        .filter { it.senderId != user.uid && user.uid !in it.readBy }
                        .forEach { message ->
                            chatRepository.markMessageAsRead(message.id, user.uid)
                        }
                } catch (e: Exception) {
                    // Handle silently
                }
            }
        }
    }
}

data class ChatUiState(
    val isLoading: Boolean = true,
    val chatRoom: ChatRoom? = null,
    val messages: List<Message> = emptyList(),
    val messageText: String = "",
    val isRecording: Boolean = false,
    val isSendingVoice: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatRoomId: String,
    onNavigateBack: () -> Unit,
    onNavigateToTaskBoard: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(chatRoomId) {
        viewModel.loadChat(chatRoomId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty() && !uiState.isLoading) {
            listState.animateScrollToItem(0)
        }
    }

    // Mark messages as read when screen is visible
    LaunchedEffect(uiState.messages) {
        viewModel.markMessagesAsRead()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = uiState.chatRoom?.name ?: "Loading...",
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (uiState.chatRoom != null) {
                        Text(
                            text = "${uiState.chatRoom!!.participantIds.size} participants",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (uiState.chatRoom?.isTaskBoardEnabled == true) {
                    IconButton(onClick = onNavigateToTaskBoard) {
                        Icon(Icons.Default.Task, contentDescription = "Task Board")
                    }
                }

                IconButton(onClick = { /* Open chat info */ }) {
                    Icon(Icons.Default.Info, contentDescription = "Chat Info")
                }
            }
        )

        // Error message
        if (uiState.error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        Text("Dismiss", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Messages list
        Box(
            modifier = Modifier.weight(1f)
        ) {
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
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        reverseLayout = true // Show newest messages at bottom
                    ) {
                        items(uiState.messages) { message ->
                            MessageBubble(
                                message = message,
                                isFromCurrentUser = message.senderId == viewModel.currentUser?.uid,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 2.dp
                                )
                            )
                        }

                        // Load more indicator
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }

                    // Load more messages when scrolled to top
                    LaunchedEffect(listState) {
                        snapshotFlow { listState.firstVisibleItemIndex }
                            .collect { index ->
                                if (index >= uiState.messages.size - 5 && uiState.hasMoreMessages) {
                                    viewModel.loadOlderMessages()
                                }
                            }
                    }
                }
            }
        }

        // Message input
        MessageInput(
            messageText = uiState.messageText,
            onMessageTextChange = viewModel::updateMessageText,
            onSendMessage = { viewModel.sendMessage(uiState.messageText) },
            onStartVoiceRecording = viewModel::startVoiceRecording,
            onStopVoiceRecording = viewModel::stopVoiceRecording,
            isRecording = uiState.isRecording,
            isSendingVoice = uiState.isSendingVoice,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromCurrentUser) {
            // Sender avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (message.senderPhotoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(message.senderPhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = message.senderName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message bubble
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Sender name (only for others)
            if (!isFromCurrentUser) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            // Message content
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isFromCurrentUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                )
            ) {
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.content,
                            color = if (isFromCurrentUser) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    MessageType.VOICE -> {
                        VoiceMessageBubble(
                            message = message,
                            isFromCurrentUser = isFromCurrentUser
                        )
                    }
                    else -> {
                        Text(
                            text = message.content,
                            color = if (isFromCurrentUser) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Timestamp and status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatMessageTime(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (isFromCurrentUser) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (message.readBy.isNotEmpty()) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (message.readBy.isNotEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }

        if (isFromCurrentUser) {
            Spacer(modifier = Modifier.width(40.dp)) // Space for alignment
        }
    }
}

@Composable
private fun VoiceMessageBubble(
    message: Message,
    isFromCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/pause button
        IconButton(
            onClick = { /* TODO: Implement audio playback */ },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play voice message",
                tint = if (isFromCurrentUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Waveform placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        if (isFromCurrentUser) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            // Duration and transcription
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "0:30", // TODO: Get actual duration
                style = MaterialTheme.typography.bodySmall,
                color = if (isFromCurrentUser) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageInput(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onStartVoiceRecording: () -> Unit,
    onStopVoiceRecording: () -> Unit,
    isRecording: Boolean,
    isSendingVoice: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageTextChange,
            placeholder = { Text("Type a message...") },
            modifier = Modifier.weight(1f),
            maxLines = 4,
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Voice/Send button
        if (messageText.isBlank()) {
            FloatingActionButton(
                onClick = {
                    if (isRecording) onStopVoiceRecording() else onStartVoiceRecording()
                },
                modifier = Modifier.size(48.dp),
                containerColor = if (isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ) {
                if (isSendingVoice) {
                    CircularProgressIndicator(
//                        size = 20.dp,
//                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop recording" else "Record voice message"
                    )
                }
            }
        } else {
            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send message")
            }
        }
    }
}

@Composable
private fun EmptyMessagesContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ChatBubble,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Start the conversation!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        calendar.get(Calendar.DAY_OF_YEAR) == messageCalendar.get(Calendar.DAY_OF_YEAR) &&
                calendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR) -> {
            // Today - show time only
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        calendar.get(Calendar.DAY_OF_YEAR) - messageCalendar.get(Calendar.DAY_OF_YEAR) == 1 &&
                calendar.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR) -> {
            // Yesterday
            "Yesterday ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
        }
        else -> {
            // Older messages
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
}