package com.example.kosmos.features.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ChatRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.core.models.User
import com.example.kosmos.core.models.ChatRoom
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.kosmos.shared.utils.ErrorMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val projectRepository: com.example.kosmos.data.repository.ProjectRepository
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
                    if (e is CancellationException) throw e
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
                    if (e is CancellationException) throw e
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
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isCreatingChat = true, error = null)

                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isCreatingChat = false,
                        error = "You must be logged in to create chats"
                    )
                    return@launch
                }

                val firebaseUser = currentUser
                val participantIds = (selectedUserIds + firebaseUser.id).distinct()
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
                            showCreateChatDialog = false,
                            lastCreatedChatRoomId = chatRoomId,
                            error = null
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
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isCreatingChat = false,
                    error = "Failed to create chat room: ${e.message}"
                )
            }
        }
    }

    /**
     * Load project members for chat creation
     * Fetches FRESH data directly from Supabase (no stale cache)
     * This ensures banned/deleted users don't appear and new users show up immediately
     * @param projectId Project ID to load members from
     */
    fun loadProjectMembers(projectId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingMembers = true, error = null)

                // Get project members (just the membership records)
                projectRepository.getProjectMembersFlow(projectId).collect { members ->
                    if (members.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            projectMembers = emptyList(),
                            isLoadingMembers = false
                        )
                        return@collect
                    }

                    // Load FRESH user details from Supabase in parallel (FIXED: much faster)
                    val users = members.map { member ->
                        async {
                            try {
                                val result = userRepository.getUserByIdFromSupabase(member.userId)
                                result.getOrNull()
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                android.util.Log.w(
                                    "ChatListViewModel",
                                    "Failed to load user ${member.userId}",
                                    e
                                )
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()

                    if (users.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            projectMembers = emptyList(),
                            isLoadingMembers = false,
                            error = "Failed to load project members. Please try again."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            projectMembers = users,
                            isLoadingMembers = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoadingMembers = false,
                    error = "Failed to load project members: ${e.message}"
                )
            }
        }
    }

    fun showCreateChatDialog(projectId: String) {
        _uiState.value = _uiState.value.copy(showCreateChatDialog = true)
        loadProjectMembers(projectId)
    }

    fun hideCreateChatDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateChatDialog = false,
            projectMembers = emptyList()
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearLastCreatedChatRoom() {
        _uiState.value = _uiState.value.copy(lastCreatedChatRoomId = null)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    /**
     * Archive a chat room
     * @param chatRoomId Chat room ID to archive
     */
    fun archiveChatRoom(chatRoomId: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.archiveChatRoom(chatRoomId, isArchived = true)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to archive chat: ${result.exceptionOrNull()?.message}"
                    )
                }
                // Room Flow will automatically update the chat list
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    error = "Error archiving chat: ${e.message}"
                )
            }
        }
    }

    /**
     * Unarchive a chat room
     * @param chatRoomId Chat room ID to unarchive
     */
    fun unarchiveChatRoom(chatRoomId: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.archiveChatRoom(chatRoomId, isArchived = false)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to unarchive chat: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    error = "Error unarchiving chat: ${e.message}"
                )
            }
        }
    }

    /**
     * Delete a chat room permanently
     * @param chatRoomId Chat room ID to delete
     */
    fun deleteChatRoom(chatRoomId: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.deleteChatRoom(chatRoomId)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete chat: ${result.exceptionOrNull()?.message}"
                    )
                }
                // Room Flow will automatically update the chat list
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    error = "Error deleting chat: ${e.message}"
                )
            }
        }
    }

    /**
     * Pin or unpin a chat room
     * @param chatRoomId Chat room ID to toggle pin status
     * @param isPinned Whether to pin (true) or unpin (false)
     */
    fun pinChatRoom(chatRoomId: String, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                val result = chatRepository.pinChatRoom(chatRoomId, isPinned)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to ${if (isPinned) "pin" else "unpin"} chat: ${result.exceptionOrNull()?.message}"
                    )
                }
                // Room Flow will automatically update the chat list
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    error = "Error ${if (isPinned) "pinning" else "unpinning"} chat: ${e.message}"
                )
            }
        }
    }
}

data class ChatListUiState(
    val isLoading: Boolean = true,
    val chatRooms: List<ChatRoom> = emptyList(),
    val currentUser: User? = null,
    val showCreateChatDialog: Boolean = false,
    val isCreatingChat: Boolean = false,
    val projectMembers: List<User> = emptyList(),
    val isLoadingMembers: Boolean = false,
    val lastCreatedChatRoomId: String? = null,
    val error: String? = null
)