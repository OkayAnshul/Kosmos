package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = "",

    /**
     * Project this task belongs to (replaces chatRoomId)
     */
    val projectId: String = "",

    /**
     * Optional chat room where this task is being discussed
     */
    val chatRoomId: String? = null,

    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,

    /**
     * User ID of assigned member
     */
    val assignedToId: String? = null,

    /**
     * Display name of assigned member
     */
    val assignedToName: String? = null,

    /**
     * Role of the assigned member at the time of assignment
     * Used to validate task assignment hierarchy
     */
    val assignedToRole: ProjectRole? = null,

    /**
     * User ID of task creator
     */
    val createdById: String = "",

    /**
     * Display name of task creator
     */
    val createdByName: String = "",

    /**
     * Role of the creator at the time of task creation
     * Used to validate assignment permissions
     */
    val createdByRole: ProjectRole? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,

    /**
     * Message that created this task (if created from chat)
     */
    val sourceMessageId: String? = null,

    val tags: List<String> = emptyList(),
    val comments: List<TaskComment> = emptyList(),

    /**
     * Parent task ID for subtask support (Phase 2 feature)
     * null = top-level task
     */
    val parentTaskId: String? = null,

    /**
     * Estimated time in hours for task completion
     */
    val estimatedHours: Float? = null,

    /**
     * Actual time spent in hours
     */
    val actualHours: Float? = null
)

@Serializable
enum class TaskStatus {
    TODO, IN_PROGRESS, DONE, CANCELLED
}

@Serializable
enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}

@Serializable
data class TaskComment(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)