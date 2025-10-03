package com.example.kosmos.features.profile.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.features.auth.presentation.AuthViewModel
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import javax.inject.Inject

/**
 * More Tab Screen - Profile + Settings Menu
 *
 * This screen provides access to:
 * - User profile (view/edit)
 * - Settings (app preferences)
 * - Privacy settings
 * - Notification settings
 * - About/Help
 * - Logout
 *
 * Design matches React theme:
 * - Dark background
 * - Purple accent
 * - Glassmorphic cards
 * - Clean typography
 */

/**
 * ViewModel helper for MoreTabScreen to inject AuthRepository
 */
class MoreTabDataViewModel @Inject constructor(
    val authRepository: AuthRepository
) : androidx.lifecycle.ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreTabScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPrivacySettings: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    authRepository: AuthRepository = hiltViewModel<MoreTabDataViewModel>().authRepository
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val currentUser = authUiState.currentUser

    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Custom top bar matching React theme
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ColorTokens.ReactTheme.card,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "More",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
            }
        }

        // Content area
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorTokens.ReactTheme.background),
            contentPadding = PaddingValues(Tokens.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Profile Card
            item {
                ProfileCard(
                    userName = currentUser?.username ?: "Guest",
                    userEmail = currentUser?.email ?: "",
                    userAvatar = currentUser?.username?.firstOrNull()?.uppercase() ?: "?",
                    onClick = onNavigateToProfile
                )
            }

            item {
                Spacer(modifier = Modifier.height(Tokens.Spacing.md))
            }

            // Settings Section
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTokens.ReactTheme.primary,
                    modifier = Modifier.padding(horizontal = Tokens.Spacing.sm)
                )
            }

            item {
                MenuCard {
                    MenuCardItem(
                        icon = IconSet.User.profile,
                        title = "Edit Profile",
                        subtitle = "Update your personal information",
                        onClick = onNavigateToEditProfile
                    )
                    HorizontalDivider(color = ColorTokens.ReactTheme.secondary.copy(alpha = 0.2f))
                    MenuCardItem(
                        icon = IconSet.Settings.settings,
                        title = "App Settings",
                        subtitle = "Preferences and customization",
                        onClick = onNavigateToSettings
                    )
                    HorizontalDivider(color = ColorTokens.ReactTheme.secondary.copy(alpha = 0.2f))
                    MenuCardItem(
                        icon = IconSet.Settings.notifications,
                        title = "Notifications",
                        subtitle = "Manage notification preferences",
                        onClick = onNavigateToNotificationSettings
                    )
                    HorizontalDivider(color = ColorTokens.ReactTheme.secondary.copy(alpha = 0.2f))
                    MenuCardItem(
                        icon = IconSet.Settings.security,
                        title = "Privacy & Security",
                        subtitle = "Control your data and privacy",
                        onClick = onNavigateToPrivacySettings
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(Tokens.Spacing.md))
            }

            // About Section
            item {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTokens.ReactTheme.primary,
                    modifier = Modifier.padding(horizontal = Tokens.Spacing.sm)
                )
            }

            item {
                MenuCard {
                    MenuCardItem(
                        icon = Icons.Filled.Info,
                        title = "About Kosmos",
                        subtitle = "Version, terms, and support",
                        onClick = onNavigateToAbout
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(Tokens.Spacing.lg))
            }

            // Logout Button
            item {
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorTokens.ReactTheme.destructive
                    )
                ) {
                    Icon(
                        IconSet.User.logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Tokens.Spacing.sm))
                    Text("Logout")
                }
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    IconSet.User.logout,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.destructive
                )
            },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ColorTokens.ReactTheme.destructive
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    userName: String,
    userEmail: String,
    userAvatar: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ColorTokens.ReactTheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userAvatar,
                    style = MaterialTheme.typography.headlineMedium,
                    color = ColorTokens.ReactTheme.primaryForeground,
                    fontWeight = FontWeight.Bold
                )
            }

            // User info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge,
                    color = ColorTokens.ReactTheme.foreground,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            // Arrow icon
            Icon(
                IconSet.Navigation.forward,
                contentDescription = "View Profile",
                tint = ColorTokens.ReactTheme.mutedForeground
            )
        }
    }
}

@Composable
private fun MenuCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ColorTokens.ReactTheme.card
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = Tokens.Spacing.xs)
        ) {
            content()
        }
    }
}

@Composable
private fun MenuCardItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = Tokens.Spacing.lg,
                vertical = Tokens.Spacing.md
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ColorTokens.ReactTheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = ColorTokens.ReactTheme.foreground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }

        Icon(
            IconSet.Navigation.forward,
            contentDescription = null,
            tint = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.size(20.dp)
        )
    }
}
