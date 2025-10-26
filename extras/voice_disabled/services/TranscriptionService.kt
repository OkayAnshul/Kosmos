package com.example.kosmos.features.voice.services

import android.content.Context
import android.util.Base64
import com.example.kosmos.BuildConfig
import com.example.kosmos.core.database.dao.VoiceMessageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionService @Inject constructor(
    private val speechToTextService: SpeechToTextService,
    private val voiceMessageDao: VoiceMessageDao,
    private val context: Context
) {

    private val apiKey = BuildConfig.GOOGLE_CLOUD_API_KEY

    suspend fun transcribeVoiceMessage(voiceMessageId: String, audioFilePath: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Mark as transcribing
                val voiceMessage = voiceMessageDao.getVoiceMessageById(voiceMessageId)
                    ?: return@withContext Result.failure(Exception("Voice message not found"))

                voiceMessageDao.updateVoiceMessage(voiceMessage.copy(isTranscribing = true))

                // Read and encode audio file
                val audioFile = File(audioFilePath)
                if (!audioFile.exists()) {
                    return@withContext Result.failure(Exception("Audio file not found"))
                }

                val audioBytes = audioFile.readBytes()
                val encodedAudio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

                // Prepare request
                val request = SpeechRecognitionRequest(
                    config = RecognitionConfig(
                        encoding = "WEBM_OPUS",
                        sampleRateHertz = 48000,
                        languageCode = "en-US",
                        enableAutomaticPunctuation = true
                    ),
                    audio = RecognitionAudio(content = encodedAudio)
                )

                // Call API
                val response = speechToTextService.recognizeSpeech(
                    authorization = "Bearer $apiKey",
                    request = request
                )

                if (response.isSuccessful) {
                    val results = response.body()?.results
                    if (!results.isNullOrEmpty() && results[0].alternatives.isNotEmpty()) {
                        val transcription = results[0].alternatives[0].transcript
                        val confidence = results[0].alternatives[0].confidence

                        // Update voice message with transcription
                        voiceMessageDao.updateVoiceMessage(
                            voiceMessage.copy(
                                transcription = transcription,
                                transcriptionConfidence = confidence,
                                isTranscribing = false
                            )
                        )

                        Result.success(transcription)
                    } else {
                        voiceMessageDao.updateVoiceMessage(
                            voiceMessage.copy(
                                transcription = "",
                                transcriptionError = "No speech detected",
                                isTranscribing = false
                            )
                        )
                        Result.failure(Exception("No speech detected"))
                    }
                } else {
                    val error = "API Error: ${response.code()}"
                    voiceMessageDao.updateVoiceMessage(
                        voiceMessage.copy(
                            transcriptionError = error,
                            isTranscribing = false
                        )
                    )
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                // Update with error
                voiceMessageDao.getVoiceMessageById(voiceMessageId)?.let { voiceMessage ->
                    voiceMessageDao.updateVoiceMessage(
                        voiceMessage.copy(
                            transcriptionError = e.message ?: "Transcription failed",
                            isTranscribing = false
                        )
                    )
                }
                Result.failure(e)
            }
        }
    }

    suspend fun processPendingTranscriptions() {
        withContext(Dispatchers.IO) {
            try {
                val pendingVoiceMessages = voiceMessageDao.getPendingTranscriptions()
                pendingVoiceMessages.forEach { voiceMessage ->
                    if (voiceMessage.audioUrl.isNotEmpty()) {
                        transcribeVoiceMessage(voiceMessage.id, voiceMessage.audioUrl)
                    }
                }
            } catch (e: Exception) {
                // Log error or handle appropriately
            }
        }
    }
}