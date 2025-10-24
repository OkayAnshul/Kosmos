package com.example.kosmos.data.repository

import android.util.Log
import androidx.activity.ComponentActivity
import com.example.kosmos.core.config.SupabaseConfig
import com.example.kosmos.core.database.dao.UserDao
import com.example.kosmos.core.models.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling user authentication operations using Supabase Auth
 * Manages authentication state, user profiles, and session management
 */
@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao
) {
    private val auth: Auth = supabase.auth

    // Current authenticated user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()

    // Authentication state
    val isAuthenticated: Flow<Boolean> = currentUser.map { it != null }

    companion object {
        private const val TAG = "AuthRepository"
    }

    init {
        // Check for existing session on initialization
        checkExistingSession()
    }

    /**
     * Check if there's an existing valid session
     */
    private fun checkExistingSession() {
        try {
            val session = auth.currentSessionOrNull()
            if (session != null) {
                val userInfo = auth.currentUserOrNull()
                if (userInfo != null) {
                    // Load user from database
                    loadUserProfile(userInfo.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking existing session", e)
        }
    }

    /**
     * Sign in user with email and password
     * @param email User's email address
     * @param password User's password
     * @return Result containing User object or error
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<User> {
        return try {
            // Sign in with Supabase Auth
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            // Get user info
            val userInfo = auth.currentUserOrNull()
                ?: return Result.failure(Exception("Sign in successful but user info not available"))

            // Load or create user profile
            val user = getUserFromDatabase(userInfo.id) ?: createUserProfile(userInfo)

            // Update current user state
            _currentUser.value = user

            // Update online status
            updateUserOnlineStatus(user.id, true)

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new user account with email and password
     * @param email User's email address
     * @param password User's password
     * @param displayName User's display name
     * @return Result containing User object or error
     */
    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String
    ): Result<User> {
        return try {
            // Create account with Supabase Auth
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = mapOf("display_name" to displayName)
            }

            // Get user info
            val userInfo = auth.currentUserOrNull()
                ?: return Result.failure(Exception("Account created but user info not available"))

            // Create user profile
            val user = createUserProfile(userInfo, displayName)

            // Update current user state
            _currentUser.value = user

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google OAuth
     * @param activity ComponentActivity for OAuth flow
     * @return Result containing User object or error
     */
    suspend fun signInWithGoogle(activity: ComponentActivity): Result<User> {
        return try {
            // Initiate Google OAuth flow
            auth.signInWith(Google) {
                // OAuth configuration handled by SupabaseConfig
            }

            // Wait for OAuth callback (handled in MainActivity)
            // After callback, get user info
            val userInfo = auth.currentUserOrNull()
                ?: return Result.failure(Exception("Google sign in completed but user info not available"))

            // Load or create user profile
            val user = getUserFromDatabase(userInfo.id) ?: createUserProfile(userInfo)

            // Update current user state
            _currentUser.value = user

            // Update online status
            updateUserOnlineStatus(user.id, true)

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in error", e)
            Result.failure(e)
        }
    }

    /**
     * Handle OAuth callback after successful authentication
     * Called from MainActivity when OAuth redirect is received
     */
    suspend fun handleOAuthCallback(): Result<User> {
        return try {
            val userInfo = auth.currentUserOrNull()
                ?: return Result.failure(Exception("No authenticated user after OAuth callback"))

            // Load or create user profile
            val user = getUserFromDatabase(userInfo.id) ?: createUserProfile(userInfo)

            // Update current user state
            _currentUser.value = user

            // Update online status
            updateUserOnlineStatus(user.id, true)

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "OAuth callback handling error", e)
            Result.failure(e)
        }
    }

    /**
     * Sign out the current user
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            val userId = _currentUser.value?.id

            // Update online status before signing out
            if (userId != null) {
                updateUserOnlineStatus(userId, false)
            }

            // Sign out from Supabase
            auth.signOut()

            // Clear current user state
            _currentUser.value = null

            // Clear local cache
            userDao.deleteAll()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error", e)
            Result.failure(e)
        }
    }

    /**
     * Get current user synchronously
     */
    fun getCurrentUser(): User? = _currentUser.value

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean = _currentUser.value != null

    /**
     * Send password reset email
     * @param email User's email address
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset error", e)
            Result.failure(e)
        }
    }

    /**
     * Update user profile
     * @param user Updated user object
     */
    suspend fun updateUserProfile(user: User): Result<User> {
        return try {
            // Update in Supabase
            supabase.from("users")
                .update(user)
                .eq("id", user.id)
                .execute()

            // Update in local database
            userDao.insert(user)

            // Update current user state
            _currentUser.value = user

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Profile update error", e)
            Result.failure(e)
        }
    }

    /**
     * Update user's online status
     * @param userId User ID
     * @param isOnline Online status
     */
    private suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            val updates = mapOf(
                "is_online" to isOnline,
                "last_seen" to System.currentTimeMillis()
            )

            supabase.from("users")
                .update(updates)
                .eq("id", userId)
                .execute()

            // Also update local cache
            val currentUser = _currentUser.value
            if (currentUser != null && currentUser.id == userId) {
                val updatedUser = currentUser.copy(
                    isOnline = isOnline,
                    lastSeen = System.currentTimeMillis()
                )
                userDao.insert(updatedUser)
                _currentUser.value = updatedUser
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Online status update error", e)
            Result.failure(e)
        }
    }

    /**
     * Load user profile from database and update state
     */
    private suspend fun loadUserProfile(userId: String) {
        try {
            val user = getUserFromDatabase(userId)
            if (user != null) {
                _currentUser.value = user
                // Update local cache
                userDao.insert(user)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user profile", e)
        }
    }

    /**
     * Get user profile from Supabase database
     * @param userId User ID
     * @return User object or null if not found
     */
    private suspend fun getUserFromDatabase(userId: String): User? {
        return try {
            val response = supabase.from("users")
                .select() {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<User>()

            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user from database", e)
            null
        }
    }

    /**
     * Create user profile in Supabase database
     * @param userInfo Supabase UserInfo from auth
     * @param customDisplayName Optional custom display name
     * @return Created User object
     */
    private suspend fun createUserProfile(
        userInfo: UserInfo,
        customDisplayName: String? = null
    ): User {
        val user = User(
            id = userInfo.id,
            email = userInfo.email ?: "",
            displayName = customDisplayName
                ?: userInfo.userMetadata?.get("display_name") as? String
                ?: userInfo.userMetadata?.get("full_name") as? String
                ?: "User",
            photoUrl = userInfo.userMetadata?.get("avatar_url") as? String
                ?: userInfo.userMetadata?.get("picture") as? String,
            isOnline = true,
            lastSeen = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )

        // Save to Supabase
        try {
            supabase.from("users")
                .insert(user)
                .execute()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user profile", e)
            // Continue anyway, user might already exist
        }

        // Save to local cache
        userDao.insert(user)

        return user
    }

    /**
     * Refresh current session
     * Useful for checking if session is still valid
     */
    suspend fun refreshSession(): Result<Unit> {
        return try {
            auth.refreshCurrentSession()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Session refresh error", e)
            // Session expired, sign out
            signOut()
            Result.failure(e)
        }
    }
}
