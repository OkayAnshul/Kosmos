package com.example.kosmos.features.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kosmos.MainActivity
import com.example.kosmos.R
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Notification Listener
 *
 * Subscribes to Supabase Realtime for new notifications and displays them as Android notifications.
 *
 * Features:
 * - Real-time notification delivery via Supabase Realtime
 * - Automatic Android notification display
 * - Notification channel management
 * - Deep linking to specific screens
 * - Unread count tracking
 *
 * Usage:
 * ```kotlin
 * // In Application.onCreate() or MainActivity
 * notificationListener.startListening(userId)
 *
 * // Stop listening when user logs out
 * notificationListener.stopListening()
 * ```
 */
@Singleton
class NotificationListener @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient
) {
    private val TAG = "NotificationListener"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Notification channel
    private val CHANNEL_ID = "kosmos_notifications"
    private val CHANNEL_NAME = "Kosmos Notifications"

    // Unread count
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // Latest notification for in-app snackbar
    private val _latestNotification = MutableStateFlow<SupabaseNotification?>(null)
    val latestNotification: StateFlow<SupabaseNotification?> = _latestNotification.asStateFlow()

    // Current subscription
    private var currentUserId: String? = null

    init {
        createNotificationChannel()
    }

    /**
     * Start listening for notifications for a specific user
     *
     * @param userId User ID to listen for
     */
    fun startListening(userId: String) {
        if (currentUserId == userId) {
            Log.d(TAG, "Already listening for user: $userId")
            return
        }

        Log.d(TAG, "Starting notification listener for user: $userId")
        currentUserId = userId

        // Subscribe to notifications table changes
        scope.launch {
            try {
                val channel = supabase.channel("notifications:$userId")

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                }

                channel.subscribe()
                Log.d(TAG, "✅ Successfully subscribed to notifications for user: $userId")

                // Initial unread count
                updateUnreadCount(userId)

                // Listen for changes and filter client-side
                changeFlow
                    .catch { e ->
                        Log.e(TAG, "Error in notification flow", e)
                    }
                    .collect { action ->
                        // Filter notifications by user ID (client-side filtering)
                        val notificationUserId = when (action) {
                            is PostgresAction.Insert -> action.record["user_id"] as? String
                            is PostgresAction.Update -> action.record["user_id"] as? String
                            is PostgresAction.Delete -> action.oldRecord["user_id"] as? String
                            else -> null
                        }

                        // Only process if notification belongs to this user
                        if (notificationUserId == userId) {
                            when (action) {
                                is PostgresAction.Insert -> {
                                    handleNewNotification(action.record as SupabaseNotification)
                                }
                                is PostgresAction.Update -> {
                                    // Update unread count when notification is marked as read
                                    updateUnreadCount(userId)
                                }
                                is PostgresAction.Delete -> {
                                    // Update unread count when notification is deleted
                                    updateUnreadCount(userId)
                                }
                                else -> {
                                    Log.d(TAG, "Unhandled action type: $action")
                                }
                            }
                        }
                    }

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "❌ Failed to subscribe to notifications", e)
            }
        }
    }

    /**
     * Stop listening for notifications
     */
    fun stopListening() {
        Log.d(TAG, "Stopping notification listener")
        currentUserId = null
        _unreadCount.value = 0
        // Note: Supabase channels are automatically cleaned up when scope is cancelled
    }

    /**
     * Consume the latest notification (reset to null so snackbar only shows once)
     */
    fun consumeLatestNotification() {
        _latestNotification.value = null
    }

    /**
     * Immediately reset the unread badge to 0 (call when user opens notification screen).
     * The real count will be confirmed by the next Supabase Realtime update event.
     */
    fun resetUnreadCount() {
        _unreadCount.value = 0
    }

    /**
     * Handle new notification from Realtime
     */
    private fun handleNewNotification(notification: SupabaseNotification) {
        Log.d(TAG, "Received new notification: ${notification.title}")

        // Update unread count
        _unreadCount.value += 1

        // Signal in-app snackbar
        _latestNotification.value = notification

        // Show Android notification
        showAndroidNotification(notification)
    }

    /**
     * Show Android notification
     */
    private fun showAndroidNotification(notification: SupabaseNotification) {
        try {
            // Create intent for notification click
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("notification_id", notification.id)
                putExtra("task_id", notification.data["task_id"])
                putExtra("project_id", notification.data["project_id"])
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notification.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build notification
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // You'll need to add this icon
                .setContentTitle(notification.title)
                .setContentText(notification.body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Skipping system notification: POST_NOTIFICATIONS permission not granted")
                return
            }

            // Show notification
            NotificationManagerCompat.from(context).notify(
                notification.id.hashCode(),
                builder.build()
            )

            Log.d(TAG, "✅ Displayed Android notification: ${notification.title}")

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to show Android notification", e)
        }
    }

    /**
     * Update unread count from database
     */
    private suspend fun updateUnreadCount(userId: String) {
        try {
            val result = supabase.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                }

            val count = result.decodeList<SupabaseNotification>().size
            _unreadCount.value = count

            Log.d(TAG, "Unread count: $count")

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to fetch unread count", e)
        }
    }

    /**
     * Create notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for tasks, comments, and reminders"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "Created notification channel: $CHANNEL_ID")
        }
    }

    /**
     * Fetch all unread notifications for a user
     *
     * @param userId User ID
     * @return List of unread notifications
     */
    suspend fun getUnreadNotifications(userId: String): List<SupabaseNotification> {
        return try {
            val result = supabase.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(50)
                }

            result.decodeList<SupabaseNotification>()

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to fetch unread notifications", e)
            emptyList()
        }
    }

    /**
     * Fetch all notifications for a user (read and unread)
     *
     * @param userId User ID
     * @param limit Max number of notifications to fetch
     * @return List of notifications
     */
    suspend fun getAllNotifications(userId: String, limit: Int = 50): List<SupabaseNotification> {
        return try {
            val result = supabase.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }

            result.decodeList<SupabaseNotification>()

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to fetch notifications", e)
            emptyList()
        }
    }
}
