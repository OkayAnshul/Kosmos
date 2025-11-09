package com.example.kosmos.features.users.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.User
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.components.EmptyState
import com.example.kosmos.shared.ui.components.ErrorState
import com.example.kosmos.shared.ui.components.LoadingIndicator
import com.example.kosmos.shared.ui.components.SearchBarStandard
import com.example.kosmos.shared.ui.components.IconButtonStandard

/**
 * Invite Members Screen
 * Allows bulk selection and invitation of members to a project
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteMembersScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InviteMembersViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedUsers by viewModel.selectedUsers.collectAsStateWithLifecycle()
    val selectedRole by viewModel.selectedRole.collectAsStateWithLifecycle()

    // Load project members when screen opens
    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
        viewModel.loadExistingMembers()
    }

    // Handle successful invitation
    LaunchedEffect(uiState.invitationSuccess) {
        if (uiState.invitationSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Invite Members")
                        if (selectedUsers.isNotEmpty()) {
                            Text(
                                text = "${selectedUsers.size} selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButtonStandard(
                        icon = IconSet.Navigation.back,
                        onClick = onNavigateBack,
                        contentDescription = "Back"
                    )
                },
                actions = {
                    if (selectedUsers.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearSelection() }
                        ) {
                            Text("Clear")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedUsers.isNotEmpty()) {
                InviteBottomBar(
                    selectedCount = selectedUsers.size,
                    selectedRole = selectedRole,
                    onRoleChange = viewModel::setRole,
                    onInviteClick = viewModel::inviteMembers,
                    isInviting = uiState.isInviting
                )
            }
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }

                uiState.error != null -> {
                    uiState.error?.let { error ->
                        ErrorState(
                            title = "Error",
                            message = error,
                            onRetry = viewModel::retrySearch
                        )
                    }
                }

                uiState.users.isEmpty() && searchQuery.isBlank() -> {
                    EmptyState(
                        icon = IconSet.User.personAdd,
                        title = "Search for users to invite",
                        message = "Enter a name or email to find users"
                    )
                }

                uiState.users.isEmpty() && searchQuery.isNotBlank() -> {
                    EmptyState(
                        icon = IconSet.Action.search,
                        title = "No users found",
                        message = "No results for \"$searchQuery\""
                    )
                }

                else -> {
                    UserResultsList(
                        users = uiState.users,
                        selectedUserIds = selectedUsers.map { it.id }.toSet(),
                        existingMemberIds = uiState.existingMemberIds,
                        onUserToggle = viewModel::toggleUserSelection
                    )
                }
            }
        }
    }
}

/**
 * Bottom bar with role selection and invite button
 */
@Composable
private fun InviteBottomBar(
    selectedCount: Int,
    selectedRole: ProjectRole,
    onRoleChange: (ProjectRole) -> Unit,
    onInviteClick: () -> Unit,
    isInviting: Boolean
) {
    Surface(
        tonalElevation = Tokens.Elevation.level2,
        shadowElevation = Tokens.Elevation.level4
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            // Role selection
            Text(
                text = "Select role for invited members:",
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
            ) {
                FilterChip(
                    selected = selectedRole == ProjectRole.MEMBER,
                    onClick = { onRoleChange(ProjectRole.MEMBER) },
                    label = { Text("Member") },
                    enabled = !isInviting
                )
                FilterChip(
                    selected = selectedRole == ProjectRole.MANAGER,
                    onClick = { onRoleChange(ProjectRole.MANAGER) },
                    label = { Text("Manager") },
                    enabled = !isInviting
                )
            }

            // Note about admin
            Text(
                text = "Note: ADMIN role can only be assigned by other admins in project settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Invite button
            Button(
                onClick = onInviteClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isInviting
            ) {
                if (isInviting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Tokens.Size.iconSmall),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                }
                Text("Invite $selectedCount member${if (selectedCount != 1) "s" else ""}")
            }
        }
    }
}

/**
 * User Results List with multi-select
 */
@Composable
private fun UserResultsList(
    users: List<User>,
    selectedUserIds: Set<String>,
    existingMemberIds: Set<String>,
    onUserToggle: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Results count header
        Text(
            text = "${users.size} user${if (users.size != 1) "s" else ""} found",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.xs)
        )

        // User list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = Tokens.Spacing.xs)
        ) {
            items(
                items = users,
                key = { user -> user.id }
            ) { user ->
                val isExistingMember = user.id in existingMemberIds
                val isSelected = user.id in selectedUserIds

                UserSelectionItem(
                    user = user,
                    isSelected = isSelected,
                    isExistingMember = isExistingMember,
                    onToggle = { onUserToggle(user) }
                )
            }
        }
    }
}

/**
 * User item with checkbox for selection
 */
@Composable
private fun UserSelectionItem(
    user: User,
    isSelected: Boolean,
    isExistingMember: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = { if (!isExistingMember) onToggle() },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isExistingMember
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected || isExistingMember,
                onCheckedChange = { if (!isExistingMember) onToggle() },
                enabled = !isExistingMember
            )

            Spacer(modifier = Modifier.width(Tokens.Spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
                Text(
                    text = if (isExistingMember) "Already a member" else "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isExistingMember)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExistingMember) {
                Icon(
                    IconSet.Status.success,
                    contentDescription = "Already member",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

