package com.example.kosmos.shared.utils

import com.example.kosmos.core.exceptions.ConflictException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * P1-08: Error Mapper for User-Friendly Error Messages
 *
 * Translates technical exceptions into actionable, user-friendly messages.
 * Use this instead of showing raw exception messages to users.
 */
object ErrorMapper {

    /**
     * Map any exception to a user-friendly error message
     *
     * @param error The exception to map
     * @param context Optional context (e.g., "create project", "send message")
     * @return User-friendly error message with actionable guidance
     */
    fun mapError(error: Throwable?, context: String = "operation"): String {
        return when (error) {
            // P1-11: Conflict errors (optimistic locking)
            is ConflictException -> "This ${error.entityType.lowercase()} was modified by someone else. Please refresh and try again."

            // Network errors
            is UnknownHostException -> "No internet connection. Please check your network and try again."
            is SocketTimeoutException -> "Request timed out. Please check your connection and try again."
            is TimeoutCancellationException -> "Request took too long. Please try again."
            is IOException -> "Network error. Please check your connection."

            // Supabase errors
            is RestException -> mapRestException(error, context)

            // Generic errors
            is IllegalArgumentException -> "Invalid input. ${error.message ?: "Please check your data."}"
            is IllegalStateException -> "Something went wrong. ${error.message ?: "Please try again."}"

            // Unknown errors
            null -> "Failed to $context. Please try again."
            else -> {
                val message = error.message
                if (message.isNullOrBlank()) {
                    "Failed to $context. Please try again."
                } else {
                    // Try to make message user-friendly
                    message.replace("_", " ")
                        .replaceFirstChar { it.uppercase() }
                        .let {
                            if (it.endsWith(".")) it else "$it."
                        }
                }
            }
        }
    }

    /**
     * Map Supabase REST exceptions to user-friendly messages
     */
    private fun mapRestException(error: RestException, context: String): String {
        val statusCode = error.statusCode
        val message = error.message?.lowercase() ?: ""

        return when {
            // Auth errors
            statusCode == 401 -> "Session expired. Please log in again."

            // Duplicate/conflict errors (409, 23505 = unique violation)
            statusCode == 409 || message.contains("duplicate") || message.contains("unique") -> {
                "This ${context.split(" ").lastOrNull() ?: "item"} already exists. Please choose a different name."
            }

            // Not found errors
            statusCode == 404 -> "Not found. This item may have been deleted."

            // Permission errors
            statusCode == 403 || message.contains("permission") || message.contains("policy") -> {
                "You don't have permission to do this. Please contact your project admin."
            }

            // Bad request
            statusCode == 400 -> "Invalid request. Please check your input and try again."

            // Server errors
            statusCode in 500..599 -> "Server error. Please try again later."

            // Unknown Supabase error
            else -> "Failed to $context. ${error.message ?: "Please try again."}"
        }
    }

    /**
     * Map validation errors to user-friendly messages
     * Used with ValidationUtils
     */
    fun mapValidationErrors(errors: Map<String, String>): String {
        if (errors.isEmpty()) return ""

        return if (errors.size == 1) {
            errors.values.first()
        } else {
            "Please fix the following errors:\n" + errors.values.joinToString("\n") { "• $it" }
        }
    }

    /**
     * Get retry message with action button text
     */
    fun getRetryMessage(error: Throwable?): Pair<String, String> {
        val message = mapError(error)
        val action = when (error) {
            is UnknownHostException, is SocketTimeoutException, is IOException -> "Retry"
            is RestException -> if (error.statusCode == 401) "Log In" else "Try Again"
            else -> "Try Again"
        }
        return message to action
    }
}
