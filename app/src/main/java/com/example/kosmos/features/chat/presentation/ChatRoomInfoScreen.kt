package com.example.kosmos.features.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.components.KosmosCard
import com.example.kosmos.shared.ui.components.SectionCard
import com.example.kosmos.shared.ui.components.DestructiveButton
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Chat Room Info Screen
 *
 * Full-screen view for chat room details, members, and settings.
 * Replaces the ChatOptionsBottomSheet for a more comprehensive info view.
 */

data class ChatMemberInfo(
    val id: String,
    val displayName: String,
    val username: String,
    val role: String = "member"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomInfoScreen(
    chatRoomName: String,
    memberCount: Int,
    createdAt: String? = null,
    members: List<ChatMemberInfo> = emptyList(),
    isCurrentUserAdmin: Boolean = false,
    isPinned: Boolean = false,
    isArchived: Boolean = false,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onAddMember: () -> Unit = {},
    onRemoveMember: (String) -> Unit = {},
    onTogglePin: () -> Unit = {},
    onToggleArchive: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ColorTokens.ReactTheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chat Info",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorTokens.ReactTheme.background,
                    titleContentColor = ColorTokens.ReactTheme.foreground,
                    navigationIconContentColor = ColorTokens.ReactTheme.mutedForeground
                )
            )
        },
        floatingActionButton = {
            if (isCurrentUserAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onEdit,
                    containerColor = ColorTokens.ReactTheme.primary,
                    contentColor = ColorTokens.ReactTheme.primaryForeground
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                    Text("Edit")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Card
            item {
                KosmosCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                    ) {
                        // Room avatar
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chatRoomName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorTokens.ReactTheme.primary
                            )
                        }

                        Text(
                            text = chatRoomName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorTokens.ReactTheme.foreground
                        )

                        Text(
                            text = "$memberCount member${if (memberCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )

                        if (createdAt != null) {
                            Text(
                                text = "Created $createdAt",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                    }
                }
            }

            // Members Section
            item {
                SectionCard(title = "MEMBERS ($memberCount)") {
                    members.forEach { member ->
                        MemberRow(
                            member = member,
                            canRemove = isCurrentUserAdmin && member.role != "owner",
                            onRemove = { onRemoveMember(member.id) }
                        )
                    }

                    if (isCurrentUserAdmin) {
                        OutlinedButton(
                            onClick = onAddMember,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ColorTokens.ReactTheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                            Text("Add Member")
                        }
                    }
                }
            }

            // Settings Section
            item {
                SectionCard(title = "SETTINGS") {
                    // Pin toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPinned) "Pinned" else "Pin Chat",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ColorTokens.ReactTheme.foreground
                            )
                            Text(
                                text = "Keep at the top of your chat list",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                        Switch(
                            checked = isPinned,
                            onCheckedChange = { onTogglePin() },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = ColorTokens.ReactTheme.primary,
                                checkedThumbColor = ColorTokens.ReactTheme.primaryForeground
                            )
                        )
                    }

                    HorizontalDivider(color = ColorTokens.ReactTheme.border)

                    // Archive toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArchived) "Archived" else "Archive Chat",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ColorTokens.ReactTheme.foreground
                            )
                            Text(
                                text = "Move to archived chats",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                        Switch(
                            checked = isArchived,
                            onCheckedChange = { onToggleArchive() },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = ColorTokens.ReactTheme.primary,
                                checkedThumbColor = ColorTokens.ReactTheme.primaryForeground
                            )
                        )
                    }
                }
            }

            // Danger Zone
            if (isCurrentUserAdmin) {
                item {
                    SectionCard(title = "DANGER ZONE") {
                        DestructiveButton(
                            text = "Delete Chat Room",
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Bottom spacer for FAB
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Chat Room?") },
            text = { Text("This will permanently delete \"$chatRoomName\" and all its messages. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ColorTokens.ReactTheme.destructive
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
            containerColor = ColorTokens.ReactTheme.card,
            titleContentColor = ColorTokens.ReactTheme.foreground,
            textContentColor = ColorTokens.ReactTheme.mutedForeground
        )
    }
}

@Composable
private fun MemberRow(
    member: ChatMemberInfo,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Tokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ColorTokens.ReactTheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.displayName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = ColorTokens.ReactTheme.primaryForeground
            )
        }

        // Name + role
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = ColorTokens.ReactTheme.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${member.username}",
                style = MaterialTheme.typography.bodySmall,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }

        // Role chip
        if (member.role != "member") {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (member.role) {
                    "owner" -> ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
                    "admin" -> ColorTokens.ReactTheme.accent.copy(alpha = 0.15f)
                    else -> ColorTokens.ReactTheme.secondary
                }
            ) {
                Text(
                    text = member.role.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (member.role) {
                        "owner" -> ColorTokens.ReactTheme.primary
                        "admin" -> ColorTokens.ReactTheme.accent
                        else -> ColorTokens.ReactTheme.mutedForeground
                    },
                    modifier = Modifier.padding(horizontal = Tokens.Spacing.sm, vertical = Tokens.Spacing.xxs)
                )
            }
        }

        // Remove button
        if (canRemove) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove ${member.displayName}",
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
