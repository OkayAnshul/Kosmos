package com.example.kosmos.core.models

import kotlinx.serialization.Serializable

/**
 * User Settings Model
 * Stores user preferences for privacy and notifications
 * Persisted in Supabase as JSONB column in users table
 */
@Serializable
data class UserSettings(
    val privacy: PrivacySettings = PrivacySettings(),
    val notifications: NotificationSettings = NotificationSettings()
)

/**
 * Privacy Settings
 */
@Serializable
data class PrivacySettings(
    val profileVisibility: String = "PUBLIC", // PUBLIC, FRIENDS_ONLY, PRIVATE
    val showEmail: Boolean = false,
    val showLastSeen: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val allowDirectMessages: Boolean = true,
    val allowMentions: Boolean = true
)

/**
 * Notification Settings
 */
@Serializable
data class NotificationSettings(
    val enabled: Boolean = true,
    val messages: Boolean = true,
    val tasks: Boolean = true,
    val projectUpdates: Boolean = true,
    val mentions: Boolean = true,
    val mentionsOnlyMode: Boolean = false,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val dnd: DoNotDisturbSettings = DoNotDisturbSettings()
)

/**
 * Do Not Disturb Settings
 */
@Serializable
data class DoNotDisturbSettings(
    val enabled: Boolean = false,
    val startHour: Int = 22,
    val startMinute: Int = 0,
    val endHour: Int = 8,
    val endMinute: Int = 0
)
