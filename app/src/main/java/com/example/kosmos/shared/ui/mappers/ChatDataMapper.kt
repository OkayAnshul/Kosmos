package com.example.kosmos.shared.ui.mappers

import com.example.kosmos.core.models.ChatRoom
import com.example.kosmos.core.models.Message
import com.example.kosmos.features.chat.components.MessageReaction
import com.example.kosmos.features.chat.presentation.redesign.ChatMessage
import com.example.kosmos.features.chat.presentation.redesign.ChatRoomItem
import com.example.kosmos.features.chat.presentation.redesign.Reaction
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Mapper functions to convert between domain models and UI models for Chat features
 */
object ChatDataMapper {

    /**
     * Convert Message domain model to ChatMessage UI model
     */
    fun Message.toChatMessage(
        currentUserId: String,
        allUsers: Map<String, String> = emptyMap(),
        allMessages: Map<String, Message> = emptyMap()
    ): ChatMessage {
        return ChatMessage(
            id = this.id,
            content = this.content,
            senderId = this.senderId,
            senderName = this.senderName,
            timestamp = this.timestamp,
            formattedTime = formatTime(this.timestamp),
            formattedDate = formatDate(this.timestamp),
            isEdited = this.isEdited,
            reactions = this.reactions.toReactionList(currentUserId, allUsers),
            readBy = this.readBy,
            replyToMessageId = this.replyToMessageId,
            replyToSenderName = this.replyToMessageId?.let { msgId ->
                allMessages[msgId]?.senderName
            }
        )
    }

    /**
     * Convert reactions map to Reaction list for UI
     */
    private fun Map<String, String>.toReactionList(
        currentUserId: String,
        allUsers: Map<String, String>
    ): List<Reaction> {
        // Group by emoji and count
        val grouped = this.entries.groupBy { it.value }

        return grouped.map { (emoji, entries) ->
            val userIds = entries.map { it.key }
            Reaction(
                emoji = emoji,
                userIds = userIds,
                count = entries.size
            )
        }
    }

    /**
     * Convert MessageReaction (component model) to Reaction (screen model)
     */
    fun MessageReaction.toReaction(): com.example.kosmos.features.chat.presentation.redesign.Reaction {
        return com.example.kosmos.features.chat.presentation.redesign.Reaction(
            emoji = this.emoji,
            userIds = emptyList(), // Not available in component model
            count = this.count
        )
    }

    /**
     * Convert ChatRoom to ChatRoomItem for list display
     */
    fun ChatRoom.toChatRoomItem(
        unreadCount: Int = 0,
        isPinned: Boolean = false,
        isOnline: Boolean = false
    ): ChatRoomItem {
        return ChatRoomItem(
            id = this.id,
            name = this.name,
            lastMessage = this.lastMessage.ifBlank { "No messages yet" },
            timestamp = this.lastMessageTimestamp,
            formattedTimestamp = formatRelativeTime(this.lastMessageTimestamp),
            unreadCount = unreadCount,
            isPinned = isPinned,
            isArchived = this.isArchived,
            isOnline = isOnline,
            participantCount = this.participantIds.size
        )
    }

    /**
     * Format timestamp to time string (HH:mm)
     */
    fun formatTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    /**
     * Format timestamp to date string (MMM dd, yyyy)
     */
    fun formatDate(timestamp: Long): String {
        val today = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            // Today - show "Today"
            isSameDay(today, date) -> "Today"
            // Yesterday - show "Yesterday"
            isYesterday(today, date) -> "Yesterday"
            // This year - show "MMM dd"
            today.get(Calendar.YEAR) == date.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
            // Other years - show "MMM dd, yyyy"
            else -> {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    /**
     * Format relative time for chat list (e.g., "5m", "2h", "3d")
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "${minutes}m"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "${hours}h"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "${days}d"
            }
            else -> {
                // Show actual date for older messages
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    /**
     * Check if two calendars are on the same day
     */
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Check if date is yesterday
     */
    private fun isYesterday(today: Calendar, date: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, date)
    }

    /**
     * Group messages by 5-minute window and same sender
     * Returns list with grouping flags
     */
    fun groupMessages(
        messages: List<ChatMessage>,
        currentUserId: String
    ): List<ChatMessage> {
        if (messages.isEmpty()) return emptyList()

        val grouped = mutableListOf<ChatMessage>()
        var previousMessage: ChatMessage? = null

        messages.forEach { message ->
            val isFirstInGroup = previousMessage == null ||
                    previousMessage.senderId != message.senderId ||
                    (message.timestamp - previousMessage.timestamp) > TimeUnit.MINUTES.toMillis(5)

            grouped.add(message)
            previousMessage = message
        }

        return grouped
    }
}
