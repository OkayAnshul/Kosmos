package com.example.kosmos.shared.ui.features.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Bottom Navigation for Kosmos App
 *
 * 3 main tabs: Projects, Chats, Tasks
 * Profile/Settings accessible via bottom sheet on ProjectList screen.
 *
 * Design: SCREEN_THEME_GUIDE.md
 * - bar bg: ReactTheme.card (#18181D)
 * - top border: 1dp ReactTheme.border (#2A2A32)
 * - selected: ReactTheme.primary (#7C3AED)
 * - unselected: ReactTheme.mutedForeground (#9CA3AF)
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

    object Discover : BottomNavDestination(
        route = "discover",
        label = "Discover",
        selectedIcon = IconSet.Action.search,
        unselectedIcon = IconSet.Action.search
    )

    object More : BottomNavDestination(
        route = "more",
        label = "More",
        selectedIcon = IconSet.Navigation.more,
        unselectedIcon = IconSet.Navigation.moreOutlined
    )

    companion object {
        val destinations = listOf(Projects, Chats, Tasks, Discover)
        val allDestinations = listOf(Projects, Chats, Tasks, Discover, More)

        fun fromRoute(route: String?): BottomNavDestination {
            return allDestinations.find { it.route == route } ?: Projects
        }
    }
}

@Composable
fun KosmosBottomNavigation(
    selectedDestination: BottomNavDestination,
    onDestinationSelected: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    unreadChatsCount: Int = 0,
    pendingTasksCount: Int = 0
) {
    // Ensure we only render for valid destinations (not More)
    val safeSelected = if (selectedDestination in BottomNavDestination.destinations) {
        selectedDestination
    } else {
        BottomNavDestination.Projects
    }

    NavigationBar(
        modifier = modifier
            .border(
                width = 1.dp,
                color = ColorTokens.ReactTheme.border,
                shape = RectangleShape
            ),
        containerColor = ColorTokens.ReactTheme.card,
        contentColor = ColorTokens.ReactTheme.foreground,
        tonalElevation = 0.dp
    ) {
        // Projects
        NavBarItem(
            destination = BottomNavDestination.Projects,
            selected = safeSelected == BottomNavDestination.Projects,
            badgeCount = 0,
            onSelect = onDestinationSelected
        )

        // Chats
        NavBarItem(
            destination = BottomNavDestination.Chats,
            selected = safeSelected == BottomNavDestination.Chats,
            badgeCount = unreadChatsCount,
            onSelect = onDestinationSelected
        )

        // Tasks
        NavBarItem(
            destination = BottomNavDestination.Tasks,
            selected = safeSelected == BottomNavDestination.Tasks,
            badgeCount = pendingTasksCount,
            onSelect = onDestinationSelected
        )

        // Discover
        NavBarItem(
            destination = BottomNavDestination.Discover,
            selected = safeSelected == BottomNavDestination.Discover,
            badgeCount = 0,
            onSelect = onDestinationSelected
        )
    }
}

@Composable
private fun RowScope.NavBarItem(
    destination: BottomNavDestination,
    selected: Boolean,
    badgeCount: Int,
    onSelect: (BottomNavDestination) -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = { onSelect(destination) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = ColorTokens.ReactTheme.primary,
            selectedTextColor = ColorTokens.ReactTheme.primary,
            unselectedIconColor = ColorTokens.ReactTheme.mutedForeground,
            unselectedTextColor = ColorTokens.ReactTheme.mutedForeground,
            indicatorColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
        ),
        icon = {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = ColorTokens.ReactTheme.primary,
                            contentColor = ColorTokens.ReactTheme.primaryForeground
                        ) {
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

@Composable
fun rememberBottomNavState(
    initialDestination: BottomNavDestination = BottomNavDestination.Projects
): BottomNavState {
    return remember {
        BottomNavState(initialDestination)
    }
}

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

object BottomNavRoutes {
    const val PROJECTS = "projects"
    const val CHATS = "chats_all"
    const val TASKS = "tasks_all"
    const val MORE = "more"

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
