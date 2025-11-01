package com.example.kosmos.data.datasource

import android.util.Log
import com.example.kosmos.core.models.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for message operations using Supabase Postgrest
 * Handles CRUD operations, reactions, and read receipts for messages
 */
@Singleton
class SupabaseMessageDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseMessageDataSource"
        private const val TABLE_NAME = "messages"
    }

    /**
     * Insert a new message into Supabase
     * @param message Message to insert
     * @return Result with inserted message or error
     */
    suspend fun insertMessage(message: Message): Result<Message> {
        return try {
            supabase.from(TABLE_NAME)
                .insert(message)
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting message", e)
            Result.failure(e)
        }
    }

    /**
     * Update message content (for editing)
     * @param messageId Message ID to update
     * @param content New message content
     * @param editedAt Timestamp of edit
     * @return Result with Unit or error
     */
    suspend fun updateMessage(
        messageId: String,
        content: String,
        editedAt: Long
    ): Result<Unit> {
        return try {
            // Use UpdateBuilder DSL to avoid "Serializer for class 'Any'" error
            // Mixed types (String, Boolean, Long) require explicit type safety
            supabase.from(TABLE_NAME)
                .update({
                    set("content", content)
                    set("is_edited", true)
                    set("edited_at", editedAt)
                }) {
                    filter {
                        eq("id", messageId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating message: id=$messageId", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a message from Supabase
     * @param messageId ID of message to delete
     * @return Result with Unit or error
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME)
                .delete {
                    filter {
                        eq("id", messageId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message: id=$messageId", e)
            Result.failure(e)
        }
    }

    /**
     * Get messages for a chat room with pagination
     * @param chatRoomId Chat room ID
     * @param limit Maximum number of messages to fetch
     * @param before Timestamp cursor for pagination (fetch messages before this time)
     * @return Result with list of messages or error
     */
    suspend fun getMessages(
        chatRoomId: String,
        limit: Int = 50,
        before: Long? = null
    ): Result<List<Message>> {
        return try {
            val messages = supabase.from(TABLE_NAME)
                .select() {
                    filter {
                        eq("chat_room_id", chatRoomId)

                        // Pagination: fetch messages before cursor timestamp
                        if (before != null) {
                            lt("timestamp", before)
                        }
                    }

                    // Order by newest first
                    order(column = "timestamp", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)

                    // Limit results
                    limit(limit.toLong())
                }
                .decodeList<Message>()

            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching messages: chatRoomId=$chatRoomId", e)
            Result.failure(e)
        }
    }

    /**
     * Mark a message as read by a user
     * Adds userId to the readBy list if not already present
     * @param messageId Message ID
     * @param userId User ID who read the message
     * @return Result with Unit or error
     */
    suspend fun markAsRead(messageId: String, userId: String): Result<Unit> {
        return try {
            // Fetch current message to get readBy list
            val message = supabase.from(TABLE_NAME)
                .select() {
                    filter {
                        eq("id", messageId)
                    }
                }
                .decodeSingleOrNull<Message>()

            if (message != null && !message.readBy.contains(userId)) {
                // Add userId to readBy list
                val updatedReadBy = message.readBy + userId

                supabase.from(TABLE_NAME)
                    .update(mapOf("read_by" to updatedReadBy)) {
                        filter {
                            eq("id", messageId)
                        }
                    }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message as read: id=$messageId, userId=$userId", e)
            Result.failure(e)
        }
    }

    /**
     * Mark multiple messages as read in batch
     * More efficient than calling markAsRead for each message
     * @param messageIds List of message IDs
     * @param userId User ID who read the messages
     * @return Result with Unit or error
     */
    suspend fun markMessagesAsRead(messageIds: List<String>, userId: String): Result<Unit> {
        return try {
            // Fetch current messages
            val messages = supabase.from(TABLE_NAME)
                .select() {
                    filter {
                        isIn("id", messageIds)
                    }
                }
                .decodeList<Message>()

            // Update each message that user hasn't read yet
            messages.forEach { message ->
                if (!message.readBy.contains(userId)) {
                    val updatedReadBy = message.readBy + userId

                    supabase.from(TABLE_NAME)
                        .update(mapOf("read_by" to updatedReadBy)) {
                            filter {
                                eq("id", message.id)
                            }
                        }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read: userId=$userId", e)
            Result.failure(e)
        }
    }

    /**
     * Add a reaction to a message
     * If user already reacted with different emoji, it replaces it
     * @param messageId Message ID
     * @param userId User ID adding reaction
     * @param emoji Emoji reaction (e.g., "👍", "❤️")
     * @return Result with Unit or error
     */
    suspend fun addReaction(
        messageId: String,
        userId: String,
        emoji: String
    ): Result<Unit> {
        return try {
            // Fetch current message to get reactions map
            val message = supabase.from(TABLE_NAME)
                .select() {
                    filter {
                        eq("id", messageId)
                    }
                }
                .decodeSingleOrNull<Message>()

            if (message != null) {
                // Add or update user's reaction
                val updatedReactions = message.reactions.toMutableMap()
                updatedReactions[userId] = emoji

                supabase.from(TABLE_NAME)
                    .update(mapOf("reactions" to updatedReactions)) {
                        filter {
                            eq("id", messageId)
                        }
                    }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding reaction: messageId=$messageId, emoji=$emoji", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a user's reaction from a message
     * @param messageId Message ID
     * @param userId User ID removing reaction
     * @return Result with Unit or error
     */
    suspend fun removeReaction(messageId: String, userId: String): Result<Unit> {
        return try {
            // Fetch current message to get reactions map
            val message = supabase.from(TABLE_NAME)
                .select() {
                    filter {
                        eq("id", messageId)
                    }
                }
                .decodeSingleOrNull<Message>()

            if (message != null) {
                // Remove user's reaction
                val updatedReactions = message.reactions.toMutableMap()
                updatedReactions.remove(userId)

                supabase.from(TABLE_NAME)
                    .update(mapOf("reactions" to updatedReactions)) {
                        filter {
                            eq("id", messageId)
                        }
                    }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing reaction: messageId=$messageId, userId=$userId", e)
            Result.failure(e)
        }
    }

    /**
     * Batch insert multiple messages
     * Useful for initial data sync or bulk operations
     * @param messages List of messages to insert
     * @return Result with inserted messages or error
     */
    suspend fun insertAll(messages: List<Message>): Result<List<Message>> {
        return try {
            supabase.from(TABLE_NAME)
                .insert(messages)
            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Error batch inserting messages", e)
            Result.failure(e)
        }
    }
}
