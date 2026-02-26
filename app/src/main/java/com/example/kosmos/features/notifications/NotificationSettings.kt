package com.example.kosmos.features.notifications

import kotlinx.serialization.Serializable

/**
 * Notification Settings
 *
 * User preferences for notifications.
 * Stored in users.settings.notificationPreferences JSONB field.
 *
 * Usage:
 * ```kotlin
 * val settings = NotificationSettings(
 *     enabled = true,
 *     notifyOnMentions = true,
 *     quietHoursEnabled = true
 * )
 * ```
 */
@Serializable
data class NotificationSettings(
    // Master toggle
    val enabled: Boolean = true,

    // Notification types
    val notifyOnAssignment: Boolean = true,
    val notifyOnStatusChange: Boolean = true,
    val notifyOnPriorityChange: Boolean = true,
    val notifyOnComments: Boolean = true,
    val notifyOnMentions: Boolean = true,
    val notifyOnDueDate: Boolean = true,

    // Due date reminders
    val dueDateReminders: DueDateReminders = DueDateReminders(),

    // Quiet hours
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22,  // 22:00 (10:00 PM)
    val quietHoursEnd: Int = 8,     // 08:00 (8:00 AM)

    // Sound and vibration
    val sound: Boolean = true,
    val vibration: Boolean = true,

    // Per-project muting
    val mutedProjects: List<String> = emptyList(),

    // Per-task muting
    val mutedTasks: List<String> = emptyList()
) {
    /**
     * Check if notifications enabled for an action type
     */
    fun isEnabledFor(actionType: String): Boolean {
        if (!enabled) return false

        return when (actionType.uppercase()) {
            "ASSIGNED" -> notifyOnAssignment
            "STATUS_CHANGED" -> notifyOnStatusChange
            "PRIORITY_CHANGED" -> notifyOnPriorityChange
            "COMMENT_ADDED" -> notifyOnComments
            "DUE_DATE_CHANGED" -> notifyOnDueDate
            else -> true
        }
    }

    /**
     * Check if project is muted
     */
    fun isProjectMuted(projectId: String): Boolean {
        return mutedProjects.contains(projectId)
    }

    /**
     * Check if task is muted
     */
    fun isTaskMuted(taskId: String): Boolean {
        return mutedTasks.contains(taskId)
    }

    /**
     * Check if currently in quiet hours
     */
    fun isQuietHoursActive(): Boolean {
        if (!quietHoursEnabled) return false

        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        return if (quietHoursStart < quietHoursEnd) {
            currentHour >= quietHoursStart || currentHour < quietHoursEnd
        } else {
            currentHour >= quietHoursStart && currentHour < quietHoursEnd
        }
    }
}

/**
 * Due Date Reminders Configuration
 */
@Serializable
data class DueDateReminders(
    val oneWeekBefore: Boolean = true,
    val threeDaysBefore: Boolean = true,
    val oneDayBefore: Boolean = true,
    val oneHourBefore: Boolean = true
) {
    /**
     * Get all enabled reminder types
     */
    fun getEnabled(): List<String> {
        val enabled = mutableListOf<String>()
        if (oneWeekBefore) enabled.add("one_week_before")
        if (threeDaysBefore) enabled.add("three_days_before")
        if (oneDayBefore) enabled.add("one_day_before")
        if (oneHourBefore) enabled.add("one_hour_before")
        return enabled
    }
}
