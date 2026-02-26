package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "project_join_requests")
data class ProjectJoinRequest(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @SerialName("project_id") val projectId: String,
    @SerialName("requester_id") val requesterId: String,
    val message: String? = null,
    val status: JoinRequestStatus = JoinRequestStatus.PENDING,
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("responded_at") val respondedAt: Long? = null
)

@Serializable
enum class JoinRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
