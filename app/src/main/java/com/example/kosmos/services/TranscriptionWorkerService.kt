package com.example.kosmos.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.kosmos.features.voice.services.TranscriptionService
import com.example.kosmos.data.repository.VoiceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Background service for processing voice message transcriptions
 * This service runs when there are pending voice messages to transcribe
 */
@AndroidEntryPoint
class TranscriptionWorkerService : Service() {

    @Inject
    lateinit var transcriptionService: TranscriptionService

    @Inject
    lateinit var voiceRepository: VoiceRepository

    private var job: Job? = null

    companion object {
        private const val TAG = "TranscriptionWorker"

        /**
         * Start the transcription service to process pending transcriptions
         */
        fun startService(context: Context) {
            val intent = Intent(context, TranscriptionWorkerService::class.java)
            context.startService(intent)
            Log.d(TAG, "Transcription service start requested")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Transcription service created")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Transcription service started with startId: $startId")

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                processPendingTranscriptions()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing transcriptions", e)
            } finally {
                stopSelf(startId)
            }
        }

        // Don't restart if killed
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        Log.d(TAG, "Transcription service destroyed")
    }

    private suspend fun processPendingTranscriptions() {
        try {
            Log.d(TAG, "Starting to process pending transcriptions")

            // Get all voice messages that need transcription
            val pendingVoiceMessages = voiceRepository.getPendingTranscriptions()
            Log.d(TAG, "Found ${pendingVoiceMessages.size} pending transcriptions")

            if (pendingVoiceMessages.isEmpty()) {
                Log.d(TAG, "No pending transcriptions found")
                return
            }

            var successCount = 0
            var failureCount = 0

            // Process each voice message
            pendingVoiceMessages.forEach { voiceMessage ->
                try {
                    if (voiceMessage.audioUrl.isNotEmpty()) {
                        Log.d(TAG, "Processing transcription for voice message: ${voiceMessage.id}")

                        val result = transcriptionService.transcribeVoiceMessage(
                            voiceMessage.id,
                            voiceMessage.audioUrl
                        )

                        if (result.isSuccess) {
                            val transcription = result.getOrNull()
                            Log.d(TAG, "Transcription successful for ${voiceMessage.id}: '$transcription'")
                            successCount++

                            // Notify user if transcription is ready
                            notifyTranscriptionComplete(voiceMessage.id, transcription)
                        } else {
                            val error = result.exceptionOrNull()?.message ?: "Unknown error"
                            Log.e(TAG, "Transcription failed for ${voiceMessage.id}: $error")
                            failureCount++
                        }
                    } else {
                        Log.w(TAG, "Voice message ${voiceMessage.id} has empty audio URL")
                        failureCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception processing voice message ${voiceMessage.id}", e)
                    failureCount++
                }
            }

            Log.d(TAG, "Transcription processing completed. Success: $successCount, Failures: $failureCount")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process pending transcriptions", e)
        }
    }

    private suspend fun notifyTranscriptionComplete(voiceMessageId: String, transcription: String?) {
        // Here you could send a local notification or update the UI
        // For now, just log the completion
        Log.d(TAG, "Transcription completed for $voiceMessageId: ${transcription?.take(50)}...")

        // TODO: You could add FCM notification here if needed
        // val fcmData = mapOf(
        //     "type" to "voice_transcription_complete",
        //     "voiceMessageId" to voiceMessageId,
        //     "transcription" to (transcription ?: "")
        // )
        // sendFCMNotification(fcmData)
    }

    /**
     * Check if there are pending transcriptions that need processing
     */
    suspend fun hasPendingTranscriptions(): Boolean {
        return try {
            val pending = voiceRepository.getPendingTranscriptions()
            pending.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check pending transcriptions", e)
            false
        }
    }
}

/**
 * Extension function to start transcription service easily from anywhere
 */
fun Context.startTranscriptionService() {
    TranscriptionWorkerService.startService(this)
}