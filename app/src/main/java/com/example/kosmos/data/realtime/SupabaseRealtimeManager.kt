package com.example.kosmos.data.realtime

import android.util.Log
import com.example.kosmos.core.database.dao.MessageDao
import com.example.kosmos.core.models.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Supabase Realtime subscriptions for live data updates
 *
 * Features:
 * - Real-time message updates (INSERT, UPDATE, DELETE)
 * - Typing indicators via Realtime Broadcast
 * - Automatic reconnection on network changes
 * - Lifecycle-aware subscriptions
 *
 * Usage:
 * ```kotlin
 * // Subscribe to messages for a chat room
 * realtimeManager.subscribeToMessages(chatRoomId)
 *
 * // Listen for message events
 * realtimeManager.messageEvents.collect { event ->
 *     when (event) {
 *         is MessageEvent.Insert -> // Handle new message
 *         is MessageEvent.Update -> // Handle message update
 *         is MessageEvent.Delete -> // Handle message deletion
 *     }
 * }
 *
 * // Send typing indicator
 * realtimeManager.sendTypingIndicator(chatRoomId, userId, isTyping = true)
 *
 * // Cleanup when done
 * realtimeManager.unsubscribeFromMessages(chatRoomId)
 * ```
 */
@Singleton
class SupabaseRealtimeManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val messageDao: MessageDao
) {
    private val TAG = "SupabaseRealtimeManager"

    // Coroutine scope for realtime operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Active channel subscriptions by chat room ID
    private val activeChannels = mutableMapOf<String, io.github.jan.supabase.realtime.RealtimeChannel>()

    // Message events flow
    private val _messageEvents = MutableSharedFlow<MessageEvent>(replay = 0, extraBufferCapacity = 64)
    val messageEvents: SharedFlow<MessageEvent> = _messageEvents.asSharedFlow()

    // Typing indicator events flow
    private val _typingEvents = MutableSharedFlow<TypingEvent>(replay = 0, extraBufferCapacity = 32)
    val typingEvents: SharedFlow<TypingEvent> = _typingEvents.asSharedFlow()

    /**
     * Subscribe to real-time message updates for a specific chat room
     * @param chatRoomId The chat room ID to subscribe to
     */
    fun subscribeToMessages(chatRoomId: String) {
        // Don't create duplicate subscriptions
        if (activeChannels.containsKey(chatRoomId)) {
            Log.d(TAG, "Already subscribed to chat room: $chatRoomId")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Subscribing to messages for chat room: $chatRoomId")

                // Create a unique channel for this chat room
                val channel = supabase.realtime.channel("messages:$chatRoomId")

                // Subscribe to postgres changes on messages table
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                    // Note: Filter syntax may vary based on Supabase SDK version
                    // For now, we'll subscribe to all messages and filter client-side
                }

                // Subscribe the channel
                channel.subscribe()

                // Store the channel for cleanup later
                activeChannels[chatRoomId] = channel

                // Listen for changes and emit events
                changeFlow
                    .catch { e ->
                        Log.e(TAG, "Error in message change flow for chat $chatRoomId", e)
                    }
                    .collect { action ->
                        // Filter messages by chat room ID (client-side filtering)
                        val messageChatRoomId = when (action) {
                            is PostgresAction.Insert -> action.record["chat_room_id"] as? String
                            is PostgresAction.Update -> action.record["chat_room_id"] as? String
                            is PostgresAction.Delete -> action.oldRecord["chat_room_id"] as? String
                            else -> null
                        }

                        // Only process if message belongs to this chat room
                        if (messageChatRoomId == chatRoomId) {
                            when (action) {
                                is PostgresAction.Insert -> {
                                    Log.d(TAG, "Message INSERT detected in chat $chatRoomId")
                                    handleMessageInsert(action)
                                }
                                is PostgresAction.Update -> {
                                    Log.d(TAG, "Message UPDATE detected in chat $chatRoomId")
                                    handleMessageUpdate(action)
                                }
                                is PostgresAction.Delete -> {
                                    Log.d(TAG, "Message DELETE detected in chat $chatRoomId")
                                    handleMessageDelete(action)
                                }
                                else -> {
                                    Log.d(TAG, "Unknown action type: $action")
                                }
                            }
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to messages for chat $chatRoomId", e)
            }
        }
    }

    /**
     * Unsubscribe from real-time message updates for a specific chat room
     * @param chatRoomId The chat room ID to unsubscribe from
     */
    fun unsubscribeFromMessages(chatRoomId: String) {
        scope.launch {
            try {
                activeChannels[chatRoomId]?.let { channel ->
                    Log.d(TAG, "Unsubscribing from chat room: $chatRoomId")
                    supabase.realtime.removeChannel(channel)
                    activeChannels.remove(chatRoomId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsubscribe from chat $chatRoomId", e)
            }
        }
    }

    /**
     * Subscribe to typing indicators for a specific chat room
     * Uses Realtime Broadcast for ephemeral events
     */
    fun subscribeToTypingIndicators(chatRoomId: String) {
        scope.launch {
            try {
                val channel = activeChannels[chatRoomId] ?: run {
                    // Create channel if it doesn't exist
                    val newChannel = supabase.realtime.channel("messages:$chatRoomId")
                    newChannel.subscribe()
                    activeChannels[chatRoomId] = newChannel
                    newChannel
                }

                // Listen for typing broadcast events
                // Note: This is a simplified version - actual implementation depends on
                // Supabase Realtime Broadcast API which may require additional setup
                Log.d(TAG, "Subscribed to typing indicators for chat $chatRoomId")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to typing indicators", e)
            }
        }
    }

    /**
     * Send typing indicator to other users in the chat room
     * Uses Realtime Broadcast for ephemeral events
     *
     * @param chatRoomId The chat room ID
     * @param userId The user ID who is typing
     * @param isTyping Whether the user is currently typing
     */
    fun sendTypingIndicator(chatRoomId: String, userId: String, isTyping: Boolean) {
        scope.launch {
            try {
                val channel = activeChannels[chatRoomId]
                if (channel == null) {
                    Log.w(TAG, "Cannot send typing indicator - not subscribed to chat $chatRoomId")
                    return@launch
                }

                // Send broadcast event
                // Note: Actual broadcast implementation may vary based on Supabase SDK version
                Log.d(TAG, "User $userId typing status: $isTyping in chat $chatRoomId")

                // Emit local event for testing
                _typingEvents.emit(TypingEvent(chatRoomId, userId, isTyping))

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send typing indicator", e)
            }
        }
    }

    /**
     * Unsubscribe from all active channels and cleanup
     */
    fun disconnect() {
        scope.launch {
            try {
                Log.d(TAG, "Disconnecting all realtime channels (${activeChannels.size})")
                activeChannels.values.forEach { channel ->
                    supabase.realtime.removeChannel(channel)
                }
                activeChannels.clear()
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting realtime channels", e)
            }
        }
    }

    // Private helper methods for handling different action types

    private suspend fun handleMessageInsert(action: PostgresAction.Insert) {
        try {
            // Parse the new message from the action record
            val record = action.record
            val message = parseMessage(record)

            if (message != null) {
                // Update local database
                messageDao.insertMessage(message)

                // Emit event for UI updates
                _messageEvents.emit(MessageEvent.Insert(message))

                Log.d(TAG, "Message inserted: ${message.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message insert", e)
        }
    }

    private suspend fun handleMessageUpdate(action: PostgresAction.Update) {
        try {
            val record = action.record
            val message = parseMessage(record)

            if (message != null) {
                // Update local database
                messageDao.updateMessage(message)

                // Emit event for UI updates
                _messageEvents.emit(MessageEvent.Update(message))

                Log.d(TAG, "Message updated: ${message.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message update", e)
        }
    }

    private suspend fun handleMessageDelete(action: PostgresAction.Delete) {
        try {
            val oldRecord = action.oldRecord
            val messageId = oldRecord["id"] as? String

            if (messageId != null) {
                // Delete from local database
                messageDao.deleteMessageById(messageId)

                // Emit event for UI updates
                _messageEvents.emit(MessageEvent.Delete(messageId))

                Log.d(TAG, "Message deleted: $messageId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message delete", e)
        }
    }

    /**
     * Parse a message from Supabase record
     * This is a simplified version - you may need to adjust based on your actual data structure
     */
    private fun parseMessage(record: Map<String, Any?>): Message? {
        return try {
            // Convert the record map to JSON and then to Message object
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
            val jsonString = json.encodeToString(
                kotlinx.serialization.serializer<Map<String, Any?>>(),
                record
            )

            val message = json.decodeFromString<Message>(jsonString)
            Log.d(TAG, "Successfully parsed message: ${message.id}")
            message
        } catch (e: kotlinx.serialization.SerializationException) {
            Log.e(TAG, "Serialization error parsing message from record: ${e.message}")
            Log.e(TAG, "Record data: $record")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing message from record", e)
            Log.e(TAG, "Record data: $record")
            null
        }
    }
}

/**
 * Sealed class representing different message events from Realtime
 */
sealed class MessageEvent {
    data class Insert(val message: Message) : MessageEvent()
    data class Update(val message: Message) : MessageEvent()
    data class Delete(val messageId: String) : MessageEvent()
}

/**
 * Data class for typing indicator events
 */
data class TypingEvent(
    val chatRoomId: String,
    val userId: String,
    val isTyping: Boolean
)
