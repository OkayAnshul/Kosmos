package com.example.kosmos.features.users.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        // Additional Info Section (optional)
        ProfileInfoCard(user = user, sharedProjectCount = sharedProjectCount)
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
