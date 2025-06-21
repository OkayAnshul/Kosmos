package com.example.kosmos.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kosmos.core.database.dao.*
import com.example.kosmos.core.models.*

@Database(
    entities = [
        User::class,
        ChatRoom::class,
        Message::class,
        VoiceMessage::class,
        Task::class,
        TaskActivity::class,
        ActionItem::class,
        Project::class,
        ProjectMember::class,
        SyncQueueItem::class,  // P0-08 FIX: Sync queue for offline operations
        SyncTimestamp::class,  // Incremental sync: Track last sync timestamps
        TimeEntry::class,      // Time tracking entries
        TaskDependency::class,  // Task dependency relationships
        ProjectInvite::class,   // Project invite workflow
        UserConnection::class,  // User-to-user connections
        ProjectJoinRequest::class  // Join request workflow
    ],
    version = 12,  // Migration 11→12: Add project_invites, user_connections, project_join_requests
    exportSchema = false
)
@TypeConverters(
    Converters::class,
    com.example.kosmos.core.database.converters.UserSettingsConverters::class,
    com.example.kosmos.core.database.converters.FieldChangeListConverter::class
)
abstract class KosmosDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun chatRoomDao(): ChatRoomDao
    abstract fun messageDao(): MessageDao
    abstract fun voiceMessageDao(): VoiceMessageDao
    abstract fun taskDao(): TaskDao
    abstract fun taskActivityDao(): TaskActivityDao
    abstract fun actionItemDao(): ActionItemDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectMemberDao(): ProjectMemberDao
    abstract fun syncQueueDao(): SyncQueueDao  // P0-08 FIX
    abstract fun syncTimestampDao(): SyncTimestampDao  // Incremental sync
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun taskDependencyDao(): TaskDependencyDao
    abstract fun projectInviteDao(): ProjectInviteDao
    abstract fun userConnectionDao(): UserConnectionDao
    abstract fun projectJoinRequestDao(): ProjectJoinRequestDao

    companion object {
        const val DATABASE_NAME = "kosmos_database"

        /**
         * GAP-001 FIX: Add missing migrations for backward compatibility
         *
         * Migration from version 1 to 2
         * Note: Schema at v1 is unknown - empty migration for safety
         * If app was installed at v1, this prevents data loss
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Schema changes from v1 to v2 (if any)
                // Empty migration - schema unchanged or changes were handled by destructive migration
            }
        }

        /**
         * Migration from version 2 to 3
         * Note: Schema at v2 is unknown - empty migration for safety
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Schema changes from v2 to v3 (if any)
                // Empty migration - schema unchanged or changes were handled by destructive migration
            }
        }

        /**
         * Migration from version 3 to 4
         * Adds project creation wizard fields to projects table
         *
         * Added fields:
         * - category (TEXT, default 'other')
         * - deadline (INTEGER, nullable)
         * - website_url (TEXT, nullable)
         * - github_url (TEXT, nullable)
         * - project_motive (TEXT, nullable)
         * - tech_stack (TEXT, nullable) - JSON array stored as string
         * - tags (TEXT, nullable) - JSON array stored as string
         * - business_model (TEXT, nullable)
         * - target_audience (TEXT, nullable)
         * - industry_tags (TEXT, nullable) - JSON array stored as string
         * - open_source_license (TEXT, nullable)
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add category field with default value
                database.execSQL("ALTER TABLE projects ADD COLUMN category TEXT NOT NULL DEFAULT 'OTHER'")

                // Add deadline field (nullable)
                database.execSQL("ALTER TABLE projects ADD COLUMN deadline INTEGER")

                // Add website_url field (nullable)
                database.execSQL("ALTER TABLE projects ADD COLUMN website_url TEXT")

                // Add github_url field (nullable)
                database.execSQL("ALTER TABLE projects ADD COLUMN github_url TEXT")

                // Add project_motive field (nullable)
                database.execSQL("ALTER TABLE projects ADD COLUMN project_motive TEXT")

                // Add tech_stack field (nullable, stores JSON array as TEXT)
                database.execSQL("ALTER TABLE projects ADD COLUMN tech_stack TEXT")

                // Add tags field (nullable, stores JSON array as TEXT)
                database.execSQL("ALTER TABLE projects ADD COLUMN tags TEXT")

                // Add business_model field (nullable)
                database.execSQL("ALTER TABLE projects ADD COLUMN business_model TEXT")

                // Add target_audience field (nullable)
                database.execSQL("ALTER TABLE projects ADD COLUMN target_audience TEXT")

                // Add industry_tags field (nullable, stores JSON array as TEXT)
                database.execSQL("ALTER TABLE projects ADD COLUMN industry_tags TEXT")

                // Add open_source_license field (nullable)
                database.execSQL("ALTER TABLE projects ADD COLUMN open_source_license TEXT")
            }
        }

        /**
         * Migration from version 4 to 5 (P0-01 & P0-02 FIX)
         * 1. Adds version field to users table for optimistic locking (P0-01)
         * 2. Creates task_activity table for tracking task changes (P0-02)
         *
         * Added fields to users:
         * - version (INTEGER, default 1) - Used for conflict detection in concurrent updates
         *
         * Created task_activity table:
         * - Stores all task change history with Git-style commit messages
         * - Enables activity timeline, audit logs, and rollback capabilities
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // P0-01 FIX: Add version field to users table
                database.execSQL("ALTER TABLE users ADD COLUMN version INTEGER NOT NULL DEFAULT 1")

                // P0-02 FIX: Create task_activity table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS task_activity (
                        id TEXT PRIMARY KEY NOT NULL,
                        taskId TEXT NOT NULL,
                        projectId TEXT NOT NULL,
                        actorId TEXT NOT NULL,
                        actorName TEXT NOT NULL,
                        actorRole TEXT,
                        actionType TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        changes TEXT NOT NULL,
                        commitMessage TEXT,
                        autoDescription TEXT NOT NULL,
                        metadata TEXT NOT NULL
                    )
                """.trimIndent())

                // Create indexes for performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_task_activity_taskId ON task_activity(taskId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_task_activity_projectId ON task_activity(projectId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_task_activity_actorId ON task_activity(actorId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_task_activity_timestamp ON task_activity(timestamp)")
            }
        }

        /**
         * Migration from version 5 to 6 (P0-05 FIX)
         * Enables foreign key enforcement in SQLite
         *
         * NOTE: Foreign key constraints were added to entity annotations
         * but don't apply to existing tables. They only apply to:
         * - Fresh installations at version 6+
         * - Tables created/recreated after this migration
         *
         * For existing installations, we enable enforcement but don't
         * recreate tables (too risky for production data).
         *
         * Future: Consider table recreation migration for major version bump.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // P0-05 FIX: Enable foreign key enforcement
                // This enables FK checks for future operations, but doesn't
                // retroactively add FK constraints to existing tables
                database.execSQL("PRAGMA foreign_keys = ON")

                // Add missing indexes for foreign key columns that were added
                // (These improve query performance even without FK constraints)

                // Tasks indexes
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_project_id ON tasks(project_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_chat_room_id ON tasks(chat_room_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_assigned_to_id ON tasks(assigned_to_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_created_by_id ON tasks(created_by_id)")

                // Messages indexes
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_chat_room_id ON messages(chat_room_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_sender_id ON messages(sender_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_voice_message_id ON messages(voice_message_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_reply_to_message_id ON messages(reply_to_message_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_timestamp ON messages(timestamp)")

                // ChatRooms indexes
                database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_rooms_project_id ON chat_rooms(project_id)")

                // ProjectMembers indexes
                database.execSQL("CREATE INDEX IF NOT EXISTS index_project_members_project_id ON project_members(project_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_project_members_user_id ON project_members(user_id)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_project_members_project_user ON project_members(project_id, user_id)")

                // VoiceMessages indexes
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_voice_messages_message_id ON voice_messages(message_id)")

                // ActionItems indexes
                database.execSQL("CREATE INDEX IF NOT EXISTS index_action_items_message_id ON action_items(message_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_action_items_voice_message_id ON action_items(voice_message_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_action_items_chat_room_id ON action_items(chat_room_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_action_items_task_id ON action_items(task_id)")
            }
        }

        /**
         * Migration from version 6 to 7 (P0-08 FIX)
         * Creates sync_queue table for offline operation retry
         *
         * Added table:
         * - sync_queue: Stores failed Supabase operations for retry
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // P0-08 FIX: Create sync_queue table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        id TEXT PRIMARY KEY NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        entityJson TEXT NOT NULL,
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        maxRetries INTEGER NOT NULL DEFAULT 5,
                        lastAttemptTimestamp INTEGER NOT NULL,
                        createdTimestamp INTEGER NOT NULL,
                        lastErrorMessage TEXT,
                        lastErrorCode TEXT,
                        priority INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create indexes for performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entityType ON sync_queue(entityType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entityId ON sync_queue(entityId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_priority ON sync_queue(priority DESC)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_createdTimestamp ON sync_queue(createdTimestamp)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_retryCount ON sync_queue(retryCount)")
            }
        }

        /**
         * Migration from version 7 to 8
         * Adds missing schema fields that exist in models but weren't migrated:
         * - chat_rooms.is_pinned: Allows users to pin important chats to the top
         * - tasks.estimated_hours: Tracks estimated time for task completion
         * - tasks.actual_hours: Tracks actual time spent on task (from time tracker)
         *
         * These fields are referenced in ChatRoom and Task models but were never
         * added to the database schema, causing potential data loss and FK issues.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add is_pinned to chat_rooms (for pinning important chats)
                database.execSQL("ALTER TABLE chat_rooms ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0")

                // Add time tracking fields to tasks
                database.execSQL("ALTER TABLE tasks ADD COLUMN estimated_hours REAL")
                database.execSQL("ALTER TABLE tasks ADD COLUMN actual_hours REAL")

                android.util.Log.d("KosmosDatabase", "✅ Migration 7→8 complete: Added is_pinned, estimated_hours, actual_hours")
            }
        }

        /**
         * Migration from version 8 to 9
         * Creates sync_timestamps table for incremental sync
         *
         * Incremental Sync Benefits:
         * - Only fetches data modified since last sync (reduces data transfer 50-90%)
         * - Faster sync times (skip unchanged data)
         * - Better offline experience (less network usage)
         * - Scales to large projects with thousands of messages/tasks
         *
         * Added table:
         * - sync_timestamps: Tracks last successful sync timestamp per resource type per project
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create sync_timestamps table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_timestamps (
                        id TEXT PRIMARY KEY NOT NULL,
                        projectId TEXT,
                        resourceType TEXT NOT NULL,
                        lastSyncTimestamp INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create indexes for performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_timestamps_projectId ON sync_timestamps(projectId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_timestamps_resourceType ON sync_timestamps(resourceType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_timestamps_lastSyncTimestamp ON sync_timestamps(lastSyncTimestamp)")

                android.util.Log.d("KosmosDatabase", "✅ Migration 8→9 complete: Added sync_timestamps table for incremental sync")
            }
        }

        /**
         * Migration 9→10: Change all CASCADE FKs to NO_ACTION
         *
         * CASCADE + OnConflictStrategy.REPLACE = data loss during sync.
         * REPLACE does DELETE+INSERT, which triggers CASCADE on child tables.
         * NO_ACTION prevents unintended cascade deletes during offline-first sync.
         *
         * Tables affected: task_activity, project_members, chat_rooms, tasks, messages, action_items
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Disable FK enforcement during migration to avoid constraint errors
                database.execSQL("PRAGMA foreign_keys = OFF")

                // --- 1. task_activity: Remove actorId FK, change task/project to NO_ACTION ---
                recreateTable(database,
                    oldTable = "task_activity",
                    createSql = """
                        CREATE TABLE task_activity_new (
                            id TEXT PRIMARY KEY NOT NULL,
                            taskId TEXT NOT NULL,
                            projectId TEXT NOT NULL,
                            actorId TEXT NOT NULL,
                            actorName TEXT NOT NULL,
                            actorRole TEXT,
                            actionType TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            changes TEXT NOT NULL,
                            commitMessage TEXT,
                            autoDescription TEXT NOT NULL,
                            FOREIGN KEY (taskId) REFERENCES tasks(id) ON DELETE NO ACTION,
                            FOREIGN KEY (projectId) REFERENCES projects(id) ON DELETE NO ACTION
                        )
                    """,
                    copySql = """
                        INSERT INTO task_activity_new
                        SELECT id, taskId, projectId, actorId, actorName, actorRole,
                               actionType, timestamp, changes, commitMessage, autoDescription
                        FROM task_activity
                    """,
                    indexes = listOf(
                        "CREATE INDEX IF NOT EXISTS index_task_activity_taskId ON task_activity(taskId)",
                        "CREATE INDEX IF NOT EXISTS index_task_activity_projectId ON task_activity(projectId)",
                        "CREATE INDEX IF NOT EXISTS index_task_activity_actorId ON task_activity(actorId)",
                        "CREATE INDEX IF NOT EXISTS index_task_activity_timestamp ON task_activity(timestamp)"
                    )
                )

                // --- 2. project_members: CASCADE → NO_ACTION ---
                recreateTable(database,
                    oldTable = "project_members",
                    createSql = """
                        CREATE TABLE project_members_new (
                            id TEXT PRIMARY KEY NOT NULL,
                            projectId TEXT NOT NULL,
                            userId TEXT NOT NULL,
                            role TEXT NOT NULL,
                            joinedAt INTEGER NOT NULL,
                            invitedBy TEXT,
                            isActive INTEGER NOT NULL,
                            lastActivityAt INTEGER NOT NULL,
                            customPermissions TEXT,
                            FOREIGN KEY (projectId) REFERENCES projects(id) ON DELETE NO ACTION,
                            FOREIGN KEY (userId) REFERENCES users(id) ON DELETE NO ACTION
                        )
                    """,
                    copySql = """
                        INSERT INTO project_members_new
                        SELECT id, projectId, userId, role, joinedAt, invitedBy,
                               isActive, lastActivityAt, customPermissions
                        FROM project_members
                    """,
                    indexes = listOf(
                        "CREATE INDEX IF NOT EXISTS index_project_members_projectId ON project_members(projectId)",
                        "CREATE INDEX IF NOT EXISTS index_project_members_userId ON project_members(userId)",
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_project_members_projectId_userId ON project_members(projectId, userId)"
                    )
                )

                // --- 3. chat_rooms: CASCADE → NO_ACTION ---
                recreateTable(database,
                    oldTable = "chat_rooms",
                    createSql = """
                        CREATE TABLE chat_rooms_new (
                            id TEXT PRIMARY KEY NOT NULL,
                            projectId TEXT NOT NULL,
                            name TEXT NOT NULL,
                            description TEXT NOT NULL,
                            imageUrl TEXT,
                            type TEXT NOT NULL,
                            participantIds TEXT NOT NULL,
                            createdBy TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            lastMessageId TEXT,
                            lastMessage TEXT NOT NULL,
                            lastMessageTimestamp INTEGER NOT NULL,
                            isTaskBoardEnabled INTEGER NOT NULL,
                            isArchived INTEGER NOT NULL,
                            isPinned INTEGER NOT NULL,
                            isPrivate INTEGER NOT NULL,
                            FOREIGN KEY (projectId) REFERENCES projects(id) ON DELETE NO ACTION
                        )
                    """,
                    copySql = """
                        INSERT INTO chat_rooms_new
                        SELECT id, projectId, name, description, imageUrl, type,
                               participantIds, createdBy, createdAt, updatedAt,
                               lastMessageId, lastMessage, lastMessageTimestamp,
                               isTaskBoardEnabled, isArchived, isPinned, isPrivate
                        FROM chat_rooms
                    """,
                    indexes = listOf(
                        "CREATE INDEX IF NOT EXISTS index_chat_rooms_projectId ON chat_rooms(projectId)"
                    )
                )

                // --- 4. tasks: project CASCADE → NO_ACTION, createdById CASCADE → NO_ACTION ---
                recreateTable(database,
                    oldTable = "tasks",
                    createSql = """
                        CREATE TABLE tasks_new (
                            id TEXT PRIMARY KEY NOT NULL,
                            projectId TEXT NOT NULL,
                            chatRoomId TEXT,
                            title TEXT NOT NULL,
                            description TEXT,
                            status TEXT NOT NULL,
                            priority TEXT NOT NULL,
                            assignedToId TEXT,
                            assignedToName TEXT,
                            assignedToRole TEXT,
                            createdById TEXT NOT NULL,
                            createdByName TEXT NOT NULL,
                            createdByRole TEXT,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            version INTEGER NOT NULL,
                            dueDate INTEGER,
                            sourceMessageId TEXT,
                            tags TEXT NOT NULL,
                            comments TEXT NOT NULL,
                            parentTaskId TEXT,
                            estimatedHours REAL,
                            actualHours REAL,
                            FOREIGN KEY (projectId) REFERENCES projects(id) ON DELETE NO ACTION,
                            FOREIGN KEY (chatRoomId) REFERENCES chat_rooms(id) ON DELETE SET NULL,
                            FOREIGN KEY (assignedToId) REFERENCES users(id) ON DELETE SET NULL,
                            FOREIGN KEY (createdById) REFERENCES users(id) ON DELETE NO ACTION
                        )
                    """,
                    copySql = """
                        INSERT INTO tasks_new
                        SELECT id, projectId, chatRoomId, title, description, status, priority,
                               assignedToId, assignedToName, assignedToRole,
                               createdById, createdByName, createdByRole,
                               createdAt, updatedAt, version, dueDate, sourceMessageId,
                               tags, comments, parentTaskId, estimatedHours, actualHours
                        FROM tasks
                    """,
                    indexes = listOf(
                        "CREATE INDEX IF NOT EXISTS index_tasks_projectId ON tasks(projectId)",
                        "CREATE INDEX IF NOT EXISTS index_tasks_chatRoomId ON tasks(chatRoomId)",
                        "CREATE INDEX IF NOT EXISTS index_tasks_assignedToId ON tasks(assignedToId)",
                        "CREATE INDEX IF NOT EXISTS index_tasks_createdById ON tasks(createdById)"
                    )
                )

                // --- 5. messages: chatRoom/sender CASCADE → NO_ACTION ---
                recreateTable(database,
                    oldTable = "messages",
                    createSql = """
                        CREATE TABLE messages_new (
                            id TEXT PRIMARY KEY NOT NULL,
                            chatRoomId TEXT NOT NULL,
                            senderId TEXT NOT NULL,
                            senderName TEXT NOT NULL,
                            senderPhotoUrl TEXT,
                            content TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            voiceMessageId TEXT,
                            taskIds TEXT NOT NULL,
                            replyToMessageId TEXT,
                            isEdited INTEGER NOT NULL,
                            editedAt INTEGER,
                            reactions TEXT NOT NULL,
                            readBy TEXT NOT NULL,
                            FOREIGN KEY (chatRoomId) REFERENCES chat_rooms(id) ON DELETE NO ACTION,
                            FOREIGN KEY (senderId) REFERENCES users(id) ON DELETE NO ACTION,
                            FOREIGN KEY (voiceMessageId) REFERENCES voice_messages(id) ON DELETE SET NULL,
                            FOREIGN KEY (replyToMessageId) REFERENCES messages_new(id) ON DELETE SET NULL
                        )
                    """,
                    copySql = """
                        INSERT INTO messages_new
                        SELECT id, chatRoomId, senderId, senderName, senderPhotoUrl,
                               content, timestamp, type, voiceMessageId, taskIds,
                               replyToMessageId, isEdited, editedAt, reactions, readBy
                        FROM messages
                    """,
                    indexes = listOf(
                        "CREATE INDEX IF NOT EXISTS index_messages_chatRoomId ON messages(chatRoomId)",
                        "CREATE INDEX IF NOT EXISTS index_messages_senderId ON messages(senderId)",
                        "CREATE INDEX IF NOT EXISTS index_messages_voiceMessageId ON messages(voiceMessageId)",
                        "CREATE INDEX IF NOT EXISTS index_messages_replyToMessageId ON messages(replyToMessageId)",
                        "CREATE INDEX IF NOT EXISTS index_messages_timestamp ON messages(timestamp)"
                    )
                )

                // --- 6. action_items: message/voice/chatRoom CASCADE → NO_ACTION ---
                recreateTable(database,
                    oldTable = "action_items",
                    createSql = """
                        CREATE TABLE action_items_new (
                            id TEXT PRIMARY KEY NOT NULL,
                            messageId TEXT,
                            voiceMessageId TEXT,
                            chatRoomId TEXT NOT NULL,
                            type TEXT NOT NULL,
                            text TEXT NOT NULL,
                            extractedText TEXT NOT NULL,
                            confidence REAL NOT NULL,
                            isProcessed INTEGER NOT NULL,
                            taskId TEXT,
                            reminderTime INTEGER,
                            createdAt INTEGER NOT NULL,
                            FOREIGN KEY (messageId) REFERENCES messages(id) ON DELETE NO ACTION,
                            FOREIGN KEY (voiceMessageId) REFERENCES voice_messages(id) ON DELETE NO ACTION,
                            FOREIGN KEY (chatRoomId) REFERENCES chat_rooms(id) ON DELETE NO ACTION,
                            FOREIGN KEY (taskId) REFERENCES tasks(id) ON DELETE SET NULL
                        )
                    """,
                    copySql = """
                        INSERT INTO action_items_new
                        SELECT id, messageId, voiceMessageId, chatRoomId, type, text,
                               extractedText, confidence, isProcessed, taskId, reminderTime, createdAt
                        FROM action_items
                    """,
                    indexes = listOf(
                        "CREATE INDEX IF NOT EXISTS index_action_items_messageId ON action_items(messageId)",
                        "CREATE INDEX IF NOT EXISTS index_action_items_voiceMessageId ON action_items(voiceMessageId)",
                        "CREATE INDEX IF NOT EXISTS index_action_items_chatRoomId ON action_items(chatRoomId)",
                        "CREATE INDEX IF NOT EXISTS index_action_items_taskId ON action_items(taskId)"
                    )
                )

                // Re-enable FK enforcement
                database.execSQL("PRAGMA foreign_keys = ON")

                android.util.Log.d("KosmosDatabase", "✅ Migration 9→10 complete: All CASCADE FKs changed to NO_ACTION (prevents data loss during sync)")
            }

            /**
             * Helper: recreate table with new schema, copy data, drop old, rename new, add indexes
             */
            private fun recreateTable(
                database: SupportSQLiteDatabase,
                oldTable: String,
                createSql: String,
                copySql: String,
                indexes: List<String>
            ) {
                database.execSQL(createSql.trimIndent())
                database.execSQL(copySql.trimIndent())
                database.execSQL("DROP TABLE $oldTable")
                database.execSQL("ALTER TABLE ${oldTable}_new RENAME TO $oldTable")
                indexes.forEach { database.execSQL(it) }
            }
        }

        /**
         * Migration 10→11: Add time_entries and task_dependencies tables
         *
         * - time_entries: Time tracking entries per task (timer + manual)
         * - task_dependencies: Dependency relationships between tasks
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create time_entries table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS time_entries (
                        id TEXT PRIMARY KEY NOT NULL,
                        taskId TEXT NOT NULL,
                        projectId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        durationSeconds INTEGER,
                        description TEXT,
                        isBillable INTEGER NOT NULL DEFAULT 1,
                        hourlyRate REAL,
                        isManual INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                database.execSQL("CREATE INDEX IF NOT EXISTS index_time_entries_taskId ON time_entries(taskId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_time_entries_projectId ON time_entries(projectId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_time_entries_userId ON time_entries(userId)")

                // Create task_dependencies table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS task_dependencies (
                        id TEXT PRIMARY KEY NOT NULL,
                        taskId TEXT NOT NULL,
                        dependsOnTaskId TEXT NOT NULL,
                        dependencyType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        createdBy TEXT NOT NULL
                    )
                """.trimIndent())

                database.execSQL("CREATE INDEX IF NOT EXISTS index_task_dependencies_taskId ON task_dependencies(taskId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_task_dependencies_dependsOnTaskId ON task_dependencies(dependsOnTaskId)")

                android.util.Log.d("KosmosDatabase", "✅ Migration 10→11 complete: Added time_entries and task_dependencies tables")
            }
        }

        /**
         * Migration 11→12: Add project_invites, user_connections, project_join_requests tables
         *
         * - project_invites: Invite workflow (PENDING → ACCEPTED/DECLINED/EXPIRED)
         * - user_connections: Social graph (friend requests)
         * - project_join_requests: Public project join workflow
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create project_invites table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS project_invites (
                        id TEXT PRIMARY KEY NOT NULL,
                        projectId TEXT NOT NULL,
                        inviteeId TEXT NOT NULL,
                        inviterId TEXT NOT NULL,
                        role TEXT NOT NULL DEFAULT 'MEMBER',
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        message TEXT,
                        createdAt INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        respondedAt INTEGER
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_project_invites_projectId ON project_invites(projectId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_project_invites_inviteeId ON project_invites(inviteeId)")

                // Create user_connections table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_connections (
                        id TEXT PRIMARY KEY NOT NULL,
                        requesterId TEXT NOT NULL,
                        addresseeId TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        createdAt INTEGER NOT NULL,
                        respondedAt INTEGER
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_connections_requesterId ON user_connections(requesterId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_connections_addresseeId ON user_connections(addresseeId)")

                // Create project_join_requests table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS project_join_requests (
                        id TEXT PRIMARY KEY NOT NULL,
                        projectId TEXT NOT NULL,
                        requesterId TEXT NOT NULL,
                        message TEXT,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        reviewedBy TEXT,
                        createdAt INTEGER NOT NULL,
                        respondedAt INTEGER
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_project_join_requests_projectId ON project_join_requests(projectId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_project_join_requests_requesterId ON project_join_requests(requesterId)")

                android.util.Log.d("KosmosDatabase", "✅ Migration 11→12 complete: Added project_invites, user_connections, project_join_requests tables")
            }
        }
    }
}