package com.example.kosmos.core.database

import androidx.room.TypeConverter
import com.example.kosmos.core.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromStringLongMap(value: Map<String, Long>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringLongMap(value: String): Map<String, Long> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromFloatList(value: List<Float>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toFloatList(value: String): List<Float> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromTaskCommentList(value: List<TaskComment>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toTaskCommentList(value: String): List<TaskComment> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType =
        try { MessageType.valueOf(value) } catch (e: Exception) { MessageType.TEXT }

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus =
        try { TaskStatus.valueOf(value) } catch (e: Exception) { TaskStatus.TODO }

    @TypeConverter
    fun fromTaskPriority(value: TaskPriority): String = value.name

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority =
        try { TaskPriority.valueOf(value) } catch (e: Exception) { TaskPriority.MEDIUM }

    @TypeConverter
    fun fromActionType(value: ActionType): String = value.name

    @TypeConverter
    fun toActionType(value: String): ActionType =
        try { ActionType.valueOf(value) } catch (e: Exception) { ActionType.TASK }
}