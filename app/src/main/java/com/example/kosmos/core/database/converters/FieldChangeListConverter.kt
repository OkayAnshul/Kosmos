package com.example.kosmos.core.database.converters

import androidx.room.TypeConverter
import com.example.kosmos.core.models.FieldChange
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Room TypeConverter for List<FieldChange>
 *
 * Converts FieldChange list to/from JSON string for Room database storage.
 * Used by TaskActivity entity to store before/after field changes.
 */
class FieldChangeListConverter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromFieldChangeList(value: List<FieldChange>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toFieldChangeList(value: String?): List<FieldChange>? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return value?.let { json.decodeFromString(it) }
    }
}
