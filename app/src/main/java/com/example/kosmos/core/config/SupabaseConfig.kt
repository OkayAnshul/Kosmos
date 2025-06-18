package com.example.kosmos.core.config

import com.example.kosmos.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json

/**
 * Supabase configuration and client initialization
 *
 * Provides a singleton Supabase client with all necessary modules:
 * - Auth (GoTrue) for authentication
 * - Postgrest for database operations
 * - Storage for file uploads
 * - Realtime for live subscriptions
 *
 * Note: HTTP timeout is set to default 10 seconds. For slow networks,
 * the AuthRepository provides user-friendly timeout error messages.
 */
object SupabaseConfig {

    /**
     * Lazy-initialized Supabase client
     * Thread-safe singleton pattern
     */
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            // Use OkHttp engine for WebSocket support (required for Realtime)
            httpEngine = OkHttp.create()

            install(Auth) {
                // OAuth configuration
                scheme = "kosmos" // Deep link scheme for OAuth callback
                host = "auth-callback" // Deep link host for OAuth callback

                // Session persistence - keeps users logged in
                alwaysAutoRefresh = true // Auto-refresh expired tokens
                autoLoadFromStorage = true // Auto-restore session on app start
                autoSaveToStorage = true // Auto-save session after login
            }

            install(Postgrest) {
                // Custom JSON serialization with null handling
                serializer = KotlinXSerializer(Json {
                    ignoreUnknownKeys = true        // Ignore fields not in model
                    coerceInputValues = true        // Convert NULL to default values
                    encodeDefaults = true           // Include default values in serialization
                })
            }

            install(Storage) {
                // File upload/download with progress tracking
            }

            install(Realtime) {
                // WebSocket connection for live updates
                // Automatically reconnects on network changes
            }
        }
    }

    /**
     * Quick access to Auth module
     */
    val auth: Auth
        get() = client.auth

    /**
     * Quick access to Postgrest module
     */
    val database: Postgrest
        get() = client.postgrest

    /**
     * Quick access to Storage module
     */
    val storage: Storage
        get() = client.storage

    /**
     * Quick access to Realtime module
     */
    val realtime: Realtime
        get() = client.realtime

    /**
     * Check if Supabase is properly configured
     */
    fun isConfigured(): Boolean {
        return BuildConfig.SUPABASE_URL.isNotEmpty() &&
               BuildConfig.SUPABASE_ANON_KEY.isNotEmpty()
    }

    /**
     * Get the configured redirect URL for OAuth
     */
    fun getOAuthRedirectUrl(): String {
        return "kosmos://auth-callback"
    }
}
