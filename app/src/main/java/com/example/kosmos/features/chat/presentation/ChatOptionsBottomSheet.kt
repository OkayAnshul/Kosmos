package com.example.kosmos.features.chat.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Chat Options Bottom Sheet
 * Shows options for managing a chat room: pin/unpin, archive/unarchive, delete
 *
 * This provides an alternative to swipe actions for users who prefer tap-based interaction
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatOptionsBottomSheet(
    chatName: String,
    isPinned: Boolean,
    isArchived: Boolean,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "Chat Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Text(
                text = chatName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pin/Unpin Option
            ChatOptionItem(
                icon = if (isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                label = if (isPinned) "Unpin Chat" else "Pin Chat",
                description = if (isPinned) "Remove from pinned section" else "Keep at the top of your chat list",
                onClick = {
                    onPin()
                    onDismiss()
                }
            )

            Divider(modifier = Modifier.padding(horizontal = 24.dp))

            // Archive/Unarchive Option
            ChatOptionItem(
                icon = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                label = if (isArchived) "Unarchive Chat" else "Archive Chat",
                description = if (isArchived) "Move back to active chats" else "Hide from your chat list",
                onClick = {
                    onArchive()
                    onDismiss()
                }
            )

            Divider(modifier = Modifier.padding(horizontal = 24.dp))

            // Delete Option (dangerous action)
            ChatOptionItem(
                icon = Icons.Default.Delete,
                label = "Delete Chat",
                description = "Permanently remove this chat and all messages",
                onClick = {
                    showDeleteConfirmation = true
                },
                isDestructive = true
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete Chat?")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete \"$chatName\"?")
                    Text(
                        "This action cannot be undone. All messages in this chat will be permanently deleted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Individual option item in the bottom sheet
 */
@Composable
private fun ChatOptionItem(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
