package com.example.kosmos.features.tasks.presentation.redesign

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.core.models.User
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Wrapper for MyTasksScreenReact that connects to the backend for PROJECT-SCOPED tasks.
 * Handles all task actions (status change, assign, delete, time entry) inline with dialogs.
 */
@Composable
fun ProjectTasksScreenReactWrapper(
    projectId: String,
    onTaskClick: (String) -> Unit,
    onCreateTask: () -> Unit,
    onTaskEdit: (String) -> Unit = {},
    taskRepository: TaskRepository = hiltViewModel<ProjectTasksDataViewModel>().taskRepository,
    authRepository: AuthRepository = hiltViewModel<ProjectTasksDataViewModel>().authRepository,
    projectRepository: ProjectRepository = hiltViewModel<ProjectTasksDataViewModel>().projectRepository,
    userRepository: UserRepository = hiltViewModel<ProjectTasksDataViewModel>().userRepository
) {
    var viewMode by remember { mutableStateOf(TaskViewModeReact.LIST) }
    var filter by remember { mutableStateOf(TaskFilterReact.ALL) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentUser = authRepository.getCurrentUser()

    val tasks by taskRepository.getTasksForProjectFlow(projectId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val project by projectRepository.getProjectFlow(projectId)
        .collectAsStateWithLifecycle(initialValue = null)

    val projectName = project?.name

    // Dialog states
    var statusDialogTaskId by remember { mutableStateOf<String?>(null) }
    var assignDialogTaskId by remember { mutableStateOf<String?>(null) }
    var deleteDialogTaskId by remember { mutableStateOf<String?>(null) }
    var timeEntryDialogTaskId by remember { mutableStateOf<String?>(null) }
    var assignableUsers by remember { mutableStateOf<List<User>>(emptyList()) }

    val taskDataList = tasks.map { task ->
        TaskData(
            id = task.id,
            title = task.title,
            status = when (task.status) {
                TaskStatus.TODO -> TaskStatusReact.TODO
                TaskStatus.IN_PROGRESS -> TaskStatusReact.IN_PROGRESS
                TaskStatus.DONE -> TaskStatusReact.DONE
                TaskStatus.CANCELLED -> TaskStatusReact.DONE
            },
            priority = when (task.priority) {
                TaskPriority.LOW -> TaskPriorityReact.LOW
                TaskPriority.MEDIUM -> TaskPriorityReact.MEDIUM
                TaskPriority.HIGH -> TaskPriorityReact.HIGH
                TaskPriority.URGENT -> TaskPriorityReact.HIGH
            },
            dueDate = task.dueDate?.let { formatProjectDueDate(it) },
            assignee = task.assignedToName?.let { name ->
                Assignee(
                    name = name,
                    avatar = name.firstOrNull()?.toString() ?: "?"
                )
            },
            projectName = projectName
        )
    }

    // ── Status Picker Dialog ──
    statusDialogTaskId?.let { taskId ->
        val task = tasks.find { it.id == taskId }
        if (task != null) {
            ProjectTaskStatusPickerDialog(
                currentStatus = task.status,
                onStatusSelected = { newStatus ->
                    statusDialogTaskId = null
                    coroutineScope.launch {
                        val result = taskRepository.updateTaskStatus(taskId, newStatus, currentUser?.id ?: "")
                        val msg = if (result.isSuccess) "Status → ${newStatus.name.replace('_', ' ')}"
                        else result.exceptionOrNull()?.message ?: "Status update failed"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { statusDialogTaskId = null }
            )
        }
    }

    // ── Assign Dialog ──
    assignDialogTaskId?.let { taskId ->
        val task = tasks.find { it.id == taskId }
        if (task != null) {
            ProjectTaskAssignPickerDialog(
                users = assignableUsers,
                currentAssigneeId = task.assignedToId,
                onUserSelected = { userId ->
                    assignDialogTaskId = null
                    coroutineScope.launch {
                        val result = taskRepository.assignTask(taskId, userId, currentUser?.id ?: "")
                        val msg = if (result.isSuccess) "Task assigned"
                        else result.exceptionOrNull()?.message ?: "Assign failed"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onUnassign = {
                    assignDialogTaskId = null
                    coroutineScope.launch {
                        val result = taskRepository.unassignTask(taskId, currentUser?.id ?: "")
                        val msg = if (result.isSuccess) "Task unassigned"
                        else result.exceptionOrNull()?.message ?: "Unassign failed"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { assignDialogTaskId = null }
            )
        }
    }

    // ── Delete Confirmation Dialog ──
    deleteDialogTaskId?.let { taskId ->
        val task = tasks.find { it.id == taskId }
        AlertDialog(
            onDismissRequest = { deleteDialogTaskId = null },
            title = { Text("Delete Task", color = ColorTokens.ReactTheme.foreground) },
            text = {
                Text(
                    "Delete \"${task?.title ?: "this task"}\"? This cannot be undone.",
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteDialogTaskId = null
                    coroutineScope.launch {
                        val result = taskRepository.deleteTask(taskId, currentUser?.id ?: "")
                        val msg = if (result.isSuccess) "Task deleted"
                        else result.exceptionOrNull()?.message ?: "Delete failed"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Delete", color = ColorTokens.ReactTheme.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogTaskId = null }) {
                    Text("Cancel", color = ColorTokens.ReactTheme.mutedForeground)
                }
            },
            containerColor = ColorTokens.ReactTheme.card,
            shape = RoundedCornerShape(Tokens.CornerRadius.lg)
        )
    }

    // ── Time Entry Dialog ──
    timeEntryDialogTaskId?.let { taskId ->
        val task = tasks.find { it.id == taskId }
        if (task != null) {
            com.example.kosmos.features.tasks.components.AddManualTimeEntryDialog(
                isVisible = true,
                onConfirm = { startTime, endTime, description, _, _ ->
                    timeEntryDialogTaskId = null
                    coroutineScope.launch {
                        val result = taskRepository.addManualTimeEntry(
                            taskId = taskId,
                            projectId = task.projectId,
                            userId = currentUser?.id ?: "",
                            startTime = startTime,
                            endTime = endTime,
                            description = description
                        )
                        val msg = if (result.isSuccess) "Time entry added"
                        else result.exceptionOrNull()?.message ?: "Failed to add time entry"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { timeEntryDialogTaskId = null }
            )
        }
    }

    MyTasksScreenReact(
        tasks = taskDataList,
        viewMode = viewMode,
        onViewModeChange = { viewMode = it },
        filter = filter,
        onFilterChange = { filter = it },
        onTaskClick = onTaskClick,
        onTaskEdit = onTaskEdit,
        onTaskDelete = { taskId -> deleteDialogTaskId = taskId },
        onTaskStatusChange = { taskId -> statusDialogTaskId = taskId },
        onTaskAssign = { taskId ->
            coroutineScope.launch {
                val members = projectRepository.getProjectMembers(projectId)
                val users = members.mapNotNull { member ->
                    userRepository.getUserById(member.userId)
                }
                assignableUsers = users
                assignDialogTaskId = taskId
            }
        },
        onTaskAddTimeEntry = { taskId -> timeEntryDialogTaskId = taskId },
        onCreateTask = onCreateTask
    )
}

// ── Status Picker Dialog ──
@Composable
private fun ProjectTaskStatusPickerDialog(
    currentStatus: TaskStatus,
    onStatusSelected: (TaskStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Status", color = ColorTokens.ReactTheme.foreground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    TaskStatus.TODO to "To Do",
                    TaskStatus.IN_PROGRESS to "In Progress",
                    TaskStatus.DONE to "Done",
                    TaskStatus.CANCELLED to "Cancelled"
                ).forEach { (status, label) ->
                    val isSelected = status == currentStatus
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
                                else ColorTokens.ReactTheme.card
                            )
                            .clickable { onStatusSelected(status) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = when (status) {
                                TaskStatus.TODO -> Icons.Default.RadioButtonUnchecked
                                TaskStatus.IN_PROGRESS -> Icons.Default.Pending
                                TaskStatus.DONE -> Icons.Default.CheckCircle
                                TaskStatus.CANCELLED -> Icons.Default.Cancel
                            },
                            contentDescription = null,
                            tint = if (isSelected) ColorTokens.ReactTheme.primary
                            else ColorTokens.ReactTheme.mutedForeground,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) ColorTokens.ReactTheme.primary
                            else ColorTokens.ReactTheme.foreground
                        )
                        if (isSelected) {
                            Spacer(Modifier.weight(1f))
                            Text("Current", fontSize = 11.sp, color = ColorTokens.ReactTheme.mutedForeground)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ColorTokens.ReactTheme.mutedForeground)
            }
        },
        containerColor = ColorTokens.ReactTheme.card,
        shape = RoundedCornerShape(Tokens.CornerRadius.lg)
    )
}

// ── Assign Picker Dialog ──
@Composable
private fun ProjectTaskAssignPickerDialog(
    users: List<User>,
    currentAssigneeId: String?,
    onUserSelected: (String) -> Unit,
    onUnassign: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign To", color = ColorTokens.ReactTheme.foreground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (users.isEmpty()) {
                    Text("No project members found", color = ColorTokens.ReactTheme.mutedForeground, fontSize = 13.sp)
                }
                if (currentAssigneeId != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onUnassign() }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.PersonOff, null, tint = ColorTokens.ReactTheme.destructive, modifier = Modifier.size(20.dp))
                        Text("Unassign", fontSize = 14.sp, color = ColorTokens.ReactTheme.destructive)
                    }
                    HorizontalDivider(color = ColorTokens.ReactTheme.border.copy(alpha = 0.3f))
                }
                users.forEach { user ->
                    val isSelected = user.id == currentAssigneeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
                                else ColorTokens.ReactTheme.card
                            )
                            .clickable { onUserSelected(user.id) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp).background(ColorTokens.ReactTheme.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (user.displayName.firstOrNull() ?: user.email.first()).uppercase(),
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ColorTokens.ReactTheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                user.displayName.ifEmpty { user.username.ifEmpty { user.email } },
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = ColorTokens.ReactTheme.foreground
                            )
                            if (user.displayName.isNotEmpty()) {
                                Text(user.email, fontSize = 11.sp, color = ColorTokens.ReactTheme.mutedForeground)
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = ColorTokens.ReactTheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ColorTokens.ReactTheme.mutedForeground)
            }
        },
        containerColor = ColorTokens.ReactTheme.card,
        shape = RoundedCornerShape(Tokens.CornerRadius.lg)
    )
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ProjectTasksDataViewModel @Inject constructor(
    val taskRepository: TaskRepository,
    val authRepository: AuthRepository,
    val projectRepository: ProjectRepository,
    val userRepository: UserRepository
) : androidx.lifecycle.ViewModel()

private fun formatProjectDueDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
