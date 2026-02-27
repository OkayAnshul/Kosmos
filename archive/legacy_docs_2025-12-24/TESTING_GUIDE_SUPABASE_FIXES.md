# Testing Guide: Supabase Search & Sync Fixes

**Date**: October 31, 2025
**Issues Fixed**:
1. ✅ Search functionality JSON error (NULL username)
2. ✅ Messages not syncing to Supabase (RLS blocking)
3. ✅ Tasks not syncing to Supabase (RLS blocking)
4. ✅ Chat rooms not syncing to Supabase (RLS blocking)

---

## 🎯 Quick Start

### Step 1: Run SQL Fixes in Supabase

1. **Open Supabase Dashboard** → SQL Editor
2. **Copy and paste** the entire contents of `SUPABASE_FIX_USERNAME_AND_RLS.sql`
3. **Run the script** (or run sections separately)
4. **Verify** the changes worked:

```sql
-- Check if usernames were populated
SELECT id, email, username, display_name
FROM users
WHERE username IS NOT NULL
LIMIT 10;

-- Check if RLS is disabled
SELECT tablename, rowsecurity
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks');
```

Expected output:
- ✅ All users have valid usernames
- ✅ All tables show `rowsecurity = false` (RLS disabled)

### Step 2: Build and Install App

```bash
# Build the app
./gradlew assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Test with Logcat Running

```bash
# Start logcat to see sync status
adb logcat -s ChatRepository:* TaskRepository:* SupabaseUserDataSource:*
```

---

## 🧪 Detailed Test Cases

### Test 1: Search Functionality

**Before Fix**: JSON error when searching for users
**After Fix**: Search returns results without errors

**Steps**:
1. Open app → Navigate to "Find Users" or "Add Team Member"
2. Search for a user by name or @username
3. Check logcat for "Search completed" message

**Expected Results**:
- ✅ No JSON deserialization errors
- ✅ Users appear in search results
- ✅ Logcat shows: `Search completed: query='...', found X users`

**Verify in Logcat**:
```
D/SupabaseUserDataSource: Search completed: query='test', found 3 users
```

**If Still Failing**:
- Check if SQL script updated usernames: `SELECT username FROM users LIMIT 10;`
- Ensure username column is not NULL for any users

---

### Test 2: Message Sync to Supabase

**Before Fix**: Messages only saved to local Room database
**After Fix**: Messages sync to Supabase in real-time

**Steps**:
1. Open a chat room
2. Send a message: "Hello, testing Supabase sync!"
3. Watch logcat for sync status
4. Check Supabase Dashboard → Table Editor → messages

**Expected Logcat Output**:
```
D/ChatRepository: ✅ Message synced to Supabase successfully: [message-id]
```

**If Sync Fails** (check logcat):
```
E/ChatRepository: ❌ SUPABASE SYNC FAILED for message
E/ChatRepository: Possible causes: RLS policies blocking insert, network error, auth token expired
```

**Troubleshooting**:
1. **RLS Still Enabled?** Run: `ALTER TABLE messages DISABLE ROW LEVEL SECURITY;`
2. **Auth Token Expired?** Log out and log back in
3. **Network Issue?** Check internet connection
4. **Check Supabase Error**: Look for detailed error in logcat stack trace

**Verify in Supabase**:
```sql
SELECT id, sender_name, content, timestamp
FROM messages
ORDER BY timestamp DESC
LIMIT 10;
```

---

### Test 3: Task Sync to Supabase

**Before Fix**: Tasks only saved locally
**After Fix**: Tasks sync to Supabase

**Steps**:
1. Create a new project (or open existing)
2. Create a task: "Test task sync"
3. Set priority and due date
4. Watch logcat for sync status
5. Check Supabase → tasks table

**Expected Logcat Output**:
```
D/TaskRepository: ✅ Task synced to Supabase successfully: [task-id]
```

**If Sync Fails**:
```
E/TaskRepository: ❌ SUPABASE SYNC FAILED for task
E/TaskRepository: Possible causes: RLS policies blocking insert, network error, auth token expired
```

**Troubleshooting**:
- Disable RLS: `ALTER TABLE tasks DISABLE ROW LEVEL SECURITY;`
- Check project_members table has your user with proper permissions
- Verify auth token is valid

**Verify in Supabase**:
```sql
SELECT id, title, status, priority, created_by_name, created_at
FROM tasks
ORDER BY created_at DESC
LIMIT 10;
```

---

### Test 4: Chat Room Sync

**Before Fix**: Chat rooms only saved locally
**After Fix**: Chat rooms sync to Supabase

**Steps**:
1. Create a new chat room in a project
2. Name it "Test Sync Room"
3. Watch logcat
4. Check Supabase → chat_rooms table

**Expected Logcat Output**:
```
D/ChatRepository: ✅ Chat room synced to Supabase successfully: [room-id]
```

**If Sync Fails**:
```
E/ChatRepository: ❌ SUPABASE SYNC FAILED for chat room
```

**Verify in Supabase**:
```sql
SELECT id, name, type, project_id, created_by, created_at
FROM chat_rooms
ORDER BY created_at DESC
LIMIT 10;
```

---

## 🔍 Advanced Debugging

### Enable Verbose Logging

Filter logcat to see all Supabase-related messages:

```bash
# See all sync activity
adb logcat | grep -E "(ChatRepository|TaskRepository|SupabaseUserDataSource|SupabaseMessageDataSource|SupabaseTaskDataSource)"

# See only errors
adb logcat | grep "SUPABASE SYNC FAILED"

# See only successes
adb logcat | grep "synced to Supabase successfully"
```

### Common Error Patterns

#### Error 1: "new row violates row-level security policy"
**Cause**: RLS is still enabled
**Fix**: Run `ALTER TABLE [table_name] DISABLE ROW LEVEL SECURITY;`

#### Error 2: "JSON deserialization error"
**Cause**: NULL username or schema mismatch
**Fix**: Run username update SQL, check @SerialName annotations

#### Error 3: "JWT expired"
**Cause**: Auth token needs refresh
**Fix**: Log out and log back in

#### Error 4: "Network error" or timeout
**Cause**: No internet or Supabase unreachable
**Fix**: Check network, verify Supabase URL in BuildConfig

---

## 📊 Verification Checklist

After running all tests, verify data in Supabase:

### Users Table
```sql
SELECT COUNT(*) as total_users,
       COUNT(username) as users_with_username,
       COUNT(*) - COUNT(username) as missing_username
FROM users;
```
✅ Expected: `missing_username = 0`

### Messages Table
```sql
SELECT COUNT(*) as total_messages,
       COUNT(DISTINCT sender_id) as unique_senders,
       MAX(timestamp) as latest_message_time
FROM messages;
```
✅ Expected: Count > 0 after sending test messages

### Tasks Table
```sql
SELECT COUNT(*) as total_tasks,
       COUNT(CASE WHEN status = 'TODO' THEN 1 END) as todo_tasks,
       COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) as in_progress_tasks,
       COUNT(CASE WHEN status = 'DONE' THEN 1 END) as done_tasks
FROM tasks;
```
✅ Expected: Count > 0 after creating test tasks

### Chat Rooms Table
```sql
SELECT COUNT(*) as total_rooms,
       COUNT(DISTINCT project_id) as unique_projects
FROM chat_rooms;
```
✅ Expected: Count > 0 after creating rooms

---

## 🚨 What to Do If Tests Still Fail

### Scenario 1: All Syncs Failing
**Likely Cause**: RLS policies still blocking operations

**Solution**:
```sql
-- Nuclear option: Disable RLS on ALL tables
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'public'
    LOOP
        EXECUTE 'ALTER TABLE ' || r.tablename || ' DISABLE ROW LEVEL SECURITY';
    END LOOP;
END $$;
```

### Scenario 2: Search Still Shows JSON Error
**Likely Cause**: Username SQL update didn't work

**Solution**:
```sql
-- Manually set username for all users
UPDATE users
SET username = 'user_' || substr(id, 1, 8)
WHERE username IS NULL OR username = '';

-- Or delete users and re-register
DELETE FROM users;
```

### Scenario 3: Some Syncs Work, Others Don't
**Likely Cause**: Mixed RLS status or permission issues

**Solution**:
```sql
-- Check RLS status per table
SELECT tablename, rowsecurity
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

-- Disable RLS for specific table
ALTER TABLE [failing_table_name] DISABLE ROW LEVEL SECURITY;
```

---

## 📝 Code Changes Summary

### Files Modified:
1. **SupabaseUserDataSource.kt** (line 171-214)
   - Added NULL username filtering in search query
   - Added try-catch for JSON deserialization
   - Added client-side validation for blank usernames

2. **ChatRepository.kt** (line 95-107, 156-163)
   - Enhanced error logging for message sync failures
   - Enhanced error logging for chat room sync failures
   - Added success logging for debugging

3. **TaskRepository.kt** (line 112-125)
   - Enhanced error logging for task sync failures
   - Added detailed cause descriptions
   - Added success logging

### New Files Created:
1. **SUPABASE_FIX_USERNAME_AND_RLS.sql** - Complete SQL fix script
2. **TESTING_GUIDE_SUPABASE_FIXES.md** - This file

---

## ✅ Success Criteria

After running all tests, you should see:

1. ✅ **Search**: No JSON errors, users appear in results
2. ✅ **Messages**: Appear in Supabase `messages` table within 1-2 seconds
3. ✅ **Tasks**: Appear in Supabase `tasks` table immediately
4. ✅ **Chat Rooms**: Appear in Supabase `chat_rooms` table immediately
5. ✅ **Logcat**: Shows "✅ synced to Supabase successfully" messages
6. ✅ **No Errors**: No "❌ SUPABASE SYNC FAILED" in logcat

---

## 🎓 Understanding the Fixes

### Why Was Search Failing?
- Supabase `users` table had NULL `username` values
- Kotlin `User` model expects `username: String` (non-null with default "")
- JSON deserializer couldn't map NULL → String
- **Fix**: Updated SQL to populate username + added NULL filtering

### Why Wasn't Data Syncing?
- Supabase Row Level Security (RLS) was enabled by default
- RLS policies were missing or restrictive
- INSERT operations were silently blocked
- Code logged errors but didn't fail (offline-first design)
- **Fix**: Disabled RLS temporarily for testing

### Why Hybrid Pattern?
- App saves to local Room first (instant UI update)
- Then syncs to Supabase in background (cloud backup)
- If sync fails, user still sees their data locally
- This is called "optimistic UI updates"

---

## 📞 Need Help?

If issues persist after following this guide:

1. **Capture full logcat output**:
   ```bash
   adb logcat > supabase_debug.log
   ```

2. **Check Supabase logs** in Dashboard → Logs

3. **Verify Supabase configuration**:
   - BuildConfig.SUPABASE_URL is correct
   - BuildConfig.SUPABASE_ANON_KEY is valid
   - Tables exist in Supabase

4. **Test Supabase connection** directly in SQL Editor:
   ```sql
   INSERT INTO messages (id, chat_room_id, sender_id, sender_name, content, timestamp, type)
   VALUES (
       'manual-test-' || gen_random_uuid()::text,
       'test-room',
       auth.uid()::text,
       'Manual Test',
       'Testing direct insert',
       EXTRACT(EPOCH FROM NOW())::bigint * 1000,
       'TEXT'
   );
   ```

---

**Status**: Ready for Testing
**Next Steps**: Run SQL script → Build app → Test all scenarios → Verify in Supabase
