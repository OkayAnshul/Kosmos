package com.example.kosmos.features.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.components.IconButtonStandard
import com.example.kosmos.shared.ui.components.SecondaryButton
import com.example.kosmos.shared.ui.components.StandardCard

/**
 * Privacy Settings Screen
 * Allows users to manage their privacy preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBackClick: () -> Unit,
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Show data download success snackbar
    LaunchedEffect(uiState.dataDownloadSuccess) {
        if (uiState.dataDownloadSuccess) {
            // TODO: Show snackbar with success message
            viewModel.clearDataDownloadStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Settings") },
                navigationIcon = {
                    IconButtonStandard(
                        icon = IconSet.Navigation.back,
                        onClick = onBackClick,
                        contentDescription = "Back"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(Tokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.lg)
        ) {
            // Profile Visibility Section
            PrivacySectionHeader(
                icon = Icons.Default.Visibility,
                title = "Profile Visibility"
            )

            ProfileVisibilitySelector(
                currentVisibility = uiState.profileVisibility,
                onVisibilityChange = viewModel::updateProfileVisibility
            )

            HorizontalDivider()

            // What Others Can See Section
            PrivacySectionHeader(
                icon = IconSet.Visibility.visible,
                title = "What Others Can See"
            )

            PrivacyToggle(
                label = "Show Email Address",
                description = "Let others see your email on your profile",
                checked = uiState.showEmail,
                onCheckedChange = viewModel::toggleShowEmail
            )

            PrivacyToggle(
                label = "Show Last Seen",
                description = "Let others see when you were last active",
                checked = uiState.showLastSeen,
                onCheckedChange = viewModel::toggleShowLastSeen
            )

            PrivacyToggle(
                label = "Show Online Status",
                description = "Show a green dot when you're online",
                checked = uiState.showOnlineStatus,
                onCheckedChange = viewModel::toggleShowOnlineStatus
            )

            HorizontalDivider()

            // Communication Preferences Section
            PrivacySectionHeader(
                icon = IconSet.Message.chat,
                title = "Communication Preferences"
            )

            PrivacyToggle(
                label = "Allow Direct Messages",
                description = "Let others send you direct messages",
                checked = uiState.allowDirectMessages,
                onCheckedChange = viewModel::toggleAllowDirectMessages
            )

            PrivacyToggle(
                label = "Allow Mentions",
                description = "Let others mention you in messages",
                checked = uiState.allowMentions,
                onCheckedChange = viewModel::toggleAllowMentions
            )

            HorizontalDivider()

            // Data Management Section
            PrivacySectionHeader(
                icon = Icons.Default.Storage,
                title = "Data Management"
            )

            // Data Download Button
            SecondaryButton(
                text = if (uiState.isDownloadingData) "Preparing Download..." else "Download My Data",
                onClick = { viewModel.requestDataDownload() },
                enabled = !uiState.isDownloadingData,
                icon = if (!uiState.isDownloadingData) IconSet.Action.download else null,
                fullWidth = true,
                modifier = Modifier
            )

            Text(
                text = "Request a download of all your data including messages, tasks, and profile information.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Blocked Users Section (Future Implementation)
            PrivacySectionHeader(
                icon = Icons.Default.Block,
                title = "Blocked Users"
            )

            if (uiState.blockedUsers.isEmpty()) {
                StandardCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Tokens.Spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "No blocked users",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "You haven't blocked anyone yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacySectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Tokens.Size.iconMedium)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ProfileVisibilitySelector(
    currentVisibility: String,
    onVisibilityChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
        VisibilityOption(
            title = "Public",
            description = "Anyone can see your profile",
            selected = currentVisibility == "PUBLIC",
            onClick = { onVisibilityChange("PUBLIC") }
        )
        VisibilityOption(
            title = "Friends Only",
            description = "Only people in your projects can see your profile",
            selected = currentVisibility == "FRIENDS_ONLY",
            onClick = { onVisibilityChange("FRIENDS_ONLY") }
        )
        VisibilityOption(
            title = "Private",
            description = "Only you can see your profile",
            selected = currentVisibility == "PRIVATE",
            onClick = { onVisibilityChange("PRIVATE") }
        )
    }
}

@Composable
private fun VisibilityOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                Tokens.BorderWidth.thick,
                MaterialTheme.colorScheme.primary
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                Tokens.BorderWidth.thin,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
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
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (selected) {
                Icon(
                    IconSet.Status.success,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Tokens.Size.iconMedium)
                )
            }
        }
    }
}

@Composable
private fun PrivacyToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(Tokens.Spacing.md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
