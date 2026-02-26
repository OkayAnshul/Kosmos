package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sync Timestamp Entity
 *
 * Tracks the last successful sync timestamp for each resource type per project.
 * Enables incremental sync: only fetch data modified since last sync.
 *
 * Benefits:
 * - Reduces data transfer (only fetch new/updated data)
 * - Faster sync times (skip unchanged data)
 * - Better offline experience (less network usage)
 *
 * Resource Types:
 * - "members" - Project members
 * - "chat_rooms" - Chat rooms
 * - "messages_{chatRoomId}" - Messages for specific chat room
 * - "tasks" - Tasks
 * - "users" - Users (global, not project-specific)
 */
@Entity(tableName = "sync_timestamps")
data class SyncTimestamp(
    /**
     * Composite key: "{projectId}_{resourceType}" or "global_{resourceType}"
     * Examples:
     * - "project123_members"
     * - "project123_chat_rooms"
     * - "project123_messages_room456"
     * - "project123_tasks"
     * - "global_users"
     */
    @PrimaryKey
    val id: String,

    /**
     * Project ID (null for global resources like users)
     */
    val projectId: String?,

    /**
     * Resource type being tracked
     */
    val resourceType: String,

    /**
     * Last successful sync timestamp (milliseconds since epoch)
     */
    val lastSyncTimestamp: Long,

    /**
     * When this record was last updated
     */
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Create a sync timestamp ID for a project resource
         */
        fun createProjectResourceId(projectId: String, resourceType: String): String {
            return "${projectId}_${resourceType}"
        }

        /**
         * Create a sync timestamp ID for a global resource
         */
        fun createGlobalResourceId(resourceType: String): String {
            return "global_${resourceType}"
        }

        // Resource type constants
        const val RESOURCE_USERS = "users"
        const val RESOURCE_MEMBERS = "members"
        const val RESOURCE_CHAT_ROOMS = "chat_rooms"
        const val RESOURCE_TASKS = "tasks"

        /**
         * Create a resource type for messages in a specific chat room
         */
        fun createMessagesResourceType(chatRoomId: String): String {
            return "messages_${chatRoomId}"
        }
    }
}
