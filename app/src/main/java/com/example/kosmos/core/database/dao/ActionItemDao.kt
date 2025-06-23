package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.ActionItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionItemDao {
    @Query("SELECT * FROM action_items WHERE id = :actionItemId")
    suspend fun getActionItemById(actionItemId: String): ActionItem?

    @Query("SELECT * FROM action_items WHERE chatRoomId = :chatRoomId ORDER BY createdAt DESC")
    fun getActionItemsForChatRoomFlow(chatRoomId: String): Flow<List<ActionItem>>

    @Query("SELECT * FROM action_items WHERE isProcessed = 0 ORDER BY createdAt ASC")
    suspend fun getUnprocessedActionItems(): List<ActionItem>

    @Query("SELECT * FROM action_items WHERE messageId = :messageId")
    suspend fun getActionItemsForMessage(messageId: String): List<ActionItem>

    @Query("SELECT * FROM action_items WHERE voiceMessageId = :voiceMessageId")
    suspend fun getActionItemsForVoiceMessage(voiceMessageId: String): List<ActionItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionItem(actionItem: ActionItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionItems(actionItems: List<ActionItem>)

    @Update
    suspend fun updateActionItem(actionItem: ActionItem)

    @Delete
    suspend fun deleteActionItem(actionItem: ActionItem)

    @Query("DELETE FROM action_items WHERE id = :actionItemId")
    suspend fun deleteActionItemById(actionItemId: String)
}