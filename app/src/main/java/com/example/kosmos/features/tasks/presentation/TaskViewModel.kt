package com.example.kosmos.features.tasks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    fun loadTasks(chatRoomId: String) {
        viewModelScope.launch {
            try {
                taskRepository.getTasksForChatRoomFlow(chatRoomId).collect { tasks ->
                    _uiState.value = _uiState.value.copy(
                        tasks = tasks,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load tasks: ${e.message}"
                )
            }
        }
    }

    fun createTask(
        chatRoomId: String,
        title: String,
        description: String,
        priority: TaskPriority = TaskPriority.MEDIUM,
        assignedToId: String? = null
    ) {
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Task title cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                val task = Task(
                    chatRoomId = chatRoomId,
                    title = title.trim(),
                    description = description.trim(),
                    priority = priority,
                    assignedToId = assignedToId,
                    assignedToName = assignedToId?.let { getUserDisplayName(it) },
                    createdByName = "Current User", // TODO: Get from auth
                    status = TaskStatus.TODO
                )

                val result = taskRepository.createTask(task)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        showCreateTaskDialog = false,
                        error = null
                    )
                    clearCreateTaskForm()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to create task: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to create task: ${e.message}"
                )
            }
        }
    }

    fun updateTaskStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            try {
                val result = taskRepository.updateTaskStatus(taskId, status)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to update task: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update task: ${e.message}"
                )
            }
        }
    }

    fun assignTask(taskId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = taskRepository.assignTask(taskId, userId)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to assign task: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to assign task: ${e.message}"
                )
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                val result = taskRepository.deleteTask(taskId)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete task: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete task: ${e.message}"
                )
            }
        }
    }

    fun showCreateTaskDialog() {
        _uiState.value = _uiState.value.copy(showCreateTaskDialog = true)
    }

    fun hideCreateTaskDialog() {
        _uiState.value = _uiState.value.copy(showCreateTaskDialog = false)
        clearCreateTaskForm()
    }

    fun updateCreateTaskTitle(title: String) {
        _uiState.value = _uiState.value.copy(createTaskTitle = title)
    }

    fun updateCreateTaskDescription(description: String) {
        _uiState.value = _uiState.value.copy(createTaskDescription = description)
    }

    fun updateCreateTaskPriority(priority: TaskPriority) {
        _uiState.value = _uiState.value.copy(createTaskPriority = priority)
    }

    fun filterTasksByStatus(status: TaskStatus?) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun clearCreateTaskForm() {
        _uiState.value = _uiState.value.copy(
            createTaskTitle = "",
            createTaskDescription = "",
            createTaskPriority = TaskPriority.MEDIUM
        )
    }

    private suspend fun getUserDisplayName(userId: String): String {
        return try {
            val user = userRepository.getUserById(userId)
            user?.displayName ?: "Unknown User"
        } catch (e: Exception) {
            "Unknown User"
        }
    }

    fun getFilteredTasks(): List<Task> {
        val currentState = _uiState.value
        return if (currentState.selectedStatusFilter != null) {
            currentState.tasks.filter { it.status == currentState.selectedStatusFilter }
        } else {
            currentState.tasks
        }
    }
}

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showCreateTaskDialog: Boolean = false,
    val createTaskTitle: String = "",
    val createTaskDescription: String = "",
    val createTaskPriority: TaskPriority = TaskPriority.MEDIUM,
    val selectedStatusFilter: TaskStatus? = null
)