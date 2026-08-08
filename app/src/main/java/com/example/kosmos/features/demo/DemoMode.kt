package com.example.kosmos.features.demo

import android.content.SharedPreferences
import com.example.kosmos.BuildConfig
import com.example.kosmos.core.models.User
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether the app is running in offline "Demo Mode".
 *
 * Demo mode seeds the local Room database with realistic mock data and skips all
 * network / Supabase interactions so the app is fully explorable without an account.
 */
@Singleton
class DemoMode @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {

    companion object {
        /** SharedPreferences key backing the demo-mode flag. */
        const val PREFS_KEY = "demo_mode_enabled"

        /** Fixed, valid-UUID id for the demo user so seeded membership queries resolve. */
        val DEMO_USER_ID: String = "11111111-1111-4111-8111-111111111111"

        val DEMO_USER: User = User(
            id = DEMO_USER_ID,
            email = "aravya@kosmos.demo",
            username = "aravya",
            displayName = "Aravya Sharma",
            role = "Android Engineer",
            bio = "Building Kosmos — an offline-first team workspace.",
            photoUrl = null,
            createdAt = 1_700_000_000_000L
        )
    }

    private val key = PREFS_KEY

    val isEnabled: Boolean
        get() = BuildConfig.DEMO_MODE_ENABLED && sharedPreferences.getBoolean(key, false)

    fun enable() {
        sharedPreferences.edit().putBoolean(key, true).apply()
    }

    fun disable() {
        sharedPreferences.edit().putBoolean(key, false).apply()
    }
}
