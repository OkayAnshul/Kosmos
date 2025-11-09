package com.example.kosmos.features.users.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.User
import com.example.kosmos.features.users.presentation.components.UserListItem
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * User Search Screen
 * Allows searching for users by name or email
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit, // Navigate to user profile with userId
    modifier: Modifier = Modifier,
    viewModel: UserSearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Users") },
                navigationIcon = {
                    IconButtonStandard(
                        icon = IconSet.Navigation.back,
                        onClick = onNavigateBack,
                        contentDescription = "Back"
                    )
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBarStandard(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search by name, @username, or email",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.md)
            )

            // Content
            when {
                uiState.isLoading && uiState.users.isEmpty() -> {
                    // Loading state (first load)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                        ) {
                            LoadingIndicator()
                            Text(
                                text = "Searching...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.error != null -> {
                    // Error state
                    uiState.error?.let { error ->
                        ErrorState(
                            error = error,
                            onRetry = viewModel::retrySearch
                        )
                    }
                }

                uiState.users.isEmpty() && searchQuery.isBlank() -> {
                    // Empty search prompt
                    EmptySearchPrompt()
                }

                uiState.users.isEmpty() && searchQuery.isNotBlank() -> {
                    // No results found
                    NoResultsFound(query = searchQuery)
                }

                else -> {
                    // Results list
                    UserResultsList(
                        users = uiState.users,
                        onUserClick = onUserClick,
                        isRefreshing = uiState.isLoading
                    )
                }
            }
        }
    }
}

/**
 * User Results List
 */
@Composable
private fun UserResultsList(
    users: List<User>,
    onUserClick: (String) -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Results count header
        if (!isRefreshing) {
            Text(
                text = "${users.size} user${if (users.size != 1) "s" else ""} found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.xs)
            )
        }

        // User list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = Tokens.Spacing.xs)
        ) {
            items(
                items = users,
                key = { user -> user.id }
            ) { user ->
                UserListItem(
                    user = user,
                    onClick = { onUserClick(user.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Empty Search Prompt
 */
@Composable
private fun EmptySearchPrompt(modifier: Modifier = Modifier) {
    EmptyState(
        icon = IconSet.Action.search,
        title = "Search for users",
        message = "Enter a name or email to find other users",
        modifier = modifier
    )
}

/**
 * No Results Found
 */
@Composable
private fun NoResultsFound(
    query: String,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = IconSet.Action.search,
        title = "No users found",
        message = "No results for \"$query\"",
        modifier = modifier
    )
}

/**
 * Error State - Use design system component
 */
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.example.kosmos.shared.ui.components.ErrorState(
        title = "Search failed",
        message = error,
        onRetry = onRetry,
        modifier = modifier
    )
}
