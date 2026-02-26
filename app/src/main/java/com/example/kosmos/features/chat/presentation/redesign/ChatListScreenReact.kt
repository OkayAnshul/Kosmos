package com.example.kosmos.features.chat.presentation.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Chat List Screen - React Design Implementation
 *
 * Design Reference: documents/Kosmos/src/app/components/chat/ChatListScreen.tsx
 *                   documents/Kosmos/src/app/components/chat/ChatListItem.tsx
 *
 * Features:
 * - Chat list with search and filters
 * - Top app bar with title + search + filter chips
 * - Chat list items with pin indicator, unread badge
 * - Smart sorting (pinned → unread → timestamp)
 * - FAB for creating new chat
 * - Empty state
 *
 * All styling matches React design exactly:
 * - Colors from ColorTokens.ReactTheme.*
 * - Search bar: secondary bg, rounded-xl, focus ring
 * - Filter chips: All/Unread/Mentions with primary selection
 * - Chat items: card with border, shadow, hover effect
 * - Pin icon, unread badge (primary bg, circular)
 * - FAB with shadow glow
 * - NO backend wiring (mock data only)
 */

// Data models
data class ChatListItemData(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val projectName: String
)

enum class FilterType {
    ALL, UNREAD, MENTIONS
}

@Composable
fun ChatListScreenReact(
    chats: List<ChatListItemData> = mockChats,
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    filter: FilterType = FilterType.ALL,
    onFilterChange: (FilterType) -> Unit = {},
    onChatClick: (String) -> Unit = {},
    onCreateChat: () -> Unit = {}
) {
    // Filter and sort chats (matches React logic)
    val filteredChats = remember(searchQuery, filter, chats) {
        chats.filter { chat ->
            val matchesSearch = chat.name.contains(searchQuery, ignoreCase = true) ||
                    chat.lastMessage.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filter) {
                FilterType.ALL -> true
                FilterType.UNREAD -> chat.unreadCount > 0
                FilterType.MENTIONS -> false  // Not implemented in mock
            }
            matchesSearch && matchesFilter
        }
    }

    // Smart sorting: pinned first, then unread, then timestamp
    val sortedChats = remember(filteredChats) {
        filteredChats.sortedWith(compareBy(
            { !it.isPinned },  // Pinned first (inverted)
            { if (it.unreadCount > 0) 0 else 1 },  // Unread first
        ))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                searchQuery = searchQuery,
                onSearchChange = onSearchChange,
                filter = filter,
                onFilterChange = onFilterChange
            )

            // Chat List
            if (sortedChats.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedChats) { chat ->
                        ChatListItem(
                            chat = chat,
                            onClick = { onChatClick(chat.id) }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onCreateChat,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp),
            containerColor = ColorTokens.ReactTheme.primary,
            contentColor = ColorTokens.ReactTheme.primaryForeground,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create chat",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun TopAppBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filter: FilterType,
    onFilterChange: (FilterType) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = ColorTokens.ReactTheme.border,
                shape = RectangleShape
            ),
        color = ColorTokens.ReactTheme.card,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title with React design styling
            Text(
                text = "Chats",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorTokens.ReactTheme.foreground
            )

            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchChange
            )

            // Filter Chips
            FilterChips(
                selectedFilter = filter,
                onFilterChange = onFilterChange
            )
        }
    }

    HorizontalDivider(color = ColorTokens.ReactTheme.border, thickness = 1.dp)
}

// Mock data for testing
private val mockChats = listOf(
    ChatListItemData(
        id = "1",
        name = "Design Discussion",
        lastMessage = "Alice: I think we should use the purple variant for the primary buttons",
        timestamp = "2m ago",
        unreadCount = 3,
        isPinned = true,
        projectName = "Mobile App Redesign"
    ),
    ChatListItemData(
        id = "2",
        name = "Sprint Planning",
        lastMessage = "Bob: Let's schedule the planning meeting for tomorrow at 10 AM",
        timestamp = "1h ago",
        unreadCount = 1,
        isPinned = false,
        projectName = "Customer Portal v2"
    ),
    ChatListItemData(
        id = "3",
        name = "Bug Fixes",
        lastMessage = "Carol: Fixed the login issue on Safari",
        timestamp = "3h ago",
        unreadCount = 0,
        isPinned = false,
        projectName = "Customer Portal v2"
    )
)

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                color = ColorTokens.ReactTheme.secondary,
                shape = RoundedCornerShape(Tokens.CornerRadius.md)
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.size(20.dp)
            )

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 15.sp,
                    color = ColorTokens.ReactTheme.foreground
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search chats...",
                            fontSize = 15.sp,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun FilterChips(
    selectedFilter: FilterType,
    onFilterChange: (FilterType) -> Unit
) {
    val filters = listOf(
        FilterType.ALL to "All",
        FilterType.UNREAD to "Unread",
        FilterType.MENTIONS to "Mentions"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (filterType, label) ->
            FilterChip(
                selected = selectedFilter == filterType,
                onClick = { onFilterChange(filterType) },
                label = {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ColorTokens.ReactTheme.primary,
                    selectedLabelColor = ColorTokens.ReactTheme.primaryForeground,
                    containerColor = ColorTokens.ReactTheme.secondary,
                    labelColor = ColorTokens.ReactTheme.foreground
                ),
                border = null,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
private fun ChatListItem(
    chat: ChatListItemData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        ),
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = Tokens.Elevation.level1)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row: name/pin + timestamp/badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Name + project name
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (chat.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = ColorTokens.ReactTheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = chat.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorTokens.ReactTheme.foreground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = chat.projectName,
                        fontSize = 12.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }

                // Right: Timestamp + unread badge
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = chat.timestamp,
                        fontSize = 12.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 20.dp)
                                .clip(CircleShape)
                                .background(ColorTokens.ReactTheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorTokens.ReactTheme.primaryForeground
                            )
                        }
                    }
                }
            }

            // Last message
            Text(
                text = chat.lastMessage,
                fontSize = 14.sp,
                color = ColorTokens.ReactTheme.mutedForeground,
                lineHeight = 19.6.sp,  // 1.4 line height
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No chats found",
            fontSize = 14.sp,
            color = ColorTokens.ReactTheme.mutedForeground
        )
    }
}
