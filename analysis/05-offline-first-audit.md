# Offline-First Architecture Audit

**Date:** January 23, 2026
**Assessment:** Offline-First Grade: F (55/100)

---

## Executive Summary

The Kosmos app is **designed** for offline-first but **implementation is broken**:

- ✅ **Design:** Room-first updates (correct pattern)
- ❌ **NetworkMonitor:** Not wired in DI (app has no idea if offline)
- ❌ **OfflineModeBanner:** Exists but never used (no user feedback)
- ❌ **Sync Queue:** Doesn't exist (offline changes lost if app closes)
- ❌ **Conflict Resolution:** Doesn't exist (last write wins, data loss)
- ⚠️ **Retry Mechanism:** Logs say "will retry later" but won't

**Verdict:** Offline-first architecture is **theoretically sound** but **practically broken**. Users working offline will have a poor experience with potential data loss.

---

## The Offline-First Promise

**What Offline-First Means:**
1. App works fully without internet
2. All writes save locally immediately (UI updates instantly)
3. Changes sync to server when connection restored
4. User sees clear feedback about online/offline status
5. Conflicts resolved gracefully (no data loss)

**What Kosmos Delivers:**
1. ✅ App works without internet (Room caching)
2. ✅ Writes save locally (correct)
3. ⚠️ Sync happens opportunistically (no retry if fails)
4. ❌ No user feedback (NetworkMonitor not wired)
5. ❌ No conflict resolution (last write wins)

**Grade: 55/100 (F)** - Design is correct, execution is incomplete

---

## Component 1: NetworkMonitor

### Implementation Status: ❌ 0% Wired (CRITICAL)

**What Exists:**
```kotlin
// app/src/main/java/com/example/kosmos/shared/ui/utils/NetworkMonitor.kt
interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

class NetworkMonitorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkMonitor {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkConnection())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    private fun checkConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

**What's Missing:**
```kotlin
// Module.kt - THIS IS MISSING!
@Provides
@Singleton
fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
    return NetworkMonitorImpl(context)
}
```

**Impact:**
- NetworkMonitor exists but never instantiated
- App has no idea if it's online or offline
- Repositories attempt sync without checking network state
- Users have no feedback about connectivity

**Fix:**
```kotlin
// Module.kt - ADD:
@Module
@InstallIn(SingletonComponent::class)
object Module {
    // ... existing providers

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitorImpl(context)
    }
}
```

**Fix Time:** 30 minutes

**File:** `app/src/main/java/com/example/kosmos/Module.kt`

---

## Component 2: OfflineModeBanner

### Implementation Status: ❌ 0% Used

**What Exists:**
```kotlin
// app/src/main/java/com/example/kosmos/shared/ui/components/OfflineModeBanner.kt
@Composable
fun OfflineModeBanner(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Surface(
            color = ColorTokens.warning,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(Tokens.space3),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(Tokens.space2))
                Text(
                    text = "Offline Mode - Changes saved locally",
                    style = TypographyTokens.body2,
                    color = Color.White
                )
            }
        }
    }
}
```

**Grep Result:**
```bash
$ grep -r "OfflineModeBanner" app/src/
app/src/main/java/com/example/kosmos/shared/ui/components/OfflineModeBanner.kt:fun OfflineModeBanner(
# NO OTHER USAGE FOUND!
```

**What's Missing:**
```kotlin
// ProjectListScreen.kt - ADD:
@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel = hiltViewModel(),
    networkMonitor: NetworkMonitor = hiltViewModel<ProjectViewModel>().networkMonitor
) {
    val isOffline by networkMonitor.isOnline.collectAsState()

    Column {
        OfflineModeBanner(isOffline = !isOffline)  // ✅ ADD THIS

        // ... rest of UI
    }
}
```

**Should Be Added To (5 screens):**
1. ProjectListScreen - Main project view
2. ChatListScreen - Main chat view
3. MyTasksScreen - Main task view
4. TaskBoardScreen - Kanban board
5. TaskDetailScreen - Task details

**Impact:**
- Beautiful component exists but completely unused
- Users have zero visual feedback when offline
- When offline, users create data thinking it's synced
- No indication that "saving locally" vs "synced to server"

**Fix Time:** 2 hours
- Wire NetworkMonitor to 5 ViewModels (1 hour)
- Integrate OfflineModeBanner in 5 screens (1 hour)

**Files:**
- `app/src/main/java/com/example/kosmos/features/projects/presentation/redesign/ProjectListScreen.kt`
- `app/src/main/java/com/example/kosmos/features/chat/presentation/redesign/ChatListScreen.kt`
- `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/MyTasksScreen.kt`
- `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskBoardScreen.kt`
- `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskDetailScreen.kt`

---

## Component 3: Sync Queue & Retry Mechanism

### Implementation Status: ❌ 0% Exists (CRITICAL DATA LOSS RISK)

**Current Pattern (Broken):**
```kotlin
// TaskRepository.kt - CURRENT (WRONG):
suspend fun createTask(task: Task): Result<Task> {
    // Step 1: Save to Room (immediate)
    taskDao.insertTask(task)  // ✅ Correct

    // Step 2: Try sync to Supabase
    return try {
        supabaseTaskDataSource.insertTask(task)
        Result.success(task)
    } catch (e: Exception) {
        // ❌ PROBLEM: Says "will retry later" but WON'T!
        Log.w(TAG, "Supabase sync failed, will retry later")
        Result.success(task)  // Returns success even though sync failed
    }
}
```

**What Happens:**
1. User creates task offline
2. Task saves to Room ✅
3. Supabase sync fails (no network) ✅
4. Log says "will retry later" ❌ (it won't!)
5. User closes app
6. Task never syncs to Supabase ❌
7. **PERMANENT DATA LOSS** if Room database cleared ❌

**What Should Happen (Proper Offline-First):**
```kotlin
// TaskRepository.kt - CORRECT PATTERN:
suspend fun createTask(task: Task): Result<Task> {
    // Step 1: Save to Room (immediate)
    taskDao.insertTask(task)

    // Step 2: Try sync to Supabase
    try {
        supabaseTaskDataSource.insertTask(task)
        // Success: sync completed
    } catch (e: Exception) {
        // Step 3: Queue for retry
        syncQueue.add(
            PendingSyncOperation(
                operation = "create_task",
                entityId = task.id,
                payload = Json.encodeToString(task)
            )
        )
        Log.w(TAG, "Sync failed, added to queue")
    }

    return Result.success(task)
}
```

**Required Components:**

```kotlin
// 1. PendingSyncOperation.kt - Entity for sync queue
@Entity(tableName = "pending_syncs")
data class PendingSyncOperation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val operation: String,  // "create_task", "update_user", "delete_project"
    val entityId: String,   // ID of the entity
    val payload: String,    // JSON serialized entity
    val timestamp: Instant = Instant.now(),
    val retryCount: Int = 0,
    val maxRetries: Int = 5
)

// 2. PendingSyncDao.kt - DAO for sync queue
@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_syncs ORDER BY timestamp ASC")
    fun getPendingSyncs(): Flow<List<PendingSyncOperation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sync: PendingSyncOperation)

    @Delete
    suspend fun delete(sync: PendingSyncOperation)

    @Query("UPDATE pending_syncs SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: String)

    @Query("DELETE FROM pending_syncs WHERE retryCount >= maxRetries")
    suspend fun deleteFailedSyncs()
}

// 3. SyncManager.kt - Processes sync queue
@Singleton
class SyncManager @Inject constructor(
    private val syncDao: PendingSyncDao,
    private val networkMonitor: NetworkMonitor,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    // ... other repositories
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Watch for network connectivity
        scope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    processPendingSyncs()
                }
            }
        }
    }

    private suspend fun processPendingSyncs() {
        val pending = syncDao.getPendingSyncs().first()

        pending.forEach { sync ->
            try {
                when (sync.operation) {
                    "create_task" -> {
                        val task = Json.decodeFromString<Task>(sync.payload)
                        taskRepository.syncTask(task)
                    }
                    "update_user" -> {
                        val user = Json.decodeFromString<User>(sync.payload)
                        userRepository.syncUser(user)
                    }
                    "delete_project" -> {
                        projectRepository.syncDelete(sync.entityId)
                    }
                    // ... handle all operations
                }

                // Success: remove from queue
                syncDao.delete(sync)

            } catch (e: Exception) {
                // Retry failed
                if (sync.retryCount < sync.maxRetries) {
                    syncDao.incrementRetry(sync.id)
                } else {
                    // Max retries reached: delete
                    syncDao.delete(sync)
                    Log.e(TAG, "Sync failed permanently for ${sync.operation}")
                }
            }
        }
    }
}
```

**Implementation Steps:**
1. Create `PendingSyncOperation.kt` entity
2. Create `PendingSyncDao.kt` DAO
3. Create `SyncManager.kt` with retry logic
4. Add `sync_operation` methods to all Repositories
5. Modify all Repository methods to use sync queue
6. Add database migration (Room version bump)
7. Test offline scenarios

**Fix Time:** 24 hours
- Create entities/DAOs (4 hours)
- Implement SyncManager (8 hours)
- Wire to 7 Repositories (8 hours)
- Test scenarios (4 hours)

**Files:**
- Need new: `PendingSyncOperation.kt`, `PendingSyncDao.kt`, `SyncManager.kt`
- Update: All 7 Repository files

---

## Component 4: Conflict Resolution

### Implementation Status: ❌ 0% Exists

**Current Behavior (Last Write Wins):**
```kotlin
// Scenario:
// 1. User A updates Task title to "Fix Bug A" (offline)
// 2. User B updates Task title to "Fix Bug B" (offline)
// 3. User A comes online → syncs "Fix Bug A"
// 4. User B comes online → syncs "Fix Bug B" (overwrites A!)
// Result: User A's change silently lost ❌
```

**What's Missing:**
```kotlin
// Task.kt - ADD VERSION FIELD:
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val title: String,
    // ... other fields
    val version: Int = 1,  // ✅ ADD THIS
    val updatedAt: Instant = Instant.now()
)

// TaskRepository.kt - ADD CONFLICT DETECTION:
suspend fun updateTask(task: Task): Result<Unit> {
    // Save locally
    taskDao.updateTask(task)

    // Try sync
    try {
        val remoteTask = supabaseTaskDataSource.getTask(task.id)

        if (remoteTask.version > task.version) {
            // ❌ CONFLICT: Remote is newer
            handleConflict(local = task, remote = remoteTask)
        } else {
            // ✅ OK: Update remote
            supabaseTaskDataSource.updateTask(task.copy(version = task.version + 1))
        }
    } catch (e: Exception) {
        // Queue for retry
    }

    return Result.success(Unit)
}

private suspend fun handleConflict(local: Task, remote: Task) {
    // Show conflict dialog to user
    val resolution = showConflictDialog(local, remote)

    when (resolution) {
        ConflictResolution.KEEP_LOCAL -> {
            // Force update remote with local version
            supabaseTaskDataSource.forceUpdate(local)
        }
        ConflictResolution.KEEP_REMOTE -> {
            // Overwrite local with remote version
            taskDao.updateTask(remote)
        }
        ConflictResolution.MERGE -> {
            // Merge fields (complex)
            val merged = mergeFields(local, remote)
            taskDao.updateTask(merged)
            supabaseTaskDataSource.updateTask(merged)
        }
    }
}
```

**Required Components:**
1. Version field on all entities
2. Conflict detection logic in repositories
3. ConflictResolutionDialog UI component
4. Merge strategy for different fields
5. User notification system

**Fix Time:** 16 hours (complex)
- Add version fields + migration (2 hours)
- Implement conflict detection (4 hours)
- Create ConflictResolutionDialog (4 hours)
- Implement merge strategies (4 hours)
- Test conflict scenarios (2 hours)

**Files:**
- Update: All 11 entities (`Task.kt`, `User.kt`, etc.)
- Update: All 7 Repositories
- Need new: `ConflictResolutionDialog.kt`

---

## Offline Scenarios Testing

### Scenario 1: Create Task Offline

**Steps:**
1. Disable network
2. Create new task "Test Task"
3. Verify task appears in UI
4. Close app
5. Enable network
6. Reopen app
7. Check if task synced to Supabase

**Current Result:**
- ✅ Task saves to Room
- ✅ Task appears in UI
- ❌ If app closed before reconnecting, task never syncs
- ❌ PERMANENT DATA LOSS if Room cleared

**Expected Result:**
- ✅ Task saves to Room
- ✅ Task appears in UI
- ✅ Task added to sync queue
- ✅ When network restored, sync queue processes
- ✅ Task syncs to Supabase (even after app restart)

---

### Scenario 2: Send Message Offline

**Steps:**
1. Disable network
2. Send message "Hello offline"
3. Verify message appears locally
4. Enable network
5. Check if message syncs

**Current Result:**
- ✅ Message saves to Room
- ✅ Message appears in chat
- ⚠️ Sync happens if user stays online
- ❌ If app closes, message lost

**Expected Result:**
- ✅ Message saves to Room
- ✅ Message appears in chat
- ✅ Sync queue ensures delivery
- ✅ Works even after app restart

---

### Scenario 3: Update Profile Offline

**Steps:**
1. Disable network
2. Edit profile (change name)
3. Enable network
4. Check if profile synced

**Current Result:**
- ✅ Profile updates in Room
- ❌ **NEVER SYNCS TO SUPABASE** (UserRepository.kt line 111 bug)
- ❌ Other devices see stale profile
- ❌ PERMANENT DATA LOSS

**Expected Result:**
- ✅ Profile updates in Room
- ✅ Sync queue ensures profile update
- ✅ Other devices see updated profile

---

### Scenario 4: Edit Task (Concurrent)

**Steps:**
1. User A edits task offline (title = "A")
2. User B edits task offline (title = "B")
3. User A comes online → syncs
4. User B comes online → syncs

**Current Result:**
- ❌ Last write wins (User B overwrites User A)
- ❌ User A's changes silently lost
- ❌ No conflict notification

**Expected Result:**
- ✅ Conflict detected (version mismatch)
- ✅ User B sees conflict dialog
- ✅ User B chooses: keep local, keep remote, or merge
- ✅ No data loss

---

## Offline-First Checklist

### Design (Pattern)
- [x] Room updates before Supabase
- [x] Repository abstraction
- [x] Async sync pattern
- [x] Flow-based reactive updates

### Implementation (Execution)
- [ ] NetworkMonitor wired in DI ❌
- [ ] OfflineModeBanner integrated ❌
- [ ] Sync queue exists ❌
- [ ] Retry mechanism works ❌
- [ ] Conflict resolution exists ❌
- [ ] Version fields on entities ❌
- [ ] Offline testing done ❌

**Score: 4/11 (36%)** - Design is correct but implementation incomplete

---

## Recommendations

### Week 1 - Critical Fixes (30 hours)

1. **Wire NetworkMonitor** (30 minutes)
   - Add provider to Module.kt
   - Inject into ViewModels

2. **Integrate OfflineModeBanner** (2 hours)
   - Add to 5 main screens
   - Test visibility on network changes

3. **Implement Sync Queue** (24 hours)
   - Create PendingSyncOperation entity
   - Create SyncManager
   - Wire to all Repositories
   - Test offline scenarios

4. **Fix User Profile Sync** (1 hour)
   - Add supabase sync call in UserRepository
   - Test multi-device profile updates

5. **Fix Activity Tracking** (2 hours)
   - Always track to Room
   - Queue Supabase sync
   - Test offline activity logging

### Week 2 - Advanced Features (16 hours)

6. **Add Conflict Resolution** (16 hours)
   - Add version fields
   - Implement conflict detection
   - Create resolution UI
   - Test concurrent edits

---

## Conclusion

**Offline-First Grade: F (55/100)**

**Strengths:**
- ✅ Architecture design is correct
- ✅ Room-first pattern implemented
- ✅ Async sync pattern used

**Critical Gaps:**
- ❌ NetworkMonitor not wired (app blind to connectivity)
- ❌ No user feedback (OfflineModeBanner unused)
- ❌ No sync queue (data loss risk)
- ❌ No conflict resolution (silent overwrites)

**Impact:**
- Users working offline have poor experience
- No indication of online/offline status
- Offline changes may be lost
- Concurrent edits cause data loss

**Verdict:** Offline-first is **designed correctly** but **implemented incompletely**. The foundation is solid but critical pieces are missing.

**Time to Fix:** 46 hours (1 week with focused effort)

---

**Next:** Read `06-mvvm-violations-and-fixes.md` for architecture compliance audit.
