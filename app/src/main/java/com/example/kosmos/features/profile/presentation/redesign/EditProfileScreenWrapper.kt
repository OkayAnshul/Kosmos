package com.example.kosmos.features.profile.presentation.redesign

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kosmos.features.auth.presentation.AuthViewModel

/**
 * Wrapper for EditProfileScreen with Hilt dependency injection
 *
 * Responsibilities:
 * - Inject AuthViewModel via Hilt
 * - Collect current user state
 * - Handle profile update calls
 * - Delegate navigation actions
 */
@Composable
fun EditProfileScreenWrapper(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    EditProfileScreen(
        user = uiState.currentUser,
        isLoading = uiState.isLoading,
        onSaveProfile = { displayName, bio, age, role, location, githubUrl, twitterUrl, linkedinUrl, websiteUrl, portfolioUrl, photoUri ->
            authViewModel.updateProfile(
                displayName = displayName,
                bio = bio,
                age = age,
                role = role,
                location = location,
                githubUrl = githubUrl,
                twitterUrl = twitterUrl,
                linkedinUrl = linkedinUrl,
                websiteUrl = websiteUrl,
                portfolioUrl = portfolioUrl,
                photoUri = photoUri
            )
        },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
