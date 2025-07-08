package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.VoiceMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceMessageDao {
    @Query("SELECT * FROM voice_messages WHERE id = :voiceMessageId")
    suspend fun getVoiceMessageById(voiceMessageId: String): VoiceMessage?

    @Query("SELECT * FROM voice_messages WHERE id = :voiceMessageId")
    fun getVoiceMessageByIdFlow(voiceMessageId: String): Flow<VoiceMessage?>

    @Query("SELECT * FROM voice_messages WHERE messageId = :messageId")
    suspend fun getVoiceMessageByMessageId(messageId: String): VoiceMessage?

    @Query("SELECT * FROM voice_messages WHERE isTranscribing = 1")
    suspend fun getPendingTranscriptions(): List<VoiceMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceMessage(voiceMessage: VoiceMessage)

    @Update
    suspend fun updateVoiceMessage(voiceMessage: VoiceMessage)

    @Delete
    suspend fun deleteVoiceMessage(voiceMessage: VoiceMessage)

    @Query("DELETE FROM voice_messages WHERE id = :voiceMessageId")
    suspend fun deleteVoiceMessageById(voiceMessageId: String)
}