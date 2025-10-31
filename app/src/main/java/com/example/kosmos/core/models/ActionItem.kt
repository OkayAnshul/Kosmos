package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "action_items")
data class ActionItem(
    @PrimaryKey
    val id: String = "",

    @SerialName("message_id")
    val messageId: String? = null,

    @SerialName("voice_message_id")
    val voiceMessageId: String? = null,

    @SerialName("chat_room_id")
    val chatRoomId: String = "",

    val type: ActionType = ActionType.TASK,
    val text: String = "",

    @SerialName("extracted_text")
    val extractedText: String = "", // The specific text that was detected

    val confidence: Float = 0f,

    @SerialName("is_processed")
    val isProcessed: Boolean = false,

    @SerialName("task_id")
    val taskId: String? = null, // If converted to task

    @SerialName("reminder_time")
    val reminderTime: Long? = null,

    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class ActionType {
    TASK, REMINDER, MEETING, DEADLINE, FOLLOW_UP
}

@Serializable
data class SmartReply(
    val text: String = "",
    val confidence: Float = 0f,
    val type: SmartReplyType = SmartReplyType.GENERAL
)

@Serializable
enum class SmartReplyType {
    GENERAL, CONFIRMATION, QUESTION, TASK_RELATED, MEETING
}