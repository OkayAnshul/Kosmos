package com.example.kosmos.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.kosmos.core.config.SupabaseConfig
import com.example.kosmos.core.database.dao.UserDao
import com.example.kosmos.core.models.User
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Repository for handling user authentication operations using Supabase Auth
 * Manages authentication state, user profiles, and session management
 */
@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao,
    private val sharedPreferences: android.content.SharedPreferences,
    @ApplicationContext private val context: Context
) {
    private val auth = supabase.auth

    // Current authenticated user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val userFlow: Flow<User?> = _currentUser.asStateFlow()

    // Authentication state
    val isAuthenticated: Flow<Boolean> = userFlow.map { it != null }

    companion object {
        private const val TAG = "AuthRepository"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_EMAIL = "saved_email"
    }

    init {
        // Check for existing session on initialization
        val currentUserInfo = auth.currentUserOrNull()
        if (currentUserInfo != null) {
            // Session exists, load user profile in background
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                val userId = currentUserInfo.id

                // 1. Always try Room first — it has the full profile from sign-up/previous loads
                val roomUser = try { userDao.getUserById(userId) } catch (e: Exception) { null }
                if (roomUser != null) {
                    // Serve the Room-cached profile immediately (fast, offline-safe)
                    _currentUser.value = roomUser
                    Log.d(TAG, "✅ Session restored from local cache for: ${roomUser.email}")
                }

                // 2. Then try Supabase to get the latest data
                try {
                    val remoteUser = getUserFromDatabase(userId)
                    if (remoteUser != null) {
                        // Remote user has full profile — update cache and state
                        userDao.insertUser(remoteUser)
                        _currentUser.value = remoteUser
                        updateUserOnlineStatus(remoteUser.id, true)
                        Log.d(TAG, "✅ Session refreshed from Supabase for: ${remoteUser.email}")
                    } else if (roomUser == null) {
                        // Neither Room nor Supabase has a profile — create a minimal placeholder
                        // so the app isn't stuck. User can complete their profile in Settings.
                        Log.w(TAG, "⚠️ No profile found in Room or Supabase, creating placeholder")
                        val placeholder = User(
                            id = userId,
                            email = currentUserInfo.email ?: "",
                            displayName = currentUserInfo.userMetadata?.get("full_name") as? String
                                ?: currentUserInfo.email?.substringBefore("@") ?: "User",
                            username = currentUserInfo.email?.substringBefore("@")
                                ?.lowercase()?.replace(Regex("[^a-z0-9_]"), "_")
                                ?: "user_${userId.take(8)}",
                            createdAt = System.currentTimeMillis()
                        )
                        userDao.insertUser(placeholder)
                        _currentUser.value = placeholder
                    }
                    // If remoteUser == null but roomUser != null: keep the Room version — don't downgrade
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "⚠️ Supabase fetch failed during init, using cached profile: ${e.message}")
                    // Room data is already served above — no further action needed
                }
            }
        }
    }

    /**
     * Save "Remember Me" preference and email
     */
    fun saveRememberMe(rememberMe: Boolean, email: String = "") {
        sharedPreferences.edit().apply {
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            if (rememberMe) {
                putString(KEY_SAVED_EMAIL, email)
            } else {
                remove(KEY_SAVED_EMAIL)
            }
            apply()
        }
    }

    /**
     * Check if "Remember Me" is enabled
     */
    fun isRememberMeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
    }

    /**
     * Get saved email from preferences
     */
    fun getSavedEmail(): String {
        return sharedPreferences.getString(KEY_SAVED_EMAIL, "") ?: ""
    }

    /**
     * Sign in user with email and password
     * @param email User's email address
     * @param password User's password
     * @param rememberMe Whether to remember the user's login
     * @return Result containing User object or error
     */
    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
        rememberMe: Boolean = false
    ): Result<User> {
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

            // Save "Remember Me" preference
            saveRememberMe(rememberMe, email)

            // Update online status
            updateUserOnlineStatus(user.id, true)

            Result.success(user)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Provide user-friendly error messages based on exception type
            val errorMessage = when {
                e is HttpRequestTimeoutException ->
                    "Connection timeout. Please check your internet connection and try again."

                e is AuthRestException && e.message?.contains("Invalid login credentials") == true ->
                    "Invalid email or password. Please try again."

                e is AuthRestException && e.message?.contains("Email not confirmed") == true ->
                    "Please verify your email address before signing in."

                e.message?.contains("Invalid email") == true ->
                    "Please enter a valid email address."

                e.message?.contains("network") == true || e.message?.contains("connection") == true ->
                    "Network error. Please check your internet connection."

                else -> "Sign in failed: ${e.message ?: "Unknown error. Please try again."}"
            }

            Log.e(TAG, "Sign in error", e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Create a new user account with email and password
     * @param email User's email address
     * @param password User's password
     * @param displayName User's display name
     * @param username Unique username
     * @param age User's age (optional)
     * @param role User's role/title (optional)
     * @param bio User's bio (optional)
     * @param location User's location (optional)
     * @param githubUrl GitHub URL (optional)
     * @param twitterUrl Twitter URL (optional)
     * @param linkedinUrl LinkedIn URL (optional)
     * @param websiteUrl Website URL (optional)
     * @param portfolioUrl Portfolio URL (optional)
     * @return Result containing User object or error
     */
    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String,
        username: String,
        age: Int? = null,
        role: String? = null,
        bio: String? = null,
        location: String? = null,
        githubUrl: String? = null,
        twitterUrl: String? = null,
        linkedinUrl: String? = null,
        websiteUrl: String? = null,
        portfolioUrl: String? = null
    ): Result<User> {
        return try {
            // Create account with Supabase Auth
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                // Note: User metadata can be set via Supabase dashboard or Edge Functions
                // For now, we'll set displayName after user creation
            }

            // Get user info
            val userInfo = auth.currentUserOrNull()
                ?: return Result.failure(Exception("Account created but user info not available"))

            // Create user profile with all fields
            val user = createUserProfile(
                userInfo = userInfo,
                customDisplayName = displayName,
                username = username,
                age = age,
                role = role,
                bio = bio,
                location = location,
                githubUrl = githubUrl,
                twitterUrl = twitterUrl,
                linkedinUrl = linkedinUrl,
                websiteUrl = websiteUrl,
                portfolioUrl = portfolioUrl
            )

            // Update current user state
            _currentUser.value = user

            Result.success(user)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Provide user-friendly error messages based on exception type
            val errorMessage = when {
                e is HttpRequestTimeoutException ->
                    "Connection timeout. Please check your internet connection and try again."

                e is AuthRestException && e.message?.contains("over_email_send_rate_limit") == true ->
                    "Too many sign-up attempts. Please wait 60 seconds before trying again."

                e is AuthRestException && e.message?.contains("User already registered") == true ->
                    "This email is already registered. Please try logging in instead."

                e.message?.contains("Invalid email") == true ->
                    "Please enter a valid email address."

                e.message?.contains("Password") == true ->
                    "Password must be at least 6 characters long."

                e.message?.contains("email") == true && e.message?.contains("invalid") == true ->
                    "Please enter a valid email address."

                else -> "Sign up failed: ${e.message ?: "Unknown error. Please try again."}"
            }

            Log.e(TAG, "Sign up failed", e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Sign in with Google OAuth
     * @param activity ComponentActivity for OAuth flow
     * @return Result containing User object or error
     */
    suspend fun signInWithGoogle(activity: ComponentActivity): Result<User> {
        return signInWithGoogleOAuth().map { it.first }
    }

    /**
     * Sign in with Google OAuth, also returns whether the user is brand new.
     * @return Pair<User, Boolean> where Boolean is true if the user was just created.
     */
    suspend fun signInWithGoogleOAuth(): Result<Pair<User, Boolean>> {
        return try {
            // Initiate Google OAuth flow — opens browser/Custom Tab
            auth.signInWith(Google) {
                // OAuth configuration handled by SupabaseConfig
            }

            val userInfo = auth.currentUserOrNull()
                ?: return Result.failure(Exception("Google sign in completed but user info not available"))

            // Detect if this is a new user by checking the database
            val existingUser = getUserFromDatabase(userInfo.id)
            val isNewUser = existingUser == null
            val user = existingUser ?: createUserProfile(userInfo)

            _currentUser.value = user
            updateUserOnlineStatus(user.id, true)

            Result.success(Pair(user, isNewUser))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Google sign in error", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with a Google ID token obtained via Android Credential Manager.
     * This is the native Android path — no browser redirect needed.
     *
     * @param googleIdToken  ID token from GoogleIdTokenCredential
     * @param rawNonce       Raw nonce used when building GetGoogleIdOption (if any)
     * @return Pair<User, Boolean> where Boolean = true if the user is brand new.
     */
    suspend fun signInWithGoogleIdToken(
        googleIdToken: String,
        rawNonce: String? = null
    ): Result<Pair<User, Boolean>> {
        return try {
            auth.signInWith(IDToken) {
                idToken = googleIdToken
                provider = Google
                rawNonce?.let { nonce = it }
            }

            val userInfo = auth.currentUserOrNull()
                ?: return Result.failure(Exception("Google sign-in succeeded but no user info available"))

            val existingUser = getUserFromDatabase(userInfo.id)
            val isNewUser = existingUser == null
            val user = existingUser ?: createUserProfile(userInfo)

            _currentUser.value = user
            updateUserOnlineStatus(user.id, true)

            Result.success(Pair(user, isNewUser))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Google ID token sign-in error", e)
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
            if (e is CancellationException) throw e
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

            // Clear "Remember Me" preference (keep email if was checked)
            // Note: We keep the email but clear the session
            // If you want to clear everything, use: saveRememberMe(false)

            // Clear local cache (if needed, implement deleteAll in DAO or clear specific user)

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Sign out error", e)
            Result.failure(e)
        }
    }

    /**
     * Get current user synchronously
     */
    fun getCurrentUser(): User? = _currentUser.value

    /**
     * Update the cached current user (e.g., after profile edit via UserRepository)
     */
    fun updateCurrentUser(user: User) {
        _currentUser.value = user
    }

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
            if (e is CancellationException) throw e
            Log.e(TAG, "Password reset error", e)
            Result.failure(e)
        }
    }

    /**
     * Change password for the currently authenticated user
     * @param newPassword New password (must meet Supabase password requirements)
     * @return Result indicating success or failure
     */
    suspend fun changePassword(newPassword: String): Result<Unit> {
        return try {
            auth.updateUser {
                password = newPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Password change error", e)
            Result.failure(e)
        }
    }

    /**
     * Change email for the currently authenticated user
     * Supabase sends a confirmation email to the new address
     * @param newEmail New email address
     * @return Result indicating success or failure
     */
    suspend fun changeEmail(newEmail: String): Result<Unit> {
        return try {
            auth.updateUser {
                email = newEmail
            }
            // Note: Supabase sends a confirmation link to the new email
            // The email is not changed until the user confirms
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Email change error", e)
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
            supabase.from("users")
                .update({
                    set("is_online", isOnline)
                    set("last_seen", System.currentTimeMillis())
                }) {
                    filter {
                        eq("id", userId)
                    }
                }

            // Also update local cache
            val currentUser = _currentUser.value
            if (currentUser != null && currentUser.id == userId) {
                val updatedUser = currentUser.copy(
                    isOnline = isOnline,
                    lastSeen = System.currentTimeMillis()
                )
                userDao.insertUser(updatedUser)
                _currentUser.value = updatedUser
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Online status update error", e)
            Result.failure(e)
        }
    }

    /**
     * Load user profile from database and update state
     */
    suspend fun loadUserProfile(userId: String) {
        try {
            val user = getUserFromDatabase(userId)
            if (user != null) {
                _currentUser.value = user
                // Update local cache
                userDao.insertUser(user)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
                .decodeList<User>()
                .firstOrNull()

            response
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error fetching user from database", e)
            null
        }
    }

    /**
     * Create user profile in Supabase database
     * @param userInfo Supabase UserInfo from auth
     * @param customDisplayName Optional custom display name
     * @param username Unique username
     * @param age User's age (optional)
     * @param role User's role/title (optional)
     * @param bio User's bio (optional)
     * @param location User's location (optional)
     * @param githubUrl GitHub URL (optional)
     * @param twitterUrl Twitter URL (optional)
     * @param linkedinUrl LinkedIn URL (optional)
     * @param websiteUrl Website URL (optional)
     * @param portfolioUrl Portfolio URL (optional)
     * @return Created User object
     */
    private suspend fun createUserProfile(
        userInfo: UserInfo,
        customDisplayName: String? = null,
        username: String = "",
        age: Int? = null,
        role: String? = null,
        bio: String? = null,
        location: String? = null,
        githubUrl: String? = null,
        twitterUrl: String? = null,
        linkedinUrl: String? = null,
        websiteUrl: String? = null,
        portfolioUrl: String? = null
    ): User {
        val displayNameValue = customDisplayName
            ?: userInfo.userMetadata?.get("display_name") as? String
            ?: userInfo.userMetadata?.get("full_name") as? String
            ?: userInfo.email?.substringBefore("@")
            ?: "User"

        val user = User(
            id = userInfo.id,
            email = userInfo.email ?: "",
            username = username,
            displayName = displayNameValue,
            age = age,
            role = role,
            bio = bio,
            location = location,
            githubUrl = githubUrl,
            twitterUrl = twitterUrl,
            linkedinUrl = linkedinUrl,
            websiteUrl = websiteUrl,
            portfolioUrl = portfolioUrl,
            photoUrl = userInfo.userMetadata?.get("avatar_url") as? String
                ?: userInfo.userMetadata?.get("picture") as? String,
            isOnline = true,
            lastSeen = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )

        // Save to Supabase (critical for user visibility)
        try {
            Log.d(TAG, "Creating user profile in Supabase for: ${user.email}")
            supabase.from("users")
                .insert(user)
            Log.d(TAG, "User profile created successfully in Supabase")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "❌ CRITICAL: Failed to create user in Supabase — user will be invisible to search", e)
            // Bug F fix: Re-throw so signup fails loudly. User must be in Supabase DB.
            // Queueing is wrong here — if user profile is missing, the app is broken.
            throw e
        }

        // Save to local cache (always succeeds)
        userDao.insertUser(user)
        Log.d(TAG, "User profile saved to local cache")

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
            if (e is CancellationException) throw e
            Log.e(TAG, "Session refresh error", e)
            // Session expired, sign out
            signOut()
            Result.failure(e)
        }
    }

    /**
     * Upload a profile photo to Supabase Storage (avatars bucket) and return the public URL.
     * Bucket must exist in Supabase dashboard with public access enabled.
     * Path: public/{userId}.jpg — deterministic, idempotent re-uploads via upsert.
     *
     * [FUTURE F3] When the Attachment Module is implemented, consider extracting a shared
     * SupabaseStorageDataSource that handles all bucket uploads (avatars, attachments, voice-messages).
     */
    suspend fun uploadProfilePhoto(userId: String, photoUri: Uri): Result<String> {
        return try {
            val bytes = context.contentResolver.openInputStream(photoUri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("Cannot open photo URI"))
            val path = "public/$userId.jpg"
            supabase.storage.from("avatars").upload(path, bytes) {
                upsert = true
            }
            val publicUrl = supabase.storage.from("avatars").publicUrl(path)
            Result.success(publicUrl)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Photo upload failed", e)
            Result.failure(e)
        }
    }
}
