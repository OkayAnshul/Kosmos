package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String = "",
    val chatRoomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String? = null,
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val voiceMessageId: String? = null,
    val taskIds: List<String> = emptyList(), // Tasks created from this message
    val replyToMessageId: String? = null,
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val reactions: Map<String, String> = emptyMap(), // userId -> emoji
    val readBy: List<String> = emptyList() // userIds who read this message
)

@Serializable
enum class MessageType {
    TEXT, VOICE, IMAGE, FILE, SYSTEM, TASK_CREATED
}