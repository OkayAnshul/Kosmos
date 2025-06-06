package com.example.kosmos.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kosmos.core.database.KosmosDatabase
import com.example.kosmos.core.models.ChatRoom
import com.example.kosmos.core.models.ChatRoomType
import com.example.kosmos.core.models.Message
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.User
import com.example.kosmos.data.datasource.SupabaseChatDataSource
import com.example.kosmos.data.datasource.SupabaseMessageDataSource
import com.example.kosmos.data.realtime.SupabaseRealtimeManager
import com.example.kosmos.data.sync.FKRetryQueue
import com.example.kosmos.shared.utils.NetworkMonitor
import io.github.jan.supabase.SupabaseClient
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
class ChatRepositoryIntegrationTest {

    private lateinit var db: KosmosDatabase
    private lateinit var repository: ChatRepository

    private val supabaseClient: SupabaseClient = mockk(relaxed = true)
    private val supabaseMessageDataSource: SupabaseMessageDataSource = mockk(relaxed = true)
    private val supabaseChatDataSource: SupabaseChatDataSource = mockk(relaxed = true)
    private val realtimeManager: SupabaseRealtimeManager = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)
    private val fkRetryQueue: FKRetryQueue = mockk(relaxed = true)

    private val projectId = "proj-chat-1"
    private val userId = "user-chat-1"

    @Before
    fun setUp() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, KosmosDatabase::class.java)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()

        db.userDao().insertUser(User(id = userId, email = "u@test.com", username = "user", displayName = "User"))
        db.projectDao().insertProject(Project(id = projectId, name = "Chat Project", ownerId = userId))

        coEvery { networkMonitor.isOffline } returns MutableStateFlow(false)
        coEvery { supabaseChatDataSource.insertChatRoom(any()) } returns Result.success(mockk(relaxed = true))
        coEvery { supabaseChatDataSource.updateChatRoom(any()) } returns Result.success(mockk(relaxed = true))
        coEvery { supabaseChatDataSource.deleteChatRoom(any()) } returns Result.success(Unit)
        coEvery { supabaseMessageDataSource.insertMessage(any()) } returns Result.success(mockk(relaxed = true))
        coEvery { supabaseMessageDataSource.updateMessage(any(), any(), any()) } returns Result.success(mockk(relaxed = true))
        coEvery { supabaseMessageDataSource.deleteMessage(any()) } returns Result.success(Unit)

        repository = ChatRepository(
            chatRoomDao = db.chatRoomDao(),
            messageDao = db.messageDao(),
            projectDao = db.projectDao(),
            supabase = supabaseClient,
            supabaseMessageDataSource = supabaseMessageDataSource,
            supabaseChatDataSource = supabaseChatDataSource,
            realtimeManager = realtimeManager,
            networkMonitor = networkMonitor,
            syncQueueDao = db.syncQueueDao(),
            fkRetryQueue = fkRetryQueue
        )
    }

    @After
    fun tearDown() { db.close() }

    private fun newChatRoom() = ChatRoom(
        projectId = projectId,
        name = "General",
        type = ChatRoomType.CHANNEL,
        createdBy = userId,
        participantIds = listOf(userId)
    )

    private fun newMessage(chatRoomId: String) = Message(
        chatRoomId = chatRoomId,
        senderId = userId,
        senderName = "User",
        content = "Hello world"
    )

    @Test
    fun createChatRoom_success_savesToRoom() = runTest {
        val result = repository.createChatRoom(newChatRoom())
        assertTrue("createChatRoom should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val roomId = result.getOrThrow()
        assertNotNull(db.chatRoomDao().getChatRoomById(roomId))
    }

    @Test
    fun sendMessage_success_savesToRoom() = runTest {
        val roomId = repository.createChatRoom(newChatRoom()).getOrThrow()
        val result = repository.sendMessage(newMessage(roomId))
        assertTrue("sendMessage should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val messages = db.messageDao().getMessagesForChatRoom(roomId)
        assertEquals(1, messages.size)
    }

    @Test
    fun markMessageAsRead_addsUserToReadBy() = runTest {
        val roomId = repository.createChatRoom(newChatRoom()).getOrThrow()
        val messageId = repository.sendMessage(newMessage(roomId)).getOrThrow()
        val readerId = "reader-1"
        val result = repository.markMessageAsRead(messageId, readerId)
        assertTrue(result.isSuccess)
        val message = db.messageDao().getMessageById(messageId)
        assertTrue("Reader should be in readBy list", message?.readBy?.contains(readerId) == true)
    }

    @Test
    fun editMessage_updatesContent() = runTest {
        val roomId = repository.createChatRoom(newChatRoom()).getOrThrow()
        val messageId = repository.sendMessage(newMessage(roomId)).getOrThrow()
        val result = repository.editMessage(messageId, "Updated content")
        assertTrue(result.isSuccess)
        val updated = db.messageDao().getMessageById(messageId)
        assertEquals("Updated content", updated?.content)
        assertTrue("isEdited should be true", updated?.isEdited == true)
    }

    @Test
    fun deleteMessage_removesFromRoom() = runTest {
        val roomId = repository.createChatRoom(newChatRoom()).getOrThrow()
        val messageId = repository.sendMessage(newMessage(roomId)).getOrThrow()
        assertNotNull(db.messageDao().getMessageById(messageId))
        repository.deleteMessage(messageId)
        assertNull(db.messageDao().getMessageById(messageId))
    }

    @Test
    fun archiveChatRoom_setsIsArchived() = runTest {
        val roomId = repository.createChatRoom(newChatRoom()).getOrThrow()
        val result = repository.archiveChatRoom(roomId, true)
        assertTrue(result.isSuccess)
        val room = db.chatRoomDao().getChatRoomById(roomId)
        assertTrue("Room should be archived", room?.isArchived == true)
    }

    @Test
    fun getMessagesFlow_emitsInsertedMessages() = runTest {
        val roomId = repository.createChatRoom(newChatRoom()).getOrThrow()
        repository.sendMessage(newMessage(roomId))
        repository.sendMessage(newMessage(roomId))
        val messages = repository.getMessagesFlow(roomId).first()
        assertEquals(2, messages.size)
    }

    @Test
    fun getChatRoomsForProject_emitsProjectRooms() = runTest {
        repository.createChatRoom(newChatRoom())
        val rooms = repository.getChatRoomsForProject(userId, projectId).first()
        assertEquals(1, rooms.size)
    }
}
