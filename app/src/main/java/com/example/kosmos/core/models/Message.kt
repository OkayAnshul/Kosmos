package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String = "",

    @SerialName("chat_room_id")
    val chatRoomId: String = "",

    @SerialName("sender_id")
    val senderId: String = "",

    @SerialName("sender_name")
    val senderName: String = "",

    @SerialName("sender_photo_url")
    val senderPhotoUrl: String? = null,

    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,

    @SerialName("voice_message_id")
    val voiceMessageId: String? = null,

    @SerialName("task_ids")
    val taskIds: List<String> = emptyList(), // Tasks created from this message

    @SerialName("reply_to_message_id")
    val replyToMessageId: String? = null,

    @SerialName("is_edited")
    val isEdited: Boolean = false,

    @SerialName("edited_at")
    val editedAt: Long? = null,

    val reactions: Map<String, String> = emptyMap(), // userId -> emoji

    @SerialName("read_by")
    val readBy: List<String> = emptyList() // userIds who read this message
)

@Serializable
enum class MessageType {
    TEXT, VOICE, IMAGE, FILE, SYSTEM, TASK_CREATED
}