package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.UserConnection
import kotlinx.coroutines.flow.Flow

@Dao
interface UserConnectionDao {

    @Query("SELECT * FROM user_connections WHERE (requesterId = :userId OR addresseeId = :userId) AND status = 'ACCEPTED' ORDER BY respondedAt DESC")
    fun getAcceptedFlow(userId: String): Flow<List<UserConnection>>

    @Query("SELECT * FROM user_connections WHERE addresseeId = :userId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingRequestsFlow(userId: String): Flow<List<UserConnection>>

    @Query("SELECT * FROM user_connections WHERE (requesterId = :userId OR addresseeId = :userId) ORDER BY createdAt DESC")
    fun getForUserFlow(userId: String): Flow<List<UserConnection>>

    @Query("SELECT * FROM user_connections WHERE id = :connectionId")
    suspend fun getById(connectionId: String): UserConnection?

    @Query("""
        SELECT * FROM user_connections
        WHERE (requesterId = :userA AND addresseeId = :userB)
           OR (requesterId = :userB AND addresseeId = :userA)
        LIMIT 1
    """)
    suspend fun getBetween(userA: String, userB: String): UserConnection?

    @Query("""
        SELECT CASE WHEN requesterId = :userId THEN addresseeId ELSE requesterId END
        FROM user_connections
        WHERE (requesterId = :userId OR addresseeId = :userId) AND status = 'ACCEPTED'
    """)
    suspend fun getAcceptedIds(userId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: UserConnection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(connections: List<UserConnection>)

    @Query("UPDATE user_connections SET status = :status, respondedAt = :respondedAt WHERE id = :connectionId")
    suspend fun updateStatus(connectionId: String, status: String, respondedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_connections WHERE id = :connectionId")
    suspend fun deleteById(connectionId: String)
}
