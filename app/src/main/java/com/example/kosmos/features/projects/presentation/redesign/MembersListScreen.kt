package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.features.projects.components.ChangeRoleDialog
import com.example.kosmos.features.projects.components.RemoveMemberDialog
import com.example.kosmos.features.projects.presentation.MemberWithUser
import com.example.kosmos.features.users.presentation.components.UserAvatar
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.layouts.ScreenScaffoldStandard

/**
 * Members List Screen - Redesigned to Match Reference Design
 *
 * Reference Design Features:
 * - Offline banner: "You are offline. Showing cached data."
 * - Search box: "Search by name or role..."
 * - Role filter tabs: All | Administrators | Contributors | Viewers
 * - Sectioned list by role:
 *   - ADMINISTRATORS section
 *   - CONTRIBUTORS section
 *   - VIEWERS section
 * - Each member item:
 *   - Avatar (circular)
 *   - Name (white, bold)
 *   - Username (@username, secondary)
 *   - Role badge (color-coded)
 *   - Online status (green dot)
 *   - More menu (...)
 *   - Last seen text (if offline)
 *
 * Stitch Design Features:
 * - Navy background (#1A1D2E)
 * - Card-based layout
 * - Blue accent for active filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersListScreen(
    members: List<MemberWithUser>,
    searchQuery: String,
    selectedRoleFilter: ProjectRole?,
    currentUserRole: ProjectRole,
    isLoading: Boolean,
    isUpdating: Boolean,
    error: String?,
    successMessage: String?,
    isOffline: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onRoleFilterChange: (ProjectRole?) -> Unit,
    onChangeMemberRole: (String, ProjectRole) -> Unit,
    onRemoveMember: (String) -> Unit,
    onClearMessages: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMemberForRoleChange by remember { mutableStateOf<MemberWithUser?>(null) }

    // Snackbar for success/error messages
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedMemberForRemoval by remember { mutableStateOf<MemberWithUser?>(null) }
    var showMoreMenuForMember by remember { mutableStateOf<String?>(null) }

    ScreenScaffoldStandard(
        title = "Members (${members.size})",
        onNavigationClick = onNavigateBack,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorTokens.ReactTheme.background)
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    LoadingIndicator(
                        message = "Loading members...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    ErrorState(
                        title = "Failed to load members",
                        message = error,
                        onRetry = { /* Retry handled by wrapper */ },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Offline Banner
                        if (isOffline) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = ColorTokens.ReactTheme.card
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Tokens.Spacing.md),
                                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = IconSet.Status.offline,
                                        contentDescription = "",
                                        tint = ColorTokens.ReactTheme.mutedForeground,
                                        modifier = Modifier.size(Tokens.Size.iconSmall)
                                    )
                                    Text(
                                        text = "You are offline. Showing cached data.",
                                        style = TypographyTokens.typography.bodySmall,
                                        color = ColorTokens.ReactTheme.mutedForeground
                                    )
                                }
                            }
                        }

                        // Search Bar
                        SearchBarStandard(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            placeholder = "Search by name or role...",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Tokens.Spacing.md)
                        )

                        // Role Filter Tabs
                        RoleFilterTabs(
                            selectedRole = selectedRoleFilter,
                            onRoleSelect = onRoleFilterChange,
                            modifier = Modifier.padding(horizontal = Tokens.Spacing.md)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = Tokens.Spacing.sm))

                        // Members List (Sectioned by Role)
                        if (members.isEmpty()) {
                            EmptyState(
                                icon = IconSet.User.group,
                                title = "No members found",
                                message = if (searchQuery.isNotBlank()) {
                                    "Try a different search query"
                                } else {
                                    "No members in this project yet"
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(Tokens.Spacing.xl)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = Tokens.Spacing.md,
                                    vertical = Tokens.Spacing.xs
                                )
                            ) {
                                // Group members by role
                                val groupedMembers = members.groupBy { it.member.role }

                                // ADMINISTRATORS section
                                groupedMembers[ProjectRole.ADMIN]?.let { adminMembers ->
                                    item {
                                        SectionHeader(
                                            title = "ADMINISTRATORS",
                                            count = adminMembers.size
                                        )
                                    }
                                    items(
                                        items = adminMembers,
                                        key = { it.member.id }
                                    ) { memberWithUser ->
                                        MemberItem(
                                            memberWithUser = memberWithUser,
                                            currentUserRole = currentUserRole,
                                            isUpdating = isUpdating,
                                            onMoreClick = { showMoreMenuForMember = memberWithUser.member.id },
                                            onChangeRole = { selectedMemberForRoleChange = memberWithUser },
                                            onRemove = { selectedMemberForRemoval = memberWithUser },
                                            modifier = Modifier.padding(vertical = Tokens.Spacing.xxs)
                                        )
                                    }
                                }

                                // CONTRIBUTORS section (Managers + Members)
                                val contributorMembers = listOfNotNull(
                                    groupedMembers[ProjectRole.MANAGER],
                                    groupedMembers[ProjectRole.MEMBER]
                                ).flatten()

                                if (contributorMembers.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            title = "CONTRIBUTORS",
                                            count = contributorMembers.size
                                        )
                                    }
                                    items(
                                        items = contributorMembers,
                                        key = { it.member.id }
                                    ) { memberWithUser ->
                                        MemberItem(
                                            memberWithUser = memberWithUser,
                                            currentUserRole = currentUserRole,
                                            isUpdating = isUpdating,
                                            onMoreClick = { showMoreMenuForMember = memberWithUser.member.id },
                                            onChangeRole = { selectedMemberForRoleChange = memberWithUser },
                                            onRemove = { selectedMemberForRemoval = memberWithUser },
                                            modifier = Modifier.padding(vertical = Tokens.Spacing.xxs)
                                        )
                                    }
                                }

                                // Note: ProjectRole.VIEWER doesn't exist in the model
                                // All non-Admin, non-Manager, non-Member roles are implicitly viewers

                                // Bottom spacing
                                item {
                                    Spacer(modifier = Modifier.height(Tokens.Spacing.xl))
                                }
                            }
                        }
                    }
                }
            }

            // Show snackbar for success messages
            successMessage?.let { message ->
                LaunchedEffect(message) {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                    onClearMessages()
                }
            }

            // Show snackbar for errors
            error?.let { errorMsg ->
                LaunchedEffect(errorMsg) {
                    snackbarHostState.showSnackbar(
                        message = errorMsg,
                        duration = SnackbarDuration.Short
                    )
                    onClearMessages()
                }
            }

            // Snackbar host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Change Role Dialog
    selectedMemberForRoleChange?.let { memberWithUser ->
        ChangeRoleDialog(
            member = memberWithUser.member,
            memberName = memberWithUser.user.displayName,
            currentRole = memberWithUser.member.role,
            onRoleSelected = { newRole ->
                onChangeMemberRole(memberWithUser.member.id, newRole)
            },
            onDismiss = { selectedMemberForRoleChange = null }
        )
    }

    // Remove Member Dialog
    selectedMemberForRemoval?.let { memberWithUser ->
        RemoveMemberDialog(
            member = memberWithUser.member,
            memberName = memberWithUser.user.displayName,
            onConfirm = {
                onRemoveMember(memberWithUser.member.id)
            },
            onDismiss = { selectedMemberForRemoval = null }
        )
    }
}

/**
 * Role Filter Tabs
 */
@Composable
private fun RoleFilterTabs(
    selectedRole: ProjectRole?,
    onRoleSelect: (ProjectRole?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
    ) {
        // All
        FilterChip(
            selected = selectedRole == null,
            onClick = { onRoleSelect(null) },
            label = { Text("All") },
            modifier = Modifier.weight(1f)
        )

        // Administrators
        FilterChip(
            selected = selectedRole == ProjectRole.ADMIN,
            onClick = { onRoleSelect(ProjectRole.ADMIN) },
            label = { Text("Administrators") },
            modifier = Modifier.weight(1f)
        )

        // Contributors (Manager + Member)
        FilterChip(
            selected = selectedRole == ProjectRole.MANAGER || selectedRole == ProjectRole.MEMBER,
            onClick = {
                // Toggle between Manager and null
                if (selectedRole == ProjectRole.MANAGER) {
                    onRoleSelect(null)
                } else {
                    onRoleSelect(ProjectRole.MANAGER)
                }
            },
            label = { Text("Contributors") },
            modifier = Modifier.weight(1f)
        )

        // Note: VIEWER role removed - only ADMIN, MANAGER, MEMBER exist
    }
}

/**
 * Section Header
 */
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$title ($count)",
        style = TypographyTokens.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold
        ),
        color = ColorTokens.ReactTheme.mutedForeground,
        modifier = modifier.padding(
            vertical = Tokens.Spacing.sm,
            horizontal = Tokens.Spacing.xs
        )
    )
}

/**
 * Member Item
 */
@Composable
private fun MemberItem(
    memberWithUser: MemberWithUser,
    currentUserRole: ProjectRole,
    isUpdating: Boolean,
    onMoreClick: () -> Unit,
    onChangeRole: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ColorTokens.ReactTheme.card
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
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
                modifier = Modifier.weight(1f)
            ) {
                // Avatar with online indicator
                UserAvatar(
                    photoUrl = memberWithUser.user.photoUrl,
                    displayName = memberWithUser.user.displayName,
                    isOnline = memberWithUser.user.isOnline,
                    size = 40.dp,
                    showOnlineIndicator = true
                )

                Column(modifier = Modifier.weight(1f)) {
                    // Name
                    Text(
                        text = memberWithUser.user.displayName,
                        style = TypographyTokens.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = ColorTokens.ReactTheme.foreground
                    )

                    // Username
                    Text(
                        text = "@${memberWithUser.user.username}",
                        style = TypographyTokens.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )

                    // Last seen (if offline)
                    if (!memberWithUser.user.isOnline) {
                        Text(
                            text = "Last seen ${getRelativeTime(memberWithUser.user.lastSeen)}",
                            style = TypographyTokens.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                // Role badge
                RoleBadge(role = memberWithUser.member.role)

                // More menu (only for admins)
                if (currentUserRole == ProjectRole.ADMIN) {
                    Box {
                        IconButtonStandard(
                            icon = IconSet.Action.moreVert,
                            onClick = { showMenu = true },
                            contentDescription = "More options"
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Role") },
                                onClick = {
                                    showMenu = false
                                    onChangeRole()
                                },
                                leadingIcon = {
                                    Icon(IconSet.Action.edit, contentDescription = "")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove Member") },
                                onClick = {
                                    showMenu = false
                                    onRemove()
                                },
                                leadingIcon = {
                                    Icon(IconSet.Action.delete, contentDescription = "")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Role Badge
 */
@Composable
private fun RoleBadge(
    role: ProjectRole,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (role) {
        ProjectRole.ADMIN -> ColorTokens.ReactTheme.destructive
        ProjectRole.MANAGER -> ColorTokens.ReactTheme.primary
        ProjectRole.MEMBER -> ColorTokens.Status.online
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = backgroundColor.copy(alpha = 0.2f)
    ) {
        Text(
            text = role.name.lowercase().replaceFirstChar { it.uppercase() },
            style = TypographyTokens.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = backgroundColor,
            modifier = Modifier.padding(
                horizontal = Tokens.Spacing.sm,
                vertical = Tokens.Spacing.xxs
            )
        )
    }
}

/**
 * Get relative time string
 */
private fun getRelativeTime(timestamp: Long?): String {
    if (timestamp == null) return "recently"

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> "${diff / 604800_000}w ago"
    }
}
