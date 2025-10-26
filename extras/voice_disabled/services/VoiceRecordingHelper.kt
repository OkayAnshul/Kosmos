package com.example.kosmos.features.voice.services

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.io.IOException

class VoiceRecordingHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false

    fun startRecording(): Flow<VoiceRecordingState> = callbackFlow {
        try {
            // Create output directory if it doesn't exist
            val outputDir = File(context.cacheDir, "voice_recordings")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            outputFile = File(outputDir, "recording_${System.currentTimeMillis()}.m4a")

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)

                try {
                    prepare()
                    start()
                    isRecording = true
                    trySend(VoiceRecordingState.Recording(outputFile!!))
                } catch (e: IOException) {
                    trySend(VoiceRecordingState.Error("Failed to start recording: ${e.message}"))
                    close(e)
                }
            }

        } catch (e: Exception) {
            trySend(VoiceRecordingState.Error("Recording setup failed: ${e.message}"))
            close(e)
        }

        awaitClose {
            stopRecording()
        }
    }

    fun stopRecording(): VoiceRecordingState {
        return try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false

                outputFile?.let { file ->
                    if (file.exists() && file.length() > 0) {
                        VoiceRecordingState.Completed(file)
                    } else {
                        VoiceRecordingState.Error("Recording file is empty or doesn't exist")
                    }
                } ?: VoiceRecordingState.Error("No output file")
            } else {
                VoiceRecordingState.Error("Not currently recording")
            }
        } catch (e: Exception) {
            try {
                mediaRecorder?.release()
            } catch (releaseException: Exception) {
                // Ignore release exceptions
            }
            mediaRecorder = null
            isRecording = false
            VoiceRecordingState.Error("Failed to stop recording: ${e.message}")
        }
    }

    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
            }
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false

            // Delete the recording file
            outputFile?.delete()
            outputFile = null
        } catch (e: Exception) {
            // Handle silently - just ensure cleanup
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            outputFile?.delete()
            outputFile = null
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}

sealed class VoiceRecordingState {
    data class Recording(val outputFile: File) : VoiceRecordingState()
    data class Completed(val audioFile: File) : VoiceRecordingState()
    data class Error(val message: String) : VoiceRecordingState()
}