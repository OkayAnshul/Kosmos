// Add to AndroidManifest.xml - Uncomment these services:

<!--FCM Service for push notifications-->
<service
    android:name=".fcm.KosmosFCMService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>

<!-- Background services for transcription and action detection -->
<service
    android:name=".services.TranscriptionWorkerService"
    android:exported="false" />

<service
    android:name=".services.ActionDetectionWorkerService"
    android:exported="false" />

<!-- File provider for sharing voice messages -->
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>

<!-- Firebase messaging settings -->
<meta-data
    android:name="com.google.firebase.messaging.default_notification_icon"
    android:resource="@drawable/ic_notification" />
<meta-data
    android:name="com.google.firebase.messaging.default_notification_color"
    android:resource="@color/notification_color" />

// Create res/xml/file_paths.xml:
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="voice_recordings" path="voice_recordings/" />
    <files-path name="voice_files" path="voice/" />
</paths>

// Create missing services:

// fcm/KosmosFCMService.kt
@AndroidEntryPoint
class KosmosFCMService : FirebaseMessagingService() {

    @Inject
    lateinit var userRepository: UserRepository

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle FCM messages
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Kosmos",
                body = notification.body ?: "New message"
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Update FCM token
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
                    userRepository.updateFcmToken(userId, token)
                }
            } catch (e: Exception) {
                Log.e("FCM", "Failed to update token", e)
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
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "kosmos_messages")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

// services/TranscriptionWorkerService.kt
@AndroidEntryPoint
class TranscriptionWorkerService : Service() {

    @Inject
    lateinit var transcriptionService: TranscriptionService

    @Inject
    lateinit var voiceRepository: VoiceRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CoroutineScope(Dispatchers.IO).launch {
            processPendingTranscriptions()
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private suspend fun processPendingTranscriptions() {
        try {
            val pendingVoiceMessages = voiceRepository.getPendingTranscriptions()
            pendingVoiceMessages.forEach { voiceMessage ->
                transcriptionService.transcribeVoiceMessage(voiceMessage.id, voiceMessage.audioUrl)
            }
        } catch (e: Exception) {
            Log.e("TranscriptionService", "Failed to process transcriptions", e)
        }
    }
}