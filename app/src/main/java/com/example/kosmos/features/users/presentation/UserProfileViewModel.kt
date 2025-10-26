package com.example.kosmos.features.users.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.User
import com.example.kosmos.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for User Profile Screen
 * Handles loading and displaying user profile information
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileState())
    val uiState: StateFlow<UserProfileState> = _uiState.asStateFlow()

    /**
     * Load user profile by ID
     * Uses hybrid sync to get cached data first, then fresh from Supabase
     */
    fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = UserProfileState(isLoading = true)

            userRepository.getUserByIdWithSync(userId).collect { result ->
                when {
                    result.isSuccess -> {
                        val user = result.getOrNull()
                        if (user != null) {
                            _uiState.value = UserProfileState(
                                user = user,
                                isLoading = false,
                                error = null
                            )
                        } else {
                            _uiState.value = UserProfileState(
                                user = null,
                                isLoading = false,
                                error = "User not found"
                            )
                        }
                    }
                    result.isFailure -> {
                        val error = result.exceptionOrNull()
                        _uiState.value = UserProfileState(
                            user = null,
                            isLoading = false,
                            error = error?.message ?: "Failed to load user profile"
                        )
                    }
                }
            }
        }
    }
}

/**
 * UI State for User Profile Screen
 */
data class UserProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
