package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.features.projects.presentation.MembersListViewModel
import com.example.kosmos.features.tasks.presentation.redesign.TaskNetworkMonitorEntryPoint
import dagger.hilt.android.EntryPointAccessors

/**
 * Wrapper for MembersListScreen with Hilt dependency injection
 *
 * Responsibilities:
 * - Inject MembersListViewModel via Hilt
 * - Collect UI state from ViewModel
 * - Load members when screen opens
 * - Handle member management actions
 * - Wire offline detection via NetworkMonitor
 */
@Composable
fun MembersListScreenWrapper(
    projectId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MembersListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Get NetworkMonitor via EntryPoint
    val context = LocalContext.current
    val networkMonitor = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TaskNetworkMonitorEntryPoint::class.java
        ).networkMonitor()
    }
    val isOffline by networkMonitor.isOffline.collectAsState()

    // Load members when screen opens
    LaunchedEffect(projectId) {
        viewModel.loadMembers(projectId)
    }

    MembersListScreen(
        members = uiState.filteredMembers,
        searchQuery = uiState.searchQuery,
        selectedRoleFilter = uiState.selectedRoleFilter,
        currentUserRole = uiState.currentUserRole,
        isLoading = uiState.isLoading,
        isUpdating = uiState.isUpdating,
        error = uiState.error,
        successMessage = uiState.successMessage,
        isOffline = isOffline,
        onSearchQueryChange = viewModel::searchMembers,
        onRoleFilterChange = viewModel::filterByRole,
        onChangeMemberRole = { memberId, newRole ->
            viewModel.changeRole(projectId, memberId, newRole)
        },
        onRemoveMember = { memberId ->
            viewModel.removeMember(projectId, memberId)
        },
        onClearMessages = viewModel::clearMessages,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
