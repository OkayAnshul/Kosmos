package com.example.kosmos.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kosmos.core.database.KosmosDatabase
import com.example.kosmos.core.database.dao.SyncQueueDao
import com.example.kosmos.core.database.dao.UserConnectionDao
import com.example.kosmos.core.models.ConnectionStatus
import com.example.kosmos.core.models.User
import com.example.kosmos.core.models.UserConnection
import com.example.kosmos.data.datasource.SupabaseUserConnectionDataSource
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
class UserConnectionRepositoryIntegrationTest {

    private lateinit var db: KosmosDatabase
    private lateinit var connectionDao: UserConnectionDao
    private lateinit var syncQueueDao: SyncQueueDao

    private val supabaseDataSource: SupabaseUserConnectionDataSource = mockk(relaxed = true)
    private val notificationService: SupabaseNotificationService = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)

    private lateinit var repository: UserConnectionRepository

    private val requesterId = "user-req-1"
    private val addresseeId = "user-addr-1"

    @Before
    fun setUp() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, KosmosDatabase::class.java)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
        connectionDao = db.userConnectionDao()
        syncQueueDao = db.syncQueueDao()

        db.userDao().insertUser(User(id = requesterId, email = "req@test.com", username = "requester", displayName = "Requester"))
        db.userDao().insertUser(User(id = addresseeId, email = "addr@test.com", username = "addressee", displayName = "Addressee"))

        coEvery { networkMonitor.isOffline } returns MutableStateFlow(false)
        coEvery { supabaseDataSource.createConnection(any()) } returns Result.success(mockk(relaxed = true))
        coEvery { supabaseDataSource.updateStatus(any(), any()) } returns Result.success(Unit)
        coEvery { supabaseDataSource.removeConnection(any()) } returns Result.success(Unit)
        coEvery { notificationService.sendNotification(any(), any(), any(), any(), any()) } returns Result.success(Unit)

        repository = UserConnectionRepository(
            connectionDao = connectionDao,
            supabaseDataSource = supabaseDataSource,
            notificationService = notificationService,
            networkMonitor = networkMonitor,
            syncQueueDao = syncQueueDao
        )
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun sendRequest_newRequest_savesToRoom() = runTest {
        val result = repository.sendRequest(requesterId, addresseeId, "Requester")
        assertTrue("sendRequest should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val connection = connectionDao.getBetween(requesterId, addresseeId)
        assertNotNull(connection)
        assertEquals(ConnectionStatus.PENDING, connection?.status)
    }

    @Test
    fun sendRequest_selfConnection_returnsFailure() = runTest {
        val result = repository.sendRequest(requesterId, requesterId)
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue("Expected 'yourself' in error: $msg", msg.contains("yourself", ignoreCase = true))
    }

    @Test
    fun sendRequest_existingAcceptedConnection_returnsFailure() = runTest {
        val connection = UserConnection(requesterId = requesterId, addresseeId = addresseeId, status = ConnectionStatus.ACCEPTED)
        connectionDao.insert(connection)
        val result = repository.sendRequest(requesterId, addresseeId)
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue("Expected 'connected' in error: $msg", msg.contains("connected", ignoreCase = true))
    }

    @Test
    fun sendRequest_existingPendingConnection_returnsFailure() = runTest {
        val connection = UserConnection(requesterId = requesterId, addresseeId = addresseeId, status = ConnectionStatus.PENDING)
        connectionDao.insert(connection)
        val result = repository.sendRequest(requesterId, addresseeId)
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue("Expected 'pending' in error: $msg", msg.contains("pending", ignoreCase = true))
    }

    @Test
    fun acceptConnection_updatesStatusToAccepted() = runTest {
        val conn = repository.sendRequest(requesterId, addresseeId).getOrThrow()
        val result = repository.acceptConnection(conn.id)
        assertTrue(result.isSuccess)
        val updated = connectionDao.getById(conn.id)
        assertEquals(ConnectionStatus.ACCEPTED, updated?.status)
    }

    @Test
    fun declineConnection_updatesStatusToDeclined() = runTest {
        val conn = repository.sendRequest(requesterId, addresseeId).getOrThrow()
        val result = repository.declineConnection(conn.id)
        assertTrue(result.isSuccess)
        val updated = connectionDao.getById(conn.id)
        assertEquals(ConnectionStatus.DECLINED, updated?.status)
    }

    @Test
    fun removeConnection_deletesFromRoom() = runTest {
        val conn = repository.sendRequest(requesterId, addresseeId).getOrThrow()
        assertNotNull(connectionDao.getById(conn.id))
        repository.removeConnection(conn.id)
        assertNull(connectionDao.getById(conn.id))
    }

    @Test
    fun blockUser_noExistingConnection_createsBlockedConnection() = runTest {
        val result = repository.blockUser(requesterId, addresseeId)
        assertTrue(result.isSuccess)
        val connection = connectionDao.getBetween(requesterId, addresseeId)
        assertNotNull(connection)
        assertEquals(ConnectionStatus.BLOCKED, connection?.status)
    }

    @Test
    fun getBetween_bidirectional_findsBothWays() = runTest {
        repository.sendRequest(requesterId, addresseeId)
        val fromReq = connectionDao.getBetween(requesterId, addresseeId)
        val fromAddr = connectionDao.getBetween(addresseeId, requesterId)
        assertNotNull(fromReq)
        assertNotNull(fromAddr)
        assertEquals(fromReq?.id, fromAddr?.id)
    }

    @Test
    fun getAcceptedConnectionsFlow_emitsOnlyAccepted() = runTest {
        val conn = repository.sendRequest(requesterId, addresseeId).getOrThrow()
        repository.acceptConnection(conn.id)
        val accepted = repository.getAcceptedConnectionsFlow(requesterId).first()
        assertEquals(1, accepted.size)
        assertEquals(ConnectionStatus.ACCEPTED, accepted[0].status)
    }

    @Test
    fun getPendingRequestsFlow_emitsOnlyPendingForAddressee() = runTest {
        repository.sendRequest(requesterId, addresseeId)
        val pending = repository.getPendingRequestsFlow(addresseeId).first()
        assertEquals(1, pending.size)
        assertEquals(ConnectionStatus.PENDING, pending[0].status)
    }
}
