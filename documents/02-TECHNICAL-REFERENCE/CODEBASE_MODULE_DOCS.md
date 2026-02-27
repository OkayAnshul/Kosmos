# CODEBASE MODULE DOCUMENTATION

**Document Version:** 1.0
**Generated:** 2025-12-23
**Purpose:** Complete technical reference for all code modules, classes, and implementation patterns

---

## Module Architecture Overview

```
/app/src/main/java/com/example/kosmos/
├── core/                    # Core infrastructure (models, database, config, validators)
├── data/                    # Data layer (repositories, data sources, sync, real-time)
├── features/                # Feature modules (auth, chat, tasks, projects, users, profile, smart)
├── shared/                  # Shared UI (design system, components, layouts, theme)
├── MainActivity.kt          # Single activity entry point
├── KosmosApplication.kt     # Application class (Hilt setup)
└── Module.kt                # Dependency injection (all DI modules)
```

**Architecture Pattern:** Clean Architecture + MVVM + Repository
**Module Organization:** Feature-first (vertical slices, not horizontal layers)
**Dependency Flow:** UI → ViewModel → Repository → DataSource → Database/API

---

## CORE LAYER

### Package: `com.example.kosmos.core`

Foundation layer containing domain models, database, configuration, and validators.

---

### core.database

**Package:** `com.example.kosmos.core.database`

#### KosmosDatabase.kt

**Purpose:** Room database definition with all entities and DAOs

```kotlin
@Database(
    entities = [
        User::class,
        ChatRoom::class,
        Message::class,
        VoiceMessage::class,
        Task::class,
        ActionItem::class,
        Project::class,
        ProjectMember::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KosmosDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatRoomDao(): ChatRoomDao
    abstract fun messageDao(): MessageDao
    abstract fun voiceMessageDao(): VoiceMessageDao
    abstract fun taskDao(): TaskDao
    abstract fun actionItemDao(): ActionItemDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectMemberDao(): ProjectMemberDao
}
```

**Configuration:**
- **Database Name:** "kosmos_database"
- **Version:** 2 (incremented for RBAC changes)
- **Migration Strategy:** ⚠️ `fallbackToDestructiveMigration()` (dev only - TODO: proper migrations)

**8 DAOs Provided:**
1. UserDao - User CRUD operations
2. ChatRoomDao - Chat room management
3. MessageDao - Message operations with pagination
4. VoiceMessageDao - Voice message storage
5. TaskDao - Task management with filtering
6. ActionItemDao - AI-detected action items
7. ProjectDao - Project CRUD with metadata
8. ProjectMemberDao - Team membership with RBAC

---

#### Converters.kt

**Purpose:** Type converters for Room database (complex types ↔ primitive types)

**Conversions:**
```kotlin
// List conversions
@TypeConverter fun fromStringList(value: List<String>): String = Gson().toJson(value)
@TypeConverter fun toStringList(value: String): List<String> = Gson().fromJson(value, ...)

// Enum conversions
@TypeConverter fun fromProjectStatus(value: ProjectStatus): String = value.name
@TypeConverter fun toProjectStatus(value: String): ProjectStatus = ProjectStatus.valueOf(value)

// JSONB conversions (for Supabase compatibility)
@TypeConverter fun fromTaskCommentList(value: List<TaskComment>): String
@TypeConverter fun toTaskCommentList(value: String): List<TaskComment>

@TypeConverter fun fromReactionsMap(value: Map<String, List<String>>): String
@TypeConverter fun toReactionsMap(value: String): Map<String, List<String>>
```

**Special Handling:**
- **Timestamps:** Stored as Long (milliseconds since epoch)
- **IDs:** Stored as String (UUID format)
- **Enums:** Stored as String (name)
- **Complex Objects:** Stored as JSON string (Gson serialization)

---

#### dao/*.kt (8 DAO Interfaces)

**Common Pattern:**
```kotlin
@Dao
interface SomeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: Entity): Long

    @Update
    suspend fun update(entity: Entity)

    @Delete
    suspend fun delete(entity: Entity)

    @Query("SELECT * FROM table_name WHERE id = :id")
    suspend fun getById(id: String): Entity?

    @Query("SELECT * FROM table_name")
    fun getAllFlow(): Flow<List<Entity>>
}
```

**Key Methods by DAO:**

**UserDao:**
- `insertUser(user: User)`
- `updateUser(user: User)`
- `getUserById(userId: String): User?`
- `getUserByIdFlow(userId: String): Flow<User?>`
- `searchUsersByUsername(query: String): List<User>`
- `getAllUsers(): List<User>`

**ChatRoomDao:**
- `insertChatRoom(chatRoom: ChatRoom)`
- `getChatRoomById(chatRoomId: String): ChatRoom?`
- `getChatRoomsForProject(projectId: String): Flow<List<ChatRoom>>`
- `updateLastMessage(chatRoomId: String, lastMessage: String, lastMessageTimestamp: Long)`
- `archiveChatRoom(chatRoomId: String, isArchived: Boolean)`
- `pinChatRoom(chatRoomId: String, isPinned: Boolean)`
- `deleteChatRoom(chatRoomId: String)`

**MessageDao:**
- `insertMessage(message: Message)`
- `getMessagesForChatRoomFlow(chatRoomId: String): Flow<List<Message>>`
- `getMessageById(messageId: String): Message?`
- `updateMessage(message: Message)`
- `deleteMessage(messageId: String)`
- `markMessagesAsRead(chatRoomId: String, userId: String)`
- `getMessagesPaginated(chatRoomId: String, limit: Int, offset: Int): List<Message>`

**TaskDao:**
- `insertTask(task: Task)`
- `updateTask(task: Task)`
- `deleteTask(taskId: String)`
- `getTasksForProject(projectId: String): Flow<List<Task>>`
- `getTasksForChatRoom(chatRoomId: String): Flow<List<Task>>`
- `getUserTasks(userId: String): Flow<List<Task>>`
- `getTasksByStatus(projectId: String, status: TaskStatus): Flow<List<Task>>`

**ProjectDao:**
- `insertProject(project: Project)`
- `updateProject(project: Project)`
- `deleteProject(projectId: String)`
- `getProjectById(projectId: String): Project?`
- `getUserProjectsFlow(userId: String): Flow<List<Project>>`
- `updateProjectMetadata(projectId, memberCount, chatCount, taskCount, ...)`

**ProjectMemberDao:**
- `insertMember(member: ProjectMember)`
- `removeMember(projectId: String, userId: String)`
- `updateMemberRole(projectId: String, userId: String, role: ProjectRole)`
- `getProjectMembers(projectId: String): Flow<List<ProjectMember>>`
- `getMemberRole(projectId: String, userId: String): ProjectRole?`
- `getUserMemberships(userId: String): Flow<List<ProjectMember>>`

**VoiceMessageDao:** (Minimal - feature disabled for MVP)
- `insertVoiceMessage(voiceMessage: VoiceMessage)`
- `getVoiceMessageById(messageId: String): VoiceMessage?`

**ActionItemDao:** (Planned for future - AI features)
- `insertActionItem(actionItem: ActionItem)`
- `getActionItemsForMessage(messageId: String): List<ActionItem>`

---

### core.models

**Package:** `com.example.kosmos.core.models`

#### Domain Models (11 Entities)

All models use `@Entity` (Room) and `@Serializable` (kotlinx.serialization for Supabase).

---

#### 1. User.kt

**Purpose:** User profile and authentication data

```kotlin
@Entity(tableName = "users")
@Serializable
data class User(
    @PrimaryKey val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val photoUrl: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val role: String? = null,
    val age: Int? = null,

    // Social links
    val githubUrl: String? = null,
    val twitterUrl: String? = null,
    val linkedinUrl: String? = null,
    val websiteUrl: String? = null,
    val portfolioUrl: String? = null,

    // Status
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,

    val createdAt: Long = System.currentTimeMillis()
)
```

**Fields:** 17 total
**Indexed:** id (PK), email, username
**Unique Constraints:** email, username (handled by Supabase)

---

#### 2. Project.kt

**Purpose:** Project workspace with cached metadata

```kotlin
@Entity(tableName = "projects")
@Serializable
data class Project(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val visibility: ProjectVisibility = ProjectVisibility.PRIVATE,

    // Metadata (cached for performance - updated by DB triggers)
    val memberCount: Int = 0,
    val chatCount: Int = 0,
    val taskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val pendingTaskCount: Int = 0,
    val lastActivityAt: Long? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ProjectStatus { ACTIVE, ARCHIVED, COMPLETED, ON_HOLD }
enum class ProjectVisibility { PRIVATE, INTERNAL, PUBLIC }
```

**Fields:** 14 total (11 base + 6 metadata - some overlap)
**Metadata Optimization:** Cached counts avoid N+1 queries (25x performance improvement)
**Triggers:** Supabase triggers auto-update metadata on related table changes

---

#### 3. ProjectMember.kt

**Purpose:** RBAC membership with role hierarchy

```kotlin
@Entity(
    tableName = "project_members",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(entity = Project::class, parentColumns = ["id"], childColumns = ["projectId"]),
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"])
    ]
)
@Serializable
data class ProjectMember(
    val id: String,
    val projectId: String,
    val userId: String,
    val role: ProjectRole = ProjectRole.MEMBER,
    val joinedAt: Long = System.currentTimeMillis(),
    val invitedBy: String? = null,
    val isActive: Boolean = true,
    val customPermissions: String? = null  // JSON override for granular control
)

enum class ProjectRole(val weight: Int) {
    ADMIN(3),
    MANAGER(2),
    MEMBER(1);

    fun canManage(other: ProjectRole): Boolean = this.weight > other.weight
    fun canAssignTo(other: ProjectRole): Boolean = this.weight >= other.weight
}
```

**Fields:** 9 total
**Role Hierarchy:** ADMIN > MANAGER > MEMBER
**Permissions:** 49 granular permissions defined in Permission enum
**Custom Permissions:** JSON override allows per-member exceptions

---

#### 4. ChatRoom.kt

**Purpose:** Chat channel within projects

```kotlin
@Entity(
    tableName = "chat_rooms",
    foreignKeys = [
        ForeignKey(entity = Project::class, parentColumns = ["id"], childColumns = ["projectId"])
    ]
)
@Serializable
data class ChatRoom(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val description: String? = null,
    val type: ChatRoomType = ChatRoomType.CHANNEL,
    val createdBy: String,

    // Participants (JSON array of user IDs)
    val participantIds: List<String> = emptyList(),

    // Status
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val isTaskBoardEnabled: Boolean = false,

    // Last message (for list preview)
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long? = null,
    val lastMessageSenderId: String? = null,

    val createdAt: Long = System.currentTimeMillis()
)

enum class ChatRoomType {
    GENERAL,      // Default project chat
    DIRECT,       // 1-on-1 chat
    CHANNEL,      // Topic-specific channel
    TASK_DISCUSSION,  // Linked to specific task
    ANNOUNCEMENTS  // Read-only for members
}
```

**Fields:** 14 total
**Special:** participantIds stored as JSON array via Converters

---

#### 5. Message.kt

**Purpose:** Chat message with reactions and threading

```kotlin
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(entity = ChatRoom::class, parentColumns = ["id"], childColumns = ["chatRoomId"]),
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["senderId"])
    ]
)
@Serializable
data class Message(
    @PrimaryKey val id: String,
    val chatRoomId: String,
    val senderId: String,
    val senderName: String,      // Denormalized for performance
    val senderPhotoUrl: String?, // Denormalized for performance
    val content: String,
    val type: MessageType = MessageType.TEXT,

    // Reactions (map of emoji to list of user IDs)
    val reactions: Map<String, List<String>> = emptyMap(),

    // Read receipts
    val readBy: List<String> = emptyList(),

    // Threading
    val replyToMessageId: String? = null,

    // Editing
    val isEdited: Boolean = false,
    val editedAt: Long? = null,

    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class MessageType {
    TEXT,
    VOICE,
    IMAGE,
    FILE,
    SYSTEM,        // System-generated (user joined, etc.)
    TASK_CREATED   // Task creation notification
}
```

**Fields:** 15 total
**Special Features:**
- Reactions stored as Map (emoji → user IDs)
- Read receipts as List<String>
- Threading via replyToMessageId
- Denormalized sender info for performance

**Serialization:** Custom serializer for reactions (Supabase JSONB compatibility)

---

#### 6. Task.kt

**Purpose:** Task with subtasks and time tracking

```kotlin
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(entity = Project::class, parentColumns = ["id"], childColumns = ["projectId"]),
        ForeignKey(entity ChatRoom::class, parentColumns = ["id"], childColumns = ["chatRoomId"])
    ]
)
@Serializable
data class Task(
    @PrimaryKey val id: String,
    val projectId: String,
    val chatRoomId: String? = null,
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,

    // Assignment
    val assignedToId: String? = null,
    val assignedById: String? = null,

    // Subtasks
    val parentTaskId: String? = null,

    // Time tracking
    val estimatedHours: Double? = null,
    val actualHours: Double? = null,
    val dueDate: Long? = null,
    val completedAt: Long? = null,

    // Metadata
    val tags: List<String> = emptyList(),
    val comments: List<TaskComment> = emptyList(),

    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TaskStatus { TODO, IN_PROGRESS, DONE, CANCELLED }
enum class TaskPriority { LOW, MEDIUM, HIGH, URGENT }

@Serializable
data class TaskComment(
    val id: String,
    val userId: String,
    val userName: String,
    val text: String,
    val timestamp: Long
)
```

**Fields:** 21 total
**Features:**
- Subtask support via parentTaskId
- Time tracking (estimated vs actual)
- Tags and comments (JSON)
- Due dates with completion tracking

---

#### 7. VoiceMessage.kt (Disabled for MVP)

**Purpose:** Voice recording metadata

```kotlin
@Entity(
    tableName = "voice_messages",
    foreignKeys = [
        ForeignKey(entity = Message::class, parentColumns = ["id"], childColumns = ["messageId"])
    ]
)
@Serializable
data class VoiceMessage(
    @PrimaryKey val id: String,
    val messageId: String,
    val audioUrl: String,
    val duration: Int, // seconds
    val transcription: String? = null,
    val transcriptionConfidence: Float? = null,
    val waveform: List<Float>? = null,  // Amplitude data for visualization
    val actionItems: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
```

**Integration:** Google Cloud Speech-to-Text API (when re-enabled)

---

#### 8. ActionItem.kt (Future)

**Purpose:** AI-detected actionable items from messages

```kotlin
@Entity(
    tableName = "action_items",
    foreignKeys = [
        ForeignKey(entity = Message::class, parentColumns = ["id"], childColumns = ["messageId"])
    ]
)
@Serializable
data class ActionItem(
    @PrimaryKey val id: String,
    val messageId: String,
    val chatRoomId: String,
    val type: ActionType,
    val text: String,
    val confidence: Float,
    val isProcessed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ActionType {
    TASK,       // "need to", "should", "must"
    REMINDER,   // "remind", "don't forget"
    MEETING,    // "meeting", "call", "schedule"
    DEADLINE,   // "due", "deadline", "by"
    FOLLOW_UP   // "follow up", "check back"
}
```

**Detection:** Regex-based pattern matching in ActionDetectionService

---

#### 9-11. Composite Models

**ProjectWithMembers:**
```kotlin
data class ProjectWithMembers(
    @Embedded val project: Project,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val members: List<ProjectMember>
)
```

**ProjectStats:** (Computed from metadata)
```kotlin
data class ProjectStats(
    val projectId: String,
    val memberCount: Int,
    val chatCount: Int,
    val taskCount: Int,
    val completedTaskCount: Int,
    val pendingTaskCount: Int,
    val unreadChatCount: Int
)
```

**Permission Enum:** (49 granular permissions)
```kotlin
enum class Permission {
    // Project (8)
    VIEW_PROJECT, EDIT_PROJECT, DELETE_PROJECT, ARCHIVE_PROJECT,
    CHANGE_PROJECT_SETTINGS, CHANGE_PROJECT_VISIBILITY,
    INVITE_MEMBERS, REMOVE_MEMBERS,

    // Member Management (6)
    VIEW_MEMBERS, CHANGE_MEMBER_ROLES, VIEW_MEMBER_PERMISSIONS,
    REMOVE_ANY_MEMBER, REMOVE_LOWER_ROLE_MEMBERS, LEAVE_PROJECT,

    // Chat (12)
    VIEW_CHATS, CREATE_CHAT, DELETE_ANY_CHAT, DELETE_OWN_CHAT,
    ARCHIVE_CHAT, PIN_CHAT, SEND_MESSAGE, EDIT_OWN_MESSAGE,
    DELETE_ANY_MESSAGE, DELETE_OWN_MESSAGE, REACT_TO_MESSAGE,
    VIEW_MESSAGE_HISTORY,

    // Tasks (15)
    VIEW_TASKS, CREATE_TASK, EDIT_ANY_TASK, EDIT_ASSIGNED_TASKS,
    DELETE_ANY_TASK, DELETE_OWN_TASKS, ASSIGN_TASK,
    CHANGE_TASK_STATUS, CHANGE_TASK_PRIORITY, ADD_TASK_COMMENT,
    DELETE_TASK_COMMENT, VIEW_ALL_TASKS, VIEW_ASSIGNED_TASKS,
    CREATE_SUBTASK, COMPLETE_TASK,

    // Files (8)
    UPLOAD_FILE, DELETE_ANY_FILE, DELETE_OWN_FILE, DOWNLOAD_FILE,
    VIEW_FILES, MANAGE_FILE_PERMISSIONS, SHARE_FILE_EXTERNALLY,
    VIEW_FILE_VERSIONS
}
```

---

### core.config

**Package:** `com.example.kosmos.core.config`

#### SupabaseConfig.kt

**Purpose:** Supabase client singleton configuration

```kotlin
object SupabaseConfig {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                scheme = "kosmos"
                host = "auth-callback"
                alwaysAutoRefresh = true
            }

            install(Postgrest) {
                serializer = KotlinXSerializer(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }

            install(Storage)
            install(Realtime)

            httpEngine = OkHttp.create()
        }
    }
}
```

**Modules Configured:**
- **Auth**: GoTrue with OAuth deep linking
- **Postgrest**: REST API with JSON serialization
- **Storage**: File upload/download
- **Realtime**: WebSocket subscriptions

**OAuth Deep Link:** `kosmos://auth-callback`

---

### core.validators

**Package:** `com.example.kosmos.core.validators`

#### PermissionChecker.kt

**Purpose:** RBAC permission validation

```kotlin
object PermissionChecker {
    fun hasPermission(
        member: ProjectMember,
        project: Project,
        permission: Permission
    ): PermissionResult {
        // 1. Check project status
        if (project.status == ProjectStatus.ARCHIVED &&
            permission !in listOf(Permission.VIEW_PROJECT, Permission.ARCHIVE_PROJECT)) {
            return PermissionResult.ProjectNotModifiable("Project is archived")
        }

        // 2. Check role default permissions
        val rolePermissions = when (member.role) {
            ProjectRole.ADMIN -> Permission.ADMIN_PERMISSIONS
            ProjectRole.MANAGER -> Permission.MANAGER_PERMISSIONS
            ProjectRole.MEMBER -> Permission.MEMBER_PERMISSIONS
        }

        // 3. Check custom permissions override
        val customPermissions = member.customPermissions?.let {
            parseCustomPermissions(it)
        }

        // 4. Determine final result
        val hasPermission = permission in rolePermissions ||
                           (customPermissions != null && permission in customPermissions)

        return if (hasPermission) {
            PermissionResult.Granted
        } else {
            PermissionResult.Denied("Insufficient permissions")
        }
    }

    sealed class PermissionResult {
        object Granted : PermissionResult()
        data class Denied(val reason: String) : PermissionResult()
        data class NotProjectMember(val userId: String) : PermissionResult()
        data class ProjectNotModifiable(val reason: String) : PermissionResult()
    }
}
```

**Permission Sets:**
- **ADMIN_PERMISSIONS**: All 49 permissions
- **MANAGER_PERMISSIONS**: 30 permissions (no project deletion, no role changes to ADMIN)
- **MEMBER_PERMISSIONS**: 15 permissions (basic read/write, no management)

---

#### RoleValidator.kt

**Purpose:** Role hierarchy validation

```kotlin
object RoleValidator {
    fun canChangeRole(
        changerRole: ProjectRole,
        targetCurrentRole: ProjectRole,
        newRole: ProjectRole
    ): ValidationResult {
        // Cannot change own role
        if (changerRole == targetCurrentRole) {
            return ValidationResult.Error("Cannot change your own role")
        }

        // Must have higher role than target
        if (!changerRole.canManage(targetCurrentRole)) {
            return ValidationResult.Error("Cannot change role of equal or higher member")
        }

        // Must have higher role than new role
        if (!changerRole.canManage(newRole)) {
            return ValidationResult.Error("Cannot assign role equal to or higher than yours")
        }

        return ValidationResult.Success
    }

    fun canRemoveMember(
        removerRole: ProjectRole,
        targetRole: ProjectRole,
        isLastAdmin: Boolean
    ): ValidationResult {
        // Cannot remove last admin
        if (targetRole == ProjectRole.ADMIN && isLastAdmin) {
            return ValidationResult.Error("Cannot remove last admin")
        }

        // Must have higher role than target
        if (!removerRole.canManage(targetRole)) {
            return ValidationResult.Error("Cannot remove equal or higher member")
        }

        return ValidationResult.Success
    }

    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
```

---

## DATA LAYER

### Package: `com.example.kosmos.data`

Data layer implementing Repository pattern with offline-first sync.

---

### data.repository (6 Repositories)

**Common Pattern:**
```kotlin
@Singleton
class SomeRepository @Inject constructor(
    private val dao: SomeDao,
    private val supabaseDataSource: SupabaseDataSource
) {
    // Read (always from Room, reactive)
    fun getSomeFlow(id: String): Flow<Some> = dao.getSomeFlow(id)

    // Write (Room first, Supabase async)
    suspend fun createSome(some: Some): Result<String> {
        return try {
            // 1. Save to Room (instant UI update)
            dao.insert(some)

            // 2. Sync to Supabase (background)
            supabaseDataSource.insert(some)

            Result.success(some.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

#### 1. AuthRepository

**File:** `data/repository/AuthRepository.kt`
**Dependencies:** Supabase Auth, UserDao

**Key Methods:**

```kotlin
// Authentication
suspend fun signInWithEmailAndPassword(email: String, password: String): Result<User>
suspend fun createUserWithEmailAndPassword(
    email: String,
    password: String,
    displayName: String,
    username: String,
    age: Int?,
    role: String?,
    ...
): Result<User>
suspend fun signInWithGoogle(activity: ComponentActivity): Result<User>
suspend fun signOut(): Result<Unit>

// Session Management
suspend fun refreshSession(): Result<Unit>
fun getCurrentUser(): User?
fun isUserLoggedIn(): Boolean
val currentUser: Flow<User?>
val isAuthenticated: Flow<Boolean>

// Profile Management
suspend fun updateUserProfile(user: User): Result<User>
suspend fun loadUserProfile(userId: String)
private suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean): Result<Unit>
```

**Features:**
- Firebase Auth integration (kept for stability)
- Google OAuth with deep link handling
- Auto-session restore on app startup
- Online/offline status tracking
- User-friendly error messages

**Error Handling:**
```kotlin
val errorMessage = when {
    e is HttpRequestTimeoutException ->
        "Connection timeout. Please check your internet connection."
    e is AuthRestException && e.message?.contains("Invalid login credentials") == true ->
        "Invalid email or password. Please try again."
    e.message?.contains("network") == true ->
        "Network error. Please check your internet connection."
    else -> "Sign in failed: ${e.message ?: "Unknown error"}"
}
```

---

#### 2. UserRepository

**File:** `data/repository/UserRepository.kt`
**Dependencies:** UserDao, SupabaseUserDataSource

**Key Methods:**

```kotlin
suspend fun getUserById(userId: String): User?
fun getUserByIdFlow(userId: String): Flow<User?>
suspend fun searchUsers(query: String): Result<List<User>>
suspend fun updateUser(user: User): Result<Unit>
suspend fun syncUser(userId: String): Result<Unit>
```

**Use Cases:**
- User profile viewing
- User search (for invites, mentions)
- Profile updates
- Background user sync

---

#### 3. ProjectRepository

**File:** `data/repository/ProjectRepository.kt`
**Dependencies:** ProjectDao, ProjectMemberDao, SupabaseProjectDataSource, SupabaseProjectMemberDataSource

**Key Methods:**

```kotlin
// Project CRUD
suspend fun createProject(name: String, description: String, ownerId: String): Result<Project>
suspend fun updateProject(project: Project, userId: String): Result<Unit>
suspend fun deleteProject(projectId: String, userId: String): Result<Unit>
suspend fun updateProjectStatus(projectId: String, status: ProjectStatus, userId: String): Result<Unit>
fun getUserProjectsFlow(userId: String): Flow<List<Project>>
suspend fun syncUserProjects(userId: String): Result<Unit>

// Member Management
suspend fun addMember(projectId: String, userId: String, role: ProjectRole, invitedBy: String): Result<ProjectMember>
suspend fun removeMember(projectId: String, userIdToRemove: String, removedBy: String): Result<Unit>
suspend fun changeRole(projectId: String, userIdToChange: String, newRole: ProjectRole, changedBy: String): Result<Unit>
fun getProjectMembersFlow(projectId: String): Flow<List<ProjectMember>>
suspend fun syncProjectMembers(projectId: String): Result<Unit>

// Permissions
suspend fun hasPermission(projectId: String, userId: String, permission: Permission): Boolean
suspend fun getMemberRole(projectId: String, userId: String): ProjectRole?

// Stats (Optimized with cached metadata)
fun getProjectStatsFlow(projectId: String): Flow<ProjectStats>
suspend fun getProjectStats(projectId: String): ProjectStats
fun getAllProjectsStatsFlow(userId: String): Flow<Map<String, ProjectStats>>
```

**RBAC Enforcement:**
- All write operations check permissions via PermissionChecker
- Role hierarchy validated via RoleValidator
- Custom permissions support via JSON override

**Performance Optimization:**
```kotlin
// OLD (250ms - 5 queries):
val memberCount = projectMemberDao.getMemberCount(projectId)
val taskCount = taskDao.getTaskCount(projectId)
val chatCount = chatRoomDao.getChatCount(projectId)
// ...

// NEW (10ms - 1 query):
val project = projectDao.getProjectById(projectId)
val stats = ProjectStats(
    memberCount = project.memberCount,  // Cached
    taskCount = project.taskCount,      // Cached
    // ...
)
```

**Result:** 25x performance improvement

---

#### 4. ChatRepository

**File:** `data/repository/ChatRepository.kt`
**Dependencies:** ChatRoomDao, MessageDao, SupabaseMessageDataSource, SupabaseChatDataSource, SupabaseRealtimeManager

**Key Methods:**

```kotlin
// Chat Room Management
fun getChatRoomsForProject(userId: String, projectId: String): Flow<List<ChatRoom>>
suspend fun syncUserChatRooms(userId: String): Result<Unit>
suspend fun createChatRoom(chatRoom: ChatRoom): Result<String>
suspend fun deleteChatRoom(chatRoomId: String): Result<Unit>
suspend fun archiveChatRoom(chatRoomId: String, isArchived: Boolean): Result<Unit>
suspend fun pinChatRoom(chatRoomId: String, isPinned: Boolean): Result<Unit>

// Messaging
fun getMessagesFlow(chatRoomId: String): Flow<List<Message>>
suspend fun sendMessage(message: Message): Result<String>
suspend fun editMessage(messageId: String, newContent: String): Result<Unit>
suspend fun deleteMessage(messageId: String): Result<Unit>
suspend fun toggleReaction(messageId: String, userId: String, emoji: String): Result<Unit>
suspend fun markMessagesAsRead(chatRoomId: String, userId: String): Result<Unit>

// Real-time
fun startRealtimeSubscription(chatRoomId: String)
fun stopRealtimeSubscription(chatRoomId: String)
fun getMessageEvents(): SharedFlow<MessageEvent>
fun sendTypingIndicator(chatRoomId: String, userId: String, isTyping: Boolean)

// Pagination
suspend fun loadMoreMessages(chatRoomId: String, beforeTimestamp: Long, limit: Int): Result<List<Message>>
```

**Data Flow:**
```
Write: UI → ViewModel → Repository → Room (immediate) → Supabase (async)
Read:  Room Flow → Repository → ViewModel → UI (reactive)
Real-time: Supabase WebSocket → SupabaseRealtimeManager → Room → Flow → UI
```

**Features:**
- **Optimistic updates**: Messages appear instantly before server confirmation
- **Retry logic**: SyncRetryHelper handles FK violations with exponential backoff
- **Real-time sync**: WebSocket subscriptions auto-update local cache
- **Offline-first**: All operations work offline, sync when online

---

#### 5. TaskRepository

**File:** `data/repository/TaskRepository.kt`
**Dependencies:** TaskDao, ProjectDao, ProjectMemberDao, SupabaseTaskDataSource

**Key Methods:**

```kotlin
// Task CRUD
suspend fun createTask(task: Task, createdByUserId: String): Result<Task>
suspend fun updateTask(task: Task, userId: String): Result<Unit>
suspend fun deleteTask(taskId: String, userId: String): Result<Unit>
suspend fun assignTask(taskId: String, assignedToUserId: String, assignedByUserId: String): Result<Unit>
suspend fun updateTaskStatus(taskId: String, status: TaskStatus, userId: String): Result<Unit>

// Queries
fun getTasksForProjectFlow(projectId: String): Flow<List<Task>>
fun getTasksForChatRoomFlow(chatRoomId: String): Flow<List<Task>>
fun getUserTasksFlow(userId: String): Flow<List<Task>>
suspend fun syncUserTasks(userId: String): Result<Unit>
suspend fun syncProjectTasks(projectId: String): Result<Unit>
```

**RBAC Integration:**
- Task assignment respects role hierarchy
- ADMIN/MANAGER can assign to anyone
- MEMBER can only assign to equal or lower roles
- Task deletion checks DELETE_ANY_TASK or DELETE_OWN_TASKS permission

---

#### 6. VoiceRepository (Minimal - MVP Disabled)

**File:** `data/repository/VoiceRepository.kt`
**Dependencies:** VoiceMessageDao

**Status:** Placeholder for Phase 5 (voice transcription feature)

---

### data.datasource (6 Supabase Data Sources)

**Common Pattern:**
```kotlin
class SupabaseSomeDataSource @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun insert(entity: Entity): Result<Unit> {
        return try {
            supabase.from("table_name").insert(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getById(id: String): Result<Entity?> {
        return try {
            val result = supabase.from("table_name")
                .select()
                .eq("id", id)
                .decodeSingleOrNull<Entity>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**6 Data Sources:**
1. **SupabaseProjectDataSource**: Projects table operations
2. **SupabaseProjectMemberDataSource**: Project members table operations
3. **SupabaseUserDataSource**: Users table operations
4. **SupabaseMessageDataSource**: Messages table operations
5. **SupabaseTaskDataSource**: Tasks table operations
6. **SupabaseChatDataSource**: Chat rooms table operations

**All support:**
- insert, update, delete operations
- getById, getAll queries
- Custom queries (by project, by user, etc.)
- Error handling with Result type

---

### data.realtime

**Package:** `com.example.kosmos.data.realtime`

#### SupabaseRealtimeManager.kt

**Purpose:** WebSocket subscriptions for live updates

```kotlin
@Singleton
class SupabaseRealtimeManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val messageDao: MessageDao
) {
    private val channels = mutableMapOf<String, RealtimeChannel>()
    private val _messageEvents = MutableSharedFlow<MessageEvent>(replay = 0)
    val messageEvents: SharedFlow<MessageEvent> = _messageEvents.asSharedFlow()

    fun subscribeToMessages(chatRoomId: String) {
        val channel = supabase.channel("room:$chatRoomId") {
            postgresChangeFlow<PostgresAction.Insert>("public", "messages") {
                filter("chat_room_id", "eq", chatRoomId)
            }.onEach { change ->
                val message = parseMessage(change.record)
                messageDao.insertMessage(message)
                _messageEvents.emit(MessageEvent.Insert(message))
            }.launchIn(coroutineScope)

            postgresChangeFlow<PostgresAction.Update>("public", "messages") {
                filter("chat_room_id", "eq", chatRoomId)
            }.onEach { change ->
                val message = parseMessage(change.record)
                messageDao.updateMessage(message)
                _messageEvents.emit(MessageEvent.Update(message))
            }.launchIn(coroutineScope)

            postgresChangeFlow<PostgresAction.Delete>("public", "messages") {
                filter("chat_room_id", "eq", chatRoomId)
            }.onEach { change ->
                val messageId = change.oldRecord["id"] as String
                messageDao.deleteMessage(messageId)
                _messageEvents.emit(MessageEvent.Delete(messageId))
            }.launchIn(coroutineScope)
        }

        channels[chatRoomId] = channel
        channel.subscribe()
    }

    fun unsubscribeFromMessages(chatRoomId: String) {
        channels[chatRoomId]?.let { channel ->
            channel.unsubscribe()
            channels.remove(chatRoomId)
        }
    }

    fun sendTypingIndicator(chatRoomId: String, userId: String, isTyping: Boolean) {
        channels[chatRoomId]?.sendBroadcast(
            "typing",
            mapOf("user_id" to userId, "is_typing" to isTyping)
        )
    }

    sealed class MessageEvent {
        data class Insert(val message: Message) : MessageEvent()
        data class Update(val message: Message) : MessageEvent()
        data class Delete(val messageId: String) : MessageEvent()
    }
}
```

**Features:**
- Per-chat-room subscriptions (not global - memory efficient)
- INSERT, UPDATE, DELETE event handling
- Typing indicators via broadcast
- Auto-cleanup on unsubscribe
- Direct Room database updates (triggers Flow emissions)

---

### data.sync

**Package:** `com.example.kosmos.data.sync`

#### InitialSyncManager.kt

**Purpose:** Sync all user data on login

```kotlin
@Singleton
class InitialSyncManager @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val chatRepository: ChatRepository,
    private val taskRepository: TaskRepository
) {
    suspend fun syncAllData(userId: String): SyncProgress {
        val progress = MutableStateFlow(SyncProgress())

        coroutineScope {
            // Sync in parallel for speed
            val projectsDeferred = async {
                projectRepository.syncUserProjects(userId).also {
                    progress.update { it.copy(projectsSynced = true) }
                }
            }

            val chatRoomsDeferred = async {
                chatRepository.syncUserChatRooms(userId).also {
                    progress.update { it.copy(chatsSynced = true) }
                }
            }

            val tasksDeferred = async {
                taskRepository.syncUserTasks(userId).also {
                    progress.update { it.copy(tasksSynced = true) }
                }
            }

            // Wait for all
            val results = awaitAll(projectsDeferred, chatRoomsDeferred, tasksDeferred)

            progress.update { it.copy(completed = true) }
        }

        return progress.value
    }

    data class SyncProgress(
        val projectsSynced: Boolean = false,
        val chatsSynced: Boolean = false,
        val tasksSynced: Boolean = false,
        val completed: Boolean = false
    )
}
```

**Usage:**
```kotlin
// In MainActivity after login
LaunchedEffect(user.id) {
    val progress = initialSyncManager.syncAllData(user.id)
    // Show progress indicator to user
}
```

**Performance:** Syncs all data in 3-5 seconds (parallel execution)

---

#### SyncRetryHelper.kt

**Purpose:** Retry logic for failed syncs (especially FK violations)

```kotlin
object SyncRetryHelper {
    suspend fun <T> retryOnForeignKeyViolation(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        entityName: String,
        operation: suspend () -> Result<T>
    ): Result<T> {
        repeat(maxRetries) { attempt ->
            val result = operation()

            if (result.isSuccess) return result

            val error = result.exceptionOrNull()
            if (!isForeignKeyViolation(error)) {
                // Not a FK violation, don't retry
                return result
            }

            Log.d("SyncRetryHelper", "$entityName FK violation, retry ${attempt + 1}/$maxRetries")

            // Wait with exponential backoff
            delay(initialDelayMs * (attempt + 1))
        }

        // Final attempt
        return operation()
    }

    private fun isForeignKeyViolation(error: Throwable?): Boolean {
        return error?.message?.contains("foreign key", ignoreCase = true) == true ||
               error?.message?.contains("violates foreign key constraint", ignoreCase = true)
    }
}
```

**Use Cases:**
- Message insert when chat room not yet synced
- Task insert when project not yet synced
- Member add when project not yet synced

**Strategy:** Exponential backoff (1s, 2s, 3s) for 3 attempts

---

## FEATURES LAYER

### Package: `com.example.kosmos.features`

Feature modules organized by vertical slice (each feature has presentation, domain, data if needed).

---

### features.auth

**Package:** `com.example.kosmos.features.auth.presentation`

#### AuthViewModel.kt

**Purpose:** Authentication state management

```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    data class AuthUiState(
        val isLoggedIn: Boolean = false,
        val currentUser: User? = null,
        val isLoading: Boolean = false,
        val error: String? = null,

        // Login form
        val email: String = "",
        val password: String = "",
        val passwordVisible: Boolean = false,

        // SignUp form
        val displayName: String = "",
        val username: String = "",
        val confirmPassword: String = "",
        val age: Int? = null,
        val role: String? = null,
        val bio: String? = null,
        val location: String? = null,
        val githubUrl: String? = null,
        val twitterUrl: String? = null,
        val linkedinUrl: String? = null,
        val websiteUrl: String? = null,
        val portfolioUrl: String? = null,

        // Username availability
        val isCheckingUsername: Boolean = false,
        val isUsernameAvailable: Boolean? = null
    )

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.signInWithEmailAndPassword(
                uiState.value.email,
                uiState.value.password
            )

            result.fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(
                        isLoggedIn = true,
                        currentUser = user,
                        isLoading = false
                    )}
                },
                onFailure = { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message
                    )}
                }
            )
        }
    }

    fun signUp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Validation
            if (uiState.value.password != uiState.value.confirmPassword) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Passwords do not match"
                )}
                return@launch
            }

            val result = authRepository.createUserWithEmailAndPassword(
                email = uiState.value.email,
                password = uiState.value.password,
                displayName = uiState.value.displayName,
                username = uiState.value.username,
                age = uiState.value.age,
                role = uiState.value.role,
                bio = uiState.value.bio,
                location = uiState.value.location,
                githubUrl = uiState.value.githubUrl,
                twitterUrl = uiState.value.twitterUrl,
                linkedinUrl = uiState.value.linkedinUrl,
                websiteUrl = uiState.value.websiteUrl,
                portfolioUrl = uiState.value.portfolioUrl
            )

            result.fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(
                        isLoggedIn = true,
                        currentUser = user,
                        isLoading = false
                    )}
                },
                onFailure = { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message
                    )}
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { AuthUiState() } // Reset to default
        }
    }

    fun checkUsernameAvailability(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUsername = true) }

            val result = userRepository.searchUsers(username)
            val isAvailable = result.getOrNull()?.none { it.username == username } ?: false

            _uiState.update { it.copy(
                isCheckingUsername = false,
                isUsernameAvailable = isAvailable
            )}
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    // ... more update methods ...
}
```

**Features:**
- Comprehensive form state management
- Real-time username availability checking
- Password confirmation validation
- Error handling with user-friendly messages
- OAuth support (Google Sign-In)

---

#### AuthScreens.kt

Contains two screens:
1. **LoginScreen** (176 lines) - Email/password login
2. **SignUpScreen** (510 lines) - Full registration with optional fields

**LoginScreen UI:**
```kotlin
@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(Tokens.Spacing.md),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to Kosmos", style = MaterialTheme.typography.headlineLarge)

        Spacer(height = Tokens.Spacing.lg)

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") },
            visualTransformation = if (uiState.passwordVisible)
                VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = viewModel::togglePasswordVisibility) {
                    Icon(
                        if (uiState.passwordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )

        if (uiState.error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        PrimaryButton(
            text = "Login",
            onClick = { viewModel.login() },
            isLoading = uiState.isLoading,
            enabled = uiState.email.isNotBlank() && uiState.password.isNotBlank() && !uiState.isLoading
        )

        TextButton(onClick = onNavigateToSignUp) {
            Text("Don't have an account? Sign up")
        }
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }
}
```

**SignUpScreen UI:**
- Required fields: email, password, confirm password, display name, username
- Optional fields (expandable): age, role, location, bio, social links
- Real-time username availability with debounce
- Password strength indicator (not implemented yet)

---

### features.projects

**Package:** `com.example.kosmos.features.projects.presentation`

#### ProjectViewModel.kt (484 lines)

**Key Methods:**
```kotlin
fun loadUserProjects()
fun createProject(name: String, description: String)
fun updateProjectDetails(project: Project)
fun deleteProject(projectId: String)
fun loadProjectStats(projectId: String)
```

**State:**
```kotlin
data class ProjectUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedProject: Project? = null,
    val projectStats: ProjectStats? = null
)
```

---

#### ProjectListScreen (Redesign)

**File:** `features/projects/presentation/redesign/ProjectListScreen.kt`

**Features:**
- Grid/list view of projects
- Create project FAB
- Project cards with stats (members, chats, tasks)
- ⚠️ Search/filter placeholders (not wired)

---

#### ProjectDetailsScreen (Redesign)

**File:** `features/projects/presentation/redesign/ProjectDetailsScreen.kt`

**Features:**
- Tabs: Overview, Chats, Tasks, Members
- Quick actions: Create Chat, Create Task, Invite Members
- Recent activity feed
- Edit project button
- Real-time stats

---

#### ProjectWorkspaceScreen (Redesign)

**File:** `features/projects/presentation/redesign/ProjectWorkspaceScreen.kt`

**Features:**
- Persistent animated bottom navigation
- Seamless tab switching
- Real-time stat updates (fixed JobCancellationException)

---

#### MembersListViewModel.kt (254 lines)

**Key Methods:**
```kotlin
fun loadMembers(projectId: String)
fun removeMember(projectId: String, userId: String)
fun changeRole(projectId: String, userId: String, newRole: ProjectRole)
fun checkPermissions(projectId: String, permission: Permission): Boolean
```

---

#### MembersListScreen.kt (490 lines)

**Features:**
- List of project members with roles
- Online status indicators
- Add members FAB
- ⚠️ Remove/change role (admin only - UI incomplete)

---

### features.chat

**Package:** `com.example.kosmos.features.chat.presentation`

#### ChatListViewModel.kt

**Key Methods:**
```kotlin
fun loadChatRooms(projectId: String)
fun createChatRoom(name: String, type: ChatRoomType, participantIds: List<String>)
fun archiveChat(chatRoomId: String)
fun pinChat(chatRoomId: String)
fun deleteChat(chatRoomId: String)
```

---

#### ChatViewModel.kt (684 lines)

**Key Methods:**
```kotlin
fun loadMessages(chatRoomId: String)
fun sendMessage(content: String, type: MessageType = MessageType.TEXT)
fun editMessage(messageId: String, newContent: String)
fun deleteMessage(messageId: String)
fun toggleReaction(messageId: String, emoji: String)
fun markAsRead()
fun subscribeToRealtime()
fun unsubscribeFromRealtime()
fun sendTypingIndicator(isTyping: Boolean)
```

**State:**
```kotlin
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUser: User? = null,
    val chatRoom: ChatRoom? = null,
    val typingUsers: List<String> = emptyList(),
    val replyToMessage: Message? = null
)
```

---

#### EnhancedChatScreen (Redesign)

**File:** `features/chat/presentation/redesign/EnhancedChatScreen.kt`

**Features:**
- Message list with grouping (5-minute window)
- Message types: TEXT, VOICE, IMAGE, FILE, SYSTEM, TASK_CREATED
- Real-time updates
- Read receipts (double check marks)
- Reactions (emoji)
- Typing indicators
- Message editing and deletion
- Reply functionality
- Copy to clipboard
- Task creation from messages

**UI Interactions:**
- Text input field with send button
- Long press message → Context menu (Edit, Delete, Reply, React, Copy, Create Task)
- Voice record button (hold to record) - Disabled for MVP
- Camera/file attachment buttons - Placeholder

---

#### EnhancedChatListScreen (Redesign)

**File:** `features/chat/presentation/redesign/EnhancedChatListScreen.kt`

**Features:**
- List of chat rooms for project
- Unread badges
- Last message preview
- Timestamp formatting ("2 hours ago")
- Swipe actions (Archive, Delete, Pin)
- Create chat FAB

---

#### ChatOptionsBottomSheet.kt

**Features:**
- Pin/Unpin chat
- Archive/Unarchive chat
- Delete chat (with confirmation)
- ✅ All implemented and working

---

#### CreateChatDialog.kt

**Features:**
- Multi-select user picker (WhatsApp-style)
- Selected users as chips
- Chat name input
- Chat type selector (Channel, Direct, Task Discussion)
- ✅ Fully implemented (Nov 9, 2025)

---

### features.tasks

**Package:** `com.example.kosmos.features.tasks.presentation`

#### TaskViewModel.kt (684 lines)

**Key Methods:**
```kotlin
fun loadTasks(projectId: String)
fun createTask(title: String, description: String, priority: TaskPriority, assignedToId: String?, ...)
fun updateTask(task: Task)
fun deleteTask(taskId: String)
fun changeStatus(taskId: String, status: TaskStatus)
fun assignTask(taskId: String, userId: String)
fun addComment(taskId: String, text: String)
fun updateTimeTracking(taskId: String, estimatedHours: Double?, actualHours: Double?)
```

**State:**
```kotlin
data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentFilter: TaskFilter = TaskFilter.ALL,
    val showMyTasksOnly: Boolean = false,
    val showCreateTaskDialog: Boolean = false,
    val showEditTaskDialog: Boolean = false,
    val editingTask: Task? = null,
    val availableUsers: List<User> = emptyList()
)

enum class TaskFilter { ALL, TODO, IN_PROGRESS, DONE }
```

---

#### TaskScreens.kt (1,418 lines) ⚠️ OLD VERSION

**Contains 3 screens:**
1. TaskBoardScreen (34-327) - Task list with filters
2. CreateTaskDialog (330-707) - Task creation modal
3. EditTaskDialog (710-1122) - Task editing modal

**TaskBoardScreen Features:**
- Tab filtering (All, To Do, In Progress, Done)
- "My Tasks" filter chip
- Task cards with:
  - Priority badge (color-coded)
  - Status chip
  - Assigned user
  - Due date (with overdue warnings)
  - Tags (max 2 + count)
  - Time tracking (estimated vs actual)
  - Completion checkbox (quick toggle)
- Empty state with illustration
- ✅ 95% design system compliance (partial fix in Phase 6)

**CreateTaskDialog Features:**
- Title (required)
- Description (multiline, optional)
- Priority selector (LOW, MEDIUM, HIGH, URGENT chips)
- Assigned To (user picker bottom sheet)
- Due Date (Material DatePicker)
- Tags (add/remove chips)
- Time tracking (estimated/actual hours)

**EditTaskDialog Features:**
- All CreateTaskDialog fields
- Status selector (TODO, IN_PROGRESS, DONE, CANCELLED)
- Parent task selector (subtasks support)
- Comments section (expandable)
  - Comment list (sorted by newest)
  - Add comment input
- Save button
- Delete button (destructive)

---

#### MyTasksScreen (Redesign)

**File:** `features/tasks/presentation/redesign/MyTasksScreen.kt`

**Features:**
- Cross-project task view
- My assigned tasks
- Filters (all, by project, by status)
- Sort options

---

#### QuickTaskCreationSheet (Redesign)

**File:** `features/tasks/presentation/redesign/QuickTaskCreationSheet.kt`

**Features:**
- Minimal fields (title, priority)
- Fast creation UX
- ⚠️ Exists but not fully wired to ProjectDetails button

---

### features.users

**Package:** `com.example.kosmos.features.users.presentation`

#### UserSearchViewModel.kt (120 lines)

**Key Methods:**
```kotlin
fun searchUsers(query: String)
fun addToProject(userId: String, projectId: String, role: ProjectRole)
```

---

#### UserSearchScreen.kt (203 lines)

**Features:**
- Search users by name, email, username
- Debounced search (500ms)
- User list with avatars
- Click → Navigate to UserProfileScreen

---

#### UserProfileViewModel.kt (295 lines)

**Key Methods:**
```kotlin
fun loadUserProfile(userId: String)
fun startChat(userId: String)
fun addToProject(projectId: String)
```

---

#### UserProfileScreen.kt (516 lines)

**Features:**
- View other user's profile
- Profile picture, name, bio
- Social links (clickable, external browser)
- Start chat button
- Add to project button
- ✅ Fully functional

---

#### InviteMembersViewModel.kt (243 lines)

**Key Methods:**
```kotlin
fun searchUsers(query: String)
fun toggleUserSelection(userId: String)
fun inviteMembers(projectId: String, role: ProjectRole)
```

---

#### InviteMembersScreen.kt (334 lines)

**Features:**
- Search and select multiple users
- Selected users chip list
- Role selection (Admin, Manager, Member)
- Invite button
- ✅ Fully functional

---

### features.profile

**Package:** `com.example.kosmos.features.profile.presentation`

#### ProfileScreen.kt (186 lines)

**Features:**
- Large profile picture (120dp)
- User info card (name, email)
- Action list:
  - Edit Profile
  - Privacy Settings
  - Notifications
- ✅ 100% design system compliant

---

#### EditProfileScreen.kt (409 lines)

**Features:**
- Photo upload button ⚠️ TODO: Supabase Storage upload
- Edit display name
- Edit bio
- Edit location
- Edit social links
- Save button

**Known Issue:** Photo picker works, but upload to Supabase Storage not implemented

---

#### PrivacySettingsViewModel.kt (124 lines)

**Key Methods:**
```kotlin
fun loadSettings()
fun updatePrivacySettings(settings: PrivacySettings)
```

---

#### PrivacySettingsScreen.kt (346 lines)

**Features:**
- Show online status toggle
- Read receipts toggle
- Typing indicators toggle
- Last seen visibility
- Profile photo visibility
- Who can add me to projects
- ⚠️ Minimal implementation, backend TODO

---

#### NotificationSettingsViewModel.kt (171 lines)

**Key Methods:**
```kotlin
fun loadSettings()
fun updateNotificationSettings(settings: NotificationSettings)
```

---

#### NotificationSettingsScreen.kt (490 lines)

**Features:**
- Push notifications master toggle
- Message notifications
- Task notifications
- Mention notifications
- Sound toggle
- Vibration toggle
- ⚠️ Minimal implementation, backend TODO

---

### features.smart

**Package:** `com.example.kosmos.features.smart.services`

#### ActionDetectionService.kt

**Purpose:** Detect actionable items from text/voice messages

**Pattern Matching:**
```kotlin
private val actionPatterns = mapOf(
    ActionType.TASK to listOf("need to", "should", "must", "todo", "action item", "we have to"),
    ActionType.MEETING to listOf("meeting", "call", "schedule", "let's meet", "discussion"),
    ActionType.REMINDER to listOf("remind", "don't forget", "remember", "note to self"),
    ActionType.DEADLINE to listOf("due", "deadline", "expires", "by", "before"),
    ActionType.FOLLOW_UP to listOf("follow up", "check back", "circle back", "revisit")
)

fun detectActionsFromText(
    text: String,
    chatRoomId: String,
    messageId: String
): List<ActionItem> {
    val actions = mutableListOf<ActionItem>()
    val lowerText = text.lowercase()

    for ((actionType, patterns) in actionPatterns) {
        for (pattern in patterns) {
            if (pattern in lowerText) {
                val confidence = calculateConfidence(text, pattern)
                if (confidence > 0.3) {  // Threshold
                    actions.add(ActionItem(
                        id = UUID.randomUUID().toString(),
                        messageId = messageId,
                        chatRoomId = chatRoomId,
                        type = actionType,
                        text = extractActionText(text, pattern),
                        confidence = confidence
                    ))
                }
            }
        }
    }

    return actions.distinctBy { it.text }
}

private fun calculateConfidence(text: String, pattern: String): Float {
    var confidence = 0.4f  // Base confidence

    // Length boost
    if (text.length > 50) confidence += 0.1f
    if (text.length > 100) confidence += 0.1f

    // Keyword matching
    val keywords = listOf("important", "urgent", "asap", "priority")
    for (keyword in keywords) {
        if (keyword in text.lowercase()) confidence += 0.1f
    }

    // Common phrase penalty
    val commonPhrases = listOf("just to let you know", "fyi", "by the way")
    for (phrase in commonPhrases) {
        if (phrase in text.lowercase()) confidence *= 0.5f
    }

    return confidence.coerceIn(0.1f, 0.95f)
}
```

**Usage:**
```kotlin
val text = "We need to schedule a meeting tomorrow to discuss the project deadline"
val actions = actionDetectionService.detectActionsFromText(text, chatRoomId, messageId)
// Detects: TASK ("schedule a meeting"), DEADLINE ("project deadline")
```

---

#### SmartReplyService.kt

**Purpose:** Generate contextual reply suggestions

**Status:** Placeholder for ML-based suggestions (future enhancement)

**Planned Features:**
- Context-aware reply suggestions
- 3-5 quick replies per message
- Types: GENERAL, CONFIRMATION, QUESTION, TASK_RELATED, MEETING

---

## SHARED LAYER

### Package: `com.example.kosmos.shared`

Shared UI components, design system, and utilities.

---

### shared.ui.designsystem

**Package:** `com.example.kosmos.shared.ui.designsystem`

**Total Code:** 5116 lines (4 files)

---

#### Tokens.kt (1500+ lines estimated)

**Purpose:** Design tokens for consistent spacing, sizing, animations

```kotlin
object Tokens {
    object Spacing {
        val xxs = 4.dp
        val xs = 8.dp
        val sm = 12.dp
        val md = 16.dp  // Most common
        val lg = 24.dp
        val xl = 32.dp
        val xxl = 48.dp
    }

    object TouchTarget {
        val minimum = 48.dp       // Accessibility minimum
        val recommended = 56.dp   // Primary actions
        val comfortable = 64.dp   // Important actions
    }

    object Size {
        // Avatars
        val avatarSmall = 24.dp
        val avatarMedium = 40.dp
        val avatarLarge = 56.dp
        val avatarXLarge = 80.dp
        val avatarXXLarge = 120.dp

        // Icons
        val iconSmall = 16.dp
        val iconMedium = 24.dp
        val iconLarge = 32.dp

        // Chips
        val chipHeight = 32.dp

        // Badges
        val badgeSmall = 16.dp
        val badgeMedium = 20.dp
    }

    object Elevation {
        val level0 = 0.dp   // Flat
        val level1 = 1.dp   // Cards
        val level2 = 3.dp   // Buttons
        val level3 = 6.dp   // FAB
        val level4 = 8.dp   // Drawer
        val level5 = 12.dp  // Dialogs
    }

    object CornerRadius {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp   // Standard cards
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 28.dp
        val full = 9999.dp  // Pills
    }

    object Duration {
        val fast = 100
        val normal = 200
        val medium = 300
        val slow = 400
        val slowest = 500

        // Specific animations
        val ripple = 150
        val fadeIn = 200
        val fadeOut = 150
        val slideIn = 300
        val slideOut = 250
        val scale = 200
    }

    object Opacity {
        val full = 1.0f
        val high = 0.87f      // Primary text
        val medium = 0.60f    // Secondary text
        val disabled = 0.38f  // Disabled elements
        val divider = 0.12f   // Dividers
        val backdrop = 0.32f  // Modal backdrop
    }

    object ZIndex {
        val background = 0
        val content = 1
        val elevated = 2
        val sticky = 3
        val fab = 4
        val bottomNav = 5
        val snackbar = 6
        val bottomSheet = 7
        val dialog = 8
        val tooltip = 9
        val notification = 10
    }

    object MessageGrouping {
        val timeWindowMinutes = 5  // Group messages within 5 minutes
    }

    object Performance {
        val targetFps = 60
        val maxFrameTimeMs = 16  // 1000ms / 60fps
        val optimisticUiDelayMs = 100
        val listBufferItems = 20  // LazyColumn buffer
    }
}
```

---

#### ColorTokens.kt (900+ lines estimated)

**Purpose:** Semantic color system

```kotlin
object ColorTokens {
    // Primary palette
    val primary = Color(0xFF2196F3)        // Blue
    val primaryVariant = Color(0xFF1976D2) // Dark blue
    val onPrimary = Color(0xFFFFFFFF)      // White

    // Secondary palette
    val secondary = Color(0xFF03DAC6)      // Teal
    val secondaryVariant = Color(0xFF018786)
    val onSecondary = Color(0xFF000000)

    // Error palette
    val error = Color(0xFFB00020)
    val onError = Color(0xFFFFFFFF)

    // Background
    val background = Color(0xFFFFFBFE)
    val onBackground = Color(0xFF1C1B1F)

    // Surface
    val surface = Color(0xFFFFFBFE)
    val surfaceVariant = Color(0xFFE7E0EC)
    val onSurface = Color(0xFF1C1B1F)
    val onSurfaceVariant = Color(0xFF49454F)

    // Priority colors (for tasks)
    val priorityLow = Color(0xFF4CAF50)    // Green
    val priorityMedium = Color(0xFFFF9800) // Orange
    val priorityHigh = Color(0xFFFF5722)   // Deep orange
    val priorityUrgent = Color(0xFFF44336) // Red

    // Status colors (for tasks)
    val statusTodo = Color(0xFF9E9E9E)      // Gray
    val statusInProgress = Color(0xFF2196F3) // Blue
    val statusDone = Color(0xFF4CAF50)      // Green
    val statusCancelled = Color(0xFF757575) // Dark gray

    // Role colors (for badges)
    val roleAdmin = Color(0xFFE91E63)      // Pink
    val roleManager = Color(0xFF9C27B0)    // Purple
    val roleMember = Color(0xFF3F51B5)     // Indigo

    // Online status
    val statusOnline = Color(0xFF4CAF50)   // Green
    val statusAway = Color(0xFFFFC107)     // Amber
    val statusOffline = Color(0xFF9E9E9E)  // Gray

    // Dark theme colors
    object Dark {
        val primary = Color(0xFF90CAF9)     // Light blue
        val background = Color(0xFF121212)
        val surface = Color(0xFF2C2C2C)
        // ... more dark theme colors
    }
}
```

---

#### TypographyTokens.kt (700+ lines estimated)

**Purpose:** Text styles

```kotlin
object TypographyTokens {
    // Custom styles
    val buttonText = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    )

    val chipLabel = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.25.sp
    )

    val badgeNumber = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp
    )

    val bottomNavLabel = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    )

    val timestamp = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        color = Color.Gray
    )

    val messageSender = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )

    val messageContent = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    // Material 3 typography scale
    // (Display Large/Medium/Small, Headline Large/Medium/Small, etc.)
    // ... full Material 3 scale defined here
}
```

---

#### IconSet.kt (2000+ lines estimated)

**Purpose:** Centralized icon definitions (100+ icons)

```kotlin
object IconSet {
    // Navigation (15 icons)
    val projects = Icons.Filled.Folder
    val chats = Icons.Filled.Chat
    val tasks = Icons.Filled.CheckCircle
    val more = Icons.Filled.MoreVert
    val back = Icons.Filled.ArrowBack
    val forward = Icons.Filled.ArrowForward
    val close = Icons.Filled.Close
    val menu = Icons.Filled.Menu
    val home = Icons.Filled.Home
    // ... more navigation icons

    // Action (24 icons)
    val add = Icons.Filled.Add
    val create = Icons.Filled.Create
    val edit = Icons.Filled.Edit
    val delete = Icons.Filled.Delete
    val save = Icons.Filled.Save
    val cancel = Icons.Filled.Cancel
    val done = Icons.Filled.Done
    val search = Icons.Filled.Search
    val filter = Icons.Filled.FilterList
    val share = Icons.Filled.Share
    // ... more action icons

    // Message/Chat (26 icons)
    val send = Icons.Filled.Send
    val message = Icons.Filled.Message
    val chat = Icons.Filled.Chat
    val reply = Icons.Filled.Reply
    val forward = Icons.Filled.Forward
    val reaction = Icons.Filled.EmojiEmotions
    val emoji = Icons.Filled.InsertEmoticon
    val attach = Icons.Filled.AttachFile
    val image = Icons.Filled.Image
    val camera = Icons.Filled.CameraAlt
    val mic = Icons.Filled.Mic
    val pin = Icons.Filled.PushPin
    val archive = Icons.Filled.Archive
    // ... more chat icons

    // Task (13 icons)
    val task = Icons.Filled.Assignment
    val checkCircle = Icons.Filled.CheckCircle
    val assignment = Icons.Filled.Assignment
    val list = Icons.Filled.List
    val board = Icons.Filled.Dashboard
    val calendar = Icons.Filled.CalendarToday
    val priority = Icons.Filled.Flag
    // ... more task icons

    // User (14 icons)
    val person = Icons.Filled.Person
    val personAdd = Icons.Filled.PersonAdd
    val people = Icons.Filled.People
    val group = Icons.Filled.Group
    val account = Icons.Filled.AccountCircle
    val profile = Icons.Filled.AccountBox
    val logout = Icons.Filled.Logout
    val login = Icons.Filled.Login
    // ... more user icons

    // Status (14 icons)
    val online = Icons.Filled.Circle  // Green
    val offline = Icons.Filled.Circle // Gray
    val away = Icons.Filled.Circle    // Amber
    val busy = Icons.Filled.DoNotDisturb
    val typing = Icons.Filled.MoreHoriz
    val connected = Icons.Filled.CheckCircle
    val syncing = Icons.Filled.Sync
    val error = Icons.Filled.Error
    val warning = Icons.Filled.Warning
    val success = Icons.Filled.CheckCircle
    // ... more status icons

    // Settings (14 icons)
    val settings = Icons.Filled.Settings
    val notifications = Icons.Filled.Notifications
    val privacy = Icons.Filled.Lock
    val security = Icons.Filled.Security
    val theme = Icons.Filled.Palette
    val darkMode = Icons.Filled.DarkMode
    val lightMode = Icons.Filled.LightMode
    val language = Icons.Filled.Language
    val help = Icons.Filled.Help
    // ... more settings icons

    // Project (9 icons)
    val project = Icons.Filled.Folder
    val folder = Icons.Filled.Folder
    val star = Icons.Filled.Star
    val bookmark = Icons.Filled.Bookmark
    val label = Icons.Filled.Label
    // ... more project icons

    // File (11 icons)
    val file = Icons.Filled.InsertDriveFile
    val folder = Icons.Filled.Folder
    val image = Icons.Filled.Image
    val video = Icons.Filled.VideoLibrary
    val audio = Icons.Filled.AudioFile
    val document = Icons.Filled.Description
    val pdf = Icons.Filled.PictureAsPdf
    val attachment = Icons.Filled.AttachFile
    val download = Icons.Filled.Download
    val upload = Icons.Filled.Upload
    // ... more file icons

    // Time (10 icons)
    val clock = Icons.Filled.Schedule
    val calendar = Icons.Filled.CalendarToday
    val schedule = Icons.Filled.Schedule
    val history = Icons.Filled.History
    val timer = Icons.Filled.Timer
    val alarm = Icons.Filled.Alarm
    val today = Icons.Filled.Today
    // ... more time icons

    // Priority (5 icons)
    val urgent = Icons.Filled.PriorityHigh
    val high = Icons.Filled.ArrowUpward
    val medium = Icons.Filled.Remove
    val low = Icons.Filled.ArrowDownward
    val flag = Icons.Filled.Flag

    // Media (11 icons)
    val play = Icons.Filled.PlayArrow
    val pause = Icons.Filled.Pause
    val stop = Icons.Filled.Stop
    val record = Icons.Filled.FiberManualRecord
    val forward = Icons.Filled.FastForward
    val rewind = Icons.Filled.FastRewind
    val volumeUp = Icons.Filled.VolumeUp
    val volumeDown = Icons.Filled.VolumeDown
    val volumeMute = Icons.Filled.VolumeOff
    val microphone = Icons.Filled.Mic
    val microphoneOff = Icons.Filled.MicOff

    // Visibility (4 icons)
    val visible = Icons.Filled.Visibility
    val invisible = Icons.Filled.VisibilityOff
    val visibleOutlined = Icons.Outlined.Visibility
    val invisibleOutlined = Icons.Outlined.VisibilityOff

    // Direction (8 icons)
    val up = Icons.Filled.KeyboardArrowUp
    val down = Icons.Filled.KeyboardArrowDown
    val left = Icons.Filled.KeyboardArrowLeft
    val right = Icons.Filled.KeyboardArrowRight
    val expand = Icons.Filled.ExpandMore
    val collapse = Icons.Filled.ExpandLess
    val chevronUp = Icons.Filled.KeyboardArrowUp
    val chevronDown = Icons.Filled.KeyboardArrowDown

    // Helper functions
    fun getNavigationIcon(type: String, isSelected: Boolean): ImageVector {
        return when (type) {
            "projects" -> if (isSelected) Icons.Filled.Folder else Icons.Outlined.Folder
            "chats" -> if (isSelected) Icons.Filled.Chat else Icons.Outlined.Chat
            "tasks" -> if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle
            "more" -> Icons.Filled.MoreVert
            else -> Icons.Filled.Home
        }
    }

    fun getTaskStatusIcon(status: TaskStatus): ImageVector {
        return when (status) {
            TaskStatus.TODO -> Icons.Outlined.Circle
            TaskStatus.IN_PROGRESS -> Icons.Filled.MoreHoriz
            TaskStatus.DONE -> Icons.Filled.CheckCircle
            TaskStatus.CANCELLED -> Icons.Filled.Cancel
        }
    }

    fun getPriorityIcon(priority: TaskPriority): ImageVector {
        return when (priority) {
            TaskPriority.URGENT -> Icons.Filled.PriorityHigh
            TaskPriority.HIGH -> Icons.Filled.ArrowUpward
            TaskPriority.MEDIUM -> Icons.Filled.Remove
            TaskPriority.LOW -> Icons.Filled.ArrowDownward
        }
    }

    fun getStatusIcon(isOnline: Boolean, isAway: Boolean): ImageVector {
        return when {
            isOnline -> Icons.Filled.Circle  // Green circle
            isAway -> Icons.Filled.Circle    // Amber circle
            else -> Icons.Filled.Circle      // Gray circle
        }
    }
}
```

---

### shared.ui.components

**Package:** `com.example.kosmos.shared.ui.components`

Component library using design system tokens.

---

#### Buttons.kt (498 lines)

**Components:**

1. **PrimaryButton** - Filled button for main CTAs
```kotlin
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    fullWidth: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(Tokens.TouchTarget.recommended),
        colors = ButtonDefaults.buttonColors(
            containerColor = ColorTokens.primary,
            contentColor = ColorTokens.onPrimary
        ),
        shape = RoundedCornerShape(Tokens.CornerRadius.md)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Tokens.Size.iconMedium),
                color = ColorTokens.onPrimary
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(Tokens.Size.iconSmall))
                Spacer(Modifier.width(Tokens.Spacing.xs))
            }
            Text(text, style = TypographyTokens.buttonText)
        }
    }
}
```

2. **SecondaryButton** - Outlined button for secondary actions

3. **TextButtonStandard** - Text-only button for tertiary actions

4. **IconButtonStandard** - Icon-only button (48x48dp touch target)

5. **LoadingButton** - Button with loading state

6. **FABStandard** - Floating Action Button (56x56dp)

7. **FABMini** - Small FAB (40x40dp)

8. **DestructiveButton** - Red button for delete/remove

9. **ToggleButtonGroup** - Segmented buttons

10. **ButtonGroup** - Horizontal button layout (OK/Cancel)

11. **PillButton** - Fully rounded button

---

#### Cards.kt

**Components:**

1. **StandardCard** - Basic card (elevation level1)
```kotlin
@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick ?: {},
        enabled = onClick != null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = Tokens.Elevation.level1
        ),
        shape = RoundedCornerShape(Tokens.CornerRadius.md)
    ) {
        Column(
            modifier = Modifier.padding(Tokens.Spacing.md),
            content = content
        )
    }
}
```

2. **ClickableCard** - Card with ripple effect
3. **SelectableCard** - Card with selected state
4. **ElevatedCard** - Card with higher elevation

---

#### Lists.kt

**Components:**

1. **ListDivider** - Standard divider
2. **ListSection** - Section header
3. **ListItemStandard** - Pre-configured ListItem

---

#### Inputs.kt

**Components:**

1. **StandardTextField** - OutlinedTextField with consistent styling
2. **SearchTextField** - TextField with search icon and clear button
3. **PasswordTextField** - TextField with visibility toggle

---

#### Dialogs.kt

**Components:**

1. **StandardAlertDialog** - Pre-configured AlertDialog
2. **ConfirmationDialog** - Two-button confirmation
3. **InfoDialog** - Single-button info dialog

---

#### Feedback.kt

**Components:**

1. **LoadingIndicator** - Centered loading spinner
2. **EmptyState** - Empty state with icon and message
3. **ErrorState** - Error state with retry button

---

### shared.ui.layouts

**Package:** `com.example.kosmos.shared.ui.layouts`

#### ScreenScaffold.kt

**Purpose:** Standard screen layout wrapper

```kotlin
@Composable
fun ScreenScaffold(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = if (onNavigateBack != null) {
                    {
                        IconButton(onClick = onNavigateBack) {
                            Icon(IconSet.back, contentDescription = "Back")
                        }
                    }
                } else {
                    {}
                },
                actions = actions
            )
        },
        floatingActionButton = floatingActionButton
    ) { paddingValues ->
        content(paddingValues)
    }
}
```

---

#### SwipeableLayout.kt

**Purpose:** Swipeable card for lists (archive/delete actions)

---

#### ListLayouts.kt

**Purpose:** Pre-configured list patterns (LazyColumn with pull-to-refresh, etc.)

---

### shared.ui.features

**Package:** `com.example.kosmos.shared.ui.features`

#### navigation/BottomNavigation.kt

**Purpose:** Bottom navigation bar component

```kotlin
@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    unreadChatsCount: Int = 0,
    pendingTasksCount: Int = 0
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "projects",
            onClick = { onNavigate("projects") },
            icon = { Icon(IconSet.getNavigationIcon("projects", currentRoute == "projects")) },
            label = { Text("Projects") }
        )

        NavigationBarItem(
            selected = currentRoute == "chats",
            onClick = { onNavigate("chats") },
            icon = {
                BadgedBox(badge = { if (unreadChatsCount > 0) Badge { Text("$unreadChatsCount") } }) {
                    Icon(IconSet.getNavigationIcon("chats", currentRoute == "chats"))
                }
            },
            label = { Text("Chats") }
        )

        NavigationBarItem(
            selected = currentRoute == "tasks",
            onClick = { onNavigate("tasks") },
            icon = {
                BadgedBox(badge = { if (pendingTasksCount > 0) Badge { Text("$pendingTasksCount") } }) {
                    Icon(IconSet.getNavigationIcon("tasks", currentRoute == "tasks"))
                }
            },
            label = { Text("Tasks") }
        )

        NavigationBarItem(
            selected = currentRoute == "more",
            onClick = { onNavigate("more") },
            icon = { Icon(IconSet.getNavigationIcon("more", currentRoute == "more")) },
            label = { Text("More") }
        )
    }
}
```

---

#### gestures/GestureHelper.kt

**Purpose:** Gesture utilities (swipe, long press, double tap detection)

---

### shared.ui.utils

**Package:** `com.example.kosmos.shared.ui.utils`

#### DateTimeUtils.kt

**Purpose:** Timestamp formatting utilities

```kotlin
object DateTimeUtils {
    fun Long.toRelativeTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - this

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> this.toDateTime()
        }
    }

    fun Long.toDateTime(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(this))
    }

    fun Long.toDateOnly(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(this))
    }

    fun Long.toTimeOnly(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(this))
    }
}
```

---

### shared.ui.theme

**Package:** `com.example.kosmos.shared.ui.theme`

#### Theme.kt

**Purpose:** Material 3 theme setup

```kotlin
@Composable
fun KosmosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,  // Material You on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = ColorTokens.Dark.primary,
            background = ColorTokens.Dark.background,
            surface = ColorTokens.Dark.surface,
            // ... more dark colors
        )
        else -> lightColorScheme(
            primary = ColorTokens.primary,
            background = ColorTokens.background,
            surface = ColorTokens.surface,
            // ... more light colors
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,  // Defined in Type.kt
        content = content
    )
}
```

**Features:**
- Material You (dynamic colors on Android 12+)
- Dark theme support
- Falls back to static colors on older devices

---

## MAIN APPLICATION LAYER

### MainActivity.kt

**Purpose:** Single activity hosting Compose navigation

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KosmosTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

                // Initial sync on login
                LaunchedEffect(authUiState.isLoggedIn, authUiState.currentUser?.id) {
                    if (authUiState.isLoggedIn && authUiState.currentUser != null) {
                        val initialSyncManager: InitialSyncManager = /* get from Hilt */
                        initialSyncManager.syncAllData(authUiState.currentUser.id)
                    }
                }

                // Navigation
                NavHost(
                    navController = navController,
                    startDestination = if (authUiState.isLoggedIn) Screen.ProjectList.route else Screen.Login.route
                ) {
                    composable(Screen.Login.route) {
                        LoginScreen(
                            onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                            onLoginSuccess = { navController.navigate(Screen.ProjectList.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }}
                        )
                    }

                    composable(Screen.SignUp.route) {
                        SignUpScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSignUpSuccess = { navController.navigate(Screen.ProjectList.route) {
                                popUpTo(Screen.SignUp.route) { inclusive = true }
                            }}
                        )
                    }

                    composable(Screen.ProjectList.route) {
                        ProjectListScreen(
                            onProjectClick = { projectId ->
                                navController.navigate(Screen.ProjectDetail.route.replace("{projectId}", projectId))
                            }
                        )
                    }

                    // ... all other screens
                }
            }
        }

        // Handle OAuth callback
        intent?.data?.let { uri ->
            if (uri.scheme == "kosmos" && uri.host == "auth-callback") {
                handleOAuthCallback(uri)
            }
        }
    }

    private fun handleOAuthCallback(uri: Uri) {
        // Process Supabase OAuth callback
        lifecycleScope.launch {
            SupabaseConfig.client.auth.handleDeepLink(uri)
        }
    }
}
```

**Navigation Graph:**
- Login → SignUp
- ProjectList (home)
  - → ProjectDetails
    - → ChatList → Chat
    - → TaskBoard
    - → MembersList
    - → InviteMembers
  - → UserSearch → UserProfile
  - → Profile → EditProfile, PrivacySettings, NotificationSettings

---

### KosmosApplication.kt

**Purpose:** Application class for Hilt setup

```kotlin
@HiltAndroidApp
class KosmosApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize any app-wide services here
        // (e.g., Firebase, crash reporting, analytics)
    }
}
```

---

### Module.kt

**Purpose:** All dependency injection modules

**Modules:**
1. **DatabaseModule** - Room database and DAOs
2. **SupabaseModule** - Supabase client and data sources
3. **NetworkModule** - Retrofit for Google Speech API (disabled)
4. **RepositoryModule** - All repositories
5. **ServiceModule** - Smart services

**Example:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideKosmosDatabase(@ApplicationContext context: Context): KosmosDatabase {
        return Room.databaseBuilder(
            context,
            KosmosDatabase::class.java,
            "kosmos_database"
        )
        .fallbackToDestructiveMigration()  // ⚠️ TODO: Implement proper migrations
        .build()
    }

    @Provides fun provideUserDao(database: KosmosDatabase): UserDao = database.userDao()
    @Provides fun provideChatRoomDao(database: KosmosDatabase): ChatRoomDao = database.chatRoomDao()
    @Provides fun provideMessageDao(database: KosmosDatabase): MessageDao = database.messageDao()
    @Provides fun provideTaskDao(database: KosmosDatabase): TaskDao = database.taskDao()
    @Provides fun provideProjectDao(database: KosmosDatabase): ProjectDao = database.projectDao()
    @Provides fun provideProjectMemberDao(database: KosmosDatabase): ProjectMemberDao = database.projectMemberDao()
    @Provides fun provideVoiceMessageDao(database: KosmosDatabase): VoiceMessageDao = database.voiceMessageDao()
    @Provides fun provideActionItemDao(database: KosmosDatabase): ActionItemDao = database.actionItemDao()
}
```

---

## BUILD CONFIGURATION

### app/build.gradle.kts

**Key Configurations:**

```kotlin
android {
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.kosmos"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Build config fields
        buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("supabase.url")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.findProperty("supabase.anon.key")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false  // ⚠️ TODO: Enable Proguard
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ViewModel & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:2.2.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")

    // Ktor (for Supabase)
    implementation("io.ktor:ktor-client-okhttp:2.3.7")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Coil (image loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Firebase (kept for Auth and FCM)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Google Play Services (for Google Sign-In)
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

---

## SUMMARY

### Codebase Statistics

- **Total Files:** 94+ Kotlin files
- **Total Lines:** ~15,000+ (estimated)
- **Design System:** 5116 lines (4 files)
- **ViewModels:** 11
- **Repositories:** 6
- **Data Sources:** 6
- **DAOs:** 8
- **Domain Models:** 11
- **Screens:** 22+
- **Reusable Components:** 20+

### Architecture Quality

**Strengths:**
- ✅ Clean separation of concerns (MVVM + Repository)
- ✅ Comprehensive dependency injection (Hilt)
- ✅ Offline-first pattern with optimistic updates
- ✅ Complete design system (5116 lines)
- ✅ Reactive programming (Flow, StateFlow)
- ✅ Type-safe navigation

**Areas for Improvement:**
- ⚠️ Destructive migrations (data loss risk)
- ⚠️ No automated tests (0% coverage)
- ⚠️ Some duplicate files (old vs redesigned screens)
- ⚠️ Photo upload incomplete

### Technology Choices

**Well-Chosen:**
- ✅ Jetpack Compose (modern, declarative UI)
- ✅ Material 3 (consistent, accessible design)
- ✅ Supabase (open-source, powerful backend)
- ✅ Room (reliable local database)
- ✅ Hilt (standard DI solution)

**Acceptable Trade-offs:**
- ⚠️ Firebase Auth (kept for stability, mature)
- ⚠️ No Firestore (migrated to Supabase PostgreSQL)
- ⚠️ Voice features disabled (MVP scope reduction)

---

**Document Prepared By:** Claude Code Analysis System
**Related Documents:**
- PROJECT_OVERVIEW_STATUS.md - High-level project overview
- UI_UX_METHODS_FLOW.md - UI method inventory for redesign
- LOGS_SESSIONS_ANALYSIS.md - Development history
- GAPS_RISKS_VERIFICATION.md - Issues and concerns
- IMPROVEMENT_ROADMAP.md - Actionable recommendations
