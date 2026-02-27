# Quick Fix Guide - Supabase Schema Issues

**Created**: 2025-10-31
**Status**: ✅ Ready to Execute

---

## 🚨 CRITICAL: Which SQL Script to Use?

### ✅ USE THIS: `SCHEMA_FIX_COMPLETE_V2.sql`
**Why**: Fixes all columns, safe to re-run, no foreign key bugs

### ❌ DON'T USE: `SCHEMA_FIX_COMPLETE.sql` (V1)
**Why**: Has a bug - tries to create foreign keys for missing columns

---

## ⚡ 5-Minute Quick Start

### 1. Fix Database (5 minutes)
```bash
# Open Supabase Dashboard → SQL Editor
# Copy entire SCHEMA_FIX_COMPLETE_V2.sql
# Paste and Run
# Wait for completion (1-2 min)
```

**Expected Output**: All verification queries show ✅ EXISTS

### 2. Fix WebSocket (Optional, 5 minutes)
Edit `app/src/main/java/com/example/kosmos/core/config/SupabaseConfig.kt`:

```kotlin
import io.ktor.client.engine.okhttp.OkHttp

val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Postgrest)
    install(Auth)
    install(Realtime)
    install(Storage)

    // Add this line:
    httpEngine = OkHttp.create()
}
```

### 3. Build & Test (10 minutes)
```bash
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s ChatRepository:* TaskRepository:*
```

**Test**: Send a message, check logcat for:
```
D/ChatRepository: ✅ Message synced to Supabase successfully
```

---

## 🐛 Error You Reported

**Error**: `ERROR: 42703: column "reply_to_message_id" referenced in foreign key constraint does not exist`

**Cause**: You ran V1 script which has this bug

**Solution**: Run V2 script instead - it's safe even if V1 partially succeeded

---

## 📋 What V2 Fixes

### Messages Table - Missing 9 Columns
Before V2:
- ❌ sender_name
- ❌ sender_photo_url
- ❌ voice_message_id
- ❌ task_ids (array)
- ❌ reply_to_message_id
- ❌ is_edited
- ❌ edited_at
- ❌ reactions (JSONB)
- ❌ read_by (array)

After V2:
- ✅ All 9 columns added
- ✅ Data populated from users table
- ✅ Foreign keys created safely

### Chat Rooms Table - Missing 1 Column
Before V2:
- ❌ participant_ids (array)

After V2:
- ✅ participant_ids added with data

---

## ✅ Success Checklist

After running V2, verify these in Supabase SQL Editor:

```sql
-- Should return 15
SELECT COUNT(*) FROM information_schema.columns
WHERE table_name = 'messages';

-- Should return 14
SELECT COUNT(*) FROM information_schema.columns
WHERE table_name = 'chat_rooms';

-- Should show all ✅ EXISTS
SELECT
    'messages.sender_name' as col,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'messages' AND column_name = 'sender_name'
    ) THEN '✅ EXISTS' ELSE '❌ MISSING' END as status
UNION ALL
SELECT 'messages.reply_to_message_id',
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'messages' AND column_name = 'reply_to_message_id'
    ) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT 'chat_rooms.participant_ids',
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'chat_rooms' AND column_name = 'participant_ids'
    ) THEN '✅ EXISTS' ELSE '❌ MISSING' END;
```

Expected: All rows show `✅ EXISTS`

---

## 🎯 After Fix - Test These

### Test 1: Search
- Open app → Find Users
- Search for a user
- Should work without JSON errors

### Test 2: Message Sync
- Send a message
- Check Supabase → Table Editor → messages
- Should appear within 2 seconds

### Test 3: Chat Room Sync
- Create a chat room
- Check Supabase → Table Editor → chat_rooms
- Should appear immediately

---

## 📚 Full Documentation

For complete details, see:
- `COMPLETE_FIX_PLAN_2025-10-31.md` - Full execution plan
- `SCHEMA_ANALYSIS_COMPLETE.md` - All tables documented
- `SUPABASE_ARCHITECTURE_LOGBOOK.md` - Future prevention guide
- `TESTING_GUIDE_SUPABASE_FIXES.md` - Detailed testing

---

## 🔄 If Something Goes Wrong

### V2 Script Fails
```sql
-- Check what columns exist
SELECT column_name FROM information_schema.columns
WHERE table_name = 'messages'
ORDER BY column_name;

-- Manually reload cache
NOTIFY pgrst, 'reload schema';
```

### Still Getting Sync Errors
```bash
# Check logcat for specific error
adb logcat | grep "SUPABASE SYNC FAILED"

# Common issues:
# - RLS still enabled → Run: ALTER TABLE messages DISABLE ROW LEVEL SECURITY;
# - Auth expired → Log out and back in
# - Network issue → Check internet
```

---

## ⏱️ Time Estimates

- **Just Database Fix**: 5 minutes
- **Database + WebSocket**: 10 minutes
- **Full Fix + Testing**: 30 minutes
- **Full Fix + Documentation Update**: 60 minutes

---

**Ready to Execute**: Yes ✅

**Next Action**: Open Supabase Dashboard, copy `SCHEMA_FIX_COMPLETE_V2.sql`, run it

**Questions?**: Check `COMPLETE_FIX_PLAN_2025-10-31.md` for detailed troubleshooting
