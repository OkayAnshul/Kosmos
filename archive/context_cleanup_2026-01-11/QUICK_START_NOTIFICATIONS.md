# 🚀 Quick Start: Notification System Testing

## ⚡ 3-Minute Setup

### Step 1: Run Migration (1 minute)

1. Open Supabase Dashboard → SQL Editor
2. Copy contents of `NOTIFICATIONS_TABLE_MIGRATION.sql`
3. Paste and click **Run**
4. ✅ Should see "Success" with table details

### Step 2: Quick Test (1 minute)

Get a user ID:
```sql
SELECT id, username FROM users LIMIT 1;
```

Insert test notification (replace USER_ID):
```sql
INSERT INTO notifications (user_id, title, body, type, data, created_at)
VALUES (
    'PASTE_USER_ID_HERE',
    'Test Alert',
    'System is working!',
    'info',
    '{}'::jsonb,
    EXTRACT(EPOCH FROM NOW()) * 1000
)
RETURNING *;
```

Verify:
```sql
SELECT * FROM notifications ORDER BY created_at DESC LIMIT 1;
```

### Step 3: Test in App (1 minute)

1. Install app: `./gradlew installDebug`
2. Log in as a user
3. Create a task and assign it to another user
4. Check Android notifications appear

---

## 📱 Real-Time Test

**With app open:**
1. Log into app as User A
2. In Supabase SQL Editor, insert notification for User A
3. Notification should appear within 2 seconds

**Expected Behavior:**
- ✅ Notification appears instantly
- ✅ Unread count updates
- ✅ Can tap notification (when UI implemented)

---

## 🔍 Quick Verification

### Check Table Exists
```sql
SELECT COUNT(*) FROM notifications;
```

### Check Realtime Enabled
```sql
SELECT * FROM pg_publication_tables
WHERE pubname = 'supabase_realtime'
AND tablename = 'notifications';
```

### Check Policies
```sql
SELECT policyname FROM pg_policies
WHERE tablename = 'notifications';
```
Should show 4 policies.

---

## 🐛 Quick Fixes

**Problem: "table already exists"**
```sql
DROP TABLE IF EXISTS notifications CASCADE;
-- Then re-run migration
```

**Problem: "Realtime not enabled"**
```sql
ALTER PUBLICATION supabase_realtime ADD TABLE notifications;
```

**Problem: "Permission denied"**
- Check you're using the correct Supabase anon key in app
- RLS policies may need adjustment

---

## 📊 What's Been Built

✅ **Database:**
- notifications table with RLS
- 5 indexes for performance
- Realtime enabled
- Auto-update trigger

✅ **Backend (Android):**
- SupabaseNotificationService (sends notifications)
- NotificationRulesEngine (determines who gets notified)
- NotificationListener (receives Realtime events)
- ReminderScheduler (schedules due date reminders)
- TaskReminderWorker (background job for reminders)

✅ **Integration:**
- Automatic notifications on all task operations
- Real-time delivery via Supabase Realtime
- Rate limiting (5 min cooldown per user/task)
- User-specific filtering

⚠️ **Not Yet Built (Future Work):**
- UI to display notifications
- "Mark as read" UI
- Notification badge in app bar
- Deep links (tap notification → open task)
- User preferences (mute/unmute)

---

## 📁 Files Reference

- `NOTIFICATIONS_TABLE_MIGRATION.sql` - Main migration
- `TEST_NOTIFICATIONS.sql` - Test queries
- `NOTIFICATION_TESTING_GUIDE.md` - Full testing guide (this file)
- `app/.../SupabaseNotificationService.kt` - Service
- `app/.../NotificationRulesEngine.kt` - Business logic
- `app/.../NotificationListener.kt` - Realtime listener

---

## 🎯 Success Checklist

Database:
- [ ] Migration runs successfully
- [ ] Can insert notifications via SQL
- [ ] Can query notifications
- [ ] RLS policies active
- [ ] Realtime enabled

App:
- [ ] Build successful (✅ Already done!)
- [ ] App installs on device
- [ ] Can log in
- [ ] Creating/updating tasks triggers notifications
- [ ] Notifications appear in Android notification tray

---

**Next Steps After Testing:**
1. Implement NotificationScreen UI
2. Add notification badge to app bar
3. Implement "mark as read" functionality
4. Add deep links for tap navigation
5. Add user preferences for notifications

For detailed instructions, see `NOTIFICATION_TESTING_GUIDE.md`.
