# Quick Test Reference Card

**For**: Quick verification that everything works
**Time**: 5-10 minutes

---

## 🚀 Quick Start (3 Commands)

```bash
# 1. Run automated test script
./quick_test.sh

# 2. Manually test in app:
#    - Login
#    - Create project
#    - Create task
#    - Update task

# 3. Check for errors
adb logcat | grep -E "(Error|FAILED|Serializ|PGRST)"
```

**✅ If no errors**: Everything works!

---

## 📋 Essential Commands

### Installation
```bash
# Install latest APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Clear app data
adb shell pm clear com.example.kosmos
```

### Monitoring
```bash
# Monitor all errors
adb logcat -s SupabaseTaskDataSource:* TaskRepository:* | grep -E "(Error|success)"

# Check for specific errors
adb logcat | grep "Serializer for class 'Any'"  # Should be NONE
adb logcat | grep "PGRST204"                     # Should be NONE
adb logcat | grep "null literal"                 # Should be NONE
```

### Database Verification
```sql
-- In Supabase SQL Editor

-- 1. Check comments column exists
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'tasks' AND column_name = 'comments';

-- 2. Check latest tasks
SELECT id, title, description, status, comments, created_at
FROM tasks
ORDER BY created_at DESC
LIMIT 5;
```

---

## ✅ Success Indicators

**In Logcat**:
```
✅ D/SupabaseTaskDataSource: Task inserted successfully
✅ D/SupabaseTaskDataSource: Task updated successfully
✅ D/TaskRepository: ✅ SUPABASE SYNC SUCCESS
```

**In Supabase**:
```
✅ New task appears within 1-2 seconds
✅ comments column = []
✅ description can be NULL (no errors)
```

---

## ❌ Error Indicators

**In Logcat**:
```
❌ E/SupabaseTaskDataSource: Error updating task
❌ kotlinx.serialization.SerializationException: Serializer for class 'Any'
❌ Could not find the 'comments' column... PGRST204
❌ W/TaskRepository: Failed to sync task update to Supabase
```

**If you see these**: Something broke, check TESTING_GUIDE_COMPLETE_2025-11-01.md

---

## 🧪 Quick Manual Test (5 mins)

1. **Launch app** → Login
2. **Create project** → "Test Project"
3. **Create task**:
   - Title: "Test Task"
   - Description: Leave EMPTY (tests NULL handling)
   - Priority: HIGH
   - Save
4. **Check logcat**: Should see "Task inserted successfully"
5. **Update task**:
   - Change title to "Updated Task"
   - Change status to IN_PROGRESS
   - Save
6. **Check logcat**: Should see "Task updated successfully"
7. **Verify in Supabase**: Task should appear in tasks table

**✅ If all steps work**: You're good to go!

---

## 🔍 Detailed Testing

For comprehensive testing, see:
- **TESTING_GUIDE_COMPLETE_2025-11-01.md** - Full test suite

For troubleshooting, see:
- **COMPLETE_FIX_SUMMARY_2025-11-01.md** - All fixes explained

---

## 📊 Test Results Template

```
Date: [Date]
Time: [Time]

Installation:    ✅/❌
Task Creation:   ✅/❌
Task Update:     ✅/❌
Serialization:   ✅/❌ (No errors)
PGRST204:        ✅/❌ (No errors)
WebSocket:       ✅/❌

Overall: PASS / FAIL

Notes: [Any issues]
```

---

**That's it!** If quick test passes, everything is working. 🎉
