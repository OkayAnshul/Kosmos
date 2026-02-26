package com.example.kosmos.features.notifications

import android.util.Log
import com.example.kosmos.core.database.dao.UserDao
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.core.models.ActivityActionType
import com.example.kosmos.core.models.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Notification Rules Engine
 *
 * Evaluates when to send notifications based on task activities.
 * Determines recipients, checks preferences, and triggers notifications.
 *
 * Features:
 * - Recipient determination (assignee, creator, @mentions)
 * - Preference checking (muted tasks, quiet hours)
 * - Rate limiting (prevent spam)
 * - Smart routing based on action type
 *
 * Usage:
 * ```kotlin
 * notificationRulesEngine.evaluateAndNotify(activity, task)
 * ```
 */
@Singleton
class NotificationRulesEngine @Inject constructor(
    private val userDao: UserDao,
    private val supabaseNotificationService: SupabaseNotificationService
) {
    private val TAG = "NotificationRulesEngine"

    // Rate limiting: Track last notification time per user per task
    private val lastNotificationTime = mutableMapOf<Pair<String, String>, Long>()
    private val RATE_LIMIT_MS = 5 * 60 * 1000 // 5 minutes

    /**
     * Evaluate activity and send notifications to appropriate users
     *
     * @param activity The task activity that occurred
     * @param task The task that was modified
     */
    suspend fun evaluateAndNotify(activity: TaskActivity, task: Task) {
        try {
            // Determine who should be notified
            val recipients = determineRecipients(activity, task)

            Log.d(TAG, "Evaluating notifications for ${recipients.size} recipients")

            // Send notification to each recipient
            recipients.forEach { recipient ->
                if (shouldNotify(recipient, activity, task)) {
                    sendNotification(recipient, activity, task)
                }
            }

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error evaluating notifications", e)
        }
    }

    /**
     * Determine who should receive notifications
     *
     * @param activity The activity that occurred
     * @param task The task
     * @return List of users who should be notified
     */
    private suspend fun determineRecipients(
        activity: TaskActivity,
        task: Task
    ): List<User> {
        val recipients = mutableSetOf<User>()

        // Don't notify the actor (person who made the change)
        val actorId = activity.actorId

        // Action-specific recipients
        when (activity.actionType) {
            ActivityActionType.ASSIGNED -> {
                // Notify new assignee (unless they're the one who assigned it)
                task.assignedToId?.let { assigneeId ->
                    if (assigneeId != actorId) {
                        userDao.getUserById(assigneeId)?.let { recipients.add(it) }
                    }
                }
            }

            ActivityActionType.STATUS_CHANGED,
            ActivityActionType.PRIORITY_CHANGED,
            ActivityActionType.DESCRIPTION_CHANGED,
            ActivityActionType.DUE_DATE_CHANGED,
            ActivityActionType.TAGS_UPDATED -> {
                // Notify assignee (unless they made the change)
                task.assignedToId?.let { assigneeId ->
                    if (assigneeId != actorId) {
                        userDao.getUserById(assigneeId)?.let { recipients.add(it) }
                    }
                }

                // Notify creator (unless they're the actor or assignee)
                if (task.createdById != actorId && task.createdById != task.assignedToId) {
                    userDao.getUserById(task.createdById)?.let { recipients.add(it) }
                }
            }

            ActivityActionType.COMMENT_ADDED -> {
                // Notify assignee and creator
                task.assignedToId?.let { assigneeId ->
                    if (assigneeId != actorId) {
                        userDao.getUserById(assigneeId)?.let { recipients.add(it) }
                    }
                }
                if (task.createdById != actorId) {
                    userDao.getUserById(task.createdById)?.let { recipients.add(it) }
                }
            }

            else -> {
                // Default: Notify assignee only
                task.assignedToId?.let { assigneeId ->
                    if (assigneeId != actorId) {
                        userDao.getUserById(assigneeId)?.let { recipients.add(it) }
                    }
                }
            }
        }

        // Phase 4 TODO FIX: Extract @mentions from commit message
        activity.commitMessage?.let { message ->
            val mentions = extractMentions(message)
            mentions.forEach { username ->
                userDao.getUserByUsername(username)?.let { user ->
                    if (user.id != actorId) {
                        recipients.add(user)
                        Log.d(TAG, "Added @mentioned user: ${user.displayName}")
                    }
                }
            }
        }

        return recipients.toList()
    }

    /**
     * Check if user should be notified
     *
     * @param user The recipient user
     * @param activity The activity
     * @param task The task
     * @return True if user should be notified
     */
    private fun shouldNotify(
        user: User,
        activity: TaskActivity,
        task: Task
    ): Boolean {
        // Check rate limiting
        val key = user.id to task.id
        val lastTime = lastNotificationTime[key] ?: 0L
        val now = System.currentTimeMillis()

        if (now - lastTime < RATE_LIMIT_MS) {
            Log.d(TAG, "Rate limit: Skipping notification for user ${user.id}")
            return false
        }

        // Check user notification preferences (enabled, tasks, mentions-only, DND)
        val notifSettings = user.settings?.notifications
        if (notifSettings != null) {
            // Check if notifications are globally disabled
            if (!notifSettings.enabled) {
                Log.d(TAG, "User ${user.id} has notifications disabled globally")
                return false
            }

            // Check if task notifications are disabled
            if (!notifSettings.tasks) {
                Log.d(TAG, "User ${user.id} has task notifications disabled")
                return false
            }

            // Check if mentions-only mode is enabled (only notify if user is mentioned)
            if (notifSettings.mentionsOnlyMode) {
                val isMentioned = activity.commitMessage?.contains("@${user.username ?: ""}") == true
                if (!isMentioned) {
                    Log.d(TAG, "User ${user.id} in mentions-only mode, not mentioned")
                    return false
                }
            }

            // Check Do Not Disturb
            if (notifSettings.dnd.enabled) {
                val calendar = java.util.Calendar.getInstance()
                val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(java.util.Calendar.MINUTE)
                val currentTimeMinutes = currentHour * 60 + currentMinute

                val dndStartMinutes = notifSettings.dnd.startHour * 60 + notifSettings.dnd.startMinute
                val dndEndMinutes = notifSettings.dnd.endHour * 60 + notifSettings.dnd.endMinute

                val inDndPeriod = if (dndStartMinutes <= dndEndMinutes) {
                    // Normal range (e.g., 22:00 to 08:00 same day - this is unlikely)
                    currentTimeMinutes in dndStartMinutes until dndEndMinutes
                } else {
                    // Overnight range (e.g., 22:00 to 08:00 next day)
                    currentTimeMinutes >= dndStartMinutes || currentTimeMinutes < dndEndMinutes
                }

                if (inDndPeriod) {
                    Log.d(TAG, "User ${user.id} in Do Not Disturb period")
                    return false
                }
            }
        }

        return true
    }

    /**
     * Send notification to user
     *
     * @param user The recipient
     * @param activity The activity
     * @param task The task
     */
    private suspend fun sendNotification(
        user: User,
        activity: TaskActivity,
        task: Task
    ) {
        try {
            val title = generateNotificationTitle(activity, task)
            val body = activity.commitMessage ?: activity.autoDescription
            val type = mapActionTypeToNotificationType(activity.actionType)

            supabaseNotificationService.sendNotification(
                userId = user.id,
                title = title,
                body = body,
                type = type,
                data = mapOf(
                    "task_id" to task.id,
                    "project_id" to task.projectId,
                    "action_type" to activity.actionType.name
                )
            )

            // Update rate limiting
            lastNotificationTime[user.id to task.id] = System.currentTimeMillis()

            Log.d(TAG, "✅ Sent notification to user ${user.id}")

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ Failed to send notification to user ${user.id}", e)
        }
    }

    /**
     * Map activity action type to notification type
     */
    private fun mapActionTypeToNotificationType(actionType: ActivityActionType): String {
        return when (actionType) {
            ActivityActionType.ASSIGNED -> "task_assigned"
            ActivityActionType.STATUS_CHANGED -> "task_status_changed"
            ActivityActionType.PRIORITY_CHANGED -> "task_priority_changed"
            ActivityActionType.COMMENT_ADDED -> "task_comment"
            ActivityActionType.DUE_DATE_CHANGED -> "task_due_date_changed"
            ActivityActionType.CREATED -> "task_created"
            ActivityActionType.DELETED -> "task_deleted"
            ActivityActionType.UPDATED -> "task_updated"
            else -> "task_activity"
        }
    }

    /**
     * Generate notification title based on activity
     *
     * @param activity The activity
     * @param task The task
     * @return Notification title
     */
    private fun generateNotificationTitle(activity: TaskActivity, task: Task): String {
        return when (activity.actionType) {
            ActivityActionType.ASSIGNED -> "You were assigned: ${task.title}"
            ActivityActionType.STATUS_CHANGED -> "Task updated: ${task.title}"
            ActivityActionType.PRIORITY_CHANGED -> "Priority changed: ${task.title}"
            ActivityActionType.COMMENT_ADDED -> "New comment on: ${task.title}"
            ActivityActionType.DUE_DATE_CHANGED -> "Due date changed: ${task.title}"
            else -> "Task activity: ${task.title}"
        }
    }

    /**
     * Extract @mentions from text
     *
     * @param text Text to parse
     * @return List of mentioned usernames
     */
    private fun extractMentions(text: String): List<String> {
        val mentionRegex = "@(\\w+)".toRegex()
        return mentionRegex.findAll(text)
            .map { it.groupValues[1] }
            .toList()
    }
}
