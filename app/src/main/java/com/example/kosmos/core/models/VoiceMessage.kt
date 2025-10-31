package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "voice_messages")
data class VoiceMessage(
    @PrimaryKey
    val id: String = "",

    @SerialName("message_id")
    val messageId: String = "",

    @SerialName("audio_url")
    val audioUrl: String = "",

    @SerialName("duration_seconds")
    val duration: Long = 0L, // in milliseconds

    val transcription: String? = null,

    @SerialName("transcription_confidence")
    val transcriptionConfidence: Float = 0f,

    @SerialName("is_transcribing")
    val isTranscribing: Boolean = false,

    @SerialName("transcription_error")
    val transcriptionError: String? = null,

    @SerialName("action_items")
    val actionItems: List<String> = emptyList(), // ActionItem IDs

    @SerialName("waveform_data")
    val waveform: List<Float> = emptyList(), // For waveform visualization

    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis()
)