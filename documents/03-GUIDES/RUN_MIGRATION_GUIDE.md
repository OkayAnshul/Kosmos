# Database Migration & Testing Guide

**Supabase URL**: `https://krbfvekgqbcwjgntepip.supabase.co`

---

## Step 1: Run Database Migration

### Option A: Via Supabase Dashboard (Recommended)

1. **Open Supabase Dashboard**:
   - Go to https://supabase.com/dashboard
   - Navigate to your project: `krbfvekgqbcwjgntepip`

2. **Open SQL Editor**:
   - Click "SQL Editor" in left sidebar
   - Click "New Query"

3. **Copy and Execute Migration**:
   - Open `NOTIFICATIONS_TABLE_MIGRATION.sql` from this project
   - Copy the entire contents
   - Paste into SQL Editor
   - Click "Run" or press `Ctrl+Enter`

4. **Verify Success**:
   - You should see: "Success. No rows returned"
   - Check "Table Editor" → you should see new table "notifications"

### Option B: Via Supabase CLI

If you have Supabase CLI installed:

```bash
# Navigate to project directory
cd /home/anshul/WORK/DEVELOPEMENT/ANDROID-DEV/Projects/Kosmos

# Run migration
supabase db reset --db-url "postgresql://postgres:[YOUR-PASSWORD]@db.krbfvekgqbcwjgntepip.supabase.co:5432/postgres"

# Or execute the file directly
psql "postgresql://postgres:[YOUR-PASSWORD]@db.krbfvekgqbcwjgntepip.supabase.co:5432/postgres" -f NOTIFICATIONS_TABLE_MIGRATION.sql
```

---

## Step 2: Verify Migration Success

### Check in Supabase Dashboard

1. **Table Editor**:
   - Click "Table Editor" in left sidebar
   - Find "notifications" table
   - Verify columns: id, user_id, title, body, type, data, is_read, created_at, updated_at

2. **Database** → **Policies**:
   - Click "Database" → "Policies"
   - Filter by table: "notifications"
   - Verify 4 policies exist:
     - Users can view own notifications
     - Users can update own notifications
     - System can insert notifications
     - Users can delete own notifications

3. **Realtime**:
   - Click "Database" → "Replication"
   - Verify "notifications" table is in the list with "Enabled" status
   - If not, add it manually

### SQL Verification Queries

Run these in SQL Editor to verify:

```sql
-- 1. Check table exists
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE table_name = 'notifications'
ORDER BY ordinal_position;

-- 2. Check indexes
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'notifications';

-- 3. Check RLS policies
SELECT policyname, cmd, qual
FROM pg_policies
WHERE tablename = 'notifications';

-- 4. Check Realtime is enabled
SELECT * FROM pg_publication_tables
WHERE tablename = 'notifications';
```

Expected output:
- **Table columns**: 9 columns (id, user_id, title, body, type, data, is_read, created_at, updated_at)
- **Indexes**: 4-5 indexes
- **Policies**: 4 policies
- **Realtime**: 1 row showing table is published

---

## Step 3: Insert Test Notification

### Get Your User ID

First, find your user ID:

```sql
-- Run this in SQL Editor
SELECT id, email, display_name
FROM users
LIMIT 5;
```

Copy your user ID (UUID format).

### Insert Test Notification

Replace `YOUR_USER_ID` with the actual UUID from above:

```sql
INSERT INTO notifications (user_id, title, body, type, data, created_at)
VALUES (
    'YOUR_USER_ID',  -- Replace with your actual user ID
    'Test Notification',
    'This is a test notification from the migration',
    'info',
    '{"test": "true", "source": "migration"}'::jsonb,
    EXTRACT(EPOCH FROM NOW()) * 1000
);

-- Verify it was inserted
SELECT * FROM notifications
ORDER BY created_at DESC
LIMIT 1;
```

Expected output:
- 1 row inserted
- SELECT query shows your test notification

---

## Step 4: Build Android App

### Clean Build

```bash
cd /home/anshul/WORK/DEVELOPEMENT/ANDROID-DEV/Projects/Kosmos

# Clean previous builds
./gradlew clean

# Build debug APK
./gradlew assembleDebug
```

### Check for Compilation Errors

If you see errors related to notifications, check:

1. **Missing imports**:
   - `NotificationListener` might not be recognized in some files
   - Solution: The file exists, just need to rebuild

2. **Serialization errors**:
   - `SupabaseNotification` data class needs `@Serializable`
   - Solution: Already added

3. **Supabase version**:
   - Ensure you have latest Supabase Kotlin SDK
   - Check `build.gradle.kts` dependencies

---

## Step 5: Test Notifications (When App Runs)

### Important: NotificationListener Not Yet Wired

**Current Status**: The `NotificationListener` class exists but is **not yet integrated** into MainActivity.

**What Works Now**:
- ✅ Notifications are inserted into database when tasks are created/updated
- ✅ You can query notifications via SQL
- ❌ Android app does NOT yet display notifications automatically
- ❌ NotificationListener is not started

**To Test Database Insertion**:

1. **Run the app**
2. **Create a task and assign to another user**
3. **Check database**:

```sql
-- Check if notification was created
SELECT * FROM notifications
ORDER BY created_at DESC
LIMIT 10;
```

You should see notifications inserted when:
- Task assigned to user
- Task status changed
- Task priority changed
- Task updated with changes

### To Fully Enable Notifications (Next Steps)

You need to wire `NotificationListener` in MainActivity. I can help with this after we verify the database migration works.

---

## Step 6: Test Realtime Subscription (Advanced)

### Via JavaScript Console

If you want to test Realtime before wiring in Android:

1. Open Supabase Dashboard
2. Go to any page
3. Open browser DevTools → Console
4. Run this script:

```javascript
// Replace with your actual values
const SUPABASE_URL = 'https://krbfvekgqbcwjgntepip.supabase.co'
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtyYmZ2ZWtncWJjd2pnbnRlcGlwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjE0MDc5NjIsImV4cCI6MjA3Njk4Mzk2Mn0.fQlOBpHiyC5FP0CzcvX3tWK6HVQVeIaC-9gHdrkOFOs'
const YOUR_USER_ID = 'your-user-id-here' // Replace

// Subscribe to notifications
const { createClient } = supabase
const client = createClient(SUPABASE_URL, SUPABASE_ANON_KEY)

const channel = client
  .channel('notifications')
  .on(
    'postgres_changes',
    {
      event: 'INSERT',
      schema: 'public',
      table: 'notifications',
      filter: `user_id=eq.${YOUR_USER_ID}`
    },
    (payload) => {
      console.log('🔔 New notification received:', payload.new)
      alert(`Notification: ${payload.new.title}`)
    }
  )
  .subscribe()

console.log('✅ Subscribed to notifications')
```

2. **Insert a test notification** in SQL Editor:

```sql
INSERT INTO notifications (user_id, title, body, type, data, created_at)
VALUES (
    'YOUR_USER_ID',  -- Same user ID as above
    'Real-time Test',
    'Testing Realtime subscription',
    'info',
    '{}'::jsonb,
    EXTRACT(EPOCH FROM NOW()) * 1000
);
```

3. **Check browser console** - you should see:
   - "🔔 New notification received: ..."
   - Browser alert with notification title

If this works, Realtime is configured correctly!

---

## Troubleshooting

### Migration Fails

**Error**: `relation "users" does not exist`
- **Solution**: Ensure you've run the main database schema first (users table must exist)

**Error**: `permission denied for table notifications`
- **Solution**: You need service_role key, or run as postgres user

**Error**: `syntax error near "CREATE"`
- **Solution**: Copy entire SQL file, don't copy parts

### Realtime Not Working

**Check Realtime is enabled**:

```sql
-- Should return at least 1 row with 'notifications'
SELECT * FROM pg_publication_tables
WHERE pubname = 'supabase_realtime';
```

**If no rows**, manually enable:

```sql
ALTER PUBLICATION supabase_realtime ADD TABLE notifications;
```

**Verify in Dashboard**:
- Database → Replication → notifications should show "Enabled"

### Android Build Fails

**Error**: `Unresolved reference: NotificationListener`
- **Solution**: Rebuild project (`./gradlew clean build`)
- The file exists, Gradle might not have indexed it

**Error**: `Could not find kotlinx-serialization-json`
- **Solution**: Check `build.gradle.kts` has serialization plugin and dependency

**Error**: `Supabase.from() not found`
- **Solution**: Update Supabase Kotlin SDK to latest version

---

## Quick Verification Checklist

After running migration, verify:

- [ ] Table `notifications` exists in Supabase
- [ ] 9 columns present (id, user_id, title, body, type, data, is_read, created_at, updated_at)
- [ ] 4-5 indexes created
- [ ] 4 RLS policies created
- [ ] Realtime enabled for notifications table
- [ ] Test notification inserted successfully
- [ ] Android app builds without errors
- [ ] (Future) NotificationListener wired in MainActivity
- [ ] (Future) Android notifications display when task assigned

---

## Next Steps After Migration

1. **Verify migration successful** ✓
2. **Build Android app** ✓
3. **Wire NotificationListener in MainActivity** (I can help with this)
4. **Add notification icon resource**
5. **Request notification permission**
6. **Test end-to-end**

---

## Need Help?

If you encounter issues:

1. Check Supabase logs: Dashboard → Logs → select "Postgres Logs"
2. Check Android logcat: `adb logcat | grep -i notification`
3. Refer to `/documents/REALTIME_NOTIFICATIONS_COMPLETE.md` for detailed troubleshooting

---

**Created**: January 3, 2026
**Supabase Project**: krbfvekgqbcwjgntepip
**Database**: PostgreSQL via Supabase
