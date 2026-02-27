-- ============================================================================
-- NOTIFICATION SYSTEM TEST SCRIPT
-- ============================================================================
-- Run this after the main migration to test the notification system
-- ============================================================================

-- Step 1: Verify the notifications table exists
SELECT COUNT(*) as table_exists
FROM information_schema.tables
WHERE table_name = 'notifications';
-- Expected: 1

-- Step 2: Verify RLS is enabled
SELECT tablename, rowsecurity
FROM pg_tables
WHERE tablename = 'notifications';
-- Expected: rowsecurity = true

-- Step 3: Verify Realtime publication includes notifications
SELECT schemaname, tablename
FROM pg_publication_tables
WHERE pubname = 'supabase_realtime' AND tablename = 'notifications';
-- Expected: 1 row with tablename = 'notifications'

-- ============================================================================
-- MANUAL TEST: Insert a Test Notification
-- ============================================================================
-- Replace 'YOUR_USER_ID_HERE' with an actual user ID from your users table

-- First, get a valid user_id from your users table:
SELECT id, username, email
FROM users
LIMIT 5;

-- Then insert a test notification (replace the user_id):
/*
INSERT INTO notifications (user_id, title, body, type, data, created_at)
VALUES (
    'REPLACE_WITH_ACTUAL_USER_ID',
    'Test Notification',
    'This is a test notification from the migration script',
    'info',
    '{"test": true}'::jsonb,
    EXTRACT(EPOCH FROM NOW()) * 1000
)
RETURNING *;
*/

-- ============================================================================
-- VERIFY TEST NOTIFICATION
-- ============================================================================
-- Get the most recent notification:
SELECT
    id,
    user_id,
    title,
    body,
    type,
    is_read,
    to_timestamp(created_at / 1000) as created_timestamp
FROM notifications
ORDER BY created_at DESC
LIMIT 5;

-- ============================================================================
-- TEST QUERIES FOR APP FUNCTIONALITY
-- ============================================================================

-- Query 1: Get unread notifications for a user (what the app will do)
/*
SELECT * FROM notifications
WHERE user_id = 'REPLACE_WITH_ACTUAL_USER_ID'
  AND is_read = false
ORDER BY created_at DESC;
*/

-- Query 2: Get unread count (what NotificationListener does)
/*
SELECT COUNT(*) as unread_count
FROM notifications
WHERE user_id = 'REPLACE_WITH_ACTUAL_USER_ID'
  AND is_read = false;
*/

-- Query 3: Mark notification as read (what the app will do)
/*
UPDATE notifications
SET is_read = true
WHERE id = 'REPLACE_WITH_NOTIFICATION_ID'
RETURNING *;
*/

-- Query 4: Mark all as read
/*
UPDATE notifications
SET is_read = true
WHERE user_id = 'REPLACE_WITH_ACTUAL_USER_ID'
  AND is_read = false
RETURNING COUNT(*);
*/

-- ============================================================================
-- CLEANUP TEST DATA (Optional)
-- ============================================================================
-- Remove test notifications if needed:
/*
DELETE FROM notifications
WHERE type = 'info' AND data->>'test' = 'true';
*/

-- ============================================================================
-- EXPECTED BEHAVIOR IN APP
-- ============================================================================
-- When you insert a notification:
-- 1. Supabase Realtime immediately broadcasts the INSERT event
-- 2. NotificationListener receives the event in the app
-- 3. App shows Android notification (if app is open)
-- 4. Unread count updates in the UI
--
-- To test this fully:
-- 1. Install the app on a device/emulator
-- 2. Log in as a user
-- 3. Run the INSERT query above in Supabase SQL Editor
-- 4. The app should immediately show the notification
-- ============================================================================
