package com.example.kosmos.features.tasks.presentation.redesign

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.tasks.presentation.TaskViewModel
import com.example.kosmos.shared.ui.mappers.TaskDataMapper.toDomainStatus
import com.example.kosmos.shared.ui.mappers.TaskDataMapper.toDomainPriority

/**
 * Wrapper composable that connects QuickTaskCreationSheet to TaskViewModel
 * Handles task creation with data mapping
 */
@Composable
fun QuickTaskCreationSheetWrapper(
    chatRoomId: String? = null,
    projectId: String,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit, // Returns taskId on success
    viewModel: TaskViewModel = hiltViewModel(),
    projectViewModel: com.example.kosmos.features.project.presentation.ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Convert available users to assignee options with real names
    val availableAssignees = remember(uiState.availableUsersForAssignment) {
        uiState.availableUsersForAssignment.map { user ->
            AssigneeOption(
                id = user.id,
                name = user.displayName.takeIf { it.isNotBlank() } ?: user.username,
                avatarUrl = user.photoUrl
            )
        }
    }

    // Get actual project name
    val project = remember(projectId) {
        projectViewModel.getProjectById(projectId)
    }

    val availableProjects = remember(project) {
        listOfNotNull(
            project?.let {
                ProjectOption(
                    id = it.id,
                    name = it.name
                )
            }
        )
    }

    QuickTaskCreationSheet(
        onDismiss = onDismiss,
        onCreate = { quickTaskData ->
            // Create the task using the ViewModel
            // IMPORTANT: Pass projectId explicitly - tasks MUST belong to a project
            viewModel.createTask(
                projectId = projectId,
                chatRoomId = chatRoomId,
                title = quickTaskData.title,
                description = quickTaskData.description ?: "",
                priority = quickTaskData.priority.toDomainPriority(),
                dueDate = parseDateString(quickTaskData.dueDate),
                assignedToId = quickTaskData.assigneeIds.firstOrNull(),
                tags = emptyList()
            )

            // Success and errors will be handled by LaunchedEffects watching state
        },
        initialProjectId = projectId,
        initialProjectName = project?.name ?: "Unknown Project",
        availableProjects = availableProjects,
        availableAssignees = availableAssignees,
        isCreating = uiState.isCreatingTask
    )

    // Handle task creation success
    LaunchedEffect(uiState.lastCreatedTaskId) {
        uiState.lastCreatedTaskId?.let { taskId ->
            // Notify caller with the actual task ID
            onCreate(taskId)
            // Dismiss the sheet
            onDismiss()
            // Clear the state
            viewModel.clearLastCreatedTask()
        }
    }

    // Handle task creation errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            // Log the error (in a real app, you might show a snackbar/toast)
            android.util.Log.e("QuickTaskWrapper", "Task creation failed: $error")
            // Clear error so it doesn't persist
            viewModel.clearError()
            // Note: Sheet remains open on error so user can retry
        }
    }
}

/**
 * Parse a date string into a timestamp
 * Supports natural language dates: "today", "tomorrow", "next week"
 * Also supports ISO date format: "yyyy-MM-dd"
 * @param dateString Date string to parse
 * @return Timestamp in milliseconds, or null if parsing fails
 */
private fun parseDateString(dateString: String?): Long? {
    if (dateString.isNullOrBlank()) return null

    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L

    return when (dateString.lowercase().trim()) {
        "today" -> now
        "tomorrow" -> now + oneDayMs
        "next week" -> now + (7 * oneDayMs)
        "2 days", "in 2 days" -> now + (2 * oneDayMs)
        "3 days", "in 3 days" -> now + (3 * oneDayMs)
        "1 week", "in 1 week" -> now + (7 * oneDayMs)
        "2 weeks", "in 2 weeks" -> now + (14 * oneDayMs)
        else -> {
            // Try parsing as ISO date (yyyy-MM-dd)
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    java.time.LocalDate.parse(dateString)
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                } else {
                    // Fallback for older Android versions
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    sdf.parse(dateString)?.time
                }
            } catch (e: Exception) {
                // If parsing fails, return null (no due date)
                null
            }
        }
    }
}
