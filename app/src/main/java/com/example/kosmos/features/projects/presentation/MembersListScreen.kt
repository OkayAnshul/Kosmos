package com.example.kosmos.features.projects.presentation

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.kosmos.core.models.ProjectRole

/**
 * Members List Screen
 * Shows all project members with role management capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersListScreen(
    projectId: String,
    onBackClick: () -> Unit,
    onUserClick: (String) -> Unit,
    onAddMembersClick: () -> Unit,
    viewModel: MembersListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUserRole = uiState.currentUserRole

    var showRoleChangeDialog by remember { mutableStateOf<MemberWithUser?>(null) }
    var showRemoveConfirmDialog by remember { mutableStateOf<MemberWithUser?>(null) }

    // Load members on first composition
    LaunchedEffect(projectId) {
        viewModel.loadMembers(projectId)
    }

    // Show snackbar messages
    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Project Members")
                        Text(
                            text = "${uiState.filteredMembers.size} members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadMembers(projectId) }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (currentUserRole == ProjectRole.ADMIN || currentUserRole == ProjectRole.MANAGER) {
                FloatingActionButton(
                    onClick = onAddMembersClick,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.PersonAdd, "Add Members")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::searchMembers,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search members...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchMembers("") }) {
                            Icon(Icons.Default.Close, "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Role Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedRoleFilter == null,
                    onClick = { viewModel.filterByRole(null) },
                    label = { Text("All") },
                    leadingIcon = if (uiState.selectedRoleFilter == null) {
                        { Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(18.dp)) }
                    } else null
                )
                FilterChip(
                    selected = uiState.selectedRoleFilter == ProjectRole.ADMIN,
                    onClick = { viewModel.filterByRole(ProjectRole.ADMIN) },
                    label = { Text("Admins") },
                    leadingIcon = if (uiState.selectedRoleFilter == ProjectRole.ADMIN) {
                        { Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(18.dp)) }
                    } else null
                )
                FilterChip(
                    selected = uiState.selectedRoleFilter == ProjectRole.MANAGER,
                    onClick = { viewModel.filterByRole(ProjectRole.MANAGER) },
                    label = { Text("Managers") },
                    leadingIcon = if (uiState.selectedRoleFilter == ProjectRole.MANAGER) {
                        { Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(18.dp)) }
                    } else null
                )
                FilterChip(
                    selected = uiState.selectedRoleFilter == ProjectRole.MEMBER,
                    onClick = { viewModel.filterByRole(ProjectRole.MEMBER) },
                    label = { Text("Members") },
                    leadingIcon = if (uiState.selectedRoleFilter == ProjectRole.MEMBER) {
                        { Icon(Icons.Default.Check, "Selected", modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }

            HorizontalDivider()

            // Members List
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading members...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                uiState.filteredMembers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) {
                                    "No members found"
                                } else {
                                    "No members yet"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Add members to start collaborating",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.filteredMembers, key = { it.member.id }) { memberWithUser ->
                            MemberListItem(
                                memberWithUser = memberWithUser,
                                currentUserRole = currentUserRole,
                                onUserClick = { onUserClick(memberWithUser.user.id) },
                                onChangeRoleClick = { showRoleChangeDialog = memberWithUser },
                                onRemoveClick = { showRemoveConfirmDialog = memberWithUser }
                            )
                        }
                    }
                }
            }
        }
    }

    // Role Change Dialog
    showRoleChangeDialog?.let { memberWithUser ->
        RoleChangeDialog(
            memberWithUser = memberWithUser,
            currentUserRole = currentUserRole,
            onDismiss = { showRoleChangeDialog = null },
            onConfirm = { newRole ->
                viewModel.changeRole(projectId, memberWithUser.member.id, newRole)
                showRoleChangeDialog = null
            },
            isLoading = uiState.isUpdating
        )
    }

    // Remove Confirmation Dialog
    showRemoveConfirmDialog?.let { memberWithUser ->
        RemoveMemberDialog(
            memberWithUser = memberWithUser,
            onDismiss = { showRemoveConfirmDialog = null },
            onConfirm = {
                viewModel.removeMember(projectId, memberWithUser.member.id)
                showRemoveConfirmDialog = null
            },
            isLoading = uiState.isUpdating
        )
    }
}

@Composable
private fun MemberListItem(
    memberWithUser: MemberWithUser,
    currentUserRole: ProjectRole,
    onUserClick: () -> Unit,
    onChangeRoleClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val canManage = currentUserRole == ProjectRole.ADMIN &&
                   memberWithUser.member.role != ProjectRole.ADMIN

    ListItem(
        headlineContent = {
            Text(
                text = memberWithUser.user.displayName,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "@${memberWithUser.user.username}",
                    style = MaterialTheme.typography.bodySmall
                )
                // Role Badge
                val roleColor = Color(android.graphics.Color.parseColor(memberWithUser.member.role.getColorCode()))
                AssistChip(
                    onClick = { if (canManage) onChangeRoleClick() },
                    label = {
                        Text(
                            text = memberWithUser.member.role.getDisplayName(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = roleColor
                        )
                    },
                    enabled = canManage,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = roleColor.copy(alpha = 0.1f),
                        labelColor = roleColor,
                        leadingIconContentColor = roleColor
                    )
                )
            }
        },
        leadingContent = {
            if (memberWithUser.user.photoUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = memberWithUser.user.photoUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder with initials
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = memberWithUser.user.displayName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        trailingContent = {
            if (canManage) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onChangeRoleClick) {
                        Icon(Icons.Default.Edit, "Change Role", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onRemoveClick) {
                        Icon(
                            Icons.Default.PersonRemove,
                            "Remove",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    HorizontalDivider()
}

@Composable
private fun RoleChangeDialog(
    memberWithUser: MemberWithUser,
    currentUserRole: ProjectRole,
    onDismiss: () -> Unit,
    onConfirm: (ProjectRole) -> Unit,
    isLoading: Boolean
) {
    var selectedRole by remember { mutableStateOf(memberWithUser.member.role) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Change Role") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select a new role for ${memberWithUser.user.displayName}:")

                ProjectRole.values().forEach { role ->
                    if (currentUserRole.canAssignTo(role)) {
                        Card(
                            onClick = { if (!isLoading) selectedRole = role },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedRole == role) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = role.getDisplayName(),
                                    fontWeight = if (selectedRole == role) FontWeight.Bold else FontWeight.Normal
                                )
                                if (selectedRole == role) {
                                    Icon(Icons.Default.CheckCircle, "Selected", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedRole) },
                enabled = !isLoading && selectedRole != memberWithUser.member.role
            ) {
                Text("Change Role")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RemoveMemberDialog(
    memberWithUser: MemberWithUser,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Remove Member?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Are you sure you want to remove ${memberWithUser.user.displayName} from this project?")
                Text(
                    text = "They will lose access to all project resources.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}
