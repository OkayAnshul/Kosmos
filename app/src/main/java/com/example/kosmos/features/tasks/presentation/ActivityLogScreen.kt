package com.example.kosmos.features.tasks.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.ActivityActionType
import com.example.kosmos.core.models.TaskActivity
import com.example.kosmos.core.models.User
import com.example.kosmos.features.tasks.components.ActivityTimeline
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.layouts.ScreenScaffoldStandard

/**
 * Activity Log Screen
 *
 * Project-wide activity log with:
 * - Search by commit message/description/actor name
 * - Filter by action type
 * - Filter by user
 * - Pagination (100 items at a time)
 * - Clear filters option
 *
 * Pattern: Full-screen activity feed with filtering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
    activities: List<TaskActivity>,
    availableUsers: List<User>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    error: String?,
    filterSummary: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterByActionType: (ActivityActionType?) -> Unit,
    onFilterByUser: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(true) }
    var showActionTypeFilter by remember { mutableStateOf(false) }
    var showUserFilter by remember { mutableStateOf(false) }

    ScreenScaffoldStandard(
        title = "Activity Log",
        onNavigationClick = onNavigateBack,
        actions = {
            // Search icon - toggles search bar visibility
            IconButton(onClick = { showSearchBar = !showSearchBar }) {
                Icon(
                    imageVector = if (showSearchBar) IconSet.Navigation.close else IconSet.Action.search,
                    contentDescription = if (showSearchBar) "Hide search" else "Show search",
                    tint = ColorTokens.ReactTheme.foreground
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar (collapsible)
            if (showSearchBar) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        onSearchQueryChange(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.md)
                )
            }

            // Filter chips
            FilterChipsRow(
                filterSummary = filterSummary,
                onActionTypeClick = { showActionTypeFilter = true },
                onUserClick = { showUserFilter = true },
                onClearFilters = onClearFilters,
                hasFilters = filterSummary != "All activity",
                modifier = Modifier.padding(horizontal = Tokens.Spacing.md)
            )

            // Content
            when {
                isLoading -> {
                    LoadingState()
                }
                error != null -> {
                    ErrorState(
                        message = error,
                        onRetry = { /* TODO */ }
                    )
                }
                activities.isEmpty() -> {
                    EmptyState(hasFilters = filterSummary != "All activity")
                }
                else -> {
                    ActivityTimeline(
                        activities = activities,
                        onLoadMore = onLoadMore,
                        hasMore = hasMore,
                        isLoading = isLoadingMore,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Action Type Filter Bottom Sheet
        if (showActionTypeFilter) {
            ActionTypeFilterSheet(
                onSelectActionType = { actionType ->
                    onFilterByActionType(actionType)
                    showActionTypeFilter = false
                },
                onDismiss = { showActionTypeFilter = false }
            )
        }

        // User Filter Bottom Sheet
        if (showUserFilter) {
            UserFilterSheet(
                users = availableUsers,
                onSelectUser = { userId ->
                    onFilterByUser(userId)
                    showUserFilter = false
                },
                onDismiss = { showUserFilter = false }
            )
        }
    }
}

/**
 * Search Bar
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = "Search activity...",
                style = TypographyTokens.typography.bodyMedium,
                color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = IconSet.Action.search,
                contentDescription = null,
                tint = ColorTokens.ReactTheme.mutedForeground
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = IconSet.Action.clear,
                        contentDescription = "Clear",
                        tint = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ColorTokens.ReactTheme.primary,
            unfocusedBorderColor = ColorTokens.ReactTheme.border,
            cursorColor = ColorTokens.ReactTheme.primary,
            focusedTextColor = ColorTokens.ReactTheme.foreground,
            unfocusedTextColor = ColorTokens.ReactTheme.foreground
        ),
        shape = MaterialTheme.shapes.medium,
        singleLine = true
    )
}

/**
 * Filter Chips Row
 */
@Composable
private fun FilterChipsRow(
    filterSummary: String,
    onActionTypeClick: () -> Unit,
    onUserClick: () -> Unit,
    onClearFilters: () -> Unit,
    hasFilters: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Tokens.Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filter summary
        Text(
            text = filterSummary,
            style = TypographyTokens.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.weight(1f)
        )

        // Action Type filter button
        FilterChip(
            selected = false,
            onClick = onActionTypeClick,
            label = {
                Text(
                    text = "Type",
                    style = TypographyTokens.typography.labelMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = IconSet.Action.filter,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = ColorTokens.ReactTheme.card,
                labelColor = ColorTokens.ReactTheme.foreground,
                iconColor = ColorTokens.ReactTheme.primary
            )
        )

        // User filter button
        FilterChip(
            selected = false,
            onClick = onUserClick,
            label = {
                Text(
                    text = "User",
                    style = TypographyTokens.typography.labelMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = IconSet.User.person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = ColorTokens.ReactTheme.card,
                labelColor = ColorTokens.ReactTheme.foreground,
                iconColor = ColorTokens.ReactTheme.primary
            )
        )

        // Clear filters button (only if filters active)
        if (hasFilters) {
            IconButton(onClick = onClearFilters) {
                Icon(
                    imageVector = IconSet.Action.clear,
                    contentDescription = "Clear filters",
                    tint = ColorTokens.ReactTheme.destructive
                )
            }
        }
    }
}

/**
 * Action Type Filter Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionTypeFilterSheet(
    onSelectActionType: (ActivityActionType?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ColorTokens.ReactTheme.card,
        contentColor = ColorTokens.ReactTheme.foreground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md)
                .padding(bottom = Tokens.Spacing.xl)
        ) {
            Text(
                text = "Filter by Action Type",
                style = TypographyTokens.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ColorTokens.ReactTheme.foreground,
                modifier = Modifier.padding(bottom = Tokens.Spacing.md)
            )

            // All option
            ActionTypeOption(
                label = "All",
                onClick = {
                    onSelectActionType(null)
                }
            )

            HorizontalDivider(color = ColorTokens.ReactTheme.border.copy(alpha = 0.2f))

            // Individual action types
            ActivityActionType.values().forEach { actionType ->
                ActionTypeOption(
                    label = formatActionType(actionType),
                    onClick = { onSelectActionType(actionType) }
                )
            }
        }
    }
}

/**
 * User Filter Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFilterSheet(
    users: List<User>,
    onSelectUser: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ColorTokens.ReactTheme.card,
        contentColor = ColorTokens.ReactTheme.foreground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md)
                .padding(bottom = Tokens.Spacing.xl)
        ) {
            Text(
                text = "Filter by User",
                style = TypographyTokens.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ColorTokens.ReactTheme.foreground,
                modifier = Modifier.padding(bottom = Tokens.Spacing.md)
            )

            // All option
            UserOption(
                userName = "All",
                onClick = { onSelectUser(null) }
            )

            HorizontalDivider(color = ColorTokens.ReactTheme.border.copy(alpha = 0.2f))

            // Individual users
            users.forEach { user ->
                UserOption(
                    userName = user.displayName,
                    onClick = { onSelectUser(user.id) }
                )
            }
        }
    }
}

/**
 * Action Type Option Item
 */
@Composable
private fun ActionTypeOption(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = ColorTokens.ReactTheme.card
    ) {
        Text(
            text = label,
            style = TypographyTokens.typography.bodyLarge,
            color = ColorTokens.ReactTheme.foreground,
            modifier = Modifier.padding(Tokens.Spacing.md)
        )
    }
}

/**
 * User Option Item
 */
@Composable
private fun UserOption(
    userName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = ColorTokens.ReactTheme.card
    ) {
        Text(
            text = userName,
            style = TypographyTokens.typography.bodyLarge,
            color = ColorTokens.ReactTheme.foreground,
            modifier = Modifier.padding(Tokens.Spacing.md)
        )
    }
}

/**
 * Loading State
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = ColorTokens.ReactTheme.primary
        )
    }
}

/**
 * Error State
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Tokens.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = IconSet.Status.error,
            contentDescription = null,
            tint = ColorTokens.ReactTheme.destructive,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.md))

        Text(
            text = message,
            style = TypographyTokens.typography.bodyMedium,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.md))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorTokens.ReactTheme.primary,
                contentColor = ColorTokens.ReactTheme.primaryForeground
            )
        ) {
            Text("Retry")
        }
    }
}

/**
 * Empty State
 */
@Composable
private fun EmptyState(
    hasFilters: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Tokens.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = IconSet.Time.schedule,
            contentDescription = null,
            tint = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.md))

        Text(
            text = if (hasFilters) "No matching activity" else "No activity yet",
            style = TypographyTokens.typography.titleMedium,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.xs))

        Text(
            text = if (hasFilters) "Try adjusting your filters" else "Activity will appear here as tasks change",
            style = TypographyTokens.typography.bodySmall,
            color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.7f)
        )
    }
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

private fun formatActionType(actionType: ActivityActionType): String {
    return when (actionType) {
        ActivityActionType.CREATED -> "Created"
        ActivityActionType.UPDATED -> "Updated"
        ActivityActionType.STATUS_CHANGED -> "Status Changed"
        ActivityActionType.PRIORITY_CHANGED -> "Priority Changed"
        ActivityActionType.ASSIGNED -> "Assigned"
        ActivityActionType.UNASSIGNED -> "Unassigned"
        ActivityActionType.DESCRIPTION_CHANGED -> "Description Changed"
        ActivityActionType.DUE_DATE_CHANGED -> "Due Date Changed"
        ActivityActionType.TAGS_UPDATED -> "Tags Updated"
        ActivityActionType.COMMENT_ADDED -> "Comment Added"
        ActivityActionType.TIME_LOGGED -> "Time Logged"
        ActivityActionType.DEPENDENCY_ADDED -> "Dependency Added"
        ActivityActionType.DEPENDENCY_REMOVED -> "Dependency Removed"
        ActivityActionType.SUBTASK_ADDED -> "Subtask Added"
        ActivityActionType.ARCHIVED -> "Archived"
        ActivityActionType.RESTORED -> "Restored"
        ActivityActionType.DELETED -> "Deleted"
        ActivityActionType.JOURNAL_ENTRY -> "Journal Entry"
    }
}
