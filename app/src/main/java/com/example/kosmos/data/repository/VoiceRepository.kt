package com.example.kosmos.data.repository

import com.example.kosmos.core.database.dao.VoiceMessageDao
import com.example.kosmos.core.models.VoiceMessage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling voice message operations
 * Manages voice recording, storage, and transcription
 */
@Singleton
class VoiceRepository @Inject constructor(
    private val voiceMessageDao: VoiceMessageDao
) {

    /**
     * Get a voice message by ID
     * @param voiceMessageId Voice message ID
     * @return Flow of VoiceMessage or null
     */
    fun getVoiceMessageByIdFlow(voiceMessageId: String): Flow<VoiceMessage?> {
        return voiceMessageDao.getVoiceMessageByIdFlow(voiceMessageId)
    }

    /**
     * Get voice message by ID (suspend function)
     * @param voiceMessageId Voice message ID
     * @return VoiceMessage or null
     */
    suspend fun getVoiceMessageById(voiceMessageId: String): VoiceMessage? {
        return voiceMessageDao.getVoiceMessageById(voiceMessageId)
    }

    /**
     * Get voice message associated with a text message
     * @param messageId Message ID
     * @return VoiceMessage or null
     */
    suspend fun getVoiceMessageByMessageId(messageId: String): VoiceMessage? {
        return voiceMessageDao.getVoiceMessageByMessageId(messageId)
    }

    /**
     * Save a voice message
     * @param voiceMessage Voice message to save
     * @return Result with voice message ID or error
     */
    suspend fun saveVoiceMessage(voiceMessage: VoiceMessage): Result<String> {
        return try {
            val voiceMessageId = if (voiceMessage.id.isBlank()) {
                java.util.UUID.randomUUID().toString()
            } else {
                voiceMessage.id
            }

            val voiceMessageWithId = voiceMessage.copy(
                id = voiceMessageId,
                createdAt = System.currentTimeMillis()
            )

            voiceMessageDao.insertVoiceMessage(voiceMessageWithId)
            Result.success(voiceMessageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update a voice message
     * @param voiceMessage Voice message to update
     * @return Result indicating success or failure
     */
    suspend fun updateVoiceMessage(voiceMessage: VoiceMessage): Result<Unit> {
        return try {
            voiceMessageDao.updateVoiceMessage(voiceMessage)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update voice message transcription
     * @param voiceMessageId Voice message ID
     * @param transcription Transcription text
     * @param confidence Transcription confidence score
     * @return Result indicating success or failure
     */
    suspend fun updateTranscription(
        voiceMessageId: String,
        transcription: String,
        confidence: Float
    ): Result<Unit> {
        return try {
            val voiceMessage = voiceMessageDao.getVoiceMessageById(voiceMessageId)
                ?: return Result.failure(Exception("Voice message not found"))

            val updatedVoiceMessage = voiceMessage.copy(
                transcription = transcription,
                transcriptionConfidence = confidence,
                isTranscribing = false
            )

            voiceMessageDao.updateVoiceMessage(updatedVoiceMessage)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark voice message as transcribing
     * @param voiceMessageId Voice message ID
     * @return Result indicating success or failure
     */
    suspend fun markAsTranscribing(voiceMessageId: String): Result<Unit> {
        return try {
            val voiceMessage = voiceMessageDao.getVoiceMessageById(voiceMessageId)
                ?: return Result.failure(Exception("Voice message not found"))

            val updatedVoiceMessage = voiceMessage.copy(isTranscribing = true)
            voiceMessageDao.updateVoiceMessage(updatedVoiceMessage)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all voice messages that need transcription
     * @return List of voice messages pending transcription
     */
    suspend fun getPendingTranscriptions(): List<VoiceMessage> {
        return try {
            voiceMessageDao.getPendingTranscriptions()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Delete a voice message
     * @param voiceMessageId Voice message ID
     * @return Result indicating success or failure
     */
    suspend fun deleteVoiceMessage(voiceMessageId: String): Result<Unit> {
        return try {
            voiceMessageDao.deleteVoiceMessageById(voiceMessageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update voice message audio URL after upload
     * @param voiceMessageId Voice message ID
     * @param audioUrl Audio file URL
     * @return Result indicating success or failure
     */
    suspend fun updateAudioUrl(voiceMessageId: String, audioUrl: String): Result<Unit> {
        return try {
            val voiceMessage = voiceMessageDao.getVoiceMessageById(voiceMessageId)
                ?: return Result.failure(Exception("Voice message not found"))

            val updatedVoiceMessage = voiceMessage.copy(audioUrl = audioUrl)
            voiceMessageDao.updateVoiceMessage(updatedVoiceMessage)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}