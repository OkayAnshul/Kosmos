package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "project_invites")
data class ProjectInvite(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @SerialName("project_id") val projectId: String,
    @SerialName("invitee_id") val inviteeId: String,
    @SerialName("inviter_id") val inviterId: String,
    val role: String = "MEMBER",
    val status: InviteStatus = InviteStatus.PENDING,
    val message: String? = null,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("expires_at") val expiresAt: Long = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L,
    @SerialName("responded_at") val respondedAt: Long? = null
)

@Serializable
enum class InviteStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED
}
