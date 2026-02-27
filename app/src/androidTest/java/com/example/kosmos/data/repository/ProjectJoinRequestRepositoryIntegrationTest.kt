package com.example.kosmos.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kosmos.core.database.KosmosDatabase
import com.example.kosmos.core.database.dao.ProjectJoinRequestDao
import com.example.kosmos.core.database.dao.ProjectMemberDao
import com.example.kosmos.core.database.dao.SyncQueueDao
import com.example.kosmos.core.models.JoinRequestStatus
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.User
import com.example.kosmos.data.datasource.SupabaseProjectJoinRequestDataSource
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
class ProjectJoinRequestRepositoryIntegrationTest {

    private lateinit var db: KosmosDatabase
    private lateinit var joinRequestDao: ProjectJoinRequestDao
    private lateinit var projectMemberDao: ProjectMemberDao
    private lateinit var syncQueueDao: SyncQueueDao

    private val supabaseDataSource: SupabaseProjectJoinRequestDataSource = mockk(relaxed = true)
    private val projectRepository: ProjectRepository = mockk(relaxed = true)
    private val notificationService: SupabaseNotificationService = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)

    private lateinit var repository: ProjectJoinRequestRepository

    private val projectId = "proj-jr-1"
    private val ownerId = "owner-jr-1"
    private val requesterId = "requester-jr-1"
    private val reviewerId = "reviewer-jr-1"

    @Before
    fun setUp() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, KosmosDatabase::class.java)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
        joinRequestDao = db.projectJoinRequestDao()
        projectMemberDao = db.projectMemberDao()
        syncQueueDao = db.syncQueueDao()

        db.projectDao().insertProject(Project(id = projectId, name = "Test Project", ownerId = ownerId))
        db.userDao().insertUser(User(id = requesterId, email = "req@test.com", username = "requester", displayName = "Requester"))
        db.userDao().insertUser(User(id = reviewerId, email = "rev@test.com", username = "reviewer", displayName = "Reviewer"))
        projectMemberDao.insertMember(ProjectMember(id = "mem-reviewer", projectId = projectId, userId = reviewerId, role = ProjectRole.ADMIN))

        coEvery { networkMonitor.isOffline } returns MutableStateFlow(false)
        coEvery { supabaseDataSource.createRequest(any()) } returns Result.success(mockk(relaxed = true))
        coEvery { supabaseDataSource.updateStatus(any(), any(), any()) } returns Result.success(Unit)
        coEvery { supabaseDataSource.cancelRequest(any()) } returns Result.success(Unit)
        coEvery { projectRepository.addMember(any(), any(), any(), any(), any()) } returns Result.success(
            ProjectMember(id = "new-mem", projectId = projectId, userId = requesterId, role = ProjectRole.MEMBER)
        )
        coEvery { notificationService.sendNotification(any(), any(), any(), any(), any()) } returns Result.success(Unit)

        repository = ProjectJoinRequestRepository(
            joinRequestDao = joinRequestDao,
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
    fun requestToJoin_notMember_savesToRoom() = runTest {
        val result = repository.requestToJoin(
            projectId = projectId,
            requesterId = requesterId,
            message = "Please let me join",
            projectName = "Test Project",
            requesterName = "Requester"
        )
        assertTrue("requestToJoin should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val request = joinRequestDao.getExisting(projectId, requesterId)
        assertNotNull(request)
        assertEquals(JoinRequestStatus.PENDING, request?.status)
    }

    @Test
    fun requestToJoin_alreadyMember_returnsFailure() = runTest {
        projectMemberDao.insertMember(ProjectMember(id = "existing-mem", projectId = projectId, userId = requesterId, role = ProjectRole.MEMBER))
        val result = repository.requestToJoin(projectId, requesterId)
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue("Expected 'member' in error: $msg", msg.contains("member", ignoreCase = true))
    }

    @Test
    fun requestToJoin_pendingRequestExists_returnsFailure() = runTest {
        repository.requestToJoin(projectId, requesterId)
        val result = repository.requestToJoin(projectId, requesterId)
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue("Expected 'pending' in error: $msg", msg.contains("pending", ignoreCase = true))
    }

    @Test
    fun approveRequest_pendingRequest_updatesStatus() = runTest {
        val request = repository.requestToJoin(projectId, requesterId).getOrThrow()
        val result = repository.approveRequest(request.id, reviewerId)
        assertTrue("approveRequest should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val updated = joinRequestDao.getById(request.id)
        assertEquals(JoinRequestStatus.APPROVED, updated?.status)
    }

    @Test
    fun rejectRequest_pendingRequest_updatesStatusToRejected() = runTest {
        val request = repository.requestToJoin(projectId, requesterId).getOrThrow()
        val result = repository.rejectRequest(request.id, reviewerId)
        assertTrue(result.isSuccess)
        val updated = joinRequestDao.getById(request.id)
        assertEquals(JoinRequestStatus.REJECTED, updated?.status)
    }

    @Test
    fun cancelRequest_deletesFromRoom() = runTest {
        val request = repository.requestToJoin(projectId, requesterId).getOrThrow()
        assertNotNull(joinRequestDao.getById(request.id))
        repository.cancelRequest(request.id)
        assertNull(joinRequestDao.getById(request.id))
    }

    @Test
    fun getRequestsForProjectFlow_emitsPendingRequests() = runTest {
        repository.requestToJoin(projectId, requesterId)
        val requests = repository.getRequestsForProjectFlow(projectId).first()
        assertEquals(1, requests.size)
    }

    @Test
    fun getMyRequestForProject_returnsCorrectRequest() = runTest {
        repository.requestToJoin(projectId, requesterId)
        val request = repository.getMyRequestForProject(requesterId, projectId)
        assertNotNull(request)
        assertEquals(requesterId, request?.requesterId)
    }
}
