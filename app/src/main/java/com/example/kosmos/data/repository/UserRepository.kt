package com.example.kosmos.data.repository

import com.example.kosmos.core.database.dao.UserDao
import com.example.kosmos.core.models.User
import com.example.kosmos.data.datasource.SupabaseUserDataSource
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling user operations
 * Manages user profiles, presence, and data synchronization with Supabase
 * Implements hybrid architecture: Local Room cache + Remote Supabase sync
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val supabase: SupabaseClient,
    private val supabaseUserDataSource: SupabaseUserDataSource
) {

    /**
     * Get a user by ID
     * @param userId User ID
     * @return Flow of User or null
     */
    fun getUserByIdFlow(userId: String): Flow<User?> {
        return userDao.getUserByIdFlow(userId)
    }

    /**
     * Get user by ID (suspend function)
     * @param userId User ID
     * @return User or null
     */
    suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)
    }

    /**
     * Get multiple users by their IDs
     * @param userIds List of user IDs
     * @return List of users
     */
    suspend fun getUsersByIds(userIds: List<String>): List<User> {
        return userDao.getUsersByIds(userIds)
    }

    /**
     * Get all users
     * @return Flow of all users
     */
    fun getAllUsersFlow(): Flow<List<User>> {
        return userDao.getAllUsersFlow()
    }

    /**
     * Save or update user profile
     * @param user User to save
     * @return Result indicating success or failure
     */
    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            val userWithTimestamp = user.copy(
                createdAt = user.createdAt ?: System.currentTimeMillis()
            )
            userDao.insertUser(userWithTimestamp)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile
     * @param user User to update
     * @return Result indicating success or failure
     */
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            userDao.updateUser(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user presence status
     * @param userId User ID
     * @param isOnline Online status
     * @return Result indicating success or failure
     */
    suspend fun updateUserPresence(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found"))

            val updatedUser = user.copy(
                isOnline = isOnline,
                lastSeen = if (isOnline) user.lastSeen else System.currentTimeMillis()
            )

            userDao.updateUser(updatedUser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user display name
     * @param userId User ID
     * @param displayName New display name
     * @return Result indicating success or failure
     */
    suspend fun updateDisplayName(userId: String, displayName: String): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found"))

            val updatedUser = user.copy(displayName = displayName)
            userDao.updateUser(updatedUser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user avatar URL
     * @param userId User ID
     * @param avatarUrl New avatar URL
     * @return Result indicating success or failure
     */
    suspend fun updateAvatarUrl(userId: String, avatarUrl: String): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found"))

            val updatedUser = user.copy(photoUrl = avatarUrl)
            userDao.updateUser(updatedUser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a user
     * @param userId User ID
     * @return Result indicating success or failure
     */
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            userDao.deleteUserById(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save multiple users
     * @param users List of users to save
     * @return Result indicating success or failure
     */
    suspend fun saveUsers(users: List<User>): Result<Unit> {
        return try {
            userDao.insertUsers(users)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user FCM token
     * @param userId User ID
     * @param fcmToken New FCM token
     * @return Result indicating success or failure
     */
    suspend fun updateFcmToken(userId: String, fcmToken: String): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found"))

            val updatedUser = user.copy(fcmToken = fcmToken)
            userDao.updateUser(updatedUser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search for users by display name or email
     * Implements hybrid pattern:
     * 1. First searches local Room cache (fast, works offline)
     * 2. Then fetches from Supabase (fresh data)
     * 3. Caches Supabase results in Room
     *
     * @param query Search query (searches in display_name and email)
     * @param excludeIds User IDs to exclude from results (e.g., current user)
     * @param limit Maximum number of results to return
     * @return Flow emitting search results (first from cache, then from Supabase)
     */
    fun searchUsers(
        query: String,
        excludeIds: List<String> = emptyList(),
        limit: Int = 50
    ): Flow<Result<List<User>>> = flow {
        try {
            if (query.isBlank()) {
                emit(Result.success(emptyList()))
                return@flow
            }

            val trimmedQuery = query.trim()

            // Step 1: Search local Room cache first (fast, offline-capable)
            val localUsers = userDao.getAllUsers()
                .filter { user ->
                    val matchesQuery = user.displayName.contains(trimmedQuery, ignoreCase = true) ||
                                     user.email.contains(trimmedQuery, ignoreCase = true)
                    val notExcluded = !excludeIds.contains(user.id)
                    matchesQuery && notExcluded
                }
                .sortedBy { it.displayName }
                .take(limit)

            // Emit local results immediately (fast response)
            emit(Result.success(localUsers))

            // Step 2: Fetch from Supabase (fresh data)
            val supabaseResult = supabaseUserDataSource.searchUsers(
                query = trimmedQuery,
                excludeIds = excludeIds,
                limit = limit
            )

            if (supabaseResult.isSuccess) {
                val supabaseUsers = supabaseResult.getOrNull() ?: emptyList()

                // Step 3: Cache Supabase results in Room
                if (supabaseUsers.isNotEmpty()) {
                    userDao.insertUsers(supabaseUsers)
                }

                // Emit fresh results from Supabase
                emit(Result.success(supabaseUsers))
            } else {
                // If Supabase fetch fails (e.g., no internet), local cache was already emitted
                // Optionally emit error, but don't fail the whole flow
                val error = supabaseResult.exceptionOrNull()
                if (localUsers.isEmpty()) {
                    // Only emit error if we have no cached results
                    emit(Result.failure(error ?: Exception("Search failed")))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Get user by ID with hybrid sync
     * Fetches from Room first, then syncs with Supabase
     *
     * @param userId User ID
     * @return Flow emitting user (first from cache, then fresh from Supabase)
     */
    fun getUserByIdWithSync(userId: String): Flow<Result<User?>> = flow {
        try {
            // Step 1: Emit local data immediately
            val localUser = userDao.getUserById(userId)
            emit(Result.success(localUser))

            // Step 2: Fetch from Supabase
            val supabaseResult = supabaseUserDataSource.getById(userId)

            if (supabaseResult.isSuccess) {
                val supabaseUser = supabaseResult.getOrNull()

                // Step 3: Update local cache
                if (supabaseUser != null) {
                    userDao.insertUser(supabaseUser)
                    emit(Result.success(supabaseUser))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}