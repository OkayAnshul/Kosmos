package com.example.kosmos.data.datasource

import android.util.Log
import com.example.kosmos.core.models.User
import io.github.jan.supabase.SupabaseClient
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
                .update(user) {
                    filter {
                        eq("id", user.id)
                    }
                }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user", e)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user by ID", e)
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
            // Search in both display_name and email fields
            val searchPattern = "%${query.trim()}%"

            // Note: Supabase Postgrest doesn't support complex NOT IN filters easily
            // So we'll fetch and filter client-side for excludeIds
            val users = supabase.from(TABLE_NAME)
                .select() {
                    filter {
                        // Match on display_name OR email
                        or {
                            ilike("display_name", searchPattern)
                            ilike("email", searchPattern)
                        }
                    }
                    // Limit results for performance
                    limit(limit.toLong())
                }
                .decodeList<User>()

            // Client-side filtering for excludeIds
            val filtered = if (excludeIds.isNotEmpty()) {
                users.filter { !excludeIds.contains(it.id) }
            } else {
                users
            }

            // Client-side sorting by display name
            val sorted = filtered.sortedBy { it.displayName }

            Result.success(sorted)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users: query='$query'", e)
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
            val updates = mapOf(
                "is_online" to isOnline,
                "last_seen" to System.currentTimeMillis()
            )

            supabase.from(TABLE_NAME)
                .update(updates) {
                    filter {
                        eq("id", userId)
                    }
                }

            Result.success(Unit)
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
            val updates = mapOf("fcm_token" to fcmToken)

            supabase.from(TABLE_NAME)
                .update(updates) {
                    filter {
                        eq("id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Observe real-time changes to users table
     * @return Flow of user changes (INSERT, UPDATE, DELETE)
     */
    fun observeChanges(): Flow<List<User>> {
        return supabase.channel("users_changes")
            .postgresChangeFlow<PostgresAction>(schema = "public") {
                table = TABLE_NAME
            }
            .map { action ->
                // This is a simplified implementation
                // In a real app, you'd want to handle INSERT, UPDATE, DELETE separately
                // and maintain a local list that you update based on the action
                emptyList<User>()
            }
    }

    /**
     * Observe changes to a specific user
     * @param userId User ID to observe
     * @return Flow of user updates
     */
    fun observeUserById(userId: String): Flow<User?> {
        // Realtime subscriptions require proper setup in Supabase
        // For now, returning an empty flow - will implement when needed
        return kotlinx.coroutines.flow.flowOf(null)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error batch inserting users", e)
            Result.failure(e)
        }
    }
}
