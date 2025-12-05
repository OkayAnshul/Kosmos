package com.example.kosmos.features.users.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.kosmos.features.users.presentation.InviteMembersScreen

@Composable
fun InviteMembersScreenWrapper(
    projectId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    InviteMembersScreen(
        projectId = projectId,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
