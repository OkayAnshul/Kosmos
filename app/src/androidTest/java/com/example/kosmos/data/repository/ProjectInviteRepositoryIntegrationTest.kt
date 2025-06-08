package com.example.kosmos.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kosmos.core.database.KosmosDatabase
import com.example.kosmos.core.database.dao.ProjectInviteDao
import com.example.kosmos.core.database.dao.ProjectMemberDao
import com.example.kosmos.core.database.dao.SyncQueueDao
import com.example.kosmos.core.models.InviteStatus
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectInvite
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.User
import com.example.kosmos.data.datasource.SupabaseProjectInviteDataSource
import com.example.kosmos.features.notifications.SupabaseNotificationService
import com.example.kosmos.shared.utils.NetworkMonitor
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectInviteRepositoryIntegrationTest {

    private lateinit var db: KosmosDatabase
    private lateinit var inviteDao: ProjectInviteDao
    private lateinit var projectMemberDao: ProjectMemberDao
    private lateinit var syncQueueDao: SyncQueueDao

    private val supabaseDataSource: SupabaseProjectInviteDataSource = mockk(relaxed = true)
    private val projectRepository: ProjectRepository = mockk(relaxed = true)
    private val notificationService: SupabaseNotificationService = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)

    private lateinit var repository: ProjectInviteRepository

    private val projectId = "proj-invite-1"
    private val ownerId = "owner-1"
    private val inviteeId = "invitee-1"
    private val inviterId = "inviter-1"

    @Before
    fun setUp() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, KosmosDatabase::class.java)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
        inviteDao = db.projectInviteDao()
        projectMemberDao = db.projectMemberDao()
        syncQueueDao = db.syncQueueDao()

        // Seed project and inviter member
        db.projectDao().insertProject(Project(id = projectId, name = "Test Project", ownerId = ownerId))
        db.userDao().insertUser(User(id = inviterId, email = "inviter@test.com", username = "inviter", displayName = "Inviter"))
        db.userDao().insertUser(User(id = inviteeId, email = "invitee@test.com", username = "invitee", displayName = "Invitee"))
        projectMemberDao.insertMember(ProjectMember(id = "mem-1", projectId = projectId, userId = inviterId, role = ProjectRole.ADMIN))

        coEvery { networkMonitor.isOffline } returns MutableStateFlow(false)
        coEvery { supabaseDataSource.createInvite(any()) } returns Result.success(mockk(relaxed = true))
        coEvery { supabaseDataSource.updateStatus(any(), any()) } returns Result.success(Unit)
        coEvery { supabaseDataSource.cancelInvite(any()) } returns Result.success(Unit)
        coEvery { projectRepository.addMember(any(), any(), any(), any(), any()) } returns Result.success(
            ProjectMember(id = "new-mem", projectId = projectId, userId = inviteeId, role = ProjectRole.MEMBER)
        )
        coEvery { projectRepository.syncUserProjects(any()) } returns Result.success(Unit)
        coEvery { notificationService.sendNotification(any(), any(), any(), any(), any()) } returns Result.success(Unit)

        repository = ProjectInviteRepository(
            inviteDao = inviteDao,
            projectMemberDao = projectMemberDao,
            supabaseDataSource = supabaseDataSource,
            projectRepository = projectRepository,
            notificationService = notificationService,
            networkMonitor = networkMonitor,
            syncQueueDao = syncQueueDao
        )
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun sendInvite_newInvite_savesToRoom() = runTest {
        val result = repository.sendInvite(
            projectId = projectId,
            inviteeId = inviteeId,
            inviterId = inviterId,
            projectName = "Test Project",
            inviterName = "Inviter"
        )
        assertTrue("sendInvite should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val invites = inviteDao.getByProjectFlow(projectId).first()
        assertEquals(1, invites.size)
        assertEquals(inviteeId, invites[0].inviteeId)
    }

    @Test
    fun sendInvite_alreadyMember_returnsFailure() = runTest {
        // Add invitee as member
        projectMemberDao.insertMember(ProjectMember(id = "mem-2", projectId = projectId, userId = inviteeId, role = ProjectRole.MEMBER))
        val result = repository.sendInvite(projectId, inviteeId, inviterId)
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue("Expected 'member' in error: $msg", msg.contains("member", ignoreCase = true))
    }

    @Test
    fun sendInvite_pendingInviteAlreadyExists_returnsFailure() = runTest {
        repository.sendInvite(projectId, inviteeId, inviterId)
        val result = repository.sendInvite(projectId, inviteeId, inviterId)
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue("Expected 'pending' in error: $msg", msg.contains("pending", ignoreCase = true))
    }

    @Test
    fun acceptInvite_pendingInvite_updatesStatusToAccepted() = runTest {
        val invite = repository.sendInvite(projectId, inviteeId, inviterId).getOrThrow()
        val result = repository.acceptInvite(invite.id, inviteeId)
        assertTrue("acceptInvite should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val updated = inviteDao.getById(invite.id)
        assertEquals(InviteStatus.ACCEPTED.name, updated?.status?.name)
    }

    @Test
    fun acceptInvite_inviteNotFound_returnsFailure() = runTest {
        val result = repository.acceptInvite("non-existent-id", inviteeId)
        assertTrue(result.isFailure)
    }

    @Test
    fun declineInvite_pendingInvite_updatesStatusToDeclined() = runTest {
        val invite = repository.sendInvite(projectId, inviteeId, inviterId).getOrThrow()
        val result = repository.declineInvite(invite.id)
        assertTrue(result.isSuccess)
        val updated = inviteDao.getById(invite.id)
        assertEquals(InviteStatus.DECLINED.name, updated?.status?.name)
    }

    @Test
    fun cancelInvite_deletesFromRoom() = runTest {
        val invite = repository.sendInvite(projectId, inviteeId, inviterId).getOrThrow()
        assertNotNull(inviteDao.getById(invite.id))
        repository.cancelInvite(invite.id)
        assertNull(inviteDao.getById(invite.id))
    }

    @Test
    fun getPendingForUserFlow_emitsPendingInvites() = runTest {
        repository.sendInvite(projectId, inviteeId, inviterId)
        val pending = repository.getPendingForUserFlow(inviteeId).first()
        assertEquals(1, pending.size)
    }
}
