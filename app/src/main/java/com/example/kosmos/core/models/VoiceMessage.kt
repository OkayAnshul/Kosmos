package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "voice_messages")
data class VoiceMessage(
    @PrimaryKey
    val id: String = "",
    val messageId: String = "",
    val audioUrl: String = "",
    val duration: Long = 0L, // in milliseconds
    val transcription: String? = null,
    val transcriptionConfidence: Float = 0f,
    val isTranscribing: Boolean = false,
    val transcriptionError: String? = null,
    val actionItems: List<String> = emptyList(), // ActionItem IDs
    val waveform: List<Float> = emptyList(), // For waveform visualization
    val createdAt: Long = System.currentTimeMillis()
)