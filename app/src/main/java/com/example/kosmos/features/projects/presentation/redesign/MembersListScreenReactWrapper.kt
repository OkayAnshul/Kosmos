package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.features.projects.presentation.MembersListViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Wrapper for MembersListScreenReact that connects to the backend
 */
@Composable
fun MembersListScreenReactWrapper(
    projectId: String,
    onMemberClick: (String) -> Unit,
    onAddMembersClick: () -> Unit,
    viewModel: MembersListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load members, invites, and join requests on first composition
    LaunchedEffect(projectId) {
        viewModel.loadMembers(projectId)
        viewModel.loadPendingInvites(projectId)
        viewModel.loadJoinRequests(projectId)
    }

    // Map domain models to UI models
    val memberCards = uiState.filteredMembers.map { memberWithUser ->
        MemberCardData(
            userId = memberWithUser.user.id,
            name = memberWithUser.user.displayName.takeIf { it.isNotBlank() }
                ?: memberWithUser.user.username,
            email = memberWithUser.user.email,
            role = memberWithUser.member.role,
            avatar = memberWithUser.user.displayName.firstOrNull()?.uppercase() ?: "?",
            joinedAt = formatJoinedDate(memberWithUser.member.joinedAt)
        )
    }

    MembersListScreenReact(
        members = memberCards,
        pendingInvites = uiState.pendingInvites,
        joinRequests = uiState.joinRequests,
        currentUserRole = uiState.currentUserRole ?: ProjectRole.MEMBER,
        onMemberClick = onMemberClick,
        onAddMembersClick = onAddMembersClick,
        onChangeRole = { userId, newRole ->
            viewModel.changeRole(projectId, userId, newRole)
        },
        onRemoveMember = { userId ->
            viewModel.removeMember(projectId, userId)
        },
        onCancelInvite = { inviteId ->
            viewModel.cancelInvite(inviteId)
        },
        onApproveJoinRequest = { requestId ->
            viewModel.approveJoinRequest(requestId, projectId)
        },
        onRejectJoinRequest = { requestId ->
            viewModel.rejectJoinRequest(requestId)
        }
    )
}

private fun formatJoinedDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 86400_000 -> "today"
        diff < 172800_000 -> "yesterday"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        diff < 2592000_000 -> "${diff / 604800_000}w ago"
        else -> {
            val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
