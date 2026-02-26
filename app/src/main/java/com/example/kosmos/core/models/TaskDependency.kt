package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Task Dependency Model
 *
 * Represents a dependency relationship between two tasks.
 *
 * Types:
 * - BLOCKS: Task A blocks Task B (B cannot start until A is done)
 * - BLOCKED_BY: Task A is blocked by Task B (inverse of blocks)
 * - RELATED_TO: Task A is related to Task B (informational only)
 */
@Serializable
@Entity(tableName = "task_dependencies")
data class TaskDependency(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // The task that has the dependency
    @SerialName("task_id") val taskId: String,

    // The task that is depended upon
    @SerialName("depends_on_task_id") val dependsOnTaskId: String,

    // Type of dependency
    @SerialName("dependency_type") val dependencyType: DependencyType = DependencyType.BLOCKS,

    // Metadata
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("created_by") val createdBy: String
) {
    /**
     * Create inverse dependency
     * If A blocks B, the inverse is B is blocked by A
     */
    fun createInverse(): TaskDependency? {
        return when (dependencyType) {
            DependencyType.BLOCKS -> copy(
                id = UUID.randomUUID().toString(),
                taskId = dependsOnTaskId,
                dependsOnTaskId = taskId,
                dependencyType = DependencyType.BLOCKED_BY
            )
            DependencyType.BLOCKED_BY -> copy(
                id = UUID.randomUUID().toString(),
                taskId = dependsOnTaskId,
                dependsOnTaskId = taskId,
                dependencyType = DependencyType.BLOCKS
            )
            DependencyType.RELATED_TO -> copy(
                id = UUID.randomUUID().toString(),
                taskId = dependsOnTaskId,
                dependsOnTaskId = taskId
            )
        }
    }

    companion object {
        /**
         * Create a blocking dependency
         * Task A blocks Task B
         */
        fun createBlocking(
            taskId: String,
            blockedTaskId: String,
            createdBy: String
        ): TaskDependency {
            return TaskDependency(
                taskId = blockedTaskId, // The task that is blocked
                dependsOnTaskId = taskId, // The task that must complete first
                dependencyType = DependencyType.BLOCKS,
                createdBy = createdBy
            )
        }

        /**
         * Create a related dependency
         * Task A is related to Task B
         */
        fun createRelated(
            taskId: String,
            relatedTaskId: String,
            createdBy: String
        ): TaskDependency {
            return TaskDependency(
                taskId = taskId,
                dependsOnTaskId = relatedTaskId,
                dependencyType = DependencyType.RELATED_TO,
                createdBy = createdBy
            )
        }
    }
}

/**
 * Dependency Type Enum
 */
@Serializable
enum class DependencyType {
    @SerialName("blocks") BLOCKS,
    @SerialName("blocked_by") BLOCKED_BY,
    @SerialName("related_to") RELATED_TO;

    fun toDisplayString(): String {
        return when (this) {
            BLOCKS -> "Blocks"
            BLOCKED_BY -> "Blocked By"
            RELATED_TO -> "Related To"
        }
    }
}
