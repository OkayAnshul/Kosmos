package com.example.kosmos.data.sync

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import kotlinx.coroutines.CancellationException

/**
 * Foreign Key Error Handler
 *
 * Utility for detecting and logging foreign key constraint violations.
 *
 * Why this exists:
 * - Migration 5→6 enabled foreign key enforcement (PRAGMA foreign_keys = ON)
 * - If users aren't synced before dependent entities, inserts fail with FK violations
 * - This handler provides clear error messages to help diagnose sync order issues
 *
 * Usage:
 * ```kotlin
 * try {
 *     dao.insert(entity)
 * } catch (e: Exception) {
 if (e is CancellationException) throw e
 *     if (ForeignKeyErrorHandler.isForeignKeyViolation(e)) {
 *         ForeignKeyErrorHandler.logForeignKeyError(e, "Task", task.id, "insert")
 *         // Continue or skip this entity
 *     } else {
 *         throw e  // Re-throw non-FK errors
 *     }
 * }
 * ```
 */
object ForeignKeyErrorHandler {
    private const val TAG = "FKErrorHandler"

    /**
     * Check if an exception is a foreign key constraint violation
     *
     * @param e Exception to check
     * @return True if FK violation, false otherwise
     */
    fun isForeignKeyViolation(e: Exception): Boolean {
        return e is SQLiteConstraintException &&
               e.message?.contains("FOREIGN KEY constraint failed", ignoreCase = true) == true
    }

    /**
     * Log a detailed foreign key error message
     *
     * Provides actionable debugging info including:
     * - Entity type and ID
     * - Operation that failed
     * - Expected sync order
     *
     * @param e The exception
     * @param entityType Type of entity (e.g., "Task", "ProjectMember")
     * @param entityId ID of the entity
     * @param operation Operation that failed (e.g., "insert", "update")
     */
    fun logForeignKeyError(e: Exception, entityType: String, entityId: String, operation: String) {
        if (isForeignKeyViolation(e)) {
            Log.e(TAG, """
                ❌ FOREIGN KEY VIOLATION
                   Entity: $entityType ($entityId)
                   Operation: $operation
                   Error: ${e.message}

                   SOLUTION: Check sync order. Users must sync BEFORE dependent entities.
                   Expected: Users → Projects → Members → ChatRooms → Messages → Tasks

                   Possible causes:
                   1. Users table not synced before this entity
                   2. Referenced user doesn't exist in local database
                   3. Sync order changed (should be sequential, not parallel)
            """.trimIndent())
        } else {
            Log.e(TAG, "Error in $operation for $entityType ($entityId)", e)
        }
    }

    /**
     * Log FK error with additional context
     *
     * @param e The exception
     * @param entityType Type of entity
     * @param entityId ID of the entity
     * @param operation Operation that failed
     * @param referencedTable Table that the FK points to (e.g., "users")
     * @param referencedId The missing referenced ID
     */
    fun logForeignKeyErrorWithContext(
        e: Exception,
        entityType: String,
        entityId: String,
        operation: String,
        referencedTable: String,
        referencedId: String
    ) {
        if (isForeignKeyViolation(e)) {
            Log.e(TAG, """
                ❌ FOREIGN KEY VIOLATION
                   Entity: $entityType ($entityId)
                   Operation: $operation
                   Referenced: $referencedTable ($referencedId)
                   Error: ${e.message}

                   SOLUTION: Ensure $referencedTable is synced before $entityType.
                   Missing record: $referencedTable.id = $referencedId

                   Expected sync order: Users → Projects → Members → ChatRooms → Messages → Tasks
            """.trimIndent())
        } else {
            Log.e(TAG, "Error in $operation for $entityType ($entityId)", e)
        }
    }
}
