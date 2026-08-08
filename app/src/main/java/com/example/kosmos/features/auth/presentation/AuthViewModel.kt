package com.example.kosmos.features.auth.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.core.models.User
import com.example.kosmos.shared.utils.ValidationUtils
import com.example.kosmos.shared.utils.ErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var usernameCheckJob: Job? = null

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        // Set initial state synchronously (may be null if async init hasn't completed)
        val isLoggedIn = authRepository.isUserLoggedIn()
        _uiState.value = _uiState.value.copy(
            isLoggedIn = isLoggedIn,
            isCheckingAuth = false, // Sync check done — splash can navigate
            currentUser = if (isLoggedIn) authRepository.getCurrentUser() else null
        )

        // Always observe AuthRepository's currentUser —
        // AuthRepository.init loads user asynchronously, so currentUser may arrive later
        viewModelScope.launch {
            authRepository.userFlow.collect { user ->
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = user != null,
                    currentUser = user
                )
            }
        }
    }

    fun login(email: String, password: String, rememberMe: Boolean = false) {
        if (!isValidInput(email, password)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = authRepository.signInWithEmailAndPassword(email, password, rememberMe)
                result.fold(
                    onSuccess = { user ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            currentUser = user
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Login failed"
                        )
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed"
                )
            }
        }
    }

    fun getSavedEmail(): String = authRepository.getSavedEmail()

    fun isRememberMeEnabled(): Boolean = authRepository.isRememberMeEnabled()

    fun isDemoMode(): Boolean = authRepository.isDemoMode()

    /**
     * Enter offline demo mode — seeds the Room DB with mock data and signs
     * in the demo user without any network access.
     */
    fun enterDemoMode() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = authRepository.enterDemoMode()
                result.fold(
                    onSuccess = { user ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            currentUser = user
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Demo mode failed to load"
                        )
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Demo mode failed to load"
                )
            }
        }
    }

    fun signUp(signUpData: SignUpData) {
        if (!isValidSignUpInput(signUpData)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = authRepository.createUserWithEmailAndPassword(
                    email = signUpData.email,
                    password = signUpData.password,
                    displayName = signUpData.displayName,
                    username = signUpData.username,
                    age = signUpData.age,
                    role = signUpData.role,
                    bio = signUpData.bio,
                    location = signUpData.location,
                    githubUrl = signUpData.githubUrl,
                    twitterUrl = signUpData.twitterUrl,
                    linkedinUrl = signUpData.linkedinUrl,
                    websiteUrl = signUpData.websiteUrl,
                    portfolioUrl = signUpData.portfolioUrl
                )
                result.fold(
                    onSuccess = { user ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            currentUser = user
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Sign up failed"
                        )
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Sign up failed"
                )
            }
        }
    }

    /**
     * Check if username is available
     * Uses debounce to avoid excessive database queries
     * Queries Supabase directly for accurate availability check
     */
    fun checkUsernameAvailability(username: String) {
        // Cancel previous check job
        usernameCheckJob?.cancel()

        if (username.length < 3) {
            _uiState.value = _uiState.value.copy(
                isCheckingUsername = false,
                isUsernameAvailable = null
            )
            return
        }

        usernameCheckJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingUsername = true)

            // Debounce - wait for user to stop typing
            delay(500)

            try {
                // Check Supabase directly for username existence
                val exists = userRepository.checkUsernameExists(username)
                _uiState.value = _uiState.value.copy(
                    isCheckingUsername = false,
                    isUsernameAvailable = !exists // Available if NOT exists
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isCheckingUsername = false,
                    isUsernameAvailable = null
                )
            }
        }
    }

    /**
     * Complete Google sign-in after the calling composable has obtained
     * a Google ID token via Android Credential Manager.
     *
     * The UI layer is responsible for calling CredentialManager and extracting
     * the token; this function handles the Supabase sign-in and state updates.
     */
    fun signInWithGoogleIdToken(idToken: String, rawNonce: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = authRepository.signInWithGoogleIdToken(idToken, rawNonce)
                result.fold(
                    onSuccess = { (user, isNewUser) ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            currentUser = user,
                            isNewGoogleUser = isNewUser
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Google sign-in failed"
                        )
                    }
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Google sign-in failed"
                )
            }
        }
    }

    fun clearNewGoogleUserFlag() {
        _uiState.value = _uiState.value.copy(isNewGoogleUser = false)
    }

    /**
     * Save profile fields collected from the post-Google-sign-in wizard.
     */
    fun saveGoogleUserProfile(
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
    ) {
        val currentUser = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val updatedUser = currentUser.copy(
                    displayName = displayName.ifBlank { currentUser.displayName },
                    username = username.ifBlank { currentUser.username },
                    age = age ?: currentUser.age,
                    role = role ?: currentUser.role,
                    bio = bio ?: currentUser.bio,
                    location = location ?: currentUser.location,
                    githubUrl = githubUrl ?: currentUser.githubUrl,
                    twitterUrl = twitterUrl ?: currentUser.twitterUrl,
                    linkedinUrl = linkedinUrl ?: currentUser.linkedinUrl,
                    websiteUrl = websiteUrl ?: currentUser.websiteUrl,
                    portfolioUrl = portfolioUrl ?: currentUser.portfolioUrl
                )
                val result = userRepository.updateUser(updatedUser)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            currentUser = updatedUser,
                            isNewGoogleUser = false
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to save profile"
                        )
                    }
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to save profile"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _uiState.value = AuthUiState()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    error = "Logout failed"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Send password reset email
     * @param email User's email address
     */
    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(
                passwordResetError = "Please enter your email address"
            )
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = _uiState.value.copy(
                passwordResetError = "Please enter a valid email address"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                passwordResetError = null,
                passwordResetSent = false
            )

            try {
                val result = authRepository.sendPasswordResetEmail(email)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            passwordResetSent = true,
                            passwordResetError = null
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            passwordResetSent = false,
                            passwordResetError = exception.message ?: "Failed to send reset email"
                        )
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    passwordResetSent = false,
                    passwordResetError = e.message ?: "Failed to send reset email"
                )
            }
        }
    }

    /**
     * Clear password reset state
     */
    fun clearPasswordResetState() {
        _uiState.value = _uiState.value.copy(
            passwordResetSent = false,
            passwordResetError = null
        )
    }

    private fun isValidInput(email: String, password: String): Boolean {
        // Validate email
        ValidationUtils.validateRequiredEmail(email)?.let { error ->
            _uiState.value = _uiState.value.copy(error = error)
            return false
        }

        // Validate password is not blank (detailed validation only on signup)
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Password cannot be empty")
            return false
        }

        return true
    }

    private fun isValidSignUpInput(signUpData: SignUpData): Boolean {
        // Validate display name
        ValidationUtils.validateDisplayName(signUpData.displayName)?.let { error ->
            _uiState.value = _uiState.value.copy(error = error)
            return false
        }

        // Validate username
        ValidationUtils.validateUsername(signUpData.username)?.let { error ->
            _uiState.value = _uiState.value.copy(error = error)
            return false
        }

        // Check username availability
        if (_uiState.value.isUsernameAvailable != true) {
            _uiState.value = _uiState.value.copy(error = "Username is not available")
            return false
        }

        // Validate password strength
        ValidationUtils.validatePassword(signUpData.password)?.let { error ->
            _uiState.value = _uiState.value.copy(error = error)
            return false
        }

        // Validate email
        return isValidInput(signUpData.email, signUpData.password)
    }

    /**
     * Update user profile information
     * @param photoUri Optional new profile photo URI (if user selected a new photo)
     */
    fun updateProfile(
        displayName: String,
        bio: String,
        age: Int? = null,
        role: String? = null,
        location: String? = null,
        githubUrl: String? = null,
        twitterUrl: String? = null,
        linkedinUrl: String? = null,
        websiteUrl: String? = null,
        portfolioUrl: String? = null,
        photoUri: Uri? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val currentUser = _uiState.value.currentUser
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not logged in"
                    )
                    return@launch
                }

                // Upload photo if provided
                var photoUrl: String? = currentUser.photoUrl
                if (photoUri != null) {
                    val uploadResult = authRepository.uploadProfilePhoto(currentUser.id, photoUri)
                    photoUrl = uploadResult.getOrNull() ?: currentUser.photoUrl
                    if (uploadResult.isFailure) {
                        _uiState.value = _uiState.value.copy(
                            error = "Profile saved but photo upload failed: ${uploadResult.exceptionOrNull()?.message}"
                        )
                    }
                }

                // Create updated user object
                val updatedUser = currentUser.copy(
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
                    photoUrl = photoUrl
                )

                // Update in repository
                val result = userRepository.updateUser(updatedUser)
                result.fold(
                    onSuccess = {
                        // Also update AuthRepository's currentUser so other ViewModels see it
                        authRepository.updateCurrentUser(updatedUser)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            currentUser = updatedUser,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to update profile"
                        )
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update profile"
                )
            }
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isCheckingAuth: Boolean = true, // True until initial session check completes
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null,
    val isCheckingUsername: Boolean = false,
    val isUsernameAvailable: Boolean? = null,
    val passwordResetSent: Boolean = false,
    val passwordResetError: String? = null,
    // True after a successful Google OAuth sign-in where the user had no prior profile
    val isNewGoogleUser: Boolean = false
)