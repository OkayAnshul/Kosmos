# Schema Optimization - Testing & Verification Checklist

**Purpose**: Comprehensive testing guide for schema optimization implementation
**Date**: 2026-01-25
**Estimated Time**: 2-3 hours for complete verification

---

## 🎯 Overview

This checklist ensures all schema optimizations work correctly:
- ✅ Version columns added and functional
- ✅ Optimistic locking prevents data loss
- ✅ Metadata triggers auto-update counts
- ✅ Array types verified and working
- ✅ No regressions in existing functionality

**Test in this order:**
1. Database Schema Tests (Supabase SQL)
2. Data Source Tests (Kotlin code)
3. Repository Tests (Integration)
4. End-to-End Tests (Multi-device)

---

## 📋 Phase 1: Database Schema Verification

### Test 1.1: Version Columns Created

**File**: `documents/04-DATABASE/ADD_VERSION_COLUMNS.sql`

**Steps:**
1. [ ] Open Supabase SQL Editor
2. [ ] Copy entire contents of ADD_VERSION_COLUMNS.sql
3. [ ] Run Step 1 (Projects version column)
4. [ ] Run Step 2 (Tasks version column)
5. [ ] Run Step 3 (Verification query)

**Expected Output:**
```
table_name | column_name | data_type | column_default | is_nullable
-----------+-------------+-----------+----------------+-------------
projects   | version     | integer   | 1              | NO
tasks      | version     | integer   | 1              | NO
```

**Success Criteria:**
- [ ] Both tables have version column
- [ ] Default value is 1
- [ ] Column is NOT NULL
- [ ] Indexes created successfully

**If Failed:**
- Check Supabase error message
- Verify you have WRITE permissions
- Try running each ALTER TABLE separately
- Check if columns already exist

---

### Test 1.2: Version Increment Test

**File**: `documents/04-DATABASE/ADD_VERSION_COLUMNS.sql` (Step 4)

**Steps:**
1. [ ] Run Test 1 query (existing rows check)
2. [ ] Run Test 2 query (version increment simulation)

**Expected Output:**
```
Test Project: <uuid> - Old Version: 1, New Version: 2
Test passed - version incremented correctly (rolled back)
```

**Success Criteria:**
- [ ] All existing projects have version = 1
- [ ] All existing tasks have version = 1
- [ ] Test increment works (1 → 2)
- [ ] Rollback works (reverted to 1)

**If Failed:**
- Check if projects table is empty (no test data)
- Verify version column has default value
- Check for constraint violations

---

### Test 1.3: Metadata Triggers Created

**File**: `documents/04-DATABASE/ADD_METADATA_TRIGGERS.sql`

**Steps:**
1. [ ] Run Trigger 1 creation (member_count)
2. [ ] Run Trigger 2 creation (chat_count)
3. [ ] Run Trigger 3 creation (task_counts)
4. [ ] Run Trigger 4 creation (last_activity_at)
5. [ ] Run verification query

**Expected Output:**
```
trigger_name                       | event_manipulation | event_object_table
-----------------------------------+--------------------+-------------------
project_member_count_trigger       | INSERT,UPDATE,DELETE | project_members
project_chat_count_trigger         | INSERT,DELETE      | chat_rooms
project_task_counts_trigger        | INSERT,UPDATE,DELETE | tasks
project_activity_on_message_trigger | INSERT            | messages
```

**Success Criteria:**
- [ ] 4 triggers created
- [ ] 4 functions created
- [ ] Triggers attached to correct tables
- [ ] No SQL errors

**If Failed:**
- Check function syntax errors
- Verify table names match schema
- Check PLPGSQL is enabled
- Ensure DROP IF EXISTS worked

---

### Test 1.4: Initial Count Synchronization

**File**: `documents/04-DATABASE/ADD_METADATA_TRIGGERS.sql` (Initial Sync section)

**Steps:**
1. [ ] Run member_count sync UPDATE
2. [ ] Run chat_count sync UPDATE
3. [ ] Run task counts sync UPDATE
4. [ ] Run verification query

**Expected Output:**
```
id   | cached_members | actual_members | cached_tasks | actual_tasks
-----+----------------+----------------+--------------+-------------
uuid | 5              | 5              | 12           | 12
```
(All cached counts match actual counts)

**Success Criteria:**
- [ ] No count mismatches
- [ ] All projects updated
- [ ] No NULL counts
- [ ] Verification query shows 100% match

**If Failed:**
- Some counts may have been wrong before
- This is expected - the sync fixes them
- Verify after sync that counts now match

---

### Test 1.5: Array Types Verification

**File**: `documents/04-DATABASE/VERIFY_ARRAY_TYPES.sql`

**Steps:**
1. [ ] Run Step 1 (Check current data types)
2. [ ] Run Step 2 (Sample data inspection)
3. [ ] Run Step 3 (Test array operators)

**Expected Output (if TEXT[]):**
```
table_name | column_name   | data_type | status
-----------+---------------+-----------+---------------------------
projects   | tech_stack    | ARRAY     | ✅ Array type (optimal)
projects   | tags          | ARRAY     | ✅ Array type (optimal)
```

**Expected Output (if TEXT):**
```
table_name | column_name   | data_type | status
-----------+---------------+-----------+---------------------------
projects   | tech_stack    | text      | ⚠️  Text type (requires conversion)
```

**Success Criteria:**
- [ ] Data type confirmed (ARRAY or text)
- [ ] Sample data shows correct format
- [ ] If ARRAY: Array operators work
- [ ] If text: Decision made on conversion

**If TEXT (Decision Required):**
- [ ] Review conversion needs
- [ ] Backup table before conversion
- [ ] Run conversion script (if needed)
- [ ] Verify no data loss

---

## 📋 Phase 2: Android Code Verification

### Test 2.1: SupabaseTaskDataSource Version Handling

**File**: `app/src/main/java/com/example/kosmos/data/datasource/SupabaseTaskDataSource.kt`

**Manual Code Review:**
1. [ ] Open SupabaseTaskDataSource.kt
2. [ ] Find updateTask() method (line ~49)
3. [ ] Verify version increment: `set("version", newVersion)`
4. [ ] Verify version filter: `eq("version", task.version)`
5. [ ] Check log message includes version: `"version ${task.version} → $newVersion"`

**Success Criteria:**
- [ ] updateTask() has version increment
- [ ] updateTask() has version filter
- [ ] updateTaskStatus() has currentVersion parameter
- [ ] updateTaskStatus() has version increment
- [ ] updateTaskStatus() has version filter
- [ ] parent_task_id field included in updateTask()

**Code Snippet Check:**
```kotlin
// Should see this in updateTask()
val newVersion = task.version + 1
set("version", newVersion)
filter {
    eq("id", task.id)
    eq("version", task.version)  // Optimistic lock
}
```

---

### Test 2.2: SupabaseProjectDataSource Version Handling

**File**: `app/src/main/java/com/example/kosmos/data/datasource/SupabaseProjectDataSource.kt`

**Manual Code Review:**
1. [ ] Open SupabaseProjectDataSource.kt
2. [ ] Find update() method (line ~51)
3. [ ] Verify version increment
4. [ ] Verify version filter
5. [ ] Check updateStatus() has currentVersion parameter

**Success Criteria:**
- [ ] update() has version increment
- [ ] update() has version filter
- [ ] updateStatus() has currentVersion parameter
- [ ] updateStatus() has version increment and filter
- [ ] Log messages include version changes

**Code Snippet Check:**
```kotlin
// Should see this in update()
val newVersion = project.version + 1
set("version", newVersion)
filter {
    eq("id", project.id)
    eq("version", project.version)
}
Result.success(project.copy(version = newVersion))
```

---

### Test 2.3: Breaking Changes in Method Signatures

**Check these files need updates:**

**TaskRepository.kt** (if exists):
```kotlin
// OLD signature (will break):
// updateTaskStatus(taskId, status, updatedAt)

// NEW signature required:
// updateTaskStatus(taskId, status, updatedAt, currentVersion)
```

**ProjectRepository.kt** (if exists):
```kotlin
// OLD signature (will break):
// updateStatus(projectId, status)

// NEW signature required:
// updateStatus(projectId, status, currentVersion)
```

**Success Criteria:**
- [ ] All calls to updateTaskStatus include currentVersion
- [ ] All calls to updateStatus include currentVersion
- [ ] No compilation errors
- [ ] Tests updated (if they exist)

---

## 📋 Phase 3: Integration Testing

### Test 3.1: Task Version Increment

**Prerequisites:**
- [ ] ADD_VERSION_COLUMNS.sql executed
- [ ] Android app connected to Supabase
- [ ] At least 1 test task exists

**Steps:**
1. [ ] Get initial task from Supabase
2. [ ] Note current version (should be 1)
3. [ ] Update task title via app
4. [ ] Check Supabase for new version
5. [ ] Check Android logs for version increment message

**Expected Behavior:**
```
// Initial fetch
Task: id=abc, version=1

// After update
Task: id=abc, version=2

// Android log
"Task updated successfully: id=abc, version 1 → 2"
```

**Success Criteria:**
- [ ] Version increments from 1 to 2
- [ ] Log message shows version change
- [ ] No errors in Supabase logs
- [ ] No errors in Android logs

**SQL Verification:**
```sql
SELECT id, title, version FROM tasks WHERE id = 'abc';
-- Should show version = 2
```

---

### Test 3.2: Project Version Increment

**Steps:**
1. [ ] Get initial project from Supabase
2. [ ] Note current version
3. [ ] Update project name via app
4. [ ] Check Supabase for new version
5. [ ] Verify log message

**Expected Behavior:**
```
// Initial
Project: id=xyz, version=1

// After update
Project: id=xyz, version=2

// Android log
"Project updated successfully: id=xyz, version 1 → 2"
```

**Success Criteria:**
- [ ] Version increments correctly
- [ ] Log shows version change
- [ ] No sync errors

---

### Test 3.3: Conflict Detection (Critical)

**This is the MAIN test for optimistic locking**

**Prerequisites:**
- [ ] 2 devices or 2 app instances
- [ ] Same task loaded on both
- [ ] Task version = 5 (example)

**Steps:**
1. [ ] Device A: Load task (version 5)
2. [ ] Device B: Load same task (version 5)
3. [ ] Device A: Update title to "Version A" → version becomes 6
4. [ ] Device B: Update title to "Version B" (still thinks version is 5)
5. [ ] Device B update should FAIL (version mismatch)

**Expected Behavior:**
```
// Device A update succeeds
Task: id=abc, version=5 → 6, title="Version A"

// Device B update fails
Error: Version conflict detected (expected version 5, but found 6)
// OR
Supabase returns 0 rows updated (version mismatch)
```

**Success Criteria:**
- [ ] Device A update succeeds
- [ ] Device B update fails or throws error
- [ ] Final version is 6 (not 7)
- [ ] Title is "Version A" (Device B didn't overwrite)
- [ ] User sees conflict message (if UI implemented)

**If This Fails:**
- ❌ Optimistic locking NOT working
- Check version filter in data source
- Check Supabase column exists
- Check logs for version mismatch

---

### Test 3.4: Metadata Trigger - Member Count

**Prerequisites:**
- [ ] ADD_METADATA_TRIGGERS.sql executed
- [ ] Test project exists

**Steps:**
1. [ ] Get project, note member_count (e.g., 5)
2. [ ] Add new member via app or SQL
3. [ ] Refresh project
4. [ ] Verify member_count incremented to 6
5. [ ] Remove member
6. [ ] Verify member_count decremented to 5

**SQL Test:**
```sql
-- Get initial count
SELECT id, member_count FROM projects WHERE id = 'test-project';
-- member_count = 5

-- Add member
INSERT INTO project_members (project_id, user_id, role, is_active)
VALUES ('test-project', 'new-user', 'MEMBER', TRUE);

-- Check count auto-incremented
SELECT id, member_count FROM projects WHERE id = 'test-project';
-- member_count = 6 (automatic!)

-- Remove member
DELETE FROM project_members WHERE project_id = 'test-project' AND user_id = 'new-user';

-- Check count auto-decremented
SELECT id, member_count FROM projects WHERE id = 'test-project';
-- member_count = 5 (automatic!)
```

**Success Criteria:**
- [ ] member_count increases on INSERT
- [ ] member_count decreases on DELETE
- [ ] No manual UPDATE needed
- [ ] Works from both app and SQL

**If Failed:**
- Check trigger created correctly
- Check trigger fired (Supabase logs)
- Verify project_id foreign key correct

---

### Test 3.5: Metadata Trigger - Task Counts

**Steps:**
1. [ ] Get project task counts (task_count, completed_task_count, pending_task_count)
2. [ ] Create new task with status=TODO
3. [ ] Verify task_count +1, pending_task_count +1
4. [ ] Update task status to DONE
5. [ ] Verify completed_task_count +1, pending_task_count -1
6. [ ] Delete task
7. [ ] Verify task_count -1, completed_task_count -1

**SQL Test:**
```sql
-- Initial state
SELECT task_count, pending_task_count, completed_task_count
FROM projects WHERE id = 'test-project';
-- Example: 10, 7, 3

-- Create TODO task
INSERT INTO tasks (project_id, title, status, priority, created_by_id)
VALUES ('test-project', 'Test Task', 'TODO', 'MEDIUM', 'user-id');

-- Check counts
SELECT task_count, pending_task_count, completed_task_count
FROM projects WHERE id = 'test-project';
-- Should be: 11, 8, 3

-- Mark DONE
UPDATE tasks SET status = 'DONE' WHERE title = 'Test Task';

-- Check counts
SELECT task_count, pending_task_count, completed_task_count
FROM projects WHERE id = 'test-project';
-- Should be: 11, 7, 4

-- Delete task
DELETE FROM tasks WHERE title = 'Test Task';

-- Check counts
SELECT task_count, pending_task_count, completed_task_count
FROM projects WHERE id = 'test-project';
-- Should be: 10, 7, 3 (back to original)
```

**Success Criteria:**
- [ ] task_count updates on create/delete
- [ ] pending_task_count updates on status change
- [ ] completed_task_count updates on status change
- [ ] Counts always accurate
- [ ] No manual code needed

---

### Test 3.6: Metadata Trigger - Last Activity

**Steps:**
1. [ ] Get project last_activity_at timestamp
2. [ ] Wait 5 seconds
3. [ ] Send message in project chat room
4. [ ] Refresh project
5. [ ] Verify last_activity_at updated to recent timestamp

**SQL Test:**
```sql
-- Get initial timestamp
SELECT id, last_activity_at FROM projects WHERE id = 'test-project';
-- Note the timestamp

-- Send message (get chat_room_id from chat_rooms table)
INSERT INTO messages (chat_room_id, sender_id, content)
VALUES ('chat-room-id', 'user-id', 'Test message');

-- Check timestamp updated
SELECT id, last_activity_at FROM projects WHERE id = 'test-project';
-- Should be > previous timestamp (recent)
```

**Success Criteria:**
- [ ] last_activity_at updates on message send
- [ ] Timestamp is current (recent)
- [ ] Works from app and SQL
- [ ] No manual update needed

---

## 📋 Phase 4: End-to-End Testing

### Test 4.1: Multi-Device Sync with Versions

**Setup:**
- [ ] 2 Android devices or emulators
- [ ] Both logged into same account
- [ ] Same project loaded on both

**Scenario 1: Sequential Updates (Should Work)**
1. [ ] Device A: Update task title
2. [ ] Wait for sync
3. [ ] Device B: Refresh, see updated title
4. [ ] Device B: Update task description
5. [ ] Device A: Refresh, see updated description

**Success Criteria:**
- [ ] Both updates succeed
- [ ] Version increments: 1→2→3
- [ ] Both devices show final state
- [ ] No conflicts

---

**Scenario 2: Concurrent Updates (Should Conflict)**
1. [ ] Both devices: Load same task (version 5)
2. [ ] Device A: Offline mode ON
3. [ ] Device B: Offline mode ON
4. [ ] Device A: Update title to "A"
5. [ ] Device B: Update title to "B"
6. [ ] Device A: Online mode ON → syncs first (version 6)
7. [ ] Device B: Online mode ON → sync fails (conflict)

**Success Criteria:**
- [ ] Device A sync succeeds (version 5→6)
- [ ] Device B sync fails (version conflict)
- [ ] User sees conflict resolution dialog
- [ ] Can choose to keep A, B, or merge
- [ ] Final state is consistent

**If No Conflict Dialog:**
- Check repository catches version mismatch
- Verify ConflictException thrown
- Implement conflict UI if missing

---

### Test 4.2: Offline → Online Sync

**Steps:**
1. [ ] Device offline
2. [ ] Update task locally (Room only)
3. [ ] Version should increment in Room (5→6)
4. [ ] Go online
5. [ ] Sync to Supabase
6. [ ] Verify Supabase version matches Room (6)

**Success Criteria:**
- [ ] Local version increments offline
- [ ] Sync succeeds when online
- [ ] Supabase version matches Room
- [ ] No version conflicts

---

### Test 4.3: Metadata Accuracy After Sync

**Steps:**
1. [ ] Create project with 5 members, 10 tasks
2. [ ] Go offline
3. [ ] Add 2 members, create 3 tasks
4. [ ] Go online, sync
5. [ ] Check metadata counts

**Expected:**
```
member_count: 7 (was 5, added 2)
task_count: 13 (was 10, added 3)
pending_task_count: 13 (if all TODO)
```

**Success Criteria:**
- [ ] Counts accurate after offline changes
- [ ] Triggers work after sync
- [ ] No count mismatches
- [ ] last_activity_at current

---

## 🐛 Troubleshooting Guide

### Issue: Version column doesn't exist

**Symptoms:**
- Error: "column version does not exist"
- Updates fail with SQL error

**Fix:**
1. Run ADD_VERSION_COLUMNS.sql in Supabase
2. Verify column created with verification query
3. Restart app (clear cache)

---

### Issue: Optimistic locking not working

**Symptoms:**
- Concurrent edits both succeed
- No conflict detection
- Last write wins

**Debug:**
1. Check version filter in data source:
   ```kotlin
   filter {
       eq("id", task.id)
       eq("version", task.version)  // This line
   }
   ```
2. Check version column exists in Supabase
3. Check logs show version increment
4. Verify updateTask() increments version

**Root Cause:**
- Missing version filter → no conflict check
- Version not incremented → same version always matches

---

### Issue: Triggers not firing

**Symptoms:**
- member_count doesn't auto-update
- Manual UPDATE needed

**Debug:**
1. Check triggers exist:
   ```sql
   SELECT * FROM information_schema.triggers
   WHERE trigger_schema = 'public';
   ```
2. Check trigger functions exist
3. Try manual INSERT and check if count updates
4. Check Supabase logs for trigger errors

**Root Cause:**
- Trigger not created
- Function syntax error
- Table name mismatch

---

### Issue: Count mismatches

**Symptoms:**
- cached_members = 5, actual_members = 7

**Fix:**
Run initial sync from ADD_METADATA_TRIGGERS.sql:
```sql
UPDATE public.projects p
SET member_count = (
    SELECT COUNT(*)
    FROM public.project_members pm
    WHERE pm.project_id = p.id AND pm.is_active = TRUE
);
```

---

### Issue: Version conflicts on every update

**Symptoms:**
- Every update fails
- Always version mismatch

**Debug:**
1. Check Room and Supabase versions match
2. Verify task refresh after update (gets new version)
3. Check no stale cache

**Root Cause:**
- UI using old task object (old version)
- Need to refresh task after update
- Repository should return updated task with new version

---

## ✅ Final Verification Checklist

### Database
- [ ] Version columns exist in projects and tasks
- [ ] All 4 triggers created and active
- [ ] Metadata counts match reality (verification query)
- [ ] Array types confirmed (TEXT or TEXT[])

### Android Code
- [ ] SupabaseTaskDataSource has version handling
- [ ] SupabaseProjectDataSource has version handling
- [ ] Breaking changes addressed (method signatures)
- [ ] No compilation errors

### Functionality
- [ ] Task version increments on update
- [ ] Project version increments on update
- [ ] Concurrent edits detected (conflict)
- [ ] member_count auto-updates
- [ ] task counts auto-update
- [ ] last_activity_at auto-updates

### Multi-Device
- [ ] Sequential updates work
- [ ] Concurrent updates conflict
- [ ] Offline→online sync works
- [ ] Metadata accurate after sync

### Documentation
- [ ] SCHEMA_OPTIMIZATION_SUMMARY.md reviewed
- [ ] COMMENTS_STRATEGY_DECISION.md decision made
- [ ] Implementation notes read
- [ ] Breaking changes documented

---

## 📊 Test Results Summary

**Date Tested**: _________________

**Tester**: _________________

**Results:**
- Total Tests: 23
- Passed: _____ / 23
- Failed: _____ / 23
- Skipped: _____ / 23

**Critical Tests (Must Pass):**
- [ ] Test 1.1: Version columns created
- [ ] Test 3.3: Conflict detection works
- [ ] Test 3.4: Member count auto-updates
- [ ] Test 3.5: Task counts auto-update

**Status:**
- [ ] ✅ All tests passed - Ready for production
- [ ] 🟡 Minor issues - Document and monitor
- [ ] 🔴 Critical failures - Fix before deployment

**Notes:**
_______________________________________________________________
_______________________________________________________________
_______________________________________________________________

---

**Last Updated**: 2026-01-25
**Version**: 1.0
