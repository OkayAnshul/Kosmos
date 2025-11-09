package com.example.kosmos.features.profile.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.kosmos.features.auth.presentation.AuthViewModel
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.components.IconButtonStandard
import com.example.kosmos.shared.ui.components.PrimaryButton

/**
 * Edit Profile Screen
 * Allows users to update their profile information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser = uiState.currentUser

    // Form state
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var username by remember { mutableStateOf(currentUser?.username ?: "") }
    var bio by remember { mutableStateOf(currentUser?.bio ?: "") }
    var age by remember { mutableStateOf(currentUser?.age?.toString() ?: "") }
    var role by remember { mutableStateOf(currentUser?.role ?: "") }
    var location by remember { mutableStateOf(currentUser?.location ?: "") }
    var githubUrl by remember { mutableStateOf(currentUser?.githubUrl ?: "") }
    var twitterUrl by remember { mutableStateOf(currentUser?.twitterUrl ?: "") }
    var linkedinUrl by remember { mutableStateOf(currentUser?.linkedinUrl ?: "") }
    var websiteUrl by remember { mutableStateOf(currentUser?.websiteUrl ?: "") }
    var portfolioUrl by remember { mutableStateOf(currentUser?.portfolioUrl ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSocialLinks by remember { mutableStateOf(false) }

    // Validation
    var displayNameError by remember { mutableStateOf(false) }
    var bioCharCount by remember { mutableStateOf(bio.length) }
    val bioMaxLength = 500

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Success/Error handling
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            // Show error snackbar
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButtonStandard(
                        icon = IconSet.Navigation.back,
                        onClick = onNavigateBack,
                        contentDescription = "Back"
                    )
                },
                actions = {
                    // Save button in top bar
                    TextButton(
                        onClick = {
                            if (displayName.isBlank()) {
                                displayNameError = true
                            } else if (currentUser != null) {
                                viewModel.updateProfile(
                                    displayName = displayName.trim(),
                                    bio = bio.trim(),
                                    age = age.toIntOrNull(),
                                    role = role.trim().takeIf { it.isNotBlank() },
                                    location = location.trim().takeIf { it.isNotBlank() },
                                    githubUrl = githubUrl.trim().takeIf { it.isNotBlank() },
                                    twitterUrl = twitterUrl.trim().takeIf { it.isNotBlank() },
                                    linkedinUrl = linkedinUrl.trim().takeIf { it.isNotBlank() },
                                    websiteUrl = websiteUrl.trim().takeIf { it.isNotBlank() },
                                    portfolioUrl = portfolioUrl.trim().takeIf { it.isNotBlank() },
                                    photoUri = selectedImageUri
                                )
                            }
                        },
                        enabled = !uiState.isLoading && displayName.isNotBlank()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Tokens.Size.iconSmall),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Tokens.Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Avatar Editor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(Tokens.Size.avatarXXLarge)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val imageUrl = selectedImageUri?.toString() ?: currentUser?.photoUrl
                    if (imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
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
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Change photo",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(IconSet.Action.edit, contentDescription = null, modifier = Modifier.size(Tokens.Size.iconSmall))
                    Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                    Text("Change Photo")
                }
            }

            HorizontalDivider()

            // Required Fields
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    displayNameError = false
                },
                label = { Text("Display Name *") },
                isError = displayNameError,
                supportingText = if (displayNameError) {
                    { Text("Display name is required") }
                } else null,
                singleLine = true,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = username,
                onValueChange = { /* Username is read-only after signup */ },
                label = { Text("Username") },
                prefix = { Text("@") },
                enabled = false,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text("Username cannot be changed", style = MaterialTheme.typography.bodySmall)
                }
            )

            OutlinedTextField(
                value = bio,
                onValueChange = {
                    if (it.length <= bioMaxLength) {
                        bio = it
                        bioCharCount = it.length
                    }
                },
                label = { Text("Bio") },
                placeholder = { Text("Tell us about yourself...") },
                minLines = 3,
                maxLines = 5,
                enabled = !uiState.isLoading,
                supportingText = {
                    Text(
                        "$bioCharCount / $bioMaxLength characters",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // Optional Fields
            Text(
                text = "Additional Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = age,
                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) age = it },
                label = { Text("Age") },
                placeholder = { Text("e.g., 28") },
                singleLine = true,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = role,
                onValueChange = { role = it },
                label = { Text("Role/Title") },
                placeholder = { Text("e.g., Software Engineer") },
                singleLine = true,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                placeholder = { Text("e.g., San Francisco, CA") },
                singleLine = true,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // Social Links (Expandable)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSocialLinks = !showSocialLinks }
                        .padding(vertical = Tokens.Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Social Links",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        if (showSocialLinks) IconSet.Action.collapse else IconSet.Action.expand,
                        contentDescription = if (showSocialLinks) "Collapse" else "Expand"
                    )
                }

                if (showSocialLinks) {
                    OutlinedTextField(
                        value = githubUrl,
                        onValueChange = { githubUrl = it },
                        label = { Text("GitHub") },
                        placeholder = { Text("github.com/username") },
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = twitterUrl,
                        onValueChange = { twitterUrl = it },
                        label = { Text("Twitter/X") },
                        placeholder = { Text("twitter.com/username") },
                        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = linkedinUrl,
                        onValueChange = { linkedinUrl = it },
                        label = { Text("LinkedIn") },
                        placeholder = { Text("linkedin.com/in/username") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        label = { Text("Website") },
                        placeholder = { Text("yourwebsite.com") },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = portfolioUrl,
                        onValueChange = { portfolioUrl = it },
                        label = { Text("Portfolio") },
                        placeholder = { Text("portfolio.com") },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(Tokens.Spacing.lg))

            // Save Button (duplicate at bottom for convenience)
            Button(
                onClick = {
                    if (displayName.isBlank()) {
                        displayNameError = true
                    } else if (currentUser != null) {
                        viewModel.updateProfile(
                            displayName = displayName.trim(),
                            bio = bio.trim(),
                            age = age.toIntOrNull(),
                            role = role.trim().takeIf { it.isNotBlank() },
                            location = location.trim().takeIf { it.isNotBlank() },
                            githubUrl = githubUrl.trim().takeIf { it.isNotBlank() },
                            twitterUrl = twitterUrl.trim().takeIf { it.isNotBlank() },
                            linkedinUrl = linkedinUrl.trim().takeIf { it.isNotBlank() },
                            websiteUrl = websiteUrl.trim().takeIf { it.isNotBlank() },
                            portfolioUrl = portfolioUrl.trim().takeIf { it.isNotBlank() },
                            photoUri = selectedImageUri
                        )
                    }
                },
                enabled = !uiState.isLoading && displayName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Tokens.Size.iconSmall),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                }
                Text("Save Changes")
            }
        }
    }
}
