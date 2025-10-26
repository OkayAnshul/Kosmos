package com.example.kosmos.features.tasks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ChatRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.TaskRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.Task
import com.example.kosmos.core.models.TaskComment
import com.example.kosmos.core.models.TaskPriority
import com.example.kosmos.core.models.TaskStatus
import com.example.kosmos.core.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val currentUser = authRepository.getCurrentUser()

    private val _uiState = MutableStateFlow(TaskUiState(currentUserId = currentUser?.id))
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private var currentProjectId: String? = null

    // Track flow collection jobs for proper cleanup
    private var chatRoomFlowJob: Job? = null
    private var tasksFlowJob: Job? = null
    private var projectMembersJob: Job? = null

    fun loadTasks(chatRoomId: String) {
        // Cancel previous jobs
        chatRoomFlowJob?.cancel()
        tasksFlowJob?.cancel()

        chatRoomFlowJob = viewModelScope.launch {
            try {
                // Get chatRoom to extract projectId
                chatRepository.getChatRoomByIdFlow(chatRoomId).collect { chatRoom ->
                    if (chatRoom != null && currentProjectId != chatRoom.projectId) {
                        currentProjectId = chatRoom.projectId

                        // Load project members for assignment
                        if (chatRoom.projectId.isNotEmpty()) {
                            loadUsersForAssignment(chatRoom.projectId)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TaskViewModel", "Failed to load chat room", e)
            }
        }

        // Trigger Supabase sync in background
        viewModelScope.launch {
            try {
                taskRepository.syncTasksForChatRoom(chatRoomId)
            } catch (e: Exception) {
                // Log but don't block - local data will still be displayed
                android.util.Log.w("TaskViewModel", "Failed to sync tasks from Supabase", e)
            }
        }

        // Collect from Room Flow (will update when sync completes)
        tasksFlowJob = viewModelScope.launch {
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
        assignedToId: String? = null,
        dueDate: Long? = null,
        tags: List<String> = emptyList()
    ) {
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Task title cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                currentUser?.let { user ->
                    val task = Task(
                        projectId = currentProjectId ?: "",
                        chatRoomId = chatRoomId,
                        title = title.trim(),
                        description = description.trim(),
                        priority = priority,
                        assignedToId = assignedToId,
                        assignedToName = assignedToId?.let { getUserDisplayName(it) },
                        createdById = user.id,
                        createdByName = user.displayName ?: "Current User",
                        status = TaskStatus.TODO,
                        dueDate = dueDate,
                        tags = tags
                    )

                    val result = taskRepository.createTask(task, user.id)
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

    fun assignTask(taskId: String, assigneeUserId: String) {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    val result = taskRepository.assignTask(taskId, assigneeUserId, user.id)
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

    fun showEditTaskDialog(task: Task) {
        _uiState.value = _uiState.value.copy(
            showEditTaskDialog = true,
            editingTask = task,
            createTaskTitle = task.title,
            createTaskDescription = task.description,
            createTaskPriority = task.priority,
            createTaskDueDate = task.dueDate,
            createTaskTags = task.tags,
            createTaskAssignedTo = task.assignedToId?.let { id ->
                _uiState.value.availableUsersForAssignment.find { it.id == id }
            }
        )
    }

    fun hideEditTaskDialog() {
        _uiState.value = _uiState.value.copy(
            showEditTaskDialog = false,
            editingTask = null
        )
        clearCreateTaskForm()
    }

    fun showDeleteConfirmation(task: Task) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = true,
            taskToDelete = task
        )
    }

    fun hideDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = false,
            taskToDelete = null
        )
    }

    fun confirmDeleteTask() {
        val task = _uiState.value.taskToDelete ?: return

        viewModelScope.launch {
            try {
                val result = taskRepository.deleteTask(task.id)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        showDeleteConfirmation = false,
                        taskToDelete = null,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        showDeleteConfirmation = false,
                        taskToDelete = null,
                        error = "Failed to delete task: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    showDeleteConfirmation = false,
                    taskToDelete = null,
                    error = "Failed to delete task: ${e.message}"
                )
            }
        }
    }

    fun editTask(
        taskId: String,
        title: String,
        description: String,
        priority: TaskPriority,
        assignedToId: String?,
        dueDate: Long?,
        tags: List<String>
    ) {
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Task title cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                // Use the editingTask from state instead of fetching
                val existingTask = _uiState.value.editingTask
                if (existingTask != null) {
                    val updatedTask = existingTask.copy(
                        title = title.trim(),
                        description = description.trim(),
                        priority = priority,
                        assignedToId = assignedToId,
                        assignedToName = assignedToId?.let { getUserDisplayName(it) },
                        dueDate = dueDate,
                        tags = tags
                    )

                    val result = taskRepository.updateTask(updatedTask)
                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            showEditTaskDialog = false,
                            editingTask = null,
                            error = null
                        )
                        clearCreateTaskForm()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to update task: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update task: ${e.message}"
                )
            }
        }
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

    fun updateCreateTaskDueDate(dueDate: Long?) {
        _uiState.value = _uiState.value.copy(createTaskDueDate = dueDate)
    }

    fun updateCreateTaskAssignedTo(user: User?) {
        _uiState.value = _uiState.value.copy(createTaskAssignedTo = user)
    }

    fun addCreateTaskTag(tag: String) {
        val trimmedTag = tag.trim()
        if (trimmedTag.isNotBlank() && !_uiState.value.createTaskTags.contains(trimmedTag)) {
            val updatedTags = _uiState.value.createTaskTags + trimmedTag
            _uiState.value = _uiState.value.copy(createTaskTags = updatedTags)
        }
    }

    fun removeCreateTaskTag(tag: String) {
        val updatedTags = _uiState.value.createTaskTags - tag
        _uiState.value = _uiState.value.copy(createTaskTags = updatedTags)
    }

    fun loadUsersForAssignment(projectId: String) {
        // Cancel previous job
        projectMembersJob?.cancel()

        projectMembersJob = viewModelScope.launch {
            try {
                // Get all project members from Flow
                projectRepository.getProjectMembersFlow(projectId).collect { members ->
                    // Load user details for each member
                    val users = mutableListOf<User>()
                    members.forEach { member ->
                        val user = userRepository.getUserById(member.userId)
                        if (user != null) {
                            users.add(user)
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        availableUsersForAssignment = users,
                        projectMembers = members
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load project members: ${e.message}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        // Cancel all flow collection jobs
        chatRoomFlowJob?.cancel()
        tasksFlowJob?.cancel()
        projectMembersJob?.cancel()
    }

    fun filterTasksByStatus(status: TaskStatus?) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
    }

    fun toggleMyTasksFilter() {
        _uiState.value = _uiState.value.copy(showOnlyMyTasks = !_uiState.value.showOnlyMyTasks)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun clearCreateTaskForm() {
        _uiState.value = _uiState.value.copy(
            createTaskTitle = "",
            createTaskDescription = "",
            createTaskPriority = TaskPriority.MEDIUM,
            createTaskDueDate = null,
            createTaskAssignedTo = null,
            createTaskTags = emptyList()
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

    /**
     * Add a comment to a task
     * @param taskId Task ID
     * @param content Comment content
     */
    fun addComment(taskId: String, content: String) {
        if (content.isBlank()) return

        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    // Get task from editing state or find in task list
                    val task = _uiState.value.editingTask
                        ?: _uiState.value.tasks.find { it.id == taskId }
                        ?: return@launch

                    val newComment = TaskComment(
                        id = java.util.UUID.randomUUID().toString(),
                        authorId = user.id,
                        authorName = user.displayName ?: user.email,
                        content = content.trim(),
                        timestamp = System.currentTimeMillis()
                    )

                    val updatedTask = task.copy(
                        comments = task.comments + newComment,
                        updatedAt = System.currentTimeMillis()
                    )

                    val result = taskRepository.updateTask(updatedTask)
                    if (result.isSuccess) {
                        // Update editing task in state if we're editing this task
                        if (_uiState.value.editingTask?.id == taskId) {
                            _uiState.value = _uiState.value.copy(
                                editingTask = updatedTask
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to add comment: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to add comment: ${e.message}"
                    )
                }
            }
        }
    }
}

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showCreateTaskDialog: Boolean = false,
    val showEditTaskDialog: Boolean = false,
    val editingTask: Task? = null,
    val showDeleteConfirmation: Boolean = false,
    val taskToDelete: Task? = null,
    val createTaskTitle: String = "",
    val createTaskDescription: String = "",
    val createTaskPriority: TaskPriority = TaskPriority.MEDIUM,
    val createTaskDueDate: Long? = null,
    val createTaskAssignedTo: User? = null,
    val createTaskTags: List<String> = emptyList(),
    val availableUsersForAssignment: List<User> = emptyList(),
    val projectMembers: List<ProjectMember> = emptyList(),
    val selectedStatusFilter: TaskStatus? = null,
    val showOnlyMyTasks: Boolean = false,
    val currentUserId: String? = null
)