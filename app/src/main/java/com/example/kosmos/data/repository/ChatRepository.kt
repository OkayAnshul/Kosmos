package com.example.kosmos.data.repository

import com.example.kosmos.core.database.dao.ChatRoomDao
import com.example.kosmos.core.database.dao.MessageDao
import com.example.kosmos.core.models.ChatRoom
import com.example.kosmos.core.models.Message
import com.example.kosmos.data.datasource.SupabaseMessageDataSource
import com.example.kosmos.data.realtime.SupabaseRealtimeManager
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling chat operations
 * Manages chat rooms, messages, and real-time updates with Supabase
 * Implements hybrid architecture: Local Room cache + Remote Supabase sync
 */
@Singleton
class ChatRepository @Inject constructor(
    private val chatRoomDao: ChatRoomDao,
    private val messageDao: MessageDao,
    private val supabase: SupabaseClient,
    private val supabaseMessageDataSource: SupabaseMessageDataSource,
    private val supabaseChatDataSource: com.example.kosmos.data.datasource.SupabaseChatDataSource,
    private val realtimeManager: SupabaseRealtimeManager
) {

    /**
     * Get all chat rooms for a user with real-time updates
     * @param userId User ID
     * @return Flow of chat room list
     */
    fun getChatRoomsFlow(userId: String): Flow<List<ChatRoom>> {
        return chatRoomDao.getAllChatRoomsFlow().map { rooms ->
            rooms.filter { room -> room.participantIds.contains(userId) }
        }
    }

    /**
     * Get a specific chat room by ID with real-time updates
     * @param chatRoomId Chat room ID
     * @return Flow of ChatRoom or null
     */
    fun getChatRoomByIdFlow(chatRoomId: String): Flow<ChatRoom?> {
        return chatRoomDao.getChatRoomByIdFlow(chatRoomId)
    }

    /**
     * Get messages for a chat room with real-time updates
     * @param chatRoomId Chat room ID
     * @return Flow of message list (newest first)
     */
    fun getMessagesFlow(chatRoomId: String): Flow<List<Message>> {
        return messageDao.getMessagesForChatRoomFlow(chatRoomId)
    }

    /**
     * Send a message to a chat room
     * Hybrid pattern: Save to Room first (optimistic), then sync to Supabase
     * @param message Message to send
     * @return Result with message ID or error
     */
    suspend fun sendMessage(message: Message): Result<String> {
        return try {
            val messageId = if (message.id.isBlank()) {
                java.util.UUID.randomUUID().toString()
            } else {
                message.id
            }

            val messageWithId = message.copy(id = messageId)

            // Step 1: Save locally first (optimistic update)
            messageDao.insertMessage(messageWithId)

            // Step 2: Send to Supabase (async sync)
            val supabaseResult = supabaseMessageDataSource.insertMessage(messageWithId)
            if (supabaseResult.isFailure) {
                // Log error but don't fail - message is saved locally
                android.util.Log.e("ChatRepository", "Failed to sync message to Supabase", supabaseResult.exceptionOrNull())
            }

            // Step 3: Update chat room last message timestamp
            val chatRoom = chatRoomDao.getChatRoomById(message.chatRoomId)
            chatRoom?.let {
                val updatedRoom = it.copy(
                    lastMessageTimestamp = message.timestamp,
                    lastMessage = message.content.take(100)
                )
                chatRoomDao.updateChatRoom(updatedRoom)
            }

            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new chat room
     * @param chatRoom Chat room to create
     * @return Result with chat room ID or error
     */
    suspend fun createChatRoom(chatRoom: ChatRoom): Result<String> {
        return try {
            val chatRoomId = if (chatRoom.id.isBlank()) {
                java.util.UUID.randomUUID().toString()
            } else {
                chatRoom.id
            }

            val chatRoomWithId = chatRoom.copy(
                id = chatRoomId,
                createdAt = System.currentTimeMillis(),
                lastMessageTimestamp = System.currentTimeMillis()
            )

            // Save locally
            chatRoomDao.insertChatRoom(chatRoomWithId)

            // Sync to Supabase
            val supabaseResult = supabaseChatDataSource.insertChatRoom(chatRoomWithId)
            if (supabaseResult.isFailure) {
                android.util.Log.w("ChatRepository", "Failed to sync chat room to Supabase", supabaseResult.exceptionOrNull())
            }

            Result.success(chatRoomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add a user to a chat room
     * @param chatRoomId Chat room ID
     * @param userId User ID to add
     * @return Result indicating success or failure
     */
    suspend fun addUserToChatRoom(chatRoomId: String, userId: String): Result<Unit> {
        return try {
            val chatRoom = chatRoomDao.getChatRoomById(chatRoomId)
                ?: return Result.failure(Exception("Chat room not found"))

            val updatedParticipants = (chatRoom.participantIds + userId).distinct()
            val updatedChatRoom = chatRoom.copy(participantIds = updatedParticipants)

            // Update locally
            chatRoomDao.updateChatRoom(updatedChatRoom)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark a message as read by a user
     * @param messageId Message ID
     * @param userId User ID
     * @return Result indicating success or failure
     */
    suspend fun markMessageAsRead(messageId: String, userId: String): Result<Unit> {
        return try {
            val message = messageDao.getMessageById(messageId)
                ?: return Result.failure(Exception("Message not found"))

            val updatedReadBy = (message.readBy + userId).distinct()
            val updatedMessage = message.copy(readBy = updatedReadBy)

            // Update locally
            messageDao.updateMessage(updatedMessage)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load older messages for pagination
     * @param chatRoomId Chat room ID
     * @param beforeTimestamp Timestamp to load messages before
     * @param limit Number of messages to load
     * @return List of older messages
     */
    suspend fun loadOlderMessages(
        chatRoomId: String,
        beforeTimestamp: Long,
        limit: Int = 20
    ): List<Message> {
        return try {
            messageDao.getMessagesBefore(chatRoomId, beforeTimestamp, limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Delete a chat room and all its messages
     * @param chatRoomId Chat room ID
     * @return Result indicating success or failure
     */
    suspend fun deleteChatRoom(chatRoomId: String): Result<Unit> {
        return try {
            // Delete locally first
            chatRoomDao.deleteChatRoomById(chatRoomId)
            messageDao.deleteMessagesForChatRoom(chatRoomId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Edit a message's content
     * Hybrid pattern: Update Room first (optimistic), then sync to Supabase
     * @param messageId Message ID to edit
     * @param newContent New message content
     * @return Result indicating success or failure
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        return try {
            // Fetch message from local database
            val message = messageDao.getMessageById(messageId)
                ?: return Result.failure(Exception("Message not found"))

            // Update with edited flag and timestamp
            val editedAt = System.currentTimeMillis()
            val updatedMessage = message.copy(
                content = newContent,
                isEdited = true,
                editedAt = editedAt
            )

            // Step 1: Update locally first (optimistic)
            messageDao.updateMessage(updatedMessage)

            // Step 2: Sync to Supabase
            val supabaseResult = supabaseMessageDataSource.updateMessage(
                messageId = messageId,
                content = newContent,
                editedAt = editedAt
            )

            if (supabaseResult.isFailure) {
                android.util.Log.e("ChatRepository", "Failed to sync message edit to Supabase", supabaseResult.exceptionOrNull())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a message
     * Hybrid pattern: Delete from Room first, then sync to Supabase
     * @param messageId Message ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            // Step 1: Delete locally first (optimistic)
            messageDao.deleteMessageById(messageId)

            // Step 2: Sync to Supabase
            val supabaseResult = supabaseMessageDataSource.deleteMessage(messageId)

            if (supabaseResult.isFailure) {
                android.util.Log.e("ChatRepository", "Failed to sync message deletion to Supabase", supabaseResult.exceptionOrNull())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle a reaction on a message
     * If user already reacted with same emoji, removes it
     * If user reacted with different emoji, replaces it
     * If user hasn't reacted, adds the reaction
     * @param messageId Message ID
     * @param userId User ID reacting
     * @param emoji Emoji to react with
     * @return Result indicating success or failure
     */
    suspend fun toggleReaction(messageId: String, userId: String, emoji: String): Result<Unit> {
        return try {
            // Fetch message from local database
            val message = messageDao.getMessageById(messageId)
                ?: return Result.failure(Exception("Message not found"))

            val currentReaction = message.reactions[userId]
            val updatedReactions = message.reactions.toMutableMap()

            if (currentReaction == emoji) {
                // Same emoji - remove reaction
                updatedReactions.remove(userId)

                // Update locally
                val updatedMessage = message.copy(reactions = updatedReactions)
                messageDao.updateMessage(updatedMessage)

                // Sync to Supabase
                supabaseMessageDataSource.removeReaction(messageId, userId)
            } else {
                // Different emoji or no reaction - add/update reaction
                updatedReactions[userId] = emoji

                // Update locally
                val updatedMessage = message.copy(reactions = updatedReactions)
                messageDao.updateMessage(updatedMessage)

                // Sync to Supabase
                supabaseMessageDataSource.addReaction(messageId, userId, emoji)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark all unread messages in a chat room as read
     * @param chatRoomId Chat room ID
     * @param userId User ID marking messages as read
     * @return Result indicating success or failure
     */
    suspend fun markMessagesAsRead(chatRoomId: String, userId: String): Result<Unit> {
        return try {
            // Get all messages in chat room that user hasn't read
            val messages = messageDao.getMessagesForChatRoom(chatRoomId)
            val unreadMessages = messages.filter { !it.readBy.contains(userId) && it.senderId != userId }

            if (unreadMessages.isEmpty()) {
                return Result.success(Unit)
            }

            // Update locally first
            unreadMessages.forEach { message ->
                val updatedReadBy = (message.readBy + userId).distinct()
                val updatedMessage = message.copy(readBy = updatedReadBy)
                messageDao.updateMessage(updatedMessage)
            }

            // Sync to Supabase in batch
            val messageIds = unreadMessages.map { it.id }
            supabaseMessageDataSource.markMessagesAsRead(messageIds, userId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load more messages for pagination
     * @param chatRoomId Chat room ID
     * @param beforeTimestamp Load messages before this timestamp
     * @param limit Number of messages to load
     * @return Result with list of messages or error
     */
    suspend fun loadMoreMessages(
        chatRoomId: String,
        beforeTimestamp: Long,
        limit: Int = 50
    ): Result<List<Message>> {
        return try {
            // Fetch from Supabase
            val supabaseResult = supabaseMessageDataSource.getMessages(
                chatRoomId = chatRoomId,
                limit = limit,
                before = beforeTimestamp
            )

            if (supabaseResult.isSuccess) {
                val messages = supabaseResult.getOrNull() ?: emptyList()

                // Cache in Room
                messages.forEach { message ->
                    messageDao.insertMessage(message)
                }

                Result.success(messages)
            } else {
                // Fallback to local cache
                val localMessages = messageDao.getMessagesBefore(chatRoomId, beforeTimestamp, limit)
                Result.success(localMessages)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start real-time subscriptions for a chat room
     * Listens for INSERT, UPDATE, DELETE events on messages
     * @param chatRoomId Chat room ID to subscribe to
     */
    fun startRealtimeSubscription(chatRoomId: String) {
        realtimeManager.subscribeToMessages(chatRoomId)
        realtimeManager.subscribeToTypingIndicators(chatRoomId)
    }

    /**
     * Stop real-time subscriptions for a chat room
     * @param chatRoomId Chat room ID to unsubscribe from
     */
    fun stopRealtimeSubscription(chatRoomId: String) {
        realtimeManager.unsubscribeFromMessages(chatRoomId)
    }

    /**
     * Get the message events flow from realtime manager
     * Subscribe to this to receive live message updates
     */
    fun getMessageEvents() = realtimeManager.messageEvents

    /**
     * Get the typing events flow from realtime manager
     * Subscribe to this to receive typing indicator updates
     */
    fun getTypingEvents() = realtimeManager.typingEvents

    /**
     * Send typing indicator to other users in the chat room
     * @param chatRoomId Chat room ID
     * @param userId User ID who is typing
     * @param isTyping Whether the user is currently typing
     */
    fun sendTypingIndicator(chatRoomId: String, userId: String, isTyping: Boolean) {
        realtimeManager.sendTypingIndicator(chatRoomId, userId, isTyping)
    }

    /**
     * Disconnect all realtime subscriptions
     * Call this when the user logs out or app is closing
     */
    fun disconnectRealtime() {
        realtimeManager.disconnect()
    }
}