package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

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
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val isTaskBoardEnabled: Boolean = true
)