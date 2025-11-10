package com.example.kosmos.features.tasks.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Activity Log Screen Wrapper
 *
 * Hilt DI wrapper for ActivityLogScreen.
 * Connects ActivityLogViewModel to UI and handles state collection.
 */
@Composable
fun ActivityLogScreenWrapper(
    projectId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityLogViewModel = hiltViewModel()
) {
    // Load activity log on composition
    LaunchedEffect(projectId) {
        viewModel.loadActivityLog(projectId)
    }

    // Collect UI state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ActivityLogScreen(
        activities = uiState.activities,
        availableUsers = uiState.availableUsers,
        isLoading = uiState.isLoading,
        isLoadingMore = uiState.isLoadingMore,
        hasMore = uiState.hasMore,
        error = uiState.error,
        filterSummary = viewModel.getFilterSummary(),
        onSearchQueryChange = viewModel::updateSearchQuery,
        onFilterByActionType = viewModel::filterByActionType,
        onFilterByUser = viewModel::filterByUser,
        onClearFilters = viewModel::clearFilters,
        onLoadMore = viewModel::loadMore,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
