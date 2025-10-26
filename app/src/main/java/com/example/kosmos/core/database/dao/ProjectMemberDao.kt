package com.example.kosmos.core.database.dao

import androidx.room.*
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectMemberDao {
    @Query("SELECT * FROM project_members WHERE id = :memberId")
    suspend fun getMemberById(memberId: String): ProjectMember?

    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND userId = :userId")
    suspend fun getMemberByProjectAndUser(projectId: String, userId: String): ProjectMember?

    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND userId = :userId")
    fun getMemberByProjectAndUserFlow(projectId: String, userId: String): Flow<ProjectMember?>

    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND isActive = 1 ORDER BY joinedAt ASC")
    fun getProjectMembers(projectId: String): Flow<List<ProjectMember>>

    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND role = :role AND isActive = 1")
    fun getMembersByRole(projectId: String, role: ProjectRole): Flow<List<ProjectMember>>

    @Query("SELECT * FROM project_members WHERE userId = :userId AND isActive = 1 ORDER BY lastActivityAt DESC")
    fun getUserMemberships(userId: String): Flow<List<ProjectMember>>

    @Query("SELECT COUNT(*) FROM project_members WHERE projectId = :projectId AND isActive = 1")
    suspend fun getActiveMemberCount(projectId: String): Int

    @Query("SELECT COUNT(*) FROM project_members WHERE projectId = :projectId AND role = :role AND isActive = 1")
    suspend fun getMemberCountByRole(projectId: String, role: ProjectRole): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: ProjectMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<ProjectMember>)

    @Update
    suspend fun updateMember(member: ProjectMember)

    @Delete
    suspend fun deleteMember(member: ProjectMember)

    @Query("DELETE FROM project_members WHERE id = :memberId")
    suspend fun deleteMemberById(memberId: String)

    @Query("DELETE FROM project_members WHERE projectId = :projectId AND userId = :userId")
    suspend fun removeMemberFromProject(projectId: String, userId: String)

    @Query("UPDATE project_members SET role = :newRole WHERE id = :memberId")
    suspend fun updateMemberRole(memberId: String, newRole: ProjectRole)

    @Query("UPDATE project_members SET isActive = :isActive WHERE id = :memberId")
    suspend fun updateMemberStatus(memberId: String, isActive: Boolean)

    @Query("UPDATE project_members SET lastActivityAt = :timestamp WHERE projectId = :projectId AND userId = :userId")
    suspend fun updateLastActivity(projectId: String, userId: String, timestamp: Long)
}
