package com.example.kosmos.features.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.core.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

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

    fun signUp(email: String, password: String, displayName: String) {
        if (!isValidSignUpInput(email, password, displayName)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = authRepository.createUserWithEmailAndPassword(email, password, displayName)
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

    private fun isValidSignUpInput(email: String, password: String, displayName: String): Boolean {
        return when {
            displayName.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Display name cannot be empty")
                false
            }
            password.length < 6 -> {
                _uiState.value = _uiState.value.copy(error = "Password must be at least 6 characters")
                false
            }
            else -> isValidInput(email, password)
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null
)