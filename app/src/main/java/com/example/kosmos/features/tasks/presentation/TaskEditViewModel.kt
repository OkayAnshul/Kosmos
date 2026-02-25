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

/**
 * ViewModel for Task Edit Screen
 *
 * Handles comprehensive task editing with draft state management and validation.
 * Tracks original task vs draft to detect unsaved changes.
 */
@HiltViewModel
class TaskEditViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TaskEditUiState(currentUserId = authRepository.getCurrentUser()?.id ?: "")
    )
    val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

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

                    // Get available users from project members
                    val projectMembers = projectRepository.getProjectMembers(task.projectId)
                    val availableUsers = projectMembers.mapNotNull { member ->
                        userRepository.getUserById(member.userId)
                    }

                    _uiState.update {
                        it.copy(
                            originalTask = task,
                            draftTask = task.copy(), // Create draft copy
                            availableUsers = availableUsers,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load task: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Update a specific field in the draft task
     */
    fun updateDraftField(field: String, value: Any?) {
        val draft = _uiState.value.draftTask ?: return

        val updatedDraft = when (field) {
            "title" -> draft.copy(title = value as String)
            "description" -> draft.copy(description = value as? String)
            "status" -> draft.copy(status = value as TaskStatus)
            "priority" -> draft.copy(priority = value as TaskPriority)
            "assignedToId" -> {
                val user = value as? User
                draft.copy(
                    assignedToId = user?.id,
                    assignedToName = user?.username
                // Note: assignedToRole will be set by repository when saving
                )
            }
            "dueDate" -> draft.copy(dueDate = value as? Long)
            "estimatedHours" -> draft.copy(estimatedHours = value as? Float)
            "actualHours" -> draft.copy(actualHours = value as? Float)
            "tags" -> draft.copy(tags = value as List<String>)
            else -> draft
        }

        _uiState.update {
            it.copy(
                draftTask = updatedDraft,
                validationErrors = it.validationErrors - field // Clear field error on change
            )
        }
    }

    /**
     * Request to save with commit dialog
     * Shows commit dialog if there are significant changes
     */
    fun requestSaveWithCommit() {
        val draft = _uiState.value.draftTask ?: return
        val original = _uiState.value.originalTask ?: return

        // First validate
        val errors = mutableMapOf<String, String>()
        validateTitle(draft.title)?.let { errors["title"] = it }
        validateDescription(draft.description)?.let { errors["description"] = it }
        validateEstimatedHours(draft.estimatedHours)?.let { errors["estimatedHours"] = it }
        validateActualHours(draft.actualHours)?.let { errors["actualHours"] = it }
        validateTags(draft.tags)?.let { errors["tags"] = it }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        // Skip dialog if user chose "don't ask again"
        if (_uiState.value.dontAskCommitMessage) {
            validateAndSave(null)
            return
        }

        // Calculate changes
        val changes = calculateChanges(original, draft)

        // If there are significant changes, show commit dialog
        if (changes.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    showCommitDialog = true,
                    pendingChanges = changes
                )
            }
        } else {
            // No significant changes, just save
            validateAndSave(null)
        }
    }

    /**
     * Validate all fields and save if valid
     */
    private fun validateAndSave(commitMessage: String?) {
        val draft = _uiState.value.draftTask ?: return

        val errors = mutableMapOf<String, String>()

        // Validate title
        validateTitle(draft.title)?.let { errors["title"] = it }

        // Validate description
        validateDescription(draft.description)?.let { errors["description"] = it }

        // Validate estimated hours
        validateEstimatedHours(draft.estimatedHours)?.let { errors["estimatedHours"] = it }

        // Validate actual hours
        validateActualHours(draft.actualHours)?.let { errors["actualHours"] = it }

        // Validate tags
        validateTags(draft.tags)?.let { errors["tags"] = it }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        // Save task
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true, error = null) }

                val result = taskRepository.updateTask(
                    draft,
                    _uiState.value.currentUserId,
                    commitMessage = commitMessage
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            originalTask = draft,
                            draftTask = draft.copy()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "Failed to save task"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Failed to save task: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Discard all changes and reset to original
     */
    fun discardChanges() {
        val original = _uiState.value.originalTask ?: return
        _uiState.update {
            it.copy(
                draftTask = original.copy(),
                validationErrors = emptyMap(),
                showUnsavedWarning = false
            )
        }
    }

    /**
     * Check if there are unsaved changes
     */
    fun hasUnsavedChanges(): Boolean {
        val original = _uiState.value.originalTask
        val draft = _uiState.value.draftTask
        return original != draft
    }

    fun showUnsavedWarning(show: Boolean) {
        _uiState.update { it.copy(showUnsavedWarning = show) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ========================================================================
    // Validation Methods
    // ========================================================================

    private fun validateTitle(title: String): String? {
        return when {
            title.isBlank() -> "Title is required"
            title.length > 200 -> "Title must be 200 characters or less"
            else -> null
        }
    }

    private fun validateDescription(description: String?): String? {
        if (description == null) return null
        return when {
            description.length > 5000 -> "Description must be 5000 characters or less"
            else -> null
        }
    }

    private fun validateEstimatedHours(hours: Float?): String? {
        if (hours == null) return null
        return when {
            hours < 0f -> "Estimated hours must be positive"
            hours > 9999f -> "Estimated hours must be less than 10,000"
            else -> null
        }
    }

    private fun validateActualHours(hours: Float?): String? {
        if (hours == null) return null
        return when {
            hours < 0f -> "Actual hours must be positive"
            hours > 9999f -> "Actual hours must be less than 10,000"
            else -> null
        }
    }

    private fun validateTags(tags: List<String>): String? {
        return when {
            tags.size > 10 -> "Maximum 10 tags allowed"
            tags.any { it.length > 30 } -> "Each tag must be 30 characters or less"
            tags.any { it.isBlank() } -> "Tags cannot be empty"
            else -> null
        }
    }

    // ========================================================================
    // Commit Dialog Methods
    // ========================================================================

    /**
     * Calculate changes between original and draft task
     */
    private fun calculateChanges(original: Task, draft: Task): List<FieldChange> {
        val changes = mutableListOf<FieldChange>()

        // Status change
        if (original.status != draft.status) {
            changes.add(
                FieldChange(
                    field = "status",
                    fromValue = original.status.name,
                    toValue = draft.status.name,
                    displayFrom = formatStatus(original.status),
                    displayTo = formatStatus(draft.status)
                )
            )
        }

        // Priority change
        if (original.priority != draft.priority) {
            changes.add(
                FieldChange(
                    field = "priority",
                    fromValue = original.priority.name,
                    toValue = draft.priority.name,
                    displayFrom = formatPriority(original.priority),
                    displayTo = formatPriority(draft.priority)
                )
            )
        }

        // Assignment change
        if (original.assignedToId != draft.assignedToId) {
            changes.add(
                FieldChange(
                    field = "assignedTo",
                    fromValue = original.assignedToId ?: "unassigned",
                    toValue = draft.assignedToId ?: "unassigned",
                    displayFrom = original.assignedToName ?: "Unassigned",
                    displayTo = draft.assignedToName ?: "Unassigned"
                )
            )
        }

        // Due date change
        if (original.dueDate != draft.dueDate) {
            changes.add(
                FieldChange(
                    field = "dueDate",
                    fromValue = original.dueDate?.toString() ?: "none",
                    toValue = draft.dueDate?.toString() ?: "none",
                    displayFrom = original.dueDate?.let { formatDate(it) } ?: "No due date",
                    displayTo = draft.dueDate?.let { formatDate(it) } ?: "No due date"
                )
            )
        }

        // Title change
        if (original.title != draft.title) {
            changes.add(
                FieldChange(
                    field = "title",
                    fromValue = original.title,
                    toValue = draft.title,
                    displayFrom = "\"${original.title}\"",
                    displayTo = "\"${draft.title}\""
                )
            )
        }

        // Description change
        if (original.description != draft.description) {
            changes.add(
                FieldChange(
                    field = "description",
                    fromValue = original.description ?: "",
                    toValue = draft.description ?: "",
                    displayFrom = if (original.description.isNullOrBlank()) "No description" else "Updated",
                    displayTo = if (draft.description.isNullOrBlank()) "No description" else "Updated"
                )
            )
        }

        return changes
    }

    /**
     * Confirm commit with optional message
     */
    fun confirmWithCommitMessage(commitMessage: String?) {
        validateAndSave(commitMessage)
        dismissCommitDialog()
    }

    /**
     * Dismiss commit dialog
     */
    fun dismissCommitDialog() {
        _uiState.update {
            it.copy(
                showCommitDialog = false,
                pendingChanges = emptyList()
            )
        }
    }

    /**
     * Set don't ask commit message preference
     */
    fun setDontAskCommitMessage(dontAsk: Boolean) {
        _uiState.update { it.copy(dontAskCommitMessage = dontAsk) }
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
        val instant = java.time.Instant.ofEpochMilli(timestamp)
        val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
        return date.format(formatter)
    }
}

/**
 * UI state for Task Edit screen
 */
data class TaskEditUiState(
    val originalTask: Task? = null,           // From DB (unchanged)
    val draftTask: Task? = null,              // User edits (uncommitted)
    val validationErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val showUnsavedWarning: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUserId: String = "",
    val availableUsers: List<User> = emptyList(),
    // Commit dialog state
    val showCommitDialog: Boolean = false,
    val pendingChanges: List<FieldChange> = emptyList(),
    val dontAskCommitMessage: Boolean = false
)
