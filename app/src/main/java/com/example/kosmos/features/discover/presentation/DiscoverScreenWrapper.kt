package com.example.kosmos.features.discover.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DiscoverScreenWrapper(
    onUserClick: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DiscoverScreen(
        onUserClick = onUserClick,
        onProjectClick = onProjectClick,
        modifier = modifier
    )
}
