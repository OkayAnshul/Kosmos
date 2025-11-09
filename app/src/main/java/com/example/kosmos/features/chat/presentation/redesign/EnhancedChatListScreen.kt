package com.example.kosmos.features.chat.presentation.redesign

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.layouts.RefreshableStatefulList
import com.example.kosmos.shared.ui.layouts.ListState
import com.example.kosmos.shared.ui.layouts.SwipeActions

/**
 * Enhanced Chat List Screen - Redesigned
 *
 * Features:
 * - Swipe actions (archive, delete, pin)
 * - Filter chips (All, Unread, Mentions, Archived)
 * - Search integration
 * - Pull-to-refresh
 * - Pinned chats section
 * - Unread badges
 * - Last message preview
 * - Smart timestamps
 * - Online status indicators
 */

/**
 * Chat Filter
 */
enum class ChatFilter {
    ALL, UNREAD, MENTIONS, ARCHIVED
}

/**
 * Enhanced Chat List Screen
 *
 * @param projectId Project ID
 * @param chatRoomsState Chat rooms state
 * @param selectedFilter Current filter
 * @param onFilterSelected Filter selection handler
 * @param onChatClick Chat click handler
 * @param onArchiveChat Archive chat handler
 * @param onDeleteChat Delete chat handler
 * @param onPinChat Pin/unpin chat handler
 * @param onCreateChat Create chat handler
 * @param onSearchClick Search click handler
 * @param onProfileClick Profile click handler
 * @param onSettingsClick Settings click handler
 * @param onLogoutClick Logout handler
 * @param onRefresh Refresh handler
 * @param isRefreshing Whether refreshing
 * @param onBackClick Back navigation handler
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedChatListScreen(
    projectId: String,
    chatRoomsState: ListState<ChatRoomItem>,
    selectedFilter: ChatFilter = ChatFilter.ALL,
    onFilterSelected: (ChatFilter) -> Unit,
    onChatClick: (String) -> Unit,
    onArchiveChat: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onPinChat: (String) -> Unit,
    onShowChatOptions: (ChatRoomItem) -> Unit = {},
    onCreateChat: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean = false,
    onBackClick: () -> Unit
) {
    var showUserMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                navigationIcon = {
                    IconButtonStandard(
                        icon = IconSet.Navigation.back,
                        onClick = onBackClick,
                        contentDescription = "Back"
                    )
                },
                actions = {
                    IconButtonStandard(
                        icon = IconSet.Action.search,
                        onClick = onSearchClick,
                        contentDescription = "Search"
                    )

                    Box {
                        IconButtonStandard(
                            icon = IconSet.User.account,
                            onClick = { showUserMenu = true },
                            contentDescription = "Profile"
                        )

                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Profile") },
                                onClick = {
                                    showUserMenu = false
                                    onProfileClick()
                                },
                                leadingIcon = {
                                    Icon(IconSet.User.profile, null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showUserMenu = false
                                    onSettingsClick()
                                },
                                leadingIcon = {
                                    Icon(IconSet.Settings.settings, null)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = {
                                    showUserMenu = false
                                    onLogoutClick()
                                },
                                leadingIcon = {
                                    Icon(IconSet.User.logout, null)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FABStandard(
                icon = IconSet.Action.add,
                onClick = onCreateChat,
                contentDescription = "Create Chat"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips
            ChatFilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected,
                modifier = Modifier.padding(horizontal = Tokens.Spacing.md)
            )

            // Chat list with pull-to-refresh
            RefreshableStatefulList(
                state = chatRoomsState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                emptyTitle = "No chats yet",
                emptyMessage = "Create a chat to get started",
                emptyActionLabel = "Create Chat",
                onEmptyAction = onCreateChat,
                errorTitle = "Failed to load chats",
                onRetry = onRefresh
            ) { chatRooms ->
                // Separate pinned and unpinned chats
                val pinnedChats = chatRooms.filter { it.isPinned }
                val unpinnedChats = chatRooms.filter { !it.isPinned }

                // Pinned section
                if (pinnedChats.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Pinned",
                            modifier = Modifier.padding(top = Tokens.Spacing.sm)
                        )
                    }
                    items(
                        items = pinnedChats,
                        key = { it.id }
                    ) { chat ->
                        SwipeableChatItem(
                            chat = chat,
                            onClick = { onChatClick(chat.id) },
                            onArchive = { onArchiveChat(chat.id) },
                            onDelete = { onDeleteChat(chat.id) },
                            onPin = { onPinChat(chat.id) },
                            onShowOptions = { onShowChatOptions(chat) }
                        )
                        ListDivider(hasInset = true)
                    }
                }

                // Regular chats section
                if (unpinnedChats.isNotEmpty()) {
                    if (pinnedChats.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "All Chats",
                                modifier = Modifier.padding(top = Tokens.Spacing.sm)
                            )
                        }
                    }
                    items(
                        items = unpinnedChats,
                        key = { it.id }
                    ) { chat ->
                        SwipeableChatItem(
                            chat = chat,
                            onClick = { onChatClick(chat.id) },
                            onArchive = { onArchiveChat(chat.id) },
                            onDelete = { onDeleteChat(chat.id) },
                            onPin = { onPinChat(chat.id) },
                            onShowOptions = { onShowChatOptions(chat) }
                        )
                        ListDivider(hasInset = true)
                    }
                }
            }
        }
    }
}

/**
 * Chat Filter Chips
 */
@Composable
private fun ChatFilterChips(
    selectedFilter: ChatFilter,
    onFilterSelected: (ChatFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    ChipGroup(
        chips = ChatFilter.values().map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
        selectedChips = setOf(selectedFilter.name.lowercase().replaceFirstChar { c -> c.uppercase() }),
        onChipClick = { filterName ->
            val filter = ChatFilter.values().find {
                it.name.lowercase().replaceFirstChar { c -> c.uppercase() } == filterName
            }
            filter?.let { onFilterSelected(it) }
        },
        modifier = modifier,
        multiSelect = false
    )
}

/**
 * Swipeable Chat Item
 */
@Composable
private fun SwipeableChatItem(
    chat: ChatRoomItem,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onShowOptions: () -> Unit = {}
) {
    SwipeActions(
        onSwipeLeft = onDelete,
        onSwipeRight = if (chat.isPinned) onPin else onArchive,
        leftIcon = IconSet.Action.delete,
        leftLabel = "Delete",
        leftColor = ColorTokens.Error.light,
        rightIcon = if (chat.isPinned) IconSet.Message.unarchive else IconSet.Message.archive,
        rightLabel = if (chat.isPinned) "Unpin" else if (chat.isArchived) "Unarchive" else "Archive",
        rightColor = ColorTokens.Primary.light
    ) {
        ChatListItem(
            chat = chat,
            onClick = onClick,
            onPin = onPin,
            onShowOptions = onShowOptions
        )
    }
}

/**
 * Chat List Item
 */
@Composable
private fun ChatListItem(
    chat: ChatRoomItem,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onShowOptions: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (chat.hasUnread)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Avatar with online indicator
            Box {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(Tokens.Size.avatarMedium)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = chat.name.take(2).uppercase(),
                            style = TypographyTokens.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Online indicator
                if (chat.isOnline) {
                    Surface(
                        modifier = Modifier
                            .size(Tokens.Size.statusDotWithBorder)
                            .align(Alignment.BottomEnd),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = ColorTokens.Status.online
                            ) {}
                        }
                    }
                }
            }

            // Chat info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
            ) {
                // Name and timestamp row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pin indicator
                        if (chat.isPinned) {
                            Icon(
                                imageVector = IconSet.Message.pin,
                                contentDescription = "Pinned",
                                modifier = Modifier.size(Tokens.Size.iconSmall),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = chat.name,
                            style = TypographyTokens.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = chat.formattedTimestamp,
                        style = TypographyTokens.Custom.caption,
                        color = if (chat.hasUnread)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Last message row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        style = TypographyTokens.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (chat.hasUnread)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    // Unread badge
                    if (chat.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                style = TypographyTokens.Custom.badgeNumber
                            )
                        }
                    }
                }
            }

            // Options menu icon
            IconButtonStandard(
                icon = IconSet.Action.moreVert,
                onClick = { onShowOptions() },
                contentDescription = "Chat options"
            )
        }
    }
}

/**
 * Chat Room Item Data Class
 */
data class ChatRoomItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timestamp: Long,
    val formattedTimestamp: String,
    val unreadCount: Int = 0,
    val hasUnread: Boolean = unreadCount > 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isOnline: Boolean = false,
    val participantCount: Int = 0
)
