package com.example.kosmos.shared.ui.mappers

import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskPriority as DomainTaskPriority
import com.example.kosmos.core.models.TaskStatus as DomainTaskStatus
import com.example.kosmos.features.tasks.components.TaskItem
import com.example.kosmos.features.tasks.components.TaskPriority
import com.example.kosmos.features.tasks.components.TaskStatus
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Mapper functions to convert between domain models and UI models for Task features
 */
object TaskDataMapper {

    /**
     * Convert Task domain model to TaskItem UI model
     */
    fun Task.toTaskItem(projectName: String? = null): TaskItem {
        return TaskItem(
            id = this.id,
            title = this.title,
            description = this.description,
            status = this.status.toUIStatus(),
            priority = this.priority.toUIPriority(),
            dueDate = this.dueDate?.let { formatDueDate(it) },
            isOverdue = this.dueDate?.let { isOverdue(it) } ?: false,
            projectId = this.projectId,
            projectName = projectName,
            assigneeCount = if (this.assignedToId != null) 1 else 0,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    /**
     * Convert domain TaskStatus to UI TaskStatus
     */
    fun DomainTaskStatus.toUIStatus(): TaskStatus {
        return when (this) {
            DomainTaskStatus.TODO -> TaskStatus.TODO
            DomainTaskStatus.IN_PROGRESS -> TaskStatus.IN_PROGRESS
            DomainTaskStatus.DONE -> TaskStatus.DONE
            DomainTaskStatus.CANCELLED -> TaskStatus.CANCELLED
        }
    }

    /**
     * Convert UI TaskStatus to domain TaskStatus
     */
    fun TaskStatus.toDomainStatus(): DomainTaskStatus {
        return when (this) {
            TaskStatus.TODO -> DomainTaskStatus.TODO
            TaskStatus.IN_PROGRESS -> DomainTaskStatus.IN_PROGRESS
            TaskStatus.DONE -> DomainTaskStatus.DONE
            TaskStatus.CANCELLED -> DomainTaskStatus.CANCELLED
        }
    }

    /**
     * Convert domain TaskPriority to UI TaskPriority
     */
    fun DomainTaskPriority.toUIPriority(): TaskPriority {
        return when (this) {
            DomainTaskPriority.LOW -> TaskPriority.LOW
            DomainTaskPriority.MEDIUM -> TaskPriority.MEDIUM
            DomainTaskPriority.HIGH -> TaskPriority.HIGH
            DomainTaskPriority.URGENT -> TaskPriority.URGENT
        }
    }

    /**
     * Convert UI TaskPriority to domain TaskPriority
     */
    fun TaskPriority.toDomainPriority(): DomainTaskPriority {
        return when (this) {
            TaskPriority.LOW -> DomainTaskPriority.LOW
            TaskPriority.MEDIUM -> DomainTaskPriority.MEDIUM
            TaskPriority.HIGH -> DomainTaskPriority.HIGH
            TaskPriority.URGENT -> DomainTaskPriority.URGENT
        }
    }

    /**
     * Format due date for display
     */
    fun formatDueDate(timestamp: Long): String {
        val today = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            // Today
            isSameDay(today, dueDate) -> "Today"
            // Tomorrow
            isTomorrow(today, dueDate) -> "Tomorrow"
            // This week
            isThisWeek(today, dueDate) -> {
                SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
            }
            // This year
            today.get(Calendar.YEAR) == dueDate.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
            // Other years
            else -> {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    /**
     * Check if task is overdue
     */
    fun isOverdue(dueDate: Long): Boolean {
        val now = System.currentTimeMillis()
        return dueDate < now
    }

    /**
     * Get days until due
     */
    fun getDaysUntilDue(dueDate: Long): Long {
        val now = System.currentTimeMillis()
        val diff = dueDate - now
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    /**
     * Check if two calendars are on the same day
     */
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Check if date is tomorrow
     */
    private fun isTomorrow(today: Calendar, date: Calendar): Boolean {
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return isSameDay(tomorrow, date)
    }

    /**
     * Check if date is within this week
     */
    private fun isThisWeek(today: Calendar, date: Calendar): Boolean {
        val diff = date.timeInMillis - today.timeInMillis
        return diff > 0 && diff < TimeUnit.DAYS.toMillis(7)
    }

    /**
     * Sort tasks by various criteria
     */
    fun sortTasks(tasks: List<TaskItem>, sortBy: TaskSortOption): List<TaskItem> {
        return when (sortBy) {
            TaskSortOption.DUE_DATE -> tasks.sortedWith(
                compareBy(
                    nullsLast(),
                    { it.dueDate }
                )
            )
            TaskSortOption.PRIORITY -> tasks.sortedByDescending { it.priority.ordinal }
            TaskSortOption.CREATED -> tasks.sortedByDescending { it.createdAt }
            TaskSortOption.UPDATED -> tasks.sortedByDescending { it.updatedAt }
        }
    }

    /**
     * Filter tasks by status and priority
     */
    fun filterTasks(
        tasks: List<TaskItem>,
        status: TaskStatus? = null,
        priority: TaskPriority? = null
    ): List<TaskItem> {
        var filtered = tasks

        if (status != null) {
            filtered = filtered.filter { it.status == status }
        }

        if (priority != null) {
            filtered = filtered.filter { it.priority == priority }
        }

        return filtered
    }
}

/**
 * Task sort options (matching MyTasksScreen enum)
 */
enum class TaskSortOption {
    DUE_DATE, PRIORITY, CREATED, UPDATED
}
