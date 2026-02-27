# Database and Sync Review

**Date:** January 23, 2026
**Assessment:** Database Integrity Grade: D+ (60/100)

---

## Executive Summary

The Kosmos app has **critical database schema mismatches** between Room (local) and Supabase (remote):

### Critical Issues (P0)
1. ❌ **TaskActivity table missing in Supabase** → Task history sync fails
2. ❌ **User profile updates never sync** → Multi-device data loss
3. ❌ **RLS policies use wrong column names** → Access denied errors
4. ❌ **Realtime subscriptions client-side filtered** → Battery drain
5. ❌ **No foreign key constraints in Room** → Orphaned data
6. ❌ **Destructive migrations enabled** → Data wiped on upgrades
7. ❌ **Activity tracking conditional on sync success** → Audit trail incomplete

**Verdict:** Database design is sound but implementation has catastrophic gaps.

---

## Issue 1: TaskActivity Table Missing in Supabase

### Status: ❌ CRITICAL (DATA LOSS)

**Room Has It:**
```kotlin
// app/src/main/java/com/example/kosmos/core/models/TaskActivity.kt
@Entity(tableName = "task_activity")
data class TaskActivity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val userId: String,
    val action: String,  // "created", "updated", "completed", etc.
    val fieldChanged: String?,  // "status", "assignee", etc.
    val oldValue: String?,
    val newValue: String?,
    val timestamp: Instant = Instant.now(),
    val description: String
)

// TaskActivityDao.kt - 8 query methods
@Dao
interface TaskActivityDao {
    @Query("SELECT * FROM task_activity WHERE taskId = :taskId ORDER BY timestamp DESC")
    fun getActivitiesForTask(taskId: String): Flow<List<TaskActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: TaskActivity)

    // ... 6 more methods
}
```

**Supabase Missing It:**
```sql
-- documents/04-DATABASE/SCHEMA_FIX_COMPLETE_V2.sql
-- ❌ NO task_activity TABLE DEFINITION FOUND!

-- Only these tables exist:
CREATE TABLE users (...);
CREATE TABLE projects (...);
CREATE TABLE project_members (...);
CREATE TABLE tasks (...);
CREATE TABLE chat_rooms (...);
CREATE TABLE messages (...);
```

**What Happens:**
```kotlin
// TaskRepository.kt line 280
suspend fun trackActivity(activity: TaskActivity) {
    taskActivityDao.insertActivity(activity)  // ✅ Saves to Room

    try {
        supabaseTaskActivityDataSource.insertActivity(activity)  // ❌ FAILS!
        // Table 'task_activity' doesn't exist in Supabase
    } catch (e: Exception) {
        Log.w(TAG, "Activity sync failed", e)  // Silent failure
    }
}
```

**Impact:**
1. Task history saves to Room (local cache)
2. Sync to Supabase fails silently
3. Other users never see activity log
4. If Room cleared → **PERMANENT DATA LOSS**

**Fix:** Create `task_activity` table in Supabase

**SQL Script:** `create_task_activity_table.sql` (see SQL Scripts section)

**Fix Time:** 1 hour

---

## Issue 2: User Profile Updates Never Sync

### Status: ❌ CRITICAL (DATA LOSS)

**The Bug:**
```kotlin
// UserRepository.kt line 111
suspend fun saveUser(user: User): Result<Unit> {
    userDao.insertUser(user)  // ✅ Room updated

    // ❌ MISSING: Supabase sync!
    // supabaseUserDataSource.update(user)  // THIS LINE DOESN'T EXIST!

    return Result.success(Unit)
}
```

**What Happens:**
1. User edits profile (name, bio, avatar)
2. Changes save to Room (local device only)
3. **Supabase never updated**
4. Other devices see stale profile
5. If user logs in on new device → old profile loaded
6. **PERMANENT DATA LOSS** (local changes never synced)

**Impact:**
- Multi-device users experience constant profile resets
- Avatar uploads save locally but never reach server
- Profile changes appear to work but don't persist

**Fix:**
```kotlin
// UserRepository.kt - ADD THIS:
suspend fun saveUser(user: User): Result<Unit> {
    userDao.insertUser(user)  // Local

    return try {
        supabaseUserDataSource.update(user)  // ✅ ADD SYNC
        Result.success(Unit)
    } catch (e: Exception) {
        // Queue for retry
        Result.success(Unit)  // Optimistic
    }
}
```

**Fix Time:** 1 hour

**File:** `app/src/main/java/com/example/kosmos/data/repository/UserRepository.kt`

---

## Issue 3: RLS Policies Use Wrong Column Names

### Status: ❌ CRITICAL (ACCESS DENIED)

**The Bug:**
```sql
-- documents/04-DATABASE/SCHEMA_FIX_COMPLETE_V2.sql line 374

-- ❌ WRONG: Policy uses 'created_by' but table has 'created_by_id'
CREATE POLICY "Users can view their own tasks"
ON tasks FOR SELECT
USING (created_by = auth.uid());  -- ❌ Column 'created_by' doesn't exist!

-- ❌ WRONG: Policy uses 'assigned_to_user_id' but table has 'assigned_to_id'
CREATE POLICY "Users can view assigned tasks"
ON tasks FOR SELECT
USING (assigned_to_user_id = auth.uid());  -- ❌ Column 'assigned_to_user_id' doesn't exist!
```

**Actual Table Schema:**
```sql
-- tasks table columns (line 250):
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id),
    created_by_id UUID NOT NULL REFERENCES users(id),  -- ✅ Note: 'created_by_id'
    assigned_to_id UUID REFERENCES users(id),          -- ✅ Note: 'assigned_to_id'
    ...
);
```

**What Happens:**
1. User creates task
2. RLS policy checks `created_by = auth.uid()`
3. Column `created_by` doesn't exist → Policy fails
4. User gets "permission denied" error
5. Task may not be accessible

**Impact:**
- Tasks may be inaccessible due to RLS failures
- Users can't view tasks they created or were assigned
- Intermittent "permission denied" errors

**Fix:**
```sql
-- ✅ CORRECT:
CREATE POLICY "Users can view their own tasks"
ON tasks FOR SELECT
USING (created_by_id = auth.uid());  -- Use correct column name

CREATE POLICY "Users can view assigned tasks"
ON tasks FOR SELECT
USING (assigned_to_id = auth.uid());  -- Use correct column name
```

**Fix Time:** 15 minutes

**SQL Script:** `fix_rls_policies.sql` (see SQL Scripts section)

---

## Issue 4: Realtime Subscriptions Client-Side Filtered

### Status: ❌ CRITICAL (BATTERY DRAIN)

**The Bug:**
```kotlin
// SupabaseRealtimeManager.kt line 143
fun subscribeToMessages(chatRoomId: String) {
    val channel = supabaseClient.channel("messages")

    // ❌ NO SERVER-SIDE FILTER!
    channel.postgresChangeFlow<PostgresAction> {
        table = "messages"
        // MISSING: filter = "chat_room_id=eq.$chatRoomId"
    }.collect { action ->
        val messageChatRoomId = action.record["chat_room_id"]

        // ❌ FILTERS CLIENT-SIDE (after receiving ALL messages!)
        if (messageChatRoomId == chatRoomId) {
            handleMessageInsert(action.record)
        }
    }
}
```

**What Happens:**
1. Subscribe to "messages" table
2. Supabase sends **ALL message inserts** (entire database)
3. App receives 10,000 message events
4. Filters client-side to show only 10 relevant messages
5. **Massive battery drain** + **network waste**

**Impact:**
- Battery drains rapidly (constant WebSocket traffic)
- Network usage explodes
- App becomes sluggish
- Scales terribly (1M messages = 1M client events)

**Fix:**
```kotlin
// ✅ SERVER-SIDE FILTER:
fun subscribeToMessages(chatRoomId: String) {
    val channel = supabaseClient.channel("messages:$chatRoomId")

    channel.postgresChangeFlow<PostgresAction> {
        table = "messages"
        filter = "chat_room_id=eq.$chatRoomId"  // ✅ SERVER-SIDE FILTER
    }.collect { action ->
        // Now only receives relevant messages
        handleMessageInsert(action.record)
    }
}
```

**Fix Time:** 1 hour (fix all realtime subscriptions)

**File:** `app/src/main/java/com/example/kosmos/data/realtime/SupabaseRealtimeManager.kt`

---

## Issue 5: No Foreign Key Constraints in Room

### Status: ❌ HIGH (DATA INTEGRITY)

**Current Room Entities (No FK Constraints):**
```kotlin
// Message.kt - NO FOREIGN KEY
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val chatRoomId: String,  // ❌ No FK constraint
    val userId: String,      // ❌ No FK constraint
    val content: String,
    ...
)
```

**What Happens:**
1. ChatRoom deleted (ID = "abc")
2. Messages with chatRoomId = "abc" remain in database
3. **Orphaned messages** accumulate
4. Database bloat + query errors

**Fix:**
```kotlin
// ✅ WITH FOREIGN KEY:
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatRoom::class,
            parentColumns = ["id"],
            childColumns = ["chatRoomId"],
            onDelete = ForeignKey.CASCADE  // Auto-delete messages when chat deleted
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class Message(...)
```

**Entities Needing FK Constraints:**
1. Message → ChatRoom, User
2. Task → Project, User (creator, assignee)
3. ProjectMember → Project, User
4. TaskActivity → Task, User
5. TaskDependency → Task (parent, dependent)
6. TimeEntry → Task, User
7. Milestone → Project

**Fix Time:** 2 hours
- Add FK annotations (1 hour)
- Create migration script (1 hour)

**Files:** All 11 entity files

---

## Issue 6: Destructive Migrations Enabled

### Status: ❌ CRITICAL (DATA LOSS ON UPGRADE)

**The Bug:**
```kotlin
// Module.kt line 67
Room.databaseBuilder(context, KosmosDatabase::class.java, "kosmos.db")
    .fallbackToDestructiveMigration()  // ❌ DELETES ALL DATA ON SCHEMA CHANGE
    .build()
```

**What Happens:**
1. App version 1.0 ships (database version 5)
2. User creates 100 tasks, 50 projects, 200 messages
3. App version 1.1 ships (database version 6)
4. User updates app
5. **ALL DATA DELETED** (fallbackToDestructiveMigration)
6. User loses everything

**Impact:**
- Any schema change wipes all local data
- Users lose offline changes
- 1-star reviews: "Lost all my data after update"

**Fix:**
```kotlin
// ✅ REMOVE DESTRUCTIVE MIGRATION:
Room.databaseBuilder(context, KosmosDatabase::class.java, "kosmos.db")
    // .fallbackToDestructiveMigration()  // ❌ DELETE THIS LINE
    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, ...)  // ✅ Add proper migrations
    .build()
```

**Migration Example:**
```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE tasks ADD COLUMN milestone_id TEXT"
        )
    }
}
```

**Fix Time:** 5 minutes to remove line + 4 hours to write migrations

**File:** `app/src/main/java/com/example/kosmos/Module.kt`

---

## Issue 7: Activity Tracking Conditional on Sync

### Status: ❌ CRITICAL (AUDIT TRAIL INCOMPLETE)

**The Bug:**
```kotlin
// TaskRepository.kt line 266
suspend fun updateTaskStatus(taskId: String, newStatus: String): Result<Unit> {
    // Update Room
    taskDao.updateTaskStatus(taskId, newStatus)

    // Try sync to Supabase
    val syncSuccess = try {
        supabaseTaskDataSource.updateStatus(taskId, newStatus)
        true
    } catch (e: Exception) {
        false
    }

    // ❌ BUG: Only track activity if sync succeeded!
    if (syncSuccess) {
        trackActivity(TaskActivity(
            taskId = taskId,
            action = "status_changed",
            newValue = newStatus
        ))
    }

    return Result.success(Unit)
}
```

**What Happens:**
1. User changes task status offline
2. Supabase sync fails (no network)
3. `syncSuccess = false`
4. **Activity NOT tracked**
5. User comes online → Task syncs but activity log missing
6. **Audit trail incomplete**

**Impact:**
- Activity log only works when online
- Offline changes have no history
- Compliance issues (audit trail required)

**Fix:**
```kotlin
// ✅ ALWAYS TRACK (offline-first):
suspend fun updateTaskStatus(taskId: String, newStatus: String): Result<Unit> {
    // Update Room
    taskDao.updateTaskStatus(taskId, newStatus)

    // ✅ Track activity FIRST (unconditional)
    trackActivity(TaskActivity(
        taskId = taskId,
        action = "status_changed",
        newValue = newStatus
    ))

    // Try sync (doesn't affect activity tracking)
    try {
        supabaseTaskDataSource.updateStatus(taskId, newStatus)
    } catch (e: Exception) {
        // Queue for retry
    }

    return Result.success(Unit)
}
```

**Fix Time:** 2 hours (fix all activity tracking calls)

**File:** `app/src/main/java/com/example/kosmos/data/repository/TaskRepository.kt`

---

## Database Schema Comparison

### Room Schema (Local)

**11 Tables:**
1. `users` - 10 columns ✅
2. `projects` - 12 columns ✅
3. `project_members` - 8 columns ✅
4. `tasks` - 15 columns ✅
5. `chat_rooms` - 8 columns ✅
6. `messages` - 9 columns ✅
7. `task_activity` - 9 columns ❌ MISSING IN SUPABASE
8. `milestones` - 8 columns ✅
9. `task_dependencies` - 5 columns ✅
10. `time_entries` - 7 columns ✅
11. `user_settings` - 8 columns ⚠️ Unused

### Supabase Schema (Remote)

**6 Tables:**
1. `users` - 10 columns ✅
2. `projects` - 12 columns ✅
3. `project_members` - 8 columns ✅
4. `tasks` - 15 columns ✅
5. `chat_rooms` - 8 columns ✅
6. `messages` - 9 columns ✅

**MISSING:**
- ❌ `task_activity` (critical!)
- ❌ `milestones` (feature won't sync)
- ❌ `task_dependencies` (feature won't sync)
- ❌ `time_entries` (feature won't sync)
- ❌ `user_settings` (settings won't persist)

---

## Missing Indexes (Performance)

**Room Queries Without Indexes:**
```kotlin
// TaskDao.kt - Slow query (no index on project_id)
@Query("SELECT * FROM tasks WHERE project_id = :projectId")
suspend fun getTasksForProject(projectId: String): List<Task>

// MessageDao.kt - Slow query (no index on chat_room_id)
@Query("SELECT * FROM messages WHERE chat_room_id = :chatRoomId ORDER BY timestamp DESC")
suspend fun getMessagesForChatRoom(chatRoomId: String): List<Message>
```

**Fix:** Add indexes
```kotlin
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["project_id"]),  // ✅ ADD
        Index(value = ["assigned_to_id"]),
        Index(value = ["status"]),
        Index(value = ["created_at"])
    ]
)
data class Task(...)
```

**SQL Script:** `add_indexes.sql` (see SQL Scripts section)

**Fix Time:** 2 hours

---

## Recommendations

### Week 1 - Critical Fixes (P0)

1. **Create task_activity table in Supabase** (1 hour)
   - Run SQL script
   - Test activity sync

2. **Fix User Profile Sync** (1 hour)
   - Add supabase sync call
   - Test multi-device

3. **Fix RLS Policies** (15 minutes)
   - Update column names
   - Test access

4. **Fix Realtime Filtering** (1 hour)
   - Add server-side filters
   - Test battery usage

5. **Remove Destructive Migration** (5 minutes)
   - Delete line from Module.kt
   - Write proper migrations (4 hours)

6. **Fix Activity Tracking** (2 hours)
   - Always track (unconditional)
   - Test offline scenarios

7. **Add Foreign Keys** (2 hours)
   - Add FK annotations
   - Create migration

**Total Week 1:** 12 hours

### Week 2 - Performance & Integrity

8. **Add Database Indexes** (2 hours)
   - Room indexes
   - Supabase indexes

9. **Create Missing Tables** (4 hours)
   - milestones
   - task_dependencies
   - time_entries
   - user_settings

10. **Test Data Integrity** (4 hours)
    - Orphaned data checks
    - Sync verification
    - Multi-device tests

**Total Week 2:** 10 hours

---

## Conclusion

**Database Integrity Grade: D+ (60/100)**

**Critical Issues:**
1. ❌ TaskActivity table missing (data loss)
2. ❌ User profile never syncs (data loss)
3. ❌ RLS policies broken (access denied)
4. ❌ Realtime filtering broken (battery drain)
5. ❌ No foreign keys (orphaned data)
6. ❌ Destructive migrations (data wiped)
7. ❌ Activity tracking offline broken (audit trail incomplete)

**Verdict:** Database design is sound but implementation has **catastrophic gaps** that must be fixed before launch.

**Time to Fix P0:** 12 hours (1-2 days)

---

**Next:** Read `09-cleanup-and-refactor-plan.md` for code quality improvements.
