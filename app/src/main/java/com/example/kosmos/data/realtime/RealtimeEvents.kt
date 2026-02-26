package com.example.kosmos.data.realtime

import com.example.kosmos.core.models.Task
import kotlinx.serialization.Serializable

/**
 * Real-time Event Models
 *
 * Events emitted by Supabase Realtime for task collaboration.
 * Used for live updates, presence, and editing indicators.
 */

/**
 * Task Event
 *
 * Sealed class for all task-related real-time events
 */
sealed class TaskEvent {
    data class Insert(val task: Task) : TaskEvent()
    data class Update(val task: Task) : TaskEvent()
    data class Delete(val taskId: String) : TaskEvent()
}

/**
 * Task Editing Event
 *
 * Broadcast when a user starts/stops editing a specific field.
 * Used to show "Being edited by..." indicators.
 */
@Serializable
data class TaskEditingEvent(
    val taskId: String,
    val userId: String,
    val userName: String,
    val isEditing: Boolean,
    val field: String?, // null when stopped editing
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Task Presence Event
 *
 * Broadcast when users join/leave a task view.
 * Used for presence indicators (who's viewing).
 */
@Serializable
data class TaskPresenceEvent(
    val taskId: String,
    val viewers: List<TaskViewer>
)

/**
 * Task Viewer
 *
 * Represents a user currently viewing a task
 */
@Serializable
data class TaskViewer(
    val userId: String,
    val userName: String,
    val photoUrl: String?,
    val joinedAt: Long = System.currentTimeMillis()
)

/**
 * Task Activity Event
 *
 * Real-time event when new activity is added
 */
@Serializable
data class TaskActivityEvent(
    val taskId: String,
    val activityId: String,
    val actorId: String,
    val actorName: String,
    val actionType: String,
    val timestamp: Long
)

/**
 * Editing Status
 *
 * Tracks who is editing what field on a task
 */
data class EditingStatus(
    val field: String,
    val users: List<TaskViewer>
) {
    fun getEditingUserNames(): String {
        return when {
            users.isEmpty() -> ""
            users.size == 1 -> users.first().userName
            else -> "${users.first().userName} and ${users.size - 1} other${if (users.size > 2) "s" else ""}"
        }
    }
}

/**
 * Presence State
 *
 * Aggregated presence state for a task
 */
data class PresenceState(
    val taskId: String,
    val viewers: List<TaskViewer>,
    val editingStatus: Map<String, EditingStatus> = emptyMap()
) {
    /**
     * Get viewers excluding current user
     */
    fun getOtherViewers(currentUserId: String): List<TaskViewer> {
        return viewers.filter { it.userId != currentUserId }
    }

    /**
     * Get total viewer count
     */
    fun getViewerCount(): Int = viewers.size

    /**
     * Check if a field is being edited
     */
    fun isFieldBeingEdited(field: String): Boolean {
        return editingStatus[field]?.users?.isNotEmpty() == true
    }

    /**
     * Get who is editing a field
     */
    fun getFieldEditors(field: String): List<TaskViewer> {
        return editingStatus[field]?.users ?: emptyList()
    }
}

/**
 * Realtime Error
 *
 * BUG-009 FIX: Sealed class for realtime operation errors
 * Emitted when parsing fails or operations fail
 */
sealed class RealtimeError {
    /**
     * Parse failed error - emitted when a record cannot be parsed
     * @param entityType The type of entity being parsed (message, task, task_activity)
     * @param availableFields The fields that were present in the record (for debugging)
     */
    data class ParseFailed(
        val entityType: String,
        val availableFields: List<String>
    ) : RealtimeError()

    /**
     * Operation failed error - emitted when a database operation fails
     * @param operation The operation that failed (message_insert, task_update, etc.)
     * @param errorMessage The error message from the exception
     */
    data class OperationFailed(
        val operation: String,
        val errorMessage: String
    ) : RealtimeError()
}
