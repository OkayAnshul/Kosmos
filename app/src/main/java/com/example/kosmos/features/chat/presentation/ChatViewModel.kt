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
import com.example.kosmos.services.TranscriptionWorkerService
import com.example.kosmos.features.voice.services.VoiceRecordingHelper
import com.example.kosmos.features.voice.services.VoiceRecordingState
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

    // Voice recording components
    private var voiceRecordingHelper: VoiceRecordingHelper? = null
    private var currentRecordingJob: Job? = null

    init {
        voiceRecordingHelper = VoiceRecordingHelper(context)
    }

    fun loadChat(chatRoomId: String) {
        currentChatRoomId = chatRoomId
        loadChatRoom(chatRoomId)
        loadMessages(chatRoomId)
    }

    private fun loadChatRoom(chatRoomId: String) {
        viewModelScope.launch {
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
                            senderId = currentUser.uid,
                            senderName = currentUser.displayName ?: "Unknown",
                            senderPhotoUrl = currentUser.photoUrl?.toString(),
                            content = "🎤 Voice message",
                            timestamp = System.currentTimeMillis(),
                            type = MessageType.VOICE,
                            voiceMessageId = savedVoiceMessageId
                        )

                        chatRepository.sendMessage(message)

                        // Start background transcription
                        TranscriptionWorkerService.startService(context)

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
    }

    fun startVoiceRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true, error = null)
        Log.d("ChatViewModel", "Starting voice recording")

        currentRecordingJob = viewModelScope.launch {
            try {
                voiceRecordingHelper?.startRecording()?.collect { state ->
                    when (state) {
                        is VoiceRecordingState.Recording -> {
                            Log.d("ChatViewModel", "Recording started: ${state.outputFile.absolutePath}")
                        }
                        is VoiceRecordingState.Completed -> {
                            handleVoiceRecordingCompleted(state.audioFile)
                        }
                        is VoiceRecordingState.Error -> {
                            _uiState.value = _uiState.value.copy(
                                error = "Voice recording failed: ${state.message}",
                                isRecording = false
                            )
                            Log.e("ChatViewModel", "Recording error: ${state.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start recording: ${e.message}",
                    isRecording = false
                )
                Log.e("ChatViewModel", "Failed to start recording", e)
            }
        }
    }

    fun stopVoiceRecording() {
        Log.d("ChatViewModel", "Stopping voice recording")
        currentRecordingJob?.cancel()

        val result = voiceRecordingHelper?.stopRecording()
        when (result) {
            is VoiceRecordingState.Completed -> {
                handleVoiceRecordingCompleted(result.audioFile)
            }
            is VoiceRecordingState.Error -> {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to stop recording: ${result.message}",
                    isRecording = false
                )
                Log.e("ChatViewModel", "Stop recording error: ${result.message}")
            }
            else -> {
                _uiState.value = _uiState.value.copy(isRecording = false)
            }
        }
    }

    private fun handleVoiceRecordingCompleted(audioFile: File) {
        _uiState.value = _uiState.value.copy(isRecording = false)
        Log.d("ChatViewModel", "Voice recording completed: ${audioFile.absolutePath}, size: ${audioFile.length()}")

        if (!audioFile.exists() || audioFile.length() == 0L) {
            _uiState.value = _uiState.value.copy(error = "Recording failed: Empty or missing audio file")
            return
        }

        // Calculate approximate duration (rough estimate: file_size_bytes / 1000 for minimum 1 second)
        val estimatedDuration = (audioFile.length() / 1000).coerceAtLeast(1000)

        // Send voice message
        sendVoiceMessage(audioFile, estimatedDuration)
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

    override fun onCleared() {
        super.onCleared()
        voiceRecordingHelper?.cancelRecording()
        currentRecordingJob?.cancel()
        Log.d("ChatViewModel", "ViewModel cleared, voice recording cleaned up")
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