package com.example.kosmos.shared.ui.layouts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kosmos.shared.ui.components.EmptyState
import com.example.kosmos.shared.ui.components.ErrorState
import com.example.kosmos.shared.ui.components.LoadingIndicator
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * List Layout Components
 *
 * Reusable list patterns with loading, error, and empty states.
 * Includes pull-to-refresh and pagination support.
 *
 * Components:
 * - StandardList: Basic LazyColumn with state handling
 * - RefreshableList: List with pull-to-refresh
 * - PaginatedList: List with load more on scroll
 * - StatefulList: List with loading/error/empty states
 */

/**
 * Standard List
 *
 * Basic scrollable list with consistent padding
 *
 * @param modifier Modifier
 * @param state LazyListState for scroll control
 * @param contentPadding Content padding
 * @param verticalArrangement Vertical arrangement
 * @param content List content
 */
@Composable
fun StandardList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(Tokens.Spacing.md),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Tokens.Spacing.sm),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

/**
 * Refreshable List
 *
 * List with pull-to-refresh functionality
 * Common pattern for data that needs manual refresh
 *
 * @param isRefreshing Whether refresh is in progress
 * @param onRefresh Refresh handler
 * @param modifier Modifier
 * @param state LazyListState
 * @param contentPadding Content padding
 * @param content List content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableList(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(Tokens.Spacing.md),
    content: LazyListScope.() -> Unit
) {
    // Simplified implementation without pull-to-refresh for now
    // TODO: Implement proper Material3 pull-to-refresh when API stabilizes
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
            content = content
        )

        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}

/**
 * Paginated List
 *
 * List that loads more items when scrolled to bottom
 * Automatically triggers load more when near end
 *
 * @param isLoadingMore Whether loading more items
 * @param hasMore Whether more items are available
 * @param onLoadMore Load more handler
 * @param modifier Modifier
 * @param state LazyListState
 * @param contentPadding Content padding
 * @param loadMoreThreshold Items from end to trigger load (default: 3)
 * @param content List content
 */
@Composable
fun PaginatedList(
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(Tokens.Spacing.md),
    loadMoreThreshold: Int = 3,
    content: LazyListScope.() -> Unit
) {
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            hasMore && !isLoadingMore && lastVisibleItem >= totalItems - loadMoreThreshold
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onLoadMore()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        content()

        // Loading more indicator at bottom
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Tokens.Size.progressMedium)
                    )
                }
            }
        }
    }
}

/**
 * Stateful List
 *
 * List that handles loading, error, and empty states automatically
 * Reduces boilerplate for common list scenarios
 *
 * @param state List state (Loading, Success, Error, Empty)
 * @param modifier Modifier
 * @param listState LazyListState
 * @param contentPadding Content padding
 * @param emptyTitle Empty state title
 * @param emptyMessage Empty state message
 * @param emptyActionLabel Empty state action button label
 * @param onEmptyAction Empty state action handler
 * @param errorTitle Error title
 * @param errorMessage Error message
 * @param onRetry Error retry handler
 * @param loadingMessage Loading message
 * @param content List content (only shown in Success state)
 */
@Composable
fun <T> StatefulList(
    state: ListState<T>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(Tokens.Spacing.md),
    emptyTitle: String = "No items",
    emptyMessage: String? = null,
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    errorTitle: String = "Something went wrong",
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    loadingMessage: String? = null,
    content: LazyListScope.(List<T>) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            is ListState.Loading -> {
                LoadingIndicator(
                    message = loadingMessage,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is ListState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyState(
                        title = emptyTitle,
                        message = emptyMessage,
                        actionLabel = emptyActionLabel,
                        onActionClick = onEmptyAction,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                    ) {
                        content(state.data)
                    }
                }
            }

            is ListState.Error -> {
                ErrorState(
                    title = errorTitle,
                    message = errorMessage ?: state.message,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * List State Sealed Class
 *
 * Represents different states of a list
 */
sealed class ListState<out T> {
    object Loading : ListState<Nothing>()
    data class Success<T>(val data: List<T>) : ListState<T>()
    data class Error(val message: String) : ListState<Nothing>()
}

/**
 * Refreshable Stateful List
 *
 * Combines stateful list with pull-to-refresh
 *
 * @param state List state
 * @param isRefreshing Whether refresh is in progress
 * @param onRefresh Refresh handler
 * @param modifier Modifier
 * @param listState LazyListState
 * @param contentPadding Content padding
 * @param emptyTitle Empty state title
 * @param emptyMessage Empty state message
 * @param emptyActionLabel Empty state action label
 * @param onEmptyAction Empty state action handler
 * @param errorTitle Error title
 * @param errorMessage Error message
 * @param onRetry Retry handler
 * @param content List content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> RefreshableStatefulList(
    state: ListState<T>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(Tokens.Spacing.md),
    emptyTitle: String = "No items",
    emptyMessage: String? = null,
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    errorTitle: String = "Something went wrong",
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    content: LazyListScope.(List<T>) -> Unit
) {
    // Simplified implementation without pull-to-refresh
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            is ListState.Loading -> {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is ListState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyState(
                        title = emptyTitle,
                        message = emptyMessage,
                        actionLabel = emptyActionLabel,
                        onActionClick = onEmptyAction,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                    ) {
                        content(state.data)
                    }
                }
            }

            is ListState.Error -> {
                ErrorState(
                    title = errorTitle,
                    message = errorMessage ?: state.message,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}

/**
 * Grid Layout
 *
 * Simple grid layout for cards/items
 * Alternative to LazyVerticalGrid for simple cases
 *
 * @param columns Number of columns
 * @param modifier Modifier
 * @param contentPadding Content padding
 * @param horizontalSpacing Horizontal spacing between items
 * @param verticalSpacing Vertical spacing between rows
 * @param content Grid content
 */
@Composable
fun SimpleGrid(
    columns: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Tokens.Spacing.md),
    horizontalSpacing: androidx.compose.ui.unit.Dp = Tokens.Spacing.sm,
    verticalSpacing: androidx.compose.ui.unit.Dp = Tokens.Spacing.sm,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
        ) {
            repeat(columns) {
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        }
    }
}

/**
 * Sticky Header List
 *
 * List with sticky section headers
 * Headers remain visible while scrolling through sections
 *
 * Note: For production use, consider using LazyColumn with stickyHeader()
 * This is a simplified wrapper
 *
 * @param modifier Modifier
 * @param state LazyListState
 * @param contentPadding Content padding
 * @param content List content with stickyHeader support
 */
@Composable
fun StickyHeaderList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(Tokens.Spacing.md),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
        content = content
    )
}
