# Notification System Testing Guide

## 🎯 Overview

This guide walks you through testing the newly integrated Supabase Realtime notification system.

---

## 📋 Prerequisites

- ✅ Build successful (completed)
- ✅ Supabase project set up
- ✅ At least one user in the database
- ⚠️ Migration not yet run (pending)

---

## 🗄️ Step 1: Run Database Migration

### 1.1 Open Supabase Dashboard

1. Go to https://supabase.com/dashboard
2. Select your Kosmos project
3. Click **SQL Editor** in the left sidebar

### 1.2 Execute Migration

1. Click **New Query**
2. Open the file: `NOTIFICATIONS_TABLE_MIGRATION.sql`
3. Copy the entire contents
4. Paste into the SQL Editor
5. Click **Run** (or press `Ctrl+Enter`)

### 1.3 Verify Migration Success

The query will automatically run verification queries at the end. You should see:

**Table Structure:**
```
table_name: notifications
Columns: id, user_id, title, body, type, data, is_read, created_at, updated_at
```

**Indexes Created:**
- `idx_notifications_user_id`
- `idx_notifications_is_read`
- `idx_notifications_created_at`
- `idx_notifications_user_unread`
- `idx_notifications_user_created`

**RLS Policies:**
- "Users can view own notifications" (SELECT)
- "Users can update own notifications" (UPDATE)
- "System can insert notifications" (INSERT)
- "Users can delete own notifications" (DELETE)

**Realtime Enabled:**
- Table `notifications` should be in `supabase_realtime` publication

---

## 🧪 Step 2: Manual Database Testing

### 2.1 Get a Test User ID

Run this query in Supabase SQL Editor:

```sql
SELECT id, username, email
FROM users
LIMIT 5;
```

Copy one of the user IDs (you'll need it for testing).

### 2.2 Insert a Test Notification

Replace `YOUR_USER_ID_HERE` with the actual user ID from above:

```sql
INSERT INTO notifications (user_id, title, body, type, data, created_at)
VALUES (
    'YOUR_USER_ID_HERE',
    'Test Notification',
    'This is a test notification to verify the system works',
    'info',
    '{"test": true}'::jsonb,
    EXTRACT(EPOCH FROM NOW()) * 1000
)
RETURNING *;
```

**Expected Result:**
- One row inserted
- You should see the notification with all fields populated
- `is_read` should be `false`
- `created_at` should be current timestamp (in milliseconds)

### 2.3 Query Unread Notifications

```sql
SELECT
    id,
    title,
    body,
    type,
    is_read,
    to_timestamp(created_at / 1000) as created_at_readable
FROM notifications
WHERE user_id = 'YOUR_USER_ID_HERE'
  AND is_read = false
ORDER BY created_at DESC;
```

**Expected Result:**
- Should show your test notification
- `is_read` = false
- Recent timestamp

### 2.4 Mark Notification as Read

Replace `NOTIFICATION_ID_HERE` with the ID from the previous query:

```sql
UPDATE notifications
SET is_read = true
WHERE id = 'NOTIFICATION_ID_HERE'
RETURNING *;
```

**Expected Result:**
- Notification updated
- `is_read` = true
- `updated_at` changed to current time

---

## 📱 Step 3: App Integration Testing

### 3.1 Build and Install App

```bash
./gradlew installDebug
```

Or use Android Studio to run the app.

### 3.2 Test Automatic Notifications (Task Operations)

The notification system is automatically triggered when tasks are modified. Here's what to test:

#### Test 1: Task Assignment Notification

1. **In the app**: Log in as User A
2. **In the app**: Create a new task
3. **In the app**: Assign the task to User B
4. **Expected**: User B should receive a notification "You were assigned to: [Task Title]"

**How it works:**
- `TaskRepository.updateTask()` is called
- `TaskRepository.trackActivity()` creates a `TaskActivity` record
- `NotificationRulesEngine.evaluateAndNotify()` determines User B should be notified
- `SupabaseNotificationService.sendNotification()` inserts into `notifications` table
- Supabase Realtime broadcasts INSERT event
- User B's `NotificationListener` receives the event
- Android notification is shown

#### Test 2: Status Change Notification

1. **In the app**: Log in as User A (task creator)
2. **In the app**: Have User B change a task status from TODO to IN_PROGRESS
3. **Expected**: User A receives notification "changed status from To Do to In Progress"

#### Test 3: Comment Notification

1. **In the app**: Log in as User A
2. **In the app**: Have User B add a comment to a task
3. **Expected**: User A and assignee receive "added a comment" notification

### 3.3 Test Realtime Delivery (Advanced)

This tests that notifications appear instantly via Realtime:

1. **Device 1**: Install app, log in as User A, stay on any screen
2. **Supabase Dashboard**: Run this SQL query:

```sql
INSERT INTO notifications (user_id, title, body, type, data, created_at)
VALUES (
    'USER_A_ID_HERE',
    'Manual Test Notification',
    'Testing Realtime delivery from Supabase dashboard',
    'info',
    '{}'::jsonb,
    EXTRACT(EPOCH FROM NOW()) * 1000
);
```

3. **Expected**: Device 1 should show an Android notification immediately (within 1-2 seconds)

**⚠️ Important**: This only works when the app is **open**. Background notifications would require FCM integration.

---

## 🔍 Step 4: Verify Notification Listener

### 4.1 Check Logs

Use `adb logcat` to see notification system activity:

```bash
adb logcat -s NotificationListener SupabaseNotificationService NotificationRulesEngine
```

**Expected Log Messages:**

```
NotificationListener: Starting notification listener for user: [user-id]
NotificationListener: ✅ Successfully subscribed to notifications for user: [user-id]
NotificationListener: Unread count: 0
NotificationListener: Handling new notification: [title]
NotificationListener: ✅ Showed Android notification: [title]
NotificationListener: Unread count: 1
```

### 4.2 Verify Unread Count

The `NotificationListener` maintains an unread count in `StateFlow`. To verify:

1. Insert multiple notifications via SQL
2. Check logs for "Unread count: N" messages
3. Mark some as read via SQL
4. Verify unread count decreases

---

## 🧹 Step 5: Cleanup Test Data

After testing, remove test notifications:

```sql
DELETE FROM notifications
WHERE type = 'info' AND data->>'test' = 'true';
```

Or delete all notifications for a specific user:

```sql
DELETE FROM notifications
WHERE user_id = 'YOUR_USER_ID_HERE';
```

---

## ✅ Success Criteria

### Database Level
- ✅ Migration runs without errors
- ✅ Table created with correct schema
- ✅ RLS policies active
- ✅ Realtime enabled on table
- ✅ Can insert notifications via SQL
- ✅ Can query notifications with filters
- ✅ Can update `is_read` status

### App Level (When Implemented in UI)
- ✅ NotificationListener subscribes successfully
- ✅ Unread count updates in real-time
- ✅ Android notifications appear when notifications inserted
- ✅ Task operations automatically trigger notifications
- ✅ Notifications show correct user-specific data
- ✅ Marking as read updates the database

---

## 🐛 Troubleshooting

### Issue: Migration fails with "relation already exists"

**Solution**: The table already exists. Run this to check:

```sql
SELECT * FROM notifications LIMIT 1;
```

If it returns data, the table is already there. You can skip the migration or drop it first:

```sql
DROP TABLE IF EXISTS notifications CASCADE;
```

Then re-run the migration.

### Issue: "Realtime not working"

**Check 1**: Verify Realtime is enabled on the table:

```sql
SELECT schemaname, tablename
FROM pg_publication_tables
WHERE pubname = 'supabase_realtime' AND tablename = 'notifications';
```

**Check 2**: Verify Supabase client is initialized with Realtime:

Check `SupabaseConfig.kt` includes:

```kotlin
install(Realtime)
```

### Issue: "Notifications not appearing in app"

**Check 1**: Verify NotificationListener is started:

Check `MainActivity.kt` calls:

```kotlin
notificationListener.startListening(currentUserId)
```

**Check 2**: Check Android notification permissions:

For Android 13+, notification permission must be granted.

**Check 3**: Check notification channel:

Run in `adb shell`:

```bash
adb shell cmd notification list_channels com.example.kosmos
```

Should show `kosmos_notifications` channel.

### Issue: "RLS blocks inserts"

**Symptom**: Notifications insert fails with permission error.

**Solution**: The INSERT policy allows service role to insert. Make sure your Supabase client uses the service role key when inserting notifications, OR change the policy to allow authenticated users:

```sql
DROP POLICY "System can insert notifications" ON notifications;

CREATE POLICY "Authenticated users can insert notifications"
    ON notifications
    FOR INSERT
    WITH CHECK (auth.role() = 'authenticated');
```

---

## 📊 Performance Monitoring

### Query Performance

Check notification query performance:

```sql
EXPLAIN ANALYZE
SELECT * FROM notifications
WHERE user_id = 'some-user-id'
  AND is_read = false
ORDER BY created_at DESC
LIMIT 20;
```

**Expected**: Should use index `idx_notifications_user_unread` (Index Scan, <1ms)

### Realtime Latency

Measure time from INSERT to app notification:

1. Note timestamp before SQL INSERT
2. Check app logs for "Handling new notification" timestamp
3. Calculate difference

**Expected**: < 2 seconds for Realtime delivery

---

## 🎓 Next Steps

After successful testing:

1. **Implement UI**: Create NotificationScreen to display notifications
2. **Add UI Integration**: Wire up NotificationListener in MainActivity
3. **Implement Actions**: Add "Mark as read", "Delete", "Clear all" actions
4. **Add Notification Badge**: Show unread count in app bar
5. **Implement Deep Links**: Tap notification → navigate to relevant task
6. **Add Preferences**: Let users configure notification types

---

## 📝 Notes

- **In-App Only**: This implementation only works when app is **open**
- **No Background**: For background notifications, FCM integration required
- **Battery Friendly**: Realtime uses WebSocket (efficient, low battery impact)
- **Offline Support**: Notifications queued when offline, delivered when back online
- **RLS Security**: Users can only see their own notifications (enforced by Supabase)

---

**Last Updated**: January 5, 2026
**Status**: Ready for Testing
