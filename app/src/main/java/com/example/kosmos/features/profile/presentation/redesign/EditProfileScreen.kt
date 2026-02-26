package com.example.kosmos.features.profile.presentation.redesign

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.kosmos.core.models.User
import com.example.kosmos.shared.ui.designsystem.*

/**
 * Stitch Design EditProfileScreen
 *
 * Features:
 * - Navy background matching Stitch design
 * - Large centered avatar editor (120dp)
 * - All 17 profile fields with proper validation
 * - Photo upload UI (backend wiring in Day 6)
 * - Expandable social links section
 * - Save button in TopBar + bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    user: User?,
    isLoading: Boolean,
    onSaveProfile: (
        displayName: String,
        bio: String,
        age: Int?,
        role: String?,
        location: String?,
        githubUrl: String?,
        twitterUrl: String?,
        linkedinUrl: String?,
        websiteUrl: String?,
        portfolioUrl: String?,
        photoUri: Uri?
    ) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Form state
    var displayName by remember { mutableStateOf(user?.displayName ?: "") }
    var bio by remember { mutableStateOf(user?.bio ?: "") }
    var age by remember { mutableStateOf(user?.age?.toString() ?: "") }
    var role by remember { mutableStateOf(user?.role ?: "") }
    var location by remember { mutableStateOf(user?.location ?: "") }
    var githubUrl by remember { mutableStateOf(user?.githubUrl ?: "") }
    var twitterUrl by remember { mutableStateOf(user?.twitterUrl ?: "") }
    var linkedinUrl by remember { mutableStateOf(user?.linkedinUrl ?: "") }
    var websiteUrl by remember { mutableStateOf(user?.websiteUrl ?: "") }
    var portfolioUrl by remember { mutableStateOf(user?.portfolioUrl ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSocialLinks by remember { mutableStateOf(false) }

    // Validation
    var displayNameError by remember { mutableStateOf(false) }
    val bioMaxLength = 500

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val handleSave = {
        if (displayName.isBlank()) {
            displayNameError = true
        } else {
            onSaveProfile(
                displayName.trim(),
                bio.trim(),
                age.toIntOrNull(),
                role.trim().takeIf { it.isNotBlank() },
                location.trim().takeIf { it.isNotBlank() },
                githubUrl.trim().takeIf { it.isNotBlank() },
                twitterUrl.trim().takeIf { it.isNotBlank() },
                linkedinUrl.trim().takeIf { it.isNotBlank() },
                websiteUrl.trim().takeIf { it.isNotBlank() },
                portfolioUrl.trim().takeIf { it.isNotBlank() },
                selectedImageUri
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = { Text("Edit Profile", color = ColorTokens.ReactTheme.foreground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = ColorTokens.ReactTheme.foreground
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = handleSave,
                        enabled = !isLoading && displayName.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = ColorTokens.ReactTheme.primary
                            )
                        } else {
                            Text(
                                "Save",
                                color = ColorTokens.ReactTheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorTokens.ReactTheme.card
                )
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Avatar Editor
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                        .background(ColorTokens.ReactTheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    val imageUrl = selectedImageUri?.toString() ?: user?.photoUrl
                    if (imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                    }
                }

                TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "",
                        modifier = Modifier.size(18.dp),
                        tint = ColorTokens.ReactTheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Change Photo",
                        color = ColorTokens.ReactTheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Basic Information Section
                Text(
                    "BASIC INFORMATION",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTokens.ReactTheme.mutedForeground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                // Display Name *
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                        displayNameError = false
                    },
                    label = { Text("Display Name *") },
                    isError = displayNameError,
                    supportingText = if (displayNameError) {
                        { Text("Display name is required", color = ColorTokens.Error.light) }
                    } else null,
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorTokens.ReactTheme.card,
                        unfocusedContainerColor = ColorTokens.ReactTheme.card,
                        focusedTextColor = ColorTokens.ReactTheme.foreground,
                        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                        focusedBorderColor = ColorTokens.ReactTheme.primary,
                        unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                        focusedLabelColor = ColorTokens.ReactTheme.primary,
                        unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
                    )
                )

                // Username (read-only)
                OutlinedTextField(
                    value = user?.username ?: "",
                    onValueChange = {},
                    label = { Text("Username") },
                    prefix = { Text("@", color = ColorTokens.ReactTheme.mutedForeground) },
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Text(
                            "Username cannot be changed",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = ColorTokens.ReactTheme.card.copy(alpha = 0.5f),
                        disabledTextColor = ColorTokens.ReactTheme.mutedForeground,
                        disabledBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.2f),
                        disabledLabelColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.6f)
                    )
                )

                // Bio
                OutlinedTextField(
                    value = bio,
                    onValueChange = {
                        if (it.length <= bioMaxLength) {
                            bio = it
                        }
                    },
                    label = { Text("Bio") },
                    placeholder = { Text("Tell us about yourself...", color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)) },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isLoading,
                    supportingText = {
                        Text(
                            "${bio.length} / $bioMaxLength characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorTokens.ReactTheme.card,
                        unfocusedContainerColor = ColorTokens.ReactTheme.card,
                        focusedTextColor = ColorTokens.ReactTheme.foreground,
                        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                        focusedBorderColor = ColorTokens.ReactTheme.primary,
                        unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                        focusedLabelColor = ColorTokens.ReactTheme.primary,
                        unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Additional Information Section
                Text(
                    "ADDITIONAL INFORMATION",
                    style = MaterialTheme.typography.labelMedium,
                    color = ColorTokens.ReactTheme.mutedForeground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                // Age
                OutlinedTextField(
                    value = age,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) age = it },
                    label = { Text("Age") },
                    placeholder = { Text("e.g., 28", color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorTokens.ReactTheme.card,
                        unfocusedContainerColor = ColorTokens.ReactTheme.card,
                        focusedTextColor = ColorTokens.ReactTheme.foreground,
                        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                        focusedBorderColor = ColorTokens.ReactTheme.primary,
                        unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                        focusedLabelColor = ColorTokens.ReactTheme.primary,
                        unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
                    )
                )

                // Role/Title
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role/Title") },
                    placeholder = { Text("e.g., Software Engineer", color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorTokens.ReactTheme.card,
                        unfocusedContainerColor = ColorTokens.ReactTheme.card,
                        focusedTextColor = ColorTokens.ReactTheme.foreground,
                        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                        focusedBorderColor = ColorTokens.ReactTheme.primary,
                        unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                        focusedLabelColor = ColorTokens.ReactTheme.primary,
                        unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
                    )
                )

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    placeholder = { Text("e.g., San Francisco, CA", color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorTokens.ReactTheme.card,
                        unfocusedContainerColor = ColorTokens.ReactTheme.card,
                        focusedTextColor = ColorTokens.ReactTheme.foreground,
                        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                        focusedBorderColor = ColorTokens.ReactTheme.primary,
                        unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                        focusedLabelColor = ColorTokens.ReactTheme.primary,
                        unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Social Links (Expandable)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ColorTokens.ReactTheme.card
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSocialLinks = !showSocialLinks }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SOCIAL LINKS",
                                style = MaterialTheme.typography.labelMedium,
                                color = ColorTokens.ReactTheme.mutedForeground,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                if (showSocialLinks) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showSocialLinks) "Collapse" else "Expand",
                                tint = ColorTokens.ReactTheme.mutedForeground
                            )
                        }

                        if (showSocialLinks) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // GitHub
                                OutlinedTextField(
                                    value = githubUrl,
                                    onValueChange = { githubUrl = it },
                                    label = { Text("GitHub") },
                                    placeholder = { Text("github.com/username", color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)) },
                                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = "", tint = Color.White) },
                                    singleLine = true,
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = ColorTokens.ReactTheme.card,
                                        unfocusedContainerColor = ColorTokens.ReactTheme.card,
                                        focusedTextColor = ColorTokens.ReactTheme.foreground,
                                        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                                        focusedBorderColor = ColorTokens.ReactTheme.primary,
                                        unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                                        focusedLabelColor = ColorTokens.ReactTheme.primary,
                                        unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
                                    )
                                )

                                // Twitter
                                OutlinedTextField(
                                    value = twitterUrl,
                                    onValueChange = { twitterUrl = it },
                                    label = { Text("Twitter/X") },
                                    placeholder = { Text("twitter.com/username", color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)) },
                                    leadingIcon = { Icon(Icons.Default.Tag, contentDescription = "", tint = ColorTokens.ReactTheme.primary) },
                                    singleLine = true,
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = ColorTokens.ReactTheme.card,
                                        unfocusedContainerColor = ColorTokens.ReactTheme.card,
                                        focusedTextColor = ColorTokens.ReactTheme.foreground,
                                        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                                        focusedBorderColor = ColorTokens.ReactTheme.primary,
                                        unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                                        focusedLabelColor = ColorTokens.ReactTheme.primary,
                                        unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
                                    )
                                )

                                // LinkedIn
                                OutlinedTextField(
                                    value = linkedinUrl,
                                    onValueChange = { linkedinUrl = it },
                                    label = { Text("LinkedIn") },
                                    placeholder = { Text("linkedin.com/in/username", color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)) },
                                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = "", tint = Color(0xFF0A66C2)) },
                                    singleLine = true,
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = ColorTokens.ReactTheme.card,
                                        unfocusedContainerColor = ColorTokens.ReactTheme.card,
                                        focusedTextColor = ColorTokens.ReactTheme.foreground,
                                        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                                        focusedBorderColor = ColorTokens.ReactTheme.primary,
                                        unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                                        focusedLabelColor = ColorTokens.ReactTheme.primary,
                                        unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
                                    )
                                )

                                // Website
                                OutlinedTextField(
                                    value = websiteUrl,
                                    onValueChange = { websiteUrl = it },
                                    label = { Text("Website") },
                                    placeholder = { Text("yourwebsite.com", color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)) },
                                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = "", tint = ColorTokens.ReactTheme.primary) },
                                    singleLine = true,
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = ColorTokens.ReactTheme.card,
                                        unfocusedContainerColor = ColorTokens.ReactTheme.card,
                                        focusedTextColor = ColorTokens.ReactTheme.foreground,
                                        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                                        focusedBorderColor = ColorTokens.ReactTheme.primary,
                                        unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                                        focusedLabelColor = ColorTokens.ReactTheme.primary,
                                        unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
                                    )
                                )

                                // Portfolio
                                OutlinedTextField(
                                    value = portfolioUrl,
                                    onValueChange = { portfolioUrl = it },
                                    label = { Text("Portfolio") },
                                    placeholder = { Text("portfolio.com", color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)) },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = "", tint = ColorTokens.ReactTheme.primary) },
                                    singleLine = true,
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = ColorTokens.ReactTheme.card,
                                        unfocusedContainerColor = ColorTokens.ReactTheme.card,
                                        focusedTextColor = ColorTokens.ReactTheme.foreground,
                                        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
                                        focusedBorderColor = ColorTokens.ReactTheme.primary,
                                        unfocusedBorderColor = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.3f),
                                        focusedLabelColor = ColorTokens.ReactTheme.primary,
                                        unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Button (bottom)
                Button(
                    onClick = handleSave,
                    enabled = !isLoading && displayName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorTokens.ReactTheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        "Save Changes",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
