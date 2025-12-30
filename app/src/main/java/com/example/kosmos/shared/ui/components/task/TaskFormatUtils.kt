package com.example.kosmos.shared.ui.components.task

import androidx.compose.ui.graphics.Color
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import java.text.SimpleDateFormat
import java.util.*

/**
 * Task Formatting Utilities
 *
 * Shared utilities for formatting task-related data across screens.
 * Extracted from TaskDetailScreen for reuse in TaskManagementScreen and TaskEditScreen.
 */
object TaskFormatUtils {

    /**
     * Format timestamp to human-readable date
     * Example: "Oct 12, 2024"
     */
    fun formatDate(timestamp: Long?): String {
        if (timestamp == null) return "No due date"
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    /**
     * Format timestamp to human-readable date and time
     * Example: "Oct 12, 2024 at 3:45 PM"
     */
    fun formatDateTime(timestamp: Long?): String {
        if (timestamp == null) return "Unknown"
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    /**
     * Format due date with overdue indicator
     * Returns pair of (formatted text, is overdue)
     */
    fun formatDueDate(dueDate: Long?): Pair<String, Boolean> {
        if (dueDate == null) return "No due date" to false

        val now = System.currentTimeMillis()
        val isOverdue = dueDate < now

        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateText = formatter.format(Date(dueDate))

        return if (isOverdue) {
            "Overdue: $dateText" to true
        } else {
            "Due $dateText" to false
        }
    }

    /**
     * Get color for task status
     */
    fun getStatusColor(status: TaskStatus): Color {
        return when (status) {
            TaskStatus.TODO -> ColorTokens.ReactTheme.mutedForeground
            TaskStatus.IN_PROGRESS -> ColorTokens.ReactTheme.primary
            TaskStatus.DONE -> ColorTokens.Status.online
            TaskStatus.CANCELLED -> ColorTokens.ReactTheme.destructive
        }
    }

    /**
     * Get label for task status
     */
    fun getStatusLabel(status: TaskStatus): String {
        return when (status) {
            TaskStatus.TODO -> "To Do"
            TaskStatus.IN_PROGRESS -> "In Progress"
            TaskStatus.DONE -> "Done"
            TaskStatus.CANCELLED -> "Cancelled"
        }
    }

    /**
     * Get color for task priority
     */
    fun getPriorityColor(priority: TaskPriority): Color {
        return when (priority) {
            TaskPriority.LOW -> ColorTokens.Status.online
            TaskPriority.MEDIUM -> ColorTokens.Priority.medium
            TaskPriority.HIGH -> ColorTokens.ReactTheme.destructive
            TaskPriority.URGENT -> ColorTokens.ReactTheme.destructive
        }
    }

    /**
     * Get label for task priority
     */
    fun getPriorityLabel(priority: TaskPriority): String {
        return when (priority) {
            TaskPriority.LOW -> "Low Priority"
            TaskPriority.MEDIUM -> "Medium Priority"
            TaskPriority.HIGH -> "High Priority"
            TaskPriority.URGENT -> "Urgent"
        }
    }

    /**
     * Get short label for task priority (used in compact displays)
     */
    fun getPriorityShortLabel(priority: TaskPriority): String {
        return when (priority) {
            TaskPriority.LOW -> "Low"
            TaskPriority.MEDIUM -> "Medium"
            TaskPriority.HIGH -> "High"
            TaskPriority.URGENT -> "Urgent"
        }
    }

    /**
     * Format estimated hours for display
     */
    fun formatEstimatedHours(hours: Float?): String {
        return if (hours != null && hours > 0f) {
            "${String.format("%.1f", hours)}h"
        } else {
            "Not set"
        }
    }

    /**
     * Format actual hours for display
     */
    fun formatActualHours(hours: Float?): String {
        return if (hours != null && hours > 0f) {
            "${String.format("%.1f", hours)}h"
        } else {
            "Not tracked"
        }
    }

    /**
     * Calculate and format time progress
     * Returns triple of (progress 0-1, percentage text, is over budget)
     */
    fun calculateTimeProgress(estimatedHours: Float?, actualHours: Float?): Triple<Float, String, Boolean>? {
        if (estimatedHours == null || estimatedHours <= 0f || actualHours == null || actualHours <= 0f) {
            return null
        }

        val progress = (actualHours / estimatedHours).coerceIn(0f, 1.2f)
        val percentage = (progress * 100).toInt()
        val isOverBudget = progress > 1f

        val progressText = if (isOverBudget) {
            "$percentage% over budget"
        } else {
            "$percentage% complete"
        }

        return Triple(progress, progressText, isOverBudget)
    }

    /**
     * Get progress bar color based on progress value
     */
    fun getProgressColor(progress: Float): Color {
        return when {
            progress > 1f -> ColorTokens.ReactTheme.destructive
            progress >= 0.8f -> ColorTokens.Priority.medium
            else -> ColorTokens.Status.online
        }
    }

    /**
     * Format list of tags for display
     */
    fun formatTags(tags: List<String>): String {
        return if (tags.isEmpty()) {
            "No tags"
        } else {
            tags.joinToString(", ")
        }
    }

    /**
     * Format subtask count
     * Example: "2/5 completed"
     */
    fun formatSubtaskCount(total: Int, completed: Int): String {
        return "$completed/$total completed"
    }

    /**
     * Check if task is overdue
     */
    fun isTaskOverdue(dueDate: Long?): Boolean {
        if (dueDate == null) return false
        return dueDate < System.currentTimeMillis()
    }

    /**
     * Format updated timestamp as "Last updated X ago"
     */
    fun formatLastUpdated(updatedAt: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - updatedAt

        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "Last updated ${days}d ago"
            hours > 0 -> "Last updated ${hours}h ago"
            minutes > 0 -> "Last updated ${minutes}m ago"
            else -> "Last updated just now"
        }
    }

    /**
     * Validate estimated hours input
     * Returns error message or null if valid
     */
    fun validateEstimatedHours(hours: Float?): String? {
        return when {
            hours == null -> null
            hours < 0f -> "Estimated hours must be positive"
            hours > 9999f -> "Estimated hours must be less than 10,000"
            else -> null
        }
    }

    /**
     * Validate actual hours input
     * Returns error message or null if valid
     */
    fun validateActualHours(hours: Float?): String? {
        return when {
            hours == null -> null
            hours < 0f -> "Actual hours must be positive"
            hours > 9999f -> "Actual hours must be less than 10,000"
            else -> null
        }
    }
}
