package com.example.kosmos.core.exceptions

/**
 * P1-11: Exception thrown when optimistic locking detects a conflict
 * Used for concurrent update detection
 */
data class ConflictException(
    val entityType: String,
    val entityId: String,
    val localVersion: Int,
    val serverVersion: Int,
    val localData: Any,
    val serverData: Any
) : Exception("Conflict detected: $entityType $entityId has been modified. Local version: $localVersion, Server version: $serverVersion")

/**
 * Exception thrown when a network operation fails
 */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when validation fails
 */
class ValidationException(message: String) : Exception(message)

/**
 * Exception thrown when authentication fails
 */
class AuthenticationException(message: String) : Exception(message)

/**
 * Exception thrown when authorization fails (insufficient permissions)
 */
class AuthorizationException(message: String) : Exception(message)
