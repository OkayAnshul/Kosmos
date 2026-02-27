-- ============================================================================
-- NOTIFICATIONS TABLE MIGRATION
-- ============================================================================
-- Purpose: Create notifications table for in-app notifications via Supabase Realtime
-- Date: 2026-01-03
-- Dependencies: users table
-- ============================================================================

-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    type TEXT NOT NULL, -- task_assigned, task_status_changed, task_comment, task_reminder, etc.
    data JSONB DEFAULT '{}'::jsonb, -- Additional data (task_id, project_id, etc.)
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at BIGINT NOT NULL, -- Millisecond timestamp
    updated_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,

    -- Indexes for performance
    CONSTRAINT notifications_type_check CHECK (type IN (
        'task_assigned',
        'task_status_changed',
        'task_priority_changed',
        'task_comment',
        'task_due_date_changed',
        'task_created',
        'task_deleted',
        'task_updated',
        'task_reminder',
        'task_activity',
        'info'
    ))
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications(user_id, is_read)
    WHERE is_read = false;

-- Create composite index for common queries
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications(user_id, created_at DESC);

-- Enable Row Level Security
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- RLS Policies
-- Users can only read their own notifications
CREATE POLICY "Users can view own notifications"
    ON notifications
    FOR SELECT
    USING (auth.uid() = user_id);

-- Users can update their own notifications (mark as read)
CREATE POLICY "Users can update own notifications"
    ON notifications
    FOR UPDATE
    USING (auth.uid() = user_id);

-- System can insert notifications (via service role)
CREATE POLICY "System can insert notifications"
    ON notifications
    FOR INSERT
    WITH CHECK (true); -- Allow inserts from service role

-- Users can delete their own notifications
CREATE POLICY "Users can delete own notifications"
    ON notifications
    FOR DELETE
    USING (auth.uid() = user_id);

-- Enable Realtime for notifications table
ALTER PUBLICATION supabase_realtime ADD TABLE notifications;

-- Create function to automatically update updated_at
CREATE OR REPLACE FUNCTION update_notification_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = EXTRACT(EPOCH FROM NOW()) * 1000;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for updated_at
CREATE TRIGGER notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_notification_updated_at();

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Verify table creation
SELECT
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'notifications'
ORDER BY ordinal_position;

-- Verify indexes
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'notifications';

-- Verify RLS policies
SELECT
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual,
    with_check
FROM pg_policies
WHERE tablename = 'notifications';

-- ============================================================================
-- USAGE EXAMPLES
-- ============================================================================

-- Insert a notification (from service role)
/*
INSERT INTO notifications (user_id, title, body, type, data, created_at)
VALUES (
    'user-uuid-here',
    'Task assigned',
    'You were assigned to: Implement feature X',
    'task_assigned',
    '{"task_id": "task-uuid", "project_id": "project-uuid"}'::jsonb,
    EXTRACT(EPOCH FROM NOW()) * 1000
);
*/

-- Get unread notifications for a user
/*
SELECT * FROM notifications
WHERE user_id = 'user-uuid-here'
  AND is_read = false
ORDER BY created_at DESC
LIMIT 20;
*/

-- Mark notification as read
/*
UPDATE notifications
SET is_read = true
WHERE id = 'notification-uuid-here'
  AND user_id = 'user-uuid-here';
*/

-- Mark all notifications as read for a user
/*
UPDATE notifications
SET is_read = true
WHERE user_id = 'user-uuid-here'
  AND is_read = false;
*/

-- Get unread count for a user
/*
SELECT COUNT(*) as unread_count
FROM notifications
WHERE user_id = 'user-uuid-here'
  AND is_read = false;
*/

-- Delete old read notifications (cleanup)
/*
DELETE FROM notifications
WHERE is_read = true
  AND created_at < (EXTRACT(EPOCH FROM NOW() - INTERVAL '30 days') * 1000);
*/

-- ============================================================================
-- NOTES
-- ============================================================================
-- 1. Notifications are delivered via Supabase Realtime subscriptions
-- 2. Android app subscribes to notifications table filtered by user_id
-- 3. When a new row is inserted, Realtime immediately notifies the app
-- 4. No external push notification service (FCM/APNS) required
-- 5. Works only when app is open (in-app notifications only)
-- 6. For background notifications, would need FCM/APNS integration
-- ============================================================================
