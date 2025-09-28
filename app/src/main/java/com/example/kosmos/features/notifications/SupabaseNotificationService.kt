package com.example.kosmos.features.notifications

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Supabase Notification Service
 *
 * Sends in-app notifications via Supabase Realtime.
 * Notifications are inserted into the database and delivered via Realtime subscriptions.
 *
 * Architecture:
 * Android App → Supabase notifications table → Realtime subscription → In-app notification
 *
 * Usage:
 * ```kotlin
 * supabaseNotificationService.sendNotification(
 *     userId = user.id,
 *     title = "Task assigned",
 *     body = "You were assigned to: Task XYZ",
 *     type = "task_assigned",
 *     data = mapOf("task_id" to taskId)
 * )
 * ```
 */
@Singleton
class SupabaseNotificationService @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val TAG = "SupabaseNotificationService"

    /**
     * Send notification by inserting into notifications table
     * Supabase Realtime will deliver to user's device automatically
     *
     * @param userId The recipient user ID
     * @param title Notification title
     * @param body Notification body
     * @param type Notification type (task_assigned, status_changed, etc.)
     * @param data Additional data payload
     * @return Result with success or error
     */
    suspend fun sendNotification(
        userId: String,
        title: String,
        body: String,
        type: String = "info",
        data: Map<String, String> = emptyMap()
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Sending notification to user: $userId")

            // Call SECURITY DEFINER RPC — avoids RLS violation when sender != recipient
            supabase.postgrest.rpc(
                function = "insert_notification",
                parameters = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_title", title)
                    put("p_body", body)
                    put("p_type", type)
                    putJsonObject("p_data") {
                        data.forEach { (k, v) -> put(k, v) }
                    }
                }
            )

            Log.d(TAG, "✅ Successfully sent notification to user: $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to send notification to user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Send notification to multiple users
     *
     * @param userIds List of user IDs
     * @param title Notification title
     * @param body Notification body
     * @param type Notification type
     * @param data Additional data payload
     */
    suspend fun sendNotificationToMultiple(
        userIds: List<String>,
        title: String,
        body: String,
        type: String = "info",
        data: Map<String, String> = emptyMap()
    ) {
        userIds.forEach { userId ->
            try {
                sendNotification(userId, title, body, type, data)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Failed to send notification to user: $userId", e)
                // Continue sending to other users
            }
        }
    }

    /**
     * Mark notification as read
     *
     * @param notificationId Notification ID
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            supabase.from("notifications").update(
                mapOf("is_read" to true)
            ) {
                filter {
                    eq("id", notificationId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to mark notification as read: $notificationId", e)
            Result.failure(e)
        }
    }

    /**
     * Mark all notifications as read for a user
     *
     * @param userId User ID
     */
    suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            supabase.from("notifications").update(
                mapOf("is_read" to true)
            ) {
                filter {
                    eq("user_id", userId)
                    eq("is_read", false)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to mark all notifications as read for user: $userId", e)
            Result.failure(e)
        }
    }
}

/**
 * Notification data model for Supabase
 */
@Serializable
data class SupabaseNotification(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val title: String,
    val body: String,
    val type: String,
    val data: Map<String, String> = emptyMap(),
    @SerialName("is_read")
    val isRead: Boolean = false,
    @SerialName("created_at")
    val createdAt: Long
)
