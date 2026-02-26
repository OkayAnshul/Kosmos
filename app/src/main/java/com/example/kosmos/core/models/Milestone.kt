package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Milestone Model
 *
 * Represents a milestone for grouping and organizing tasks within a project.
 *
 * Features:
 * - Group tasks by milestone
 * - Track milestone progress
 * - Set due dates for milestones
 * - Color-code milestones
 */
@Serializable
@Entity(tableName = "milestones")
data class Milestone(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Foreign key
    @SerialName("project_id") val projectId: String,

    // Milestone details
    val name: String,
    val description: String? = null,
    @SerialName("due_date") val dueDate: Long? = null,
    val status: MilestoneStatus = MilestoneStatus.ACTIVE,
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,

    // Metadata
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String,
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if milestone is overdue
     */
    fun isOverdue(): Boolean {
        return dueDate?.let { it < System.currentTimeMillis() } ?: false
    }

    /**
     * Check if milestone is due soon (within 7 days)
     */
    fun isDueSoon(): Boolean {
        val sevenDaysFromNow = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)
        return dueDate?.let { it < sevenDaysFromNow && it >= System.currentTimeMillis() } ?: false
    }

    /**
     * Get display color or default
     */
    fun getDisplayColor(): String {
        return color ?: "#6366F1" // Default indigo
    }

    companion object {
        /**
         * Create a new milestone
         */
        fun create(
            projectId: String,
            name: String,
            description: String? = null,
            dueDate: Long? = null,
            color: String? = null,
            createdBy: String
        ): Milestone {
            return Milestone(
                projectId = projectId,
                name = name,
                description = description,
                dueDate = dueDate,
                color = color,
                createdBy = createdBy
            )
        }
    }
}

/**
 * Milestone Status Enum
 */
@Serializable
enum class MilestoneStatus {
    @SerialName("active") ACTIVE,
    @SerialName("completed") COMPLETED,
    @SerialName("archived") ARCHIVED;

    fun toDisplayString(): String {
        return when (this) {
            ACTIVE -> "Active"
            COMPLETED -> "Completed"
            ARCHIVED -> "Archived"
        }
    }
}

/**
 * Milestone with Progress
 *
 * Extended model with calculated progress metrics
 */
data class MilestoneWithProgress(
    val milestone: Milestone,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val inProgressTasks: Int = 0,
    val todoTasks: Int = 0
) {
    /**
     * Calculate completion percentage (0-100)
     */
    fun getCompletionPercentage(): Float {
        return if (totalTasks > 0) {
            (completedTasks.toFloat() / totalTasks.toFloat()) * 100f
        } else {
            0f
        }
    }

    /**
     * Check if milestone is complete (all tasks done)
     */
    fun isComplete(): Boolean {
        return totalTasks > 0 && completedTasks == totalTasks
    }

    /**
     * Get status summary string
     */
    fun getStatusSummary(): String {
        return "$completedTasks of $totalTasks tasks completed"
    }
}
