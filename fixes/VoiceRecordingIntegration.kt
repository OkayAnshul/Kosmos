// Add to ChatViewModel in Chat.kt

class ChatViewModel @Inject constructor(
    // ... existing dependencies
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var voiceRecordingHelper: VoiceRecordingHelper? = null

    init {
        voiceRecordingHelper = VoiceRecordingHelper(context)
    }

    fun startVoiceRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true)

        viewModelScope.launch {
            voiceRecordingHelper?.startRecording()?.collect { state ->
                when (state) {
                    is VoiceRecordingState.Recording -> {
                        // Recording started successfully
                    }
                    is VoiceRecordingState.Completed -> {
                        handleVoiceRecordingCompleted(state.audioFile)
                    }
                    is VoiceRecordingState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Voice recording failed: ${state.message}",
                            isRecording = false
                        )
                    }
                }
            }
        }
    }

    fun stopVoiceRecording() {
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
            }
            else -> {
                _uiState.value = _uiState.value.copy(isRecording = false)
            }
        }
    }

    private fun handleVoiceRecordingCompleted(audioFile: File) {
        _uiState.value = _uiState.value.copy(isRecording = false)

        // Calculate duration
        val duration = calculateAudioDuration(audioFile) // Implement this

        // Send voice message
        sendVoiceMessage(audioFile, duration)
    }
}

// Add to MessageInput composable - replace existing voice recording logic:
@Composable
private fun MessageInput(
    // ... existing parameters
    onPermissionRequest: () -> Unit
) {
    // Add permission check before voice recording
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // ... existing UI code

    // Voice/Send button modification:
    if (messageText.isBlank()) {
        FloatingActionButton(
            onClick = {
                if (micPermissionState.status.isGranted) {
                    if (isRecording) onStopVoiceRecording() else onStartVoiceRecording()
                } else {
                    micPermissionState.launchPermissionRequest()
                }
            },
            // ... existing styling
        ) {
            if (isSendingVoice) {
                CircularProgressIndicator()
            } else {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop recording" else "Record voice message"
                )
            }
        }
    }
}