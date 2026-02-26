package com.example.kosmos.features.announcements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Announcement(
    val id: String,
    val title: String,
    val body: String,
    val type: String = "info",   // "info" | "warning" | "feature"
    @SerialName("cta_label") val ctaLabel: String? = null,
    @SerialName("cta_url")   val ctaUrl: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class AnnouncementSeen(
    @SerialName("announcement_id") val announcementId: String,
    @SerialName("user_id") val userId: String
)
