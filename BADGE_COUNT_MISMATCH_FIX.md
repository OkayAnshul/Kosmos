# Badge Count Mismatch Fix - Complete Guide

**Date:** 2026-01-26
**Issue:** Badges show counts but UI lists are empty (no chats/tasks visible)

---

## Problem Summary

### Symptoms
- ✅ Project badges show "5 Chats, 10 Tasks" (metadata counts)
- ❌ Chat List screen shows "No chats"
- ❌ Tasks List screen shows "No tasks"
- ❌ Data exists in Supabase but not visible in app

### Root Causes Found

#### 1. **Chat Filtering Bug** ✅ FIXED
**File:** `ChatRepository.kt:60-65`

**Old Logic (WRONG):**
```kotlin
rooms.filter { room ->
    room.participantIds.contains(userId) && room.projectId == projectId
}
```

**Problem:**
- Public/general chat rooms have **empty `participantIds`** list
- Filtering by `participantIds.contains(userId)` excluded ALL public rooms
- Only private/direct rooms with explicit participants would show

**New Logic (FIXED):**
```kotlin
rooms.filter { room ->
    // Must be in the same project
    if (room.projectId != projectId) return@filter false

    // Public/general rooms: Show to all project members
    if (!room.isPrivate) return@filter true

    // Private rooms: Only show if user is explicitly in participantIds
    room.participantIds.contains(userId)
}
```

**Explanation:**
- Public rooms (`isPrivate = false`): Show to ALL project members
- Private rooms (`isPrivate = true`): Check `participantIds` list

---

#### 2. **Build Interruption** ⚠️ NEEDS FIX
**Error:**
```
java.lang.NoSuchMethodException:
com.example.kosmos.features.tasks.presentation.redesign.ProjectTasksDataViewModel.<init> []
```

**Problem:**
- Build was interrupted before Hilt/KSP annotation processing completed
- Dagger Hilt factory classes not generated
- App crashes when trying to instantiate `ProjectTasksDataViewModel`

**Solution:** Clean rebuild (see steps below)

---

#### 3. **Potential Sync Issue** ⚠️ NEEDS VERIFICATION
**Question:** Is data actually in Room database?

The badges read from **metadata** (`projects.chatCount`, `projects.taskCount`), but the UI lists read from **actual tables** (`chat_rooms`, `tasks`).

**Possible scenarios:**
1. ✅ Sync completed → Metadata updated → But chat/task tables empty (filtering bug)
2. ❌ Sync failed silently → Metadata stale → No data in tables
3. ❌ Sync never triggered → No data fetched from Supabase

---

## Fix Steps

### Step 1: Clean Build (CRITICAL - Fix Hilt Crash)

**Run this command:**
```bash
cd "/path/to/Kosmos"
./gradlew clean :app:assembleDebug
```

**Why this is needed:**
- Clears stale build artifacts
- Forces KSP to regenerate Hilt factory classes
- Ensures `ProjectTasksDataViewModel_Factory` is generated

**Expected output:**
```
BUILD SUCCESSFUL in Xs
```

**If build fails:**
- Check for compilation errors in the output
- Ensure all imports are correct
- Verify Hilt annotations are present

---

### Step 2: Verify Data Exists in Room Database

Before running the app, let's check if data actually exists.

#### Option A: Use Android Studio Database Inspector
1. Run the app once (after successful build)
2. Open **View → Tool Windows → App Inspection**
3. Select **Database Inspector** tab
4. Select `com.example.kosmos` process
5. Check tables:
   - `chat_rooms` table - how many rows?
   - `tasks` table - how many rows?
   - `projects` table - check `chatCount`, `taskCount` columns

#### Option B: Use Sync Diagnostics Screen (Recommended)
I created a diagnostic screen for you. Add it to your test launcher:

**File:** `ReactScreensTestLauncher.kt`

Add this button:
```kotlin
Button(
    onClick = {
        navController.navigate("sync_diagnostics")
    },
    modifier = Modifier.fillMaxWidth()
) {
    Text("🔍 Sync Diagnostics")
}
```

Add this route in NavHost:
```kotlin
composable("sync_diagnostics") {
    SyncDiagnosticsScreen(
        onBack = { navController.popBackStack() }
    )
}
```

**What it shows:**
- Current user info
- For each project:
  - Metadata counts (from `projects` table) ← This is what badges use
  - Actual counts (from `chat_rooms` and `tasks` tables) ← This is what UI lists use
  - ⚠️ Highlights mismatches
- Manual sync trigger button
- Real-time sync progress

**How to use:**
1. Open Sync Diagnostics screen
2. Look at the table:
   ```
   Project: Alpha
   Chats:  Badge (Metadata): 5    Actual (Table): 0  ⚠️ SYNC ISSUE
   Tasks:  Badge (Metadata): 10   Actual (Table): 0  ⚠️ SYNC ISSUE
   ```
3. If "Actual (Table)" is 0 but "Badge (Metadata)" is >0 → **Sync failed**
4. If both are >0 → **Filtering bug** (already fixed)
5. Click "Trigger Manual Sync" button
6. Wait for sync to complete
7. Check if "Actual (Table)" counts increase

---

### Step 3: Verify Sync is Triggered

Check if `InitialSyncManager.syncAllData()` is called on app launch.

**File:** `MainActivity.kt:147`

Should have this code:
```kotlin
LaunchedEffect(userId) {
    Log.d("MainActivity", "User logged in: $userId - triggering initial sync")

    val progress = withContext(NonCancellable) {
        initialSyncManager.syncAllData(userId)
    }

    withContext(Dispatchers.Main) {
        if (progress.isComplete && !progress.hasErrors) {
            Log.d("MainActivity", "✅ Initial sync complete")
        } else {
            Log.w("MainActivity", "⚠️ Initial sync completed with errors")
        }
    }
}
```

**Check logcat for sync logs:**
```bash
adb logcat | grep -E "(InitialSyncManager|Sync)"
```

**Expected logs:**
```
🔄 Starting project-centric sync for user: <userId>
📥 [1/2] Syncing users...
✅ [1/2] Users synced
📥 [2/2] Syncing projects...
✅ [2/2] Projects synced
📦 Found 3 projects to sync
📥 [1/3] Syncing: Project Alpha
  ✅ Members synced for Project Alpha
  ✅ Chat rooms synced for Project Alpha
  ✅ Tasks synced for Project Alpha
✅ [1/3] Completed: Project Alpha
...
✅ Sync complete in 2500ms - 3/3 projects synced
```

**If you see errors:**
```
❌ Chat rooms sync failed for Project Alpha
⚠️ Sync completed with errors in 3000ms - 1/3 projects had errors
```

This means Supabase calls are failing. Check:
- Network connectivity
- Supabase credentials in `build.gradle.kts`
- RLS policies in Supabase (user might not have SELECT permission)

---

### Step 4: Test Chat Filtering Fix

After successful build and sync:

1. **Open Project Workspace**
2. **Navigate to Chats tab**
3. **Expected behavior:**
   - Public/general rooms (isPrivate = false) → ✅ Visible to all project members
   - Private rooms (isPrivate = true) → ✅ Only visible if you're in participantIds

**Test scenarios:**

| Room Type | isPrivate | participantIds | Should Show? |
|-----------|-----------|----------------|--------------|
| General   | false     | []             | ✅ YES       |
| Channel   | false     | []             | ✅ YES       |
| Direct    | true      | [user1, user2] | ✅ YES (if you're user1 or user2) |
| Direct    | true      | [user3, user4] | ❌ NO (if you're not user3 or user4) |

**Old behavior (bug):**
- General rooms with empty `participantIds` → ❌ Hidden (WRONG!)
- Only private rooms with your userId → ✅ Shown

**New behavior (fixed):**
- General rooms → ✅ Always shown to project members
- Private rooms → ✅ Shown only if you're a participant

---

### Step 5: Test Task Filtering Fix

After successful build and sync:

1. **Open Project Workspace → Tasks tab**
2. **Expected behavior:**
   - Shows ALL tasks in the current project
   - Includes tasks assigned to other team members
   - No filtering by assignee

**Verify:**
- Task 1: Assigned to User A → ✅ Visible
- Task 2: Assigned to User B → ✅ Visible (even if you're User C)
- Task 3: Assigned to you → ✅ Visible

**Compare with "My Tasks" screen:**
- My Tasks screen → Only shows YOUR tasks across ALL projects
- Project Tasks tab → Shows ALL tasks in THIS project (team view)

---

## Debugging Checklist

### If chats still don't show:

1. ✅ Check Room database has chat_rooms data
   ```sql
   SELECT * FROM chat_rooms WHERE projectId = '<your-project-id>'
   ```

2. ✅ Check `isPrivate` field values
   ```sql
   SELECT id, name, projectId, isPrivate, participantIds FROM chat_rooms
   ```

3. ✅ Verify project membership
   ```sql
   SELECT * FROM project_members WHERE projectId = '<your-project-id>' AND userId = '<your-user-id>'
   ```

4. ✅ Check filtering logic in `ChatRepository.kt:67-79`
   - Add log statements:
   ```kotlin
   rooms.filter { room ->
       Log.d("ChatRepo", "Filtering room: ${room.name}, projectId=${room.projectId}, isPrivate=${room.isPrivate}, participantIds=${room.participantIds}")

       if (room.projectId != projectId) {
           Log.d("ChatRepo", "  ❌ Filtered out: Wrong project")
           return@filter false
       }

       if (!room.isPrivate) {
           Log.d("ChatRepo", "  ✅ Included: Public room")
           return@filter true
       }

       val hasAccess = room.participantIds.contains(userId)
       Log.d("ChatRepo", "  ${if (hasAccess) "✅" else "❌"} Private room, hasAccess=$hasAccess")
       hasAccess
   }
   ```

### If tasks still don't show:

1. ✅ Check Room database has tasks data
   ```sql
   SELECT * FROM tasks WHERE projectId = '<your-project-id>'
   ```

2. ✅ Check `TaskDao.getTasksForProjectFlow()` query
   ```kotlin
   @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
   ```

3. ✅ Verify `ProjectTasksScreenReactWrapper` is being used
   - Check `ProjectWorkspaceScreen.kt:156`
   - Should be `ProjectTasksScreenReactWrapper(projectId = projectId, ...)`
   - NOT `MyTasksScreenReactWrapper(...)`

4. ✅ Add log statement in `ProjectTasksScreenReactWrapper.kt:52`
   ```kotlin
   val tasks by taskRepository.getTasksForProjectFlow(projectId)
       .onEach { taskList ->
           Log.d("ProjectTasks", "Loaded ${taskList.size} tasks for project $projectId")
           taskList.forEach { task ->
               Log.d("ProjectTasks", "  - ${task.title} (assigned to: ${task.assignedToName})")
           }
       }
       .collectAsStateWithLifecycle(initialValue = emptyList())
   ```

---

## Summary of Changes Made

### Files Created
1. ✅ `ProjectTasksScreenReactWrapper.kt` - New wrapper for project-scoped task filtering
2. ✅ `SyncDiagnosticsScreen.kt` - Diagnostic tool to compare metadata vs actual data

### Files Modified
1. ✅ `ChatRepository.kt:67-79` - Fixed chat filtering logic (isPrivate check)
2. ✅ `ProjectWorkspaceScreen.kt:29, 156` - Use ProjectTasksScreenReactWrapper instead of MyTasksScreenReactWrapper

---

## Quick Test Script

Run this after successful build:

```bash
# 1. Clean build
./gradlew clean :app:assembleDebug

# 2. Install on device
./gradlew installDebug

# 3. Launch app
adb shell am start -n com.example.kosmos/.MainActivity

# 4. Watch sync logs
adb logcat -c  # Clear log
adb logcat | grep -E "(InitialSyncManager|ChatRepo|ProjectTasks)"

# 5. Check database contents (after sync completes)
adb shell run-as com.example.kosmos sqlite3 /data/data/com.example.kosmos/databases/kosmos.db "SELECT COUNT(*) FROM chat_rooms;"
adb shell run-as com.example.kosmos sqlite3 /data/data/com.example.kosmos/databases/kosmos.db "SELECT COUNT(*) FROM tasks;"
```

---

## Expected Results After Fix

### Badges (Metadata Counts)
- Read from `projects.chatCount` and `projects.taskCount`
- Updated by sync triggers
- Used for bottom nav badges

### Chat Lists (Actual Data)
- Read from `chat_rooms` table
- Filtered by `projectId` and `isPrivate` logic
- Public rooms visible to all project members

### Task Lists (Actual Data)
- Read from `tasks` table
- Filtered by `projectId` only (no assignee filter in project view)
- All project tasks visible to all project members

### Sync Diagnostics
- Shows comparison: Metadata vs Actual
- Highlights mismatches (sync issues)
- Manual sync trigger for testing

---

## Next Steps

1. **Run clean build** (`./gradlew clean :app:assembleDebug`)
2. **Install and launch app**
3. **Open Sync Diagnostics screen** (if you added it)
4. **Check if data exists** in actual tables
5. **If data is missing** → Trigger manual sync
6. **If data exists but not showing** → Check filter logs
7. **Report findings** (metadata counts, actual counts, sync errors)

---

## Contact

If issues persist after following this guide:
1. Share logcat output of sync process
2. Share Sync Diagnostics screen screenshot
3. Share database query results (chat_rooms, tasks row counts)

**Date:** 2026-01-26
**Status:** Fix implemented, awaiting clean build + testing
