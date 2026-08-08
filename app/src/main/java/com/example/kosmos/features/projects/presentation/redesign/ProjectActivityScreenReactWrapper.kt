package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.ActivityActionType
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.shared.utils.NetworkMonitor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * EntryPoint for accessing TaskRepository
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ProjectActivityEntryPoint {
    fun taskRepository(): TaskRepository
    fun networkMonitor(): NetworkMonitor
}

/**
 * Wrapper for ProjectActivityScreenReact that connects to backend
 *
 * Loads real task activities from TaskRepository and maps to UI models.
 */
@Composable
fun ProjectActivityScreenReactWrapper(
    projectId: String,
    modifier: Modifier = androidx.compose.ui.Modifier
) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ProjectActivityEntryPoint::class.java
        )
    }
    val taskRepository = entryPoint.taskRepository()
    val networkMonitor = entryPoint.networkMonitor()

    // Collect activities from repository
    val activities by taskRepository.getActivityForProjectFlow(projectId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Collect tasks for title lookup
    val tasks by taskRepository.getTasksForProjectFlow(projectId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val isOffline by networkMonitor.isOffline.collectAsState()

    // Build task title map
    val taskTitleMap = remember(tasks) {
        tasks.associate { it.id to it.title }
    }

    // Map TaskActivity to ProjectActivityItem
    val mappedActivities = remember(activities, taskTitleMap) {
        activities.map { activity ->
            mapTaskActivityToProjectActivityItem(activity, taskTitleMap)
        }
    }

    ProjectActivityScreenReact(
        activities = mappedActivities,
        modifier = modifier
    )
}

/**
 * Map TaskActivity (domain model) to ProjectActivityItem (UI model)
 */
private fun mapTaskActivityToProjectActivityItem(activity: TaskActivity, taskTitleMap: Map<String, String> = emptyMap()): ProjectActivityItem {
    val type = when (activity.actionType) {
        ActivityActionType.CREATED -> ProjectActivityType.TASK_CREATED
        ActivityActionType.STATUS_CHANGED -> ProjectActivityType.TASK_STATUS_CHANGED
        ActivityActionType.ASSIGNED, ActivityActionType.UNASSIGNED -> ProjectActivityType.TASK_ASSIGNED
        else -> ProjectActivityType.PROJECT_UPDATED // Default for other action types
    }

    // Include task title if available for context
    val taskTitle = taskTitleMap[activity.taskId]
    val taskSuffix = if (taskTitle != null) " on '$taskTitle'" else ""
    val title = activity.actorName + " " + activity.autoDescription + taskSuffix

    // Build rich description from field changes + commit message
    val descriptionParts = mutableListOf<String>()

    // Add field change details (old → new)
    if (activity.changes.isNotEmpty()) {
        activity.changes.forEach { change ->
            val fieldLabel = when (change.field) {
                "status" -> "Status"
                "priority" -> "Priority"
                "assignedTo" -> "Assigned to"
                "dueDate" -> "Due date"
                "tags" -> "Tags"
                "title" -> "Title"
                "description" -> "Description"
                "estimatedHours" -> "Estimated hours"
                "actualHours" -> "Actual hours"
                else -> change.field.replaceFirstChar { it.uppercase() }
            }
            descriptionParts.add("$fieldLabel: ${change.getFormattedFromValue()} → ${change.getFormattedToValue()}")
        }
    }

    // Add commit message if present
    if (!activity.commitMessage.isNullOrBlank()) {
        descriptionParts.add("\"${activity.commitMessage}\"")
    }

    return ProjectActivityItem(
        id = activity.id,
        type = type,
        userId = activity.actorId,
        userName = activity.actorName,
        userAvatar = activity.actorName.take(1).uppercase(),
        title = title,
        description = descriptionParts.joinToString("\n"),
        timestamp = activity.timestamp
    )
}
