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

    val isOffline by networkMonitor.isOffline.collectAsState()

    // Map TaskActivity to ProjectActivityItem
    val mappedActivities = remember(activities) {
        activities.map { activity ->
            mapTaskActivityToProjectActivityItem(activity)
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
private fun mapTaskActivityToProjectActivityItem(activity: TaskActivity): ProjectActivityItem {
    val type = when (activity.actionType) {
        ActivityActionType.CREATED -> ProjectActivityType.TASK_CREATED
        ActivityActionType.STATUS_CHANGED -> ProjectActivityType.TASK_STATUS_CHANGED
        ActivityActionType.ASSIGNED, ActivityActionType.UNASSIGNED -> ProjectActivityType.TASK_ASSIGNED
        else -> ProjectActivityType.PROJECT_UPDATED // Default for other action types
    }

    // Generate title based on action type
    val title = when (activity.actionType) {
        ActivityActionType.CREATED -> "Created a task"
        ActivityActionType.STATUS_CHANGED -> "Updated task status"
        ActivityActionType.ASSIGNED -> "Assigned a task"
        ActivityActionType.UNASSIGNED -> "Unassigned a task"
        ActivityActionType.PRIORITY_CHANGED -> "Changed task priority"
        ActivityActionType.DUE_DATE_CHANGED -> "Updated due date"
        ActivityActionType.COMMENT_ADDED -> "Added a comment"
        ActivityActionType.DELETED -> "Deleted a task"
        else -> "Updated a task"
    }

    return ProjectActivityItem(
        id = activity.id,
        type = type,
        userId = activity.actorId,
        userName = activity.actorName,
        userAvatar = activity.actorName.take(1).uppercase(),
        title = title,
        description = activity.commitMessage ?: activity.autoDescription,
        timestamp = activity.timestamp
    )
}
