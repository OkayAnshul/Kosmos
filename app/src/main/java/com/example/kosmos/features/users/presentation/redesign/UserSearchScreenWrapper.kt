package com.example.kosmos.features.users.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.users.presentation.UserSearchViewModel

/**
 * Wrapper for UserSearchScreen with Hilt dependency injection
 *
 * Responsibilities:
 * - Inject UserSearchViewModel via Hilt
 * - Collect UI state from ViewModel
 * - Delegate user actions to ViewModel
 */
@Composable
fun UserSearchScreenWrapper(
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UserSearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    UserSearchScreen(
        searchQuery = searchQuery,
        users = uiState.users,
        isLoading = uiState.isLoading,
        error = uiState.error,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onUserClick = onUserClick,
        onRetry = viewModel::retrySearch,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
