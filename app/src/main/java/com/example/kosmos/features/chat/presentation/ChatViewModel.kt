package com.example.kosmos.features.chat.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ChatRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.data.repository.VoiceRepository
import com.example.kosmos.core.models.ChatRoom
import com.example.kosmos.core.models.Message
import com.example.kosmos.core.models.MessageType
import com.example.kosmos.core.models.VoiceMessage
// Voice features disabled for MVP - will be re-enabled in Phase 5
// import com.example.kosmos.services.TranscriptionWorkerService
// import com.example.kosmos.features.voice.services.VoiceRecordingHelper
// import com.example.kosmos.features.voice.services.VoiceRecordingState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val voiceRepository: VoiceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val currentUser = authRepository.getCurrentUser()
    private var currentChatRoomId: String = ""

    // Track flow collection jobs for proper cleanup
    private var chatRoomJob: Job? = null
    private var messagesJob: Job? = null
    private var messageEventsJob: Job? = null
    private var typingEventsJob: Job? = null

    // Voice recording components - disabled for MVP (Phase 5)
    // private var voiceRecordingHelper: VoiceRecordingHelper? = null
    // private var currentRecordingJob: Job? = null

    // init {
    //     voiceRecordingHelper = VoiceRecordingHelper(context)
    // }

    fun loadChat(chatRoomId: String) {
        currentChatRoomId = chatRoomId
        loadChatRoom(chatRoomId)
        loadMessages(chatRoomId)
        startRealtimeSubscription(chatRoomId)
    }

    private fun loadChatRoom(chatRoomId: String) {
        // Cancel previous job if any
        chatRoomJob?.cancel()

        chatRoomJob = viewModelScope.launch {
            try {
                chatRepository.getChatRoomByIdFlow(chatRoomId).collect { chatRoom ->
                    if (chatRoom != null) {
                        _uiState.value = _uiState.value.copy(chatRoom = chatRoom)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            chatRoom = ChatRoom(id = chatRoomId, name = "Loading...")
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load chat room: ${e.message}"
                )
            }
        }
    }

    private fun loadMessages(chatRoomId: String) {
        // Cancel previous job if any
        messagesJob?.cancel()

        messagesJob = viewModelScope.launch {
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
                    senderId = currentUser.id,
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
                Log.d("ChatViewModel", "Sending voice message: ${audioFile.absolutePath}")

                val voiceMessageId = UUID.randomUUID().toString()
                val messageId = UUID.randomUUID().toString()

                val voiceMessage = VoiceMessage(
                    id = voiceMessageId,
                    messageId = messageId,
                    duration = duration,
                    isTranscribing = true
                )

                // Save voice message locally (TODO: implement upload to cloud storage)
                val saveResult = voiceRepository.saveVoiceMessage(voiceMessage)
                saveResult.fold(
                    onSuccess = { savedVoiceMessageId ->
                        Log.d("ChatViewModel", "Voice message saved: $savedVoiceMessageId")

                        // Create text message with voice attachment
                        val message = Message(
                            id = messageId,
                            chatRoomId = currentChatRoomId,
                            senderId = currentUser.id,
                            senderName = currentUser.displayName ?: "Unknown",
                            senderPhotoUrl = currentUser.photoUrl?.toString(),
                            content = "🎤 Voice message",
                            timestamp = System.currentTimeMillis(),
                            type = MessageType.VOICE,
                            voiceMessageId = savedVoiceMessageId
                        )

                        chatRepository.sendMessage(message)

                        // Start background transcription - disabled for MVP (Phase 5)
                        // TranscriptionWorkerService.startService(context)

                        Log.d("ChatViewModel", "Voice message sent successfully")
                    },
                    onFailure = { exception ->
                        Log.e("ChatViewModel", "Failed to upload voice message", exception)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to send voice message: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending voice message", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send voice message: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isSendingVoice = false)

                // Clean up temporary file
                try {
                    audioFile.delete()
                    Log.d("ChatViewModel", "Temporary audio file deleted")
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Failed to delete temporary audio file", e)
                }
            }
        }
    }

    fun updateMessageText(text: String) {
        _uiState.value = _uiState.value.copy(messageText = text)

        // Send typing indicator when user types
        if (currentUser != null && currentChatRoomId.isNotEmpty()) {
            val isTyping = text.isNotEmpty()
            chatRepository.sendTypingIndicator(currentChatRoomId, currentUser.id, isTyping)
        }
    }

    // Voice recording methods - disabled for MVP (Phase 5)
    // TODO: Re-enable in Phase 5 when voice features are restored
    fun startVoiceRecording() {
        _uiState.value = _uiState.value.copy(error = "Voice recording not available in MVP")
        Log.d("ChatViewModel", "Voice recording disabled for MVP")
    }

    fun stopVoiceRecording() {
        Log.d("ChatViewModel", "Voice recording disabled for MVP")
    }

    // private fun handleVoiceRecordingCompleted(audioFile: File) {
    //     _uiState.value = _uiState.value.copy(isRecording = false)
    //     Log.d("ChatViewModel", "Voice recording completed: ${audioFile.absolutePath}, size: ${audioFile.length()}")
    //
    //     if (!audioFile.exists() || audioFile.length() == 0L) {
    //         _uiState.value = _uiState.value.copy(error = "Recording failed: Empty or missing audio file")
    //         return
    //     }
    //
    //     // Calculate approximate duration (rough estimate: file_size_bytes / 1000 for minimum 1 second)
    //     val estimatedDuration = (audioFile.length() / 1000).coerceAtLeast(1000)
    //
    //     // Send voice message
    //     sendVoiceMessage(audioFile, estimatedDuration)
    // }

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
                        .filter { it.senderId != user.id && user.id !in it.readBy }
                        .forEach { message ->
                            chatRepository.markMessageAsRead(message.id, user.id)
                        }
                } catch (e: Exception) {
                    // Handle silently
                }
            }
        }
    }

    // Message action methods
    fun showMessageContextMenu(message: Message) {
        _uiState.value = _uiState.value.copy(
            selectedMessage = message,
            showMessageContextMenu = true
        )
    }

    fun hideMessageContextMenu() {
        _uiState.value = _uiState.value.copy(
            showMessageContextMenu = false
        )
    }

    fun showEditDialog() {
        _uiState.value = _uiState.value.copy(
            showMessageContextMenu = false,
            showEditDialog = true
        )
    }

    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            selectedMessage = null
        )
    }

    fun showDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showMessageContextMenu = false,
            showDeleteDialog = true
        )
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            selectedMessage = null
        )
    }

    fun showReactionPicker() {
        _uiState.value = _uiState.value.copy(
            showMessageContextMenu = false,
            showReactionPicker = true
        )
    }

    fun hideReactionPicker() {
        _uiState.value = _uiState.value.copy(
            showReactionPicker = false,
            selectedMessage = null
        )
    }

    fun editMessage(newContent: String) {
        val messageId = _uiState.value.selectedMessage?.id ?: return

        viewModelScope.launch {
            try {
                val result = chatRepository.editMessage(messageId, newContent)
                result.fold(
                    onSuccess = {
                        Log.d("ChatViewModel", "Message edited successfully")
                        hideEditDialog()
                    },
                    onFailure = { exception ->
                        Log.e("ChatViewModel", "Failed to edit message", exception)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to edit message: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error editing message", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to edit message: ${e.message}"
                )
            }
        }
    }

    fun deleteMessage() {
        val messageId = _uiState.value.selectedMessage?.id ?: return

        viewModelScope.launch {
            try {
                val result = chatRepository.deleteMessage(messageId)
                result.fold(
                    onSuccess = {
                        Log.d("ChatViewModel", "Message deleted successfully")
                        hideDeleteDialog()
                    },
                    onFailure = { exception ->
                        Log.e("ChatViewModel", "Failed to delete message", exception)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to delete message: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error deleting message", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete message: ${e.message}"
                )
            }
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val userId = currentUser?.id ?: return

        viewModelScope.launch {
            try {
                val result = chatRepository.toggleReaction(messageId, userId, emoji)
                result.fold(
                    onSuccess = {
                        Log.d("ChatViewModel", "Reaction toggled successfully")
                    },
                    onFailure = { exception ->
                        Log.e("ChatViewModel", "Failed to toggle reaction", exception)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to react to message: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error toggling reaction", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to react to message: ${e.message}"
                )
            }
        }
    }

    // Real-time subscription methods

    private fun startRealtimeSubscription(chatRoomId: String) {
        // Cancel previous subscriptions
        messageEventsJob?.cancel()
        typingEventsJob?.cancel()

        // Start realtime subscriptions
        chatRepository.startRealtimeSubscription(chatRoomId)

        // Listen for realtime message events
        messageEventsJob = viewModelScope.launch {
            chatRepository.getMessageEvents().collect { event ->
                when (event) {
                    is com.example.kosmos.data.realtime.MessageEvent.Insert -> {
                        Log.d("ChatViewModel", "Realtime: New message received")
                        // Message is already inserted into Room by RealtimeManager
                        // The getMessagesFlow will automatically update the UI
                    }
                    is com.example.kosmos.data.realtime.MessageEvent.Update -> {
                        Log.d("ChatViewModel", "Realtime: Message updated")
                        // Message is already updated in Room by RealtimeManager
                    }
                    is com.example.kosmos.data.realtime.MessageEvent.Delete -> {
                        Log.d("ChatViewModel", "Realtime: Message deleted")
                        // Message is already deleted from Room by RealtimeManager
                    }
                }
            }
        }

        // Listen for typing indicator events
        typingEventsJob = viewModelScope.launch {
            chatRepository.getTypingEvents().collect { event ->
                if (event.chatRoomId == chatRoomId && event.userId != currentUser?.id) {
                    Log.d("ChatViewModel", "User ${event.userId} is typing: ${event.isTyping}")
                    updateTypingIndicator(event.userId, event.isTyping)
                }
            }
        }
    }

    private fun updateTypingIndicator(userId: String, isTyping: Boolean) {
        _uiState.value = _uiState.value.copy(
            typingUsers = if (isTyping) {
                _uiState.value.typingUsers + userId
            } else {
                _uiState.value.typingUsers - userId
            }
        )
    }

    override fun onCleared() {
        super.onCleared()

        // Cancel all flow collection jobs
        chatRoomJob?.cancel()
        messagesJob?.cancel()
        messageEventsJob?.cancel()
        typingEventsJob?.cancel()

        // Stop realtime subscriptions
        if (currentChatRoomId.isNotEmpty()) {
            chatRepository.stopRealtimeSubscription(currentChatRoomId)
        }

        // Voice recording cleanup disabled for MVP (Phase 5)
        // voiceRecordingHelper?.cancelRecording()
        // currentRecordingJob?.cancel()
        Log.d("ChatViewModel", "ViewModel cleared")
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
    val error: String? = null,
    // Message actions
    val selectedMessage: Message? = null,
    val showMessageContextMenu: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showReactionPicker: Boolean = false,
    // Typing indicators
    val typingUsers: Set<String> = emptySet()
)