# Realtime Notifications System - Complete

**Date**: January 3, 2026
**Status**: ✅ SUPABASE REALTIME IMPLEMENTATION COMPLETE
**Type**: In-App Notifications (No FCM Required)

---

## Overview

The notification system has been updated to use **Supabase Realtime** instead of Firebase Cloud Messaging (FCM). This provides in-app notifications without requiring any external push notification service.

### Key Changes

- ❌ **Removed**: Firebase Cloud Messaging (FCM)
- ❌ **Removed**: Supabase Edge Function (send-notification)
- ✅ **Added**: Notifications database table
- ✅ **Added**: Supabase Realtime subscriptions
- ✅ **Added**: Android local notifications

---

## Architecture

### Old Architecture (FCM)
```
Android App → Supabase Edge Function → FCM → User Device
```
**Issues**: Requires Firebase setup, FCM server key, external dependency

### New Architecture (Supabase Realtime)
```
Android App → Supabase notifications table → Realtime subscription → Android notification
```
**Benefits**: No external dependencies, simpler setup, works with Supabase only

---

## Data Flow

### 1. Sending Notifications

```
User Action (e.g., assign task)
    ↓
TaskRepository.trackActivity()
    ↓
NotificationRulesEngine.evaluateAndNotify()
    ↓
SupabaseNotificationService.sendNotification()
    ↓
INSERT INTO notifications table
    ↓
Supabase Realtime detects INSERT
    ↓
Broadcasts to subscribed clients
    ↓
NotificationListener receives notification
    ↓
Displays Android notification
```

### 2. Receiving Notifications

```
App starts / User logs in
    ↓
NotificationListener.startListening(userId)
    ↓
Subscribe to Supabase Realtime channel
    ↓
Filter: notifications WHERE user_id = userId
    ↓
[Wait for new notifications...]
    ↓
PostgresAction.Insert received
    ↓
handleNewNotification()
    ↓
┌─────────────────────────────────────┐
│ 1. Update unread count              │
│ 2. Create Android notification      │
│ 3. Show notification to user        │
└─────────────────────────────────────┘
```

---

## Database Schema

### notifications Table

```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    type TEXT NOT NULL, -- task_assigned, task_status_changed, etc.
    data JSONB DEFAULT '{}'::jsonb,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at BIGINT NOT NULL,
    updated_at BIGINT
);
```

### Indexes

```sql
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read)
    WHERE is_read = false;
```

### Notification Types

- `task_assigned` - User assigned to task
- `task_status_changed` - Task status changed
- `task_priority_changed` - Task priority changed
- `task_comment` - Comment added to task
- `task_due_date_changed` - Due date changed
- `task_created` - Task created
- `task_deleted` - Task deleted
- `task_updated` - Task updated
- `task_reminder` - Due date reminder
- `task_activity` - General activity
- `info` - General information

---

## Components

### 1. SupabaseNotificationService

**File**: `app/src/main/java/com/example/kosmos/features/notifications/SupabaseNotificationService.kt`

**Purpose**: Insert notifications into the database

**Key Methods**:
```kotlin
suspend fun sendNotification(
    userId: String,
    title: String,
    body: String,
    type: String = "info",
    data: Map<String, String> = emptyMap()
): Result<Unit>
```

**Changes from FCM version**:
- Removed Edge Function call
- Added direct database insert
- Added `type` parameter
- Added `markAsRead()` and `markAllAsRead()` methods

### 2. NotificationListener

**File**: `app/src/main/java/com/example/kosmos/features/notifications/NotificationListener.kt`

**Purpose**: Subscribe to Realtime updates and display Android notifications

**Key Methods**:
```kotlin
fun startListening(userId: String)
fun stopListening()
suspend fun getUnreadNotifications(userId: String): List<SupabaseNotification>
suspend fun getAllNotifications(userId: String, limit: Int = 50): List<SupabaseNotification>
```

**Features**:
- Realtime subscription management
- Automatic Android notification display
- Unread count tracking (StateFlow)
- Deep linking support
- Notification channel management

### 3. NotificationRulesEngine

**File**: `app/src/main/java/com/example/kosmos/features/notifications/NotificationRulesEngine.kt`

**Changes**:
- Added `mapActionTypeToNotificationType()` function
- Updated `sendNotification()` to include `type` parameter

### 4. TaskReminderWorker

**File**: `app/src/main/java/com/example/kosmos/features/notifications/TaskReminderWorker.kt`

**Changes**:
- Updated to use `type = "task_reminder"`
- Removed `type` from data map (now top-level parameter)

---

## Integration Points

### Application Level

**In MainActivity or Application.onCreate()**:

```kotlin
@Inject
lateinit var notificationListener: NotificationListener

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Start listening when user is logged in
    authViewModel.currentUser.observe(this) { user ->
        if (user != null) {
            notificationListener.startListening(user.id)
        } else {
            notificationListener.stopListening()
        }
    }
}
```

### UI Integration

**Display unread count in app bar**:

```kotlin
val unreadCount by notificationListener.unreadCount.collectAsState()

TopAppBar(
    title = { Text("Kosmos") },
    actions = {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge { Text("$unreadCount") }
                }
            }
        ) {
            IconButton(onClick = { /* Navigate to notifications screen */ }) {
                Icon(Icons.Default.Notifications, "Notifications")
            }
        }
    }
)
```

---

## Deployment Steps

### 1. Database Migration

Run the SQL migration:

```bash
# In Supabase SQL Editor or via CLI
psql -h your-project.supabase.co -U postgres -d postgres -f NOTIFICATIONS_TABLE_MIGRATION.sql
```

Or manually execute `/NOTIFICATIONS_TABLE_MIGRATION.sql` in Supabase dashboard.

### 2. Enable Realtime

Verify Realtime is enabled for notifications table:

```sql
-- Check if table is in Realtime publication
SELECT * FROM pg_publication_tables
WHERE tablename = 'notifications';

-- If not, add it:
ALTER PUBLICATION supabase_realtime ADD TABLE notifications;
```

### 3. Android Notification Icon

Add notification icon to your app:

**File**: `app/src/main/res/drawable/ic_notification.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z"/>
</vector>
```

### 4. Android Notification Permission

**For Android 13+ (API 33+)**, request notification permission:

```kotlin
// In MainActivity
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    requestPermissions(
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        NOTIFICATION_PERMISSION_REQUEST_CODE
    )
}
```

**Add to AndroidManifest.xml**:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

### 5. Wire NotificationListener

Inject into MainActivity and start listening:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var notificationListener: NotificationListener

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start listening when user logs in
        lifecycleScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    notificationListener.startListening(user.id)
                } else {
                    notificationListener.stopListening()
                }
            }
        }
    }
}
```

---

## Testing

### Unit Tests

```kotlin
@Test
fun `sendNotification inserts into database`() = runTest {
    val service = SupabaseNotificationService(supabase)

    val result = service.sendNotification(
        userId = "user-123",
        title = "Test",
        body = "Test notification",
        type = "info"
    )

    assertTrue(result.isSuccess)
}

@Test
fun `NotificationListener receives Realtime updates`() = runTest {
    val listener = NotificationListener(context, supabase)
    listener.startListening("user-123")

    // Insert notification
    supabase.from("notifications").insert(...)

    // Verify Android notification was shown
    delay(1000)
    verify(notificationManager).notify(any(), any())
}
```

### Integration Tests

1. **Create task and assign to user**
   - Verify notification inserted into database
   - Verify Realtime broadcasts to user
   - Verify Android notification displayed

2. **Update task status**
   - Verify assignee receives notification
   - Verify creator receives notification (if different)

3. **Add comment with @mention**
   - Verify mentioned user receives notification

4. **Mark notification as read**
   - Verify `is_read` updated in database
   - Verify unread count decreases

### Manual Testing

```sql
-- Insert test notification manually
INSERT INTO notifications (user_id, title, body, type, data, created_at)
VALUES (
    'your-user-id',
    'Test Notification',
    'This is a test notification',
    'info',
    '{}'::jsonb,
    EXTRACT(EPOCH FROM NOW()) * 1000
);

-- Verify Android notification appears immediately
```

---

## Limitations

### In-App Only

**Current Implementation**: Notifications only work when app is open or in background.

**Limitation**: No notifications when app is fully closed.

**Workaround (if needed)**:
- Add WorkManager periodic job to check for new notifications
- Or implement FCM for background notifications
- Or use Supabase Edge Functions with scheduled tasks

### Battery Optimization

Android may kill Realtime connections on aggressive battery optimization.

**Mitigation**:
- Request battery optimization exemption
- Use WorkManager as fallback
- Reconnect on app resume

---

## Future Enhancements

### 1. Notification Center Screen

Create a dedicated screen to view all notifications:

```kotlin
@Composable
fun NotificationCenterScreen(
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    LazyColumn {
        item {
            Text("$unreadCount unread notifications")
            TextButton(onClick = { viewModel.markAllAsRead() }) {
                Text("Mark all as read")
            }
        }

        items(notifications) { notification ->
            NotificationCard(
                notification = notification,
                onClick = { viewModel.markAsRead(notification.id) }
            )
        }
    }
}
```

### 2. Notification Preferences UI

Let users customize notification settings:

```kotlin
@Composable
fun NotificationSettingsScreen() {
    var notifyOnAssignment by remember { mutableStateOf(true) }
    var notifyOnComments by remember { mutableStateOf(true) }
    var quietHoursEnabled by remember { mutableStateOf(false) }

    Column {
        SwitchRow("Notify on task assignment", notifyOnAssignment)
        SwitchRow("Notify on comments", notifyOnComments)
        SwitchRow("Enable quiet hours", quietHoursEnabled)
    }
}
```

### 3. Rich Notifications

Add notification actions:

```kotlin
val builder = NotificationCompat.Builder(context, CHANNEL_ID)
    .addAction(
        R.drawable.ic_done,
        "Mark Done",
        createActionIntent("mark_done", taskId)
    )
    .addAction(
        R.drawable.ic_view,
        "View Task",
        createActionIntent("view_task", taskId)
    )
```

### 4. Notification Grouping

Group related notifications:

```kotlin
.setGroup("task_updates")
.setGroupSummary(true)
```

---

## Files Created/Modified

### Created Files

1. **NOTIFICATIONS_TABLE_MIGRATION.sql** (185 lines)
   - Database schema for notifications table
   - RLS policies
   - Indexes
   - Realtime enablement

2. **NotificationListener.kt** (300 lines)
   - Realtime subscription management
   - Android notification display
   - Unread count tracking

### Modified Files

1. **SupabaseNotificationService.kt**
   - Removed Edge Function call
   - Added database insert
   - Added `type` parameter
   - Added helper methods (markAsRead, markAllAsRead)

2. **NotificationRulesEngine.kt**
   - Added `mapActionTypeToNotificationType()` function
   - Updated `sendNotification()` to use `type` parameter

3. **TaskReminderWorker.kt**
   - Updated to use `type = "task_reminder"`
   - Removed `type` from data map

---

## Comparison: FCM vs Realtime

| Feature | FCM (Old) | Realtime (New) |
|---------|-----------|----------------|
| Push notifications when app closed | ✅ Yes | ❌ No |
| Setup complexity | High (Firebase project, server key) | Low (Supabase only) |
| External dependencies | Firebase, Edge Function | None |
| Real-time delivery | Yes (via FCM) | Yes (via Realtime) |
| Works offline | Queue and send later | Queue and send later |
| Cost | Free (Firebase quota) | Free (Supabase quota) |
| Notification customization | Full (Android NotificationCompat) | Full (Android NotificationCompat) |
| Data payload | Limited (4KB) | Unlimited (JSONB) |
| Delivery guarantee | High | High (when app open) |

---

## Migration Checklist

### Backend
- [x] Create notifications table in Supabase
- [x] Enable RLS policies
- [x] Enable Realtime for notifications table
- [x] Create indexes
- [ ] Run migration in production
- [ ] Verify Realtime working with test insert

### Android App
- [x] Update SupabaseNotificationService
- [x] Create NotificationListener
- [x] Update NotificationRulesEngine
- [x] Update TaskReminderWorker
- [ ] Add notification icon resource
- [ ] Request notification permission (Android 13+)
- [ ] Wire NotificationListener in MainActivity
- [ ] Test end-to-end

### Cleanup
- [ ] Delete send-notification Edge Function (if exists)
- [ ] Remove FCM references from documentation
- [ ] Remove FCM_SERVER_KEY environment variable
- [ ] Update README with new notification system

---

## Summary

**Status**: ✅ Implementation complete, ready for testing

**Changes**: 5 files modified, 2 files created, ~485 new lines of code

**Benefits**:
- Simpler architecture (no external dependencies)
- Faster setup (no Firebase configuration)
- Lower cost (no Edge Function invocations)
- Better integration with existing Supabase stack

**Trade-offs**:
- No push notifications when app is fully closed
- Requires app to be open/background for notifications
- Suitable for in-app notification use case

**Next Steps**:
1. Run database migration
2. Add notification icon
3. Wire NotificationListener in MainActivity
4. Test end-to-end
5. Deploy to production

---

**Date**: January 3, 2026
**Version**: 2.0 (Realtime)
**Previous Version**: 1.0 (FCM - deprecated)
