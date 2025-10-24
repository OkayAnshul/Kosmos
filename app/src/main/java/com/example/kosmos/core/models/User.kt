package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)