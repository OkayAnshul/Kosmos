package com.example.kosmos.features.profile.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.kosmos.features.auth.presentation.AuthViewModel
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    onPrivacySettingsClick: () -> Unit = {},
    onNotificationSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Profile") },
            navigationIcon = {
                IconButtonStandard(
                    icon = IconSet.Navigation.back,
                    onClick = onNavigateBack,
                    contentDescription = "Back"
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Tokens.Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.lg)
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(Tokens.Size.avatarXXLarge)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val currentUser = uiState.currentUser
                if (currentUser?.photoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentUser.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = currentUser?.displayName?.take(2)?.uppercase() ?: "U",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // User Info
            StandardCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
                ) {
                    Column {
                        Text(
                            text = "Display Name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = uiState.currentUser?.displayName ?: "Unknown",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column {
                        Text(
                            text = "Email",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = uiState.currentUser?.email ?: "Unknown",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Profile Actions
            StandardCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("Edit Profile") },
                    leadingContent = {
                        Icon(
                            imageVector = IconSet.Action.edit,
                            contentDescription = "Edit profile"
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = IconSet.Navigation.forward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { onEditProfileClick() }
                )

                ListDivider()

                ListItem(
                    headlineContent = { Text("Privacy Settings") },
                    leadingContent = {
                        Icon(
                            imageVector = IconSet.Settings.privacy,
                            contentDescription = "Privacy settings"
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = IconSet.Navigation.forward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { onPrivacySettingsClick() }
                )

                ListDivider()

                ListItem(
                    headlineContent = { Text("Notifications") },
                    leadingContent = {
                        Icon(
                            imageVector = IconSet.Settings.notifications,
                            contentDescription = "Notification settings"
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = IconSet.Navigation.forward,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { onNotificationSettingsClick() }
                )
            }
        }
    }
}