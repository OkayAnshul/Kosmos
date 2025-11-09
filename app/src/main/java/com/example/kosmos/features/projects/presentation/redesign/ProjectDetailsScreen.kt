package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.features.projects.components.*
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Project Details Screen
 *
 * Features:
 * - Project overview with stats
 * - Quick access to chats and tasks
 * - Member management
 * - Activity feed
 * - Project settings
 *
 * Power user features:
 * - Quick navigation to sections
 * - Inline task/chat creation
 * - Member quick actions
 */

/**
 * Project Tab
 */
enum class ProjectTab {
    OVERVIEW, CHATS, TASKS, MEMBERS, ACTIVITY
}

/**
 * Project Details Screen
 *
 * @param project Project details
 * @param members Project members
 * @param recentActivity Recent activity
 * @param selectedTab Selected tab
 * @param onTabSelected Tab selection handler
 * @param onViewAllChats View all chats handler
 * @param onViewAllTasks View all tasks handler
 * @param onViewAllMembers View all members handler
 * @param onCreateChat Create chat handler
 * @param onCreateTask Create task handler
 * @param onInviteMember Invite member handler
 * @param onEditProject Edit project handler
 * @param onArchiveProject Archive project handler
 * @param onBackClick Back navigation handler
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    project: ProjectItem,
    members: List<ProjectMember>,
    recentActivity: List<ProjectActivity>,
    chatRooms: List<com.example.kosmos.features.chat.presentation.redesign.ChatRoomItem> = emptyList(),
    tasks: List<com.example.kosmos.features.tasks.components.TaskItem> = emptyList(),
    selectedTab: ProjectTab,
    onTabSelected: (ProjectTab) -> Unit,
    onViewAllChats: () -> Unit,
    onViewAllTasks: () -> Unit,
    onViewAllMembers: () -> Unit,
    onCreateChat: () -> Unit,
    onCreateTask: () -> Unit,
    onInviteMember: () -> Unit,
    onEditProject: () -> Unit,
    onArchiveProject: () -> Unit,
    onChatClick: (String) -> Unit = {},
    onTaskClick: (String) -> Unit = {},
    onBackClick: () -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project.name) },
                navigationIcon = {
                    IconButtonStandard(
                        icon = IconSet.Navigation.back,
                        onClick = onBackClick,
                        contentDescription = "Back"
                    )
                },
                actions = {
                    IconButtonStandard(
                        icon = IconSet.Action.edit,
                        onClick = onEditProject,
                        contentDescription = "Edit"
                    )

                    Box {
                        IconButtonStandard(
                            icon = IconSet.Action.moreVert,
                            onClick = { showMoreMenu = true },
                            contentDescription = "More"
                        )

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Project Settings") },
                                onClick = {
                                    showMoreMenu = false
                                    // Navigate to settings
                                },
                                leadingIcon = {
                                    Icon(IconSet.Settings.settings, null)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (project.isArchived) "Unarchive" else "Archive",
                                        color = ColorTokens.Error.light
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onArchiveProject()
                                },
                                leadingIcon = {
                                    Icon(
                                        IconSet.Message.archive,
                                        null,
                                        tint = ColorTokens.Error.light
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Tab content
            Box(modifier = Modifier
                .weight(1f)
                .padding(padding)) {
                when (selectedTab) {
                    ProjectTab.OVERVIEW -> {
                        OverviewTab(
                            project = project,
                            members = members,
                            onViewAllChats = onViewAllChats,
                            onViewAllTasks = onViewAllTasks,
                            onViewAllMembers = onViewAllMembers,
                            onCreateChat = onCreateChat,
                            onCreateTask = onCreateTask
                        )
                    }
                    ProjectTab.CHATS -> {
                        ChatsTab(
                            chatRooms = chatRooms,
                            onChatClick = onChatClick,
                            onCreateChat = onCreateChat
                        )
                    }
                    ProjectTab.TASKS -> {
                        TasksTab(
                            tasks = tasks,
                            onTaskClick = onTaskClick,
                            onCreateTask = onCreateTask
                        )
                    }
                    ProjectTab.MEMBERS -> {
                        MembersTab(
                            members = members,
                            onInviteMember = onInviteMember
                        )
                    }
                    ProjectTab.ACTIVITY -> {
                        ActivityTab(
                            activities = recentActivity
                        )
                    }
                }
            }

            // Custom Animated Bottom Navigation
            AnimatedBottomBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }
    }
}

/**
 * Overview Tab
 */
@Composable
private fun OverviewTab(
    project: ProjectItem,
    members: List<ProjectMember>,
    onViewAllChats: () -> Unit,
    onViewAllTasks: () -> Unit,
    onViewAllMembers: () -> Unit,
    onCreateChat: () -> Unit,
    onCreateTask: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Tokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
    ) {
        // Description
        if (!project.description.isNullOrBlank()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Tokens.Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                    ) {
                        Text(
                            text = "About",
                            style = TypographyTokens.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = project.description,
                            style = TypographyTokens.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Stats cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                // Chats card
                StatCard(
                    icon = IconSet.Navigation.chats,
                    value = project.chatCount.toString(),
                    label = "Chats",
                    onClick = onViewAllChats,
                    hasBadge = project.unreadChatCount > 0,
                    badgeValue = project.unreadChatCount,
                    modifier = Modifier.weight(1f)
                )

                // Tasks card
                StatCard(
                    icon = IconSet.Navigation.tasks,
                    value = project.taskCount.toString(),
                    label = "Tasks",
                    onClick = onViewAllTasks,
                    hasBadge = project.pendingTaskCount > 0,
                    badgeValue = project.pendingTaskCount,
                    modifier = Modifier.weight(1f)
                )

                // Members card
                StatCard(
                    icon = IconSet.User.profile,
                    value = project.memberCount.toString(),
                    label = "Members",
                    onClick = onViewAllMembers,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick actions
        item {
            Text(
                text = "Quick Actions",
                style = TypographyTokens.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                SecondaryButton(
                    text = "New Chat",
                    icon = IconSet.Navigation.chats,
                    onClick = onCreateChat,
                    modifier = Modifier.weight(1f)
                )

                SecondaryButton(
                    text = "New Task",
                    icon = IconSet.Navigation.tasks,
                    onClick = onCreateTask,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Members preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Team Members",
                    style = TypographyTokens.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(onClick = onViewAllMembers) {
                    Text("View All")
                }
            }
        }

        item {
            ProjectMemberAvatars(
                members = members,
                maxVisible = 5
            )
        }
    }
}

/**
 * Stat Card
 */
@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasBadge: Boolean = false,
    badgeValue: Int = 0
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Tokens.Size.iconLarge)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = TypographyTokens.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (hasBadge && badgeValue > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = if (badgeValue > 99) "99+" else badgeValue.toString(),
                            style = TypographyTokens.Custom.badgeNumber
                        )
                    }
                }
            }

            Text(
                text = label,
                style = TypographyTokens.Custom.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Chats Tab
 */
@Composable
private fun ChatsTab(
    chatRooms: List<com.example.kosmos.features.chat.presentation.redesign.ChatRoomItem>,
    onChatClick: (String) -> Unit,
    onCreateChat: () -> Unit
) {
    if (chatRooms.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                title = "No Chats Yet",
                message = "Create a chat room to start collaborating",
                actionLabel = "Create Chat",
                onActionClick = onCreateChat
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            item {
                PrimaryButton(
                    text = "Create Chat",
                    onClick = onCreateChat,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(chatRooms.size) { index ->
                val chat = chatRooms[index]
                StandardCard(
                    onClick = { onChatClick(chat.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Tokens.Spacing.md)
                    ) {
                        Text(
                            text = chat.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = chat.lastMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (chat.unreadCount > 0) {
                            Text(
                                text = "${chat.unreadCount} unread messages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tasks Tab
 */
@Composable
private fun TasksTab(
    tasks: List<com.example.kosmos.features.tasks.components.TaskItem>,
    onTaskClick: (String) -> Unit,
    onCreateTask: () -> Unit
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                title = "No Tasks Yet",
                message = "Create a task to start tracking work",
                actionLabel = "Create Task",
                onActionClick = onCreateTask
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            item {
                PrimaryButton(
                    text = "Create Task",
                    onClick = onCreateTask,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(tasks.size) { index ->
                val task = tasks[index]
                StandardCard(
                    onClick = { onTaskClick(task.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Tokens.Spacing.md)
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        task.description?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                        ) {
                            Text(
                                text = task.status.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = task.priority.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (task.priority) {
                                    com.example.kosmos.features.tasks.components.TaskPriority.HIGH -> MaterialTheme.colorScheme.error
                                    com.example.kosmos.features.tasks.components.TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Members Tab
 */
@Composable
private fun MembersTab(
    members: List<ProjectMember>,
    onInviteMember: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Tokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        item {
            PrimaryButton(
                text = "Invite Member",
                icon = IconSet.User.personAdd,
                onClick = onInviteMember,
                fullWidth = true
            )
        }

        items(members) { member ->
            MemberListItem(member = member)
        }
    }
}

/**
 * Member List Item
 */
@Composable
private fun MemberListItem(
    member: ProjectMember,
    modifier: Modifier = Modifier
) {
    ListItemTwoLine(
        primaryText = member.name,
        secondaryText = member.role ?: "Member",
        leadingIcon = IconSet.User.profile,
        onClick = { /* View member profile */ },
        modifier = modifier
    )
}

/**
 * Activity Tab
 */
@Composable
private fun ActivityTab(
    activities: List<ProjectActivity>
) {
    if (activities.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                icon = IconSet.Time.history,
                title = "No Activity Yet",
                message = "Project activity will appear here.\nComing soon: member actions, task updates, and chat activity."
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(activities) { activity ->
                ProjectActivityItem(activity = activity)
                ListDivider(hasInset = true)
            }
        }
    }
}

/**
 * Custom Animated Bottom Navigation Bar
 * Inspired by AndroidAnimatedNavigationBar by Exyte
 */
@Composable
private fun AnimatedBottomBar(
    selectedTab: ProjectTab,
    onTabSelected: (ProjectTab) -> Unit
) {
    val tabs = ProjectTab.values()
    val selectedIndex = selectedTab.ordinal

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = Tokens.Spacing.md),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                val animatedScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "scale_${tab.name}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            onClick = { onTabSelected(tab) },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Background circle for selected tab
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                        )
                    }

                    // Icon
                    Icon(
                        imageVector = when (tab) {
                            ProjectTab.OVERVIEW -> IconSet.Navigation.home
                            ProjectTab.CHATS -> IconSet.Message.chat
                            ProjectTab.TASKS -> IconSet.Task.task
                            ProjectTab.MEMBERS -> IconSet.User.group
                            ProjectTab.ACTIVITY -> IconSet.Time.history
                        },
                        contentDescription = tab.name,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size((24.dp.value * animatedScale).dp)
                    )
                }
            }
        }
    }
}
