package com.example.kosmos.features.projects.presentation.redesign

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.core.models.Permission
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.shared.ui.components.KosmosCard
import com.example.kosmos.shared.ui.components.PermissionGated
import com.example.kosmos.shared.ui.components.SectionCard
import com.example.kosmos.shared.ui.components.StatusBadge
import com.example.kosmos.shared.ui.components.BadgeSize
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Project Details Screen - React Design Implementation (Overview Tab)
 *
 * Design Reference: documents/Kosmos/src/app/components/ProjectDetailsScreen.tsx
 * DESIGN-ONLY: Uses mock data, NO backend wiring
 *
 * NOTE: Tab navigation is handled by parent ProjectWorkspaceScreen.
 * This component shows ONLY the Overview content.
 *
 * Features:
 * - Hero card: project name, status badge, description, category/visibility tags
 * - Stats row: 3 equal-width clickable cards (Members / Tasks / Chats)
 * - Quick actions row: two OutlinedButtons (+ New Task / + New Chat)
 * - About card: deadline, GitHub URL, website URL, tech stack (shown only if any detail present)
 * - Recent activity card: up to 5 items + "View All Activity →" text button
 *
 * Redesign (2026-02-27):
 * - Hero card replaces the plain "About" card header
 * - Stats row is now horizontal (3 equal cards) instead of single-column
 * - Quick actions use OutlinedButton instead of filled + outlined mix
 * - About section uses SectionCard("ABOUT")
 * - Activity section uses SectionCard("RECENT ACTIVITY")
 * - TopAppBar: back arrow, project name title, MoreVert overflow menu
 *
 * Colors from theme.css:
 * - Background: --background (#0F0F14)
 * - Card: --card (#18181D)
 * - Primary: --primary (#7C3AED)
 * - Border: --border (#2A2A32)
 */

enum class ProjectTabReact {
    OVERVIEW, CHATS, TASKS, MEMBERS, ACTIVITY
}

// Mock data (from React line 18-28)
// Public so wrapper can access it
data class ProjectData(
    val id: String,
    val name: String,
    val description: String,
    val status: String,
    val memberCount: Int,
    val chatCount: Int,
    val taskCount: Int,
    val completedTasks: Int,
    val lastActivity: String,
    // Extended fields for About section
    val category: String? = null,
    val deadline: String? = null,
    val visibility: String? = null,
    val createdAt: String? = null,
    val githubUrl: String? = null,
    val websiteUrl: String? = null,
    val projectMotive: String? = null,
    val techStack: String? = null,
    val businessModel: String? = null,
    val targetAudience: String? = null,
    val tags: List<String> = emptyList(),
    val openSourceLicense: String? = null,
    val industryTags: List<String> = emptyList()  // BUSINESS category: parsed from JSON array string
)

private val mockProject = ProjectData(
    id = "1",
    name = "Mobile App Redesign",
    description = "Complete redesign of the mobile application with new branding guidelines, improved user experience, and modern UI components. This project aims to increase user engagement and satisfaction.",
    status = "Active",
    memberCount = 8,
    chatCount = 12,
    taskCount = 24,
    completedTasks = 16,
    lastActivity = "2 hours ago",
    category = "Design",
    visibility = "Team",
    deadline = "March 31, 2026",
    githubUrl = "github.com/example/app-redesign",
    techStack = "Kotlin, Compose, Figma"
)

// Recent activity mock data (line 149-152)
// Public so wrapper can access it
data class ActivityItem(
    val user: String,
    val action: String,
    val time: String
)

private val mockActivities = listOf(
    ActivityItem("Alice", "completed task \"Design mockups\"", "2 hours ago"),
    ActivityItem("Bob", "added 3 new tasks", "3 hours ago"),
    ActivityItem("Carol", "started a new chat", "5 hours ago")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreenReact(
    projectId: String = "1",
    project: ProjectData = mockProject, // Accept project data as parameter, default to mock
    activities: List<ActivityItem> = mockActivities, // Accept activities, default to mock
    currentMember: ProjectMember? = null,
    onBack: () -> Unit = {},
    onNewTask: () -> Unit = {},
    onNewChat: () -> Unit = {},
    onViewChats: () -> Unit = {},  // Navigate to Chats tab
    onViewTasks: () -> Unit = {},  // Navigate to Tasks tab
    onViewMembers: () -> Unit = {},  // Navigate to Members tab
    onViewActivity: () -> Unit = {},  // Navigate to Activity tab
    onMoreMenu: () -> Unit = {},
    onArchive: () -> Unit = {},
    onDelete: () -> Unit = {},
    onChangeVisibility: (String) -> Unit = {}
) {
    // Visibility picker dialog state
    var showVisibilityPicker by remember { mutableStateOf(false) }
    // Overflow menu state
    var showOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ColorTokens.ReactTheme.background,
        topBar = {
            ProjectDetailsTopBar(
                projectName = project.name,
                currentMember = currentMember,
                onBackClick = onBack,
                onMenuClick = { showOverflowMenu = true },
                showOverflowMenu = showOverflowMenu,
                onDismissMenu = { showOverflowMenu = false },
                onEditClick = { showOverflowMenu = false; onMoreMenu() },
                onArchiveClick = { showOverflowMenu = false; onArchive() },
                onDeleteClick = { showOverflowMenu = false; onDelete() },
                onVisibilityClick = { showOverflowMenu = false; showVisibilityPicker = true }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Hero Card
            item {
                HeroCard(project = project)
            }

            // 2. Stats Row (3 equal-width clickable cards)
            item {
                StatsRow(
                    project = project,
                    onMembersClick = onViewMembers,
                    onTasksClick = onViewTasks,
                    onChatsClick = onViewChats
                )
            }

            // 3. Quick Actions Row
            item {
                QuickActionsRow(
                    onCreateTask = onNewTask,
                    onCreateChat = onNewChat
                )
            }

            // 4. About Card (only if any detail is present)
            val hasAboutDetails = project.deadline != null ||
                    project.githubUrl != null ||
                    project.websiteUrl != null ||
                    project.techStack != null ||
                    project.projectMotive != null ||
                    project.businessModel != null ||
                    project.targetAudience != null ||
                    project.openSourceLicense != null ||
                    project.industryTags.isNotEmpty()

            if (hasAboutDetails) {
                item {
                    AboutCard(project = project)
                }
            }

            // 5. Activity Card
            item {
                ActivityCard(
                    activities = activities.take(5),
                    onViewAll = onViewActivity
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Visibility picker dialog
    if (showVisibilityPicker) {
        val options = listOf("Private", "Internal", "Public")
        val currentVis = project.visibility ?: "Private"
        AlertDialog(
            onDismissRequest = { showVisibilityPicker = false },
            title = { Text("Change Visibility", color = ColorTokens.ReactTheme.foreground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { option ->
                        val isSelected = option.equals(currentVis, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
                                    else ColorTokens.ReactTheme.card
                                )
                                .clickable {
                                    onChangeVisibility(option.uppercase())
                                    showVisibilityPicker = false
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = when (option) {
                                    "Private" -> Icons.Default.Lock
                                    "Internal" -> Icons.Default.People
                                    else -> Icons.Default.Public
                                },
                                contentDescription = null,
                                tint = if (isSelected) ColorTokens.ReactTheme.primary
                                else ColorTokens.ReactTheme.mutedForeground,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = option,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) ColorTokens.ReactTheme.primary
                                    else ColorTokens.ReactTheme.foreground
                                )
                                Text(
                                    text = when (option) {
                                        "Private" -> "Only project members"
                                        "Internal" -> "All organization members"
                                        else -> "Anyone can discover"
                                    },
                                    fontSize = 11.sp,
                                    color = ColorTokens.ReactTheme.mutedForeground
                                )
                            }
                            if (isSelected) {
                                Spacer(Modifier.weight(1f))
                                Text("Current", fontSize = 11.sp, color = ColorTokens.ReactTheme.mutedForeground)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showVisibilityPicker = false }) {
                    Text("Cancel", color = ColorTokens.ReactTheme.mutedForeground)
                }
            },
            containerColor = ColorTokens.ReactTheme.card,
            shape = RoundedCornerShape(Tokens.CornerRadius.lg)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top App Bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectDetailsTopBar(
    projectName: String,
    currentMember: ProjectMember? = null,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    showOverflowMenu: Boolean = false,
    onDismissMenu: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onVisibilityClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = projectName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = ColorTokens.ReactTheme.foreground
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = IconSet.Navigation.back,
                    contentDescription = "Back",
                    tint = ColorTokens.ReactTheme.foreground,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        actions = {
            PermissionGated(
                permission = Permission.EDIT_PROJECT,
                currentMember = currentMember,
                action = "Edit project"
            ) {
                Box {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = IconSet.Action.moreVert,
                            contentDescription = "More options",
                            tint = ColorTokens.ReactTheme.foreground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = onDismissMenu
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = onEditClick,
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Visibility") },
                            onClick = onVisibilityClick,
                            leadingIcon = {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            onClick = onArchiveClick,
                            leadingIcon = {
                                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete",
                                    color = ColorTokens.ReactTheme.destructive
                                )
                            },
                            onClick = onDeleteClick,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = ColorTokens.ReactTheme.destructive,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ColorTokens.ReactTheme.card,
            titleContentColor = ColorTokens.ReactTheme.foreground,
            navigationIconContentColor = ColorTokens.ReactTheme.foreground,
            actionIconContentColor = ColorTokens.ReactTheme.foreground
        ),
        modifier = Modifier.border(
            width = 1.dp,
            color = ColorTokens.ReactTheme.border,
            shape = RectangleShape
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Hero Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(project: ProjectData) {
    KosmosCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Project name + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    ),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                ProjectStatusChip(status = project.status)
            }

            // Description
            val descriptionText = if (project.description.isBlank()) "(No description)" else project.description
            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (project.description.isBlank())
                        ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)
                    else
                        ColorTokens.ReactTheme.mutedForeground
                ),
                lineHeight = 20.sp
            )

            // Category + Visibility tags
            val hasTags = project.category != null || project.visibility != null
            if (hasTags) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    project.category?.let { cat ->
                        OutlinedTagChip(label = cat)
                    }
                    project.visibility?.let { vis ->
                        OutlinedTagChip(label = vis)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectStatusChip(status: String) {
    val color = when (status) {
        "Active" -> ColorTokens.Status.online
        "Archived" -> ColorTokens.ReactTheme.mutedForeground
        else -> ColorTokens.ReactTheme.primary
    }
    StatusBadge(
        text = status,
        color = color,
        size = BadgeSize.MEDIUM
    )
}

@Composable
private fun OutlinedTagChip(label: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Stats Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(
    project: ProjectData,
    onMembersClick: () -> Unit,
    onTasksClick: () -> Unit,
    onChatsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniStatCard(
            icon = Icons.Default.People,
            label = "Members",
            value = project.memberCount.toString(),
            color = Color(0xFF6366F1),
            onClick = onMembersClick,
            modifier = Modifier.weight(1f)
        )
        MiniStatCard(
            icon = Icons.Default.CheckBox,
            label = "Tasks",
            value = "${project.completedTasks}/${project.taskCount}",
            color = Color(0xFFA855F7),
            onClick = onTasksClick,
            modifier = Modifier.weight(1f)
        )
        MiniStatCard(
            icon = Icons.AutoMirrored.Filled.Chat,
            label = "Chats",
            value = project.chatCount.toString(),
            color = Color(0xFF7C3AED),
            onClick = onChatsClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MiniStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = {
            Log.d("MiniStatCard", "Clicked: $label")
            onClick()
        },
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ColorTokens.ReactTheme.card),
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = Tokens.Elevation.level1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ColorTokens.ReactTheme.foreground
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ColorTokens.ReactTheme.mutedForeground
                ),
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Quick Actions Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(
    onCreateTask: () -> Unit,
    onCreateChat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCreateTask,
            modifier = Modifier.weight(1f).height(48.dp),
            border = BorderStroke(1.dp, ColorTokens.ReactTheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ColorTokens.ReactTheme.primary
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "+ New Task",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        OutlinedButton(
            onClick = onCreateChat,
            modifier = Modifier.weight(1f).height(48.dp),
            border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ColorTokens.ReactTheme.foreground
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "+ New Chat",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. About Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutCard(project: ProjectData) {
    SectionCard(title = "ABOUT") {
        project.deadline?.let {
            AboutDetailRow(Icons.Default.CalendarToday, it)
        }
        project.githubUrl?.let {
            AboutDetailRow(Icons.Default.Link, it)
        }
        project.websiteUrl?.let {
            AboutDetailRow(Icons.Default.Language, it)
        }
        project.techStack?.let { stack ->
            // Tech stack as small tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(14.dp)
                )
                stack.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tech ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ColorTokens.ReactTheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = tech,
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorTokens.ReactTheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
        project.projectMotive?.let {
            AboutDetailRow(Icons.Default.Flag, it)
        }
        project.businessModel?.let {
            AboutDetailRow(Icons.Default.Business, it)
        }
        project.targetAudience?.let {
            AboutDetailRow(Icons.Default.People, it)
        }
        if (project.industryTags.isNotEmpty()) {
            AboutDetailRow(Icons.AutoMirrored.Filled.TrendingUp, project.industryTags.joinToString(" · "))
        }
        project.openSourceLicense?.let {
            AboutDetailRow(Icons.Default.Gavel, it)
        }
    }
}

@Composable
private fun AboutDetailRow(icon: ImageVector, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = ColorTokens.ReactTheme.mutedForeground
            ),
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Activity Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActivityCard(
    activities: List<ActivityItem>,
    onViewAll: () -> Unit
) {
    SectionCard(title = "RECENT ACTIVITY") {
        if (activities.isEmpty()) {
            Text(
                text = "No recent activity.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            )
        } else {
            activities.forEachIndexed { index, activity ->
                ActivityListItem(activity = activity)
                if (index < activities.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = ColorTokens.ReactTheme.border.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                }
            }
        }

        // View All Activity link
        TextButton(
            onClick = onViewAll,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
        ) {
            Text(
                text = "View All Activity →",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = ColorTokens.ReactTheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun ActivityListItem(activity: ActivityItem) {
    val (entityIcon, entityColor) = getActivityEntityIcon(activity.action)

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon circle (entity type based)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(entityColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = entityIcon,
                contentDescription = null,
                tint = entityColor,
                modifier = Modifier.size(16.dp)
            )
        }

        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${activity.user} ${activity.action}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ColorTokens.ReactTheme.foreground
                ),
                lineHeight = 16.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.7f),
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = activity.time,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                )
            }
        }
    }
}

private fun getActivityEntityIcon(action: String): Pair<ImageVector, Color> {
    return when {
        action.contains("task", ignoreCase = true) -> Icons.Default.CheckBox to Color(0xFFA855F7)
        action.contains("chat", ignoreCase = true) || action.contains("message", ignoreCase = true) -> Icons.AutoMirrored.Filled.Chat to Color(0xFF7C3AED)
        action.contains("member", ignoreCase = true) || action.contains("joined", ignoreCase = true) || action.contains("invited", ignoreCase = true) -> Icons.Default.People to Color(0xFF6366F1)
        action.contains("completed", ignoreCase = true) -> Icons.Default.CheckCircle to Color(0xFF34D399)
        action.contains("added", ignoreCase = true) || action.contains("created", ignoreCase = true) -> Icons.Default.Add to Color(0xFF60A5FA)
        action.contains("started", ignoreCase = true) -> Icons.Default.PlayArrow to Color(0xFF60A5FA)
        else -> Icons.Default.FiberManualRecord to ColorTokens.ReactTheme.mutedForeground
    }
}
