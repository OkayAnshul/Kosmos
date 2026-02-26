package com.example.kosmos.core.sync

import com.example.kosmos.core.models.Task
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task Conflict Resolver
 *
 * Handles conflicts when multiple users edit the same task simultaneously.
 *
 * Strategy: Last-write-wins with field-level merging
 * - Compare timestamps to determine which change is newer
 * - Detect true conflicts (same field, different users, close time)
 * - Merge compatible changes (different fields)
 * - Show conflict dialog for unresolvable conflicts
 *
 * Usage:
 * ```kotlin
 * val result = conflictResolver.resolve(
 *     localTask = localTask,
 *     remoteTask = remoteTask,
 *     currentUserId = userId
 * )
 *
 * when (result) {
 *     is ConflictResolution.AutoMerged -> taskDao.updateTask(result.mergedTask)
 *     is ConflictResolution.RequiresUserInput -> showConflictDialog(result.conflicts)
 *     is ConflictResolution.KeepLocal -> // Do nothing
 *     is ConflictResolution.KeepRemote -> taskDao.updateTask(result.task)
 * }
 * ```
 */
@Singleton
class TaskConflictResolver @Inject constructor() {

    companion object {
        // If two edits are within this time window, consider it a true conflict
        private const val CONFLICT_TIME_WINDOW_MS = 5000L // 5 seconds
    }

    /**
     * Resolve conflict between local and remote task versions
     *
     * @param localTask The local version of the task
     * @param remoteTask The remote version from Supabase
     * @param currentUserId The current user's ID
     * @return ConflictResolution result
     */
    fun resolve(
        localTask: Task,
        remoteTask: Task,
        currentUserId: String
    ): ConflictResolution {
        // If timestamps are the same, no conflict
        if (localTask.updatedAt == remoteTask.updatedAt) {
            return ConflictResolution.KeepLocal
        }

        // If local is newer, keep local
        if (localTask.updatedAt > remoteTask.updatedAt) {
            return ConflictResolution.KeepLocal
        }

        // If remote is newer, check for true conflicts
        val timeDiff = remoteTask.updatedAt - localTask.updatedAt
        val isRecentConflict = timeDiff < CONFLICT_TIME_WINDOW_MS

        // Calculate field-level changes
        val localChanges = calculateChanges(localTask, remoteTask)
        val remoteChanges = calculateChanges(remoteTask, localTask)

        // If no local changes, just accept remote
        if (localChanges.isEmpty()) {
            return ConflictResolution.KeepRemote(remoteTask)
        }

        // If no remote changes, keep local (shouldn't happen but defensive)
        if (remoteChanges.isEmpty()) {
            return ConflictResolution.KeepLocal
        }

        // Check for field-level conflicts
        val conflictingFields = localChanges.keys.intersect(remoteChanges.keys)

        // If no overlapping fields, auto-merge is safe
        if (conflictingFields.isEmpty()) {
            val mergedTask = mergeNonConflictingChanges(localTask, remoteTask)
            return ConflictResolution.AutoMerged(mergedTask)
        }

        // If conflict is recent and involves same fields, require user input
        if (isRecentConflict) {
            val conflicts = conflictingFields.map { field ->
                FieldConflict(
                    fieldName = field,
                    localValue = getFieldValue(localTask, field),
                    remoteValue = getFieldValue(remoteTask, field),
                    localTimestamp = localTask.updatedAt,
                    remoteTimestamp = remoteTask.updatedAt
                )
            }
            return ConflictResolution.RequiresUserInput(
                localTask = localTask,
                remoteTask = remoteTask,
                conflicts = conflicts
            )
        }

        // For older conflicts, apply last-write-wins (remote wins)
        return ConflictResolution.KeepRemote(remoteTask)
    }

    /**
     * Calculate changes between two task versions
     * Returns map of field name to changed value
     */
    private fun calculateChanges(from: Task, to: Task): Map<String, Any?> {
        val changes = mutableMapOf<String, Any?>()

        if (from.title != to.title) changes["title"] = to.title
        if (from.description != to.description) changes["description"] = to.description
        if (from.status != to.status) changes["status"] = to.status
        if (from.priority != to.priority) changes["priority"] = to.priority
        if (from.assignedToId != to.assignedToId) changes["assignedToId"] = to.assignedToId
        if (from.dueDate != to.dueDate) changes["dueDate"] = to.dueDate
        if (from.tags != to.tags) changes["tags"] = to.tags
        if (from.estimatedHours != to.estimatedHours) changes["estimatedHours"] = to.estimatedHours
        if (from.actualHours != to.actualHours) changes["actualHours"] = to.actualHours

        return changes
    }

    /**
     * Merge non-conflicting changes from local and remote
     * Applies changes from both sides that don't overlap
     */
    private fun mergeNonConflictingChanges(localTask: Task, remoteTask: Task): Task {
        // Start with remote as base (it's newer)
        var merged = remoteTask

        // Apply local changes that aren't in remote
        val localChanges = calculateChanges(localTask, remoteTask)
        val remoteChanges = calculateChanges(remoteTask, localTask)
        val localOnlyChanges = localChanges.keys - remoteChanges.keys

        localOnlyChanges.forEach { field ->
            merged = when (field) {
                "title" -> merged.copy(title = localTask.title)
                "description" -> merged.copy(description = localTask.description)
                "status" -> merged.copy(status = localTask.status)
                "priority" -> merged.copy(priority = localTask.priority)
                "assignedToId" -> merged.copy(assignedToId = localTask.assignedToId)
                "dueDate" -> merged.copy(dueDate = localTask.dueDate)
                "tags" -> merged.copy(tags = localTask.tags)
                "estimatedHours" -> merged.copy(estimatedHours = localTask.estimatedHours)
                "actualHours" -> merged.copy(actualHours = localTask.actualHours)
                else -> merged
            }
        }

        return merged
    }

    /**
     * Get field value from task by field name
     */
    private fun getFieldValue(task: Task, fieldName: String): Any? {
        return when (fieldName) {
            "title" -> task.title
            "description" -> task.description
            "status" -> task.status
            "priority" -> task.priority
            "assignedToId" -> task.assignedToId
            "dueDate" -> task.dueDate
            "tags" -> task.tags
            "estimatedHours" -> task.estimatedHours
            "actualHours" -> task.actualHours
            else -> null
        }
    }

    /**
     * Apply user's conflict resolution choices
     *
     * @param baseTask The base task to apply choices to
     * @param choices Map of field name to chosen value (local or remote)
     * @return Merged task with user choices applied
     */
    fun applyUserChoices(
        baseTask: Task,
        choices: Map<String, ConflictChoice>
    ): Task {
        var result = baseTask

        choices.forEach { (field, choice) ->
            val value = when (choice) {
                is ConflictChoice.KeepLocal -> choice.value
                is ConflictChoice.KeepRemote -> choice.value
            }

            result = when (field) {
                "title" -> result.copy(title = value as String)
                "description" -> result.copy(description = value as? String)
                "status" -> result.copy(status = value as com.example.kosmos.core.models.TaskStatus)
                "priority" -> result.copy(priority = value as com.example.kosmos.core.models.TaskPriority)
                "assignedToId" -> result.copy(assignedToId = value as? String)
                "dueDate" -> result.copy(dueDate = value as? Long)
                "tags" -> result.copy(tags = value as List<String>)
                "estimatedHours" -> result.copy(estimatedHours = value as? Float)
                "actualHours" -> result.copy(actualHours = value as? Float)
                else -> result
            }
        }

        return result
    }
}

/**
 * Conflict Resolution Result
 */
sealed class ConflictResolution {
    /**
     * Keep local version (local is newer or no conflict)
     */
    object KeepLocal : ConflictResolution()

    /**
     * Keep remote version (remote is newer, no conflicts)
     */
    data class KeepRemote(val task: Task) : ConflictResolution()

    /**
     * Auto-merged version (non-overlapping changes)
     */
    data class AutoMerged(val mergedTask: Task) : ConflictResolution()

    /**
     * Requires user input (conflicting fields)
     */
    data class RequiresUserInput(
        val localTask: Task,
        val remoteTask: Task,
        val conflicts: List<FieldConflict>
    ) : ConflictResolution()
}

/**
 * Field-level conflict
 */
data class FieldConflict(
    val fieldName: String,
    val localValue: Any?,
    val remoteValue: Any?,
    val localTimestamp: Long,
    val remoteTimestamp: Long
)

/**
 * User's choice for resolving a conflict
 */
sealed class ConflictChoice {
    data class KeepLocal(val value: Any?) : ConflictChoice()
    data class KeepRemote(val value: Any?) : ConflictChoice()
}
