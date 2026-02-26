package com.example.kosmos.features.settings.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.core.config.AppConfig
import com.example.kosmos.shared.ui.components.LoadingIndicator
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.layouts.ScreenScaffoldStandard

/**
 * Settings Screen - Redesigned with Stitch Design
 *
 * Features:
 * - Grouped sections:
 *   - Account (Profile, Privacy, Notifications)
 *   - Preferences (Language, Theme, Data Usage)
 *   - About (Version, Help, Terms, Privacy Policy)
 * - Each item with icon + label + right arrow
 * - "Clear Cache" with confirmation
 * - "Logout" in destructive red
 *
 * Stitch Design Features:
 * - Navy background (#1A1D2E)
 * - Card-based layout
 * - Blue accent for interactive elements
 */
@Composable
fun SettingsScreen(
    appVersion: String,
    appConfig: AppConfig = AppConfig(),
    isClearing: Boolean,
    isLoggingOut: Boolean,
    onNavigateToProfile: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onClearCache: () -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ScreenScaffoldStandard(
        title = "Settings",
        onNavigationClick = onNavigateBack,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorTokens.ReactTheme.background)
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Tokens.Spacing.md,
                    vertical = Tokens.Spacing.sm
                )
            ) {
                // Feedback Section (first)
                item {
                    SectionHeader(title = "Feedback")
                }

                item {
                    SettingsItem(
                        icon = Icons.Filled.Feedback,
                        title = "Send Feedback",
                        subtitle = "Share suggestions or report issues",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${appConfig.feedbackEmail}")
                                putExtra(Intent.EXTRA_SUBJECT, "${appConfig.appName} Feedback")
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(Tokens.Spacing.md)) }

                // Account Section
                item {
                    SectionHeader(title = "Account")
                }

                items(accountSettings) { setting ->
                    SettingsItem(
                        icon = setting.icon,
                        title = setting.title,
                        subtitle = setting.subtitle,
                        onClick = {
                            when (setting.id) {
                                "profile" -> onNavigateToProfile()
                                "privacy" -> onNavigateToPrivacy()
                                "notifications" -> onNavigateToNotifications()
                            }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(Tokens.Spacing.md)) }

                // Preferences Section
                item {
                    SectionHeader(title = "Preferences")
                }

                items(preferencesSettings) { setting ->
                    SettingsItem(
                        icon = setting.icon,
                        title = setting.title,
                        subtitle = setting.subtitle,
                        onClick = {
                            // TODO: Implement preferences screens
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(Tokens.Spacing.md)) }

                // Storage Section
                item {
                    SectionHeader(title = "Storage")
                }

                item {
                    SettingsItem(
                        icon = IconSet.Action.delete,
                        title = "Clear Cache",
                        subtitle = "Free up storage space",
                        onClick = { showClearCacheDialog = true },
                        isDestructive = false
                    )
                }

                item { Spacer(modifier = Modifier.height(Tokens.Spacing.md)) }

                // About Section
                item {
                    SectionHeader(title = "About")
                }

                items(aboutSettings(appVersion)) { setting ->
                    SettingsItem(
                        icon = setting.icon,
                        title = setting.title,
                        subtitle = setting.subtitle,
                        onClick = {
                            val url = when (setting.id) {
                                "help" -> appConfig.supportUrl
                                "terms" -> appConfig.termsUrl
                                "privacy_policy" -> appConfig.privacyUrl
                                else -> null
                            }
                            url?.let {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                context.startActivity(intent)
                            }
                        },
                        showArrow = setting.id != "version"
                    )
                }

                item { Spacer(modifier = Modifier.height(Tokens.Spacing.md)) }

                // Developer Section
                item {
                    SectionHeader(title = "Developer")
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Tokens.Spacing.xxs),
                        shape = MaterialTheme.shapes.medium,
                        color = ColorTokens.ReactTheme.card
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Tokens.Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                        ) {
                            Text(
                                text = appConfig.appName,
                                style = TypographyTokens.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ColorTokens.ReactTheme.primary
                            )
                            Text(
                                text = appConfig.appDescription,
                                style = TypographyTokens.typography.bodySmall,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                            HorizontalDivider(color = ColorTokens.ReactTheme.border)
                            Text(
                                text = "Built With",
                                style = TypographyTokens.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = ColorTokens.ReactTheme.foreground
                            )
                            Text(
                                text = appConfig.builtWith,
                                style = TypographyTokens.typography.bodySmall,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                            HorizontalDivider(color = ColorTokens.ReactTheme.border)
                            Text(
                                text = "Credits",
                                style = TypographyTokens.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = ColorTokens.ReactTheme.foreground
                            )
                            Text(
                                text = appConfig.credits,
                                style = TypographyTokens.typography.bodySmall,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(Tokens.Spacing.md)) }

                // Logout Section
                item {
                    SettingsItem(
                        icon = IconSet.User.logout,
                        title = "Logout",
                        subtitle = "Sign out of your account",
                        onClick = { showLogoutDialog = true },
                        isDestructive = true
                    )
                }

                item { Spacer(modifier = Modifier.height(Tokens.Spacing.xl)) }
            }

            // Loading overlay
            if (isClearing || isLoggingOut) {
                LoadingIndicator(
                    message = if (isClearing) "Clearing cache..." else "Logging out...",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        ConfirmationDialog(
            title = "Clear Cache?",
            message = "This will delete all cached data including images and offline content. You may need to re-download content.",
            confirmText = "Clear",
            onConfirm = {
                onClearCache()
                showClearCacheDialog = false
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Logout?",
            message = "Are you sure you want to logout? Any unsaved changes will be lost.",
            confirmText = "Logout",
            isDestructive = true,
            onConfirm = {
                onLogout()
                showLogoutDialog = false
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

/**
 * Section Header
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = TypographyTokens.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        color = ColorTokens.ReactTheme.mutedForeground,
        modifier = modifier.padding(
            vertical = Tokens.Spacing.sm,
            horizontal = Tokens.Spacing.xs
        )
    )
}

/**
 * Settings Item
 */
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArrow: Boolean = true,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Tokens.Spacing.xxs),
        shape = MaterialTheme.shapes.medium,
        color = ColorTokens.ReactTheme.card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
                modifier = Modifier.weight(1f)
            ) {
                // Icon
                Icon(
                    imageVector = icon,
                    contentDescription = "",
                    tint = if (isDestructive) {
                        ColorTokens.ReactTheme.destructive
                    } else {
                        ColorTokens.ReactTheme.primary
                    },
                    modifier = Modifier.size(24.dp)
                )

                // Title and subtitle
                Column {
                    Text(
                        text = title,
                        style = TypographyTokens.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isDestructive) {
                            ColorTokens.ReactTheme.destructive
                        } else {
                            ColorTokens.ReactTheme.foreground
                        }
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = TypographyTokens.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }
            }

            // Arrow
            if (showArrow) {
                Icon(
                    imageVector = IconSet.Navigation.forward,
                    contentDescription = "",
                    tint = ColorTokens.ReactTheme.mutedForeground,
                    modifier = Modifier.size(Tokens.Size.iconSmall)
                )
            }
        }
    }
}

/**
 * Confirmation Dialog
 */
@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = TypographyTokens.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = ColorTokens.ReactTheme.foreground
            )
        },
        text = {
            Text(
                text = message,
                style = TypographyTokens.typography.bodyMedium,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) {
                        ColorTokens.ReactTheme.destructive
                    } else {
                        ColorTokens.ReactTheme.primary
                    }
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = ColorTokens.ReactTheme.foreground
                )
            }
        },
        containerColor = ColorTokens.ReactTheme.card,
        modifier = modifier
    )
}

/**
 * Settings Data Classes
 */
private data class SettingItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String?
)

private val accountSettings = listOf(
    SettingItem(
        id = "profile",
        icon = IconSet.User.person,
        title = "Profile",
        subtitle = "Edit your profile information"
    ),
    SettingItem(
        id = "privacy",
        icon = IconSet.Settings.privacy,
        title = "Privacy",
        subtitle = "Manage your privacy settings"
    ),
    SettingItem(
        id = "notifications",
        icon = IconSet.Settings.notifications,
        title = "Notifications",
        subtitle = "Configure notification preferences"
    )
)

private val preferencesSettings = listOf(
    SettingItem(
        id = "language",
        icon = IconSet.Settings.language,
        title = "Language",
        subtitle = "English"
    ),
    SettingItem(
        id = "theme",
        icon = IconSet.Settings.darkMode,
        title = "Theme",
        subtitle = "Dark"
    ),
    SettingItem(
        id = "data",
        icon = IconSet.Action.sync,
        title = "Data Usage",
        subtitle = "Manage offline sync settings"
    )
)

private fun aboutSettings(version: String) = listOf(
    SettingItem(
        id = "version",
        icon = IconSet.Settings.info,
        title = "Version",
        subtitle = version
    ),
    SettingItem(
        id = "help",
        icon = IconSet.Settings.help,
        title = "Help & Support",
        subtitle = "Get help or contact support"
    ),
    SettingItem(
        id = "terms",
        icon = IconSet.File.file,
        title = "Terms of Service",
        subtitle = null
    ),
    SettingItem(
        id = "privacy_policy",
        icon = IconSet.Settings.security,
        title = "Privacy Policy",
        subtitle = null
    )
)
