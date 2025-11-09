package com.example.kosmos.features.chat.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kosmos.core.models.User
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Create Chat Dialog - Multi-User Selection for Group Chats
 *
 * Flow: Search users → Select multiple → Enter chat name (if >1 people) → Create
 * Supports both direct chats (1 person) and group chats (multiple people)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatDialog(
    projectMembers: List<User>,
    onDismiss: () -> Unit,
    onCreate: (chatName: String?, selectedUserIds: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedUsers by remember { mutableStateOf(setOf<User>()) }
    var chatName by remember { mutableStateOf("") }
    var showNameInput by remember { mutableStateOf(false) }

    // Filter members based on search
    val filteredMembers = remember(projectMembers, searchQuery) {
        if (searchQuery.isBlank()) {
            projectMembers
        } else {
            projectMembers.filter { user ->
                user.displayName.contains(searchQuery, ignoreCase = true) ||
                user.username.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = Tokens.Elevation.level3
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (showNameInput) "Name Your Group" else "New Chat",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedUsers.isNotEmpty() && !showNameInput) {
                            Text(
                                text = "${selectedUsers.size} selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(IconSet.Navigation.close, "Close")
                    }
                }

                HorizontalDivider()

                if (!showNameInput) {
                    // Step 1: User Selection
                    Column(modifier = Modifier.weight(1f)) {
                        // Search bar
                        SearchBarStandard(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Search members...",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Tokens.Spacing.md)
                        )

                        // Selected users chips (horizontal scroll)
                        if (selectedUsers.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                            ) {
                                selectedUsers.forEach { user ->
                                    FilterChip(
                                        selected = true,
                                        onClick = { selectedUsers = selectedUsers - user },
                                        label = { Text(user.displayName) },
                                        trailingIcon = {
                                            Icon(
                                                IconSet.Navigation.close,
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(Tokens.Size.iconSmall)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // User list
                        if (filteredMembers.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyState(
                                    icon = IconSet.Action.search,
                                    title = "No users found",
                                    message = if (searchQuery.isBlank())
                                        "No project members available"
                                    else
                                        "No results for \"$searchQuery\""
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = Tokens.Spacing.xs)
                            ) {
                                items(
                                    items = filteredMembers,
                                    key = { it.id }
                                ) { user ->
                                    UserSelectionItem(
                                        user = user,
                                        isSelected = user in selectedUsers,
                                        onToggle = {
                                            selectedUsers = if (user in selectedUsers) {
                                                selectedUsers - user
                                            } else {
                                                selectedUsers + user
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Step 2: Chat Name Input (for groups)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(Tokens.Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
                    ) {
                        Text(
                            text = "Creating group with ${selectedUsers.size} members",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = chatName,
                            onValueChange = { chatName = it },
                            label = { Text("Group Name") },
                            placeholder = { Text("Enter a name for the group") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(
                            text = "Members: ${selectedUsers.joinToString(", ") { it.displayName }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                // Bottom actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                ) {
                    if (showNameInput) {
                        OutlinedButton(
                            onClick = { showNameInput = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                    }

                    Button(
                        onClick = {
                            if (!showNameInput && selectedUsers.size > 1) {
                                // Group chat needs a name
                                showNameInput = true
                            } else {
                                // Direct chat or named group
                                val finalName = if (selectedUsers.size == 1) {
                                    null // Direct chat uses user's name
                                } else {
                                    chatName.ifBlank {
                                        selectedUsers.joinToString(", ") { it.displayName.split(" ").first() }
                                    }
                                }
                                onCreate(finalName, selectedUsers.map { it.id })
                            }
                        },
                        enabled = selectedUsers.isNotEmpty() &&
                                  (!showNameInput || chatName.isNotBlank()),
                        modifier = Modifier.weight(if (showNameInput) 1f else 1f)
                    ) {
                        Icon(
                            imageVector = IconSet.Message.send,
                            contentDescription = null,
                            modifier = Modifier.size(Tokens.Size.iconSmall)
                        )
                        Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                        Text(
                            if (showNameInput) "Create Group"
                            else if (selectedUsers.size == 1) "Start Chat"
                            else "Continue"
                        )
                    }
                }
            }
        }
    }
}

/**
 * User Selection Item with Checkbox
 */
@Composable
private fun UserSelectionItem(
    user: User,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )

            // User avatar placeholder
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    IconSet.Status.checkmark,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
