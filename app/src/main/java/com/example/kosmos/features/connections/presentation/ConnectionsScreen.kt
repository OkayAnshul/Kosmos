package com.example.kosmos.features.connections.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.ProjectInvite
import com.example.kosmos.features.users.presentation.components.UserAvatar
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    onUserClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ConnectionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        Surface(
            color = ColorTokens.ReactTheme.card,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ColorTokens.ReactTheme.border, RectangleShape)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connections",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    )
                }

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ColorTokens.ReactTheme.card,
                    contentColor = ColorTokens.ReactTheme.primary,
                    divider = { HorizontalDivider(color = ColorTokens.ReactTheme.border) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Connections")
                                if (uiState.acceptedConnections.isNotEmpty()) {
                                    Badge(containerColor = ColorTokens.ReactTheme.primary) {
                                        Text("${uiState.acceptedConnections.size}")
                                    }
                                }
                            }
                        },
                        selectedContentColor = ColorTokens.ReactTheme.primary,
                        unselectedContentColor = ColorTokens.ReactTheme.mutedForeground
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Requests")
                                if (uiState.pendingRequests.isNotEmpty()) {
                                    Badge(containerColor = ColorTokens.ReactTheme.destructive) {
                                        Text("${uiState.pendingRequests.size}")
                                    }
                                }
                            }
                        },
                        selectedContentColor = ColorTokens.ReactTheme.primary,
                        unselectedContentColor = ColorTokens.ReactTheme.mutedForeground
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Invites")
                                if (uiState.pendingInvites.isNotEmpty()) {
                                    Badge(containerColor = ColorTokens.ReactTheme.destructive) {
                                        Text("${uiState.pendingInvites.size}")
                                    }
                                }
                            }
                        },
                        selectedContentColor = ColorTokens.ReactTheme.primary,
                        unselectedContentColor = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }
        }

        // Content
        when (selectedTab) {
            0 -> AcceptedConnectionsList(
                connections = uiState.acceptedConnections,
                onUserClick = onUserClick,
                onRemove = viewModel::removeConnection,
                isLoading = uiState.isLoading
            )
            1 -> PendingRequestsList(
                requests = uiState.pendingRequests,
                onAccept = viewModel::acceptConnection,
                onDecline = viewModel::declineConnection,
                onUserClick = onUserClick
            )
            2 -> ProjectInvitesList(
                invites = uiState.pendingInvites,
                onAccept = viewModel::acceptInvite,
                onDecline = viewModel::declineInvite
            )
        }
    }

    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Auto-clear after showing
            viewModel.clearError()
        }
    }
}

@Composable
private fun AcceptedConnectionsList(
    connections: List<ConnectionWithUser>,
    onUserClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(ColorTokens.ReactTheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ColorTokens.ReactTheme.primary)
        }
        return
    }

    if (connections.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(ColorTokens.ReactTheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ColorTokens.ReactTheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    "No connections yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    "Search for users and send connection requests",
                    fontSize = 14.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(connections, key = { it.connection.id }) { item ->
            ConnectionCard(
                item = item,
                onUserClick = { onUserClick(item.user.id) },
                trailing = {
                    IconButton(onClick = { onRemove(item.connection.id) }) {
                        Icon(
                            Icons.Default.PersonRemove,
                            contentDescription = "Remove",
                            tint = ColorTokens.ReactTheme.mutedForeground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PendingRequestsList(
    requests: List<ConnectionWithUser>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(ColorTokens.ReactTheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ColorTokens.ReactTheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MailOutline,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    "No pending requests",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    "Connection requests will appear here",
                    fontSize = 14.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests, key = { it.connection.id }) { item ->
            ConnectionCard(
                item = item,
                onUserClick = { onUserClick(item.user.id) },
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { onAccept(item.connection.id) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ColorTokens.ReactTheme.primary,
                                contentColor = ColorTokens.ReactTheme.primaryForeground
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Accept", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { onDecline(item.connection.id) },
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(ColorTokens.ReactTheme.border)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Ignore", fontSize = 13.sp, color = ColorTokens.ReactTheme.mutedForeground)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ProjectInvitesList(
    invites: List<ProjectInvite>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    if (invites.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(ColorTokens.ReactTheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ColorTokens.ReactTheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    "No pending invites",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    "Project invites will appear here",
                    fontSize = 14.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(invites, key = { it.id }) { invite ->
            ProjectInviteCard(
                invite = invite,
                onAccept = { onAccept(invite.id) },
                onDecline = { onDecline(invite.id) }
            )
        }
    }
}

@Composable
private fun ProjectInviteCard(
    invite: ProjectInvite,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ColorTokens.ReactTheme.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, ColorTokens.ReactTheme.border),
        shape = RoundedCornerShape(Tokens.CornerRadius.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.GroupAdd,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Project Invite",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = "Role: ${invite.role.lowercase().replaceFirstChar { it.uppercase() }}",
                    fontSize = 13.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
                invite.message?.takeIf { it.isNotBlank() }?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = ColorTokens.ReactTheme.mutedForeground,
                        maxLines = 2
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onAccept,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ColorTokens.ReactTheme.primary,
                        contentColor = ColorTokens.ReactTheme.primaryForeground
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Accept", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onDecline,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ColorTokens.ReactTheme.border)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Decline", fontSize = 13.sp, color = ColorTokens.ReactTheme.mutedForeground)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionCard(
    item: ConnectionWithUser,
    onUserClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Card(
        onClick = onUserClick,
        colors = CardDefaults.cardColors(containerColor = ColorTokens.ReactTheme.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, ColorTokens.ReactTheme.border),
        shape = RoundedCornerShape(Tokens.CornerRadius.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                photoUrl = item.user.photoUrl,
                displayName = item.user.displayName,
                isOnline = item.user.isOnline,
                size = 44.dp,
                showOnlineIndicator = true
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.user.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = "@${item.user.username}",
                    fontSize = 13.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            trailing()
        }
    }
}
