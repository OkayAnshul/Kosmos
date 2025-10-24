package com.example.kosmos.core.config

import com.example.kosmos.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Supabase configuration and client initialization
 *
 * Provides a singleton Supabase client with all necessary modules:
 * - Auth (GoTrue) for authentication
 * - Postgrest for database operations
 * - Storage for file uploads
 * - Realtime for live subscriptions
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
            install(Auth) {
                // OAuth configuration
                scheme = "kosmos" // Deep link scheme for OAuth callback
                host = "auth-callback" // Deep link host for OAuth callback
            }

            install(Postgrest) {
                // Automatically converts responses to/from Kotlin objects
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
