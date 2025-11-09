package com.example.kosmos.features.chat.presentation.redesign

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.chat.presentation.ChatListViewModel
import com.example.kosmos.features.chat.presentation.ChatOptionsBottomSheet
import com.example.kosmos.features.chat.presentation.CreateChatDialog
import com.example.kosmos.shared.ui.layouts.ListState
import com.example.kosmos.shared.ui.mappers.ChatDataMapper
import com.example.kosmos.shared.ui.mappers.ChatDataMapper.toChatRoomItem
import com.example.kosmos.shared.ui.mappers.StateMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Wrapper composable that connects EnhancedChatListScreen to ChatListViewModel
 * Handles data mapping and state transformations
 */
@Composable
fun EnhancedChatListScreenWrapper(
    projectId: String,
    onChatClick: (String) -> Unit,
    onCreateChat: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // Load chat rooms for this project
    LaunchedEffect(projectId) {
        viewModel.loadChatRooms(projectId)
    }

    // Track selected filter
    var selectedFilter by remember { mutableStateOf(ChatFilter.ALL) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedChatForOptions by remember { mutableStateOf<ChatRoomItem?>(null) }

    // Convert ChatRoom list to ChatRoomItem list
    val chatRoomItems = remember(uiState.chatRooms) {
        uiState.chatRooms.map { chatRoom ->
            chatRoom.toChatRoomItem(
                unreadCount = 0, // TODO: Calculate from messages
                isPinned = chatRoom.isPinned, // Now using real data
                isOnline = false // TODO: Implement online status
            )
        }
    }

    // Filter chat rooms based on selected filter
    val filteredChatRooms = remember(chatRoomItems, selectedFilter) {
        when (selectedFilter) {
            ChatFilter.ALL -> chatRoomItems.filter { !it.isArchived }
            ChatFilter.UNREAD -> chatRoomItems.filter { it.unreadCount > 0 && !it.isArchived }
            ChatFilter.MENTIONS -> chatRoomItems.filter { !it.isArchived } // TODO: Implement mentions
            ChatFilter.ARCHIVED -> chatRoomItems.filter { it.isArchived }
        }
    }

    // Convert to ListState
    val chatRoomsState = remember(uiState.isLoading, filteredChatRooms, uiState.error) {
        StateMapper.toListState(
            isLoading = uiState.isLoading,
            data = filteredChatRooms,
            error = uiState.error
        )
    }

    EnhancedChatListScreen(
        projectId = projectId,
        chatRoomsState = chatRoomsState,
        selectedFilter = selectedFilter,
        onFilterSelected = { filter ->
            selectedFilter = filter
        },
        onChatClick = onChatClick,
        onArchiveChat = { chatRoomId ->
            viewModel.archiveChatRoom(chatRoomId)
        },
        onDeleteChat = { chatRoomId ->
            viewModel.deleteChatRoom(chatRoomId)
        },
        onPinChat = { chatRoomId ->
            // Toggle pin status - find current status from chat room
            val chat = uiState.chatRooms.find { it.id == chatRoomId }
            val currentlyPinned = chat?.isPinned ?: false
            viewModel.pinChatRoom(chatRoomId, isPinned = !currentlyPinned)
        },
        onShowChatOptions = { chatRoomItem ->
            selectedChatForOptions = chatRoomItem
        },
        onCreateChat = {
            viewModel.showCreateChatDialog(projectId)
        },
        onSearchClick = onSearchClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogoutClick = onLogoutClick,
        onRefresh = {
            isRefreshing = true
            viewModel.loadChatRooms(projectId)
            // Reset refreshing state after a delay
            coroutineScope.launch {
                delay(1000)
                isRefreshing = false
            }
        },
        isRefreshing = isRefreshing,
        onBackClick = onBackClick
    )

    // Show CreateChatDialog when triggered
    if (uiState.showCreateChatDialog) {
        CreateChatDialog(
            projectMembers = uiState.projectMembers,
            onDismiss = { viewModel.hideCreateChatDialog() },
            onCreate = { chatName, selectedUserIds ->
                // chatName is null for direct chats (1 person), has value for groups
                val finalName = chatName ?: "Direct Chat"
                viewModel.createNewChatRoom(finalName, "", selectedUserIds, projectId)
            }
        )
    }

    // Show ChatOptionsBottomSheet when a chat's options menu is clicked
    selectedChatForOptions?.let { chat ->
        ChatOptionsBottomSheet(
            chatName = chat.name,
            isPinned = chat.isPinned,
            isArchived = chat.isArchived,
            onPin = {
                viewModel.pinChatRoom(chat.id, isPinned = !chat.isPinned)
            },
            onArchive = {
                viewModel.archiveChatRoom(chat.id)
            },
            onDelete = {
                viewModel.deleteChatRoom(chat.id)
            },
            onDismiss = {
                selectedChatForOptions = null
            }
        )
    }
}
