package com.example.kosmos.data.repository

import com.example.kosmos.core.database.dao.ProjectMemberDao
import com.example.kosmos.core.database.dao.UserDao
import com.example.kosmos.core.models.User
import com.example.kosmos.core.models.SyncOperation
import com.example.kosmos.data.datasource.SupabaseUserDataSource
import com.example.kosmos.data.sync.SyncQueueHelper
import com.example.kosmos.features.demo.DemoMode
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Repository for handling user operations
 * Manages user profiles, presence, and data synchronization with Supabase
 * Implements hybrid architecture: Local Room cache + Remote Supabase sync
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val projectMemberDao: ProjectMemberDao,
    private val supabase: SupabaseClient,
    private val supabaseUserDataSource: SupabaseUserDataSource,
    private val networkMonitor: com.example.kosmos.shared.utils.NetworkMonitor,  // P0-06 FIX
    private val syncQueueDao: com.example.kosmos.core.database.dao.SyncQueueDao,  // P0-08 FIX
    private val demoMode: DemoMode
) {
    companion object {
        private const val TAG = "UserRepository"
    }

    /**
     * P0-06 FIX: Expose network connectivity state
     * UI can observe this to show offline banner
     */
    val isOffline: kotlinx.coroutines.flow.StateFlow<Boolean> = networkMonitor.isOffline

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
     * Cache a user profile to Room (no Supabase sync).
     * Use when you fetched a user from Supabase and just want to cache it locally.
     */
    suspend fun cacheUser(user: User) {
        userDao.insertUser(user)
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
     * Get recent collaborators for a user
     * Returns users who have worked with the given user in shared projects
     * Sorted by most recent activity
     *
     * This is used in the project creation wizard to suggest users
     * the creator has recently collaborated with
     *
     * @param userId User ID
     * @param limit Maximum number of collaborators to return (default 10)
     * @return List of users (recent collaborators)
     */
    suspend fun getRecentCollaborators(userId: String, limit: Int = 10): List<User> {
        return try {
            // Get all projects the user is a member of
            val userProjectIds = projectMemberDao.getUserProjectIds(userId)

            if (userProjectIds.isEmpty()) {
                return emptyList()
            }

            // Get collaborator IDs (other members in those projects)
            // Sorted by last activity, limited to requested count
            val collaboratorIds = projectMemberDao.getCollaboratorIds(
                projectIds = userProjectIds,
                excludeUserId = userId,
                limit = limit
            )

            if (collaboratorIds.isEmpty()) {
                return emptyList()
            }

            // Fetch user details for those collaborators
            userDao.getUsersByIds(collaboratorIds)

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emptyList()
        }
    }

    /**
     * Save or update user profile (P0-01 FIX)
     * Hybrid pattern: saves to Room immediately, then syncs to Supabase
     * @param user User to save
     * @return Result indicating success or failure
     */
    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            val userWithTimestamp = user.copy(
                createdAt = user.createdAt ?: System.currentTimeMillis(),
                version = 1 // New user starts at version 1
            )

            // Step 1: Save to Room immediately (offline-first)
            userDao.insertUser(userWithTimestamp)

            // Step 2: Sync to Supabase (background sync)
            try {
                val supabaseResult = supabaseUserDataSource.insert(userWithTimestamp)
                if (supabaseResult.isFailure) {
                    android.util.Log.w(TAG, "Failed to sync new user to Supabase (will retry later)", supabaseResult.exceptionOrNull())
                    // P0-08 FIX: Queue for automatic retry
                    SyncQueueHelper.queueUser(syncQueueDao, userWithTimestamp, SyncOperation.CREATE)
                    android.util.Log.d(TAG, "📥 User queued for retry: ${userWithTimestamp.id}")
                    // Don't fail - Room cache is updated, Supabase will sync later
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.w(TAG, "Error syncing new user to Supabase (offline mode?)", e)
                // P0-08 FIX: Queue for automatic retry
                SyncQueueHelper.queueUser(syncQueueDao, userWithTimestamp, SyncOperation.CREATE)
                android.util.Log.d(TAG, "📥 User queued for retry: ${userWithTimestamp.id}")
                // Continue - offline-first pattern
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    /**
     * Update user profile (P0-01 FIX)
     * Hybrid pattern with optimistic locking:
     * 1. Updates Room immediately (offline-first)
     * 2. Syncs to Supabase with version check
     * 3. Handles conflicts if user was modified on another device
     *
     * @param user User to update
     * @return Result indicating success or failure (ConflictException if version mismatch)
     */
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            // Step 1: Update Room immediately (offline-first)
            userDao.updateUser(user)

            // Step 2: Sync to Supabase
            try {
                val supabaseResult = supabaseUserDataSource.update(user)

                if (supabaseResult.isSuccess) {
                    val updatedUser = supabaseResult.getOrNull()
                    if (updatedUser != null) {
                        // Step 3: Update local cache with new version from Supabase
                        userDao.updateUser(updatedUser)
                    }
                } else {
                    android.util.Log.w(TAG, "Failed to sync user update to Supabase (will retry later)", supabaseResult.exceptionOrNull())
                    SyncQueueHelper.queueUser(syncQueueDao, user, SyncOperation.UPDATE)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.w(TAG, "Error syncing user update to Supabase (offline mode?)", e)
                SyncQueueHelper.queueUser(syncQueueDao, user, SyncOperation.UPDATE)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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

            // Sync presence to Supabase
            try {
                supabaseUserDataSource.updateOnlineStatus(userId, isOnline)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.w(TAG, "Error syncing presence to Supabase (offline?)", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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

            // Sync to Supabase
            try {
                val supabaseResult = supabaseUserDataSource.update(updatedUser)
                if (supabaseResult.isFailure) {
                    android.util.Log.w(TAG, "Failed to sync display name to Supabase", supabaseResult.exceptionOrNull())
                    SyncQueueHelper.queueUser(syncQueueDao, updatedUser, SyncOperation.UPDATE)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.w(TAG, "Error syncing display name to Supabase (offline?)", e)
                SyncQueueHelper.queueUser(syncQueueDao, updatedUser, SyncOperation.UPDATE)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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

            // Sync to Supabase
            try {
                val supabaseResult = supabaseUserDataSource.update(updatedUser)
                if (supabaseResult.isFailure) {
                    android.util.Log.w(TAG, "Failed to sync avatar URL to Supabase", supabaseResult.exceptionOrNull())
                    SyncQueueHelper.queueUser(syncQueueDao, updatedUser, SyncOperation.UPDATE)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.w(TAG, "Error syncing avatar URL to Supabase (offline?)", e)
                SyncQueueHelper.queueUser(syncQueueDao, updatedUser, SyncOperation.UPDATE)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
            // Step 1: Delete from Supabase first (if online)
            try {
                val supabaseResult = supabaseUserDataSource.delete(userId)
                if (supabaseResult.isFailure) {
                    android.util.Log.w(TAG, "Failed to delete user from Supabase", supabaseResult.exceptionOrNull())
                    // Queue for retry - user will be deleted from Supabase later
                    val user = userDao.getUserById(userId)
                    if (user != null) {
                        SyncQueueHelper.queueUser(syncQueueDao, user, SyncOperation.DELETE)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.w(TAG, "Error deleting user from Supabase (offline?)", e)
                val user = userDao.getUserById(userId)
                if (user != null) {
                    SyncQueueHelper.queueUser(syncQueueDao, user, SyncOperation.DELETE)
                }
            }

            // Step 2: Delete from Room
            userDao.deleteUserById(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
            if (e is CancellationException) throw e
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

            // Sync to Supabase using dedicated FCM token method
            try {
                val supabaseResult = supabaseUserDataSource.updateFcmToken(userId, fcmToken)
                if (supabaseResult.isFailure) {
                    android.util.Log.w(TAG, "Failed to sync FCM token to Supabase", supabaseResult.exceptionOrNull())
                    SyncQueueHelper.queueUser(syncQueueDao, updatedUser, SyncOperation.UPDATE)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.w(TAG, "Error syncing FCM token to Supabase (offline?)", e)
                SyncQueueHelper.queueUser(syncQueueDao, updatedUser, SyncOperation.UPDATE)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
                    val matchesQuery = user.username.contains(trimmedQuery, ignoreCase = true) ||
                                     user.displayName.contains(trimmedQuery, ignoreCase = true) ||
                                     user.email.contains(trimmedQuery, ignoreCase = true)
                    val notExcluded = !excludeIds.contains(user.id)
                    matchesQuery && notExcluded
                }
                .sortedWith(
                    compareByDescending<User> {
                        it.username.equals(trimmedQuery, ignoreCase = true) // Exact username match
                    }.thenByDescending {
                        it.username.startsWith(trimmedQuery, ignoreCase = true) // Username starts with
                    }.thenByDescending {
                        it.username.contains(trimmedQuery, ignoreCase = true) // Username contains
                    }.thenByDescending {
                        it.displayName.startsWith(trimmedQuery, ignoreCase = true) // Name starts with
                    }.thenByDescending {
                        it.displayName.contains(trimmedQuery, ignoreCase = true) // Name contains
                    }.thenBy {
                        it.displayName // Alphabetical fallback
                    }
                )
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

                // Sort Supabase results with username priority
                val sortedSupabaseUsers = supabaseUsers.sortedWith(
                    compareByDescending<User> {
                        it.username.equals(trimmedQuery, ignoreCase = true) // Exact username match
                    }.thenByDescending {
                        it.username.startsWith(trimmedQuery, ignoreCase = true) // Username starts with
                    }.thenByDescending {
                        it.username.contains(trimmedQuery, ignoreCase = true) // Username contains
                    }.thenByDescending {
                        it.displayName.startsWith(trimmedQuery, ignoreCase = true) // Name starts with
                    }.thenByDescending {
                        it.displayName.contains(trimmedQuery, ignoreCase = true) // Name contains
                    }.thenBy {
                        it.displayName // Alphabetical fallback
                    }
                )

                // Step 3: Cache Supabase results in Room
                if (sortedSupabaseUsers.isNotEmpty()) {
                    userDao.insertUsers(sortedSupabaseUsers)
                }

                // Emit fresh sorted results from Supabase
                emit(Result.success(sortedSupabaseUsers))
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
            if (e is CancellationException) throw e
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

            // Demo mode: no network — local cache is authoritative
            if (demoMode.isEnabled) return@flow

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
            if (e is CancellationException) throw e
            emit(Result.failure(e))
        }
    }

    /**
     * Get user by ID directly from Supabase (no cache)
     * Use this when you need guaranteed fresh data (e.g., checking if user is banned/deleted)
     *
     * @param userId User ID
     * @return Result with fresh user data from Supabase
     */
    suspend fun getUserByIdFromSupabase(userId: String): Result<User?> {
        return supabaseUserDataSource.getById(userId)
    }

    /**
     * Search users directly from Supabase (no cache)
     * Use this for user discovery where fresh data is critical
     *
     * @param query Search query
     * @param excludeIds User IDs to exclude from results
     * @param limit Maximum number of results
     * @return Result with fresh user list from Supabase
     */
    suspend fun searchUsersFromSupabase(
        query: String,
        excludeIds: List<String> = emptyList(),
        limit: Int = 50
    ): Result<List<User>> {
        if (demoMode.isEnabled) return Result.success(searchDemoUsers(query, excludeIds, limit))
        return supabaseUserDataSource.searchUsers(query, excludeIds, limit)
    }

    /**
     * Search users via users_public view (global discovery, no RLS restriction)
     */
    suspend fun searchUsersPublic(
        query: String,
        excludeIds: List<String> = emptyList(),
        limit: Int = 50
    ): Result<List<User>> {
        if (demoMode.isEnabled) return Result.success(searchDemoUsers(query, excludeIds, limit))
        return supabaseUserDataSource.searchUsersPublic(query, excludeIds, limit)
    }

    /**
     * Demo-mode fallback: search seeded users from the local Room cache.
     */
    private suspend fun searchDemoUsers(
        query: String,
        excludeIds: List<String>,
        limit: Int
    ): List<User> {
        if (query.isBlank()) return emptyList()
        val trimmed = query.trim()
        return try {
            userDao.getAllUsers()
                .filter { user ->
                    !excludeIds.contains(user.id) &&
                        (user.username.contains(trimmed, ignoreCase = true) ||
                            user.displayName.contains(trimmed, ignoreCase = true) ||
                            user.email.contains(trimmed, ignoreCase = true))
                }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get all users directly from Supabase (no cache)
     * Use this when you need complete fresh user list
     * Filters out users with invalid/empty IDs to prevent UUID errors
     *
     * @return Result with all valid users from Supabase
     */
    suspend fun getAllUsersFromSupabase(): Result<List<User>> {
        return try {
            // CRITICAL FIX: Wrap HTTP call in NonCancellable to prevent mid-flight cancellation
            val result = withContext(NonCancellable) {
                supabaseUserDataSource.getAll()
            }

            if (result.isFailure) {
                return result
            }

            val users = result.getOrNull() ?: emptyList()

            // Filter out users with invalid IDs
            val validUsers = users.filter { user ->
                user.id.isNotEmpty() &&
                user.id.isNotBlank() &&
                user.id.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE))
            }

            if (validUsers.size < users.size) {
                android.util.Log.w(TAG, "⚠️ Filtered out ${users.size - validUsers.size} invalid users with empty/malformed IDs")
            }

            Result.success(validUsers)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e(TAG, "Error fetching users from Supabase", e)
            Result.failure(e)
        }
    }

    /**
     * Check if username exists in Supabase
     * Used for username availability validation during registration
     *
     * @param username Username to check
     * @return True if username exists, false otherwise
     */
    suspend fun checkUsernameExists(username: String): Boolean {
        return try {
            val result = supabaseUserDataSource.getByUsername(username)
            result.isSuccess && result.getOrNull() != null
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // In case of error, assume username is taken to be safe
            true
        }
    }

    /**
     * Update user settings (privacy + notifications)
     * Hybrid pattern: Update Room cache immediately, then sync to Supabase
     *
     * @param userId User ID
     * @param settings User settings to save
     * @return Result indicating success or failure
     */
    suspend fun updateUserSettings(userId: String, settings: com.example.kosmos.core.models.UserSettings): Result<Unit> {
        return try {
            // Step 1: Get current user from Room
            val user = userDao.getUserById(userId)
                ?: return Result.failure(Exception("User not found"))

            // Step 2: Update user with new settings
            val updatedUser = user.copy(settings = settings)

            // Step 3: Update Room immediately (offline-first)
            userDao.updateUser(updatedUser)

            // Step 4: Sync to Supabase
            try {
                val supabaseResult = supabaseUserDataSource.update(updatedUser)
                if (supabaseResult.isFailure) {
                    android.util.Log.w(TAG, "Failed to sync settings to Supabase", supabaseResult.exceptionOrNull())
                    SyncQueueHelper.queueUser(syncQueueDao, updatedUser, SyncOperation.UPDATE)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.w(TAG, "Error syncing settings to Supabase (offline?)", e)
                SyncQueueHelper.queueUser(syncQueueDao, updatedUser, SyncOperation.UPDATE)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    /**
     * Get user settings
     * Returns settings from Room cache (fast, works offline)
     *
     * @param userId User ID
     * @return User settings or default settings if not found
     */
    suspend fun getUserSettings(userId: String): Result<com.example.kosmos.core.models.UserSettings> {
        return try {
            val user = userDao.getUserById(userId)
            val settings = user?.settings ?: com.example.kosmos.core.models.UserSettings()
            Result.success(settings)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    /**
     * Get user settings as Flow (reactive updates)
     *
     * @param userId User ID
     * @return Flow of user settings
     */
    fun getUserSettingsFlow(userId: String): Flow<com.example.kosmos.core.models.UserSettings> {
        return userDao.getUserByIdFlow(userId)
            .map { user ->
                user?.settings ?: com.example.kosmos.core.models.UserSettings()
            }
    }

    /**
     * Sync all users from Supabase to local cache
     * CRITICAL: Must run FIRST before syncing entities with User FK dependencies
     *
     * This prevents FK constraint violations when inserting:
     * - ProjectMembers (references users.id)
     * - Messages (references users.id as sender_id)
     * - Tasks (references users.id as assigned_to_id, created_by_id)
     *
     * @return Result indicating success or failure
     */
    suspend fun syncAllUsers(): Result<Unit> {
        return try {
            android.util.Log.d(TAG, "Starting user sync from Supabase")

            // CRITICAL FIX: Wrap HTTP call in NonCancellable to prevent mid-flight cancellation
            val usersResult = withContext(NonCancellable) {
                supabaseUserDataSource.getAll()
            }
            if (usersResult.isFailure) {
                android.util.Log.w(TAG, "Failed to fetch users", usersResult.exceptionOrNull())
                return usersResult.map { }
            }

            val users = usersResult.getOrNull() ?: emptyList()

            // Filter out invalid UUIDs to prevent database errors
            val validUsers = users.filter { user ->
                user.id.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE))
            }

            if (validUsers.size < users.size) {
                android.util.Log.w(TAG, "⚠️ Filtered out ${users.size - validUsers.size} users with invalid IDs")
            }

            // Batch insert to Room
            if (validUsers.isNotEmpty()) {
                userDao.insertUsers(validUsers)
            }

            android.util.Log.d(TAG, "✅ Synced ${validUsers.size} users to local cache")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e(TAG, "❌ User sync failed", e)
            Result.failure(e)
        }
    }
}