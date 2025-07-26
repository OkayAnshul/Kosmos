package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "user_connections")
data class UserConnection(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @SerialName("requester_id") val requesterId: String,
    @SerialName("addressee_id") val addresseeId: String,
    val status: ConnectionStatus = ConnectionStatus.PENDING,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("responded_at") val respondedAt: Long? = null
)

@Serializable
enum class ConnectionStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    BLOCKED
}
