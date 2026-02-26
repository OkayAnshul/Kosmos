package com.example.kosmos.features.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kosmos.features.chat.presentation.redesign.ChatListScreenReact
import com.example.kosmos.features.chat.presentation.redesign.ChatRoomScreenReact
import com.example.kosmos.features.projects.presentation.redesign.ProjectDetailsScreenReact
import com.example.kosmos.features.projects.presentation.redesign.ProjectListScreenReact
import com.example.kosmos.features.tasks.presentation.redesign.MyTasksScreenReact
import com.example.kosmos.features.tasks.presentation.redesign.TaskDetailScreenReact
import com.example.kosmos.features.tasks.presentation.redesign.TaskEditScreenReact
import com.example.kosmos.shared.ui.designsystem.ColorTokens

/**
 * React Screens Test Launcher
 *
 * Navigation hub to test all 7 Phase 1 React screen implementations.
 *
 * Usage: Launch this screen to access all implemented React designs.
 */

// Navigation routes
object ReactTestRoutes {
    const val HOME = "react_test_home"
    const val PROJECT_LIST = "project_list_react"
    const val PROJECT_DETAILS = "project_details_react"
    const val MY_TASKS = "my_tasks_react"
    const val TASK_DETAIL = "task_detail_react"
    const val TASK_EDIT = "task_edit_react"
    const val CHAT_LIST = "chat_list_react"
    const val CHAT_ROOM = "chat_room_react"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactScreensTestLauncher() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ReactTestRoutes.HOME
    ) {
        composable(ReactTestRoutes.HOME) {
            TestHomeScreen(navController)
        }
        composable(ReactTestRoutes.PROJECT_LIST) {
            ProjectListScreenReact(
                onProjectClick = {
                    navController.navigate(ReactTestRoutes.PROJECT_DETAILS)
                }
            )
        }
        composable(ReactTestRoutes.PROJECT_DETAILS) {
            ProjectDetailsScreenReact(
                projectId = "1",
                onBack = { navController.popBackStack() }
            )
        }
        composable(ReactTestRoutes.MY_TASKS) {
            MyTasksScreenReact(
                onTaskClick = {
                    navController.navigate(ReactTestRoutes.TASK_DETAIL)
                }
            )
        }
        composable(ReactTestRoutes.TASK_DETAIL) {
            TaskDetailScreenReact(
                taskId = "1",
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(ReactTestRoutes.TASK_EDIT) }
            )
        }
        composable(ReactTestRoutes.TASK_EDIT) {
            TaskEditScreenReact(
                taskId = "1",
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
                onDelete = { navController.popBackStack() }
            )
        }
        composable(ReactTestRoutes.CHAT_LIST) {
            ChatListScreenReact(
                onChatClick = {
                    navController.navigate(ReactTestRoutes.CHAT_ROOM)
                }
            )
        }
        composable(ReactTestRoutes.CHAT_ROOM) {
            ChatRoomScreenReact(
                chatId = "1",
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestHomeScreen(navController: NavHostController) {
    val screens = listOf(
        ScreenItem(
            title = "Project List",
            description = "Browse all projects with search & filters",
            icon = Icons.Default.Folder,
            route = ReactTestRoutes.PROJECT_LIST,
            color = ColorTokens.ReactTheme.primary
        ),
        ScreenItem(
            title = "Project Details",
            description = "5 tabs: Overview, Chats, Tasks, Members, Activity",
            icon = Icons.Default.Info,
            route = ReactTestRoutes.PROJECT_DETAILS,
            color = ColorTokens.ReactTheme.primary
        ),
        ScreenItem(
            title = "My Tasks",
            description = "List & Kanban views with filters",
            icon = Icons.Default.CheckCircle,
            route = ReactTestRoutes.MY_TASKS,
            color = ColorTokens.ReactTheme.accent
        ),
        ScreenItem(
            title = "Task Detail",
            description = "Full task info with subtasks & activity",
            icon = Icons.Default.Assignment,
            route = ReactTestRoutes.TASK_DETAIL,
            color = ColorTokens.ReactTheme.accent
        ),
        ScreenItem(
            title = "Task Edit",
            description = "Create/edit task form with all fields",
            icon = Icons.Default.Edit,
            route = ReactTestRoutes.TASK_EDIT,
            color = ColorTokens.ReactTheme.accent
        ),
        ScreenItem(
            title = "Chat List",
            description = "All chats with search & filter chips",
            icon = Icons.Default.Chat,
            route = ReactTestRoutes.CHAT_LIST,
            color = ColorTokens.ReactTheme.secondary
        ),
        ScreenItem(
            title = "Chat Room",
            description = "Messages with 3 types & reactions",
            icon = Icons.Default.Message,
            route = ReactTestRoutes.CHAT_ROOM,
            color = ColorTokens.ReactTheme.secondary
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "React Screens Test",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Phase 1: 7 Screens",
                            fontSize = 12.sp,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorTokens.ReactTheme.card
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorTokens.ReactTheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoCard()
            }

            items(screens) { screen ->
                ScreenCard(
                    screen = screen,
                    onClick = { navController.navigate(screen.route) }
                )
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = ColorTokens.ReactTheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Design-Only Implementation",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = "All screens use mock data. No backend wiring. Styling matches React references 100%.",
                    fontSize = 14.sp,
                    color = ColorTokens.ReactTheme.mutedForeground,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun ScreenCard(
    screen: ScreenItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = screen.color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = null,
                    tint = screen.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = screen.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = screen.description,
                    fontSize = 13.sp,
                    color = ColorTokens.ReactTheme.mutedForeground,
                    lineHeight = 18.sp
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private data class ScreenItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val color: androidx.compose.ui.graphics.Color
)
