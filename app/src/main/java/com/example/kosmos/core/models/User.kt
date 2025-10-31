package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
data class User(
    // Account Info (Required)
    @PrimaryKey
    val id: String = "",
    val email: String = "",
    val username: String = "", // Unique @username for easy discovery

    @SerialName("display_name")
    val displayName: String = "", // Full name

    // Profile Details (Optional but recommended)
    val age: Int? = null, // User's age
    val role: String? = null, // Job title/role (e.g., "Full Stack Developer")
    val bio: String? = null, // Short description (max 200 chars)
    val location: String? = null, // City, Country

    // Social & Professional Links (All Optional)
    @SerialName("github_url")
    val githubUrl: String? = null,

    @SerialName("twitter_url")
    val twitterUrl: String? = null,

    @SerialName("linkedin_url")
    val linkedinUrl: String? = null,

    @SerialName("website_url")
    val websiteUrl: String? = null,

    @SerialName("portfolio_url")
    val portfolioUrl: String? = null,

    // System Fields
    @SerialName("photo_url")
    val photoUrl: String? = null,

    @SerialName("is_online")
    val isOnline: Boolean = false,

    @SerialName("last_seen")
    val lastSeen: Long = System.currentTimeMillis(),

    @SerialName("fcm_token")
    val fcmToken: String? = null,

    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis()
)