package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "chat_rooms")
data class ChatRoom(
    @PrimaryKey
    val id: String = "",

    /**
     * Project this chat room belongs to
     * Chat rooms are now always associated with a project
     */
    @SerialName("project_id")
    val projectId: String = "",

    val name: String = "",
    val description: String = "",

    @SerialName("image_url")
    val imageUrl: String? = null,

    /**
     * Type of chat room
     */
    val type: ChatRoomType = ChatRoomType.GENERAL,

    /**
     * List of participant user IDs
     * Note: Project members automatically have access based on permissions
     * This list is for direct/private rooms or specific channels
     */
    @SerialName("participant_ids")
    val participantIds: List<String> = emptyList(),

    @SerialName("created_by")
    val createdBy: String = "",

    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @SerialName("last_message_id")
    val lastMessageId: String? = null,

    @SerialName("last_message")
    val lastMessage: String = "",

    @SerialName("last_message_timestamp")
    val lastMessageTimestamp: Long = System.currentTimeMillis(),

    @SerialName("is_task_board_enabled")
    val isTaskBoardEnabled: Boolean = true,

    /**
     * Whether this room is archived
     */
    @SerialName("is_archived")
    val isArchived: Boolean = false,

    /**
     * Whether this is a private/invitation-only room
     */
    @SerialName("is_private")
    val isPrivate: Boolean = false
)

/**
 * Chat room type enum
 */
@Serializable
enum class ChatRoomType {
    /**
     * General project discussion room (default)
     */
    GENERAL,

    /**
     * Direct message between two users
     */
    DIRECT,

    /**
     * Topic-specific channel within project
     */
    CHANNEL,

    /**
     * Task-specific discussion room
     */
    TASK_DISCUSSION,

    /**
     * Announcement-only room (limited posting)
     */
    ANNOUNCEMENTS
}