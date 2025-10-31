package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = "",

    /**
     * Project this task belongs to (replaces chatRoomId)
     */
    @SerialName("project_id")
    val projectId: String = "",

    /**
     * Optional chat room where this task is being discussed
     */
    @SerialName("chat_room_id")
    val chatRoomId: String? = null,

    val title: String = "",
    val description: String? = null,  // Nullable to handle NULL values from database
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,

    /**
     * User ID of assigned member
     */
    @SerialName("assigned_to_id")
    val assignedToId: String? = null,

    /**
     * Display name of assigned member
     */
    @SerialName("assigned_to_name")
    val assignedToName: String? = null,

    /**
     * Role of the assigned member at the time of assignment
     * Used to validate task assignment hierarchy
     */
    @SerialName("assigned_to_role")
    val assignedToRole: ProjectRole? = null,

    /**
     * User ID of task creator
     */
    @SerialName("created_by_id")
    val createdById: String = "",

    /**
     * Display name of task creator
     */
    @SerialName("created_by_name")
    val createdByName: String = "",

    /**
     * Role of the creator at the time of task creation
     * Used to validate assignment permissions
     */
    @SerialName("created_by_role")
    val createdByRole: ProjectRole? = null,

    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @SerialName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @SerialName("due_date")
    val dueDate: Long? = null,

    /**
     * Message that created this task (if created from chat)
     */
    @SerialName("source_message_id")
    val sourceMessageId: String? = null,

    val tags: List<String> = emptyList(),
    val comments: List<TaskComment> = emptyList(),

    /**
     * Parent task ID for subtask support (Phase 2 feature)
     * null = top-level task
     */
    @SerialName("parent_task_id")
    val parentTaskId: String? = null,

    /**
     * Estimated time in hours for task completion
     */
    @SerialName("estimated_hours")
    val estimatedHours: Float? = null,

    /**
     * Actual time spent in hours
     */
    @SerialName("actual_hours")
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

    @SerialName("author_id")
    val authorId: String = "",

    @SerialName("author_name")
    val authorName: String = "",

    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)