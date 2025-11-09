package com.example.kosmos.core.models

/**
 * Statistics for a project
 * Used to display aggregate counts in ProjectListScreen
 */
data class ProjectStats(
    val projectId: String,
    val memberCount: Int = 0,
    val chatCount: Int = 0,
    val taskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val unreadChatCount: Int = 0,
    val pendingTaskCount: Int = 0,
    val lastActivityTime: Long? = null
) {
    /**
     * Calculate task completion percentage
     * @return Percentage (0-100) or null if no tasks
     */
    val completionPercentage: Int?
        get() = if (taskCount > 0) {
            (completedTaskCount * 100) / taskCount
        } else {
            null
        }
}
