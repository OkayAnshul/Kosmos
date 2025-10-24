package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "action_items")
data class ActionItem(
    @PrimaryKey
    val id: String = "",
    val messageId: String? = null,
    val voiceMessageId: String? = null,
    val chatRoomId: String = "",
    val type: ActionType = ActionType.TASK,
    val text: String = "",
    val extractedText: String = "", // The specific text that was detected
    val confidence: Float = 0f,
    val isProcessed: Boolean = false,
    val taskId: String? = null, // If converted to task
    val reminderTime: Long? = null,
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