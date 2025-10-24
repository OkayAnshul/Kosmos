package com.example.kosmos.data.repository

import com.example.kosmos.core.database.dao.UserDao
import com.example.kosmos.core.models.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling user operations
 * Manages user profiles, presence, and data synchronization
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
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
}