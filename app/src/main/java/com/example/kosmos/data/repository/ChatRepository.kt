package com.example.kosmos.data.repository

import android.util.Log
import com.example.kosmos.core.database.dao.ChatRoomDao
import com.example.kosmos.core.database.dao.MessageDao
import com.example.kosmos.core.models.ChatRoom
import com.example.kosmos.core.models.Message
import com.example.kosmos.core.models.SyncOperation
import com.example.kosmos.data.datasource.SupabaseMessageDataSource
import com.example.kosmos.data.realtime.SupabaseRealtimeManager
import com.example.kosmos.data.sync.SyncRetryHelper
import com.example.kosmos.data.sync.SyncQueueHelper
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
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
    private val projectDao: com.example.kosmos.core.database.dao.ProjectDao,
    private val supabase: SupabaseClient,
    private val supabaseMessageDataSource: SupabaseMessageDataSource,
    private val supabaseChatDataSource: com.example.kosmos.data.datasource.SupabaseChatDataSource,
    private val realtimeManager: SupabaseRealtimeManager,
    private val networkMonitor: com.example.kosmos.shared.utils.NetworkMonitor,  // P0-06 FIX
    private val syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao,  // P0-08 FIX
    private val fkRetryQueue: com.example.kosmos.data.sync.FKRetryQueue  // NEW: FK violation retry queue
) {

    companion object {
        private const val TAG = "ChatRepository"
    }

    /**
     * P0-06 FIX: Expose network connectivity state
     * UI can observe this to show offline banner
     */
    val isOffline: kotlinx.coroutines.flow.StateFlow<Boolean> = networkMonitor.isOffline

    /**
     * Get chat rooms for a specific project with real-time updates
     *
     * FIXED LOGIC (2026-01-26):
     * - Public/general rooms (isPrivate = false): Visible to ALL project members
     * - Private rooms (isPrivate = true): Only visible if user is in participantIds
     *
     * Previous bug: Always checked participantIds.contains(userId), which excluded
     * public/general rooms with empty participantIds list.
     *
     * @param userId User ID (to check private room access)
     * @param projectId Project ID (shows only rooms in this project)
     * @return Flow of chat room list for the project
     */
    fun getChatRoomsForProject(userId: String, projectId: String): Flow<List<ChatRoom>> {
        return chatRoomDao.getAllChatRoomsFlow().map { rooms ->
            rooms.filter { room ->
                // Must be in the same project
                if (room.projectId != projectId) return@filter false

                // Public/general rooms: Show to all project members
                if (!room.isPrivate) return@filter true

                // Private rooms: Only show if user is explicitly in participantIds
                room.participantIds.contains(userId)
            }
        }
    }

    /**
     * Get all chat rooms for a user across all projects (legacy method)
     * @param userId User ID
     * @return Flow of chat room list
     */
    @Deprecated("Use getChatRoomsForProject() to avoid showing all chats in every project")
    fun getChatRoomsFlow(userId: String): Flow<List<ChatRoom>> {
        return chatRoomDao.getAllChatRoomsFlow().map { rooms ->
            rooms.filter { room -> room.participantIds.contains(userId) }
        }
    }

    /**
     * Sync chat rooms for a user from Supabase to local cache
     * Fetches all chat rooms where the user is a participant
     *
     * CRITICAL: This fixes the bug where chat rooms are never fetched from Supabase.
     * Call this on app startup, login, or pull-to-refresh.
     *
     * @param userId User ID
     * @return Result indicating success or failure
     */
    @Deprecated(
        message = "Use project-centric sync via InitialSyncManager instead. " +
                  "This method fetches ALL chat rooms globally and filters client-side (inefficient).",
        replaceWith = ReplaceWith("InitialSyncManager.syncAllData(userId)"),
        level = DeprecationLevel.WARNING
    )
    suspend fun syncUserChatRooms(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Starting chat room sync for user: $userId")

            // CRITICAL FIX: Wrap HTTP call in NonCancellable to prevent mid-flight cancellation
            val chatRoomsResult = withContext(NonCancellable) {
                supabaseChatDataSource.getChatRoomsForUser(userId)
            }

            if (chatRoomsResult.isFailure) {
                Log.w(TAG, "Failed to fetch chat rooms from Supabase", chatRoomsResult.exceptionOrNull())
                return chatRoomsResult.map { }  // Convert to Result<Unit>
            }

            val chatRooms = chatRoomsResult.getOrNull() ?: emptyList()
            var chatRoomsSaved = 0
            var chatRoomsFailed = 0

            // Update local cache with granular error handling
            chatRooms.forEach { chatRoom ->
                try {
                    chatRoomDao.insertChatRoom(chatRoom)
                    chatRoomsSaved++
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "⚠️ Chat room insert cancelled for ${chatRoom.id}")
                    throw e  // Re-throw at chat room level
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to insert chat room ${chatRoom.id}", e)
                    chatRoomsFailed++
                }
            }

            Log.d(TAG, "✅ Synced $chatRoomsSaved/${chatRooms.size} chat rooms ($chatRoomsFailed failed)")

            // Also sync recent messages for each chat room (last 50 messages)
            var messagesSynced = 0
            var messageFkErrors = 0
            var messagesCancelled = 0

            chatRooms.forEach { chatRoom ->
                try {
                    // CRITICAL FIX: Wrap HTTP call in NonCancellable to prevent mid-flight cancellation
                    val messagesResult = withContext(NonCancellable) {
                        supabaseMessageDataSource.getMessages(
                            chatRoomId = chatRoom.id,
                            limit = 50,
                            before = null
                        )
                    }

                    if (messagesResult.isSuccess) {
                        val messages = messagesResult.getOrNull() ?: emptyList()
                        messages.forEach { message ->
                            try {
                                messageDao.insertMessage(message)
                                messagesSynced++
                            } catch (e: Exception) {
                                if (com.example.kosmos.data.sync.ForeignKeyErrorHandler.isForeignKeyViolation(e)) {
                                    messageFkErrors++
                                    // Queue message for retry after users sync completes
                                    fkRetryQueue.queueMessageRetry(message)
                                    Log.w(TAG, "FK violation for message ${message.id}, queued for retry (sender: ${message.senderId})")
                                } else {
                                    throw e
                                }
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "⚠️ Message sync cancelled for chat ${chatRoom.id}")
                    messagesCancelled++
                    // DON'T re-throw - continue with next chat
                } catch (e: Exception) {
                    Log.w(TAG, "Error syncing messages for chat ${chatRoom.id}", e)
                }
            }

            if (messageFkErrors > 0 || messagesCancelled > 0) {
                Log.w(TAG, "⚠️ Synced $messagesSynced messages ($messageFkErrors FK errors, $messagesCancelled cancelled)")
            } else {
                Log.d(TAG, "✅ Synced $messagesSynced messages from Supabase")
            }

            Result.success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.w(TAG, "⚠️ Chat sync cancelled (partial data saved)")
            Result.success(Unit)  // Return success - partial data is OK
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error in chat room sync", e)
            Result.failure(e)
        }
    }

    /**
     * Sync all chat rooms for a specific project from Supabase
     * PROJECT-SCOPED: Fetches all rooms in project (not filtered by user participation)
     *
     * This is the project-centric architecture approach that replaces user-centric sync.
     * Benefits:
     * - Server-side filtering by projectId (5.6x faster than client-side)
     * - Complete project data (all rooms, not just user's participated rooms)
     * - Scales to any project size
     * - INCREMENTAL SYNC: Only fetches rooms modified since last sync (50-90% less data)
     *
     * @param projectId Project ID
     * @param since Optional timestamp (milliseconds) - only fetch rooms updated after this time
     * @return Result indicating success or failure
     */
    suspend fun syncProjectChatRooms(projectId: String, since: Long? = null): Result<Unit> {
        return try {
            if (since != null) {
                Log.d(TAG, "Starting incremental chat room sync for project: $projectId (since: $since)")
            } else {
                Log.d(TAG, "Starting full chat room sync for project: $projectId")
            }

            // CRITICAL: Wrap in NonCancellable to prevent mid-flight HTTP cancellation
            val chatRoomsResult = withContext(NonCancellable) {
                supabaseChatDataSource.getChatRoomsForProject(projectId, since)
            }

            if (chatRoomsResult.isFailure) {
                Log.w(TAG, "Failed to fetch chat rooms", chatRoomsResult.exceptionOrNull())
                return chatRoomsResult.map { }
            }

            val chatRooms = chatRoomsResult.getOrNull() ?: emptyList()
            var chatRoomsSaved = 0
            var chatRoomsFailed = 0

            // Save to Room with granular error handling
            chatRooms.forEach { chatRoom ->
                try {
                    chatRoomDao.insertChatRoom(chatRoom)
                    chatRoomsSaved++
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "⚠️ Chat room insert cancelled for ${chatRoom.id}")
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to insert chat room ${chatRoom.id}", e)
                    chatRoomsFailed++
                }
            }

            Log.d(TAG, "✅ Synced $chatRoomsSaved/${chatRooms.size} chat rooms for project $projectId")

            // Sync recent messages for each chat room (last 50)
            var messagesSynced = 0
            var messageFkErrors = 0

            chatRooms.forEach { chatRoom ->
                try {
                    val messagesResult = withContext(NonCancellable) {
                        supabaseMessageDataSource.getMessages(
                            chatRoomId = chatRoom.id,
                            limit = 50,
                            before = null
                        )
                    }

                    if (messagesResult.isSuccess) {
                        val messages = messagesResult.getOrNull() ?: emptyList()
                        messages.forEach { message ->
                            try {
                                messageDao.insertMessage(message)
                                messagesSynced++
                            } catch (e: Exception) {
                                if (com.example.kosmos.data.sync.ForeignKeyErrorHandler.isForeignKeyViolation(e)) {
                                    messageFkErrors++
                                    // Queue message for retry after users sync completes
                                    fkRetryQueue.queueMessageRetry(message)
                                    Log.w(TAG, "FK violation for message ${message.id}, queued for retry (sender: ${message.senderId})")
                                } else {
                                    throw e
                                }
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "⚠️ Message sync cancelled for chat ${chatRoom.id}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error syncing messages for chat ${chatRoom.id}", e)
                }
            }

            Log.d(TAG, "✅ Synced $messagesSynced messages for project $projectId")
            Result.success(Unit)

        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.w(TAG, "⚠️ Chat sync cancelled for project $projectId")
            Result.success(Unit)  // Partial data is OK
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error in chat room sync for project $projectId", e)
            Result.failure(e)
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

            // Step 2: Send to Supabase (async sync) with retry on FK violation
            val supabaseResult = SyncRetryHelper.retryOnForeignKeyViolation(
                maxRetries = 3,
                initialDelayMs = 1000,
                entityName = "message"
            ) {
                supabaseMessageDataSource.insertMessage(messageWithId)
            }

            if (supabaseResult.isFailure) {
                val error = supabaseResult.exceptionOrNull()

                // Use diagnostic message for better error reporting
                val diagnosticMessage = SyncRetryHelper.getDiagnosticMessage(error, "message")
                android.util.Log.e("ChatRepository", "❌ SUPABASE SYNC FAILED for message")
                android.util.Log.e("ChatRepository", diagnosticMessage, error)

                // P0-08 FIX: Queue for automatic retry
                SyncQueueHelper.queueMessage(syncQueueDao, messageWithId, SyncOperation.CREATE)
                android.util.Log.d("ChatRepository", "📥 Message queued for retry: $messageId")

                // Message is still saved locally, so we don't fail the operation
                // But we could add a flag to indicate sync status if needed
            } else {
                android.util.Log.d("ChatRepository", "✅ Message synced to Supabase successfully: $messageId")
            }

            // Step 3: Update chat room last message timestamp
            val chatRoom = chatRoomDao.getChatRoomById(message.chatRoomId)
            chatRoom?.let {
                val updatedRoom = it.copy(
                    lastMessageTimestamp = message.timestamp,
                    lastMessage = message.content.take(100)
                )
                chatRoomDao.updateChatRoom(updatedRoom)

                // Try to sync chat room update as well
                val chatRoomSyncResult = supabaseChatDataSource.updateChatRoom(updatedRoom)
                if (chatRoomSyncResult.isFailure) {
                    android.util.Log.e("ChatRepository", "❌ Failed to sync chat room update to Supabase", chatRoomSyncResult.exceptionOrNull())
                    // P0-08 FIX: Queue for automatic retry
                    SyncQueueHelper.queueChatRoom(syncQueueDao, updatedRoom, SyncOperation.UPDATE)
                    android.util.Log.d("ChatRepository", "📥 Chat room update queued for retry: ${updatedRoom.id}")
                }
            }

            Result.success(messageId)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "❌ CRITICAL: Failed to save message", e)
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

            // Update project chat count if this chat has a project
            chatRoomWithId.projectId?.let { projId ->
                projectDao.incrementChatCount(projId)
            }

            // Sync to Supabase with retry on FK violation (in case project not synced yet)
            val supabaseResult = SyncRetryHelper.retryOnForeignKeyViolation(
                maxRetries = 3,
                initialDelayMs = 1000,
                entityName = "chat_room"
            ) {
                supabaseChatDataSource.insertChatRoom(chatRoomWithId)
            }

            if (supabaseResult.isFailure) {
                val error = supabaseResult.exceptionOrNull()
                val diagnosticMessage = SyncRetryHelper.getDiagnosticMessage(error, "chat room")
                android.util.Log.e("ChatRepository", "❌ SUPABASE SYNC FAILED for chat room")
                android.util.Log.e("ChatRepository", diagnosticMessage, error)
                // P0-08 FIX: Queue for automatic retry
                SyncQueueHelper.queueChatRoom(syncQueueDao, chatRoomWithId, SyncOperation.CREATE)
                android.util.Log.d("ChatRepository", "📥 Chat room queued for retry: $chatRoomId")
            } else {
                android.util.Log.d("ChatRepository", "✅ Chat room synced to Supabase successfully: $chatRoomId")
            }

            Result.success(chatRoomId)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "❌ CRITICAL: Failed to create chat room", e)
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

            // Update locally first (optimistic)
            chatRoomDao.updateChatRoom(updatedChatRoom)

            // Sync participant change to Supabase
            val supabaseResult = supabaseChatDataSource.addParticipant(chatRoomId, userId)
            if (supabaseResult.isFailure) {
                Log.e(TAG, "Failed to sync addParticipant to Supabase, queuing for retry", supabaseResult.exceptionOrNull())
                SyncQueueHelper.queueChatRoom(syncQueueDao, updatedChatRoom, SyncOperation.UPDATE)
            }

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

            // Update locally first (optimistic)
            messageDao.updateMessage(updatedMessage)

            // Sync read receipt to Supabase
            val supabaseResult = supabaseMessageDataSource.markAsRead(messageId, userId)
            if (supabaseResult.isFailure) {
                Log.e(TAG, "Failed to sync markAsRead to Supabase, queuing for retry", supabaseResult.exceptionOrNull())
                SyncQueueHelper.queueMessage(syncQueueDao, updatedMessage, SyncOperation.UPDATE)
            }

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
     * Hybrid pattern: Delete from Supabase first (cascade), then Room
     * @param chatRoomId Chat room ID
     * @return Result indicating success or failure
     */
    suspend fun deleteChatRoom(chatRoomId: String): Result<Unit> {
        return try {
            // Delete from Supabase first (will cascade delete messages via FK)
            val supabaseResult = supabaseChatDataSource.deleteChatRoom(chatRoomId)

            if (supabaseResult.isFailure) {
                android.util.Log.e("ChatRepository", "❌ Failed to delete chat room from Supabase", supabaseResult.exceptionOrNull())
                // Continue to delete locally anyway for offline support
            } else {
                android.util.Log.d("ChatRepository", "✅ Chat room deleted from Supabase: $chatRoomId")
            }

            // Delete locally
            chatRoomDao.deleteChatRoomById(chatRoomId)
            messageDao.deleteMessagesForChatRoom(chatRoomId)

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "❌ Failed to delete chat room", e)
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
                // Bug A fix: queue for retry
                SyncQueueHelper.queueMessage(syncQueueDao, updatedMessage, SyncOperation.UPDATE)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a message
     * Hybrid pattern: Fetch message first, delete from Room, then sync to Supabase
     * @param messageId Message ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            // Bug B fix: Fetch message before deleting so we can queue it if Supabase fails
            val message = messageDao.getMessageById(messageId)

            // Step 1: Delete locally first (optimistic)
            messageDao.deleteMessageById(messageId)

            // Step 2: Sync to Supabase
            val supabaseResult = supabaseMessageDataSource.deleteMessage(messageId)

            if (supabaseResult.isFailure) {
                android.util.Log.e("ChatRepository", "Failed to sync message deletion to Supabase", supabaseResult.exceptionOrNull())
                // Queue for retry only if we have the message data
                message?.let {
                    SyncQueueHelper.queueMessage(syncQueueDao, it, SyncOperation.DELETE)
                }
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

                // Bug C fix: Wrap Supabase call in try-catch (reactions are ephemeral, no queue)
                try {
                    supabaseMessageDataSource.removeReaction(messageId, userId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync removeReaction to Supabase (ephemeral, not queued)", e)
                }
            } else {
                // Different emoji or no reaction - add/update reaction
                updatedReactions[userId] = emoji

                // Update locally
                val updatedMessage = message.copy(reactions = updatedReactions)
                messageDao.updateMessage(updatedMessage)

                // Bug C fix: Wrap Supabase call in try-catch (reactions are ephemeral, no queue)
                try {
                    supabaseMessageDataSource.addReaction(messageId, userId, emoji)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync addReaction to Supabase (ephemeral, not queued)", e)
                }
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

    /**
     * Archive a chat room
     * Hybrid pattern: Update Room first, then sync to Supabase
     * @param chatRoomId Chat room ID to archive
     * @param isArchived Whether to archive (true) or unarchive (false)
     * @return Result indicating success or failure
     */
    suspend fun archiveChatRoom(chatRoomId: String, isArchived: Boolean = true): Result<Unit> {
        return try {
            val chatRoom = chatRoomDao.getChatRoomById(chatRoomId)
                ?: return Result.failure(Exception("Chat room not found"))

            // C5 FIX: Update locally first (optimistic) with isArchived field
            val updatedChatRoom = chatRoom.copy(isArchived = isArchived)
            chatRoomDao.updateChatRoom(updatedChatRoom)

            // Sync to Supabase
            val supabaseResult = supabaseChatDataSource.archiveChatRoom(chatRoomId, isArchived)

            if (supabaseResult.isFailure) {
                android.util.Log.e("ChatRepository", "❌ Failed to archive chat room in Supabase, queuing for retry", supabaseResult.exceptionOrNull())
                // Bug D fix: queue instead of returning hard failure (local update already done)
                SyncQueueHelper.queueChatRoom(syncQueueDao, updatedChatRoom, SyncOperation.UPDATE)
            } else {
                android.util.Log.d("ChatRepository", "✅ Chat room ${if (isArchived) "archived" else "unarchived"}: $chatRoomId")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "❌ Failed to archive chat room", e)
            Result.failure(e)
        }
    }

    /**
     * Pin a chat room to the top of the chat list
     * Hybrid pattern: Update Room first, then sync to Supabase
     * @param chatRoomId Chat room ID to pin
     * @param isPinned Whether to pin (true) or unpin (false)
     * @return Result indicating success or failure
     */
    suspend fun pinChatRoom(chatRoomId: String, isPinned: Boolean): Result<Unit> {
        return try {
            val chatRoom = chatRoomDao.getChatRoomById(chatRoomId)
                ?: return Result.failure(Exception("Chat room not found"))

            // Update local database first
            val updatedChatRoom = chatRoom.copy(isPinned = isPinned)
            chatRoomDao.updateChatRoom(updatedChatRoom)

            // Sync to Supabase
            val supabaseResult = supabaseChatDataSource.pinChatRoom(chatRoomId, isPinned)

            if (supabaseResult.isFailure) {
                android.util.Log.e("ChatRepository", "❌ Failed to pin chat room in Supabase", supabaseResult.exceptionOrNull())
                return Result.failure(supabaseResult.exceptionOrNull() ?: Exception("Failed to pin chat room"))
            }

            android.util.Log.d("ChatRepository", "✅ Chat room ${if (isPinned) "pinned" else "unpinned"}: $chatRoomId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "❌ Failed to pin chat room", e)
            Result.failure(e)
        }
    }

    /**
     * Get unread message count for a chat room
     * @param chatRoomId Chat room ID
     * @param userId Current user ID
     * @return Flow of unread message count
     */
    fun getUnreadCountFlow(chatRoomId: String, userId: String): Flow<Int> {
        return messageDao.getMessagesForChatRoomFlow(chatRoomId).map { messages ->
            messages.count { message ->
                message.senderId != userId && !message.readBy.contains(userId)
            }
        }
    }

    /**
     * Get total unread message count across all user's chat rooms
     * @param userId Current user ID
     * @return Flow of total unread message count
     */
    fun getTotalUnreadCountFlow(userId: String): Flow<Int> {
        // Get all chat rooms and filter for user's participation
        return chatRoomDao.getAllChatRoomsFlow().flatMapLatest { allChatRooms: List<ChatRoom> ->
            val userChatRooms = allChatRooms.filter { chatRoom ->
                chatRoom.participantIds.contains(userId)
            }

            if (userChatRooms.isEmpty()) {
                flowOf(0)
            } else {
                // Combine unread counts from all user's chat rooms
                val flows: List<Flow<Int>> = userChatRooms.map { chatRoom: ChatRoom ->
                    getUnreadCountFlow(chatRoom.id, userId)
                }
                combine(flows) { counts: Array<Int> ->
                    counts.sum()
                }
            }
        }
    }

    /**
     * Search messages by content or sender name within a chat room
     * Searches local cache first, reactive updates from Room
     * @param chatRoomId Chat room ID to search within
     * @param query Search query
     * @return Flow of matching messages
     */
    fun searchMessages(chatRoomId: String, query: String): Flow<List<Message>> {
        // If query is blank, return all messages
        if (query.isBlank()) {
            return messageDao.getMessagesForChatRoomFlow(chatRoomId)
        }

        // Return local search results
        // Room will reactively update as messages change
        return messageDao.searchMessages(chatRoomId, query)
    }
}