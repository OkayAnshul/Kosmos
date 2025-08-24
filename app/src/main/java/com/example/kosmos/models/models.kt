package com.example.kosmos.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "chat_rooms")
data class ChatRoom(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val participantIds: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageId: String? = null,
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val isTaskBoardEnabled: Boolean = true
)

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
    val readBy: Map<String, Long> = emptyMap() // userId -> timestamp
)

@Serializable
enum class MessageType {
    TEXT, VOICE, IMAGE, FILE, SYSTEM, TASK_CREATED
}

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
    val waveform: List<Float> = emptyList() // For waveform visualization
)

@Serializable
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = "",
    val chatRoomId: String = "",
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val assignedToId: String? = null,
    val assignedToName: String? = null,
    val createdById: String = "",
    val createdByName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val sourceMessageId: String? = null, // Message that created this task
    val tags: List<String> = emptyList(),
    val comments: List<TaskComment> = emptyList()
)

@Serializable
enum class TaskStatus {
    TODO, IN_PROGRESS, DONE, CANCELLED
}

@Serializable
enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}

@Serializable
data class TaskComment(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

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