package com.example.kosmos.core.config

import kotlinx.serialization.Serializable

/**
 * Remote-configurable app strings fetched from the `app_config` Supabase table.
 * All fields have sensible hardcoded defaults so the app works even offline.
 */
data class AppConfig(
    val appName: String = "Kosmos",
    val tagline: String = "Where teams think, build, and ship.",
    val contactEmail: String = "contact.arvaya@gmail.com",
    val feedbackEmail: String = "feedback@kosmos.app",
    val supportUrl: String = "https://kosmos.app/help",
    val termsUrl: String = "https://kosmos.app/terms",
    val privacyUrl: String = "https://kosmos.app/privacy",
    val logoUrl: String = "",   // empty = show local drawable / text wordmark
    val appDescription: String = "A powerful project management app for teams — real-time messaging, Kanban tasks, and offline sync.",
    val builtWith: String = "Kotlin • Jetpack Compose • Material 3 • Room • Supabase • Hilt",
    val credits: String = "Developed with care by Aravya"
)

/** Single row from the `app_config` table. */
@Serializable
data class AppConfigEntry(
    val key: String,
    val value: String
)
