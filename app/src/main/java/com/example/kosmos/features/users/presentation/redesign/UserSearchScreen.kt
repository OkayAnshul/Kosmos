package com.example.kosmos.features.users.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kosmos.core.models.User
import com.example.kosmos.features.users.presentation.components.UserListItem
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.layouts.ScreenScaffoldStandard

/**
 * User Search Screen - Redesigned with Stitch Design
 *
 * Stitch Design Features:
 * - Navy background (#1A1D2E)
 * - Blue accent (#2196F3)
 * - Modern card-based layout
 * - Proper empty/error/loading states
 */
@Composable
fun UserSearchScreen(
    searchQuery: String,
    users: List<User>,
    isLoading: Boolean,
    error: String?,
    onSearchQueryChange: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffoldStandard(
        title = "Find Users",
        onNavigationClick = onNavigateBack,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorTokens.ReactTheme.background)
                .padding(padding)
        ) {
            // Search Bar
            SearchBarStandard(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Search by name, @username, or email",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.md)
            )

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && users.isEmpty() -> {
                        // Loading state (first load)
                        LoadingIndicator(
                            message = "Searching users...",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    error != null -> {
                        // Error state
                        ErrorState(
                            title = "Search failed",
                            message = error,
                            onRetry = onRetry,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    users.isEmpty() && searchQuery.isBlank() -> {
                        // Empty search prompt
                        EmptyState(
                            icon = IconSet.Action.search,
                            title = "Search for users",
                            message = "Enter a name, username, or email to find other users",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    users.isEmpty() && searchQuery.isNotBlank() -> {
                        // No results found
                        EmptyState(
                            icon = IconSet.Action.search,
                            title = "No users found",
                            message = "No results for \"$searchQuery\"",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        // Results list
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Results count header
                            if (!isLoading) {
                                Text(
                                    text = "${users.size} user${if (users.size != 1) "s" else ""} found",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorTokens.ReactTheme.mutedForeground,
                                    modifier = Modifier.padding(
                                        horizontal = Tokens.Spacing.md,
                                        vertical = Tokens.Spacing.xs
                                    )
                                )
                            }

                            // User list
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = Tokens.Spacing.md,
                                    vertical = Tokens.Spacing.xs
                                )
                            ) {
                                items(
                                    items = users,
                                    key = { user -> user.id }
                                ) { user ->
                                    UserListItem(
                                        user = user,
                                        onClick = { onUserClick(user.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Tokens.Spacing.xxs)
                                    )
                                }

                                // Bottom spacing
                                item {
                                    Spacer(modifier = Modifier.height(Tokens.Spacing.xl))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
