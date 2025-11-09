package com.example.kosmos.shared.ui.features.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Bottom Navigation for Kosmos App
 *
 * Persistent bottom navigation with 4 main tabs:
 * - Projects: All user projects
 * - Chats: Recent chats across projects
 * - Tasks: My tasks view (cross-project)
 * - More: Profile, settings, search
 *
 * Features:
 * - Badge counts for unread messages and pending tasks
 * - Smooth tab switching with state preservation
 * - Filled/outlined icon variants based on selection
 */

/**
 * Bottom Navigation Destinations
 */
sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Projects : BottomNavDestination(
        route = "projects",
        label = "Projects",
        selectedIcon = IconSet.Navigation.projects,
        unselectedIcon = IconSet.Navigation.projectsOutlined
    )

    object Chats : BottomNavDestination(
        route = "chats",
        label = "Chats",
        selectedIcon = IconSet.Navigation.chats,
        unselectedIcon = IconSet.Navigation.chatsOutlined
    )

    object Tasks : BottomNavDestination(
        route = "tasks",
        label = "Tasks",
        selectedIcon = IconSet.Navigation.tasks,
        unselectedIcon = IconSet.Navigation.tasksOutlined
    )

    object More : BottomNavDestination(
        route = "more",
        label = "More",
        selectedIcon = IconSet.Navigation.more,
        unselectedIcon = IconSet.Navigation.moreOutlined
    )

    companion object {
        val destinations = listOf(Projects, Chats, Tasks, More)

        fun fromRoute(route: String?): BottomNavDestination {
            return destinations.find { it.route == route } ?: Projects
        }
    }
}

/**
 * Bottom Navigation Bar
 *
 * Main bottom navigation component with badges
 *
 * @param selectedDestination Currently selected destination
 * @param onDestinationSelected Destination selection handler
 * @param modifier Modifier
 * @param unreadChatsCount Unread chats badge count
 * @param pendingTasksCount Pending tasks badge count
 */
@Composable
fun KosmosBottomNavigation(
    selectedDestination: BottomNavDestination,
    onDestinationSelected: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    unreadChatsCount: Int = 0,
    pendingTasksCount: Int = 0
) {
    NavigationBar(modifier = modifier) {
        BottomNavDestination.destinations.forEach { destination ->
            val selected = selectedDestination == destination

            // Determine badge count
            val badgeCount = when (destination) {
                is BottomNavDestination.Chats -> unreadChatsCount
                is BottomNavDestination.Tasks -> pendingTasksCount
                else -> 0
            }

            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge {
                                    Text(
                                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                        style = TypographyTokens.Custom.badgeNumber
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.label
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.label,
                        style = TypographyTokens.Custom.bottomNavLabel
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}

/**
 * Bottom Navigation State
 *
 * State holder for bottom navigation
 *
 * @param initialDestination Initial selected destination
 */
@Composable
fun rememberBottomNavState(
    initialDestination: BottomNavDestination = BottomNavDestination.Projects
): BottomNavState {
    return remember {
        BottomNavState(initialDestination)
    }
}

/**
 * Bottom Navigation State Class
 */
class BottomNavState(
    initialDestination: BottomNavDestination
) {
    var currentDestination by mutableStateOf(initialDestination)
        private set

    fun navigateTo(destination: BottomNavDestination) {
        if (currentDestination != destination) {
            currentDestination = destination
        }
    }

    fun isSelected(destination: BottomNavDestination): Boolean {
        return currentDestination == destination
    }
}

/**
 * Bottom Navigation Routes
 *
 * Route definitions for navigation graph
 */
object BottomNavRoutes {
    const val PROJECTS = "projects"
    const val CHATS = "chats_all" // Aggregated chats across projects
    const val TASKS = "tasks_all" // My tasks across projects
    const val MORE = "more"

    // Sub-routes
    const val PROJECT_DETAIL = "project/{projectId}"
    const val CHAT_ROOM = "chat/{chatRoomId}"
    const val TASK_BOARD = "taskBoard/{chatRoomId}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val USER_SEARCH = "userSearch"

    fun projectDetail(projectId: String) = "project/$projectId"
    fun chatRoom(chatRoomId: String) = "chat/$chatRoomId"
    fun taskBoard(chatRoomId: String) = "taskBoard/$chatRoomId"
}
