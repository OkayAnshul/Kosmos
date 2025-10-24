package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): Message?

    @Query("SELECT * FROM messages WHERE chatRoomId = :chatRoomId ORDER BY timestamp DESC")
    fun getMessagesForChatRoomFlow(chatRoomId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE chatRoomId = :chatRoomId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(chatRoomId: String, limit: Int): List<Message>

    @Query("SELECT * FROM messages WHERE chatRoomId = :chatRoomId AND timestamp < :beforeTimestamp ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getMessagesBefore(chatRoomId: String, beforeTimestamp: Long, limit: Int): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages WHERE chatRoomId = :chatRoomId")
    suspend fun deleteMessagesForChatRoom(chatRoomId: String)
}