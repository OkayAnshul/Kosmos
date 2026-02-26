package com.example.kosmos.features.users.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.User
import com.example.kosmos.features.users.presentation.components.UserAvatar
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Invite Members Screen - Redesigned with Stitch Design
 *
 * Features:
 * - Search users
 * - Selected users chips at top
 * - Role selector (Admin/Manager/Member)
 * - "Send Invites" button
 *
 * Stitch Design Features:
 * - Navy background (#1A1D2E)
 * - Blue accent for selected items
 * - Modern card-based layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteMembersScreen(
    searchQuery: String,
    users: List<User>,
    selectedUsers: Set<String>,
    selectedRole: ProjectRole,
    isLoading: Boolean,
    isInviting: Boolean,
    error: String?,
    onSearchQueryChange: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onUserRemove: (String) -> Unit,
    onRoleChange: (ProjectRole) -> Unit,
    onSendInvites: () -> Unit,
    onClearSelection: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                                color = ColorTokens.ReactTheme.mutedForeground
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
                        TextButton(onClick = onClearSelection) {
                            Text("Clear")
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Send Invites Button
            if (selectedUsers.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorTokens.ReactTheme.card,
                    tonalElevation = 3.dp
                ) {
                    Button(
                        onClick = onSendInvites,
                        enabled = !isInviting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Tokens.Spacing.md),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorTokens.ReactTheme.primary
                        )
                    ) {
                        if (isInviting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                        }
                        Text("Send Invites")
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorTokens.ReactTheme.background)
                .padding(paddingValues)
        ) {
            // Selected Users Chips
            if (selectedUsers.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                ) {
                    items(
                        items = selectedUsers.toList(),
                        key = { it }
                    ) { userId ->
                        val user = users.find { it.id == userId }
                        if (user != null) {
                            InputChip(
                                selected = true,
                                onClick = { onUserRemove(userId) },
                                label = { Text(user.displayName) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = IconSet.Navigation.close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()
            }

            // Role Selector
            RoleSelector(
                selectedRole = selectedRole,
                onRoleChange = onRoleChange,
                modifier = Modifier.padding(Tokens.Spacing.md)
            )

            HorizontalDivider()

            // Search Bar
            SearchBarStandard(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Search users to invite...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.md)
            )

            // User List
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && users.isEmpty() -> {
                        LoadingIndicator(
                            message = "Loading users...",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    error != null -> {
                        ErrorState(
                            title = "Failed to load users",
                            message = error,
                            onRetry = {}, // TODO: Add retry
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    users.isEmpty() -> {
                        EmptyState(
                            icon = IconSet.Action.search,
                            title = "No users found",
                            message = "Search for users to invite to this project",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = Tokens.Spacing.md,
                                vertical = Tokens.Spacing.xs
                            )
                        ) {
                            items(
                                items = users,
                                key = { it.id }
                            ) { user ->
                                InviteUserItem(
                                    user = user,
                                    isSelected = selectedUsers.contains(user.id),
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

/**
 * Role Selector
 */
@Composable
private fun RoleSelector(
    selectedRole: ProjectRole,
    onRoleChange: (ProjectRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Select Role",
            style = TypographyTokens.typography.labelLarge,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
        ) {
            ProjectRole.values().forEach { role ->
                FilterChip(
                    selected = selectedRole == role,
                    onClick = { onRoleChange(role) },
                    label = {
                        Text(
                            text = role.name.lowercase().replaceFirstChar { it.uppercase() }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Invite User Item
 */
@Composable
private fun InviteUserItem(
    user: User,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            ColorTokens.ReactTheme.primary.copy(alpha = 0.1f)
        } else {
            ColorTokens.ReactTheme.card
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                UserAvatar(
                    photoUrl = user.photoUrl,
                    displayName = user.displayName,
                    isOnline = user.isOnline,
                    size = 40.dp
                )

                Column {
                    Text(
                        text = user.displayName,
                        style = TypographyTokens.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = ColorTokens.ReactTheme.foreground
                    )
                    Text(
                        text = "@${user.username}",
                        style = TypographyTokens.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }

            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = null // Click handled by Surface
            )
        }
    }
}
