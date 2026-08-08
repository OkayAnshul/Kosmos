package com.example.kosmos.data.repository

import android.util.Log
import com.example.kosmos.features.demo.DemoMode
import com.example.kosmos.features.demo.DemoNotifications
import com.example.kosmos.features.notifications.SupabaseNotification
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Repository for managing notifications
 *
 * Provides access to user notifications stored in Supabase.
 * Handles querying, marking as read, and deleting notifications.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val demoMode: DemoMode
) {
    private val TAG = "NotificationRepository"

    /**
     * Get all notifications for a user
     *
     * @param userId User ID
     * @param limit Number of notifications to fetch
     * @param offset Pagination offset
     * @return List of notifications
     */
    suspend fun getNotifications(
        userId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<SupabaseNotification>> {
        if (demoMode.isEnabled) return Result.success(DemoNotifications.all)
        return try {
            val notifications = supabase.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                    range(offset.toLong() until (offset + limit).toLong())
                }
                .decodeList<SupabaseNotification>()

            Log.d(TAG, "✅ Fetched ${notifications.size} notifications for user: $userId")
            Result.success(notifications)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to fetch notifications for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Get unread notifications for a user
     *
     * @param userId User ID
     * @param limit Number of notifications to fetch
     * @return List of unread notifications
     */
    suspend fun getUnreadNotifications(
        userId: String,
        limit: Int = 50
    ): Result<List<SupabaseNotification>> {
        if (demoMode.isEnabled) {
            return Result.success(DemoNotifications.all.filter { !it.isRead })
        }
        return try {
            val notifications = supabase.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<SupabaseNotification>()

            Log.d(TAG, "✅ Fetched ${notifications.size} unread notifications for user: $userId")
            Result.success(notifications)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to fetch unread notifications for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Get unread notification count
     *
     * @param userId User ID
     * @return Count of unread notifications
     */
    suspend fun getUnreadCount(userId: String): Result<Int> {
        if (demoMode.isEnabled) {
            return Result.success(DemoNotifications.all.count { !it.isRead })
        }
        return try {
            val result = supabase.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                }
                .decodeList<SupabaseNotification>()

            val count = result.size
            Log.d(TAG, "✅ Unread count for user $userId: $count")
            Result.success(count)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to get unread count for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Mark notification as read
     *
     * @param notificationId Notification ID
     * @return Result of the operation
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        if (demoMode.isEnabled) return Result.success(Unit)
        return try {
            supabase.from("notifications").update(
                mapOf("is_read" to true)
            ) {
                filter {
                    eq("id", notificationId)
                }
            }

            Log.d(TAG, "✅ Marked notification as read: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to mark notification as read: $notificationId", e)
            Result.failure(e)
        }
    }

    /**
     * Mark all notifications as read for a user
     *
     * @param userId User ID
     * @return Result of the operation
     */
    suspend fun markAllAsRead(userId: String): Result<Unit> {
        if (demoMode.isEnabled) return Result.success(Unit)
        return try {
            supabase.from("notifications").update(
                mapOf("is_read" to true)
            ) {
                filter {
                    eq("user_id", userId)
                    eq("is_read", false)
                }
            }

            Log.d(TAG, "✅ Marked all notifications as read for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to mark all notifications as read for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a notification
     *
     * @param notificationId Notification ID
     * @return Result of the operation
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        if (demoMode.isEnabled) return Result.success(Unit)
        return try {
            supabase.from("notifications").delete {
                filter {
                    eq("id", notificationId)
                }
            }

            Log.d(TAG, "✅ Deleted notification: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to delete notification: $notificationId", e)
            Result.failure(e)
        }
    }

    /**
     * Delete all read notifications for a user
     *
     * @param userId User ID
     * @return Result of the operation
     */
    suspend fun deleteAllRead(userId: String): Result<Unit> {
        if (demoMode.isEnabled) return Result.success(Unit)
        return try {
            supabase.from("notifications").delete {
                filter {
                    eq("user_id", userId)
                    eq("is_read", true)
                }
            }

            Log.d(TAG, "✅ Deleted all read notifications for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to delete read notifications for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a notification by matching a JSONB data field value.
     * Used when the caller knows the field key/value but not the notification ID.
     *
     * @param userId User ID (scope guard — only deletes own notifications)
     * @param fieldName Key inside the `data` JSONB column (e.g. "connection_id")
     * @param fieldValue Value to match
     * @return Result of the operation
     */
    suspend fun deleteNotificationByDataField(
        userId: String,
        fieldName: String,
        fieldValue: String
    ): Result<Unit> {
        return try {
            supabase.from("notifications").delete {
                filter {
                    eq("user_id", userId)
                    eq("data->>'$fieldName'", fieldValue)
                }
            }
            Log.d(TAG, "✅ Deleted notification where $fieldName=$fieldValue for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to delete notification by $fieldName=$fieldValue", e)
            Result.failure(e)
        }
    }

    /**
     * Delete all notifications for a user
     *
     * @param userId User ID
     * @return Result of the operation
     */
    suspend fun deleteAll(userId: String): Result<Unit> {
        if (demoMode.isEnabled) return Result.success(Unit)
        return try {
            supabase.from("notifications").delete {
                filter {
                    eq("user_id", userId)
                }
            }

            Log.d(TAG, "✅ Deleted all notifications for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to delete all notifications for user: $userId", e)
            Result.failure(e)
        }
    }
}
