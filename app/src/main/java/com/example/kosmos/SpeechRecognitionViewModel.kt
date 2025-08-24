package com.example.kosmos

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing speech recognition state
 */
class SpeechRecognitionViewModel : androidx.lifecycle.ViewModel() {
    private val _uiState = MutableStateFlow(SpeechUiState())
    val uiState: StateFlow<SpeechUiState> = _uiState.asStateFlow()

    private var speechHelper: SpeechRecognitionHelper? = null

    fun initializeSpeechHelper(helper: SpeechRecognitionHelper) {
        speechHelper = helper
    }

    fun startListening() {
        speechHelper?.let { helper ->
            if (!helper.isSpeechRecognitionAvailable()) {
                _uiState.value = _uiState.value.copy(
                    error = "Speech recognition not available on this device"
                )
                return
            }

            _uiState.value = _uiState.value.copy(isListening = true, error = null)

            helper.startListening().let { flow ->
                // Note: In a real implementation, you'd collect this flow in a coroutine
                // and update the state accordingly. This is simplified for the example.
            }
        }
    }

    fun stopListening() {
        speechHelper?.stopListening()
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    fun handleSpeechResult(result: SpeechResult) {
        when (result) {
            is SpeechResult.Ready -> {
                _uiState.value = _uiState.value.copy(status = "Ready to listen...")
            }
            is SpeechResult.BeginningOfSpeech -> {
                _uiState.value = _uiState.value.copy(status = "Listening...")
            }
            is SpeechResult.EndOfSpeech -> {
                _uiState.value = _uiState.value.copy(
                    status = "Processing...",
                    isListening = false
                )
            }
            is SpeechResult.PartialResult -> {
                _uiState.value = _uiState.value.copy(
                    partialText = result.text,
                    status = "Listening..."
                )
            }
            is SpeechResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    finalText = result.text,
                    confidence = result.confidence,
                    partialText = "",
                    status = "Recognition complete",
                    isListening = false
                )
            }
            is SpeechResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    error = result.message,
                    isListening = false,
                    status = "Error occurred"
                )
            }
            is SpeechResult.RmsChanged -> {
                _uiState.value = _uiState.value.copy(audioLevel = result.rmsdB)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechHelper?.destroy()
    }
}

/**
 * Data class representing the UI state for speech recognition
 */
data class SpeechUiState(
    val isListening: Boolean = false,
    val partialText: String = "",
    val finalText: String = "",
    val confidence: Float = 0f,
    val audioLevel: Float = 0f,
    val status: String = "Tap to start",
    val error: String? = null
)

/**
 * Composable for speech recognition UI
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SpeechRecognitionScreen(
    modifier: Modifier = Modifier,
    viewModel: SpeechRecognitionViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Request microphone permission
    val microphonePermissionState = rememberPermissionState(
        Manifest.permission.RECORD_AUDIO
    )

    // Initialize speech helper
    LaunchedEffect(context) {
        val speechHelper = SpeechRecognitionHelper(context)
        viewModel.initializeSpeechHelper(speechHelper)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Status text
        Text(
            text = uiState.status,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Partial text (real-time results)
        if (uiState.partialText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "Listening: ${uiState.partialText}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Final text with confidence
        if (uiState.finalText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Result: ${uiState.finalText}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (uiState.confidence > 0f) {
                        Text(
                            text = "Confidence: ${(uiState.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Error message
        if (uiState.error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error: ${uiState.error}",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Microphone button
        if (microphonePermissionState.status.isGranted) {
            FloatingActionButton(
                onClick = {
                    if (uiState.isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                },
                containerColor = if (uiState.isListening) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ) {
                Icon(
                    imageVector = if (uiState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (uiState.isListening) "Stop listening" else "Start listening"
                )
            }
        } else {
            // Permission request button
            Button(
                onClick = { microphonePermissionState.launchPermissionRequest() }
            ) {
                Text("Grant Microphone Permission")
            }
        }

        // Audio level indicator (simple progress bar)
        if (uiState.isListening && uiState.audioLevel > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (uiState.audioLevel + 10f) / 20f }, // Normalize audio level
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Audio Level",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}