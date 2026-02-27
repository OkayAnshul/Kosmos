# Connections Tab in DiscoverScreen Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a "Connections" tab (tab index 2) to the DiscoverScreen so users can manage connections and view full profiles from one place.

**Architecture:** Hoist a `ConnectionsViewModel` into `DiscoverScreen` alongside the existing `DiscoverViewModel`. When tab 2 is selected, hide the search bar and render `ConnectionsTabContent` — a flat `LazyColumn` with three sections (Pending Requests, Project Invites, My Connections). Reuses all existing ViewModel actions; no new data layer needed.

**Tech Stack:** Jetpack Compose, Hilt (`hiltViewModel()`), `ConnectionsViewModel` (already exists at `features/connections/presentation/ConnectionsViewModel.kt`)

---

### Task 1: Guard search against tab 2 in DiscoverViewModel

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/discover/presentation/DiscoverViewModel.kt:52-54`

**Step 1: Apply the one-line fix**

In `onTabSelected`, add `tab < 2` guard so switching to the Connections tab doesn't trigger a search:

```kotlin
fun onTabSelected(tab: Int) {
    _uiState.update { it.copy(selectedTab = tab) }
    if (tab < 2 && _uiState.value.query.isNotBlank()) search(_uiState.value.query)
}
```

**Step 2: Compile check**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

---

### Task 2: Rewrite DiscoverScreen to add the Connections tab

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/discover/presentation/DiscoverScreen.kt`

This is the main task. Replace the entire file with the content below.

Key changes vs current file:
1. New imports: `AnimatedVisibility`, `collectAsStateWithLifecycle`, `ConnectionsViewModel`, `ConnectionsUiState`, `ConnectionWithUser`, `ProjectInvite`, `FontWeight`, material icons
2. New parameter `connectionsViewModel: ConnectionsViewModel = hiltViewModel()` on `DiscoverScreen`
3. Collect `connectionsState` from `connectionsViewModel`
4. Wrap search bar in `AnimatedVisibility(visible = uiState.selectedTab < 2)`
5. Add third tab with pending-count badge
6. Add `uiState.selectedTab == 2` branch → `ConnectionsTabContent`
7. Add `ConnectionsTabContent`, `SectionHeader`, `ConnectionRow`, `RequestRow`, `InviteRow` composables at bottom of file

**Step 1: Replace the file**

```kotlin
package com.example.kosmos.features.discover.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.ConnectionStatus
import com.example.kosmos.core.models.JoinRequestStatus
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectInvite
import com.example.kosmos.core.models.User
import com.example.kosmos.features.connections.presentation.ConnectionWithUser
import com.example.kosmos.features.connections.presentation.ConnectionsUiState
import com.example.kosmos.features.connections.presentation.ConnectionsViewModel
import com.example.kosmos.features.users.presentation.components.UserAvatar
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens

@Composable
fun DiscoverScreen(
    onUserClick: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = hiltViewModel(),
    connectionsViewModel: ConnectionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionsState by connectionsViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        // Header
        Text(
            text = "Discover",
            style = MaterialTheme.typography.headlineMedium,
            color = ColorTokens.ReactTheme.foreground,
            modifier = Modifier.padding(horizontal = Tokens.Spacing.lg, vertical = Tokens.Spacing.md)
        )

        // Search bar — hidden on Connections tab
        AnimatedVisibility(visible = uiState.selectedTab < 2) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChanged,
                placeholder = {
                    Text(
                        text = if (uiState.selectedTab == 0) "Search people..." else "Search public projects...",
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = IconSet.Action.search,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ColorTokens.ReactTheme.foreground,
                    unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                    focusedBorderColor = ColorTokens.ReactTheme.primary,
                    unfocusedBorderColor = ColorTokens.ReactTheme.border,
                    cursorColor = ColorTokens.ReactTheme.primary,
                    focusedContainerColor = ColorTokens.ReactTheme.card,
                    unfocusedContainerColor = ColorTokens.ReactTheme.card
                ),
                shape = RoundedCornerShape(Tokens.CornerRadius.lg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.Spacing.lg)
            )
        }

        Spacer(modifier = Modifier.height(Tokens.Spacing.md))

        // Tabs
        val pendingCount = connectionsState.pendingRequests.size + connectionsState.pendingInvites.size
        TabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = ColorTokens.ReactTheme.background,
            contentColor = ColorTokens.ReactTheme.primary,
            divider = { HorizontalDivider(color = ColorTokens.ReactTheme.border) }
        ) {
            Tab(
                selected = uiState.selectedTab == 0,
                onClick = { viewModel.onTabSelected(0) },
                text = {
                    Text(
                        "People",
                        color = if (uiState.selectedTab == 0) ColorTokens.ReactTheme.primary
                        else ColorTokens.ReactTheme.mutedForeground
                    )
                }
            )
            Tab(
                selected = uiState.selectedTab == 1,
                onClick = { viewModel.onTabSelected(1) },
                text = {
                    Text(
                        "Projects",
                        color = if (uiState.selectedTab == 1) ColorTokens.ReactTheme.primary
                        else ColorTokens.ReactTheme.mutedForeground
                    )
                }
            )
            Tab(
                selected = uiState.selectedTab == 2,
                onClick = { viewModel.onTabSelected(2) },
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Connections",
                            color = if (uiState.selectedTab == 2) ColorTokens.ReactTheme.primary
                            else ColorTokens.ReactTheme.mutedForeground
                        )
                        if (pendingCount > 0) {
                            Badge(containerColor = ColorTokens.ReactTheme.destructive) {
                                Text("$pendingCount")
                            }
                        }
                    }
                }
            )
        }

        // Content
        when {
            uiState.selectedTab == 2 -> {
                ConnectionsTabContent(
                    connectionsState = connectionsState,
                    onUserClick = onUserClick,
                    onAcceptRequest = connectionsViewModel::acceptConnection,
                    onDeclineRequest = connectionsViewModel::declineConnection,
                    onRemoveConnection = connectionsViewModel::removeConnection,
                    onAcceptInvite = connectionsViewModel::acceptInvite,
                    onDeclineInvite = connectionsViewModel::declineInvite
                )
            }
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorTokens.ReactTheme.primary)
                }
            }
            uiState.query.isBlank() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = IconSet.Action.search,
                            contentDescription = null,
                            tint = ColorTokens.ReactTheme.mutedForeground,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(Tokens.Spacing.md))
                        Text(
                            text = if (uiState.selectedTab == 0) "Search for people to connect with"
                            else "Search for public projects to join",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }
            }
            uiState.selectedTab == 0 -> {
                PeopleResults(
                    users = uiState.users,
                    connectionStatuses = uiState.connectionStatuses,
                    onUserClick = onUserClick,
                    onConnect = viewModel::sendConnectionRequest
                )
            }
            else -> {
                ProjectResults(
                    projects = uiState.projects,
                    joinRequestStatuses = uiState.joinRequestStatuses,
                    memberProjectIds = uiState.memberProjectIds,
                    onProjectClick = onProjectClick,
                    onRequestToJoin = { viewModel.requestToJoin(it) }
                )
            }
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

// ─── Connections Tab ─────────────────────────────────────────────────────────

@Composable
private fun ConnectionsTabContent(
    connectionsState: ConnectionsUiState,
    onUserClick: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onRemoveConnection: (String) -> Unit,
    onAcceptInvite: (String) -> Unit,
    onDeclineInvite: (String) -> Unit
) {
    val hasAny = connectionsState.acceptedConnections.isNotEmpty() ||
            connectionsState.pendingRequests.isNotEmpty() ||
            connectionsState.pendingInvites.isNotEmpty()

    if (connectionsState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorTokens.ReactTheme.primary)
        }
        return
    }

    if (!hasAny) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorTokens.ReactTheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    "No connections yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    "Search for people in the People tab to connect",
                    style = MaterialTheme.typography.bodyMedium,
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
        contentPadding = PaddingValues(Tokens.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        // Pending Requests section
        if (connectionsState.pendingRequests.isNotEmpty()) {
            item {
                ConnectionsSectionHeader(
                    title = "Pending Requests",
                    count = connectionsState.pendingRequests.size
                )
            }
            items(connectionsState.pendingRequests, key = { "req_${it.connection.id}" }) { item ->
                RequestRow(
                    item = item,
                    onUserClick = { onUserClick(item.user.id) },
                    onAccept = { onAcceptRequest(item.connection.id) },
                    onDecline = { onDeclineRequest(item.connection.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(Tokens.Spacing.md)) }
        }

        // Project Invites section
        if (connectionsState.pendingInvites.isNotEmpty()) {
            item {
                ConnectionsSectionHeader(
                    title = "Project Invites",
                    count = connectionsState.pendingInvites.size
                )
            }
            items(connectionsState.pendingInvites, key = { "inv_${it.id}" }) { invite ->
                InviteRow(
                    invite = invite,
                    onAccept = { onAcceptInvite(invite.id) },
                    onDecline = { onDeclineInvite(invite.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(Tokens.Spacing.md)) }
        }

        // Accepted Connections section
        if (connectionsState.acceptedConnections.isNotEmpty()) {
            item {
                ConnectionsSectionHeader(
                    title = "My Connections",
                    count = connectionsState.acceptedConnections.size
                )
            }
            items(connectionsState.acceptedConnections, key = { "conn_${it.connection.id}" }) { item ->
                ConnectionRow(
                    item = item,
                    onUserClick = { onUserClick(item.user.id) },
                    onRemove = { onRemoveConnection(item.connection.id) }
                )
            }
        }
    }
}

@Composable
private fun ConnectionsSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Tokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.mutedForeground
        )
        Surface(
            shape = CircleShape,
            color = ColorTokens.ReactTheme.secondary
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionRow(
    item: ConnectionWithUser,
    onUserClick: () -> Unit,
    onRemove: () -> Unit
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
                    text = item.user.displayName.ifBlank { item.user.username },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${item.user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
                item.user.role?.let { role ->
                    Text(
                        text = role,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.PersonRemove,
                    contentDescription = "Remove connection",
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RequestRow(
    item: ConnectionWithUser,
    onUserClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, ColorTokens.ReactTheme.primary.copy(alpha = 0.2f)
        ),
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
                    .background(ColorTokens.ReactTheme.primary.copy(alpha = 0.15f))
                    .clickable(onClick = onUserClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.user.displayName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.user.displayName.ifBlank { item.user.username },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${item.user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorTokens.ReactTheme.primary,
                        contentColor = ColorTokens.ReactTheme.primaryForeground
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Accept", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onDecline,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, ColorTokens.ReactTheme.border
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        "Ignore",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteRow(
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
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Project Invite",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = "Role: ${invite.role.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
                invite.message?.takeIf { it.isNotBlank() }?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorTokens.ReactTheme.primary,
                        contentColor = ColorTokens.ReactTheme.primaryForeground
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Accept", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onDecline,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, ColorTokens.ReactTheme.border
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        "Decline",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }
        }
    }
}

// ─── People / Projects tabs (unchanged from original) ─────────────────────────

@Composable
private fun PeopleResults(
    users: List<User>,
    connectionStatuses: Map<String, ConnectionStatus>,
    onUserClick: (String) -> Unit,
    onConnect: (String) -> Unit
) {
    if (users.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No users found", color = ColorTokens.ReactTheme.mutedForeground)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(Tokens.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        items(users, key = { it.id }) { user ->
            val status = connectionStatuses[user.id]
            PersonCard(
                user = user,
                connectionStatus = status,
                onClick = { onUserClick(user.id) },
                onConnect = { onConnect(user.id) }
            )
        }
    }
}

@Composable
private fun PersonCard(
    user: User,
    connectionStatus: ConnectionStatus?,
    onClick: () -> Unit,
    onConnect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(Tokens.CornerRadius.lg),
        color = ColorTokens.ReactTheme.card,
        border = androidx.compose.foundation.BorderStroke(1.dp, ColorTokens.ReactTheme.border),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(Tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ColorTokens.ReactTheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorTokens.ReactTheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName.ifBlank { user.username },
                    style = MaterialTheme.typography.bodyLarge,
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

            when (connectionStatus) {
                null -> {
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorTokens.ReactTheme.primary,
                            contentColor = ColorTokens.ReactTheme.primaryForeground
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(Tokens.CornerRadius.md)
                    ) {
                        Text("Connect", style = MaterialTheme.typography.bodySmall)
                    }
                }
                ConnectionStatus.PENDING -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ColorTokens.ReactTheme.border),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(Tokens.CornerRadius.md)
                    ) {
                        Text("Pending", style = MaterialTheme.typography.bodySmall, color = ColorTokens.ReactTheme.mutedForeground)
                    }
                }
                ConnectionStatus.ACCEPTED -> {
                    Surface(
                        shape = RoundedCornerShape(Tokens.CornerRadius.md),
                        color = ColorTokens.Stitch.success.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Connected",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.Stitch.success,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ProjectResults(
    projects: List<Project>,
    joinRequestStatuses: Map<String, JoinRequestStatus>,
    memberProjectIds: Set<String>,
    onProjectClick: (String) -> Unit,
    onRequestToJoin: (String) -> Unit
) {
    if (projects.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No public projects found", color = ColorTokens.ReactTheme.mutedForeground)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(Tokens.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        items(projects, key = { it.id }) { project ->
            val isMember = memberProjectIds.contains(project.id)
            val requestStatus = joinRequestStatuses[project.id]
            ProjectCard(
                project = project,
                isMember = isMember,
                requestStatus = requestStatus,
                onClick = { onProjectClick(project.id) },
                onRequestToJoin = { onRequestToJoin(project.id) }
            )
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    isMember: Boolean,
    requestStatus: JoinRequestStatus?,
    onClick: () -> Unit,
    onRequestToJoin: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(Tokens.CornerRadius.lg),
        color = ColorTokens.ReactTheme.card,
        border = androidx.compose.foundation.BorderStroke(1.dp, ColorTokens.ReactTheme.border),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(Tokens.Spacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(Tokens.CornerRadius.md))
                        .background(ColorTokens.ReactTheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = project.name.firstOrNull()?.uppercase() ?: "P",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorTokens.ReactTheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorTokens.ReactTheme.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (project.description.isNotBlank()) {
                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Tokens.Spacing.sm))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = IconSet.User.people,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${project.memberCount} members",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                when {
                    isMember -> {
                        Surface(
                            shape = RoundedCornerShape(Tokens.CornerRadius.md),
                            color = ColorTokens.Stitch.success.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "Member",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTokens.Stitch.success,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    requestStatus == JoinRequestStatus.PENDING -> {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ColorTokens.ReactTheme.border),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(Tokens.CornerRadius.md)
                        ) {
                            Text("Pending", style = MaterialTheme.typography.bodySmall, color = ColorTokens.ReactTheme.mutedForeground)
                        }
                    }
                    requestStatus == JoinRequestStatus.REJECTED -> {
                        Surface(
                            shape = RoundedCornerShape(Tokens.CornerRadius.md),
                            color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "Rejected",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTokens.ReactTheme.destructive,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = onRequestToJoin,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorTokens.ReactTheme.primary,
                                contentColor = ColorTokens.ReactTheme.primaryForeground
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(Tokens.CornerRadius.md)
                        ) {
                            Text("Request to Join", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
```

**Step 2: Compile check**

```bash
./gradlew compileDebugKotlin 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/kosmos/features/discover/presentation/DiscoverScreen.kt \
        app/src/main/java/com/example/kosmos/features/discover/presentation/DiscoverViewModel.kt
git commit -m "feat: add Connections tab to DiscoverScreen

Shows pending requests, project invites, and accepted connections.
Tapping a connection opens their full UserProfile screen.
Search bar hides automatically when Connections tab is active."
```

---

### Task 3: Verify via Supabase MCP

The connection between **anshul** (requester) and **hello1** (addressee) is confirmed **ACCEPTED** in Supabase. After installing the app:

1. Log in as **hello1** → Discover → Connections tab → should see "anshul" under **My Connections**
2. Tap anshul → should navigate to `UserProfileScreen`
3. Log in as **anshul** → Discover → Connections tab → should see "hello1" under **My Connections**
4. To test requests: as hello1, search for a third user in People tab → Connect → switch to anshul → Connections tab should show badge + pending request

```sql
-- Re-run this to confirm state before testing:
SELECT uc.status, r.display_name AS requester, a.display_name AS addressee
FROM user_connections uc
LEFT JOIN users_public r ON r.id = uc.requester_id
LEFT JOIN users_public a ON a.id = uc.addressee_id;
```
Expected: 1 row, status = ACCEPTED, requester = Anshul, addressee = Anshul (hello1's display name)
