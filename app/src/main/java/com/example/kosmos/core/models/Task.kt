package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = "",
    val chatRoomId: String = "",
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val assignedToId: String? = null,
    val assignedToName: String? = null,
    val createdById: String = "",
    val createdByName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val sourceMessageId: String? = null, // Message that created this task
    val tags: List<String> = emptyList(),
    val comments: List<TaskComment> = emptyList()
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