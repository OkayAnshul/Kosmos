package com.example.kosmos.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.User
import com.example.kosmos.features.users.presentation.components.UserAvatar
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * User Picker Dialog
 *
 * Reusable dialog for selecting a user from a list.
 * Features:
 * - Search functionality to filter users
 * - User avatars with online status
 * - User role display (optional)
 * - Empty state when no users found
 *
 * @param users List of users to display
 * @param title Dialog title
 * @param showRole Whether to show user roles
 * @param onUserSelected Callback when user is selected
 * @param onDismiss Callback when dialog is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPickerDialog(
    users: List<User>,
    title: String = "Select User",
    showRole: Boolean = false,
    onUserSelected: (User) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter users by search query
    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) {
            users
        } else {
            users.filter { user ->
                user.displayName.contains(searchQuery, ignoreCase = true) ||
                user.username.contains(searchQuery, ignoreCase = true) ||
                (user.email?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(Tokens.CornerRadius.lg),
            color = ColorTokens.ReactTheme.card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.lg)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = TypographyTokens.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ColorTokens.ReactTheme.foreground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Tokens.Spacing.md))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search users...",
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ColorTokens.ReactTheme.mutedForeground
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = ColorTokens.ReactTheme.mutedForeground
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(Tokens.CornerRadius.md),
                    colors = KosmosDialogDefaults.textFieldColors()
                )

                Spacer(modifier = Modifier.height(Tokens.Spacing.md))

                // User list
                if (filteredUsers.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Tokens.Spacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No users available" else "No users found",
                            style = TypographyTokens.typography.bodyMedium,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                    ) {
                        items(filteredUsers, key = { it.id }) { user ->
                            UserPickerItem(
                                user = user,
                                showRole = showRole,
                                onClick = { onUserSelected(user) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * User Picker Item
 * Individual user row in the picker dialog
 */
@Composable
private fun UserPickerItem(
    user: User,
    showRole: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        color = ColorTokens.ReactTheme.secondary,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar
            UserAvatar(
                photoUrl = user.photoUrl,
                displayName = user.displayName,
                isOnline = user.isOnline,
                size = 48.dp,
                showOnlineIndicator = true
            )

            // User info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
            ) {
                Text(
                    text = user.displayName,
                    style = TypographyTokens.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ColorTokens.ReactTheme.foreground
                )

                Text(
                    text = "@${user.username}",
                    style = TypographyTokens.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )

                // Show role if enabled
                if (showRole && user.role != null) {
                    Text(
                        text = user.role,
                        style = TypographyTokens.typography.labelSmall,
                        color = ColorTokens.ReactTheme.primary
                    )
                }
            }
        }
    }
}

