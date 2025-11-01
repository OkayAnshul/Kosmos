package com.example.kosmos.data.sync

import android.util.Log
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.delay

/**
 * Helper for handling Supabase sync failures with intelligent retry logic
 *
 * Primary use case: Resolving foreign key constraint violations when parent entities
 * haven't synced to Supabase yet (e.g., message sync fails because chat room not synced)
 *
 * Strategy: Exponential backoff retry (1s, 2s, 4s) to give parent time to sync
 */
object SyncRetryHelper {

    private const val TAG = "SyncRetryHelper"

    /**
     * Retry a Supabase sync operation if it fails due to foreign key violation
     *
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @param initialDelayMs Initial delay before first retry (default: 1000ms)
     * @param entityName Name of entity being synced (for logging)
     * @param block Supabase sync operation to execute
     * @return Result of the sync operation (success or failure)
     */
    suspend fun <T> retryOnForeignKeyViolation(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        entityName: String = "entity",
        block: suspend () -> Result<T>
    ): Result<T> {
        var lastException: Throwable? = null
        var attempt = 0

        repeat(maxRetries) { retryCount ->
            attempt = retryCount + 1

            try {
                val result = block()

                if (result.isSuccess) {
                    if (retryCount > 0) {
                        Log.d(TAG, "✅ Retry successful for $entityName after $attempt attempts")
                    }
                    return result
                }

                val exception = result.exceptionOrNull()
                lastException = exception

                // Check if it's a foreign key violation
                if (exception != null && isForeignKeyViolation(exception)) {
                    val parentEntity = getParentEntityName(exception)

                    if (retryCount < maxRetries - 1) {
                        val delayMs = initialDelayMs * (1 shl retryCount) // Exponential backoff: 1s, 2s, 4s
                        Log.w(TAG, "⚠️ FK violation for $entityName (parent: $parentEntity). Retrying in ${delayMs}ms (attempt ${attempt}/$maxRetries)")
                        delay(delayMs)
                    } else {
                        Log.e(TAG, "❌ FK violation for $entityName. All $maxRetries retry attempts exhausted.")
                        return result
                    }
                } else {
                    // Not a FK violation, don't retry
                    return result
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "❌ Unexpected error during retry attempt $attempt for $entityName", e)
                return Result.failure(e)
            }
        }

        // All retries exhausted
        return Result.failure(lastException ?: Exception("Unknown error after $maxRetries retries"))
    }

    /**
     * Check if an exception is a foreign key constraint violation
     *
     * PostgreSQL FK violation error code: 23503
     *
     * @param error The exception to check
     * @return true if this is a FK violation
     */
    fun isForeignKeyViolation(error: Throwable?): Boolean {
        if (error == null) return false

        return when (error) {
            is PostgrestRestException -> {
                val detailsStr = error.details?.toString() ?: ""
                error.code == "23503" ||
                error.message?.contains("foreign key constraint", ignoreCase = true) == true ||
                detailsStr.contains("is not present in table", ignoreCase = true)
            }
            else -> {
                error.message?.contains("foreign key constraint", ignoreCase = true) == true ||
                error.message?.contains("23503", ignoreCase = true) == true
            }
        }
    }

    /**
     * Extract the parent entity name from a FK violation error
     *
     * Example error details:
     * "Key (chat_room_id)=(xxx) is not present in table \"chat_rooms\"."
     *
     * @param error The FK violation exception
     * @return Parent entity name (e.g., "chat_rooms") or "unknown parent"
     */
    fun getParentEntityName(error: Throwable?): String {
        if (error == null) return "unknown parent"

        return when (error) {
            is PostgrestRestException -> {
                val details = error.details?.toString() ?: error.message ?: ""
                extractTableName(details)
            }
            else -> {
                val message = error.message ?: ""
                extractTableName(message)
            }
        }
    }

    /**
     * Extract table name from error message
     *
     * Looks for pattern: "table \"table_name\""
     *
     * @param errorMessage Error message containing table reference
     * @return Table name or "unknown parent"
     */
    private fun extractTableName(errorMessage: String): String {
        // Match: table "table_name"
        val tablePattern = """table\s+"([^"]+)"""".toRegex()
        val match = tablePattern.find(errorMessage)

        return match?.groupValues?.getOrNull(1) ?: "unknown parent"
    }

    /**
     * Get a user-friendly error message for FK violation
     *
     * @param entityName Entity that failed to sync
     * @param parentName Parent entity that doesn't exist
     * @return User-friendly error message
     */
    fun getFriendlyErrorMessage(entityName: String, parentName: String): String {
        return "Cannot sync $entityName because parent $parentName hasn't synced yet. " +
                "This usually happens when creating data while offline. " +
                "The app will automatically retry syncing."
    }

    /**
     * Check if error is a network connectivity issue
     *
     * @param error The exception to check
     * @return true if this is a network error
     */
    fun isNetworkError(error: Throwable?): Boolean {
        if (error == null) return false

        val message = error.message ?: ""
        return message.contains("Unable to resolve host", ignoreCase = true) ||
                message.contains("No address associated with hostname", ignoreCase = true) ||
                message.contains("network error", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true)
    }

    /**
     * Get a user-friendly error message for any sync failure
     *
     * @param error The sync failure exception
     * @param entityName Entity that failed to sync
     * @return User-friendly error message
     */
    fun getDiagnosticMessage(error: Throwable?, entityName: String): String {
        if (error == null) return "Unknown sync error for $entityName"

        return when {
            isNetworkError(error) -> {
                "No internet connection. $entityName saved locally and will sync when online."
            }
            isForeignKeyViolation(error) -> {
                val parentName = getParentEntityName(error)
                getFriendlyErrorMessage(entityName, parentName)
            }
            error is PostgrestRestException && error.code == "42501" -> {
                "Permission denied. Check Row Level Security (RLS) policies for $entityName."
            }
            error is PostgrestRestException && error.code == "PGRST204" -> {
                "Schema mismatch. Database column missing for $entityName."
            }
            else -> {
                "Sync failed for $entityName: ${error.message}. Saved locally only."
            }
        }
    }
}
