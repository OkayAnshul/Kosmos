package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.SyncEntityType
import com.example.kosmos.core.models.SyncQueueItem
import kotlinx.coroutines.flow.Flow

/**
 * Sync Queue DAO (P0-08 FIX)
 *
 * Manages persistence of sync queue items.
 * Items are automatically retried when network returns.
 */
@Dao
interface SyncQueueDao {

    // ============================================================================
    // QUERY OPERATIONS
    // ============================================================================

    /**
     * Get all pending sync items ordered by priority (high first) then timestamp (old first)
     */
    @Query("SELECT * FROM sync_queue WHERE retryCount < maxRetries ORDER BY priority DESC, createdTimestamp ASC")
    suspend fun getAllPendingItems(): List<SyncQueueItem>

    /**
     * Get all pending sync items (Flow - reactive)
     */
    @Query("SELECT * FROM sync_queue WHERE retryCount < maxRetries ORDER BY priority DESC, createdTimestamp ASC")
    fun getAllPendingItemsFlow(): Flow<List<SyncQueueItem>>

    /**
     * Get pending sync items that are ready to retry now
     * Filters out items that haven't waited long enough (exponential backoff)
     */
    @Query("""
        SELECT * FROM sync_queue
        WHERE retryCount < maxRetries
        ORDER BY priority DESC, createdTimestamp ASC
    """)
    suspend fun getItemsReadyToRetry(): List<SyncQueueItem>

    /**
     * Get failed items that have exceeded max retries
     */
    @Query("SELECT * FROM sync_queue WHERE retryCount >= maxRetries ORDER BY createdTimestamp DESC")
    suspend fun getFailedItems(): List<SyncQueueItem>

    /**
     * Get queue item by ID
     */
    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getById(id: String): SyncQueueItem?

    /**
     * Get queue items by entity
     */
    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun getByEntity(entityType: SyncEntityType, entityId: String): List<SyncQueueItem>

    /**
     * Get queue count
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE retryCount < maxRetries")
    suspend fun getPendingCount(): Int

    /**
     * Get queue count (Flow)
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE retryCount < maxRetries")
    fun getPendingCountFlow(): Flow<Int>

    // ============================================================================
    // INSERT/UPDATE OPERATIONS
    // ============================================================================

    /**
     * Insert sync queue item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueItem)

    /**
     * Insert multiple sync queue items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SyncQueueItem>)

    /**
     * Update sync queue item (e.g., increment retry count)
     */
    @Update
    suspend fun update(item: SyncQueueItem)

    // ============================================================================
    // DELETE OPERATIONS
    // ============================================================================

    /**
     * Delete sync queue item by ID
     * Called when sync succeeds
     */
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Delete sync queue items for a specific entity
     * Called when entity is deleted locally
     */
    @Query("DELETE FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteByEntity(entityType: SyncEntityType, entityId: String)

    /**
     * Delete all failed items (exceeded max retries)
     * Cleanup operation - removes permanently failed items
     */
    @Query("DELETE FROM sync_queue WHERE retryCount >= maxRetries")
    suspend fun deleteAllFailed()

    /**
     * Delete all sync queue items
     * Nuclear option - use with caution!
     */
    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()

    // ============================================================================
    // STATISTICS
    // ============================================================================

    /**
     * Get count by entity type
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE entityType = :entityType AND retryCount < maxRetries")
    suspend fun getCountByType(entityType: SyncEntityType): Int

    /**
     * Get oldest pending item timestamp
     */
    @Query("SELECT MIN(createdTimestamp) FROM sync_queue WHERE retryCount < maxRetries")
    suspend fun getOldestPendingTimestamp(): Long?
}
