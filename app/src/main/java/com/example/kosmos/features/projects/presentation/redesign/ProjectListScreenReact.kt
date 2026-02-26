package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.shared.ui.components.KosmosCard
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Project List Screen - React Design Implementation
 *
 * Design Reference: documents/Kosmos/src/app/components/ProjectListScreen.tsx
 * DESIGN-ONLY: Uses mock data, NO backend wiring
 *
 * Layout matches React exactly:
 * - Top app bar: "Projects" + notification bell
 * - Search bar: rounded-xl, secondary bg, search icon left
 * - Filter chips: All | Active | Archived (horizontally scrollable)
 * - Project cards: space-y-3 (12dp gap)
 * - Empty state: centered icon + text
 *
 * Colors from theme.css:
 * - Background: --background (#0F0F14)
 * - Card: --card (#18181D)
 * - Secondary: --secondary (#1F1F27)
 * - Primary: --primary (#7C3AED)
 * - Border: --border (#2A2A32)
 */

// Mock data from React component (ProjectListScreen.tsx line 5-61)
data class ProjectCardData(
    val id: String,
    val name: String,
    val description: String,
    val status: String, // "Active" or "Archived"
    val memberCount: Int,
    val chatCount: Int,
    val taskCount: Int,
    val completedTasks: Int,
    val lastActivity: String
)

private val mockProjects = listOf(
    ProjectCardData(
        id = "1",
        name = "Mobile App Redesign",
        description = "Complete redesign of the mobile application with new branding and improved user experience",
        status = "Active",
        memberCount = 8,
        chatCount = 12,
        taskCount = 24,
        completedTasks = 16,
        lastActivity = "2 hours ago"
    ),
    ProjectCardData(
        id = "2",
        name = "Marketing Campaign Q1",
        description = "Launch the new product marketing campaign across all channels",
        status = "Active",
        memberCount = 5,
        chatCount = 8,
        taskCount = 18,
        completedTasks = 12,
        lastActivity = "3 hours ago"
    ),
    ProjectCardData(
        id = "3",
        name = "Website Performance",
        description = "Optimize website loading times and improve Core Web Vitals scores",
        status = "Active",
        memberCount = 4,
        chatCount = 5,
        taskCount = 15,
        completedTasks = 15,
        lastActivity = "1 day ago"
    ),
    ProjectCardData(
        id = "4",
        name = "Customer Portal v2",
        description = "Build the next generation customer self-service portal",
        status = "Active",
        memberCount = 10,
        chatCount = 15,
        taskCount = 32,
        completedTasks = 8,
        lastActivity = "5 hours ago"
    ),
    ProjectCardData(
        id = "5",
        name = "API Documentation",
        description = "Create comprehensive API documentation for external developers",
        status = "Archived",
        memberCount = 3,
        chatCount = 4,
        taskCount = 10,
        completedTasks = 10,
        lastActivity = "2 weeks ago"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreenReact(
    projects: List<ProjectCardData> = mockProjects, // Accept projects as parameter, default to mock for testing
    onProjectClick: (String) -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onCreateProject: () -> Unit = {},
    onProjectEdit: (String) -> Unit = {},
    onProjectMembers: (String) -> Unit = {},
    onProjectSettings: (String) -> Unit = {},
    onProjectArchive: (String) -> Unit = {},
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    activeFilter: String = "All",
    onFilterChange: (String) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    notificationBadgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    // Filter logic from React (line 71-76)
    val filteredProjects = projects.filter { project ->
        val matchesSearch = project.name.contains(searchQuery, ignoreCase = true) ||
                project.description.contains(searchQuery, ignoreCase = true)
        val matchesFilter = activeFilter == "All" || project.status == activeFilter
        matchesSearch && matchesFilter
    }

    // Use Box to overlay FAB on content
    Box(modifier = modifier.fillMaxSize()) {
    // Use Column with custom top bar (NO Scaffold - avoids nesting with MainActivity's Scaffold)
    Column(modifier = Modifier.fillMaxSize()) {
        // Custom top bar
        Surface(
            color = ColorTokens.ReactTheme.card,  // --card: #18181D
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),  // px-4 py-3
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Projects",
                    fontSize = 20.sp,  // 1.25rem
                    fontWeight = FontWeight.SemiBold,  // 600
                    color = ColorTokens.ReactTheme.foreground
                )
                Row {
                    BadgedBox(
                        badge = {
                            if (notificationBadgeCount > 0) {
                                Badge {
                                    Text(if (notificationBadgeCount > 99) "99+" else notificationBadgeCount.toString())
                                }
                            }
                        }
                    ) {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = ColorTokens.ReactTheme.foreground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    } // end BadgedBox
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = ColorTokens.ReactTheme.foreground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Content area with pull-to-refresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .background(ColorTokens.ReactTheme.background)  // --background: #0F0F14
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)  // px-4 py-4
            ) {
                // Search Bar (line 93-108)
                SearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)  // mb-4
                )

                // Filter Chips (line 111-126)
                FilterChips(
                    activeFilter = activeFilter,
                    onFilterChange = onFilterChange,
                    modifier = Modifier.padding(bottom = 16.dp)  // mb-4
                )

                // Project List or Empty State (line 129-154)
                if (filteredProjects.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),  // space-y-3
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProjects) { project ->
                            ProjectCard(
                                project = project,
                                onClick = { onProjectClick(project.id) },
                                onEdit = { onProjectEdit(project.id) },
                                onMembers = { onProjectMembers(project.id) },
                                onSettings = { onProjectSettings(project.id) },
                                onArchive = { onProjectArchive(project.id) }
                            )
                        }
                    }
                } else {
                    EmptyState(hasSearchQuery = searchQuery.isNotEmpty())
                }
            }
        }
    }

    // FAB for creating new project
    FloatingActionButton(
        onClick = onCreateProject,
        containerColor = ColorTokens.ReactTheme.primary,
        contentColor = ColorTokens.ReactTheme.primaryForeground,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Create Project"
        )
    }
    } // Close outer Box
}

// Search Bar Component (line 94-107)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = ColorTokens.ReactTheme.secondary,  // --secondary: #1F1F27
                shape = RoundedCornerShape(Tokens.CornerRadius.md)  // rounded-xl: 12dp
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)  // pl-10 pr-4 py-3
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = ColorTokens.ReactTheme.mutedForeground,  // --muted-foreground
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    color = ColorTokens.ReactTheme.foreground,
                    fontSize = 15.sp  // 0.9375rem
                ),
                cursorBrush = SolidColor(ColorTokens.ReactTheme.primary),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search projects...",
                            color = ColorTokens.ReactTheme.mutedForeground,
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

// Filter Chips Component (line 111-126)
@Composable
private fun FilterChips(
    activeFilter: String,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),  // gap-2
        modifier = modifier.fillMaxWidth()
    ) {
        listOf("All", "Active", "Archived").forEach { filter ->
            FilterChip(
                selected = activeFilter == filter,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(
                        text = filter,
                        fontSize = 14.sp,  // 0.875rem
                        fontWeight = FontWeight.Medium  // 500
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ColorTokens.ReactTheme.primary,  // bg-primary
                    selectedLabelColor = ColorTokens.ReactTheme.primaryForeground,  // text-primary-foreground
                    containerColor = ColorTokens.ReactTheme.secondary,  // bg-secondary
                    labelColor = ColorTokens.ReactTheme.foreground  // text-foreground
                ),
                shape = RoundedCornerShape(8.dp)  // rounded-lg
            )
        }
    }
}

// Project Card Component (from ProjectCard.tsx line 24-93)
@Composable
private fun ProjectCard(
    project: ProjectCardData,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onMembers: () -> Unit = {},
    onSettings: () -> Unit = {},
    onArchive: () -> Unit = {}
) {
    KosmosCard(
        onClick = onClick
    ) {
        Column {
            // Header (line 31-51)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),  // mb-2
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,  // 600
                        color = ColorTokens.ReactTheme.foreground,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 4.dp)  // mb-1
                    )
                    Text(
                        text = project.description,
                        fontSize = 14.sp,  // 0.875rem
                        lineHeight = 19.6.sp,  // lineHeight: 1.4
                        color = ColorTokens.ReactTheme.mutedForeground,
                        maxLines = 2
                    )
                }

                // More menu
                var showProjectMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showProjectMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = ColorTokens.ReactTheme.mutedForeground,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showProjectMenu,
                        onDismissRequest = { showProjectMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = {
                                onClick()
                                showProjectMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Visibility, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Project") },
                            onClick = {
                                onEdit()
                                showProjectMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Members") },
                            onClick = {
                                onMembers()
                                showProjectMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.People, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                onSettings()
                                showProjectMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (project.status == "Archived") "Unarchive" else "Archive",
                                    color = ColorTokens.ReactTheme.mutedForeground
                                )
                            },
                            onClick = {
                                onArchive()
                                showProjectMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (project.status == "Archived")
                                        Icons.Default.Unarchive
                                    else
                                        Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = ColorTokens.ReactTheme.mutedForeground
                                )
                            }
                        )
                    }
                }
            }

            // Status Badge (line 54-56)
            StatusBadge(
                status = project.status,
                modifier = Modifier.padding(bottom = 12.dp)  // mb-3
            )

            // Stats Row (line 59-78)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),  // gap-4
                modifier = Modifier.padding(bottom = 12.dp)  // mb-3
            ) {
                StatItem(
                    icon = Icons.Default.Person,
                    count = project.memberCount
                )
                StatItem(
                    icon = Icons.Default.Chat,
                    count = project.chatCount
                )
                StatItem(
                    icon = Icons.Default.CheckBox,
                    count = project.taskCount
                )
            }

            // Progress Bar (line 81-83)
            ProgressBar(
                value = project.completedTasks,
                max = project.taskCount,
                modifier = Modifier.padding(bottom = 12.dp)  // mb-3
            )

            // Footer (line 86-91)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemberAvatars(count = project.memberCount)
                Text(
                    text = project.lastActivity,
                    fontSize = 12.sp,  // 0.75rem
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }
        }
    }
}

// Status Badge (from StatusBadge.tsx)
@Composable
private fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        "Active" -> ColorTokens.Success.light to ColorTokens.Success.onLight
        "Archived" -> ColorTokens.TaskStatus.cancelled to ColorTokens.TaskStatus.onCancelled
        else -> ColorTokens.TaskStatus.todo to ColorTokens.TaskStatus.onTodo
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),  // rounded-full style
        modifier = modifier
    ) {
        Text(
            text = status,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// Stat Item
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),  // gap-1.5
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = count.toString(),
            fontSize = 14.sp,  // 0.875rem
            fontWeight = FontWeight.Medium,  // 500
            color = ColorTokens.ReactTheme.foreground
        )
    }
}

// Progress Bar (from ProgressBar.tsx)
@Composable
private fun ProgressBar(
    value: Int,
    max: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (max > 0) value.toFloat() / max.toFloat() else 0f

    Column(modifier = modifier) {
        Text(
            text = "$value/$max completed",
            fontSize = 12.sp,
            color = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(9999.dp)),  // rounded-full
            color = ColorTokens.ReactTheme.primary,
            trackColor = ColorTokens.ReactTheme.muted
        )
    }
}

// Member Avatars (placeholder - from MemberAvatars.tsx)
@Composable
private fun MemberAvatars(
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),  // Overlapping
        modifier = modifier
    ) {
        // Show max 3 avatars
        repeat(minOf(count, 3)) { index ->
            Box(
                modifier = Modifier
                    .size(24.dp)  // sm size
                    .clip(CircleShape)
                    .background(ColorTokens.ReactTheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (index + 1).toString(),
                    fontSize = 10.sp,
                    color = ColorTokens.ReactTheme.primaryForeground
                )
            }
        }
        if (count > 3) {
            Text(
                text = "+${count - 3}",
                fontSize = 12.sp,
                color = ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// Empty State (line 141-154)
@Composable
private fun EmptyState(
    hasSearchQuery: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 64.dp),  // py-16
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(64.dp)  // w-16 h-16
                .clip(CircleShape)
                .background(ColorTokens.ReactTheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))  // mb-4

        Text(
            text = "No projects found",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,  // 600
            color = ColorTokens.ReactTheme.foreground,
            modifier = Modifier.padding(bottom = 8.dp)  // mb-2
        )

        Text(
            text = if (hasSearchQuery)
                "Try adjusting your search or filters"
            else
                "Create your first project to get started",
            fontSize = 14.sp,  // 0.875rem
            color = ColorTokens.ReactTheme.mutedForeground
        )
    }
}
