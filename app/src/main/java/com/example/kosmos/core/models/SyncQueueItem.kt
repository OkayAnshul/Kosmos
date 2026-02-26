package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Sync Queue Item (P0-08 FIX)
 *
 * Represents a failed Supabase operation that needs to be retried.
 * Stored in Room database and automatically retried when network returns.
 *
 * Pattern:
 * 1. Operation fails (network error, FK violation, etc.)
 * 2. Create SyncQueueItem with operation details
 * 3. NetworkMonitor detects network → SyncQueueManager retries
 * 4. On success → remove from queue
 * 5. On failure → increment retry count, exponential backoff
 */
@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Operation details
    val entityType: SyncEntityType,  // What type of entity (Task, Message, etc.)
    val entityId: String,  // ID of the entity
    val operation: SyncOperation,  // CREATE, UPDATE, DELETE
    val entityJson: String,  // JSON representation of entity (for CREATE/UPDATE)

    // Retry metadata
    val retryCount: Int = 0,
    val maxRetries: Int = 5,  // Give up after 5 failures
    val lastAttemptTimestamp: Long = System.currentTimeMillis(),
    val createdTimestamp: Long = System.currentTimeMillis(),

    // Error tracking
    val lastErrorMessage: String? = null,
    val lastErrorCode: String? = null,

    // Priority (higher = more important)
    val priority: Int = 0  // Future: prioritize user actions > background sync
)

/**
 * Type of entity being synced
 */
enum class SyncEntityType {
    TASK,
    TASK_ACTIVITY,
    PROJECT,
    PROJECT_MEMBER,
    MESSAGE,
    CHAT_ROOM,
    USER,
    VOICE_MESSAGE,
    ACTION_ITEM,
    PROJECT_INVITE,
    USER_CONNECTION,
    JOIN_REQUEST,
    TIME_ENTRY,
    TASK_DEPENDENCY,
    MILESTONE
}

/**
 * Type of sync operation
 */
enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Extension: Calculate next retry delay (exponential backoff)
 * Formula: min(60s, 2^retryCount seconds)
 */
fun SyncQueueItem.getNextRetryDelayMs(): Long {
    val baseDelayMs = 1000L  // 1 second
    val exponentialDelay = baseDelayMs * (1 shl retryCount)  // 2^retryCount
    val maxDelayMs = 60_000L  // 60 seconds max
    return minOf(exponentialDelay, maxDelayMs)
}

/**
 * Extension: Check if item should be retried now
 */
fun SyncQueueItem.shouldRetryNow(): Boolean {
    if (retryCount >= maxRetries) return false  // Exceeded max retries

    val timeSinceLastAttempt = System.currentTimeMillis() - lastAttemptTimestamp
    val nextRetryDelay = getNextRetryDelayMs()

    return timeSinceLastAttempt >= nextRetryDelay
}

/**
 * Extension: Check if item has exceeded max retries
 */
fun SyncQueueItem.hasExceededMaxRetries(): Boolean {
    return retryCount >= maxRetries
}
