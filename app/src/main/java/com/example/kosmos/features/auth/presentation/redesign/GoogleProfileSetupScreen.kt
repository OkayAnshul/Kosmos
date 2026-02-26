package com.example.kosmos.features.auth.presentation.redesign

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.features.auth.presentation.AuthUiState
import com.example.kosmos.shared.ui.components.LoadingButton
import com.example.kosmos.shared.ui.components.PrimaryButton
import com.example.kosmos.shared.ui.components.SecondaryButton
import com.example.kosmos.shared.ui.designsystem.ColorTokens

/**
 * Profile setup wizard shown after Google sign-in for new users.
 *
 * Covers steps 2–4 of the normal sign-up wizard:
 *   Step 1 — Identity   (username, display name)
 *   Step 2 — Profile    (role, location, age, bio)
 *   Step 3 — Links      (GitHub, Twitter, LinkedIn, Website, Portfolio)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun GoogleProfileSetupScreen(
    uiState: AuthUiState,
    onCheckUsernameAvailability: (String) -> Unit,
    onSaveProfile: (
        displayName: String,
        username: String,
        age: Int?,
        role: String?,
        bio: String?,
        location: String?,
        githubUrl: String?,
        twitterUrl: String?,
        linkedinUrl: String?,
        websiteUrl: String?,
        portfolioUrl: String?
    ) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSteps = 3
    var currentStep by remember { mutableStateOf(1) }
    var stepDirection by remember { mutableStateOf(1) }

    // Step 1 — Identity
    val prefillName = uiState.currentUser?.displayName ?: ""
    var displayName by remember { mutableStateOf(prefillName) }
    var username by remember { mutableStateOf("") }

    // Step 2 — Profile
    var role by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    // Step 3 — Links
    var githubUrl by remember { mutableStateOf("") }
    var twitterUrl by remember { mutableStateOf("") }
    var linkedinUrl by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var portfolioUrl by remember { mutableStateOf("") }

    fun goForward() { stepDirection = 1; if (currentStep < totalSteps) currentStep++ }
    fun goBack() { stepDirection = -1; if (currentStep > 1) currentStep-- }

    fun submit() {
        onSaveProfile(
            displayName.trim(),
            username.trim(),
            age.toIntOrNull(),
            role.trim().ifBlank { null },
            bio.trim().ifBlank { null },
            location.trim().ifBlank { null },
            githubUrl.trim().ifBlank { null },
            twitterUrl.trim().ifBlank { null },
            linkedinUrl.trim().ifBlank { null },
            websiteUrl.trim().ifBlank { null },
            portfolioUrl.trim().ifBlank { null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (currentStep > 1) goBack() else onSkip() }) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = ColorTokens.ReactTheme.foreground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Set up your profile",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "You can update this later in Settings",
                    fontSize = 11.sp,
                    color = ColorTokens.ReactTheme.mutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TextButton(onClick = onSkip) {
                Text("Skip", color = ColorTokens.ReactTheme.mutedForeground, fontSize = 13.sp)
            }
        }

        // Progress
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps },
            modifier = Modifier.fillMaxWidth(),
            color = ColorTokens.ReactTheme.primary,
            trackColor = ColorTokens.ReactTheme.muted
        )

        // Step content
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (stepDirection > 0) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(
                        slideOutHorizontally { it } + fadeOut()
                    )
                }
            },
            modifier = Modifier.weight(1f)
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    1 -> Step2Identity(
                        displayName = displayName, onDisplayNameChange = { displayName = it },
                        username = username,
                        onUsernameChange = {
                            username = it
                            onCheckUsernameAvailability(it)
                        },
                        isCheckingUsername = uiState.isCheckingUsername,
                        isUsernameAvailable = uiState.isUsernameAvailable
                    )
                    2 -> Step3Profile(
                        role = role, onRoleChange = { role = it },
                        location = location, onLocationChange = { location = it },
                        age = age, onAgeChange = { age = it },
                        bio = bio, onBioChange = { bio = it }
                    )
                    3 -> Step4Links(
                        githubUrl = githubUrl, onGithubChange = { githubUrl = it },
                        twitterUrl = twitterUrl, onTwitterChange = { twitterUrl = it },
                        linkedinUrl = linkedinUrl, onLinkedinChange = { linkedinUrl = it },
                        websiteUrl = websiteUrl, onWebsiteChange = { websiteUrl = it },
                        portfolioUrl = portfolioUrl, onPortfolioChange = { portfolioUrl = it }
                    )
                }

                if (uiState.error != null) {
                    Surface(
                        color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = uiState.error,
                            color = ColorTokens.ReactTheme.destructive,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Bottom bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorTokens.ReactTheme.background)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 1) {
                    SecondaryButton(
                        text = "Back",
                        onClick = ::goBack,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (currentStep < totalSteps) {
                    PrimaryButton(
                        text = "Continue",
                        onClick = ::goForward,
                        modifier = Modifier.weight(1f),
                        fullWidth = currentStep == 1
                    )
                } else {
                    LoadingButton(
                        text = "Save & Continue",
                        onClick = ::submit,
                        isLoading = uiState.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
