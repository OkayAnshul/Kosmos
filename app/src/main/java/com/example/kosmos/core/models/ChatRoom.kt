package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
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
    val projectId: String = "",

    val name: String = "",
    val description: String = "",
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
    val participantIds: List<String> = emptyList(),

    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageId: String? = null,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val isTaskBoardEnabled: Boolean = true,

    /**
     * Whether this room is archived
     */
    val isArchived: Boolean = false,

    /**
     * Whether this is a private/invitation-only room
     */
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