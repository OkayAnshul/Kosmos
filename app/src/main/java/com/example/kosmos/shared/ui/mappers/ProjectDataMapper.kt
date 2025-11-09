package com.example.kosmos.shared.ui.mappers

import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectStatus
import com.example.kosmos.features.projects.components.ProjectItem
import com.example.kosmos.features.projects.components.ProjectMember as UIProjectMember
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Mapper functions to convert between domain models and UI models for Project features
 */
object ProjectDataMapper {

    /**
     * Convert Project domain model to ProjectItem UI model
     */
    fun Project.toProjectItem(
        memberCount: Int = 0,
        chatCount: Int = 0,
        taskCount: Int = 0,
        completedTaskCount: Int = 0,
        unreadChatCount: Int = 0,
        pendingTaskCount: Int = 0,
        lastActivityTimestamp: Long? = null
    ): ProjectItem {
        return ProjectItem(
            id = this.id,
            name = this.name,
            description = this.description,
            memberCount = memberCount,
            chatCount = chatCount,
            taskCount = taskCount,
            completedTaskCount = completedTaskCount,
            unreadChatCount = unreadChatCount,
            pendingTaskCount = pendingTaskCount,
            hasUnread = unreadChatCount > 0 || pendingTaskCount > 0,
            isActive = this.status == ProjectStatus.ACTIVE,
            isArchived = this.status == ProjectStatus.ARCHIVED,
            lastActivityTimestamp = lastActivityTimestamp ?: this.updatedAt,
            createdAt = this.createdAt
        )
    }

    /**
     * Convert ProjectMember domain model to UI model
     * NOTE: This creates placeholder data. For screens showing member details,
     * use MembersListViewModel which fetches actual User data via userRepository.getUserById()
     */
    fun ProjectMember.toUIProjectMember(isOnline: Boolean = false): UIProjectMember {
        return UIProjectMember(
            id = this.userId,
            name = "Team Member", // Placeholder - actual user data loaded in Members screen
            initials = "TM",
            role = this.role.name,
            isOnline = isOnline
        )
    }

    /**
     * Get user initials from name
     */
    fun getInitials(name: String): String {
        val parts = name.trim().split(" ")
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts.first().first()}${parts.last().first()}".uppercase()
        }
    }

    /**
     * Format relative time for last activity
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days ${if (days == 1L) "day" else "days"} ago"
            }
            diff < TimeUnit.DAYS.toMillis(30) -> {
                val weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7
                "$weeks ${if (weeks == 1L) "week" else "weeks"} ago"
            }
            else -> {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    /**
     * Calculate project completion percentage
     */
    fun calculateCompletionPercentage(totalTasks: Int, completedTasks: Int): Float {
        if (totalTasks == 0) return 0f
        return (completedTasks.toFloat() / totalTasks.toFloat()) * 100f
    }

    /**
     * Sort projects by various criteria
     */
    fun sortProjects(
        projects: List<ProjectItem>,
        sortBy: ProjectSortOption
    ): List<ProjectItem> {
        return when (sortBy) {
            ProjectSortOption.NAME -> projects.sortedBy { it.name.lowercase() }
            ProjectSortOption.ACTIVITY -> projects.sortedByDescending { it.createdAt } // Use updatedAt if available
            ProjectSortOption.MEMBERS -> projects.sortedByDescending { it.memberCount }
            ProjectSortOption.TASKS -> projects.sortedByDescending { it.taskCount }
        }
    }

    /**
     * Filter projects by status
     */
    fun filterProjects(
        projects: List<ProjectItem>,
        filter: ProjectFilter
    ): List<ProjectItem> {
        return when (filter) {
            ProjectFilter.ALL -> projects
            ProjectFilter.ACTIVE -> projects.filter { it.isActive }
            ProjectFilter.ARCHIVED -> projects.filter { it.isArchived }
        }
    }

    /**
     * Format member count for display
     */
    fun formatMemberCount(count: Int): String {
        return when {
            count == 0 -> "No members"
            count == 1 -> "1 member"
            else -> "$count members"
        }
    }

    /**
     * Format task count for display
     */
    fun formatTaskCount(total: Int, completed: Int): String {
        return if (total == 0) {
            "No tasks"
        } else {
            "$completed of $total tasks"
        }
    }

    /**
     * Get project status color
     */
    fun getProjectStatusColor(project: ProjectItem): String {
        return when {
            project.isArchived -> "#9E9E9E" // Gray
            project.isActive && project.hasUnread -> "#4CAF50" // Green (active with updates)
            project.isActive -> "#2196F3" // Blue (normal active)
            else -> "#FF9800" // Orange (on hold)
        }
    }
}

/**
 * Project sort options
 */
enum class ProjectSortOption {
    NAME, ACTIVITY, MEMBERS, TASKS
}

/**
 * Project filter options
 */
enum class ProjectFilter {
    ALL, ACTIVE, ARCHIVED
}
