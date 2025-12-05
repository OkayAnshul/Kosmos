package com.example.kosmos.features.users.presentation.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.User
import com.example.kosmos.features.users.presentation.components.UserAvatar
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.layouts.ScreenScaffoldStandard

/**
 * User Profile Screen - Redesigned with Stitch Design
 *
 * Matches reference design:
 * - Large centered avatar (100px+)
 * - Display name + @username
 * - Bio section
 * - Contact information cards (email, LinkedIn, Slack)
 * - Overview stats (Active Projects, On-time Rate)
 * - "Message" button if not current user
 *
 * Stitch Design Features:
 * - Navy background (#1A1D2E)
 * - Card-based layout with proper spacing
 * - Blue accent for interactive elements
 */
@Composable
fun UserProfileScreen(
    user: User?,
    isCurrentUser: Boolean,
    sharedProjectCount: Int,
    onTimeRate: Int? = null, // Percentage (0-100) or null if no completed tasks
    onStartChat: () -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffoldStandard(
        title = "Profile",
        onNavigationClick = onNavigateBack,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorTokens.ReactTheme.background)
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    LoadingIndicator(
                        message = "Loading profile...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    ErrorState(
                        title = "Failed to load profile",
                        message = error,
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                user != null -> {
                    UserProfileContent(
                        user = user,
                        isCurrentUser = isCurrentUser,
                        sharedProjectCount = sharedProjectCount,
                        onTimeRate = onTimeRate,
                        onStartChat = onStartChat
                    )
                }
            }
        }
    }
}

/**
 * User Profile Content
 */
@Composable
private fun UserProfileContent(
    user: User,
    isCurrentUser: Boolean,
    sharedProjectCount: Int,
    onTimeRate: Int?,
    onStartChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Tokens.Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Tokens.Spacing.lg))

        // Large Avatar (100px)
        UserAvatar(
            photoUrl = user.photoUrl,
            displayName = user.displayName,
            isOnline = user.isOnline,
            size = 100.dp,
            showOnlineIndicator = false
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.md))

        // Display Name
        Text(
            text = user.displayName,
            style = TypographyTokens.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ColorTokens.ReactTheme.foreground
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.xxs))

        // @Username
        Text(
            text = "@${user.username}",
            style = TypographyTokens.typography.bodyLarge,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        Spacer(modifier = Modifier.height(Tokens.Spacing.md))

        // Message Button (if not current user)
        if (!isCurrentUser) {
            Button(
                onClick = onStartChat,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorTokens.ReactTheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.Spacing.xl)
            ) {
                Icon(
                    imageVector = IconSet.Message.send,
                    contentDescription = "",
                    modifier = Modifier.size(Tokens.Size.iconSmall)
                )
                Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                Text("Message")
            }

            Spacer(modifier = Modifier.height(Tokens.Spacing.lg))
        }

        // Bio Section
        if (!user.bio.isNullOrBlank()) {
            ProfileSection(title = "Bio") {
                Text(
                    text = user.bio,
                    style = TypographyTokens.typography.bodyMedium,
                    color = ColorTokens.ReactTheme.foreground
                )
            }
        }

        // Contact Information
        val context = LocalContext.current
        ProfileSection(title = "Contact") {
            // Email
            ContactCard(
                icon = IconSet.Message.send,
                label = "Email",
                value = user.email,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${user.email}"))
                    context.startActivity(intent)
                }
            )

            // LinkedIn (if available)
            user.linkedinUrl?.let { url ->
                Spacer(modifier = Modifier.height(Tokens.Spacing.xs))
                ContactCard(
                    icon = IconSet.Action.share,
                    label = "LinkedIn",
                    value = "View Profile",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }

            // GitHub (if available)
            user.githubUrl?.let { url ->
                Spacer(modifier = Modifier.height(Tokens.Spacing.xs))
                ContactCard(
                    icon = IconSet.Action.share,
                    label = "GitHub",
                    value = "View Profile",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Overview Stats
        ProfileSection(title = "Overview") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Active Projects",
                    value = sharedProjectCount.toString()
                )
                // Placeholder for other stats
                StatItem(
                    label = "On-time Rate",
                    value = onTimeRate?.let { "$it%" } ?: "—"
                )
            }
        }

        Spacer(modifier = Modifier.height(Tokens.Spacing.xxl))
    }
}

/**
 * Profile Section Container
 */
@Composable
private fun ProfileSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Tokens.Spacing.sm)
    ) {
        Text(
            text = title,
            style = TypographyTokens.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ColorTokens.ReactTheme.foreground,
            modifier = Modifier.padding(bottom = Tokens.Spacing.sm)
        )

        content()
    }
}

/**
 * Contact Card
 */
@Composable
private fun ContactCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "",
                    tint = ColorTokens.ReactTheme.primary,
                    modifier = Modifier.size(Tokens.Size.iconSmall)
                )
                Column {
                    Text(
                        text = label,
                        style = TypographyTokens.typography.labelMedium,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                    Text(
                        text = value,
                        style = TypographyTokens.typography.bodyMedium,
                        color = ColorTokens.ReactTheme.foreground
                    )
                }
            }

            Icon(
                imageVector = IconSet.Navigation.forward,
                contentDescription = "",
                tint = ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.size(Tokens.Size.iconSmall)
            )
        }
    }
}

/**
 * Stat Item
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = TypographyTokens.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ColorTokens.ReactTheme.primary
        )
        Text(
            text = label,
            style = TypographyTokens.typography.bodySmall,
            color = ColorTokens.ReactTheme.mutedForeground
        )
    }
}
