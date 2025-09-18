package com.example.kosmos.features.connections.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ConnectionsScreenWrapper(
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ConnectionsScreen(
        onUserClick = onUserClick,
        modifier = modifier
    )
}
