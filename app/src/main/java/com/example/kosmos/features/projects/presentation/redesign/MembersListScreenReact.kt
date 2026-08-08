package com.example.kosmos.features.projects.presentation.redesign

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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.core.models.Permission
import com.example.kosmos.core.models.ProjectInvite
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.User
import com.example.kosmos.features.projects.presentation.JoinRequestWithUser
import com.example.kosmos.shared.ui.components.PermissionGated
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Members List Screen - React Design Implementation
 *
 * Design matches React theme:
 * - Dark background
 * - Purple accent
 * - Glassmorphic cards
 * - Clean typography
 * - Role badges
 */

data class MemberCardData(
    val userId: String,
    val name: String,
    val email: String,
    val role: ProjectRole,
    val avatar: String,
    val joinedAt: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersListScreenReact(
    members: List<MemberCardData> = emptyList(),
    pendingInvites: List<ProjectInvite> = emptyList(),
    joinRequests: List<JoinRequestWithUser> = emptyList(),
    currentUserRole: ProjectRole = ProjectRole.MEMBER,
    currentMember: ProjectMember? = null,
    onMemberClick: (String) -> Unit = {},
    onAddMembersClick: () -> Unit = {},
    onChangeRole: (String, ProjectRole) -> Unit = { _, _ -> },
    onRemoveMember: (String) -> Unit = {},
    onCancelInvite: (String) -> Unit = {},
    onApproveJoinRequest: (String) -> Unit = {},
    onRejectJoinRequest: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Use Column with custom top bar (NO Scaffold - avoids nesting with MainActivity's Scaffold)
    Column(modifier = modifier.fillMaxSize()) {
        // Custom top bar
        Surface(
            color = ColorTokens.ReactTheme.card,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = ColorTokens.ReactTheme.border,
                    shape = RectangleShape
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Members",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    )
                    Text(
                        text = "${members.size} members",
                        fontSize = 14.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }

                if (currentUserRole == ProjectRole.ADMIN || currentUserRole == ProjectRole.MANAGER) {
                    IconButton(
                        onClick = onAddMembersClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Members",
                            tint = ColorTokens.ReactTheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Content area - respects bottom nav automatically
        if (members.isEmpty()) {
            // Empty state
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
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ColorTokens.ReactTheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = ColorTokens.ReactTheme.mutedForeground,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "No members yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    )
                    Text(
                        text = "Add members to get started",
                        fontSize = 14.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }
        } else {
            // Members list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorTokens.ReactTheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pending Invites Section
                if (pendingInvites.isNotEmpty() &&
                    (currentUserRole == ProjectRole.ADMIN || currentUserRole == ProjectRole.MANAGER)
                ) {
                    item {
                        var expanded by remember { mutableStateOf(true) }
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pending Invites (${pendingInvites.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorTokens.Priority.medium
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (expanded) "Collapse" else "Expand",
                                    tint = ColorTokens.ReactTheme.mutedForeground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (expanded) {
                                pendingInvites.forEach { invite ->
                                    PendingInviteCard(
                                        invite = invite,
                                        onCancel = { onCancelInvite(invite.id) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                            HorizontalDivider(
                                color = ColorTokens.ReactTheme.border,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                // Join Requests Section
                if (joinRequests.isNotEmpty() &&
                    (currentUserRole == ProjectRole.ADMIN || currentUserRole == ProjectRole.MANAGER)
                ) {
                    item {
                        var expanded by remember { mutableStateOf(true) }
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Join Requests (${joinRequests.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorTokens.ReactTheme.primary
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (expanded) "Collapse" else "Expand",
                                    tint = ColorTokens.ReactTheme.mutedForeground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (expanded) {
                                joinRequests.forEach { reqWithUser ->
                                    JoinRequestCard(
                                        requestWithUser = reqWithUser,
                                        onApprove = { onApproveJoinRequest(reqWithUser.request.id) },
                                        onReject = { onRejectJoinRequest(reqWithUser.request.id) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                            HorizontalDivider(
                                color = ColorTokens.ReactTheme.border,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                items(members, key = { it.userId }) { member ->
                    MemberCard(
                        member = member,
                        currentUserRole = currentUserRole,
                        currentMember = currentMember,
                        onMemberClick = { onMemberClick(member.userId) },
                        onChangeRole = { newRole -> onChangeRole(member.userId, newRole) },
                        onRemoveMember = { onRemoveMember(member.userId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberCard(
    member: MemberCardData,
    currentUserRole: ProjectRole,
    currentMember: ProjectMember? = null,
    onMemberClick: () -> Unit,
    onChangeRole: (ProjectRole) -> Unit,
    onRemoveMember: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    // P1-06 FIX: Add state for dialogs
    var showChangeRoleDialog by remember { mutableStateOf(false) }
    var showRemoveMemberDialog by remember { mutableStateOf(false) }
    var showRolePermissions by remember { mutableStateOf(false) }

    Card(
        onClick = onMemberClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = ColorTokens.ReactTheme.border
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Tokens.CornerRadius.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ColorTokens.ReactTheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.avatar,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTokens.ReactTheme.primaryForeground
                )
            }

            // Member info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = member.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = member.email,
                    fontSize = 14.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoleBadge(role = member.role)
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "View role permissions",
                        tint = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { showRolePermissions = true }
                    )
                    Text(
                        text = "• Joined ${member.joinedAt}",
                        fontSize = 12.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }

            // Menu button (only for admins/managers)
            if (currentUserRole == ProjectRole.ADMIN || currentUserRole == ProjectRole.MANAGER) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = ColorTokens.ReactTheme.mutedForeground,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // P1-06 FIX: Show change role dialog instead of directly changing
                        PermissionGated(
                            permission = Permission.CHANGE_MEMBER_ROLES,
                            currentMember = currentMember,
                            action = "Change member role"
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Change Role",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                                onClick = {
                                    showChangeRoleDialog = true
                                    showMenu = false
                                }
                            )
                        }

                        HorizontalDivider()

                        // P1-06 FIX: Show remove member dialog instead of directly removing
                        PermissionGated(
                            permission = Permission.REMOVE_MEMBERS,
                            currentMember = currentMember,
                            action = "Remove member from project"
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Remove from project",
                                        color = ColorTokens.ReactTheme.destructive
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PersonRemove,
                                        contentDescription = null,
                                        tint = ColorTokens.ReactTheme.destructive
                                    )
                                },
                                onClick = {
                                    showRemoveMemberDialog = true
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // P1-06 FIX: Add dialogs
    if (showChangeRoleDialog) {
        com.example.kosmos.features.projects.components.ChangeRoleDialog(
            member = com.example.kosmos.core.models.ProjectMember(
                id = member.userId,
                projectId = "",  // Not needed for dialog
                userId = member.userId,
                role = member.role,
                joinedAt = 0L  // Not needed for dialog
            ),
            memberName = member.name,
            currentRole = member.role,
            onRoleSelected = { newRole ->
                onChangeRole(newRole)
            },
            onDismiss = {
                showChangeRoleDialog = false
            }
        )
    }

    if (showRemoveMemberDialog) {
        com.example.kosmos.features.projects.components.RemoveMemberDialog(
            member = com.example.kosmos.core.models.ProjectMember(
                id = member.userId,
                projectId = "",  // Not needed for dialog
                userId = member.userId,
                role = member.role,
                joinedAt = 0L  // Not needed for dialog
            ),
            memberName = member.name,
            onConfirm = {
                onRemoveMember()
            },
            onDismiss = {
                showRemoveMemberDialog = false
            }
        )
    }

    if (showRolePermissions) {
        RolePermissionsSheet(
            role = member.role,
            onDismiss = { showRolePermissions = false }
        )
    }
}

@Composable
private fun PendingInviteCard(
    invite: ProjectInvite,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.Priority.medium.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = ColorTokens.Priority.medium.copy(alpha = 0.2f)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Tokens.CornerRadius.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invite.inviteeId.take(8) + "...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTokens.ReactTheme.foreground
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = ColorTokens.Priority.medium.copy(alpha = 0.2f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Pending",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorTokens.Priority.medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = invite.role,
                        fontSize = 12.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ColorTokens.ReactTheme.destructive
                )
            ) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun JoinRequestCard(
    requestWithUser: JoinRequestWithUser,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = ColorTokens.ReactTheme.primary.copy(alpha = 0.2f)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Tokens.CornerRadius.md)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ColorTokens.ReactTheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = requestWithUser.user.displayName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTokens.ReactTheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = requestWithUser.user.displayName.ifBlank { requestWithUser.user.username },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTokens.ReactTheme.foreground
                    )
                    Text(
                        text = "@${requestWithUser.user.username}",
                        fontSize = 12.sp,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }

            // Message if present
            requestWithUser.request.message?.let { msg ->
                if (msg.isNotBlank()) {
                    Text(
                        text = "\"$msg\"",
                        fontSize = 13.sp,
                        color = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Reject", fontSize = 12.sp, color = ColorTokens.ReactTheme.mutedForeground)
                }
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorTokens.ReactTheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Approve", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(role: ProjectRole) {
    val (bgColor, textColor, label) = when (role) {
        ProjectRole.ADMIN -> Triple(
            ColorTokens.ReactTheme.primary.copy(alpha = 0.2f),
            ColorTokens.ReactTheme.primary,
            "Admin"
        )
        ProjectRole.MANAGER -> Triple(
            ColorTokens.Success.light.copy(alpha = 0.2f),
            ColorTokens.Success.dark,
            "Manager"
        )
        ProjectRole.MEMBER -> Triple(
            ColorTokens.ReactTheme.muted,
            ColorTokens.ReactTheme.foreground,
            "Member"
        )
    }

    Surface(
        color = bgColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        modifier = Modifier
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RolePermissionsSheet(
    role: ProjectRole,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val permissions = when (role) {
        ProjectRole.ADMIN -> Permission.ADMIN_PERMISSIONS
        ProjectRole.MANAGER -> Permission.MANAGER_PERMISSIONS
        ProjectRole.MEMBER -> Permission.MEMBER_PERMISSIONS
    }
    val allPermissionsByCategory = Permission.getPermissionsByCategory()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ColorTokens.ReactTheme.card,
        contentColor = ColorTokens.ReactTheme.foreground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "${role.name.lowercase().replaceFirstChar { it.uppercase() }} Permissions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTokens.ReactTheme.foreground
            )
            Text(
                text = "${permissions.size} of ${Permission.entries.size} permissions",
                fontSize = 14.sp,
                color = ColorTokens.ReactTheme.mutedForeground
            )

            HorizontalDivider(color = ColorTokens.ReactTheme.border)

            // Permission categories
            allPermissionsByCategory.forEach { (category, permsInCategory) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = category,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.primary
                    )
                    permsInCategory.forEach { perm ->
                        val hasPermission = perm in permissions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (hasPermission) com.example.kosmos.shared.ui.designsystem.ColorTokens.Status.online
                                       else ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = perm.getDescription(),
                                fontSize = 13.sp,
                                color = if (hasPermission) ColorTokens.ReactTheme.foreground
                                        else ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
