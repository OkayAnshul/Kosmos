package com.example.kosmos.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.kosmos.core.database.dao.*
import com.example.kosmos.core.models.*

@Database(
    entities = [
        User::class,
        ChatRoom::class,
        Message::class,
        VoiceMessage::class,
        Task::class,
        ActionItem::class,
        Project::class,
        ProjectMember::class
    ],
    version = 2,  // Incremented for RBAC changes
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
    abstract fun projectDao(): ProjectDao
    abstract fun projectMemberDao(): ProjectMemberDao

    companion object {
        const val DATABASE_NAME = "kosmos_database"
    }
}