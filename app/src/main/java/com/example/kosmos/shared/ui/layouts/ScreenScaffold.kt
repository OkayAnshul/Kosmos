package com.example.kosmos.shared.ui.layouts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Screen Scaffold Components
 *
 * Base screen templates with consistent structure.
 * Handles top app bar, FAB, bottom navigation, and snackbars.
 *
 * Components:
 * - ScreenScaffoldStandard: Basic screen with top bar
 * - ScreenScaffoldWithFAB: Screen with floating action button
 * - ScreenScaffoldWithBottomNav: Screen with bottom navigation
 * - FullScreenScaffold: Full screen without app bars
 */

/**
 * Standard Screen Scaffold
 *
 * Basic screen template with top app bar and content
 * Handles padding, snackbars, and consistent structure
 *
 * @param title Screen title
 * @param modifier Modifier
 * @param subtitle Optional subtitle
 * @param navigationIcon Optional back/menu icon
 * @param onNavigationClick Navigation icon click handler
 * @param actions Optional top bar actions
 * @param snackbarHostState Snackbar host state
 * @param showTopBar Whether to show top app bar
 * @param content Screen content with padding values
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffoldStandard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = IconSet.Navigation.back,
    onNavigationClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    showTopBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = title,
                                style = TypographyTokens.typography.titleLarge
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = TypographyTokens.Custom.caption,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (navigationIcon != null && onNavigationClick != null) {
                            IconButton(onClick = onNavigationClick) {
                                Icon(
                                    imageVector = navigationIcon,
                                    contentDescription = "Navigate"
                                )
                            }
                        }
                    },
                    actions = {
                        if (actions != null) {
                            Row(content = actions)
                        }
                    }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        content = content
    )
}

/**
 * Screen Scaffold with FAB
 *
 * Screen with floating action button
 * FAB positioned at bottom-right (16dp margin)
 *
 * @param title Screen title
 * @param fabIcon FAB icon
 * @param onFabClick FAB click handler
 * @param modifier Modifier
 * @param fabText Optional extended FAB text
 * @param fabExpanded Whether FAB is extended
 * @param navigationIcon Optional navigation icon
 * @param onNavigationClick Navigation click handler
 * @param actions Optional top bar actions
 * @param snackbarHostState Snackbar host state
 * @param content Screen content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffoldWithFAB(
    title: String,
    fabIcon: ImageVector,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
    fabText: String? = null,
    fabExpanded: Boolean = false,
    navigationIcon: ImageVector? = IconSet.Navigation.back,
    onNavigationClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = TypographyTokens.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (navigationIcon != null && onNavigationClick != null) {
                        IconButton(onClick = onNavigationClick) {
                            Icon(
                                imageVector = navigationIcon,
                                contentDescription = "Navigate"
                            )
                        }
                    }
                },
                actions = {
                    if (actions != null) {
                        Row(content = actions)
                    }
                }
            )
        },
        floatingActionButton = {
            if (fabExpanded && fabText != null) {
                ExtendedFloatingActionButton(
                    onClick = onFabClick,
                    icon = {
                        Icon(
                            imageVector = fabIcon,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = fabText,
                            style = TypographyTokens.Custom.buttonText
                        )
                    }
                )
            } else {
                FloatingActionButton(
                    onClick = onFabClick,
                    modifier = Modifier.size(Tokens.TouchTarget.fab)
                ) {
                    Icon(
                        imageVector = fabIcon,
                        contentDescription = null
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        content = content
    )
}

/**
 * Screen Scaffold with Bottom Navigation
 *
 * Screen with bottom navigation bar
 * Content adjusts padding to account for bottom nav
 *
 * @param title Screen title
 * @param selectedTab Currently selected tab index
 * @param navigationItems Bottom navigation items
 * @param onTabSelected Tab selection handler
 * @param modifier Modifier
 * @param navigationIcon Optional navigation icon
 * @param onNavigationClick Navigation click handler
 * @param actions Optional top bar actions
 * @param snackbarHostState Snackbar host state
 * @param content Screen content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffoldWithBottomNav(
    title: String,
    selectedTab: Int,
    navigationItems: List<BottomNavItem>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = TypographyTokens.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (navigationIcon != null && onNavigationClick != null) {
                        IconButton(onClick = onNavigationClick) {
                            Icon(
                                imageVector = navigationIcon,
                                contentDescription = "Navigate"
                            )
                        }
                    }
                },
                actions = {
                    if (actions != null) {
                        Row(content = actions)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                navigationItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (item.badgeCount > 0) {
                                        Badge {
                                            Text(
                                                text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                                style = TypographyTokens.Custom.badgeNumber
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            }
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = TypographyTokens.Custom.bottomNavLabel
                            )
                        }
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        content = content
    )
}

/**
 * Bottom Navigation Item Data Class
 *
 * @param label Tab label
 * @param selectedIcon Icon when selected
 * @param unselectedIcon Icon when not selected
 * @param badgeCount Optional badge count (0 for no badge)
 */
data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

/**
 * Full Screen Scaffold
 *
 * Screen without top app bar or bottom navigation
 * Used for full-screen immersive experiences
 *
 * @param modifier Modifier
 * @param snackbarHostState Snackbar host state
 * @param floatingActionButton Optional FAB
 * @param content Screen content
 */
@Composable
fun FullScreenScaffold(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButton = {
            if (floatingActionButton != null) {
                floatingActionButton()
            }
        },
        content = content
    )
}

/**
 * Search Screen Scaffold
 *
 * Screen with search bar as top app bar
 * Common pattern for search-heavy screens
 *
 * @param searchQuery Current search query
 * @param onSearchQueryChange Search query change handler
 * @param modifier Modifier
 * @param placeholder Search placeholder
 * @param onNavigationClick Back button handler
 * @param actions Optional actions
 * @param snackbarHostState Snackbar host state
 * @param content Screen content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreenScaffold(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    onNavigationClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(placeholder) },
                        leadingIcon = {
                            Icon(
                                imageVector = IconSet.Action.search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = IconSet.Action.clear,
                                        contentDescription = "Clear"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                navigationIcon = {
                    if (onNavigationClick != null) {
                        IconButton(onClick = onNavigationClick) {
                            Icon(
                                imageVector = IconSet.Navigation.back,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (actions != null) {
                        Row(content = actions)
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        content = content
    )
}

/**
 * Tab Screen Scaffold
 *
 * Screen with tab row below top app bar
 * Common pattern for categorized content
 *
 * @param title Screen title
 * @param selectedTabIndex Currently selected tab
 * @param tabs List of tab labels
 * @param onTabSelected Tab selection handler
 * @param modifier Modifier
 * @param navigationIcon Optional navigation icon
 * @param onNavigationClick Navigation click handler
 * @param actions Optional top bar actions
 * @param snackbarHostState Snackbar host state
 * @param content Screen content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabScreenScaffold(
    title: String,
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = IconSet.Navigation.back,
    onNavigationClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = TypographyTokens.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        if (navigationIcon != null && onNavigationClick != null) {
                            IconButton(onClick = onNavigationClick) {
                                Icon(
                                    imageVector = navigationIcon,
                                    contentDescription = "Navigate"
                                )
                            }
                        }
                    },
                    actions = {
                        if (actions != null) {
                            Row(content = actions)
                        }
                    }
                )

                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, tabLabel ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { onTabSelected(index) },
                            text = {
                                Text(
                                    text = tabLabel,
                                    style = TypographyTokens.Custom.tabLabel
                                )
                            }
                        )
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        content = content
    )
}
