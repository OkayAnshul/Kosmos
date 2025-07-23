package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

@Serializable
@Entity(
    tableName = "tasks",
    foreignKeys = [
        // NO_ACTION: REPLACE strategy in DAOs does DELETE+INSERT, CASCADE would wipe child records during sync
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ChatRoom::class,
            parentColumns = ["id"],
            childColumns = ["chatRoomId"],
            onDelete = ForeignKey.SET_NULL  // Tasks can exist without chat room
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["assignedToId"],
            onDelete = ForeignKey.SET_NULL  // Task remains if assignee deleted
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["createdById"],
            onDelete = ForeignKey.NO_ACTION  // Keep tasks even if creator deleted
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["chatRoomId"]),
        Index(value = ["assignedToId"]),
        Index(value = ["createdById"])
    ]
)
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

    /**
     * P1-11: Version field for optimistic locking
     * Incremented on every update to detect conflicts
     */
    val version: Int = 1,

    @SerialName("due_date")
    val dueDate: Long? = null,

    /**
     * Message that created this task (if created from chat)
     */
    @SerialName("source_message_id")
    val sourceMessageId: String? = null,

    val tags: List<String> = emptyList(),
    @Serializable(with = CommentsSerializer::class)
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

/**
 * Resilient deserializer for comments JSONB column.
 * Historical rows may have been double-encoded (stored as a JSON string literal instead of array).
 * This unwraps the string form transparently.
 */
object CommentsSerializer : JsonTransformingSerializer<List<TaskComment>>(
    ListSerializer(TaskComment.serializer())
) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonPrimitive && element.isString)
            Json.Default.decodeFromString(JsonElement.serializer(), element.content)
        else element
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