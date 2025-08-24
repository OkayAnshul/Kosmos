package com.example.kosmos

import android.content.Context
import android.net.Uri
import com.example.kosmos.database.ChatRoomDao
import com.example.kosmos.database.MessageDao
import com.example.kosmos.database.TaskDao
import com.example.kosmos.database.UserDao
import com.example.kosmos.database.VoiceMessageDao
import com.example.kosmos.models.ChatRoom
import com.example.kosmos.models.Message
import com.example.kosmos.models.Task
import com.example.kosmos.models.TaskStatus
import com.example.kosmos.models.User
import com.example.kosmos.models.VoiceMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    fun isUserLoggedIn(): Boolean = getCurrentUser() != null

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val user = getUserFromFirestore(firebaseUser.uid)
                    ?: createUserInFirestore(firebaseUser)
                Result.success(user)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String
    ): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val user = User(
                    id = firebaseUser.uid,
                    email = email,
                    displayName = displayName,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    isOnline = true
                )
                saveUserToFirestore(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Account creation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            getCurrentUser()?.uid?.let { userId ->
                updateUserOnlineStatus(userId, false)
            }
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getUserFromFirestore(userId: String): User? {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun createUserInFirestore(firebaseUser: FirebaseUser): User {
        val user = User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "",
            photoUrl = firebaseUser.photoUrl?.toString(),
            isOnline = true
        )
        saveUserToFirestore(user)
        return user
    }

    private suspend fun saveUserToFirestore(user: User) {
        firestore.collection("users").document(user.id).set(user).await()
    }

    private suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean) {
        try {
            val updates = mapOf(
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
            firestore.collection("users").document(userId).update(updates).await()
        } catch (e: Exception) {
            // Handle error silently
        }
    }
}



@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore
) {

    suspend fun getUserById(userId: String): User? {
        // Try local first
        val localUser = userDao.getUserById(userId)
        if (localUser != null) return localUser

        // Fetch from Firebase if not in local DB
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            val user = document.toObject(User::class.java)
            user?.let { userDao.insertUser(it) }
            user
        } catch (e: Exception) {
            null
        }
    }

    fun getUserByIdFlow(userId: String): Flow<User?> = flow {
        // Emit local data first
        val localUser = userDao.getUserById(userId)
        emit(localUser)

        // Listen to Firebase updates
        try {
            firestore.collection("users").document(userId)
                .snapshots()
                .collect { snapshot ->
                    val user = snapshot.toObject(User::class.java)
                    if (user != null) {
                        userDao.insertUser(user)
                        emit(user)
                    }
                }
        } catch (e: Exception) {
            // Continue with local data
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            // Update Firebase
            firestore.collection("users").document(user.id).set(user).await()
            // Update local
            userDao.updateUser(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): List<User> {
        return try {
            val result = firestore.collection("users")
                .whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
                .limit(20)
                .get()
                .await()

            val users = result.documents.mapNotNull { it.toObject(User::class.java) }
            userDao.insertUsers(users)
            users
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateFcmToken(userId: String, fcmToken: String) {
        try {
            val updates = mapOf("fcmToken" to fcmToken)
            firestore.collection("users").document(userId).update(updates).await()
            userDao.getUserById(userId)?.let { user ->
                userDao.updateUser(user.copy(fcmToken = fcmToken))
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }
}



@Singleton
class ChatRepository @Inject constructor(
    private val chatRoomDao: ChatRoomDao,
    private val messageDao: MessageDao,
    private val firestore: FirebaseFirestore
) {

    suspend fun createChatRoom(chatRoom: ChatRoom): Result<String> {
        return try {
            val roomId = if (chatRoom.id.isEmpty()) UUID.randomUUID().toString() else chatRoom.id
            val roomWithId = chatRoom.copy(id = roomId)

            // Save to Firebase
            firestore.collection("chatRooms").document(roomId).set(roomWithId).await()
            // Save to local
            chatRoomDao.insertChatRoom(roomWithId)

            Result.success(roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getChatRoomsFlow(userId: String): Flow<List<ChatRoom>> = flow {
        // Emit local data first
        val localRooms = chatRoomDao.getAllChatRoomsFlow()
        localRooms.collect { rooms ->
            emit(rooms.filter { it.participantIds.contains(userId) })
        }

        // Listen to Firebase updates
        try {
            firestore.collection("chatRooms")
                .whereArrayContains("participantIds", userId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .snapshots()
                .collect { snapshot ->
                    val rooms = snapshot.documents.mapNotNull { it.toObject(ChatRoom::class.java) }
                    chatRoomDao.insertChatRooms(rooms)
                    emit(rooms)
                }
        } catch (e: Exception) {
            // Continue with local data
        }
    }

    fun getMessagesFlow(chatRoomId: String): Flow<List<Message>> = flow {
        // Emit local data first
        val localMessages = messageDao.getMessagesForChatRoomFlow(chatRoomId)
        localMessages.collect { messages ->
            emit(messages)
        }

        // Listen to Firebase updates
        try {
            firestore.collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .collect { snapshot ->
                    val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    messageDao.insertMessages(messages)
                    emit(messages)
                }
        } catch (e: Exception) {
            // Continue with local data
        }
    }

    suspend fun sendMessage(message: Message): Result<String> {
        return try {
            val messageId = if (message.id.isEmpty()) UUID.randomUUID().toString() else message.id
            val messageWithId = message.copy(id = messageId)

            // Save to Firebase
            firestore.collection("chatRooms")
                .document(message.chatRoomId)
                .collection("messages")
                .document(messageId)
                .set(messageWithId)
                .await()

            // Update chat room's last message info
            val chatRoomUpdates = mapOf(
                "lastMessageId" to messageId,
                "lastMessageTimestamp" to messageWithId.timestamp
            )
            firestore.collection("chatRooms")
                .document(message.chatRoomId)
                .update(chatRoomUpdates)
                .await()

            // Save to local
            messageDao.insertMessage(messageWithId)

            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadOlderMessages(chatRoomId: String, beforeTimestamp: Long): List<Message> {
        return try {
            val result = firestore.collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .whereLessThan("timestamp", beforeTimestamp)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()

            val messages = result.documents.mapNotNull { it.toObject(Message::class.java) }
            messageDao.insertMessages(messages)
            messages
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun markMessageAsRead(messageId: String, userId: String) {
        try {
            val message = messageDao.getMessageById(messageId)
            if (message != null) {
                val updatedReadBy = message.readBy.toMutableMap()
                updatedReadBy[userId] = System.currentTimeMillis()

                val updates = mapOf("readBy.$userId" to System.currentTimeMillis())
                firestore.collection("chatRooms")
                    .document(message.chatRoomId)
                    .collection("messages")
                    .document(messageId)
                    .update(updates)
                    .await()

                messageDao.updateMessage(message.copy(readBy = updatedReadBy))
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }
}

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val firestore: FirebaseFirestore
) {

    suspend fun createTask(task: Task): Result<String> {
        return try {
            val taskId = if (task.id.isEmpty()) UUID.randomUUID().toString() else task.id
            val taskWithId = task.copy(id = taskId)

            // Save to Firebase
            firestore.collection("chatRooms")
                .document(task.chatRoomId)
                .collection("tasks")
                .document(taskId)
                .set(taskWithId)
                .await()

            // Save to local
            taskDao.insertTask(taskWithId)

            Result.success(taskId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTasksFlow(chatRoomId: String): Flow<List<Task>> = flow {
        // Emit local data first
        val localTasks = taskDao.getTasksForChatRoomFlow(chatRoomId)
        localTasks.collect { tasks ->
            emit(tasks)
        }

        // Listen to Firebase updates
        try {
            firestore.collection("chatRooms")
                .document(chatRoomId)
                .collection("tasks")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .snapshots()
                .collect { snapshot ->
                    val tasks = snapshot.documents.mapNotNull { it.toObject(Task::class.java) }
                    taskDao.insertTasks(tasks)
                    emit(tasks)
                }
        } catch (e: Exception) {
            // Continue with local data
        }
    }

    suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            val updatedTask = task.copy(updatedAt = System.currentTimeMillis())

            // Update Firebase
            firestore.collection("chatRooms")
                .document(task.chatRoomId)
                .collection("tasks")
                .document(task.id)
                .set(updatedTask)
                .await()

            // Update local
            taskDao.updateTask(updatedTask)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: TaskStatus): Result<Unit> {
        return try {
            val task = taskDao.getTaskById(taskId) ?: return Result.failure(Exception("Task not found"))
            val updatedTask = task.copy(status = newStatus, updatedAt = System.currentTimeMillis())

            updateTask(updatedTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignTask(taskId: String, assignedToId: String, assignedToName: String): Result<Unit> {
        return try {
            val task = taskDao.getTaskById(taskId) ?: return Result.failure(Exception("Task not found"))
            val updatedTask = task.copy(
                assignedToId = assignedToId,
                assignedToName = assignedToName,
                updatedAt = System.currentTimeMillis()
            )

            updateTask(updatedTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMyTasksFlow(userId: String): Flow<List<Task>> {
        return taskDao.getMyActiveTasksFlow(userId)
    }

    suspend fun getOverdueTasks(): List<Task> {
        return taskDao.getOverdueTasks(System.currentTimeMillis())
    }
}

@Singleton
class VoiceRepository @Inject constructor(
    private val voiceMessageDao: VoiceMessageDao,
    private val firebaseStorage: FirebaseStorage,
    @ApplicationContext private val context: Context
) {

    suspend fun uploadVoiceMessage(audioFile: File, voiceMessage: VoiceMessage): Result<String> {
        return try {
            val voiceMessageId = if (voiceMessage.id.isEmpty()) UUID.randomUUID().toString() else voiceMessage.id
            val audioFileName = "${voiceMessageId}.m4a"

            // Upload to Firebase Storage
            val storageRef = firebaseStorage.reference
                .child("voice_messages")
                .child(audioFileName)

            val uploadTask = storageRef.putFile(Uri.fromFile(audioFile)).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()

            val voiceMessageWithUrl = voiceMessage.copy(
                id = voiceMessageId,
                audioUrl = downloadUrl.toString()
            )

            // Save to local database
            voiceMessageDao.insertVoiceMessage(voiceMessageWithUrl)

            Result.success(voiceMessageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTranscription(voiceMessageId: String, transcription: String, confidence: Float) {
        try {
            val voiceMessage = voiceMessageDao.getVoiceMessageById(voiceMessageId)
            if (voiceMessage != null) {
                val updatedVoiceMessage = voiceMessage.copy(
                    transcription = transcription,
                    transcriptionConfidence = confidence,
                    isTranscribing = false
                )
                voiceMessageDao.updateVoiceMessage(updatedVoiceMessage)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun markTranscriptionInProgress(voiceMessageId: String) {
        try {
            val voiceMessage = voiceMessageDao.getVoiceMessageById(voiceMessageId)
            if (voiceMessage != null) {
                val updatedVoiceMessage = voiceMessage.copy(isTranscribing = true)
                voiceMessageDao.updateVoiceMessage(updatedVoiceMessage)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun getPendingTranscriptions(): List<VoiceMessage> {
        return voiceMessageDao.getPendingTranscriptions()
    }
}