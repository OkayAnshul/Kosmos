# Complete Testing & Verification Guide

**Date**: November 1, 2025
**Purpose**: Verify all fixes are working properly
**Status**: Ready to execute

---

## 📋 Pre-Test Checklist

Before starting tests, verify:

- [ ] Latest APK built successfully
- [ ] APK installed on device/emulator
- [ ] Device connected via ADB
- [ ] Supabase project accessible
- [ ] Internet connection active

**Quick Verify**:
```bash
# Check ADB connection
adb devices

# Check app installed
adb shell pm list packages | grep kosmos

# Check internet
ping google.com
```

---

## 🗄️ Phase 1: Database Schema Verification

### Test 1.1: Verify Tasks Table Schema

**Run in Supabase SQL Editor**:

```sql
-- Check all columns in tasks table
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'tasks'
ORDER BY ordinal_position;
```

**Expected Result** (21 columns):
```
id                   | uuid      | NO  | gen_random_uuid()
project_id           | uuid      | NO  |
chat_room_id         | uuid      | YES |
title                | text      | NO  |
description          | text      | YES | NULL
status               | text      | NO  | 'TODO'
priority             | text      | NO  | 'MEDIUM'
assigned_to_id       | uuid      | YES |
assigned_to_name     | text      | YES |
assigned_to_role     | text      | YES |
created_by_id        | uuid      | NO  |
created_by_name      | text      | NO  |
created_by_role      | text      | YES |
created_at           | bigint    | NO  |
updated_at           | bigint    | NO  |
due_date             | bigint    | YES |
source_message_id    | uuid      | YES |
tags                 | text[]    | YES | '{}'
comments             | jsonb     | YES | '[]'::jsonb
parent_task_id       | uuid      | YES |
estimated_hours      | real      | YES |
actual_hours         | real      | YES |
```

**✅ Success Criteria**:
- All 21 columns present
- `comments` column exists with type `jsonb`
- `description` allows NULL
- All data types match expected

**❌ If Missing Columns**:
```sql
-- Run the migration
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS comments JSONB DEFAULT '[]'::jsonb;
NOTIFY pgrst, 'reload schema';
```

---

### Test 1.2: Verify Foreign Keys

**Run in Supabase SQL Editor**:

```sql
-- Check foreign keys on tasks table
SELECT
    tc.constraint_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_name = 'tasks'
ORDER BY tc.constraint_name;
```

**Expected Result**:
```
fk_tasks_chat_room    | chat_room_id      | chat_rooms  | id
fk_tasks_creator      | created_by_id     | users       | id
fk_tasks_message      | source_message_id | messages    | id
fk_tasks_parent       | parent_task_id    | tasks       | id
fk_tasks_project      | project_id        | projects    | id
```

**✅ Success Criteria**: All expected foreign keys present

---

### Test 1.3: Check Schema Cache

**Run in Supabase SQL Editor**:

```sql
-- Force reload schema cache
NOTIFY pgrst, 'reload schema';

-- Verify tasks table is accessible
SELECT COUNT(*) as task_count FROM tasks;
```

**✅ Success Criteria**: Query runs without errors

---

## 📱 Phase 2: App Installation & Setup

### Test 2.1: Fresh Install

```bash
# Navigate to project
cd /home/anshul/WORK/DEVELOPEMENT/ANDROID-DEV/Projects/Kosmos

# Install latest APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Expected output:
# Success
```

**✅ Success Criteria**: Installation succeeds

---

### Test 2.2: Clear App Data (Fresh Start)

```bash
# Clear all app data
adb shell pm clear com.example.kosmos

# Expected output:
# Success
```

**✅ Success Criteria**: Data cleared successfully

---

### Test 2.3: Start Logcat Monitoring

**Open separate terminal and run**:

```bash
# Monitor all app logs
adb logcat -c  # Clear old logs
adb logcat -s \
  SupabaseTaskDataSource:* \
  TaskRepository:* \
  SupabaseUserDataSource:* \
  UserRepository:* \
  SupabaseChatDataSource:* \
  ChatRepository:* \
  SupabaseProjectDataSource:* \
  ProjectRepository:* \
  SupabaseConfig:*
```

**Keep this running** during all tests to monitor for errors.

---

## ✅ Phase 3: Core Functionality Tests

### Test 3.1: User Login

**Steps**:
1. Launch app
2. Login with Google or email
3. Verify login succeeds

**Monitor Logcat For**:
```
✅ D/SupabaseUserDataSource: User inserted successfully
✅ D/UserRepository: User synced to Supabase
```

**❌ Red Flags**:
```
❌ E/SupabaseUserDataSource: Error inserting user
❌ E/UserRepository: SUPABASE SYNC FAILED
```

**✅ Success Criteria**:
- Login succeeds
- User appears in Supabase `users` table
- No errors in logcat

---

### Test 3.2: Create Project

**Steps**:
1. Tap "+" button
2. Create new project
3. Enter project name and description
4. Save

**Monitor Logcat For**:
```
✅ D/SupabaseProjectDataSource: Project inserted successfully
✅ D/ProjectRepository: Project synced to Supabase
```

**Verify in Supabase**:
```sql
SELECT id, name, description, created_at
FROM projects
ORDER BY created_at DESC
LIMIT 5;
```

**✅ Success Criteria**:
- Project created in app
- Project appears in Supabase `projects` table
- No PGRST204 errors
- No serialization errors

---

### Test 3.3: Create Task (CRITICAL TEST)

**Steps**:
1. Open project
2. Create new task
3. Fill in:
   - Title: "Test Task 1"
   - Description: "Testing task creation"
   - Priority: HIGH
   - Status: TODO

**Monitor Logcat For**:
```
✅ D/SupabaseTaskDataSource: Task inserted successfully: id=...
✅ D/TaskRepository: ✅ SUPABASE SYNC SUCCESS for task (Test Task 1)
```

**❌ Red Flags**:
```
❌ E/SupabaseTaskDataSource: Error inserting task... PGRST204
❌ E/SupabaseTaskDataSource: Could not find the 'comments' column
❌ E/TaskRepository: ❌ SUPABASE SYNC FAILED
❌ kotlinx.serialization.SerializationException
```

**Verify in Supabase**:
```sql
SELECT
    id,
    title,
    description,
    status,
    priority,
    comments,
    created_at
FROM tasks
ORDER BY created_at DESC
LIMIT 5;
```

**✅ Success Criteria**:
- Task created in app
- Task appears in Supabase `tasks` table
- `comments` field = `[]` (empty array)
- `description` field has value or NULL (no errors)
- No PGRST204 errors
- No serialization errors

---

### Test 3.4: Update Task (CRITICAL TEST)

**Steps**:
1. Open the task created in Test 3.3
2. Update title to "Updated Test Task"
3. Change status to IN_PROGRESS
4. Change priority to URGENT
5. Save changes

**Monitor Logcat For**:
```
✅ D/SupabaseTaskDataSource: Task updated successfully: id=...
✅ D/TaskRepository: Task update synced to Supabase
```

**❌ Red Flags**:
```
❌ E/SupabaseTaskDataSource: Error updating task
❌ kotlinx.serialization.SerializationException: Serializer for class 'Any'
❌ W/TaskRepository: Failed to sync task update to Supabase
```

**Verify in Supabase**:
```sql
SELECT
    title,
    status,
    priority,
    updated_at
FROM tasks
WHERE id = '<task-id-from-logcat>';
```

**✅ Success Criteria**:
- Task updates in app UI
- Changes appear in Supabase within 1-2 seconds
- `updated_at` timestamp changed
- No "Serializer for class 'Any'" errors
- No sync failures

---

### Test 3.5: Update Task with NULL Description

**Steps**:
1. Create new task
2. Leave description EMPTY (don't fill it)
3. Save task
4. Verify task created successfully

**Monitor Logcat For**:
```
✅ D/SupabaseTaskDataSource: Task inserted successfully
```

**Verify in Supabase**:
```sql
SELECT title, description
FROM tasks
WHERE description IS NULL
ORDER BY created_at DESC
LIMIT 1;
```

**✅ Success Criteria**:
- Task with NULL description creates successfully
- No "Unexpected JSON token" errors
- No NULL handling errors

---

### Test 3.6: Fetch Tasks with NULL Description

**Steps**:
1. Navigate away from task screen
2. Navigate back to task list
3. Verify all tasks load, including ones with NULL description

**Monitor Logcat For**:
```
✅ D/SupabaseTaskDataSource: Fetched X tasks for project
✅ D/TaskRepository: Tasks synced from Supabase
```

**❌ Red Flags**:
```
❌ E/SupabaseTaskDataSource: Error fetching tasks
❌ JsonDecodingException: Expected string literal but 'null' literal was found
```

**✅ Success Criteria**:
- All tasks display correctly
- Tasks with NULL description show without errors
- No JSON decoding errors

---

### Test 3.7: Update Task Status (Quick Action)

**Steps**:
1. Open task list
2. Drag task from TODO → IN_PROGRESS
3. Observe immediate UI update

**Monitor Logcat For**:
```
✅ D/SupabaseTaskDataSource: Task status updated: id=..., status=IN_PROGRESS
```

**✅ Success Criteria**:
- UI updates immediately (optimistic update)
- Supabase sync happens in background
- No errors

---

### Test 3.8: Delete Task

**Steps**:
1. Long-press task or open task details
2. Delete task
3. Confirm deletion

**Monitor Logcat For**:
```
✅ D/SupabaseTaskDataSource: Task deleted successfully: id=...
```

**Verify in Supabase**:
```sql
SELECT COUNT(*) FROM tasks WHERE id = '<deleted-task-id>';
-- Should return 0
```

**✅ Success Criteria**:
- Task removed from app UI
- Task deleted from Supabase
- No errors

---

## 🔄 Phase 4: Real-time & Sync Tests

### Test 4.1: WebSocket Connection

**Monitor Logcat For** (within 10 seconds of app start):
```
✅ I/SupabaseConfig: WebSocket connected
✅ D/Realtime: Channel subscribed
```

**❌ Red Flags**:
```
❌ E/SupabaseConfig: Engine doesn't support WebSocketCapability
❌ E/Realtime: WebSocket connection failed
```

**✅ Success Criteria**:
- WebSocket connects without errors
- No repeated connection failures every 7 seconds

---

### Test 4.2: Real-time Updates (Two Devices)

**If you have two devices/emulators**:

**Device 1**:
1. Create a task

**Device 2**:
2. Wait 1-2 seconds
3. Task should appear automatically

**✅ Success Criteria**: Real-time sync works

**If only one device**: Skip this test (optional feature)

---

### Test 4.3: Offline Mode

**Steps**:
1. Enable airplane mode on device
2. Create a task
3. Verify task saved locally
4. Disable airplane mode
5. Wait 2-3 seconds
6. Verify task syncs to Supabase

**Monitor Logcat For**:
```
✅ D/TaskRepository: Task saved locally (offline)
✅ D/TaskRepository: Task synced to Supabase after reconnect
```

**✅ Success Criteria**:
- Tasks save locally when offline
- Tasks sync to Supabase when back online

---

## 🧪 Phase 5: Error Monitoring

### Test 5.1: No Serialization Errors

**Search Logcat For**:
```bash
adb logcat | grep -i "serialization"
```

**✅ Success Criteria**: No serialization errors appear

---

### Test 5.2: No PGRST204 Errors

**Search Logcat For**:
```bash
adb logcat | grep -i "PGRST204"
```

**✅ Success Criteria**: No PGRST204 (column not found) errors

---

### Test 5.3: No NULL Handling Errors

**Search Logcat For**:
```bash
adb logcat | grep -i "null literal"
```

**✅ Success Criteria**: No JSON decoding errors for NULL values

---

### Test 5.4: No WebSocket Failures

**Search Logcat For**:
```bash
adb logcat | grep -i "websocket"
```

**✅ Success Criteria**: WebSocket connects successfully, no repeated failures

---

## 📊 Phase 6: Final Verification

### Test 6.1: Supabase Data Integrity

**Run in Supabase SQL Editor**:

```sql
-- Check all tasks have valid data
SELECT
    COUNT(*) as total_tasks,
    COUNT(CASE WHEN description IS NULL THEN 1 END) as null_descriptions,
    COUNT(CASE WHEN comments IS NULL THEN 1 END) as null_comments,
    COUNT(CASE WHEN tags IS NULL THEN 1 END) as null_tags
FROM tasks;
```

**Expected**:
- `null_comments` should be 0 (default to `[]`)
- `null_descriptions` can be > 0 (allowed)
- `null_tags` should be 0 (default to `{}`)

---

### Test 6.2: Room Database Integrity

**Check for schema errors**:
```bash
adb logcat | grep -i "room cannot verify"
```

**✅ Success Criteria**: No Room schema integrity errors

---

## ✅ Complete Test Results Checklist

Mark each as you complete:

### Database Schema
- [ ] All 21 columns exist in tasks table
- [ ] Comments column is JSONB type
- [ ] Description allows NULL
- [ ] All foreign keys configured
- [ ] Schema cache reloaded

### App Functionality
- [ ] User login works
- [ ] Project creation works
- [ ] Task creation works
- [ ] Task update works
- [ ] Task with NULL description works
- [ ] Fetch tasks with NULL works
- [ ] Task status update works
- [ ] Task deletion works

### Sync & Real-time
- [ ] WebSocket connects successfully
- [ ] Local → Supabase sync works
- [ ] Supabase → Local sync works
- [ ] Offline mode works

### Error Monitoring
- [ ] No serialization errors
- [ ] No PGRST204 errors
- [ ] No NULL handling errors
- [ ] No WebSocket failures
- [ ] No Room schema errors

---

## 🎯 Overall Success Criteria

**ALL of the following must be true**:

✅ All 21 tasks table columns exist
✅ Task creation succeeds
✅ Task update succeeds (NO "Serializer for class 'Any'" error)
✅ Tasks with NULL description work
✅ WebSocket connection stable
✅ No PGRST204 errors in logcat
✅ No serialization errors in logcat
✅ Data appears in Supabase within 1-2 seconds

---

## 🚨 Troubleshooting

### If Task Insert Fails with PGRST204

**Error**: `Could not find the 'comments' column`

**Solution**:
```sql
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS comments JSONB DEFAULT '[]'::jsonb;
NOTIFY pgrst, 'reload schema';
```

---

### If Task Update Fails with "Serializer for Any"

**Error**: `kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found`

**Check**:
1. Verify latest APK is installed
2. Clear app data and reinstall
3. Check SupabaseTaskDataSource.kt uses UpdateBuilder DSL (not mapOf)

**Solution**: Should already be fixed in current build

---

### If NULL Description Fails

**Error**: `Unexpected JSON token... 'null' literal was found`

**Check**:
1. Task.kt has `description: String?` (nullable)
2. SupabaseConfig.kt has `coerceInputValues = true`

**Solution**: Should already be fixed in current build

---

### If WebSocket Fails

**Error**: `Engine doesn't support WebSocketCapability`

**Check**:
1. `ktor-client-okhttp` dependency in build.gradle.kts
2. `httpEngine = OkHttp.create()` in SupabaseConfig.kt

**Solution**: Should already be fixed in current build

---

## 📝 Test Report Template

After completing all tests, document results:

```
# Test Results - [Date/Time]

## Environment
- Device: [e.g., Pixel 6 Emulator]
- Android Version: [e.g., Android 15]
- APK Build: [timestamp]

## Database Schema
- Tasks table columns: ✅/❌ (21/21)
- Comments column: ✅/❌
- Foreign keys: ✅/❌

## App Functionality
- User login: ✅/❌
- Task creation: ✅/❌
- Task update: ✅/❌
- Task deletion: ✅/❌

## Sync & Real-time
- WebSocket: ✅/❌
- Supabase sync: ✅/❌
- Offline mode: ✅/❌

## Error Monitoring
- Serialization errors: 0
- PGRST204 errors: 0
- NULL errors: 0
- WebSocket errors: 0

## Overall Status
[  ] PASS - All tests successful
[  ] FAIL - Errors found (list below)

## Notes
[Any observations or issues]
```

---

## 🎉 Expected Final Status

After all tests pass:

```
✅✅✅ ALL TESTS PASSED ✅✅✅

- Database schema aligned
- All CRUD operations working
- Real-time sync functional
- No errors in production
- Ready for continued development
```

---

**End of Testing Guide**

Good luck with testing! 🚀
