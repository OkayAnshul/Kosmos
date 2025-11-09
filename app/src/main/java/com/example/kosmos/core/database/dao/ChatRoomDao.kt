package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.ChatRoom
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatRoomDao {
    @Query("SELECT * FROM chat_rooms WHERE id = :roomId")
    suspend fun getChatRoomById(roomId: String): ChatRoom?

    @Query("SELECT * FROM chat_rooms WHERE id = :roomId")
    fun getChatRoomByIdFlow(roomId: String): Flow<ChatRoom?>

    @Query("SELECT * FROM chat_rooms ORDER BY lastMessageTimestamp DESC")
    fun getAllChatRoomsFlow(): Flow<List<ChatRoom>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRoom(chatRoom: ChatRoom)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRooms(chatRooms: List<ChatRoom>)

    @Update
    suspend fun updateChatRoom(chatRoom: ChatRoom)

    @Delete
    suspend fun deleteChatRoom(chatRoom: ChatRoom)

    @Query("DELETE FROM chat_rooms WHERE id = :roomId")
    suspend fun deleteChatRoomById(roomId: String)

    // Stats queries for project statistics
    @Query("SELECT COUNT(*) FROM chat_rooms WHERE projectId = :projectId")
    suspend fun getChatRoomCountForProject(projectId: String): Int

    @Query("SELECT COUNT(*) FROM chat_rooms WHERE projectId = :projectId")
    fun getChatRoomCountForProjectFlow(projectId: String): Flow<Int>
}