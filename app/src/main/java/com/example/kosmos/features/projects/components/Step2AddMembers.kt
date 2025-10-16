package com.example.kosmos.features.projects.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.User
import com.example.kosmos.features.project.presentation.SelectedMember
import com.example.kosmos.features.users.presentation.components.UserAvatar
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens.Spacing

/**
 * Step 2: Add Team Members
 *
 * Features:
 * - Search users by username (@username), display name, or email
 * - Recent collaborators section (horizontal chips)
 * - All users list with search results
 * - Selected members section (expandable card)
 * - Role assignment (MANAGER/MEMBER)
 * - Remove members
 * - Creator automatically added as ADMIN (cannot remove)
 *
 * Search behavior:
 * - Starts with @: Username search
 * - Contains @: Email search
 * - Otherwise: Display name search
 * - Debounced (300ms) to reduce queries
 *
 * @param selectedMembers Currently selected members (excluding owner)
 * @param recentCollaborators Recent collaborators for quick selection
 * @param allUsers All available users (filtered by search)
 * @param searchQuery Current search query
 * @param currentUserId ID of user creating project (owner)
 * @param onAddMember Callback to add member with role
 * @param onRemoveMember Callback to remove member by user ID
 * @param onUpdateMemberRole Callback to update member role
 * @param onSearchQueryChange Callback when search query changes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step2AddMembers(
    selectedMembers: List<SelectedMember>,
    recentCollaborators: List<User>,
    connectionUsers: List<User> = emptyList(),
    allUsers: List<User>,
    searchQuery: String,
    currentUserId: String,
    onAddMember: (User, ProjectRole) -> Unit,
    onRemoveMember: (String) -> Unit,
    onUpdateMemberRole: (String, ProjectRole) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    // Track whether selected members section is expanded
    var selectedMembersExpanded by remember { mutableStateOf(false) }

    val textFieldColors = wizardTextFieldColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Header
        Text(
            text = "Build Your Team",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground
        )

        Text(
            text = "Add members and assign roles. You can always add more members later.",
            style = MaterialTheme.typography.bodyMedium,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        HorizontalDivider(color = ColorTokens.ReactTheme.border)

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search users") },
            placeholder = { Text("Search by @username, name, or email") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = ColorTokens.ReactTheme.mutedForeground
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }
            },
            singleLine = true,
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Search hint
        if (searchQuery.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ColorTokens.ReactTheme.primary
                )
                Text(
                    text = when {
                        searchQuery.startsWith("@") -> "Searching usernames..."
                        searchQuery.contains("@") -> "Searching emails..."
                        else -> "Searching display names..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.primary
                )
            }
        }

        // Connections Section (shown first for easy selection)
        if (searchQuery.isEmpty() && connectionUsers.isNotEmpty()) {
            HorizontalDivider(color = ColorTokens.ReactTheme.border)

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Connections",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("Quick Add") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                Text(
                    text = "People you're connected with",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(connectionUsers) { user ->
                        val isSelected = selectedMembers.any { it.user.id == user.id }
                        RecentCollaboratorChip(
                            user = user,
                            isSelected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    onAddMember(user, ProjectRole.MEMBER)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Recent Collaborators Section (only show if no search query and has collaborators)
        if (searchQuery.isEmpty() && recentCollaborators.isNotEmpty()) {
            HorizontalDivider(color = ColorTokens.ReactTheme.border)

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Collaborators",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("Quick Add") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                Text(
                    text = "Users you've worked with recently",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentCollaborators) { collaborator ->
                        val isSelected = selectedMembers.any { it.user.id == collaborator.id }
                        RecentCollaboratorChip(
                            user = collaborator,
                            isSelected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    onAddMember(collaborator, ProjectRole.MEMBER)
                                }
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = ColorTokens.ReactTheme.border)

        // Selected Members Section (Expandable Card)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = ColorTokens.ReactTheme.card,
            border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
            onClick = { selectedMembersExpanded = !selectedMembersExpanded }
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = ColorTokens.ReactTheme.primary
                        )
                        Text(
                            text = "Selected Members",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorTokens.ReactTheme.foreground
                        )
                        Badge(
                            containerColor = ColorTokens.ReactTheme.primary,
                            contentColor = ColorTokens.ReactTheme.primaryForeground
                        ) {
                            Text("${selectedMembers.size + 1}") // +1 for owner
                        }
                    }

                    Icon(
                        imageVector = if (selectedMembersExpanded) Icons.Default.ExpandLess
                        else Icons.Default.ExpandMore,
                        contentDescription = if (selectedMembersExpanded) "Collapse" else "Expand",
                        tint = ColorTokens.ReactTheme.mutedForeground
                    )
                }

                AnimatedVisibility(
                    visible = selectedMembersExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        // Owner info (cannot be removed)
                        Text(
                            text = "You (Owner)",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = ColorTokens.ReactTheme.primary.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, ColorTokens.ReactTheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = ColorTokens.Priority.medium
                                    )
                                    Text(
                                        text = "You - ADMIN",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = ColorTokens.ReactTheme.foreground
                                    )
                                }
                                Text(
                                    text = "Creator",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorTokens.ReactTheme.mutedForeground
                                )
                            }
                        }

                        // Selected members
                        if (selectedMembers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "Team Members",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTokens.ReactTheme.mutedForeground,
                                fontWeight = FontWeight.Medium
                            )

                            selectedMembers.forEach { member ->
                                MemberCard(
                                    member = member,
                                    isOwner = false,
                                    onRoleChange = { newRole ->
                                        onUpdateMemberRole(member.user.id, newRole)
                                    },
                                    onRemove = { onRemoveMember(member.user.id) }
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "No team members added yet. Search and add users below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTokens.ReactTheme.mutedForeground,
                                modifier = Modifier.padding(vertical = Spacing.sm)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = ColorTokens.ReactTheme.border)

        // All Users / Search Results Section
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "Search Results" else "All Users",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )

                if (allUsers.isNotEmpty()) {
                    Text(
                        text = "${allUsers.size} user${if (allUsers.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Users list
            if (allUsers.isEmpty()) {
                // Empty state
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.md),
                    shape = RoundedCornerShape(12.dp),
                    color = ColorTokens.ReactTheme.card,
                    border = BorderStroke(1.dp, ColorTokens.ReactTheme.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = ColorTokens.ReactTheme.mutedForeground
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No users found" else "No users available",
                            style = MaterialTheme.typography.titleMedium,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty())
                                "Try a different search term"
                            else
                                "Users will appear here when available",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }
            } else {
                // Users list (limit to 10 items visible, rest scrollable)
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    allUsers.take(10).forEach { user ->
                        // Skip current user (they're already owner)
                        if (user.id == currentUserId) return@forEach

                        val isSelected = selectedMembers.any { it.user.id == user.id }

                        UserSearchResultCard(
                            user = user,
                            isSelected = isSelected,
                            onAddClick = { onAddMember(user, ProjectRole.MEMBER) }
                        )
                    }

                    if (allUsers.size > 10) {
                        Text(
                            text = "Showing 10 of ${allUsers.size} users. Use search to find specific users.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground,
                            modifier = Modifier.padding(vertical = Spacing.sm)
                        )
                    }
                }
            }
        }

        // Bottom info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = ColorTokens.ReactTheme.primary.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, ColorTokens.ReactTheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = ColorTokens.ReactTheme.primary
                )
                Column {
                    Text(
                        text = "About Roles",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.primary
                    )
                    Text(
                        text = "ADMIN: Full control \u2022 MANAGER: Manage tasks & members \u2022 MEMBER: View & contribute",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }
        }
    }
}

/**
 * User search result card with add button
 */
@Composable
private fun UserSearchResultCard(
    user: User,
    isSelected: Boolean,
    onAddClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) WizardColors.emerald.copy(alpha = 0.1f)
        else ColorTokens.ReactTheme.card,
        border = BorderStroke(
            1.dp,
            if (isSelected) WizardColors.emerald.copy(alpha = 0.3f)
            else ColorTokens.ReactTheme.border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isSelected, onClick = onAddClick)
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User info
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
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
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = ColorTokens.ReactTheme.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Online indicator
                if (user.isOnline) {
                    Badge(
                        containerColor = ColorTokens.Status.online
                    ) {
                        Text("Online", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Add/Selected indicator
            if (isSelected) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = WizardColors.emerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Added",
                        style = MaterialTheme.typography.labelMedium,
                        color = WizardColors.emerald,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add member",
                        tint = ColorTokens.ReactTheme.primary
                    )
                }
            }
        }
    }
}
