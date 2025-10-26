package com.example.kosmos.features.users.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * User Avatar Component
 * Displays user profile picture or initials with online status indicator
 */
@Composable
fun UserAvatar(
    photoUrl: String?,
    displayName: String,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    showOnlineIndicator: Boolean = true
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Avatar Image or Placeholder
        if (photoUrl?.isNotEmpty() == true) {
            // Load image from URL
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$displayName avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder with initials
            AvatarPlaceholder(
                displayName = displayName,
                size = size
            )
        }

        // Online Status Indicator
        if (showOnlineIndicator && isOnline) {
            OnlineStatusIndicator(
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

/**
 * Avatar Placeholder with Initials
 */
@Composable
private fun AvatarPlaceholder(
    displayName: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val initials = getInitials(displayName)
    val backgroundColor = getColorForName(displayName)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isNotEmpty()) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value / 2.2).sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

/**
 * Online Status Indicator (Green Dot)
 */
@Composable
private fun OnlineStatusIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(Color(0xFF4CAF50)) // Green
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
    )
}

/**
 * Get initials from display name
 * Examples: "John Doe" -> "JD", "Alice" -> "A"
 */
private fun getInitials(displayName: String): String {
    val parts = displayName.trim().split(" ")
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> "${parts[0].take(1)}${parts.last().take(1)}".uppercase()
    }
}

/**
 * Generate a consistent color for a given name
 * Uses hash to ensure same name always gets same color
 */
private fun getColorForName(name: String): Color {
    val colors = listOf(
        Color(0xFF1976D2), // Blue
        Color(0xFF388E3C), // Green
        Color(0xFFD32F2F), // Red
        Color(0xFF7B1FA2), // Purple
        Color(0xFFF57C00), // Orange
        Color(0xFF0097A7), // Cyan
        Color(0xFF5D4037), // Brown
        Color(0xFF455A64), // Blue Grey
        Color(0xFFE64A19), // Deep Orange
        Color(0xFF00796B), // Teal
    )

    val hash = name.hashCode()
    val index = kotlin.math.abs(hash) % colors.size
    return colors[index]
}
