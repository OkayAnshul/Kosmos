package com.example.kosmos.data.datasource

import android.util.Log
import com.example.kosmos.core.models.ChatRoom
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for chat room operations using Supabase Postgrest
 * Handles CRUD operations and participant management for chat rooms
 */
@Singleton
class SupabaseChatDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseChatDataSource"
        private const val CHAT_ROOMS_TABLE = "chat_rooms"
        private const val PARTICIPANTS_TABLE = "chat_room_participants"
    }

    /**
     * Insert a new chat room into Supabase
     * @param chatRoom ChatRoom to insert
     * @return Result with inserted chat room or error
     */
    suspend fun insertChatRoom(chatRoom: ChatRoom): Result<ChatRoom> {
        return try {
            supabase.from(CHAT_ROOMS_TABLE)
                .insert(chatRoom)
            Result.success(chatRoom)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting chat room", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing chat room in Supabase
     * @param chatRoom ChatRoom to update
     * @return Result with updated chat room or error
     */
    suspend fun updateChatRoom(chatRoom: ChatRoom): Result<ChatRoom> {
        return try {
            supabase.from(CHAT_ROOMS_TABLE)
                .update(chatRoom) {
                    filter {
                        eq("id", chatRoom.id)
                    }
                }
            Result.success(chatRoom)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating chat room", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a chat room from Supabase
     * @param chatRoomId ID of chat room to delete
     * @return Result with Unit or error
     */
    suspend fun deleteChatRoom(chatRoomId: String): Result<Unit> {
        return try {
            supabase.from(CHAT_ROOMS_TABLE)
                .delete {
                    filter {
                        eq("id", chatRoomId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chat room", e)
            Result.failure(e)
        }
    }

    /**
     * Get a chat room by ID
     * @param chatRoomId Chat room ID
     * @return Result with ChatRoom or error (null if not found)
     */
    suspend fun getChatRoomById(chatRoomId: String): Result<ChatRoom?> {
        return try {
            val chatRoom = supabase.from(CHAT_ROOMS_TABLE)
                .select() {
                    filter {
                        eq("id", chatRoomId)
                    }
                }
                .decodeSingleOrNull<ChatRoom>()

            Result.success(chatRoom)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chat room by ID", e)
            Result.failure(e)
        }
    }

    /**
     * Get all chat rooms for a specific user
     * Queries the participants junction table to find rooms user belongs to
     * @param userId User ID
     * @return Result with list of chat rooms or error
     */
    suspend fun getChatRoomsForUser(userId: String): Result<List<ChatRoom>> {
        return try {
            // Get chat room IDs where user is a participant
            // Note: This is a simplified version. In production, you'd want to:
            // 1. Join chat_rooms with chat_room_participants
            // 2. Filter by user_id
            // 3. Select all chat_room fields
            // For MVP, we'll fetch all rooms and filter client-side

            val allRooms = supabase.from(CHAT_ROOMS_TABLE)
                .select()
                .decodeList<ChatRoom>()

            // Client-side filtering (TODO: optimize with server-side join in Phase 2)
            val userRooms = allRooms.filter { room ->
                room.participantIds.contains(userId)
            }

            Result.success(userRooms)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chat rooms for user", e)
            Result.failure(e)
        }
    }

    /**
     * Get all chat rooms
     * @return Result with list of all chat rooms or error
     */
    suspend fun getAllChatRooms(): Result<List<ChatRoom>> {
        return try {
            val chatRooms = supabase.from(CHAT_ROOMS_TABLE)
                .select()
                .decodeList<ChatRoom>()

            Result.success(chatRooms)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all chat rooms", e)
            Result.failure(e)
        }
    }

    /**
     * Add a participant to a chat room
     * Updates the participantIds list in the chat room
     * @param chatRoomId Chat room ID
     * @param userId User ID to add
     * @return Result with Unit or error
     */
    suspend fun addParticipant(chatRoomId: String, userId: String): Result<Unit> {
        return try {
            // Fetch current chat room
            val chatRoom = getChatRoomById(chatRoomId).getOrNull()
                ?: return Result.failure(Exception("Chat room not found"))

            // Add user if not already a participant
            if (!chatRoom.participantIds.contains(userId)) {
                val updatedParticipants = chatRoom.participantIds + userId
                val updatedRoom = chatRoom.copy(participantIds = updatedParticipants)
                updateChatRoom(updatedRoom)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding participant", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a participant from a chat room
     * Updates the participantIds list in the chat room
     * @param chatRoomId Chat room ID
     * @param userId User ID to remove
     * @return Result with Unit or error
     */
    suspend fun removeParticipant(chatRoomId: String, userId: String): Result<Unit> {
        return try {
            // Fetch current chat room
            val chatRoom = getChatRoomById(chatRoomId).getOrNull()
                ?: return Result.failure(Exception("Chat room not found"))

            // Remove user if they are a participant
            if (chatRoom.participantIds.contains(userId)) {
                val updatedParticipants = chatRoom.participantIds - userId
                val updatedRoom = chatRoom.copy(participantIds = updatedParticipants)
                updateChatRoom(updatedRoom)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing participant", e)
            Result.failure(e)
        }
    }

    /**
     * Get list of participant IDs for a chat room
     * @param chatRoomId Chat room ID
     * @return Result with list of participant user IDs or error
     */
    suspend fun getParticipants(chatRoomId: String): Result<List<String>> {
        return try {
            val chatRoom = getChatRoomById(chatRoomId).getOrNull()
                ?: return Result.failure(Exception("Chat room not found"))

            Result.success(chatRoom.participantIds)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching participants", e)
            Result.failure(e)
        }
    }

    /**
     * Update chat room's last message info
     * Called when a new message is sent
     * @param chatRoomId Chat room ID
     * @param messageId Message ID
     * @param messagePreview Preview of message content
     * @param timestamp Message timestamp
     * @return Result with Unit or error
     */
    suspend fun updateLastMessage(
        chatRoomId: String,
        messageId: String,
        messagePreview: String,
        timestamp: Long
    ): Result<Unit> {
        return try {
            supabase.from(CHAT_ROOMS_TABLE)
                .update({
                    set("last_message_id", messageId)
                    set("last_message", messagePreview)
                    set("last_message_timestamp", timestamp)
                }) {
                    filter {
                        eq("id", chatRoomId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last message", e)
            Result.failure(e)
        }
    }

    /**
     * Observe real-time changes to chat rooms
     * Returns a Flow that emits when chat rooms are updated
     * @return Flow of chat room changes
     */
    fun observeChatRooms(): Flow<List<ChatRoom>> {
        // Real-time subscriptions require proper setup in Supabase
        // For now, returning an empty flow - will implement when Supabase is configured
        return flowOf(emptyList())
    }

    /**
     * Observe changes to a specific chat room
     * @param chatRoomId Chat room ID to observe
     * @return Flow of chat room updates
     */
    fun observeChatRoomById(chatRoomId: String): Flow<ChatRoom?> {
        // Placeholder for real-time subscription
        return flowOf(null)
    }

    /**
     * Batch insert multiple chat rooms
     * @param chatRooms List of chat rooms to insert
     * @return Result with inserted chat rooms or error
     */
    suspend fun insertAll(chatRooms: List<ChatRoom>): Result<List<ChatRoom>> {
        return try {
            supabase.from(CHAT_ROOMS_TABLE)
                .insert(chatRooms)
            Result.success(chatRooms)
        } catch (e: Exception) {
            Log.e(TAG, "Error batch inserting chat rooms", e)
            Result.failure(e)
        }
    }
}
