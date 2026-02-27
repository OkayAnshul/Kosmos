# Missing and Unwired Features

**Date:** January 23, 2026
**Category:** Implementation Gaps

---

## Executive Summary

The Kosmos app has **23 partially implemented features** where either:
1. **UI exists but backend broken** (13 features)
2. **Backend exists but UI missing/incomplete** (6 features)
3. **Both exist but not wired together** (4 features)

**Impact:** Users see buttons/screens that don't work → Poor UX, frustration

**Time to Fix:** 60-80 hours (2-3 weeks)

---

## Category 1: UI Exists, Backend Broken

### 1. Photo Upload (Profile Pictures)

**Status:** ❌ 30% Complete

**What Exists:**
```kotlin
// EditProfileScreen.kt (DELETED but functionality exists in UserProfileScreen)
LaunchedEffect(Unit) {
    photoPickerLauncher.launch(PickVisualMediaRequest(...))
}
```

**What's Missing:**
```kotlin
// UserRepository.kt - MISSING:
suspend fun uploadProfilePhoto(userId: String, uri: Uri): Result<String> {
    // ❌ Supabase Storage upload not implemented
    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
    val path = supabaseClient.storage["avatars"]
        .upload("$userId/avatar.jpg", bytes)

    // Update user.avatar_url
    supabaseUserDataSource.update(user.copy(avatar_url = path))

    return Result.success(path)
}
```

**Impact:**
- Users can select photos but nothing happens
- Profile photos never upload
- Multi-device users see default avatars

**Fix Time:** 8 hours
- Implement Supabase Storage upload (4 hours)
- Wire to UserRepository (2 hours)
- Test upload flow (2 hours)

**File Locations:**
- `app/src/main/java/com/example/kosmos/data/repository/UserRepository.kt`
- `app/src/main/java/com/example/kosmos/features/users/presentation/UserProfileScreen.kt`

---

### 2. Privacy Settings

**Status:** ❌ 30% Complete

**What Exists:**
```kotlin
// PrivacySettingsViewModel.kt
data class PrivacySettingsState(
    val showEmail: Boolean = true,
    val showPhone: Boolean = false,
    val allowMessagesFrom: String = "everyone"
)

fun toggleShowEmail(show: Boolean) {
    _state.value = _state.value.copy(showEmail = show)
    // ❌ STOPS HERE - doesn't save to database
}
```

**What's Missing:**
```kotlin
// PrivacySettingsViewModel.kt - ADD:
fun toggleShowEmail(show: Boolean) {
    _state.value = _state.value.copy(showEmail = show)

    viewModelScope.launch {
        val settings = UserSettings(
            userId = authRepository.currentUserId,
            showEmail = show,
            // ... other fields
        )
        userRepository.saveSettings(settings)  // ✅ Persist to Room + Supabase
    }
}
```

```kotlin
// UserRepository.kt - ADD:
suspend fun saveSettings(settings: UserSettings): Result<Unit> {
    settingsDao.insertSettings(settings)  // Room
    supabaseUserDataSource.updateSettings(settings)  // Supabase
    return Result.success(Unit)
}
```

**Impact:**
- Users toggle settings, but they reset on app restart
- No privacy control actually enforced
- Poor UX

**Fix Time:** 4 hours
- Wire ViewModel to Repository (1 hour)
- Implement Repository methods (2 hours)
- Test persistence (1 hour)

**File Locations:**
- `app/src/main/java/com/example/kosmos/features/profile/presentation/PrivacySettingsViewModel.kt`
- `app/src/main/java/com/example/kosmos/data/repository/UserRepository.kt`

---

### 3. Notification Settings

**Status:** ❌ 30% Complete

**What Exists:**
```kotlin
// NotificationSettingsViewModel.kt
data class NotificationSettingsState(
    val notificationsEnabled: Boolean = true,
    val taskAssigned: Boolean = true,
    val taskStatusChanged: Boolean = true,
    val newMessage: Boolean = true,
    val projectInvite: Boolean = true
)

fun toggleNotifications(enabled: Boolean) {
    _state.value = _state.value.copy(notificationsEnabled = enabled)
    // ❌ STOPS HERE
}
```

**What's Missing:**
- Same as Privacy Settings (see above)
- Settings never save to database
- FCM token not updated based on preferences

**Impact:**
- Users disable notifications, but they keep coming
- No granular notification control
- Poor UX

**Fix Time:** 4 hours (same as Privacy Settings)

**File Locations:**
- `app/src/main/java/com/example/kosmos/features/profile/presentation/NotificationSettingsViewModel.kt`
- `app/src/main/java/com/example/kosmos/data/repository/UserRepository.kt`

---

### 4. Task Comments

**Status:** ❌ 50% Complete

**What Exists:**
```kotlin
// TaskDetailScreen.kt
// Comments section with input field
OutlinedTextField(
    value = commentText,
    onValueChange = { commentText = it },
    placeholder = { Text("Add a comment...") }
)

Button(onClick = {
    // ❌ onClick is empty!
}) {
    Text("Post")
}
```

**What's Missing:**
```kotlin
// TaskDetailViewModel.kt - ADD:
fun postComment(taskId: String, text: String) {
    viewModelScope.launch {
        val comment = TaskComment(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            userId = authRepository.currentUserId,
            text = text,
            createdAt = Instant.now()
        )

        taskRepository.addComment(comment)
    }
}
```

```kotlin
// TaskRepository.kt - ADD:
suspend fun addComment(comment: TaskComment): Result<Unit> {
    commentDao.insert(comment)  // Room
    supabaseTaskDataSource.insertComment(comment)  // Supabase
    return Result.success(Unit)
}
```

**Database Schema:**
- ❌ `task_comments` table doesn't exist in Room or Supabase
- Need to create entity, DAO, data source

**Impact:**
- Comment input exists but does nothing
- Users can't discuss tasks
- Feature appears broken

**Fix Time:** 8 hours
- Create TaskComment entity (1 hour)
- Create DAO + data source (2 hours)
- Wire to ViewModel (2 hours)
- Add to Supabase schema (1 hour)
- Test (2 hours)

**File Locations:**
- `app/src/main/java/com/example/kosmos/features/tasks/presentation/TaskDetailViewModel.kt`
- `app/src/main/java/com/example/kosmos/data/repository/TaskRepository.kt`
- Need new: `TaskComment.kt`, `TaskCommentDao.kt`, `SupabaseTaskCommentDataSource.kt`

---

### 5. Chat Search

**Status:** ❌ 20% Complete

**What Exists:**
```kotlin
// ChatListScreen.kt
IconButton(onClick = {
    // ❌ onClick is empty!
}) {
    Icon(Icons.Default.Search, contentDescription = "Search")
}
```

**What's Missing:**
```kotlin
// ChatSearchDialog.kt - CREATE:
@Composable
fun ChatSearchDialog(
    onDismiss: () -> Unit,
    onMessageClick: (Message) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val viewModel: ChatListViewModel = hiltViewModel()
    val searchResults by viewModel.searchResults.collectAsState()

    // Search UI with results
}
```

```kotlin
// ChatListViewModel.kt - ADD:
private val _searchResults = MutableStateFlow<List<Message>>(emptyList())
val searchResults = _searchResults.asStateFlow()

fun searchMessages(query: String) {
    viewModelScope.launch {
        _searchResults.value = chatRepository.searchMessages(query).getOrElse { emptyList() }
    }
}
```

```kotlin
// ChatRepository.kt - ADD:
suspend fun searchMessages(query: String): Result<List<Message>> {
    val messages = messageDao.searchMessages("%$query%")
    return Result.success(messages)
}
```

**Database:**
- ✅ MessageDao already has: `@Query("SELECT * FROM messages WHERE content LIKE :query")`
- Just needs wiring!

**Impact:**
- Search button exists but does nothing
- Can't find old messages
- Poor UX for large chat histories

**Fix Time:** 6 hours
- Create ChatSearchDialog (3 hours)
- Wire to ViewModel (1 hour)
- Test search (2 hours)

**File Locations:**
- `app/src/main/java/com/example/kosmos/features/chat/presentation/ChatListViewModel.kt`
- `app/src/main/java/com/example/kosmos/data/repository/ChatRepository.kt`
- Need new: `ChatSearchDialog.kt`

---

### 6. Message Reactions

**Status:** ❌ 30% Complete

**What Exists:**
```kotlin
// MessageComponents.kt
// Reaction UI partially implemented but not functional
Row {
    ReactionChip(emoji = "👍", count = 3)
    ReactionChip(emoji = "❤️", count = 1)
}
```

**What's Missing:**
- MessageReaction entity
- Database schema
- Add/remove reaction methods
- Real-time reaction updates

**Impact:**
- Reactions UI exists but doesn't work
- Can't express quick feedback

**Fix Time:** 10 hours (low priority)

**File Locations:**
- `app/src/main/java/com/example/kosmos/features/chat/components/MessageComponents.kt`

---

### 7. Blocked Users

**Status:** ❌ 10% Complete

**What Exists:**
```kotlin
// PrivacySettingsScreen.kt (DELETED)
// Had a "Blocked Users" section with placeholder UI
```

**What's Missing:**
- BlockedUser entity
- Database schema
- Block/unblock methods
- UI to manage blocked users

**Impact:**
- No user blocking feature
- Privacy feature missing

**Fix Time:** 12 hours (low priority)

---

### 8. Task Subtask Picker

**Status:** ❌ 60% Complete

**What Exists:**
```kotlin
// TaskDependency.kt - ✅ Entity exists
// TaskDependencyDao.kt - ✅ DAO exists
// Database schema supports dependencies
```

**What's Missing:**
```kotlin
// TaskEditScreen.kt - ADD:
// UI to select parent task or dependencies
LazyColumn {
    items(availableTasks) { task ->
        TaskListItem(
            task = task,
            onClick = { viewModel.addDependency(taskId, task.id) }
        )
    }
}
```

**Impact:**
- Backend ready but no UI to create task hierarchies
- Can't organize complex tasks

**Fix Time:** 6 hours
- Add subtask picker UI (4 hours)
- Wire to ViewModel (1 hour)
- Test (1 hour)

**File Locations:**
- `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskEditScreen.kt`

---

### 9. Task Drag-and-Drop (Kanban)

**Status:** ❌ 40% Complete

**What Exists:**
```kotlin
// TaskBoardScreen.kt - UI exists with columns
// Can change status via dropdown
```

**What's Missing:**
```kotlin
// TaskBoardScreen.kt - ADD:
// Drag-and-drop between columns
val dragDropState = rememberDragDropState()

LazyColumn(modifier = Modifier.dragContainer(dragDropState)) {
    items(tasks) { task ->
        TaskCard(
            task = task,
            modifier = Modifier.draggableItem(dragDropState, task.id)
        )
    }
}
```

**Impact:**
- Kanban board exists but no drag-and-drop
- Must use dropdown to change status (clunky)

**Fix Time:** 12 hours (complex)

**File Locations:**
- `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskBoardScreen.kt`

---

### 10-13. Various Search/Filter Placeholders

**Status:** ❌ 10-20% Complete

**What Exists:**
- Project filter icon (no logic)
- Task filter icon (no logic)
- User search filters (basic, need advanced)
- Sort options (not wired)

**What's Missing:**
- Filter dialogs
- Sort logic wired to DAOs
- Multi-criteria filtering

**Impact:**
- Icons exist but don't work
- Can't filter large lists
- Poor UX for power users

**Fix Time:** 16 hours total (4 hours each)

---

## Category 2: Backend Exists, UI Missing

### 1. Project Archive/Delete

**Status:** ⚠️ 80% Complete

**What Exists:**
```kotlin
// ProjectRepository.kt
suspend fun deleteProject(projectId: String): Result<Unit> {
    projectDao.deleteProject(projectId)
    supabaseProjectDataSource.delete(projectId)
    return Result.success(Unit)
}

suspend fun archiveProject(projectId: String): Result<Unit> {
    // ✅ Backend method exists
}
```

**What's Missing:**
```kotlin
// ProjectDetailsScreen.kt - ADD:
// Options menu with Archive/Delete
DropdownMenu {
    DropdownMenuItem(
        text = { Text("Archive Project") },
        onClick = { viewModel.archiveProject(projectId) }
    )
    DropdownMenuItem(
        text = { Text("Delete Project") },
        onClick = { showDeleteDialog = true }
    )
}
```

**Impact:**
- Can't delete/archive projects from UI
- Must use backend directly (not user-friendly)

**Fix Time:** 2 hours
- Add menu options to UI (1 hour)
- Add confirmation dialogs (1 hour)

**File Locations:**
- `app/src/main/java/com/example/kosmos/features/projects/presentation/redesign/ProjectDetailsScreen.kt`

---

### 2. Member Context Menu (Remove/Change Role)

**Status:** ⚠️ 80% Complete

**What Exists:**
```kotlin
// MembersListViewModel.kt
suspend fun removeMember(projectId: String, userId: String): Result<Unit> {
    // ✅ Backend method exists
}

suspend fun changeRole(projectId: String, userId: String, role: String): Result<Unit> {
    // ✅ Backend method exists
}
```

**What's Missing:**
```kotlin
// MembersListScreen.kt - ADD:
// Long-press menu on member items
DropdownMenu {
    DropdownMenuItem(
        text = { Text("Change Role") },
        onClick = { showRoleDialog = true }
    )
    DropdownMenuItem(
        text = { Text("Remove Member") },
        onClick = { showRemoveDialog = true }
    )
}
```

**Impact:**
- Backend ready but no UI
- Can't manage members from app

**Fix Time:** 4 hours
- Add context menu (2 hours)
- Add dialogs (2 hours)

**File Locations:**
- `app/src/main/java/com/example/kosmos/features/projects/presentation/MembersListScreen.kt`

---

### 3. Task Milestone Picker

**Status:** ⚠️ 60% Complete

**What Exists:**
```kotlin
// Milestone.kt - ✅ Entity exists
// MilestoneDao.kt - ✅ DAO exists
// SupabaseMilestoneDataSource.kt - ✅ Data source exists
```

**What's Missing:**
- UI to create milestones
- UI to assign tasks to milestones
- Milestone progress tracking UI

**Impact:**
- Backend ready but feature invisible
- Can't use milestone functionality

**Fix Time:** 8 hours
- Create milestone management screen (4 hours)
- Add milestone picker to TaskEdit (2 hours)
- Add progress UI (2 hours)

**File Locations:**
- Need new: `MilestoneScreen.kt`, `MilestoneViewModel.kt`

---

### 4. Time Entry Reports

**Status:** ⚠️ 50% Complete

**What Exists:**
```kotlin
// TimeEntry.kt - ✅ Entity exists
// TimeEntryDao.kt - ✅ DAO exists
// Manual time entry UI exists
```

**What's Missing:**
- Time reports screen
- Time aggregation by project/user/date
- Export functionality

**Impact:**
- Can track time but can't see reports
- Feature half-done

**Fix Time:** 10 hours
- Create time reports screen (6 hours)
- Add aggregation queries (2 hours)
- Test (2 hours)

**File Locations:**
- Need new: `TimeReportsScreen.kt`, `TimeReportsViewModel.kt`

---

### 5. Notification List UI

**Status:** ⚠️ 60% Complete

**What Exists:**
```kotlin
// NotificationRepository.kt - ✅ Repository exists (300 LOC)
suspend fun getNotifications(userId: String): Result<List<Notification>> {
    // ✅ Backend method exists
}
```

**What's Missing:**
```kotlin
// NotificationScreen.kt - CREATE:
@Composable
fun NotificationScreen(viewModel: NotificationViewModel = hiltViewModel()) {
    val notifications by viewModel.notifications.collectAsState()

    LazyColumn {
        items(notifications) { notification ->
            NotificationItem(notification = notification)
        }
    }
}
```

**Impact:**
- Push notifications work but no in-app list
- Can't review past notifications

**Fix Time:** 6 hours
- Create NotificationScreen (3 hours)
- Create NotificationViewModel (2 hours)
- Test (1 hour)

**File Locations:**
- Need new: `NotificationScreen.kt`, `NotificationViewModel.kt`
- Exists: `app/src/main/java/com/example/kosmos/data/repository/NotificationRepository.kt`

---

### 6. Advanced Task Filters

**Status:** ⚠️ 40% Complete

**What Exists:**
```kotlin
// TaskDao.kt - ✅ Complex query methods exist
@Query("SELECT * FROM tasks WHERE project_id = :projectId AND status = :status AND priority = :priority")
suspend fun getTasksFiltered(projectId: String, status: String, priority: String): List<Task>
```

**What's Missing:**
- Filter dialog UI
- Multi-select filters
- Save filter presets

**Impact:**
- Backend ready but no UI
- Can't use advanced filtering

**Fix Time:** 6 hours

**File Locations:**
- Need new: `TaskFilterDialog.kt`

---

## Category 3: Both Exist but Not Wired

### 1. NetworkMonitor + OfflineModeBanner

**Status:** ❌ 0% Wired (CRITICAL)

**What Exists:**
```kotlin
// NetworkMonitor.kt - ✅ Interface + implementation exist
interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

class NetworkMonitorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkMonitor {
    // ✅ Implementation complete
}
```

```kotlin
// OfflineModeBanner.kt - ✅ Beautiful component exists
@Composable
fun OfflineModeBanner(isOffline: Boolean) {
    AnimatedVisibility(visible = isOffline) {
        Surface(color = Color.Orange) {
            Text("Offline Mode - Changes saved locally")
        }
    }
}
```

**What's Missing:**
```kotlin
// Module.kt - ADD:
@Provides
@Singleton
fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
    return NetworkMonitorImpl(context)  // ❌ THIS IS MISSING!
}
```

```kotlin
// ProjectListScreen.kt, ChatListScreen.kt, MyTasksScreen.kt - ADD:
@Composable
fun ProjectListScreen(
    networkMonitor: NetworkMonitor = hiltViewModel<ProjectViewModel>().networkMonitor
) {
    val isOffline by networkMonitor.isOnline.collectAsState()

    Column {
        OfflineModeBanner(isOffline = !isOffline)  // ✅ USE IT!
        // ... rest of UI
    }
}
```

**Impact:**
- Network monitoring exists but not wired
- Offline banner exists but never shown
- Users have no feedback when offline (CRITICAL UX ISSUE!)

**Fix Time:** 4 hours
- Wire NetworkMonitor in DI (30 minutes)
- Add to ViewModels (1 hour)
- Integrate OfflineModeBanner in 5 screens (2 hours)
- Test (30 minutes)

**File Locations:**
- `app/src/main/java/com/example/kosmos/Module.kt`
- `app/src/main/java/com/example/kosmos/shared/ui/components/OfflineModeBanner.kt`
- All main screens (ProjectList, ChatList, MyTasks, TaskBoard, TaskDetail)

---

### 2. UserSettings Entity (Unused)

**Status:** ❌ 0% Wired

**What Exists:**
```kotlin
// UserSettings.kt - ✅ Entity exists
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val userId: String,
    val notificationsEnabled: Boolean = true,
    val showEmail: Boolean = true,
    val showPhone: Boolean = false,
    val theme: String = "dark",
    val language: String = "en"
)
```

**What's Missing:**
- DAO never created
- Never wired to SettingsViewModels
- Never synced to Supabase

**Impact:**
- Entity exists but completely unused
- Settings don't persist

**Fix Time:** 4 hours (covered in Settings section above)

**File Locations:**
- `app/src/main/java/com/example/kosmos/core/models/UserSettings.kt`
- Need new: `UserSettingsDao.kt`

---

### 3. Task Comments Schema (Partial)

**Status:** ⚠️ 30% Wired

**What Exists:**
- UI components exist
- TaskDetailScreen has comment section

**What's Missing:**
- TaskComment entity
- Database schema
- Repository methods

**Impact:**
- Comment UI exists but completely non-functional

**Fix Time:** 8 hours (covered above)

---

### 4. Sync Queue (Designed but Not Implemented)

**Status:** ❌ 0% Implemented (CRITICAL)

**What Exists:**
```kotlin
// Repositories have try-catch with "will retry later" logs
} catch (e: Exception) {
    Log.w(TAG, "Sync failed, will retry later")  // ❌ IT WON'T!
    Result.success(Unit)  // Optimistic
}
```

**What's Missing:**
```kotlin
// SyncQueue.kt - CREATE:
@Entity(tableName = "pending_syncs")
data class PendingSyncOperation(
    @PrimaryKey val id: String,
    val operation: String,  // "create_task", "update_user", etc.
    val entityId: String,
    val payload: String,  // JSON
    val timestamp: Instant,
    val retryCount: Int = 0
)

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_syncs ORDER BY timestamp ASC")
    fun getPendingSyncs(): Flow<List<PendingSyncOperation>>

    @Insert
    suspend fun insert(sync: PendingSyncOperation)

    @Delete
    suspend fun delete(sync: PendingSyncOperation)
}

// SyncManager.kt - CREATE:
class SyncManager @Inject constructor(
    private val syncDao: PendingSyncDao,
    private val networkMonitor: NetworkMonitor,
    // ... all repositories
) {
    init {
        // Watch network state
        networkMonitor.isOnline.collect { isOnline ->
            if (isOnline) {
                processPendingSyncs()
            }
        }
    }

    private suspend fun processPendingSyncs() {
        val pending = syncDao.getPendingSyncs().first()
        pending.forEach { sync ->
            when (sync.operation) {
                "create_task" -> taskRepository.sync(sync.entityId)
                "update_user" -> userRepository.sync(sync.entityId)
                // ... etc
            }
            syncDao.delete(sync)
        }
    }
}
```

**Impact:**
- Offline changes lost if app closes before reconnecting
- No retry mechanism (DATA LOSS RISK!)

**Fix Time:** 24 hours (complex, critical)
- Create PendingSyncOperation entity (2 hours)
- Create PendingSyncDao (2 hours)
- Create SyncManager (8 hours)
- Wire to all Repositories (8 hours)
- Test offline scenarios (4 hours)

**File Locations:**
- Need new: `PendingSyncOperation.kt`, `PendingSyncDao.kt`, `SyncManager.kt`
- Update: All 7 Repositories

---

## Summary by Priority

### P0 - CRITICAL (Must Fix)

| Feature | Status | Fix Time | Impact |
|---------|--------|----------|--------|
| NetworkMonitor + OfflineModeBanner | 0% wired | 4 hours | No offline feedback |
| Sync Queue | 0% impl | 24 hours | Data loss risk |
| User Profile Sync | 50% | 1 hour | Multi-device data loss |
| Activity Tracking Offline | 70% | 2 hours | Audit trail incomplete |

**Total P0 Time:** 31 hours

### P1 - HIGH (Fix Before Production)

| Feature | Status | Fix Time | Impact |
|---------|--------|----------|--------|
| Privacy Settings | 30% | 4 hours | Settings don't save |
| Notification Settings | 30% | 4 hours | Settings don't save |
| Photo Upload | 30% | 8 hours | Feature broken |
| Chat Search | 20% | 6 hours | Poor UX |
| Task Comments | 50% | 8 hours | Feature broken |
| Project Delete UI | 80% | 2 hours | Can't delete projects |
| Member Context Menu | 80% | 4 hours | Can't manage members |

**Total P1 Time:** 36 hours

### P2 - MEDIUM (Polish)

| Feature | Status | Fix Time | Impact |
|---------|--------|----------|--------|
| Task Subtask Picker | 60% | 6 hours | Missing feature |
| Notification List UI | 60% | 6 hours | Incomplete feature |
| Task Drag-and-Drop | 40% | 12 hours | Poor UX |
| Milestone UI | 60% | 8 hours | Invisible feature |
| Message Reactions | 30% | 10 hours | Nice-to-have |
| Time Reports | 50% | 10 hours | Incomplete feature |
| Various Filters | 20% | 16 hours | Power user features |

**Total P2 Time:** 68 hours

---

## Grand Total

**Time to Fix All Gaps:** 135 hours (3-4 weeks with 1 developer)

**Recommended Approach:**
1. **Week 1:** Fix P0 (31 hours) - Critical data integrity
2. **Week 2:** Fix P1 (36 hours) - Complete broken features
3. **Week 3-4:** Fix P2 (68 hours) - Polish and advanced features

---

## Conclusion

The Kosmos app has **excellent UI coverage** (95%) but suffers from **incomplete wiring**:
- 13 features have UI but broken backend
- 6 features have backend but missing UI
- 4 features have both but not connected

**Critical Issues:**
1. NetworkMonitor + OfflineModeBanner not wired (CRITICAL UX)
2. No sync queue (DATA LOSS RISK)
3. Profile updates don't sync (DATA LOSS)
4. Settings don't persist (POOR UX)

**Verdict:** App looks complete but many features don't actually work. Users will encounter numerous "broken buttons" and silent failures.

**Recommendation:** Fix P0 + P1 (67 hours) before any launch. P2 can wait for post-launch.

---

**Next:** Read `05-offline-first-audit.md` for detailed offline mode analysis.
