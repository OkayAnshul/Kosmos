package com.example.kosmos.database

import androidx.room.*
import com.example.kosmos.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByIdFlow(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE id IN (:userIds)")
    suspend fun getUsersByIds(userIds: List<String>): List<User>

    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)
}

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
}

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

@Dao
interface VoiceMessageDao {
    @Query("SELECT * FROM voice_messages WHERE id = :voiceMessageId")
    suspend fun getVoiceMessageById(voiceMessageId: String): VoiceMessage?

    @Query("SELECT * FROM voice_messages WHERE id = :voiceMessageId")
    fun getVoiceMessageByIdFlow(voiceMessageId: String): Flow<VoiceMessage?>

    @Query("SELECT * FROM voice_messages WHERE messageId = :messageId")
    suspend fun getVoiceMessageByMessageId(messageId: String): VoiceMessage?

    @Query("SELECT * FROM voice_messages WHERE isTranscribing = 1")
    suspend fun getPendingTranscriptions(): List<VoiceMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceMessage(voiceMessage: VoiceMessage)

    @Update
    suspend fun updateVoiceMessage(voiceMessage: VoiceMessage)

    @Delete
    suspend fun deleteVoiceMessage(voiceMessage: VoiceMessage)

    @Query("DELETE FROM voice_messages WHERE id = :voiceMessageId")
    suspend fun deleteVoiceMessageById(voiceMessageId: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): Task?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskByIdFlow(taskId: String): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE chatRoomId = :chatRoomId ORDER BY createdAt DESC")
    fun getTasksForChatRoomFlow(chatRoomId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE chatRoomId = :chatRoomId AND status = :status ORDER BY createdAt DESC")
    fun getTasksByStatusFlow(chatRoomId: String, status: TaskStatus): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE assignedToId = :userId AND status != 'DONE' AND status != 'CANCELLED' ORDER BY dueDate ASC")
    fun getMyActiveTasksFlow(userId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dueDate IS NOT NULL AND dueDate < :timestamp AND status != 'DONE' AND status != 'CANCELLED'")
    suspend fun getOverdueTasks(timestamp: Long): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks WHERE chatRoomId = :chatRoomId")
    suspend fun deleteTasksForChatRoom(chatRoomId: String)
}

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

class Converters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromStringLongMap(value: Map<String, Long>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringLongMap(value: String): Map<String, Long> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromFloatList(value: List<Float>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toFloatList(value: String): List<Float> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromTaskCommentList(value: List<TaskComment>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toTaskCommentList(value: String): List<TaskComment> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType =
        try { MessageType.valueOf(value) } catch (e: Exception) { MessageType.TEXT }

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus =
        try { TaskStatus.valueOf(value) } catch (e: Exception) { TaskStatus.TODO }

    @TypeConverter
    fun fromTaskPriority(value: TaskPriority): String = value.name

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority =
        try { TaskPriority.valueOf(value) } catch (e: Exception) { TaskPriority.MEDIUM }

    @TypeConverter
    fun fromActionType(value: ActionType): String = value.name

    @TypeConverter
    fun toActionType(value: String): ActionType =
        try { ActionType.valueOf(value) } catch (e: Exception) { ActionType.TASK }
}

@Database(
    entities = [
        User::class,
        ChatRoom::class,
        Message::class,
        VoiceMessage::class,
        Task::class,
        ActionItem::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KosmosDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun chatRoomDao(): ChatRoomDao
    abstract fun messageDao(): MessageDao
    abstract fun voiceMessageDao(): VoiceMessageDao
    abstract fun taskDao(): TaskDao
    abstract fun actionItemDao(): ActionItemDao

    companion object {
        const val DATABASE_NAME = "kosmos_database"
    }
}