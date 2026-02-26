package com.example.kosmos.features.auth.presentation.redesign

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.features.auth.presentation.AuthUiState
import com.example.kosmos.features.auth.presentation.SignUpData
import com.example.kosmos.shared.ui.components.LoadingButton
import com.example.kosmos.shared.ui.components.PrimaryButton
import com.example.kosmos.shared.ui.components.SecondaryButton
import com.example.kosmos.shared.ui.components.TextButtonStandard
import com.example.kosmos.shared.ui.components.TextFieldMultiline
import com.example.kosmos.shared.ui.components.TextFieldPassword
import com.example.kosmos.shared.ui.components.TextFieldStandard
import com.example.kosmos.shared.ui.designsystem.ColorTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    uiState: AuthUiState,
    onSignUp: (SignUpData) -> Unit,
    onCheckUsernameAvailability: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Step tracking
    var currentStep by remember { mutableStateOf(1) }
    var stepDirection by remember { mutableStateOf(1) } // 1=forward, -1=back

    // Step 1 — Credentials
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Step 2 — Identity
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    // Step 3 — Profile
    var role by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    // Step 4 — Social Links
    var githubUrl by remember { mutableStateOf("") }
    var twitterUrl by remember { mutableStateOf("") }
    var linkedinUrl by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var portfolioUrl by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onSignUpSuccess()
    }

    val totalSteps = 4

    fun goForward() {
        stepDirection = 1
        if (currentStep < totalSteps) currentStep++
    }

    fun goBack() {
        stepDirection = -1
        if (currentStep > 1) currentStep--
    }

    fun submitSignUp() {
        onSignUp(
            SignUpData(
                email = email.trim(),
                password = password,
                displayName = displayName.trim(),
                username = username.trim(),
                age = age.toIntOrNull(),
                role = role.trim().ifBlank { null },
                bio = bio.trim().ifBlank { null },
                location = location.trim().ifBlank { null },
                githubUrl = githubUrl.trim().ifBlank { null },
                twitterUrl = twitterUrl.trim().ifBlank { null },
                linkedinUrl = linkedinUrl.trim().ifBlank { null },
                websiteUrl = websiteUrl.trim().ifBlank { null },
                portfolioUrl = portfolioUrl.trim().ifBlank { null }
            )
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
            IconButton(onClick = {
                if (currentStep > 1) goBack() else onNavigateToLogin()
            }) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = ColorTokens.ReactTheme.foreground
                )
            }
            Text(
                text = "Create Account",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorTokens.ReactTheme.foreground,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "$currentStep / $totalSteps",
                fontSize = 13.sp,
                color = ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.End
            )
        }

        // Progress bar
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
                    1 -> Step1Credentials(
                        email = email, onEmailChange = { email = it },
                        password = password, onPasswordChange = { password = it },
                        confirmPassword = confirmPassword, onConfirmPasswordChange = { confirmPassword = it }
                    )
                    2 -> Step2Identity(
                        displayName = displayName, onDisplayNameChange = { displayName = it },
                        username = username,
                        onUsernameChange = {
                            username = it
                            onCheckUsernameAvailability(it)
                        },
                        isCheckingUsername = uiState.isCheckingUsername,
                        isUsernameAvailable = uiState.isUsernameAvailable
                    )
                    3 -> Step3Profile(
                        role = role, onRoleChange = { role = it },
                        location = location, onLocationChange = { location = it },
                        age = age, onAgeChange = { age = it },
                        bio = bio, onBioChange = { bio = it }
                    )
                    4 -> Step4Links(
                        githubUrl = githubUrl, onGithubChange = { githubUrl = it },
                        twitterUrl = twitterUrl, onTwitterChange = { twitterUrl = it },
                        linkedinUrl = linkedinUrl, onLinkedinChange = { linkedinUrl = it },
                        websiteUrl = websiteUrl, onWebsiteChange = { websiteUrl = it },
                        portfolioUrl = portfolioUrl, onPortfolioChange = { portfolioUrl = it }
                    )
                }

                // Error display
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

        // Bottom navigation bar
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
                        text = "Create Account",
                        onClick = ::submitSignUp,
                        isLoading = uiState.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = "Powered by Supabase · by Aravya",
                fontSize = 11.sp,
                color = ColorTokens.ReactTheme.mutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun StepHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground
        )
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = ColorTokens.ReactTheme.mutedForeground
        )
    }
}

@Composable
internal fun Step1Credentials(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmPasswordChange: (String) -> Unit
) {
    val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword

    StepHeader(title = "Your credentials", subtitle = "Start with your email and a secure password")

    TextFieldStandard(
        value = email,
        onValueChange = onEmailChange,
        label = "Email",
        keyboardType = KeyboardType.Email,
        modifier = Modifier.fillMaxWidth()
    )
    TextFieldPassword(
        value = password,
        onValueChange = onPasswordChange,
        label = "Password",
        modifier = Modifier.fillMaxWidth()
    )
    TextFieldPassword(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = "Confirm Password",
        isError = !passwordsMatch,
        supportingText = if (!passwordsMatch) "Passwords don't match" else null,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun Step2Identity(
    displayName: String, onDisplayNameChange: (String) -> Unit,
    username: String, onUsernameChange: (String) -> Unit,
    isCheckingUsername: Boolean,
    isUsernameAvailable: Boolean?
) {
    StepHeader(title = "Your identity", subtitle = "How others will find and know you")

    TextFieldStandard(
        value = displayName,
        onValueChange = onDisplayNameChange,
        label = "Full name",
        modifier = Modifier.fillMaxWidth()
    )

    TextFieldStandard(
        value = username,
        onValueChange = onUsernameChange,
        label = "Username",
        trailingIcon = when {
            isCheckingUsername -> null // handled below
            isUsernameAvailable == true -> Icons.Default.CheckCircle
            isUsernameAvailable == false -> Icons.Default.Cancel
            else -> null
        },
        modifier = Modifier.fillMaxWidth()
    )

    // Username availability indicator
    if (isCheckingUsername) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = ColorTokens.ReactTheme.mutedForeground,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Checking availability...",
                fontSize = 12.sp,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
    } else if (isUsernameAvailable == true) {
        Text(
            text = "Username is available",
            fontSize = 12.sp,
            color = Color(0xFF3ECF8E)
        )
    } else if (isUsernameAvailable == false) {
        Text(
            text = "Username is taken",
            fontSize = 12.sp,
            color = ColorTokens.ReactTheme.destructive
        )
    }
}

@Composable
internal fun Step3Profile(
    role: String, onRoleChange: (String) -> Unit,
    location: String, onLocationChange: (String) -> Unit,
    age: String, onAgeChange: (String) -> Unit,
    bio: String, onBioChange: (String) -> Unit
) {
    StepHeader(title = "Tell the world who you are", subtitle = "Your profile (all optional)")

    TextFieldStandard(
        value = role,
        onValueChange = onRoleChange,
        label = "Job title / role (optional)",
        modifier = Modifier.fillMaxWidth()
    )
    TextFieldStandard(
        value = location,
        onValueChange = onLocationChange,
        label = "City, Country (optional)",
        modifier = Modifier.fillMaxWidth()
    )
    TextFieldStandard(
        value = age,
        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) onAgeChange(it) },
        label = "Age (optional)",
        keyboardType = KeyboardType.Number,
        modifier = Modifier.fillMaxWidth()
    )
    TextFieldMultiline(
        value = bio,
        onValueChange = { if (it.length <= 500) onBioChange(it) },
        label = "Bio (optional)",
        maxLines = 5,
        supportingText = "${bio.length}/500",
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun Step4Links(
    githubUrl: String, onGithubChange: (String) -> Unit,
    twitterUrl: String, onTwitterChange: (String) -> Unit,
    linkedinUrl: String, onLinkedinChange: (String) -> Unit,
    websiteUrl: String, onWebsiteChange: (String) -> Unit,
    portfolioUrl: String, onPortfolioChange: (String) -> Unit
) {
    StepHeader(title = "Connect your world", subtitle = "Social links (optional)")

    TextFieldStandard(
        value = githubUrl,
        onValueChange = onGithubChange,
        label = "GitHub URL",
        keyboardType = KeyboardType.Uri,
        modifier = Modifier.fillMaxWidth()
    )
    TextFieldStandard(
        value = twitterUrl,
        onValueChange = onTwitterChange,
        label = "Twitter / X URL",
        keyboardType = KeyboardType.Uri,
        modifier = Modifier.fillMaxWidth()
    )
    TextFieldStandard(
        value = linkedinUrl,
        onValueChange = onLinkedinChange,
        label = "LinkedIn URL",
        keyboardType = KeyboardType.Uri,
        modifier = Modifier.fillMaxWidth()
    )
    TextFieldStandard(
        value = websiteUrl,
        onValueChange = onWebsiteChange,
        label = "Website URL",
        keyboardType = KeyboardType.Uri,
        modifier = Modifier.fillMaxWidth()
    )
    TextFieldStandard(
        value = portfolioUrl,
        onValueChange = onPortfolioChange,
        label = "Portfolio URL",
        keyboardType = KeyboardType.Uri,
        modifier = Modifier.fillMaxWidth()
    )
}
