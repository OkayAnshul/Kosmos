package com.example.kosmos.data.datasource

import android.util.Log
import kotlinx.coroutines.CancellationException
import com.example.kosmos.core.exceptions.ConflictException
import com.example.kosmos.core.models.User
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for user operations using Supabase Postgrest
 * Handles CRUD operations and real-time subscriptions for users
 */
@Singleton
class SupabaseUserDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseUserDataSource"
        private const val TABLE_NAME = "users"
    }

    /**
     * Insert a new user into Supabase
     * @param user User to insert
     * @return Result with inserted user or error
     */
    suspend fun insert(user: User): Result<User> {
        return try {
            supabase.from(TABLE_NAME)
                .insert(user)
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting user", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing user in Supabase
     * @param user User to update
     * @return Result with updated user or error
     */
    suspend fun update(user: User): Result<User> {
        return try {
            supabase.from(TABLE_NAME)
                .update({
                    set("email", user.email)
                    set("username", user.username)
                    set("display_name", user.displayName)
                    set("age", user.age)
                    set("role", user.role)
                    set("bio", user.bio)
                    set("location", user.location)
                    set("github_url", user.githubUrl)
                    set("twitter_url", user.twitterUrl)
                    set("linkedin_url", user.linkedinUrl)
                    set("website_url", user.websiteUrl)
                    set("portfolio_url", user.portfolioUrl)
                    set("photo_url", user.photoUrl)
                    set("is_online", user.isOnline)
                    set("last_seen", user.lastSeen)
                    set("fcm_token", user.fcmToken)
                    set("settings", user.settings)
                }) {
                    filter {
                        eq("id", user.id)
                    }
                }

            Log.d(TAG, "✅ User ${user.id} updated in Supabase")
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a user from Supabase
     * @param userId ID of user to delete
     * @return Result with Unit or error
     */
    suspend fun delete(userId: String): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME)
                .delete {
                    filter {
                        eq("id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user", e)
            Result.failure(e)
        }
    }

    /**
     * Get a user by ID
     * @param userId User ID
     * @return Result with User or error (null if not found)
     */
    suspend fun getById(userId: String): Result<User?> {
        return try {
            val user = supabase.from(TABLE_NAME)
                .select() {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<User>()

            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user by ID", e)
            Result.failure(e)
        }
    }

    /**
     * Get a user by username
     * @param username Username to search for (case-insensitive)
     * @return Result with User or error (null if not found)
     */
    suspend fun getByUsername(username: String): Result<User?> {
        return try {
            val user = supabase.from(TABLE_NAME)
                .select() {
                    filter {
                        ilike("username", username)
                    }
                }
                .decodeSingleOrNull<User>()

            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user by username: $username", e)
            Result.failure(e)
        }
    }

    /**
     * Get all users
     * @return Result with list of users or error
     */
    suspend fun getAll(): Result<List<User>> {
        return try {
            val users = supabase.from(TABLE_NAME)
                .select()
                .decodeList<User>()

            Result.success(users)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all users", e)
            Result.failure(e)
        }
    }

    /**
     * Search users by display name or email
     * Uses server-side filtering with Supabase ilike for case-insensitive search
     * @param query Search query
     * @param excludeIds User IDs to exclude from results
     * @param limit Maximum number of results
     * @return Result with list of matching users or error
     */
    suspend fun searchUsers(
        query: String,
        excludeIds: List<String> = emptyList(),
        limit: Int = 50
    ): Result<List<User>> {
        return try {
            if (query.isBlank()) {
                // Return empty list for blank query
                return Result.success(emptyList())
            }

            // Server-side filtering using Supabase ilike (case-insensitive LIKE)
            // Search in username, display_name, and email fields
            val searchPattern = "%${query.trim()}%"

            // Note: Supabase Postgrest doesn't support complex NOT IN filters easily
            // So we'll fetch and filter client-side for excludeIds
            // DEFENSIVE: Wrap the Supabase call in try-catch to handle NULL username deserialization errors
            // If NULL usernames exist in the database, the JSON deserialization will fail
            // The migration script POPULATE_NULL_USERNAMES_MIGRATION.sql should be run to fix this permanently
            val users = try {
                supabase.from(TABLE_NAME)
                    .select() {
                        filter {
                            // Match on username OR display_name OR email
                            or {
                                ilike("username", searchPattern)
                                ilike("display_name", searchPattern)
                                ilike("email", searchPattern)
                            }
                        }
                        // Limit results for performance
                        limit(limit.toLong())
                    }
                    .decodeList<User>()
            } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
                // Catch deserialization errors that occur when username is NULL in the database
                Log.e(TAG, "JSON deserialization error during user search.", e)
                Log.w(TAG, "This may indicate NULL username fields still exist in the database.")
                Log.w(TAG, "SOLUTION: Run the migration script: POPULATE_NULL_USERNAMES_MIGRATION.sql")
                Log.w(TAG, "Location: /POPULATE_NULL_USERNAMES_MIGRATION.sql in project root")
                // Return empty list instead of crashing the app
                emptyList()
            }

            // Client-side filtering for excludeIds and additional null safety
            val filtered = users
                .filter { user ->
                    // Ensure username is not empty/blank
                    val hasValidUsername = user.username.isNotBlank()
                    val notExcluded = !excludeIds.contains(user.id)
                    hasValidUsername && notExcluded
                }

            // Client-side sorting by display name
            val sorted = filtered.sortedBy { it.displayName }

            Log.d(TAG, "Search completed: query='$query', found ${sorted.size} users")
            Result.success(sorted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users: query='$query'", e)
            Result.failure(e)
        }
    }

    /**
     * Search users via the users_public view (bypasses RLS for global discovery)
     * Only returns: id, username, display_name, photo_url, is_online, last_seen
     */
    suspend fun searchUsersPublic(
        query: String,
        excludeIds: List<String> = emptyList(),
        limit: Int = 50
    ): Result<List<User>> {
        return try {
            if (query.isBlank()) return Result.success(emptyList())

            val searchPattern = "%${query.trim()}%"
            val users = try {
                supabase.from("users_public")
                    .select {
                        filter {
                            or {
                                ilike("username", searchPattern)
                                ilike("display_name", searchPattern)
                            }
                        }
                        limit(limit.toLong())
                    }
                    .decodeList<User>()
            } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
                Log.e(TAG, "Error decoding users_public search results", e)
                emptyList()
            }

            val filtered = users
                .filter { it.username.isNotBlank() && !excludeIds.contains(it.id) }
                .sortedBy { it.displayName }

            Result.success(filtered)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users_public", e)
            Result.failure(e)
        }
    }

    /**
     * Update user's online status
     * @param userId User ID
     * @param isOnline Online status
     * @return Result with Unit or error
     */
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME)
                .update(buildJsonObject {
                    put("is_online", isOnline)
                    put("last_seen", System.currentTimeMillis())
                }) {
                    filter {
                        eq("id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating online status", e)
            Result.failure(e)
        }
    }

    /**
     * Update user's FCM token
     * @param userId User ID
     * @param fcmToken FCM token
     * @return Result with Unit or error
     */
    suspend fun updateFcmToken(userId: String, fcmToken: String?): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME)
                .update(buildJsonObject {
                    put("fcm_token", fcmToken)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Observe real-time changes to users table
     * @return Flow of user changes (INSERT, UPDATE, DELETE)
     */
    fun observeChanges(): Flow<PostgresAction> {
        return supabase.channel("users_changes")
            .postgresChangeFlow<PostgresAction>(schema = "public") {
                table = TABLE_NAME
            }
    }

    /**
     * Observe changes to a specific user via realtime
     * @param userId User ID to observe
     * @return Flow of user updates (emits updated User on INSERT/UPDATE, null on DELETE)
     */
    fun observeUserById(userId: String): Flow<User?> {
        val channel = supabase.channel("user_$userId")
        return channel
            .postgresChangeFlow<PostgresAction>(schema = "public") {
                table = TABLE_NAME
                filter("id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, userId)
            }
            .map { action ->
                when (action) {
                    is PostgresAction.Update -> {
                        try {
                            // Fetch fresh user data after update
                            val result = getById(userId)
                            result.getOrNull()
                        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching user after realtime update", e)
                            null
                        }
                    }
                    is PostgresAction.Insert -> {
                        try {
                            val result = getById(userId)
                            result.getOrNull()
                        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching user after realtime insert", e)
                            null
                        }
                    }
                    is PostgresAction.Delete -> null
                    else -> null
                }
            }
    }

    /**
     * Batch insert multiple users
     * @param users List of users to insert
     * @return Result with inserted users or error
     */
    suspend fun insertAll(users: List<User>): Result<List<User>> {
        return try {
            supabase.from(TABLE_NAME)
                .insert(users)
            Result.success(users)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error batch inserting users", e)
            Result.failure(e)
        }
    }
}
