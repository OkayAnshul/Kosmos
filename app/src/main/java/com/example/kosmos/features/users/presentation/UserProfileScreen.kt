package com.example.kosmos.features.users.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.core.models.User
import com.example.kosmos.features.users.presentation.components.UserAvatar

/**
 * User Profile Screen
 * Displays detailed information about a user
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    projectId: String,
    onNavigateBack: () -> Unit,
    onStartChat: (String, String) -> Unit, // Navigate to chat with (userId, chatRoomId)
    modifier: Modifier = Modifier,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    // Navigate to chat when created
    LaunchedEffect(uiState.createdChatRoomId) {
        uiState.createdChatRoomId?.let { chatRoomId ->
            onStartChat(userId, chatRoomId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.padding(paddingValues))
            }

            uiState.error != null -> {
                ErrorState(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadUser(userId) },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            uiState.user != null -> {
                ProfileContent(
                    user = uiState.user!!,
                    projectId = projectId,
                    sharedProjectCount = uiState.sharedProjectCount,
                    onStartChat = { chatRoomId ->
                        onStartChat(userId, chatRoomId)
                    },
                    onCreateOrGetChat = { targetUserId ->
                        viewModel.createOrGetDirectChat(projectId, targetUserId)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Profile Content
 */
@Composable
private fun ProfileContent(
    user: User,
    projectId: String,
    sharedProjectCount: Int,
    onStartChat: (String) -> Unit, // chatRoomId
    onCreateOrGetChat: (String) -> Unit, // targetUserId
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Large Avatar
        UserAvatar(
            photoUrl = user.photoUrl,
            displayName = user.displayName,
            isOnline = user.isOnline,
            size = 120.dp,
            showOnlineIndicator = true
        )

        // Display Name
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        // Username
        if (user.username.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "@${user.username}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Email
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Online Status
        OnlineStatusCard(
            isOnline = user.isOnline,
            lastSeen = user.lastSeen
        )

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Chat Button
            Button(
                onClick = { onCreateOrGetChat(user.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Chat")
            }

            // Add to Project Button (placeholder for future)
            OutlinedButton(
                onClick = { /* TODO: Implement add to project */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            ) {
                Text("Add to Project")
            }
        }

        // Additional Info Sections
        ProfileInfoCard(user = user, sharedProjectCount = sharedProjectCount)

        // Bio Section
        if (!user.bio.isNullOrBlank()) {
            BioSection(bio = user.bio)
        }

        // Social Links Section
        SocialLinksSection(user = user)
    }
}

/**
 * Online Status Card
 */
@Composable
private fun OnlineStatusCard(
    isOnline: Boolean,
    lastSeen: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (isOnline) {
                            androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Status Text
            Text(
                text = if (isOnline) {
                    "Online"
                } else {
                    formatLastSeen(lastSeen)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isOnline) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Profile Info Card
 */
@Composable
private fun ProfileInfoCard(
    user: User,
    sharedProjectCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Age
            if (user.age != null && user.age > 0) {
                InfoRow(
                    label = "Age",
                    value = user.age.toString()
                )
            }

            // Role/Title
            if (!user.role.isNullOrBlank()) {
                InfoRow(
                    label = "Role",
                    value = user.role
                )
            }

            // Location
            if (!user.location.isNullOrBlank()) {
                InfoRow(
                    label = "Location",
                    value = user.location
                )
            }

            // Member Since
            InfoRow(
                label = "Member since",
                value = formatMemberSince(user.createdAt)
            )

            // Projects in Common
            InfoRow(
                label = "Projects in common",
                value = sharedProjectCount.toString()
            )
        }
    }
}

/**
 * Info Row Component
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Loading State
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Error State
 */
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Failed to load profile",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

/**
 * Bio Section
 */
@Composable
private fun BioSection(
    bio: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Social Links Section
 */
@Composable
private fun SocialLinksSection(
    user: User,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    // Build list of available social links
    val socialLinks = buildList {
        user.githubUrl?.takeIf { it.isNotBlank() }?.let {
            add(Triple("GitHub", Icons.Default.Code, it))
        }
        user.twitterUrl?.takeIf { it.isNotBlank() }?.let {
            add(Triple("Twitter", Icons.Default.Tag, it))
        }
        user.linkedinUrl?.takeIf { it.isNotBlank() }?.let {
            add(Triple("LinkedIn", Icons.Default.Business, it))
        }
        user.websiteUrl?.takeIf { it.isNotBlank() }?.let {
            add(Triple("Website", Icons.Default.Language, it))
        }
        user.portfolioUrl?.takeIf { it.isNotBlank() }?.let {
            add(Triple("Portfolio", Icons.Default.Folder, it))
        }
    }

    if (socialLinks.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Social Links",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    socialLinks.forEach { (name, icon, url) ->
                        FilledTonalIconButton(
                            onClick = {
                                try {
                                    val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                        "https://$url"
                                    } else {
                                        url
                                    }
                                    uriHandler.openUri(formattedUrl)
                                } catch (e: Exception) {
                                    // Handle URL error gracefully
                                }
                            }
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format last seen timestamp
 */
private fun formatLastSeen(timestamp: Long): String {
    if (timestamp == 0L) return "Last seen a while ago"

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Last seen just now"
        diff < 3600_000 -> "Last seen ${diff / 60_000}m ago"
        diff < 86400_000 -> "Last seen ${diff / 3600_000}h ago"
        diff < 604800_000 -> "Last seen ${diff / 86400_000}d ago"
        else -> "Last seen a while ago"
    }
}

/**
 * Format member since date
 */
private fun formatMemberSince(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
    return format.format(date)
}
