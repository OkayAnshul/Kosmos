package com.example.kosmos.data.repository

import com.example.kosmos.core.database.dao.ChatRoomDao
import com.example.kosmos.core.database.dao.MessageDao
import com.example.kosmos.core.models.ChatRoom
import com.example.kosmos.core.models.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling chat operations
 * Manages chat rooms, messages, and real-time updates
 */
@Singleton
class ChatRepository @Inject constructor(
    private val chatRoomDao: ChatRoomDao,
    private val messageDao: MessageDao,
    private val firestore: FirebaseFirestore
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
     * @param message Message to send
     * @return Result with message ID or error
     */
    suspend fun sendMessage(message: Message): Result<String> {
        return try {
            val messageId = if (message.id.isBlank()) {
                firestore.collection("messages").document().id
            } else {
                message.id
            }

            val messageWithId = message.copy(id = messageId)

            // Save locally first (optimistic update)
            messageDao.insertMessage(messageWithId)

            // Send to Firestore
            firestore.collection("messages")
                .document(messageId)
                .set(messageWithId)
                .await()

            // Update chat room last message timestamp
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
                firestore.collection("chatRooms").document().id
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
}