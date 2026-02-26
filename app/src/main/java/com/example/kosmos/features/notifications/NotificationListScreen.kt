package com.example.kosmos.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import java.text.SimpleDateFormat
import java.util.*

/**
 * Notification List Screen
 *
 * Displays all notifications for the current user with Navy Stitch design.
 * Features:
 * - List of notifications (unread first)
 * - Swipe to delete
 * - Mark as read on tap
 * - Mark all as read button
 * - Clear all button
 * - Pull to refresh
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    onNavigateBack: () -> Unit,
    onNotificationTap: (notificationId: String, data: Map<String, String>) -> Unit = { _, _ -> },
    onNavigateToConnections: () -> Unit = {},
    viewModel: NotificationListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearAllDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notifications",
                            style = TypographyTokens.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ColorTokens.ReactTheme.foreground
                        )
                        if (uiState.unreadCount > 0) {
                            Text(
                                text = "${uiState.unreadCount} unread",
                                style = TypographyTokens.typography.bodySmall,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = IconSet.Navigation.back,
                            contentDescription = "Back",
                            tint = ColorTokens.ReactTheme.foreground
                        )
                    }
                },
                actions = {
                    // Mark all as read
                    if (uiState.unreadCount > 0) {
                        IconButton(onClick = { viewModel.markAllAsRead() }) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark all as read",
                                tint = ColorTokens.ReactTheme.primary
                            )
                        }
                    }

                    // Clear all
                    if (uiState.hasNotifications) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear all",
                                tint = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorTokens.ReactTheme.card
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ColorTokens.ReactTheme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ColorTokens.ReactTheme.background)
        ) {
            when {
                uiState.isLoading && uiState.notifications.isEmpty() -> {
                    // Initial loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ColorTokens.ReactTheme.primary
                        )
                    }
                }

                !uiState.hasNotifications -> {
                    // Empty state
                    EmptyNotificationsView()
                }

                else -> {
                    // Notification list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Tokens.Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                    ) {
                        items(
                            items = uiState.notifications,
                            key = { it.id ?: UUID.randomUUID().toString() }
                        ) { notification ->
                            // Mark as read as soon as this item scrolls into view
                            if (!notification.isRead) {
                                LaunchedEffect(notification.id) {
                                    viewModel.markAsRead(notification.id ?: "")
                                }
                            }
                            NotificationItem(
                                notification = notification,
                                onTap = {
                                    onNotificationTap(notification.id ?: "", notification.data)
                                },
                                onDelete = {
                                    viewModel.deleteNotification(notification.id ?: "")
                                },
                                onNavigateToConnections = onNavigateToConnections,
                                onApproveJoinRequest = { reqId -> viewModel.approveJoinRequest(reqId) },
                                onRejectJoinRequest = { reqId -> viewModel.rejectJoinRequest(reqId) }
                            )
                        }
                    }
                }
            }
        }

        // Clear all confirmation dialog
        if (showClearAllDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                title = {
                    Text(
                        text = "Clear All Notifications?",
                        style = TypographyTokens.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        text = "This will permanently delete all notifications. This action cannot be undone.",
                        style = TypographyTokens.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAll()
                            showClearAllDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = ColorTokens.ReactTheme.destructive
                        )
                    ) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = ColorTokens.ReactTheme.card
            )
        }
    }
}

/**
 * Individual Notification Item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationItem(
    notification: SupabaseNotification,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToConnections: () -> Unit = {},
    onApproveJoinRequest: (String) -> Unit = {},
    onRejectJoinRequest: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = onTap,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (notification.isRead) {
            ColorTokens.ReactTheme.card
        } else {
            ColorTokens.ReactTheme.primary.copy(alpha = 0.05f)
        },
        tonalElevation = if (notification.isRead) 1.dp else 2.dp,
        border = if (!notification.isRead) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                ColorTokens.ReactTheme.primary.copy(alpha = 0.2f)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            // Notification icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getNotificationColor(notification.type).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getNotificationIcon(notification.type),
                    contentDescription = null,
                    tint = getNotificationColor(notification.type),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
            ) {
                // Title
                Text(
                    text = notification.title,
                    style = TypographyTokens.typography.bodyLarge.copy(
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold
                    ),
                    color = ColorTokens.ReactTheme.foreground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Body
                Text(
                    text = notification.body,
                    style = TypographyTokens.typography.bodyMedium,
                    color = ColorTokens.ReactTheme.mutedForeground,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Timestamp
                Text(
                    text = formatTimestamp(notification.createdAt),
                    style = TypographyTokens.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )

                // Action buttons for actionable notifications
                when (notification.type) {
                    "project_invite", "connection_request" -> {
                        OutlinedButton(
                            onClick = onNavigateToConnections,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .height(32.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Text("View in Connections", style = TypographyTokens.typography.labelSmall)
                        }
                    }
                    "join_request" -> {
                        val requestId = notification.data["request_id"] ?: ""
                        if (requestId.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Button(
                                    onClick = { onApproveJoinRequest(requestId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ColorTokens.ReactTheme.primary
                                    ),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                ) {
                                    Text("Approve", style = TypographyTokens.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { onRejectJoinRequest(requestId) },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                ) {
                                    Text("Reject", style = TypographyTokens.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Delete button
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Notification?") },
            text = { Text("This notification will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ColorTokens.ReactTheme.destructive
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = ColorTokens.ReactTheme.card
        )
    }
}

/**
 * Empty state when no notifications
 */
@Composable
private fun EmptyNotificationsView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "No Notifications",
                style = TypographyTokens.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = ColorTokens.ReactTheme.foreground
            )

            Text(
                text = "You're all caught up!",
                style = TypographyTokens.typography.bodyMedium,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
    }
}

/**
 * Get icon for notification type
 */
private fun getNotificationIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        "task_assigned" -> Icons.Default.AssignmentInd
        "task_status_changed" -> Icons.Default.Update
        "task_priority_changed" -> Icons.Default.Flag
        "task_comment" -> Icons.Default.Comment
        "task_due_date_changed" -> Icons.Default.CalendarToday
        "task_created" -> Icons.Default.Add
        "task_updated" -> Icons.Default.Edit
        "task_reminder" -> Icons.Default.Alarm
        "project_invite" -> Icons.Default.Mail
        "connection_request" -> Icons.Default.PersonAdd
        "join_request" -> Icons.Default.GroupAdd
        "join_approved" -> Icons.Default.CheckCircle
        "member_joined" -> Icons.Default.PersonAdd
        else -> Icons.Default.Info
    }
}

/**
 * Get color for notification type
 */
private fun getNotificationColor(type: String): Color {
    return when (type) {
        "task_assigned" -> ColorTokens.ReactTheme.primary
        "task_status_changed" -> ColorTokens.Status.online
        "task_priority_changed" -> ColorTokens.Priority.medium
        "task_comment" -> ColorTokens.ReactTheme.primary
        "task_due_date_changed" -> ColorTokens.Priority.medium
        "task_reminder" -> ColorTokens.ReactTheme.destructive
        "project_invite" -> ColorTokens.ReactTheme.primary
        "connection_request" -> ColorTokens.ReactTheme.primary
        "join_request" -> ColorTokens.Priority.medium
        "join_approved" -> ColorTokens.Status.online
        "member_joined" -> ColorTokens.Status.online
        else -> ColorTokens.ReactTheme.mutedForeground
    }
}

/**
 * Format timestamp to relative time
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
