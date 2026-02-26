package com.example.kosmos.features.tasks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.*
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * ViewModel for Task Detail Screen
 * Handles task viewing, editing, subtask management, and comments
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TaskDetailUiState(currentUserId = authRepository.getCurrentUser()?.id ?: "")
    )
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Get task using Flow
                taskRepository.getTaskByIdFlow(taskId).collect { task ->
                    if (task == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Task not found"
                            )
                        }
                        return@collect
                    }

                    // Get assigned user if exists
                    val assignedUser = task.assignedToId?.let { userId ->
                        userRepository.getUserById(userId)
                    }

                    // Get available users from project members
                    val projectMembers = projectRepository.getProjectMembers(task.projectId)
                    val availableUsers = projectMembers.mapNotNull { member ->
                        userRepository.getUserById(member.userId)
                    }

                    // C3 FIX: Use comments embedded in the task (JSONB field)
                    val comments = task.comments

                    _uiState.update {
                        it.copy(
                            task = task,
                            assignedUser = assignedUser,
                            availableUsers = availableUsers,
                            comments = comments,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load task: ${e.message}"
                    )
                }
            }
        }

        // Load subtasks in separate flow
        viewModelScope.launch {
            try {
                taskRepository.getSubtasksFlow(taskId).collect { subtasks ->
                    _uiState.update {
                        it.copy(subtasks = subtasks)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Subtasks loading failure shouldn't break the whole screen
                // Just log or show a non-blocking error
            }
        }
    }

    /**
     * Request status change - shows commit dialog if not disabled
     */
    fun requestStatusChange(newStatus: TaskStatus) {
        val task = _uiState.value.task ?: return

        // Skip dialog if user chose "don't ask again"
        if (_uiState.value.dontAskCommitMessage) {
            updateTaskStatus(newStatus, null)
            return
        }

        // Show commit dialog
        val changes = listOf(
            FieldChange(
                field = "status",
                fromValue = task.status.name,
                toValue = newStatus.name,
                displayFrom = formatStatus(task.status),
                displayTo = formatStatus(newStatus)
            )
        )

        _uiState.update {
            it.copy(
                showCommitDialog = true,
                pendingChanges = changes,
                pendingAction = PendingTaskAction.StatusChange(newStatus)
            )
        }
    }

    /**
     * Request assignment change - shows commit dialog if not disabled
     */
    fun requestAssignmentChange(userId: String, userName: String) {
        val task = _uiState.value.task ?: return

        // Skip dialog if user chose "don't ask again"
        if (_uiState.value.dontAskCommitMessage) {
            assignUser(userId, null)
            return
        }

        // Calculate changes
        val changes = listOf(
            FieldChange(
                field = "assignedTo",
                fromValue = task.assignedToId ?: "unassigned",
                toValue = userId,
                displayFrom = task.assignedToName ?: "Unassigned",
                displayTo = userName
            )
        )

        _uiState.update {
            it.copy(
                showCommitDialog = true,
                pendingChanges = changes,
                pendingAction = PendingTaskAction.Assignment(userId, userName)
            )
        }
    }

    /**
     * Request priority change - shows commit dialog if not disabled
     */
    fun requestPriorityChange(newPriority: TaskPriority) {
        val task = _uiState.value.task ?: return

        if (_uiState.value.dontAskCommitMessage) {
            updateTaskPriority(newPriority, null)
            return
        }

        val changes = listOf(
            FieldChange(
                field = "priority",
                fromValue = task.priority.name,
                toValue = newPriority.name,
                displayFrom = formatPriority(task.priority),
                displayTo = formatPriority(newPriority)
            )
        )

        _uiState.update {
            it.copy(
                showCommitDialog = true,
                pendingChanges = changes,
                pendingAction = PendingTaskAction.PriorityChange(newPriority)
            )
        }
    }

    /**
     * Request tags change - shows commit dialog if not disabled
     */
    fun requestTagsChange(newTags: List<String>) {
        val task = _uiState.value.task ?: return

        if (_uiState.value.dontAskCommitMessage) {
            updateTags(newTags, null)
            return
        }

        val added = newTags - task.tags.toSet()
        val removed = task.tags - newTags.toSet()

        val changes = mutableListOf<FieldChange>()
        if (added.isNotEmpty()) {
            changes.add(
                FieldChange(
                    field = "tags",
                    fromValue = "",
                    toValue = added.joinToString(),
                    displayFrom = "Tags",
                    displayTo = "Added: ${added.joinToString(", ")}"
                )
            )
        }
        if (removed.isNotEmpty()) {
            changes.add(
                FieldChange(
                    field = "tags",
                    fromValue = removed.joinToString(),
                    toValue = "",
                    displayFrom = "Tags",
                    displayTo = "Removed: ${removed.joinToString(", ")}"
                )
            )
        }

        if (changes.isEmpty()) {
            return // No changes
        }

        _uiState.update {
            it.copy(
                showCommitDialog = true,
                pendingChanges = changes,
                pendingAction = PendingTaskAction.TagsChange(newTags)
            )
        }
    }

    /**
     * Request description change - shows commit dialog if not disabled
     */
    fun requestDescriptionChange(newDescription: String) {
        val task = _uiState.value.task ?: return

        if (task.description == newDescription) return // No change

        if (_uiState.value.dontAskCommitMessage) {
            updateDescription(newDescription, null)
            return
        }

        val changes = listOf(
            FieldChange(
                field = "description",
                fromValue = task.description ?: "",
                toValue = newDescription,
                displayFrom = if (task.description.isNullOrBlank()) "No description" else "Description",
                displayTo = if (newDescription.isBlank()) "No description" else "Description updated"
            )
        )

        _uiState.update {
            it.copy(
                showCommitDialog = true,
                pendingChanges = changes,
                pendingAction = PendingTaskAction.DescriptionChange(newDescription)
            )
        }
    }

    /**
     * Request estimated hours change - shows commit dialog if not disabled
     */
    fun requestEstimatedHoursChange(newHours: Float?) {
        val task = _uiState.value.task ?: return

        if (task.estimatedHours == newHours) return // No change

        if (_uiState.value.dontAskCommitMessage) {
            updateEstimatedHours(newHours, null)
            return
        }

        val changes = listOf(
            FieldChange(
                field = "estimatedHours",
                fromValue = task.estimatedHours?.toString() ?: "none",
                toValue = newHours?.toString() ?: "none",
                displayFrom = task.estimatedHours?.let { "${it}h" } ?: "Not set",
                displayTo = newHours?.let { "${it}h" } ?: "Not set"
            )
        )

        _uiState.update {
            it.copy(
                showCommitDialog = true,
                pendingChanges = changes,
                pendingAction = PendingTaskAction.EstimatedHoursChange(newHours)
            )
        }
    }

    /**
     * Request actual hours change - shows commit dialog if not disabled
     */
    fun requestActualHoursChange(newHours: Float?) {
        val task = _uiState.value.task ?: return

        if (task.actualHours == newHours) return // No change

        if (_uiState.value.dontAskCommitMessage) {
            updateActualHours(newHours, null)
            return
        }

        val changes = listOf(
            FieldChange(
                field = "actualHours",
                fromValue = task.actualHours?.toString() ?: "none",
                toValue = newHours?.toString() ?: "none",
                displayFrom = task.actualHours?.let { "${it}h logged" } ?: "No time logged",
                displayTo = newHours?.let { "${it}h logged" } ?: "No time logged"
            )
        )

        _uiState.update {
            it.copy(
                showCommitDialog = true,
                pendingChanges = changes,
                pendingAction = PendingTaskAction.ActualHoursChange(newHours)
            )
        }
    }

    /**
     * Request due date change - shows commit dialog if not disabled
     */
    fun requestDueDateChange(newDueDate: Long?) {
        val task = _uiState.value.task ?: return

        if (task.dueDate == newDueDate) return // No change

        if (_uiState.value.dontAskCommitMessage) {
            updateDueDate(newDueDate, null)
            return
        }

        val changes = listOf(
            FieldChange(
                field = "dueDate",
                fromValue = task.dueDate?.toString() ?: "none",
                toValue = newDueDate?.toString() ?: "none",
                displayFrom = task.dueDate?.let { formatDate(it) } ?: "No due date",
                displayTo = newDueDate?.let { formatDate(it) } ?: "No due date"
            )
        )

        _uiState.update {
            it.copy(
                showCommitDialog = true,
                pendingChanges = changes,
                pendingAction = PendingTaskAction.DueDateChange(newDueDate)
            )
        }
    }

    /**
     * Check if current user can mark task as complete
     * Only the assignee can mark a task as DONE
     */
    fun canMarkTaskComplete(task: Task): Boolean {
        val userId = _uiState.value.currentUserId ?: return false
        // Allow if user is assignee, or if task is unassigned (anyone can complete)
        return task.assignedToId == userId || task.assignedToId == null
    }

    /**
     * Update task status with optional commit message
     */
    private fun updateTaskStatus(newStatus: TaskStatus, commitMessage: String?) {
        val task = _uiState.value.task ?: return

        viewModelScope.launch {
            try {
                // Permission check: Only assignee can mark task as DONE
                if (newStatus == TaskStatus.DONE && !canMarkTaskComplete(task)) {
                    _uiState.update {
                        it.copy(
                            error = "Only the assigned user can mark this task as complete"
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(isUpdating = true) }

                val result = taskRepository.updateTask(
                    task.copy(status = newStatus),
                    _uiState.value.currentUserId,
                    commitMessage = commitMessage
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            task = task.copy(status = newStatus),
                            isUpdating = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Failed to update status"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Failed to update status: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateTaskPriority(newPriority: TaskPriority, commitMessage: String?) {
        val task = _uiState.value.task ?: return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUpdating = true) }

                val result = taskRepository.updateTask(
                    task.copy(priority = newPriority),
                    _uiState.value.currentUserId,
                    commitMessage = commitMessage
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            task = task.copy(priority = newPriority),
                            isUpdating = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Failed to update priority"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Failed to update priority: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateDescription(newDescription: String, commitMessage: String?) {
        val task = _uiState.value.task ?: return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUpdating = true) }

                val result = taskRepository.updateTask(
                    task.copy(description = newDescription),
                    _uiState.value.currentUserId,
                    commitMessage = commitMessage
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            task = task.copy(description = newDescription),
                            isUpdating = false,
                            isEditingDescription = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Failed to update description"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Failed to update description: ${e.message}"
                    )
                }
            }
        }
    }

    fun toggleSubtask(subtaskId: String) {
        viewModelScope.launch {
            try {
                val subtask = _uiState.value.subtasks.find { it.id == subtaskId } ?: return@launch

                val newStatus = if (subtask.status == TaskStatus.DONE) {
                    TaskStatus.TODO
                } else {
                    TaskStatus.DONE
                }

                val result = taskRepository.updateTask(
                    subtask.copy(status = newStatus),
                    _uiState.value.currentUserId
                )

                if (result.isSuccess) {
                    // Reload task to get updated subtasks
                    _uiState.value.task?.let { loadTask(it.id) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(error = "Failed to toggle subtask: ${e.message}")
                }
            }
        }
    }

    fun addComment(comment: String) {
        if (comment.isBlank()) return
        val task = _uiState.value.task ?: return
        val userId = _uiState.value.currentUserId
        if (userId.isBlank()) return

        viewModelScope.launch {
            try {
                // Get current user's display name
                val user = userRepository.getUserById(userId)
                val userName = user?.displayName ?: user?.username ?: "Unknown"

                val result = taskRepository.addComment(
                    taskId = task.id,
                    authorId = userId,
                    authorName = userName,
                    content = comment
                )

                if (result.isFailure) {
                    _uiState.update {
                        it.copy(error = "Failed to add comment: ${result.exceptionOrNull()?.message}")
                    }
                }
                // On success, the Flow from loadTask will automatically update comments
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(error = "Failed to add comment: ${e.message}")
                }
            }
        }
    }

    fun deleteTask() {
        val task = _uiState.value.task ?: return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isDeleting = true) }

                val result = taskRepository.deleteTask(task.id, _uiState.value.currentUserId)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            deleteSuccess = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = "Failed to delete task"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        error = "Failed to delete task: ${e.message}"
                    )
                }
            }
        }
    }

    private fun assignUser(userId: String, commitMessage: String?) {
        val task = _uiState.value.task ?: return
        val currentUserId = authRepository.getCurrentUser()?.id ?: return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUpdating = true) }

                val result = taskRepository.assignTask(
                    taskId = task.id,
                    assigneeUserId = userId,
                    assignerUserId = currentUserId,
                    commitMessage = commitMessage
                )

                if (result.isSuccess) {
                    // Reload task to get updated assignee
                    loadTask(task.id)
                } else {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Failed to assign user"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Failed to assign user: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateTags(newTags: List<String>, commitMessage: String?) {
        val task = _uiState.value.task ?: return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUpdating = true) }

                val result = taskRepository.updateTask(
                    task.copy(tags = newTags),
                    _uiState.value.currentUserId,
                    commitMessage = commitMessage
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            task = task.copy(tags = newTags),
                            isUpdating = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Failed to update tags"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Failed to update tags: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateEstimatedHours(hours: Float?, commitMessage: String?) {
        val task = _uiState.value.task ?: return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUpdating = true) }

                val result = taskRepository.updateTask(
                    task.copy(estimatedHours = hours),
                    _uiState.value.currentUserId,
                    commitMessage = commitMessage
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            task = task.copy(estimatedHours = hours),
                            isUpdating = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Failed to update estimated hours"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Failed to update estimated hours: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateActualHours(hours: Float?, commitMessage: String?) {
        val task = _uiState.value.task ?: return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUpdating = true) }

                val result = taskRepository.updateTask(
                    task.copy(actualHours = hours),
                    _uiState.value.currentUserId,
                    commitMessage = commitMessage
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            task = task.copy(actualHours = hours),
                            isUpdating = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Failed to update actual hours"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Failed to update actual hours: ${e.message}"
                    )
                }
            }
        }
    }

    fun setEditingDescription(editing: Boolean) {
        _uiState.update { it.copy(isEditingDescription = editing) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Bottom Sheet Management
    fun toggleManagementSheet() {
        _uiState.update { it.copy(showManagementSheet = !it.showManagementSheet) }
    }

    // Assign User (overload for User object)
    fun assignUser(user: User) {
        requestAssignmentChange(user.id, user.username)
    }

    /**
     * Confirm commit with optional message
     */
    fun confirmWithCommitMessage(commitMessage: String?) {
        val action = _uiState.value.pendingAction ?: return

        when (action) {
            is PendingTaskAction.StatusChange -> {
                updateTaskStatus(action.newStatus, commitMessage)
            }
            is PendingTaskAction.Assignment -> {
                assignUser(action.userId, commitMessage)
            }
            is PendingTaskAction.PriorityChange -> {
                updateTaskPriority(action.newPriority, commitMessage)
            }
            is PendingTaskAction.DescriptionChange -> {
                updateDescription(action.newDescription, commitMessage)
            }
            is PendingTaskAction.TagsChange -> {
                updateTags(action.newTags, commitMessage)
            }
            is PendingTaskAction.DueDateChange -> {
                updateDueDate(action.newDate, commitMessage)
            }
            is PendingTaskAction.EstimatedHoursChange -> {
                updateEstimatedHours(action.newHours, commitMessage)
            }
            is PendingTaskAction.ActualHoursChange -> {
                updateActualHours(action.newHours, commitMessage)
            }
        }

        dismissCommitDialog()
    }

    /**
     * Dismiss commit dialog
     */
    fun dismissCommitDialog() {
        _uiState.update {
            it.copy(
                showCommitDialog = false,
                pendingChanges = emptyList(),
                pendingAction = null
            )
        }
    }

    /**
     * Set don't ask commit message preference
     */
    fun setDontAskCommitMessage(dontAsk: Boolean) {
        _uiState.update { it.copy(dontAskCommitMessage = dontAsk) }
    }

    /**
     * Update due date with optional commit message
     */
    private fun updateDueDate(newDueDate: Long?, commitMessage: String?) {
        val task = _uiState.value.task ?: return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUpdating = true) }

                val result = taskRepository.updateTask(
                    task.copy(dueDate = newDueDate),
                    _uiState.value.currentUserId,
                    commitMessage = commitMessage
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            task = task.copy(dueDate = newDueDate),
                            isUpdating = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = "Failed to update due date"
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        error = "Failed to update due date: ${e.message}"
                    )
                }
            }
        }
    }

    // Helper functions for formatting
    private fun formatStatus(status: TaskStatus): String {
        return when (status) {
            TaskStatus.TODO -> "To Do"
            TaskStatus.IN_PROGRESS -> "In Progress"
            TaskStatus.DONE -> "Done"
            TaskStatus.CANCELLED -> "Cancelled"
        }
    }

    private fun formatPriority(priority: TaskPriority): String {
        return when (priority) {
            TaskPriority.LOW -> "Low"
            TaskPriority.MEDIUM -> "Medium"
            TaskPriority.HIGH -> "High"
            TaskPriority.URGENT -> "Urgent"
        }
    }

    private fun formatDate(timestamp: Long): String {
        val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return format.format(java.util.Date(timestamp))
    }
}

/**
 * UI state for Task Detail screen
 */
data class TaskDetailUiState(
    val task: Task? = null,
    val assignedUser: User? = null,
    val currentUserId: String = "",
    val availableUsers: List<User> = emptyList(),
    val subtasks: List<Task> = emptyList(),
    val comments: List<TaskComment> = emptyList(),
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val isEditingDescription: Boolean = false,
    val showManagementSheet: Boolean = false, // NEW: For task management bottom sheet
    val error: String? = null,
    // Commit dialog state
    val showCommitDialog: Boolean = false,
    val pendingChanges: List<FieldChange> = emptyList(),
    val pendingAction: PendingTaskAction? = null,
    val dontAskCommitMessage: Boolean = false
)

/**
 * Pending action awaiting commit message
 */
sealed class PendingTaskAction {
    data class StatusChange(val newStatus: TaskStatus) : PendingTaskAction()
    data class PriorityChange(val newPriority: TaskPriority) : PendingTaskAction()
    data class Assignment(val userId: String, val userName: String) : PendingTaskAction()
    data class DueDateChange(val newDate: Long?) : PendingTaskAction()
    data class DescriptionChange(val newDescription: String) : PendingTaskAction()
    data class TagsChange(val newTags: List<String>) : PendingTaskAction()
    data class EstimatedHoursChange(val newHours: Float?) : PendingTaskAction()
    data class ActualHoursChange(val newHours: Float?) : PendingTaskAction()
}
