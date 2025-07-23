package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.kosmos.core.database.converters.FieldChangeListConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Task Activity Entity
 *
 * Tracks all changes to tasks with Git-style commit messages.
 * Stores before/after field changes, optional user commit messages,
 * and auto-generated descriptions.
 *
 * Offline-First Pattern:
 * - Save to Room immediately (instant UI update)
 * - Sync to Supabase in background
 * - Continue on failure (activity is safe locally)
 */
@Serializable
@Entity(
    tableName = "task_activity",
    foreignKeys = [
        // P0-05 FIX: Foreign key constraints prevent orphaned activity records
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.NO_ACTION  // Don't cascade — REPLACE strategy in TaskDao triggers DELETE+INSERT which would wipe activity
        ),
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.NO_ACTION  // Don't cascade — activity is audit data
        )
        // NOTE: Removed FK on actorId → users.id. Activity logs are audit records
        // and must not fail to insert if user isn't cached locally yet.
        // actorId is still indexed for query performance.
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["projectId"]),
        Index(value = ["actorId"]),
        Index(value = ["timestamp"])
    ]
)
@TypeConverters(FieldChangeListConverter::class)
data class TaskActivity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Foreign keys
    @SerialName("task_id") val taskId: String,
    @SerialName("project_id") val projectId: String,

    // Actor information (snapshot at time of action for audit trail)
    @SerialName("actor_id") val actorId: String,
    @SerialName("actor_name") val actorName: String,
    @SerialName("actor_role") val actorRole: String? = null,

    // Action metadata
    @SerialName("action_type") val actionType: ActivityActionType,
    val timestamp: Long = System.currentTimeMillis(),

    // Change tracking
    val changes: List<FieldChange> = emptyList(),

    // User-provided commit message (optional)
    @SerialName("commit_message") val commitMessage: String? = null,

    // System-generated description (required)
    @SerialName("auto_description") val autoDescription: String,

    // Additional context (extensible metadata)
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Activity Action Type Enum
 *
 * All possible task change types that are tracked.
 */
@Serializable
enum class ActivityActionType {
    @SerialName("created") CREATED,
    @SerialName("updated") UPDATED,
    @SerialName("status_changed") STATUS_CHANGED,
    @SerialName("priority_changed") PRIORITY_CHANGED,
    @SerialName("assigned") ASSIGNED,
    @SerialName("unassigned") UNASSIGNED,
    @SerialName("description_changed") DESCRIPTION_CHANGED,
    @SerialName("due_date_changed") DUE_DATE_CHANGED,
    @SerialName("tags_updated") TAGS_UPDATED,
    @SerialName("comment_added") COMMENT_ADDED,
    @SerialName("time_logged") TIME_LOGGED,
    @SerialName("dependency_added") DEPENDENCY_ADDED,
    @SerialName("dependency_removed") DEPENDENCY_REMOVED,
    @SerialName("subtask_added") SUBTASK_ADDED,
    @SerialName("archived") ARCHIVED,
    @SerialName("restored") RESTORED,
    @SerialName("deleted") DELETED,
    @SerialName("journal_entry") JOURNAL_ENTRY
}

/**
 * Field Change Data Class
 *
 * Represents a before/after change for a specific field.
 * Stores both raw values and human-readable display values.
 */
@Serializable
data class FieldChange(
    val field: String,
    val fromValue: String?,
    val toValue: String?,
    val displayFrom: String? = null,
    val displayTo: String? = null
) {
    /**
     * Get human-readable "from" value
     */
    fun getFormattedFromValue(): String = displayFrom ?: fromValue ?: "None"

    /**
     * Get human-readable "to" value
     */
    fun getFormattedToValue(): String = displayTo ?: toValue ?: "None"
}

/**
 * Activity Description Generator
 *
 * Generates auto-descriptions for all activity types.
 * Used when user doesn't provide a commit message.
 */
object ActivityDescriptionGenerator {

    /**
     * Generate auto-description based on action type and changes
     */
    fun generate(
        actionType: ActivityActionType,
        changes: List<FieldChange> = emptyList(),
        taskTitle: String? = null
    ): String {
        return when (actionType) {
            ActivityActionType.CREATED -> "created this task"

            ActivityActionType.STATUS_CHANGED -> {
                val statusChange = changes.firstOrNull { it.field == "status" }
                if (statusChange != null) {
                    "changed status from ${statusChange.getFormattedFromValue()} to ${statusChange.getFormattedToValue()}"
                } else {
                    "changed status"
                }
            }

            ActivityActionType.PRIORITY_CHANGED -> {
                val priorityChange = changes.firstOrNull { it.field == "priority" }
                if (priorityChange != null) {
                    "changed priority from ${priorityChange.getFormattedFromValue()} to ${priorityChange.getFormattedToValue()}"
                } else {
                    "changed priority"
                }
            }

            ActivityActionType.ASSIGNED -> {
                val assigneeChange = changes.firstOrNull { it.field == "assignedTo" }
                if (assigneeChange != null) {
                    val newAssignee = assigneeChange.getFormattedToValue()
                    if (newAssignee == "None" || newAssignee == "Unassigned") {
                        "unassigned this task"
                    } else {
                        "assigned to $newAssignee"
                    }
                } else {
                    "assigned this task"
                }
            }

            ActivityActionType.UNASSIGNED -> "unassigned this task"

            ActivityActionType.DESCRIPTION_CHANGED -> "updated description"

            ActivityActionType.DUE_DATE_CHANGED -> {
                val dueDateChange = changes.firstOrNull { it.field == "dueDate" }
                if (dueDateChange != null) {
                    val from = dueDateChange.getFormattedFromValue()
                    val to = dueDateChange.getFormattedToValue()
                    if (from == "None") {
                        "set due date to $to"
                    } else if (to == "None") {
                        "removed due date"
                    } else {
                        "changed due date from $from to $to"
                    }
                } else {
                    "updated due date"
                }
            }

            ActivityActionType.TAGS_UPDATED -> {
                val tagsChange = changes.firstOrNull { it.field == "tags" }
                if (tagsChange != null) {
                    "updated tags: ${tagsChange.getFormattedToValue()}"
                } else {
                    "updated tags"
                }
            }

            ActivityActionType.COMMENT_ADDED -> "added a comment"

            ActivityActionType.TIME_LOGGED -> {
                val timeChange = changes.firstOrNull { it.field == "actualHours" }
                if (timeChange != null) {
                    val hours = timeChange.toValue?.toFloatOrNull() ?: 0f
                    "logged ${formatHours(hours)} of work"
                } else {
                    "logged time"
                }
            }

            ActivityActionType.DEPENDENCY_ADDED -> {
                val dependencyChange = changes.firstOrNull { it.field == "dependency" }
                if (dependencyChange != null && taskTitle != null) {
                    "added dependency: $taskTitle"
                } else {
                    "added a dependency"
                }
            }

            ActivityActionType.DEPENDENCY_REMOVED -> {
                val dependencyChange = changes.firstOrNull { it.field == "dependency" }
                if (dependencyChange != null && taskTitle != null) {
                    "removed dependency: $taskTitle"
                } else {
                    "removed a dependency"
                }
            }

            ActivityActionType.SUBTASK_ADDED -> {
                val subtaskChange = changes.firstOrNull { it.field == "subtask" }
                if (subtaskChange != null) {
                    "added subtask: ${subtaskChange.toValue}"
                } else {
                    "added a subtask"
                }
            }

            ActivityActionType.ARCHIVED -> "archived this task"

            ActivityActionType.RESTORED -> "restored this task"

            ActivityActionType.DELETED -> "deleted this task"

            ActivityActionType.JOURNAL_ENTRY -> "added a journal entry"

            ActivityActionType.UPDATED -> {
                val fieldCount = changes.size
                if (fieldCount == 1) {
                    "updated ${changes.first().field}"
                } else if (fieldCount > 1) {
                    "updated $fieldCount fields"
                } else {
                    "updated this task"
                }
            }
        }
    }

    /**
     * Format hours for display
     */
    private fun formatHours(hours: Float): String {
        return when {
            hours < 1 -> {
                val minutes = (hours * 60).toInt()
                "$minutes minutes"
            }
            hours == 1f -> "1 hour"
            hours % 1 == 0f -> "${hours.toInt()} hours"
            else -> String.format("%.1f hours", hours)
        }
    }
}

/**
 * Extension function to determine if commit message should be prompted
 */
fun ActivityActionType.shouldPromptCommitMessage(): Boolean {
    return when (this) {
        // Always prompt for these important changes
        ActivityActionType.STATUS_CHANGED,
        ActivityActionType.ASSIGNED,
        ActivityActionType.DUE_DATE_CHANGED,
        ActivityActionType.DESCRIPTION_CHANGED -> true

        // Don't prompt for these minor/system actions
        ActivityActionType.CREATED,
        ActivityActionType.PRIORITY_CHANGED,
        ActivityActionType.TAGS_UPDATED,
        ActivityActionType.COMMENT_ADDED,
        ActivityActionType.TIME_LOGGED,
        ActivityActionType.DEPENDENCY_ADDED,
        ActivityActionType.DEPENDENCY_REMOVED,
        ActivityActionType.SUBTASK_ADDED,
        ActivityActionType.ARCHIVED,
        ActivityActionType.RESTORED,
        ActivityActionType.DELETED,
        ActivityActionType.UPDATED,
        ActivityActionType.UNASSIGNED,
        ActivityActionType.JOURNAL_ENTRY -> false
    }
}
