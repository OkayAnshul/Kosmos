package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.ProjectInvite
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectInviteDao {

    @Query("SELECT * FROM project_invites WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getByProjectFlow(projectId: String): Flow<List<ProjectInvite>>

    @Query("SELECT * FROM project_invites WHERE inviteeId = :userId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingForUserFlow(userId: String): Flow<List<ProjectInvite>>

    @Query("SELECT * FROM project_invites WHERE id = :inviteId")
    suspend fun getById(inviteId: String): ProjectInvite?

    @Query("SELECT * FROM project_invites WHERE projectId = :projectId AND inviteeId = :inviteeId AND status = 'PENDING' LIMIT 1")
    suspend fun getPendingInvite(projectId: String, inviteeId: String): ProjectInvite?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invite: ProjectInvite)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invites: List<ProjectInvite>)

    @Query("UPDATE project_invites SET status = :status, respondedAt = :respondedAt WHERE id = :inviteId")
    suspend fun updateStatus(inviteId: String, status: String, respondedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM project_invites WHERE id = :inviteId")
    suspend fun deleteById(inviteId: String)

    @Query("DELETE FROM project_invites WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: String)

    @Query("UPDATE project_invites SET status = 'EXPIRED' WHERE status = 'PENDING' AND expiresAt < :now")
    suspend fun expireOldInvites(now: Long = System.currentTimeMillis()): Int
}
