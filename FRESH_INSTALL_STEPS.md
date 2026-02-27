# Fresh Install Testing Steps

## The Problem
Your logs show FK errors for user `b4330dd0-1066-4702-bede-2beef09fc847` because the new sequential sync code hasn't run yet. The app you're currently running is still using the old code.

## Solution: Complete Fresh Install

### Step 1: Uninstall Old App
```bash
# Option A: Via ADB
adb uninstall com.example.kosmos

# Option B: Via Device
Long press app icon → Uninstall
```

### Step 2: Clear App Data (Extra Safety)
```bash
# Via ADB
adb shell pm clear com.example.kosmos
```

### Step 3: Build Fresh APK
```bash
cd "/home/anshul/1 - Work./Projects-Big-Three/File/Kosmos"
./gradlew clean
./gradlew assembleDebug
```

### Step 4: Install Fresh Build
```bash
# Via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# OR via Android Studio
Click "Run" button
```

### Step 5: Monitor Logs BEFORE Opening App
```bash
# Clear old logs
adb logcat -c

# Start monitoring (in a separate terminal)
adb logcat -s InitialSyncManager:D UserRepository:D FKErrorHandler:E *:E
```

### Step 6: Open App and Login

**Expected Log Output:**
```
InitialSyncManager: 🔄 Starting sequential sync for user: <your-user-id>
InitialSyncManager: 📥 [1/6] Syncing users...
UserRepository: Starting user sync from Supabase
UserRepository: ✅ Synced 42 users to local cache
InitialSyncManager: ✅ [1/6] Users synced
InitialSyncManager: 📥 [2/6] Syncing projects...
ProjectRepository: ✅ Synced 5 projects for user <your-user-id>
InitialSyncManager: ✅ [2/6] Projects synced
InitialSyncManager: 📥 [3/6] Members synced via projects
InitialSyncManager: 📥 [4/6] Syncing chat rooms...
ChatRepository: ✅ Synced 3 chat rooms from Supabase
ChatRepository: ✅ Synced 127 messages from Supabase  # <-- Should be NO FK errors now
InitialSyncManager: ✅ [4/6] Chat rooms synced
InitialSyncManager: 📥 [5/6] Messages synced via chat rooms
InitialSyncManager: 📥 [6/6] Syncing tasks...
TaskRepository: ✅ Synced 18 tasks from Supabase
InitialSyncManager: ✅ [6/6] Tasks synced
InitialSyncManager: ✅ Sync complete in 2847ms - 6/6 succeeded
```

### Step 7: Verify No FK Errors
```bash
# Check for any FK violations
adb logcat | grep "FOREIGN KEY"

# Expected: NO OUTPUT (no FK errors)
```

---

## If FK Errors Still Occur

If you still see FK errors after fresh install, it means **the user truly doesn't exist in Supabase**. This is a data integrity issue, not a code issue.

### Fix Orphaned Messages in Supabase

Run this SQL in your Supabase SQL Editor:

```sql
-- 1. Check if user exists
SELECT id, username, display_name
FROM users
WHERE id = 'b4330dd0-1066-4702-bede-2beef09fc847';

-- If user doesn't exist, you have 2 options:

-- Option A: Delete orphaned messages (SAFE - Recommended)
DELETE FROM messages
WHERE sender_id = 'b4330dd0-1066-4702-bede-2beef09fc847';

-- Option B: Create placeholder "Deleted User" account
INSERT INTO users (
  id,
  username,
  display_name,
  email,
  created_at
) VALUES (
  'b4330dd0-1066-4702-bede-2beef09fc847',
  'deleted_user',
  'Deleted User',
  'deleted@placeholder.com',
  NOW()
);
```

---

## Quick Verification Checklist

After fresh install:

- [ ] Uninstalled old app completely
- [ ] Cleared app data
- [ ] Built fresh APK (`./gradlew clean assembleDebug`)
- [ ] Installed fresh APK
- [ ] Started logcat BEFORE opening app
- [ ] Logged in with test account
- [ ] Saw `InitialSyncManager` logs in output
- [ ] Saw `[1/6] Syncing users...` log
- [ ] Saw `✅ Synced X users to local cache`
- [ ] Saw all 6 sync steps complete
- [ ] Checked for FK errors: `adb logcat | grep "FOREIGN KEY"`
- [ ] Verified zero FK errors

---

## Troubleshooting

### I don't see InitialSyncManager logs at all

**Cause**: The new code isn't running.

**Fix**:
1. Verify build succeeded: `./gradlew assembleDebug`
2. Check APK timestamp is recent: `ls -lh app/build/outputs/apk/debug/`
3. Completely uninstall old app first
4. Restart device to clear any cached app data

### I still see FK errors after fresh install

**Cause**: The referenced user doesn't exist in Supabase (data integrity issue).

**Fix**:
1. Run `check_missing_user.sql` to identify orphaned messages
2. Either delete orphaned messages or create placeholder users
3. Re-run fresh install test

### Sync takes too long (> 10 seconds)

**Cause**: Network latency or large dataset.

**Fix**: Normal for first sync. Subsequent syncs will be faster.

---

## Expected Results

✅ **Success Indicators:**
- All 6 sync steps complete
- Zero FK constraint errors
- All screens show data correctly
- No crashes during sync

❌ **Failure Indicators:**
- No InitialSyncManager logs (old code still running)
- FK errors in logcat (data integrity issue in Supabase)
- App crashes (different issue - check full stack trace)

---

**Last Updated**: 2026-01-25
