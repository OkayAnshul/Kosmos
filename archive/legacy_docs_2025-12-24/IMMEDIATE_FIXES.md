# 🔧 **IMMEDIATE FIXES - Step-by-Step Implementation**

## 🎯 **PRIORITY ORDER (Fix in this order for fastest results)**

1. ✅ **Enable Background Services** (Uncomment existing code)
2. ✅ **Add API Key Configuration** (Simple config change)
3. ✅ **Integrate Voice Recording** (Connect existing components)
4. ✅ **Add Permission Handling** (Add runtime permissions)
5. ✅ **Test & Validate** (Verify functionality)

---

## 🚀 **FIX #1: ENABLE BACKGROUND SERVICES**

### **Problem**: Critical services commented out in AndroidManifest.xml
### **Impact**: No FCM notifications, no background transcription, no file sharing
### **Time**: 10 minutes

### **Step 1.1: Uncomment Services in AndroidManifest.xml**

```xml
<!-- AndroidManifest.xml - UNCOMMENT these sections -->

<!-- FCM Service for push notifications -->
<service
    android:name=".fcm.KosmosFCMService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>

<!-- Background services -->
<service
    android:name=".services.TranscriptionWorkerService"
    android:exported="false" />

<service
    android:name=".services.ActionDetectionWorkerService"
    android:exported="false" />

<!-- File Provider for voice message sharing -->
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>

<!-- Firebase messaging default settings -->
<meta-data
    android:name="com.google.firebase.messaging.default_notification_icon"
    android:resource="@drawable/ic_notification" />
<meta-data
    android:name="com.google.firebase.messaging.default_notification_color"
    android:resource="@color/notification_color" />
<meta-data
    android:name="com.google.firebase.messaging.default_notification_channel_id"
    android:value="@string/default_notification_channel_id" />
```

### **Step 1.2: Create Missing Resource Files**

```xml
<!-- Create app/src/main/res/xml/file_paths.xml -->
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="voice_recordings" path="voice_recordings/" />
    <files-path name="voice_files" path="voice/" />
    <external-files-path name="external_voice" path="voice/" />
</paths>
```

```xml
<!-- Add to app/src/main/res/values/strings.xml -->
<string name="default_notification_channel_id">kosmos_messages</string>
```

```xml
<!-- Add to app/src/main/res/values/colors.xml -->
<color name="notification_color">#FF6200EE</color>
```

### **Step 1.3: Create Missing Service Files**

```kotlin
// Create app/src/main/java/com/example/kosmos/fcm/KosmosFCMService.kt
package com.example.kosmos.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.kosmos.R
import com.example.kosmos.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class KosmosFCMService : FirebaseMessagingService() {

    @Inject
    lateinit var userRepository: UserRepository

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Handle FCM data messages
        remoteMessage.data.isNotEmpty().let {
            Log.d("FCM", "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Handle FCM notification messages
        remoteMessage.notification?.let { notification ->
            Log.d("FCM", "Message Notification Body: ${notification.body}")
            showNotification(
                title = notification.title ?: "Kosmos",
                body = notification.body ?: "New message"
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")

        // Send token to server
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
                    userRepository.updateFcmToken(userId, token)
                    Log.d("FCM", "Token updated for user: $userId")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Failed to update token", e)
            }
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        // Handle different message types
        when (data["type"]) {
            "new_message" -> {
                val chatRoomId = data["chatRoomId"]
                val senderName = data["senderName"]
                val messageContent = data["content"]

                showNotification(
                    title = senderName ?: "New Message",
                    body = messageContent ?: "You have a new message"
                )
            }
            "voice_transcription_complete" -> {
                showNotification(
                    title = "Voice Message Ready",
                    body = "Your voice message has been transcribed"
                )
            }
        }
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "kosmos_messages",
                "Kosmos Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new messages in Kosmos"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "kosmos_messages")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(getColor(R.color.notification_color))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
```

```kotlin
// Create app/src/main/java/com/example/kosmos/services/TranscriptionWorkerService.kt
package com.example.kosmos.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.kosmos.TranscriptionService
import com.example.kosmos.VoiceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TranscriptionWorkerService : Service() {

    @Inject
    lateinit var transcriptionService: TranscriptionService

    @Inject
    lateinit var voiceRepository: VoiceRepository

    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TranscriptionService", "Service started")

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                processPendingTranscriptions()
            } catch (e: Exception) {
                Log.e("TranscriptionService", "Error processing transcriptions", e)
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        Log.d("TranscriptionService", "Service destroyed")
    }

    private suspend fun processPendingTranscriptions() {
        try {
            val pendingVoiceMessages = voiceRepository.getPendingTranscriptions()
            Log.d("TranscriptionService", "Found ${pendingVoiceMessages.size} pending transcriptions")

            pendingVoiceMessages.forEach { voiceMessage ->
                if (voiceMessage.audioUrl.isNotEmpty()) {
                    Log.d("TranscriptionService", "Transcribing voice message: ${voiceMessage.id}")
                    val result = transcriptionService.transcribeVoiceMessage(
                        voiceMessage.id,
                        voiceMessage.audioUrl
                    )

                    if (result.isSuccess) {
                        Log.d("TranscriptionService", "Transcription successful: ${result.getOrNull()}")
                    } else {
                        Log.e("TranscriptionService", "Transcription failed", result.exceptionOrNull())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TranscriptionService", "Failed to process transcriptions", e)
        }
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, TranscriptionWorkerService::class.java)
            context.startService(intent)
        }
    }
}
```

```kotlin
// Create app/src/main/java/com/example/kosmos/services/ActionDetectionWorkerService.kt
package com.example.kosmos.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.kosmos.ActionDetectionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActionDetectionWorkerService : Service() {

    @Inject
    lateinit var actionDetectionService: ActionDetectionService

    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ActionDetectionService", "Service started")

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                processUnprocessedActions()
            } catch (e: Exception) {
                Log.e("ActionDetectionService", "Error processing actions", e)
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        Log.d("ActionDetectionService", "Service destroyed")
    }

    private suspend fun processUnprocessedActions() {
        try {
            val unprocessedActions = actionDetectionService.processUnprocessedActions()
            Log.d("ActionDetectionService", "Found ${unprocessedActions.size} unprocessed actions")

            // Here you could implement logic to create tasks from action items
            // or send notifications to relevant users
            unprocessedActions.forEach { actionItem ->
                Log.d("ActionDetectionService", "Processing action: ${actionItem.text}")
                // TODO: Implement action processing logic
            }
        } catch (e: Exception) {
            Log.e("ActionDetectionService", "Failed to process actions", e)
        }
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, ActionDetectionWorkerService::class.java)
            context.startService(intent)
        }
    }
}
```

### **Step 1.4: Add Missing Icons**

```xml
<!-- Create app/src/main/res/drawable/ic_notification.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorOnPrimary">
  <path
      android:fillColor="@android:color/white"
      android:pathData="M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z"/>
</vector>
```

---

## 🔑 **FIX #2: ADD API KEY CONFIGURATION**

### **Problem**: Google Cloud API key empty, transcription fails
### **Impact**: Voice message transcription always fails
### **Time**: 5 minutes

### **Step 2.1: Update build.gradle.kts**

```kotlin
// app/build.gradle.kts - Replace existing buildTypes section
buildTypes {
    debug {
        // Add debug-specific build config fields
        buildConfigField("String", "GOOGLE_CLOUD_API_KEY", "\"YOUR_DEV_API_KEY_HERE\"") // Replace with actual key
        buildConfigField("boolean", "ENABLE_LOGGING", "true")
    }

    release {
        isMinifyEnabled = false
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        // Add production API keys here
        buildConfigField("String", "GOOGLE_CLOUD_API_KEY", "\"YOUR_PROD_API_KEY_HERE\"") // Replace with actual key
        buildConfigField("boolean", "ENABLE_LOGGING", "false")

        // Signing config for release
        //signingConfig signingConfigs.debug // Replace with actual signing config
    }
}
```

### **Step 2.2: Fix TranscriptionService API Call**

```kotlin
// Update Services.kt - TranscriptionService.transcribeVoiceMessage method
suspend fun transcribeVoiceMessage(voiceMessageId: String, audioFilePath: String): Result<String> {
    return withContext(Dispatchers.IO) {
        try {
            // Validate API key first
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Google Cloud API key not configured. Please add your API key to build.gradle.kts"))
            }

            // Mark as transcribing
            val voiceMessage = voiceMessageDao.getVoiceMessageById(voiceMessageId)
                ?: return@withContext Result.failure(Exception("Voice message not found"))

            voiceMessageDao.updateVoiceMessage(voiceMessage.copy(isTranscribing = true))

            // Read and encode audio file
            val audioFile = File(audioFilePath)
            if (!audioFile.exists()) {
                return@withContext Result.failure(Exception("Audio file not found: $audioFilePath"))
            }

            val audioBytes = audioFile.readBytes()
            val encodedAudio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            // Prepare request
            val request = SpeechRecognitionRequest(
                config = RecognitionConfig(
                    encoding = "OGG_OPUS", // Changed from WEBM_OPUS
                    sampleRateHertz = 48000,
                    languageCode = "en-US",
                    enableAutomaticPunctuation = true
                ),
                audio = RecognitionAudio(content = encodedAudio)
            )

            // Call API with proper authorization
            val response = speechToTextService.recognizeSpeech(
                authorization = "Bearer $apiKey", // or use "X-Goog-Api-Key: $apiKey"
                request = request
            )

            if (response.isSuccessful) {
                val results = response.body()?.results
                if (!results.isNullOrEmpty() && results[0].alternatives.isNotEmpty()) {
                    val transcription = results[0].alternatives[0].transcript
                    val confidence = results[0].alternatives[0].confidence

                    // Update voice message with transcription
                    voiceMessageDao.updateVoiceMessage(
                        voiceMessage.copy(
                            transcription = transcription,
                            transcriptionConfidence = confidence,
                            isTranscribing = false,
                            transcriptionError = null
                        )
                    )

                    Result.success(transcription)
                } else {
                    voiceMessageDao.updateVoiceMessage(
                        voiceMessage.copy(
                            transcription = "",
                            transcriptionError = "No speech detected",
                            isTranscribing = false
                        )
                    )
                    Result.failure(Exception("No speech detected"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val error = "API Error: ${response.code()} - $errorBody"

                voiceMessageDao.updateVoiceMessage(
                    voiceMessage.copy(
                        transcriptionError = error,
                        isTranscribing = false
                    )
                )
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            // Update with error
            voiceMessageDao.getVoiceMessageById(voiceMessageId)?.let { voiceMessage ->
                voiceMessageDao.updateVoiceMessage(
                    voiceMessage.copy(
                        transcriptionError = e.message ?: "Transcription failed",
                        isTranscribing = false
                    )
                )
            }
            Result.failure(e)
        }
    }
}
```

### **Step 2.3: Get Google Cloud API Key**

**Quick Setup Instructions:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create new project or select existing project
3. Enable **Cloud Speech-to-Text API**
4. Go to **Credentials** → **Create Credentials** → **API Key**
5. **Restrict the key** to Speech-to-Text API only (Security → API restrictions)
6. Copy the key and replace `YOUR_DEV_API_KEY_HERE` in build.gradle.kts

---

## 🎤 **FIX #3: INTEGRATE VOICE RECORDING**

### **Problem**: Voice recording UI exists but not connected to actual recording
### **Impact**: Voice message button does nothing
### **Time**: 15 minutes

### **Step 3.1: Update ChatViewModel to Use VoiceRecordingHelper**

```kotlin
// Update Chat.kt - Add to ChatViewModel class
class ChatViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val voiceRepository: VoiceRepository,
    @ApplicationContext private val context: Context // Add this injection
) : ViewModel() {

    // Add voice recording helper
    private var voiceRecordingHelper: VoiceRecordingHelper? = null
    private var currentRecordingJob: Job? = null

    init {
        voiceRecordingHelper = VoiceRecordingHelper(context)
    }

    // Replace existing startVoiceRecording method
    fun startVoiceRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true, error = null)

        currentRecordingJob = viewModelScope.launch {
            try {
                voiceRecordingHelper?.startRecording()?.collect { state ->
                    when (state) {
                        is VoiceRecordingState.Recording -> {
                            Log.d("ChatViewModel", "Recording started: ${state.outputFile.absolutePath}")
                        }
                        is VoiceRecordingState.Completed -> {
                            handleVoiceRecordingCompleted(state.audioFile)
                        }
                        is VoiceRecordingState.Error -> {
                            _uiState.value = _uiState.value.copy(
                                error = "Voice recording failed: ${state.message}",
                                isRecording = false
                            )
                            Log.e("ChatViewModel", "Recording error: ${state.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start recording: ${e.message}",
                    isRecording = false
                )
                Log.e("ChatViewModel", "Failed to start recording", e)
            }
        }
    }

    // Replace existing stopVoiceRecording method
    fun stopVoiceRecording() {
        currentRecordingJob?.cancel()

        val result = voiceRecordingHelper?.stopRecording()
        when (result) {
            is VoiceRecordingState.Completed -> {
                handleVoiceRecordingCompleted(result.audioFile)
            }
            is VoiceRecordingState.Error -> {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to stop recording: ${result.message}",
                    isRecording = false
                )
            }
            else -> {
                _uiState.value = _uiState.value.copy(isRecording = false)
            }
        }
    }

    private fun handleVoiceRecordingCompleted(audioFile: File) {
        _uiState.value = _uiState.value.copy(isRecording = false)

        if (!audioFile.exists() || audioFile.length() == 0L) {
            _uiState.value = _uiState.value.copy(error = "Recording failed: Empty or missing audio file")
            return
        }

        Log.d("ChatViewModel", "Voice recording completed: ${audioFile.absolutePath}, size: ${audioFile.length()}")

        // Calculate duration (approximation)
        val estimatedDuration = (audioFile.length() / 1000).coerceAtLeast(1000) // Rough estimate

        // Send voice message
        sendVoiceMessage(audioFile, estimatedDuration)
    }

    // Keep existing sendVoiceMessage method but add logging
    fun sendVoiceMessage(audioFile: File, duration: Long) {
        if (currentUser == null) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSendingVoice = true)
                Log.d("ChatViewModel", "Sending voice message: ${audioFile.absolutePath}")

                val voiceMessageId = UUID.randomUUID().toString()
                val messageId = UUID.randomUUID().toString()

                val voiceMessage = VoiceMessage(
                    id = voiceMessageId,
                    messageId = messageId,
                    duration = duration,
                    isTranscribing = true
                )

                // Upload voice message
                val uploadResult = voiceRepository.uploadVoiceMessage(audioFile, voiceMessage)
                uploadResult.fold(
                    onSuccess = { uploadedVoiceMessageId ->
                        Log.d("ChatViewModel", "Voice message uploaded: $uploadedVoiceMessageId")

                        // Create text message with voice attachment
                        val message = Message(
                            id = messageId,
                            chatRoomId = currentChatRoomId,
                            senderId = currentUser.uid,
                            senderName = currentUser.displayName ?: "Unknown",
                            senderPhotoUrl = currentUser.photoUrl?.toString(),
                            content = "🎤 Voice message",
                            timestamp = System.currentTimeMillis(),
                            type = MessageType.VOICE,
                            voiceMessageId = uploadedVoiceMessageId
                        )

                        chatRepository.sendMessage(message)

                        // Start background transcription
                        TranscriptionWorkerService.startService(context)

                        Log.d("ChatViewModel", "Voice message sent successfully")
                    },
                    onFailure = { exception ->
                        Log.e("ChatViewModel", "Failed to upload voice message", exception)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to send voice message: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending voice message", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send voice message: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isSendingVoice = false)

                // Clean up temporary file
                try {
                    audioFile.delete()
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Failed to delete temporary audio file", e)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecordingHelper?.cancelRecording()
        currentRecordingJob?.cancel()
    }
}
```

### **Step 3.2: Add Required Dependency Injection**

```kotlin
// Update Module.kt - Add Context injection to RepositoryModule
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // ... existing providers

    @Provides
    @Singleton
    fun provideChatRepository(
        chatRoomDao: ChatRoomDao,
        messageDao: MessageDao,
        firestore: FirebaseFirestore
    ): ChatRepository = ChatRepository(chatRoomDao, messageDao, firestore)

    // Add Context parameter to ChatViewModel dependencies if needed via ViewModels
}

// Or add to ServiceModule
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    // ... existing providers

    @Provides
    @Singleton
    fun provideVoiceRecordingHelper(@ApplicationContext context: Context): VoiceRecordingHelper {
        return VoiceRecordingHelper(context)
    }
}
```

---

## 🔐 **FIX #4: ADD PERMISSION HANDLING**

### **Problem**: No runtime permission requests for microphone
### **Impact**: Voice recording fails silently on Android 6+
### **Time**: 10 minutes

### **Step 4.1: Update MessageInput Composable**

```kotlin
// Update Chat.kt - Replace MessageInput composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun MessageInput(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onStartVoiceRecording: () -> Unit,
    onStopVoiceRecording: () -> Unit,
    isRecording: Boolean,
    isSendingVoice: Boolean,
    modifier: Modifier = Modifier
) {
    // Add permission state
    val microphonePermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )

    var showPermissionRationale by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageTextChange,
            placeholder = { Text("Type a message...") },
            modifier = Modifier.weight(1f),
            maxLines = 4,
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Voice/Send button with permission handling
        if (messageText.isBlank()) {
            FloatingActionButton(
                onClick = {
                    when {
                        microphonePermissionState.status.isGranted -> {
                            if (isRecording) onStopVoiceRecording() else onStartVoiceRecording()
                        }
                        microphonePermissionState.status.shouldShowRationale -> {
                            showPermissionRationale = true
                        }
                        else -> {
                            microphonePermissionState.launchPermissionRequest()
                        }
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = if (isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ) {
                when {
                    isSendingVoice -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    !microphonePermissionState.status.isGranted -> {
                        Icon(
                            Icons.Default.MicOff,
                            contentDescription = "Grant microphone permission"
                        )
                    }
                    else -> {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop recording" else "Record voice message"
                        )
                    }
                }
            }
        } else {
            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send message")
            }
        }
    }

    // Permission rationale dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Microphone Permission Required") },
            text = {
                Text("Kosmos needs access to your microphone to record voice messages. Please grant permission to continue.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionRationale = false
                        microphonePermissionState.launchPermissionRequest()
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionRationale = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
```

### **Step 4.2: Add Permission Import**

```kotlin
// Add to Chat.kt imports
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
```

---

## 🧪 **FIX #5: TEST & VALIDATE**

### **Problem**: Need to verify all fixes work together
### **Impact**: Ensure app is functional before production
### **Time**: 20 minutes

### **Step 5.1: Create Testing Checklist**

```kotlin
// Create app/src/main/java/com/example/kosmos/testing/TestingHelper.kt
package com.example.kosmos.testing

import android.content.Context
import android.util.Log
import com.example.kosmos.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestingHelper @Inject constructor(
    private val context: Context
) {

    fun validateConfiguration(): ConfigurationStatus {
        val issues = mutableListOf<String>()

        // Check API key
        if (BuildConfig.GOOGLE_CLOUD_API_KEY.isBlank()) {
            issues.add("Google Cloud API key not configured")
        }

        // Check permissions in manifest
        val packageManager = context.packageManager
        try {
            val packageInfo = packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

            if (!permissions.contains(android.Manifest.permission.RECORD_AUDIO)) {
                issues.add("RECORD_AUDIO permission not declared in manifest")
            }
            if (!permissions.contains(android.Manifest.permission.INTERNET)) {
                issues.add("INTERNET permission not declared in manifest")
            }
        } catch (e: Exception) {
            issues.add("Failed to check manifest permissions: ${e.message}")
        }

        // Check if voice recording directory exists
        val voiceDir = java.io.File(context.cacheDir, "voice_recordings")
        if (!voiceDir.exists()) {
            val created = voiceDir.mkdirs()
            if (!created) {
                issues.add("Failed to create voice recordings directory")
            }
        }

        return ConfigurationStatus(
            isValid = issues.isEmpty(),
            issues = issues
        )
    }

    fun logSystemInfo() {
        Log.d("TestingHelper", "=== KOSMOS CONFIGURATION ===")
        Log.d("TestingHelper", "API Key configured: ${BuildConfig.GOOGLE_CLOUD_API_KEY.isNotBlank()}")
        Log.d("TestingHelper", "Debug mode: ${BuildConfig.DEBUG}")
        Log.d("TestingHelper", "Package name: ${context.packageName}")
        Log.d("TestingHelper", "Cache dir: ${context.cacheDir}")
        Log.d("TestingHelper", "Files dir: ${context.filesDir}")
        Log.d("TestingHelper", "============================")
    }
}

data class ConfigurationStatus(
    val isValid: Boolean,
    val issues: List<String>
)
```

### **Step 5.2: Add Debug Logging to MainActivity**

```kotlin
// Update MainActivity.kt - Add validation on startup
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var testingHelper: TestingHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()
        enableEdgeToEdge()

        // Add configuration validation in debug builds
        if (BuildConfig.DEBUG) {
            testingHelper.logSystemInfo()
            val status = testingHelper.validateConfiguration()
            if (!status.isValid) {
                Log.w("MainActivity", "Configuration issues found:")
                status.issues.forEach { issue ->
                    Log.w("MainActivity", "  - $issue")
                }
            } else {
                Log.i("MainActivity", "All configurations valid!")
            }
        }

        setContent {
            KosmosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KosmosApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
```

### **Step 5.3: Manual Testing Procedure**

```markdown
# Manual Testing Checklist

## Before Testing
- [ ] Replace YOUR_DEV_API_KEY_HERE with actual Google Cloud API key
- [ ] Build and install on physical device (emulator may have audio issues)
- [ ] Enable logcat filtering for tags: "ChatViewModel", "FCM", "TranscriptionService"

## Test 1: Basic Functionality
- [ ] App launches without crashes
- [ ] Can create account / login
- [ ] Can create new chat room
- [ ] Can send text messages
- [ ] Messages appear in real-time

## Test 2: Voice Recording
- [ ] Microphone permission is requested on first voice recording attempt
- [ ] Voice recording button changes color when recording
- [ ] Can record voice message (at least 3 seconds)
- [ ] Voice message appears in chat with "🎤 Voice message" text
- [ ] Check logcat for "Recording started" and "Voice message sent successfully"

## Test 3: Background Services
- [ ] FCM service starts without errors
- [ ] TranscriptionWorkerService starts after sending voice message
- [ ] Check logcat for "Service started" messages
- [ ] Notification channel created (check Settings > Apps > Kosmos > Notifications)

## Test 4: File Operations
- [ ] Voice recordings saved to cache directory
- [ ] Temporary files cleaned up after sending
- [ ] No permission errors in logcat

## Expected Issues (Normal)
- [ ] Transcription may fail if API key is test key or quota exceeded
- [ ] Voice playback not implemented yet (placeholder UI)
- [ ] Firebase Storage upload may be slow on poor connections

## Red Flags (Stop and Fix)
- [ ] App crashes on voice recording start
- [ ] Permission denied errors for microphone
- [ ] "Service not found" errors in logcat
- [ ] Firebase authentication failures
```

### **Step 5.4: Automated Build Validation**

```kotlin
// Add to app/build.gradle.kts
android {
    // ... existing configuration

    buildTypes {
        debug {
            buildConfigField("String", "GOOGLE_CLOUD_API_KEY", "\"${project.findProperty("GOOGLE_CLOUD_API_KEY") ?: ""}\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")

            // Validate API key is provided
            if (project.findProperty("GOOGLE_CLOUD_API_KEY") == null) {
                println("WARNING: GOOGLE_CLOUD_API_KEY not provided. Voice transcription will fail.")
                println("Set it in gradle.properties or pass as -PGOOGLE_CLOUD_API_KEY=your_key")
            }
        }
    }
}

// Add to gradle.properties (create if doesn't exist):
# gradle.properties
GOOGLE_CLOUD_API_KEY=your_actual_api_key_here
```

---

## ✅ **VERIFICATION STEPS**

### **Quick Smoke Test (5 minutes):**
1. Clean build: `./gradlew clean assembleDebug`
2. Install on device: `./gradlew installDebug`
3. Open app, create account
4. Create new chat room
5. Tap microphone button
6. Grant permission when prompted
7. Record 3-second voice message
8. Check voice message appears in chat
9. Check logcat for success messages

### **Success Indicators:**
- ✅ No crashes during voice recording
- ✅ Microphone permission requested and granted
- ✅ Voice message appears in chat UI
- ✅ Background services start without errors
- ✅ Configuration validation passes

### **Common Issues & Solutions:**

| Issue | Solution |
|-------|----------|
| "API key not configured" error | Add actual Google Cloud API key to build.gradle.kts |
| Permission denied for microphone | Check AndroidManifest.xml has RECORD_AUDIO permission |
| "Service not found" errors | Verify services are uncommented in AndroidManifest.xml |
| Voice recording fails | Test on physical device, not emulator |
| Firebase errors | Verify google-services.json is present and valid |

---

This completes the immediate fixes needed to make the Kosmos app functional. These changes will enable:

1. ✅ **Working voice messages** with recording and upload
2. ✅ **Background transcription** service
3. ✅ **Push notifications** via FCM
4. ✅ **Proper permission handling** for Android 6+
5. ✅ **File sharing** between app components
6. ✅ **Comprehensive testing** and validation

The app should now be fully functional for its core features on real devices.