package com.example.kosmos.features.projects.presentation.redesign

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.navigation.compose.rememberNavController
import com.example.kosmos.core.models.Permission
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.shared.ui.components.KosmosCard
import com.example.kosmos.shared.ui.components.PermissionGated
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
 * - Project details card (About section)
 * - Stats grid (single-column layout, all clickable to navigate to tabs)
 * - Quick actions (Create Task / Start Chat buttons)
 * - Recent activity timeline (with "View All" button to Activity tab)
 *
 * Design Improvements (2026-01-23):
 * - COMPACT DESIGN: Reduced all spacing for better content visibility
 * - Single-column stats layout (matches React grid-cols-1) - fixes text truncation
 * - CLICKABLE STATS: All stat cards navigate to respective tabs (Chats/Tasks/Members)
 * - CLICKABLE ACTIVITY: "View All" button navigates to Activity tab
 * - All text sizes reduced: About 12sp, headers 15sp, stats labels 14sp
 * - Button heights reduced to 46dp, icon sizes reduced throughout
 * - Section spacing: 12dp between sections, 8-10dp within cards
 * - Activity items: 32dp avatars, 12sp text, tighter line heights
 * - Improved readability with more content visible on screen at once
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
    lastActivity = "2 hours ago"
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
    onMoreMenu: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        // React-designed top app bar (consistent with other screens)
        ProjectDetailsTopBar(
            projectName = project.name,
            projectStatus = project.status,
            currentMember = currentMember,
            onBackClick = onBack,
            onMenuClick = onMoreMenu
        )

        // Overview content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),  // px-4
            verticalArrangement = Arrangement.spacedBy(12.dp)  // Reduced for compactness
        ) {
            // Add top spacing
            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            item {
                OverviewTab(
                    project = project,
                    activities = activities,
                    onNewTask = onNewTask,
                    onNewChat = onNewChat,
                    onViewChats = onViewChats,
                    onViewTasks = onViewTasks,
                    onViewMembers = onViewMembers,
                    onViewActivity = onViewActivity,
                    onTabSelected = {} // No longer needed, but kept for compatibility
                )
            }

            // Add bottom padding for bottom nav
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Project Details Top App Bar - React Design
 * Acts as top bar for Overview screen, showing project name and status
 */
@Composable
private fun ProjectDetailsTopBar(
    projectName: String,
    projectStatus: String,
    currentMember: ProjectMember? = null,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Surface(
        color = ColorTokens.ReactTheme.card,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = ColorTokens.ReactTheme.border,
                shape = RectangleShape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = IconSet.Navigation.back,
                    contentDescription = "Back",
                    tint = ColorTokens.ReactTheme.foreground,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Project name and status
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(status = projectStatus)
                    Text(
                        text = "Project Details",
                        fontSize = 12.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }

            // Menu button - gated on EDIT_PROJECT permission
            PermissionGated(
                permission = Permission.EDIT_PROJECT,
                currentMember = currentMember,
                action = "Edit project"
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = IconSet.Action.moreVert,
                        contentDescription = "More options",
                        tint = ColorTokens.ReactTheme.foreground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// Overview Tab Content (line 79-180)
@Composable
private fun OverviewTab(
    project: ProjectData,
    activities: List<ActivityItem> = mockActivities,  // Accept activities parameter
    onNewTask: () -> Unit = {},
    onNewChat: () -> Unit = {},
    onViewChats: () -> Unit = {},  // Navigate to Chats tab
    onViewTasks: () -> Unit = {},  // Navigate to Tasks tab
    onViewMembers: () -> Unit = {},  // Navigate to Members tab
    onViewActivity: () -> Unit = {},  // Navigate to Activity tab
    onTabSelected: (ProjectTabReact) -> Unit = {}  // Kept for compatibility but unused
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)  // More compact
    ) {
        // About Card - shows all available project details
        KosmosCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "About",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    )
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (project.description.isNotBlank()) {
                    Text(
                        text = project.description,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }

                // Detail rows for non-null fields
                project.category?.let { AboutDetailRow(Icons.Default.Category, "Category", it) }
                project.visibility?.let { AboutDetailRow(Icons.Default.Visibility, "Visibility", it) }
                project.deadline?.let { AboutDetailRow(Icons.Default.CalendarToday, "Deadline", it) }
                project.createdAt?.let { AboutDetailRow(Icons.Default.Schedule, "Created", it) }
                project.projectMotive?.let { AboutDetailRow(Icons.Default.Flag, "Motive", it) }
                project.techStack?.let { AboutDetailRow(Icons.Default.Code, "Tech Stack", it) }
                project.businessModel?.let { AboutDetailRow(Icons.Default.Business, "Business Model", it) }
                project.targetAudience?.let { AboutDetailRow(Icons.Default.People, "Target Audience", it) }
                // industryTags: BUSINESS category only — parsed from JSON array stored in Project.industryTags
                if (project.industryTags.isNotEmpty()) {
                    AboutDetailRow(Icons.Default.TrendingUp, "Industry", project.industryTags.joinToString(" · "))
                }
                project.openSourceLicense?.let { AboutDetailRow(Icons.Default.Gavel, "License", it) }
                project.githubUrl?.let { AboutDetailRow(Icons.Default.Link, "GitHub", it) }
                project.websiteUrl?.let { AboutDetailRow(Icons.Default.Language, "Website", it) }

                if (project.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null,
                            tint = ColorTokens.ReactTheme.mutedForeground,
                            modifier = Modifier.size(14.dp)
                        )
                        project.tags.forEach { tag ->
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                color = ColorTokens.ReactTheme.primary,
                                modifier = Modifier
                                    .background(
                                        ColorTokens.ReactTheme.primary.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Stats Grid - All stats are clickable to navigate to respective tabs
        StatsGrid(
            project = project,
            onChatsClick = onViewChats,  // Navigate to Chats tab
            onTasksClick = onViewTasks,  // Navigate to Tasks tab
            onMembersClick = onViewMembers  // Navigate to Members tab
        )

        // Quick Actions (line 115-127)
        QuickActions(
            onNewTask = onNewTask,
            onNewChat = onNewChat
        )

        // Recent Activity (line 146-179)
        RecentActivityCard(
            activities = activities,
            onViewAll = onViewActivity  // Navigate to Activity tab
        )
    }
}

// Stats Grid (line 93-112)
// Single-column layout as per React design (grid-cols-1)
// All stats are clickable to navigate to their respective tabs
@Composable
private fun StatsGrid(
    project: ProjectData,
    onChatsClick: (() -> Unit)? = null,  // Navigate to Chats tab
    onTasksClick: (() -> Unit)? = null,  // Navigate to Tasks tab
    onMembersClick: (() -> Unit)? = null  // Navigate to Members tab
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)  // Compact spacing
    ) {
        StatCard(
            icon = Icons.Default.Chat,
            label = "Active Chats",
            value = project.chatCount,
            color = Color(0xFF7C3AED),
            onClick = onChatsClick,
            modifier = Modifier.fillMaxWidth()
        )
        StatCard(
            icon = Icons.Default.CheckBox,
            label = "Total Tasks",
            value = project.taskCount,
            color = Color(0xFFA855F7),
            onClick = onTasksClick,
            modifier = Modifier.fillMaxWidth()
        )
        StatCard(
            icon = Icons.Default.People,
            label = "Team Members",
            value = project.memberCount,
            color = Color(0xFF6366F1),
            onClick = onMembersClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AboutDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.size(14.dp).padding(top = 2.dp)
        )
        Text(
            text = "$label: ",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTokens.ReactTheme.foreground
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.weight(1f)
        )
    }
}

// Stat Card Component (from StatCard.tsx line 12-34)
// Enhanced: High contrast design with excellent readability
@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: Int,
    color: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardModifier = modifier.then(
        Modifier.shadow(
            elevation = 4.dp,
            shape = RoundedCornerShape(10.dp),
            ambientColor = Color.Black.copy(alpha = 0.15f),
            spotColor = Color.Black.copy(alpha = 0.15f)
        )
    )

    // Use Card directly for better click handling
    Card(
        onClick = {
            Log.d("StatCard", "Card clicked! Label: $label, onClick null: ${onClick == null}")
            onClick?.invoke()
        },
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        ),
        border = BorderStroke(
            width = 1.dp,
            color = ColorTokens.ReactTheme.border
        ),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Tokens.Elevation.level1
        ),
        enabled = onClick != null  // Only enable if clickable
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),  // Compact
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Tokens.Spacing.md)  // Card padding
        ) {
            // Icon container with glow
            Box(
                modifier = Modifier
                    .size(40.dp)  // More compact
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(8.dp),
                        ambientColor = color.copy(alpha = 0.2f),
                        spotColor = color.copy(alpha = 0.2f)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)  // More compact
                )
            }
            // Label and value - ENHANCED READABILITY
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)  // Tighter spacing
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,  // Readable size
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value.toString(),
                    fontSize = 24.sp,  // Slightly smaller but still prominent
                    fontWeight = FontWeight.Bold,
                    color = ColorTokens.ReactTheme.foreground,
                    lineHeight = 24.sp
                )
            }
            // Show arrow icon if clickable
            if (onClick != null) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View all",
                    tint = ColorTokens.ReactTheme.primary,
                    modifier = Modifier.size(18.dp)  // More compact
                )
            }
        }  // Close Row
    }  // Close Card
}

// Quick Actions (line 115-127)
// Enhanced: Compact design with better labels
@Composable
private fun QuickActions(
    onNewTask: () -> Unit = {},
    onNewChat: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)  // More compact
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = ColorTokens.ReactTheme.primary,
                modifier = Modifier.size(16.dp)  // More compact
            )
            Text(
                text = "Quick Actions",
                fontSize = 15.sp,  // More compact
                fontWeight = FontWeight.SemiBold,
                color = ColorTokens.ReactTheme.foreground
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Create Task button (primary) - clearer title
            Button(
                onClick = onNewTask,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)  // More compact
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(10.dp),
                        ambientColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.35f),
                        spotColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.35f)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorTokens.ReactTheme.primary,
                    contentColor = ColorTokens.ReactTheme.primaryForeground
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(10.dp)  // More compact
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)  // More compact
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Create Task",
                        fontSize = 13.sp,  // More compact
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Start Chat button (secondary) - clearer title
            OutlinedButton(
                onClick = onNewChat,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),  // More compact
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ColorTokens.ReactTheme.secondary,
                    contentColor = ColorTokens.ReactTheme.foreground
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = ColorTokens.ReactTheme.border
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(10.dp)  // More compact
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)  // More compact
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Start Chat",
                        fontSize = 13.sp,  // More compact
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Recent Activity Card (line 146-179)
// Enhanced: Compact design with better spacing, clickable to view all
@Composable
private fun RecentActivityCard(
    activities: List<ActivityItem> = mockActivities,
    onViewAll: () -> Unit = {}
) {
    KosmosCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)  // More compact
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.primary,
                        modifier = Modifier.size(16.dp)  // More compact
                    )
                    Text(
                        text = "Recent Activity",
                        fontSize = 15.sp,  // More compact
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    )
                }
                // View All button
                TextButton(
                    onClick = onViewAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "View All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTokens.ReactTheme.primary
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)  // More compact
            ) {
                activities.forEachIndexed { index, activity ->
                    ActivityItem(activity = activity)
                    if (index < activities.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = ColorTokens.ReactTheme.border.copy(alpha = 0.4f),  // More subtle
                            thickness = 0.5.dp  // Thinner
                        )
                    }
                }
            }
        }
    }
}

// Activity Item (line 154-176)
// Enhanced: Compact design with better readability
@Composable
private fun ActivityItem(
    activity: ActivityItem
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),  // More compact
        verticalAlignment = Alignment.Top
    ) {
        // Avatar - more compact
        Box(
            modifier = Modifier
                .size(32.dp)  // More compact
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    ambientColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.25f),
                    spotColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.25f)
                )
                .clip(CircleShape)
                .background(ColorTokens.ReactTheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = activity.user.first().toString(),
                fontSize = 12.sp,  // More compact
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        // Activity text - more compact
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)  // Tighter spacing
        ) {
            Text(
                text = buildString {
                    append(activity.user)
                    append(" ")
                    append(activity.action)
                },
                fontSize = 12.sp,  // More compact
                lineHeight = 16.sp,  // Tighter line height
                color = ColorTokens.ReactTheme.foreground
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),  // Tighter spacing
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.7f),
                    modifier = Modifier.size(10.dp)  // More compact
                )
                Text(
                    text = activity.time,
                    fontSize = 10.sp,  // More compact
                    color = ColorTokens.ReactTheme.mutedForeground,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Status Badge (reused from ProjectListScreenReact)
@Composable
private fun StatusBadge(
    status: String
) {
    val (bgColor, textColor) = when (status) {
        "Active" -> ColorTokens.Success.light to ColorTokens.Success.onLight
        "Archived" -> ColorTokens.TaskStatus.cancelled to ColorTokens.TaskStatus.onCancelled
        else -> ColorTokens.TaskStatus.todo to ColorTokens.TaskStatus.onTodo
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
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

