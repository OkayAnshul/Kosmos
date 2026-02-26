package com.example.kosmos.core.database.converters

import androidx.room.TypeConverter
import com.example.kosmos.core.models.UserSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Room TypeConverters for UserSettings
 * Converts UserSettings to/from JSON string for Room database storage
 */
class UserSettingsConverters {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromUserSettings(value: UserSettings?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toUserSettings(value: String?): UserSettings? {
        return value?.let { json.decodeFromString(it) }
    }
}
