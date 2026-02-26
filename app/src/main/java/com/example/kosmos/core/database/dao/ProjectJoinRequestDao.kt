package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.ProjectJoinRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectJoinRequestDao {

    @Query("SELECT * FROM project_join_requests WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getByProjectFlow(projectId: String): Flow<List<ProjectJoinRequest>>

    @Query("SELECT * FROM project_join_requests WHERE requesterId = :userId ORDER BY createdAt DESC")
    fun getByRequesterFlow(userId: String): Flow<List<ProjectJoinRequest>>

    @Query("SELECT * FROM project_join_requests WHERE id = :requestId")
    suspend fun getById(requestId: String): ProjectJoinRequest?

    @Query("SELECT * FROM project_join_requests WHERE projectId = :projectId AND requesterId = :requesterId LIMIT 1")
    suspend fun getExisting(projectId: String, requesterId: String): ProjectJoinRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: ProjectJoinRequest)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<ProjectJoinRequest>)

    @Query("UPDATE project_join_requests SET status = :status, reviewedBy = :reviewedBy, respondedAt = :respondedAt WHERE id = :requestId")
    suspend fun updateStatus(requestId: String, status: String, reviewedBy: String? = null, respondedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM project_join_requests WHERE id = :requestId")
    suspend fun deleteById(requestId: String)
}
