# 🚀 Kosmos Supabase Migration Plan

## 📋 **MIGRATION OVERVIEW**

**Strategy: Hybrid Firebase Auth + Supabase Backend**
- Keep Firebase Authentication (working well)
- Migrate Storage from Firebase to Supabase
- Migrate Database from Firestore to PostgreSQL
- Add Supabase Real-time subscriptions
- Maintain Room for offline caching

## 🎯 **PHASE 1: IMMEDIATE FIXES (Required for Basic Functionality)**

### Fix 1: Enable Voice Recording
**Problem**: Voice recording UI exists but not connected
**Files to modify**: `Chat.kt`, `Services.kt`, `AndroidManifest.xml`

### Fix 2: Configure API Keys
**Problem**: Google Cloud API key empty, transcription fails
**Files to modify**: `app/build.gradle.kts`

### Fix 3: Enable Background Services
**Problem**: FCM, transcription services commented out
**Files to modify**: `AndroidManifest.xml`, create missing service files

### Fix 4: Add Permission Handling
**Problem**: Runtime permissions not requested
**Files to modify**: `Chat.kt`, create permission helper

## 🗄️ **PHASE 2: SUPABASE SETUP**

### Step 1: Supabase Project Setup
```bash
# 1. Create Supabase project at supabase.com
# 2. Get project URL and anon key
# 3. Enable Row Level Security on all tables
```

### Step 2: Database Schema Migration
```sql
-- PostgreSQL schema for Kosmos
-- Replace Firestore collections with relational tables

-- Users table (matches existing User model)
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  firebase_uid TEXT UNIQUE NOT NULL, -- Link to Firebase Auth
  email TEXT NOT NULL,
  display_name TEXT NOT NULL,
  photo_url TEXT,
  is_online BOOLEAN DEFAULT false,
  last_seen TIMESTAMP DEFAULT NOW(),
  fcm_token TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Chat rooms table
CREATE TABLE chat_rooms (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name TEXT NOT NULL,
  description TEXT DEFAULT '',
  image_url TEXT,
  created_by UUID REFERENCES users(id),
  created_at TIMESTAMP DEFAULT NOW(),
  last_message_timestamp TIMESTAMP DEFAULT NOW(),
  is_task_board_enabled BOOLEAN DEFAULT true
);

-- Chat room participants (many-to-many)
CREATE TABLE chat_participants (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  chat_room_id UUID REFERENCES chat_rooms(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  joined_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(chat_room_id, user_id)
);

-- Messages table
CREATE TABLE messages (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  chat_room_id UUID REFERENCES chat_rooms(id) ON DELETE CASCADE,
  sender_id UUID REFERENCES users(id),
  content TEXT NOT NULL,
  message_type TEXT CHECK (message_type IN ('TEXT', 'VOICE', 'IMAGE', 'FILE', 'SYSTEM', 'TASK_CREATED')),
  voice_message_id UUID,
  reply_to_message_id UUID REFERENCES messages(id),
  is_edited BOOLEAN DEFAULT false,
  edited_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Voice messages table
CREATE TABLE voice_messages (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
  storage_path TEXT NOT NULL, -- Supabase Storage path
  duration_ms BIGINT DEFAULT 0,
  transcription TEXT,
  transcription_confidence REAL DEFAULT 0,
  is_transcribing BOOLEAN DEFAULT false,
  transcription_error TEXT,
  waveform REAL[],
  created_at TIMESTAMP DEFAULT NOW()
);

-- Tasks table
CREATE TABLE tasks (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  chat_room_id UUID REFERENCES chat_rooms(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  description TEXT DEFAULT '',
  status TEXT CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED')) DEFAULT 'TODO',
  priority TEXT CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')) DEFAULT 'MEDIUM',
  assigned_to_id UUID REFERENCES users(id),
  created_by_id UUID REFERENCES users(id),
  source_message_id UUID REFERENCES messages(id),
  due_date TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Action items table
CREATE TABLE action_items (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  message_id UUID REFERENCES messages(id),
  voice_message_id UUID REFERENCES voice_messages(id),
  chat_room_id UUID REFERENCES chat_rooms(id),
  action_type TEXT CHECK (action_type IN ('TASK', 'REMINDER', 'MEETING', 'DEADLINE', 'FOLLOW_UP')),
  text TEXT NOT NULL,
  extracted_text TEXT NOT NULL,
  confidence REAL DEFAULT 0,
  is_processed BOOLEAN DEFAULT false,
  task_id UUID REFERENCES tasks(id),
  reminder_time TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Message reactions (many-to-many)
CREATE TABLE message_reactions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  emoji TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(message_id, user_id, emoji)
);

-- Message read receipts
CREATE TABLE message_read_receipts (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  read_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(message_id, user_id)
);

-- Row Level Security Policies
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE voice_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE action_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE message_read_receipts ENABLE ROW LEVEL SECURITY;

-- Example RLS policy for messages (users can only see messages in chats they're part of)
CREATE POLICY "Users can view messages in their chat rooms" ON messages
  FOR SELECT USING (
    chat_room_id IN (
      SELECT chat_room_id FROM chat_participants
      WHERE user_id = auth.uid()::uuid
    )
  );

-- Add indexes for performance
CREATE INDEX idx_messages_chat_room_created ON messages(chat_room_id, created_at DESC);
CREATE INDEX idx_chat_participants_user ON chat_participants(user_id);
CREATE INDEX idx_chat_participants_room ON chat_participants(chat_room_id);
CREATE INDEX idx_tasks_chat_room ON tasks(chat_room_id);
CREATE INDEX idx_voice_messages_message ON voice_messages(message_id);
```

### Step 3: Storage Setup
```sql
-- Supabase Storage buckets
INSERT INTO storage.buckets (id, name, public) VALUES
  ('voice-messages', 'voice-messages', false),
  ('profile-images', 'profile-images', true),
  ('chat-images', 'chat-images', false);

-- Storage policies for voice messages
CREATE POLICY "Users can upload voice messages" ON storage.objects
  FOR INSERT WITH CHECK (
    bucket_id = 'voice-messages' AND
    auth.role() = 'authenticated'
  );

CREATE POLICY "Users can view voice messages in their chats" ON storage.objects
  FOR SELECT USING (
    bucket_id = 'voice-messages' AND
    auth.role() = 'authenticated'
  );
```

## 🔧 **PHASE 3: ANDROID INTEGRATION**

### Step 1: Add Supabase Dependencies
```kotlin
// app/build.gradle.kts
dependencies {
    // Existing dependencies...

    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.0.4")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.0.4")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.0.4")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.0.4")

    // Ktor for HTTP client (required by Supabase)
    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-utils:2.3.7")
}
```

### Step 2: Supabase Configuration
```kotlin
// Module.kt - Add Supabase module
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Storage)
            install(Realtime)
            install(GoTrue)
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseDatabase(client: SupabaseClient): Postgrest {
        return client.postgrest
    }

    @Provides
    @Singleton
    fun provideSupabaseStorage(client: SupabaseClient): Storage {
        return client.storage
    }

    @Provides
    @Singleton
    fun provideSupabaseRealtime(client: SupabaseClient): Realtime {
        return client.realtime
    }
}

// Update build config
buildTypes {
    debug {
        buildConfigField("String", "SUPABASE_URL", "\"https://your-project.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"your-anon-key\"")
    }
    release {
        buildConfigField("String", "SUPABASE_URL", "\"https://your-project.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"your-anon-key\"")
    }
}
```

### Step 3: Data Models for Supabase
```kotlin
// models/SupabaseModels.kt
@Serializable
data class SupabaseUser(
    val id: String,
    @SerialName("firebase_uid") val firebaseUid: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("last_seen") val lastSeen: String,
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class SupabaseChatRoom(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_message_timestamp") val lastMessageTimestamp: String,
    @SerialName("is_task_board_enabled") val isTaskBoardEnabled: Boolean = true,
    val participants: List<SupabaseUser> = emptyList()
)

@Serializable
data class SupabaseMessage(
    val id: String,
    @SerialName("chat_room_id") val chatRoomId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("message_type") val messageType: String,
    @SerialName("voice_message_id") val voiceMessageId: String? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    val sender: SupabaseUser? = null,
    @SerialName("voice_message") val voiceMessage: SupabaseVoiceMessage? = null
)

@Serializable
data class SupabaseVoiceMessage(
    val id: String,
    @SerialName("message_id") val messageId: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("duration_ms") val durationMs: Long = 0,
    val transcription: String? = null,
    @SerialName("transcription_confidence") val transcriptionConfidence: Float = 0f,
    @SerialName("is_transcribing") val isTranscribing: Boolean = false,
    @SerialName("transcription_error") val transcriptionError: String? = null,
    val waveform: List<Float> = emptyList(),
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class SupabaseTask(
    val id: String,
    @SerialName("chat_room_id") val chatRoomId: String,
    val title: String,
    val description: String = "",
    val status: String = "TODO",
    val priority: String = "MEDIUM",
    @SerialName("assigned_to_id") val assignedToId: String? = null,
    @SerialName("created_by_id") val createdById: String,
    @SerialName("source_message_id") val sourceMessageId: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("assigned_to") val assignedTo: SupabaseUser? = null,
    @SerialName("created_by") val createdBy: SupabaseUser? = null
)
```

## 🔄 **PHASE 4: HYBRID REPOSITORY IMPLEMENTATION**

### Hybrid Repository Pattern
```kotlin
// GlobalRepository.kt - Update existing repositories

@Singleton
class HybridChatRepository @Inject constructor(
    private val chatRoomDao: ChatRoomDao,
    private val messageDao: MessageDao,
    private val firestore: FirebaseFirestore, // Keep for gradual migration
    private val supabase: Postgrest
) {

    // Hybrid approach - read from both sources during migration
    fun getChatRoomsFlow(userId: String): Flow<List<ChatRoom>> = flow {
        try {
            // 1. Emit cached data first
            val localRooms = chatRoomDao.getAllChatRoomsFlow()
            localRooms.collect { cachedRooms ->
                emit(cachedRooms.filter { it.participantIds.contains(userId) })
            }

            // 2. Fetch from Supabase
            val supabaseRooms = supabase.from("chat_rooms")
                .select("""
                    *,
                    chat_participants!inner(user_id),
                    users!chat_rooms_created_by_fkey(display_name, photo_url)
                """.trimIndent()) {
                    ChatParticipants.userId eq userId
                }
                .decodeList<SupabaseChatRoom>()

            // 3. Convert and cache
            val convertedRooms = supabaseRooms.map { it.toDomainModel() }
            chatRoomDao.insertChatRooms(convertedRooms)
            emit(convertedRooms)

        } catch (e: Exception) {
            // Fallback to Firebase during migration
            firestore.collection("chatRooms")
                .whereArrayContains("participantIds", userId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .snapshots()
                .collect { snapshot ->
                    val rooms = snapshot.documents.mapNotNull { it.toObject(ChatRoom::class.java) }
                    chatRoomDao.insertChatRooms(rooms)
                    emit(rooms)
                }
        }
    }

    suspend fun sendMessage(message: Message): Result<String> {
        return try {
            val messageId = message.id.ifEmpty { UUID.randomUUID().toString() }
            val messageWithId = message.copy(id = messageId)

            // 1. Save to local first (optimistic update)
            messageDao.insertMessage(messageWithId)

            // 2. Send to Supabase
            val supabaseMessage = messageWithId.toSupabaseModel()
            supabase.from("messages").insert(supabaseMessage)

            // 3. Update chat room timestamp
            supabase.from("chat_rooms")
                .update({
                    set("last_message_timestamp", messageWithId.timestamp)
                }) {
                    eq("id", message.chatRoomId)
                }

            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class HybridVoiceRepository @Inject constructor(
    private val voiceMessageDao: VoiceMessageDao,
    private val supabaseStorage: Storage,
    private val supabase: Postgrest,
    @ApplicationContext private val context: Context
) {

    suspend fun uploadVoiceMessage(audioFile: File, voiceMessage: VoiceMessage): Result<String> {
        return try {
            val voiceMessageId = voiceMessage.id.ifEmpty { UUID.randomUUID().toString() }
            val fileName = "${voiceMessageId}.m4a"
            val storagePath = "voice-messages/$fileName"

            // 1. Upload to Supabase Storage
            supabaseStorage.from("voice-messages")
                .upload(fileName, audioFile.readBytes()) {
                    upsert = false
                }

            // 2. Save metadata to database
            val supabaseVoiceMessage = voiceMessage.copy(
                id = voiceMessageId,
                audioUrl = storagePath // Store path instead of full URL
            ).toSupabaseModel()

            supabase.from("voice_messages").insert(supabaseVoiceMessage)

            // 3. Cache locally
            voiceMessageDao.insertVoiceMessage(voiceMessage.copy(id = voiceMessageId))

            Result.success(voiceMessageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVoiceMessageUrl(storagePath: String): String {
        return supabaseStorage.from("voice-messages").createSignedUrl(
            path = storagePath.removePrefix("voice-messages/"),
            expiresIn = 3600 // 1 hour
        )
    }
}
```

### Real-time Subscriptions
```kotlin
// RealtimeService.kt
@Singleton
class SupabaseRealtimeService @Inject constructor(
    private val realtime: Realtime,
    private val messageDao: MessageDao,
    private val chatRoomDao: ChatRoomDao
) {

    private val subscriptions = mutableMapOf<String, RealtimeChannel>()

    suspend fun subscribeToChat(chatRoomId: String): Flow<Message> = callbackFlow {
        val channel = realtime.createChannel("chat_$chatRoomId")

        // Subscribe to new messages
        val messageSubscription = channel.postgresChangeFlow<SupabaseMessage>(schema = "public") {
            table = "messages"
            filter = "chat_room_id=eq.$chatRoomId"
            event = PostgresAction.INSERT
        }.onEach { change ->
            when (change) {
                is PostgresAction.Insert -> {
                    val message = change.record.toDomainModel()
                    messageDao.insertMessage(message)
                    trySend(message)
                }
            }
        }

        channel.subscribe()
        subscriptions[chatRoomId] = channel

        awaitClose {
            channel.unsubscribe()
            subscriptions.remove(chatRoomId)
        }
    }

    suspend fun subscribeToUserPresence(userId: String) {
        val channel = realtime.createChannel("presence")

        channel.presence {
            onJoin { key, current, new ->
                // Update user online status
            }
            onLeave { key, current, leftPresences ->
                // Update user offline status
            }
        }

        channel.subscribe()
    }

    fun unsubscribeFromChat(chatRoomId: String) {
        subscriptions[chatRoomId]?.unsubscribe()
        subscriptions.remove(chatRoomId)
    }
}
```

## 📱 **PHASE 5: EXTENSION FUNCTIONS FOR MODEL CONVERSION**

```kotlin
// ModelExtensions.kt
fun SupabaseUser.toDomainModel(): User = User(
    id = firebaseUid, // Use Firebase UID as primary key in domain
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    isOnline = isOnline,
    lastSeen = Instant.parse(lastSeen).toEpochMilli(),
    fcmToken = fcmToken,
    createdAt = Instant.parse(createdAt).toEpochMilli()
)

fun User.toSupabaseModel(firebaseUid: String): SupabaseUser = SupabaseUser(
    id = UUID.randomUUID().toString(),
    firebaseUid = firebaseUid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    isOnline = isOnline,
    lastSeen = Instant.ofEpochMilli(lastSeen).toString(),
    fcmToken = fcmToken,
    createdAt = Instant.ofEpochMilli(createdAt).toString()
)

fun SupabaseMessage.toDomainModel(): Message = Message(
    id = id,
    chatRoomId = chatRoomId,
    senderId = senderId,
    senderName = sender?.displayName ?: "",
    senderPhotoUrl = sender?.photoUrl,
    content = content,
    timestamp = Instant.parse(createdAt).toEpochMilli(),
    type = MessageType.valueOf(messageType),
    voiceMessageId = voiceMessageId,
    replyToMessageId = replyToMessageId,
    isEdited = isEdited,
    editedAt = editedAt?.let { Instant.parse(it).toEpochMilli() }
)

fun Message.toSupabaseModel(): SupabaseMessage = SupabaseMessage(
    id = id,
    chatRoomId = chatRoomId,
    senderId = senderId,
    content = content,
    messageType = type.name,
    voiceMessageId = voiceMessageId,
    replyToMessageId = replyToMessageId,
    isEdited = isEdited,
    editedAt = editedAt?.let { Instant.ofEpochMilli(it).toString() },
    createdAt = Instant.ofEpochMilli(timestamp).toString()
)

fun SupabaseVoiceMessage.toDomainModel(): VoiceMessage = VoiceMessage(
    id = id,
    messageId = messageId,
    audioUrl = storagePath, // Will be converted to signed URL when needed
    duration = durationMs,
    transcription = transcription,
    transcriptionConfidence = transcriptionConfidence,
    isTranscribing = isTranscribing,
    transcriptionError = transcriptionError,
    waveform = waveform
)

fun VoiceMessage.toSupabaseModel(): SupabaseVoiceMessage = SupabaseVoiceMessage(
    id = id,
    messageId = messageId,
    storagePath = audioUrl,
    durationMs = duration,
    transcription = transcription,
    transcriptionConfidence = transcriptionConfidence,
    isTranscribing = isTranscribing,
    transcriptionError = transcriptionError,
    waveform = waveform,
    createdAt = Instant.now().toString()
)
```

## 🧪 **PHASE 6: MIGRATION STRATEGY**

### Step 1: Dual-Write Phase
```kotlin
// MigrationRepository.kt - Temporary dual-write implementation
@Singleton
class MigrationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val supabase: Postgrest,
    private val preferences: SharedPreferences
) {

    private val isMigrationEnabled: Boolean
        get() = preferences.getBoolean("supabase_migration_enabled", false)

    suspend fun sendMessage(message: Message): Result<String> {
        return if (isMigrationEnabled) {
            // Dual-write: Send to both Firebase and Supabase
            try {
                val firebaseResult = sendToFirebase(message)
                val supabaseResult = sendToSupabase(message)

                if (firebaseResult.isSuccess && supabaseResult.isSuccess) {
                    Result.success(message.id)
                } else {
                    // Log discrepancies but don't fail
                    Result.success(message.id)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            sendToFirebase(message)
        }
    }

    private suspend fun sendToFirebase(message: Message): Result<String> {
        // Existing Firebase implementation
    }

    private suspend fun sendToSupabase(message: Message): Result<String> {
        // New Supabase implementation
    }
}
```

### Step 2: Data Validation
```kotlin
// MigrationValidator.kt
class MigrationValidator @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val supabase: Postgrest
) {

    suspend fun validateDataConsistency(chatRoomId: String): ValidationResult {
        return try {
            // Compare message counts
            val firebaseCount = getFirebaseMessageCount(chatRoomId)
            val supabaseCount = getSupabaseMessageCount(chatRoomId)

            // Compare latest messages
            val firebaseLatest = getLatestFirebaseMessage(chatRoomId)
            val supabaseLatest = getLatestSupabaseMessage(chatRoomId)

            ValidationResult(
                isConsistent = firebaseCount == supabaseCount &&
                              firebaseLatest?.content == supabaseLatest?.content,
                firebaseCount = firebaseCount,
                supabaseCount = supabaseCount,
                discrepancies = findDiscrepancies(chatRoomId)
            )
        } catch (e: Exception) {
            ValidationResult(isConsistent = false, error = e.message)
        }
    }
}

data class ValidationResult(
    val isConsistent: Boolean,
    val firebaseCount: Int = 0,
    val supabaseCount: Int = 0,
    val discrepancies: List<String> = emptyList(),
    val error: String? = null
)
```

## 🚀 **MIGRATION TIMELINE**

### Week 1: Immediate Fixes
- Fix voice recording integration
- Add API keys and enable background services
- Test basic functionality on real devices

### Week 2: Supabase Setup
- Create Supabase project and database schema
- Add Supabase dependencies to Android project
- Implement basic Supabase models and configuration

### Week 3: Hybrid Implementation
- Implement dual-write repositories
- Add Supabase real-time subscriptions
- Create model conversion extensions

### Week 4: Migration & Testing
- Enable dual-write mode
- Run data validation
- Perform comprehensive testing
- Gradually switch reads to Supabase

### Week 5: Cutover
- Switch all operations to Supabase-only
- Remove Firebase dependencies (except Auth)
- Monitor and fix any issues

## 🎯 **BENEFITS OF THIS APPROACH**

### Technical Benefits
- **Better Performance**: PostgreSQL queries vs Firestore limitations
- **Real-time**: Native WebSocket subscriptions vs Firebase listeners
- **Cost Effective**: Supabase pricing vs Firebase pricing
- **Full Control**: Self-hosted option available
- **Better Offline**: More predictable caching with PostgreSQL

### Development Benefits
- **SQL Familiarity**: Easier complex queries
- **Better Dev Tools**: Supabase dashboard vs Firebase console
- **Type Safety**: Generated TypeScript/Kotlin types
- **Migration Path**: Gradual migration without downtime

This plan provides a comprehensive approach to both fixing the immediate issues and implementing a robust Supabase backend while maintaining Firebase Auth for user management.