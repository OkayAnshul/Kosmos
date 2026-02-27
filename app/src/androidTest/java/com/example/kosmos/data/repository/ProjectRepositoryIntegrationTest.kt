package com.example.kosmos.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kosmos.core.coroutines.DispatcherProvider
import com.example.kosmos.core.database.KosmosDatabase
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.User
import com.example.kosmos.data.datasource.SupabaseProjectDataSource
import com.example.kosmos.data.datasource.SupabaseProjectInviteDataSource
import com.example.kosmos.data.datasource.SupabaseProjectMemberDataSource
import com.example.kosmos.features.notifications.SupabaseNotificationService
import com.example.kosmos.shared.utils.NetworkMonitor
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
class ProjectRepositoryIntegrationTest {

    private lateinit var db: KosmosDatabase
    private lateinit var repository: ProjectRepository

    private val supabaseProjectDataSource: SupabaseProjectDataSource = mockk(relaxed = true)
    private val supabaseProjectMemberDataSource: SupabaseProjectMemberDataSource = mockk(relaxed = true)
    private val supabaseProjectInviteDataSource: SupabaseProjectInviteDataSource = mockk(relaxed = true)
    private val notificationService: SupabaseNotificationService = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)

    private val testDispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private val ownerId = "owner-repo-1"
    private val memberId = "member-repo-1"

    @Before
    fun setUp() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, KosmosDatabase::class.java)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()

        db.userDao().insertUser(User(id = ownerId, email = "owner@test.com", username = "owner", displayName = "Owner"))
        db.userDao().insertUser(User(id = memberId, email = "member@test.com", username = "member", displayName = "Member"))

        coEvery { networkMonitor.isOffline } returns MutableStateFlow(false)
        coEvery { supabaseProjectDataSource.insert(any()) } returns Result.success(mockk(relaxed = true))
        coEvery { supabaseProjectDataSource.update(any()) } returns Result.success(mockk(relaxed = true))
        coEvery { supabaseProjectDataSource.delete(any()) } returns Result.success(Unit)
        coEvery { supabaseProjectMemberDataSource.insert(any()) } returns Result.success(mockk(relaxed = true))
        coEvery { supabaseProjectMemberDataSource.update(any()) } returns Result.success(mockk(relaxed = true))
        coEvery { supabaseProjectMemberDataSource.removeMember(any(), any()) } returns Result.success(Unit)
        coEvery { notificationService.sendNotification(any(), any(), any(), any(), any()) } returns Result.success(Unit)

        repository = ProjectRepository(
            database = db,
            projectDao = db.projectDao(),
            projectMemberDao = db.projectMemberDao(),
            supabaseProjectDataSource = supabaseProjectDataSource,
            supabaseProjectMemberDataSource = supabaseProjectMemberDataSource,
            chatRoomDao = db.chatRoomDao(),
            taskDao = db.taskDao(),
            networkMonitor = networkMonitor,
            syncQueueDao = db.syncQueueDao(),
            dispatchers = testDispatchers,
            projectInviteDao = db.projectInviteDao(),
            supabaseProjectInviteDataSource = supabaseProjectInviteDataSource,
            notificationService = notificationService
        )
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun createProject_success_savesToRoom() = runTest {
        val result = repository.createProject("My Project", "Description", ownerId)
        assertTrue("createProject should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val project = result.getOrThrow()
        assertNotNull(db.projectDao().getProjectById(project.id))
    }

    @Test
    fun createProject_ownerBecomesMember() = runTest {
        val project = repository.createProject("My Project", "Desc", ownerId).getOrThrow()
        val member = db.projectMemberDao().getMemberByProjectAndUser(project.id, ownerId)
        assertNotNull("Owner should be added as member", member)
        assertEquals(ProjectRole.ADMIN, member?.role)
    }

    @Test
    fun addMember_newMember_incrementsMemberCount() = runTest {
        val project = repository.createProject("Proj", "Desc", ownerId).getOrThrow()
        val memberCountBefore = db.projectDao().getProjectById(project.id)?.memberCount ?: 0

        repository.addMember(project.id, memberId, ProjectRole.MEMBER, ownerId)

        val memberCountAfter = db.projectDao().getProjectById(project.id)?.memberCount ?: 0
        assertTrue("Member count should increase", memberCountAfter > memberCountBefore)
    }

    @Test
    fun addMember_existingMember_returnsFailure() = runTest {
        val project = repository.createProject("Proj", "Desc", ownerId).getOrThrow()
        repository.addMember(project.id, memberId, ProjectRole.MEMBER, ownerId)
        val result = repository.addMember(project.id, memberId, ProjectRole.MEMBER, ownerId)
        assertTrue("Adding duplicate member should fail", result.isFailure)
    }

    @Test
    fun removeMember_memberExists_removesFromRoom() = runTest {
        val project = repository.createProject("Proj", "Desc", ownerId).getOrThrow()
        repository.addMember(project.id, memberId, ProjectRole.MEMBER, ownerId)
        assertNotNull(db.projectMemberDao().getMemberByProjectAndUser(project.id, memberId))

        val result = repository.removeMember(project.id, memberId, ownerId)
        assertTrue("removeMember should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        assertNull(db.projectMemberDao().getMemberByProjectAndUser(project.id, memberId))
    }

    @Test
    fun changeRole_memberExists_updatesRole() = runTest {
        val project = repository.createProject("Proj", "Desc", ownerId).getOrThrow()
        repository.addMember(project.id, memberId, ProjectRole.MEMBER, ownerId)

        val result = repository.changeRole(project.id, memberId, ProjectRole.MANAGER, ownerId)
        assertTrue("changeRole should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val member = db.projectMemberDao().getMemberByProjectAndUser(project.id, memberId)
        assertEquals(ProjectRole.MANAGER, member?.role)
    }

    @Test
    fun getUserProjectsFlow_emitsProjectsForUser() = runTest {
        repository.createProject("Proj 1", "Desc", ownerId)
        repository.createProject("Proj 2", "Desc", ownerId)
        val projects = repository.getUserProjectsFlow(ownerId).first()
        assertTrue("Should have at least 2 projects", projects.size >= 2)
    }

    @Test
    fun deleteProject_nonOwner_returnsFailure() = runTest {
        // Only ADMIN/OWNER can delete; plain MEMBER cannot
        val project = repository.createProject("Proj", "Desc", ownerId).getOrThrow()
        // Add a plain MEMBER
        db.projectMemberDao().insertMember(
            ProjectMember(id = "mem-plain", projectId = project.id, userId = memberId, role = ProjectRole.MEMBER)
        )
        val result = repository.deleteProject(project.id, memberId)
        // MEMBER does not have DELETE_PROJECT permission → should fail
        assertTrue("Plain member should not be able to delete project", result.isFailure)
        assertNotNull("Project should still exist", db.projectDao().getProjectById(project.id))
    }

    @Test
    fun addMember_noPermission_returnsFailure() = runTest {
        val project = repository.createProject("Proj", "Desc", ownerId).getOrThrow()
        // memberId is not yet in the project, so cannot invite anyone
        val thirdUserId = "third-user-1"
        db.userDao().insertUser(User(id = thirdUserId, email = "t@test.com", username = "third", displayName = "Third"))
        val result = repository.addMember(project.id, thirdUserId, ProjectRole.MEMBER, memberId)
        assertTrue("Non-member should not be able to add members", result.isFailure)
    }
}
