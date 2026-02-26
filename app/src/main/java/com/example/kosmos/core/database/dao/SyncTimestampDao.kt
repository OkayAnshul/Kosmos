package com.example.kosmos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kosmos.core.models.SyncTimestamp

/**
 * DAO for Sync Timestamp operations
 *
 * Manages sync timestamps for incremental sync.
 * Enables fetching only data modified since last sync.
 */
@Dao
interface SyncTimestampDao {

    /**
     * Get the last sync timestamp for a specific resource
     *
     * @param id Sync timestamp ID (e.g., "project123_members", "global_users")
     * @return Last sync timestamp in milliseconds, or null if never synced
     */
    @Query("SELECT lastSyncTimestamp FROM sync_timestamps WHERE id = :id")
    suspend fun getLastSyncTimestamp(id: String): Long?

    /**
     * Update or insert a sync timestamp
     * Uses REPLACE strategy to update existing records
     *
     * @param syncTimestamp Sync timestamp to save
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncTimestamp(syncTimestamp: SyncTimestamp)

    /**
     * Update sync timestamp for a project resource
     *
     * @param projectId Project ID
     * @param resourceType Resource type (e.g., "members", "chat_rooms", "tasks")
     * @param timestamp Sync timestamp in milliseconds
     */
    suspend fun updateProjectResourceTimestamp(
        projectId: String,
        resourceType: String,
        timestamp: Long
    ) {
        val id = SyncTimestamp.createProjectResourceId(projectId, resourceType)
        val syncTimestamp = SyncTimestamp(
            id = id,
            projectId = projectId,
            resourceType = resourceType,
            lastSyncTimestamp = timestamp,
            updatedAt = System.currentTimeMillis()
        )
        upsertSyncTimestamp(syncTimestamp)
    }

    /**
     * Update sync timestamp for a global resource
     *
     * @param resourceType Resource type (e.g., "users")
     * @param timestamp Sync timestamp in milliseconds
     */
    suspend fun updateGlobalResourceTimestamp(
        resourceType: String,
        timestamp: Long
    ) {
        val id = SyncTimestamp.createGlobalResourceId(resourceType)
        val syncTimestamp = SyncTimestamp(
            id = id,
            projectId = null,
            resourceType = resourceType,
            lastSyncTimestamp = timestamp,
            updatedAt = System.currentTimeMillis()
        )
        upsertSyncTimestamp(syncTimestamp)
    }

    /**
     * Get last sync timestamp for a project resource
     *
     * @param projectId Project ID
     * @param resourceType Resource type
     * @return Last sync timestamp or null if never synced
     */
    suspend fun getProjectResourceTimestamp(
        projectId: String,
        resourceType: String
    ): Long? {
        val id = SyncTimestamp.createProjectResourceId(projectId, resourceType)
        return getLastSyncTimestamp(id)
    }

    /**
     * Get last sync timestamp for a global resource
     *
     * @param resourceType Resource type
     * @return Last sync timestamp or null if never synced
     */
    suspend fun getGlobalResourceTimestamp(
        resourceType: String
    ): Long? {
        val id = SyncTimestamp.createGlobalResourceId(resourceType)
        return getLastSyncTimestamp(id)
    }

    /**
     * Delete all sync timestamps for a project
     * Useful when leaving/deleting a project
     *
     * @param projectId Project ID
     */
    @Query("DELETE FROM sync_timestamps WHERE projectId = :projectId")
    suspend fun deleteProjectTimestamps(projectId: String)

    /**
     * Delete all sync timestamps (for logout/app reset)
     */
    @Query("DELETE FROM sync_timestamps")
    suspend fun deleteAllTimestamps()

    /**
     * Get all sync timestamps for a project
     *
     * @param projectId Project ID
     * @return List of sync timestamps for the project
     */
    @Query("SELECT * FROM sync_timestamps WHERE projectId = :projectId")
    suspend fun getProjectTimestamps(projectId: String): List<SyncTimestamp>
}
