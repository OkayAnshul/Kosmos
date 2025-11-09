package com.example.kosmos.features.users.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.core.models.User
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for User Search Screen
 * Handles user search with debouncing and state management
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow(UserSearchState())
    val uiState: StateFlow<UserSearchState> = _uiState.asStateFlow()

    init {
        // Setup debounced search
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // 300ms debounce to prevent excessive API calls
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isNotBlank()) {
                        searchUsers(query)
                    } else {
                        _uiState.value = UserSearchState()
                    }
                }
        }
    }

    /**
     * Update search query
     * Will trigger debounced search automatically
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Clear search query and results
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.value = UserSearchState()
    }

    /**
     * Search for users by display name or email
     * Fetches FRESH data from Supabase (no stale cache)
     * Excludes current user from results
     */
    private fun searchUsers(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Get current user ID to exclude from results
            val currentUserId = authRepository.getCurrentUser()?.id ?: ""

            // Use Supabase-only search for fresh, real-time data
            val result = userRepository.searchUsersFromSupabase(
                query = query,
                excludeIds = if (currentUserId.isNotEmpty()) listOf(currentUserId) else emptyList(),
                limit = 50
            )

            when {
                result.isSuccess -> {
                    val users = result.getOrNull() ?: emptyList()
                    _uiState.value = UserSearchState(
                        users = users,
                        isLoading = false,
                        error = null
                    )
                }
                result.isFailure -> {
                    val error = result.exceptionOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error?.message ?: "Search failed"
                    )
                }
            }
        }
    }

    /**
     * Retry search with current query
     */
    fun retrySearch() {
        val currentQuery = _searchQuery.value
        if (currentQuery.isNotBlank()) {
            searchUsers(currentQuery)
        }
    }
}

/**
 * UI State for User Search Screen
 */
data class UserSearchState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
