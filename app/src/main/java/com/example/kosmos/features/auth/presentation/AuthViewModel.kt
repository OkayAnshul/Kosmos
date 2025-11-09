package com.example.kosmos.features.auth.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.core.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        _uiState.value = _uiState.value.copy(
            isLoggedIn = authRepository.isUserLoggedIn()
        )
    }

    fun login(email: String, password: String) {
        if (!isValidInput(email, password)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = authRepository.signInWithEmailAndPassword(email, password)
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed"
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
                _uiState.value = _uiState.value.copy(
                    isCheckingUsername = false,
                    isUsernameAvailable = null
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
                _uiState.value = _uiState.value.copy(
                    error = "Logout failed"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun isValidInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Email cannot be empty")
                false
            }
            password.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Password cannot be empty")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.value = _uiState.value.copy(error = "Please enter a valid email")
                false
            }
            else -> true
        }
    }

    private fun isValidSignUpInput(signUpData: SignUpData): Boolean {
        return when {
            signUpData.displayName.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Display name cannot be empty")
                false
            }
            signUpData.username.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Username cannot be empty")
                false
            }
            signUpData.username.length < 3 -> {
                _uiState.value = _uiState.value.copy(error = "Username must be at least 3 characters")
                false
            }
            !signUpData.username.matches(Regex("^[a-zA-Z0-9_]+$")) -> {
                _uiState.value = _uiState.value.copy(error = "Username can only contain letters, numbers, and underscores")
                false
            }
            _uiState.value.isUsernameAvailable != true -> {
                _uiState.value = _uiState.value.copy(error = "Username is not available")
                false
            }
            signUpData.password.length < 6 -> {
                _uiState.value = _uiState.value.copy(error = "Password must be at least 6 characters")
                false
            }
            else -> isValidInput(signUpData.email, signUpData.password)
        }
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
                    // TODO: Implement photo upload to Supabase Storage
                    // For now, use the local URI (this won't work across devices)
                    // In a real implementation:
                    // val uploadResult = userRepository.uploadProfilePhoto(currentUser.id, photoUri)
                    // photoUrl = uploadResult.getOrNull()
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
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null,
    val isCheckingUsername: Boolean = false,
    val isUsernameAvailable: Boolean? = null
)