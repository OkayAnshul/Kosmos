package com.example.kosmos.features.chat.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.kosmos.core.models.ChatRoom
import com.example.kosmos.core.models.Project
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ChatRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Chat Hub - Shows all chats across all projects
 *
 * This is a hub screen accessible from bottom navigation that displays
 * all chat rooms the user has access to, organized by project.
 *
 * Architecture:
 * - Groups chats by project for clarity
 * - Shows project name as section header
 * - Each chat shows last message, timestamp, unread count
 * - Flat list sorted by recent activity across all projects
 */

/**
 * ViewModel helper for ChatHubScreenWrapper to inject repositories
 */
class ChatHubDataViewModel @Inject constructor(
    val chatRepository: ChatRepository,
    val projectRepository: ProjectRepository,
    val authRepository: AuthRepository
) : androidx.lifecycle.ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHubScreenWrapper(
    onChatClick: (String) -> Unit,
    onCreateChat: () -> Unit = {},
    chatRepository: ChatRepository = hiltViewModel<ChatHubDataViewModel>().chatRepository,
    projectRepository: ProjectRepository = hiltViewModel<ChatHubDataViewModel>().projectRepository,
    authRepository: AuthRepository = hiltViewModel<ChatHubDataViewModel>().authRepository
) {
    var searchQuery by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Get current user
    val currentUser = authRepository.getCurrentUser()

    // Load all user's projects
    val projects by if (currentUser != null) {
        projectRepository.getUserProjectsFlow(currentUser.id)
            .collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    // Load all chat rooms across all projects
    val allChatRooms = remember { mutableStateListOf<ChatRoom>() }

    projects.forEach { project ->
        val chatRooms by if (currentUser != null) {
            chatRepository.getChatRoomsForProject(currentUser.id, project.id)
                .collectAsStateWithLifecycle(initialValue = emptyList())
        } else {
            remember { mutableStateOf(emptyList()) }
        }

        // Update chat rooms for this project
        LaunchedEffect(chatRooms) {
            // Remove old chats from this project
            allChatRooms.removeAll { room -> room.projectId == project.id }
            // Add updated chats
            allChatRooms.addAll(chatRooms)
        }
    }

    // Create project map for lookup
    val projectMap = projects.associateBy { it.id }

    // Group chats by project
    val chatsByProject = allChatRooms
        .filter { chatRoom ->
            if (searchQuery.isBlank()) true
            else chatRoom.name.contains(searchQuery, ignoreCase = true)
        }
        .groupBy { it.projectId }
        .toList()
        .sortedByDescending { (_, chats) ->
            // Sort projects by most recent chat activity
            chats.maxOfOrNull { it.lastMessageTimestamp } ?: 0L
        }

    // Handle refresh - triggers reload of chat rooms
    val onRefresh: () -> Unit = {
        coroutineScope.launch {
            isRefreshing = true
            // Clear and reload chat rooms by updating the key to force recomposition
            allChatRooms.clear()
            delay(500) // Allow time for Flows to re-emit
            isRefreshing = false
        }
    }

    // Use Box > Column for FAB layering (NO Scaffold - avoids nesting with MainActivity's Scaffold)
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom top bar
            Surface(
                color = ColorTokens.ReactTheme.card,  // #18181D
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chats",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    )
                }
            }

            // Content area with background and pull-to-refresh
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorTokens.ReactTheme.background)  // #0F0F14
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Tokens.Spacing.lg, vertical = Tokens.Spacing.md),
                    placeholder = { Text("Search chats...") },
                    leadingIcon = { Icon(IconSet.Action.search, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorTokens.ReactTheme.card,
                        unfocusedContainerColor = ColorTokens.ReactTheme.card,
                        focusedBorderColor = ColorTokens.ReactTheme.border,
                        unfocusedBorderColor = ColorTokens.ReactTheme.border,
                        focusedTextColor = ColorTokens.ReactTheme.foreground,
                        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                        focusedPlaceholderColor = ColorTokens.ReactTheme.mutedForeground,
                        unfocusedPlaceholderColor = ColorTokens.ReactTheme.mutedForeground,
                        focusedLeadingIconColor = ColorTokens.ReactTheme.mutedForeground,
                        unfocusedLeadingIconColor = ColorTokens.ReactTheme.mutedForeground
                    )
                )

                if (chatsByProject.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
                        ) {
                            Icon(
                                IconSet.Message.chat,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = ColorTokens.ReactTheme.mutedForeground
                            )
                            Text(
                                text = if (searchQuery.isBlank()) "No chats yet" else "No chats found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorTokens.ReactTheme.foreground
                            )
                        }
                    }
                } else {
                    // Chat list grouped by project
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)  // Extra padding to avoid bottom nav + FAB
                    ) {
                        chatsByProject.forEach { (projectId, chats) ->
                            val project = projectMap[projectId]

                            // Project header
                            item(key = "header_$projectId") {
                                Text(
                                    text = project?.name ?: "Unknown Project",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorTokens.ReactTheme.primary,
                                    modifier = Modifier.padding(
                                        horizontal = Tokens.Spacing.lg,
                                        vertical = Tokens.Spacing.md
                                    )
                                )
                            }

                            // Chat rooms in this project
                            items(
                                items = chats.sortedByDescending { it.lastMessageTimestamp },
                                key = { it.id }
                            ) { chatRoom ->
                                // Get unread count for this chat room
                                val unreadCount by chatRepository.getUnreadCountFlow(chatRoom.id, currentUser?.id ?: "")
                                    .collectAsStateWithLifecycle(initialValue = 0)

                                ChatListItem(
                                    chatRoom = chatRoom,
                                    unreadCount = unreadCount,
                                    onClick = { onChatClick(chatRoom.id) }
                                )
                            }

                            // Divider between projects
                            item(key = "divider_$projectId") {
                                Spacer(modifier = Modifier.height(Tokens.Spacing.md))
                            }
                        }
                    }
                }
                }
            }
        }

        // FAB positioned outside Column to avoid being affected by scroll
        if (currentUser != null) {
            FloatingActionButton(
                onClick = onCreateChat,
                containerColor = ColorTokens.ReactTheme.primary,
                contentColor = ColorTokens.ReactTheme.primaryForeground,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 80.dp)  // Clear bottom nav (56dp nav + 24dp margin)
            ) {
                Icon(
                    IconSet.Action.add,
                    contentDescription = "Create Chat",
                    tint = ColorTokens.ReactTheme.primaryForeground
                )
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chatRoom: ChatRoom,
    unreadCount: Int = 0,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Tokens.Spacing.lg,
                vertical = Tokens.Spacing.xs
            ),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = ColorTokens.ReactTheme.border
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Tokens.CornerRadius.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
            ) {
                Text(
                    text = chatRoom.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTokens.ReactTheme.foreground
                )
                if (chatRoom.lastMessage != null) {
                    Text(
                        text = chatRoom.lastMessage!!,
                        fontSize = 14.sp,
                        color = ColorTokens.ReactTheme.mutedForeground,
                        maxLines = 1
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
            ) {
                Text(
                    text = formatTimestamp(chatRoom.lastMessageTimestamp),
                    fontSize = 12.sp,
                    color = if (unreadCount > 0) ColorTokens.ReactTheme.primary else ColorTokens.ReactTheme.mutedForeground
                )
                // Unread badge
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = ColorTokens.ReactTheme.primary,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTokens.ReactTheme.primaryForeground
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
