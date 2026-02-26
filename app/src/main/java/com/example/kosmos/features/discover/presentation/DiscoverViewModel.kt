package com.example.kosmos.features.discover.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.ConnectionStatus
import com.example.kosmos.core.models.JoinRequestStatus
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.User
import com.example.kosmos.core.feedback.UserFeedbackManager
import com.example.kosmos.core.feedback.safeCall
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ProjectJoinRequestRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.data.repository.UserConnectionRepository
import com.example.kosmos.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val query: String = "",
    val selectedTab: Int = 0, // 0 = People, 1 = Projects
    val users: List<User> = emptyList(),
    val projects: List<Project> = emptyList(),
    val connectionStatuses: Map<String, ConnectionStatus> = emptyMap(),
    val joinRequestStatuses: Map<String, JoinRequestStatus> = emptyMap(),
    val memberProjectIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val userConnectionRepository: UserConnectionRepository,
    private val joinRequestRepository: ProjectJoinRequestRepository,
    private val authRepository: AuthRepository,
    private val feedbackManager: UserFeedbackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val currentUserId: String?
        get() = authRepository.getCurrentUser()?.id

    fun onTabSelected(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab < 2 && _uiState.value.query.isNotBlank()) search(_uiState.value.query)
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            search(query)
        }
    }

    private fun search(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(users = emptyList(), projects = emptyList(), isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val myId = currentUserId ?: ""
                if (_uiState.value.selectedTab == 0) {
                    val result = userRepository.searchUsersPublic(query, excludeIds = listOfNotNull(myId.takeIf { it.isNotBlank() }))
                    if (result.isSuccess) {
                        val users = result.getOrDefault(emptyList())
                        _uiState.update { it.copy(users = users, isLoading = false) }
                        loadConnectionStatuses(users.map { it.id })
                    } else {
                        _uiState.update { it.copy(error = result.exceptionOrNull()?.message, isLoading = false) }
                    }
                } else {
                    val result = projectRepository.searchPublicProjects(query)
                    if (result.isSuccess) {
                        val projects = result.getOrDefault(emptyList())
                        _uiState.update { it.copy(projects = projects, isLoading = false) }
                        loadJoinRequestStatuses(projects.map { it.id })
                    } else {
                        _uiState.update { it.copy(error = result.exceptionOrNull()?.message, isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private suspend fun loadConnectionStatuses(userIds: List<String>) {
        val myId = currentUserId ?: return
        // Sync connections from Supabase so Room has fresh data
        safeCall(feedbackManager, tag = "DiscoverViewModel", action = "sync connections") {
            userConnectionRepository.syncFromSupabase(myId)
        }
        val statuses = mutableMapOf<String, ConnectionStatus>()
        userIds.forEach { userId ->
            safeCall(feedbackManager, tag = "DiscoverViewModel", action = "load connection status") {
                val status = userConnectionRepository.getConnectionStatus(myId, userId)
                if (status != null) statuses[userId] = status
            }
        }
        _uiState.update { it.copy(connectionStatuses = statuses) }
    }

    private suspend fun loadJoinRequestStatuses(projectIds: List<String>) {
        val myId = currentUserId ?: return
        val statuses = mutableMapOf<String, JoinRequestStatus>()
        val memberIds = mutableSetOf<String>()
        projectIds.forEach { projectId ->
            safeCall(feedbackManager, tag = "DiscoverViewModel", action = "load join request status") {
                val request = joinRequestRepository.getMyRequestForProject(myId, projectId)
                if (request != null) statuses[projectId] = request.status
            }
            // Check if already a member
            safeCall(feedbackManager, tag = "DiscoverViewModel", action = "check project membership") {
                val members = projectRepository.getProjectMembers(projectId)
                if (members.any { it.userId == myId }) memberIds.add(projectId)
            }
        }
        _uiState.update { it.copy(joinRequestStatuses = statuses, memberProjectIds = memberIds) }
    }

    fun sendConnectionRequest(targetUserId: String) {
        val myId = currentUserId ?: return
        viewModelScope.launch {
            try {
                val myUser = userRepository.getUserById(myId)
                userConnectionRepository.sendRequest(myId, targetUserId, myUser?.displayName ?: "")
                _uiState.update {
                    it.copy(connectionStatuses = it.connectionStatuses + (targetUserId to ConnectionStatus.PENDING))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun requestToJoin(projectId: String, message: String? = null) {
        val myId = currentUserId ?: return
        viewModelScope.launch {
            try {
                val myUser = userRepository.getUserById(myId)
                val project = projectRepository.getProject(projectId)
                joinRequestRepository.requestToJoin(
                    projectId = projectId,
                    requesterId = myId,
                    message = message,
                    projectName = project?.name ?: "",
                    requesterName = myUser?.displayName ?: ""
                )
                _uiState.update {
                    it.copy(joinRequestStatuses = it.joinRequestStatuses + (projectId to JoinRequestStatus.PENDING))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
