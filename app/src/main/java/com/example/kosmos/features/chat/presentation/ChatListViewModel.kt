package com.example.kosmos.features.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ChatRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.core.models.User
import com.example.kosmos.core.models.ChatRoom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val currentUser = authRepository.getCurrentUser()

    init {
        if (currentUser != null) {
            // loadChatRooms() will be called from ChatListScreen with projectId
            loadCurrentUser()
        }
    }

    private fun loadCurrentUser() {
        currentUser?.let { firebaseUser ->
            viewModelScope.launch {
                try {
                    val user = userRepository.getUserById(firebaseUser.id)
                    _uiState.value = _uiState.value.copy(currentUser = user)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load user profile"
                    )
                }
            }
        }
    }

    /**
     * Load chat rooms for a specific project
     * @param projectId Project ID to filter chat rooms
     */
    fun loadChatRooms(projectId: String) {
        currentUser?.let { firebaseUser ->
            viewModelScope.launch {
                try {
                    // Use the new method that filters by projectId at repository level
                    chatRepository.getChatRoomsForProject(firebaseUser.id, projectId).collect { chatRooms ->
                        _uiState.value = _uiState.value.copy(
                            chatRooms = chatRooms,
                            isLoading = false
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load chat rooms: ${e.message}"
                    )
                }
            }
        }
    }

    fun createNewChatRoom(
        name: String,
        description: String,
        selectedUserIds: List<String>,
        projectId: String
    ) {
        currentUser?.let { firebaseUser ->
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(isCreatingChat = true)

                    val participantIds = selectedUserIds + firebaseUser.id
                    val chatRoom = ChatRoom(
                        projectId = projectId,
                        name = name,
                        description = description,
                        participantIds = participantIds,
                        createdBy = firebaseUser.id,
                        createdAt = System.currentTimeMillis()
                    )

                    val result = chatRepository.createChatRoom(chatRoom)
                    result.fold(
                        onSuccess = { chatRoomId ->
                            _uiState.value = _uiState.value.copy(
                                isCreatingChat = false,
                                showCreateChatDialog = false
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isCreatingChat = false,
                                error = "Failed to create chat room: ${exception.message}"
                            )
                        }
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isCreatingChat = false,
                        error = "Failed to create chat room: ${e.message}"
                    )
                }
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }

        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    _uiState.value = _uiState.value.copy(searchResults = emptyList())
                    return@launch
                }

                // Get all users and filter by display name or email
                userRepository.getAllUsersFlow().collect { allUsers ->
                    val filteredUsers = allUsers.filter { user ->
                        user.displayName.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true)
                    }.take(10) // Limit to 10 results

                    _uiState.value = _uiState.value.copy(searchResults = filteredUsers)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to search users: ${e.message}"
                )
            }
        }
    }

    fun showCreateChatDialog() {
        _uiState.value = _uiState.value.copy(showCreateChatDialog = true)
    }

    fun hideCreateChatDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateChatDialog = false,
            searchResults = emptyList()
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

data class ChatListUiState(
    val isLoading: Boolean = true,
    val chatRooms: List<ChatRoom> = emptyList(),
    val currentUser: User? = null,
    val showCreateChatDialog: Boolean = false,
    val isCreatingChat: Boolean = false,
    val searchResults: List<User> = emptyList(),
    val error: String? = null
)