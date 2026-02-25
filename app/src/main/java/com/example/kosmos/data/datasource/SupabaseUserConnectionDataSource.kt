package com.example.kosmos.data.datasource
import kotlinx.coroutines.CancellationException

import android.util.Log
import com.example.kosmos.core.models.ConnectionStatus
import com.example.kosmos.core.models.UserConnection
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseUserConnectionDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseUserConnDS"
        private const val TABLE_NAME = "user_connections"
    }

    suspend fun createConnection(connection: UserConnection): Result<UserConnection> {
        return try {
            supabase.from(TABLE_NAME).insert(connection)
            Log.d(TAG, "Connection created: ${connection.id}")
            Result.success(connection)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error creating connection", e)
            Result.failure(e)
        }
    }

    suspend fun getForUser(userId: String): Result<List<UserConnection>> {
        return try {
            val sent = supabase.from(TABLE_NAME).select {
                filter { eq("requester_id", userId) }
                order("created_at", Order.DESCENDING)
            }.decodeList<UserConnection>()

            val received = supabase.from(TABLE_NAME).select {
                filter { eq("addressee_id", userId) }
                order("created_at", Order.DESCENDING)
            }.decodeList<UserConnection>()

            Result.success((sent + received).distinctBy { it.id })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching connections for user", e)
            Result.failure(e)
        }
    }

    suspend fun getAccepted(userId: String): Result<List<UserConnection>> {
        return try {
            val sent = supabase.from(TABLE_NAME).select {
                filter {
                    eq("requester_id", userId)
                    eq("status", ConnectionStatus.ACCEPTED.name)
                }
                order("responded_at", Order.DESCENDING)
            }.decodeList<UserConnection>()

            val received = supabase.from(TABLE_NAME).select {
                filter {
                    eq("addressee_id", userId)
                    eq("status", ConnectionStatus.ACCEPTED.name)
                }
                order("responded_at", Order.DESCENDING)
            }.decodeList<UserConnection>()

            Result.success((sent + received).distinctBy { it.id })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching accepted connections", e)
            Result.failure(e)
        }
    }

    suspend fun updateStatus(connectionId: String, status: ConnectionStatus, respondedAt: Long = System.currentTimeMillis()): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME).update({
                set("status", status.name)
                set("responded_at", respondedAt)
            }) {
                filter { eq("id", connectionId) }
            }
            Log.d(TAG, "Connection $connectionId updated to $status")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating connection status", e)
            Result.failure(e)
        }
    }

    suspend fun getBetween(userA: String, userB: String): Result<UserConnection?> {
        return try {
            val connections = supabase.from(TABLE_NAME).select {
                filter {
                    or {
                        and {
                            eq("requester_id", userA)
                            eq("addressee_id", userB)
                        }
                        and {
                            eq("requester_id", userB)
                            eq("addressee_id", userA)
                        }
                    }
                }
                limit(1)
            }.decodeList<UserConnection>()
            Result.success(connections.firstOrNull())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching connection between users", e)
            Result.failure(e)
        }
    }

    suspend fun removeConnection(connectionId: String): Result<Unit> {
        return try {
            supabase.from(TABLE_NAME).delete {
                filter { eq("id", connectionId) }
            }
            Log.d(TAG, "Connection $connectionId removed")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error removing connection", e)
            Result.failure(e)
        }
    }
}
