-- ================================================================
-- Supabase Schema Verification Queries
-- Run these in Supabase SQL Editor to check current schema
-- ================================================================

-- Query 1: Check ALL tables exist
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Expected: users, chat_rooms, messages, tasks, projects, project_members,
--           voice_messages (if created), action_items (if created)

-- ================================================================
-- Query 2: Check tasks table columns (21 expected)
-- ================================================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'tasks'
ORDER BY ordinal_position;

-- Expected columns:
-- id, project_id, chat_room_id, title, description, status, priority,
-- assigned_to_id, assigned_to_name, assigned_to_role,
-- created_by_id, created_by_name, created_by_role,
-- created_at, updated_at, due_date, source_message_id,
-- tags, comments, parent_task_id, estimated_hours, actual_hours

-- ================================================================
-- Query 3: Check messages table columns (15 expected)
-- ================================================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'messages'
ORDER BY ordinal_position;

-- Expected columns:
-- id, chat_room_id, sender_id, sender_name, sender_photo_url,
-- content, timestamp, type, voice_message_id, task_ids,
-- reply_to_message_id, is_edited, edited_at, reactions, read_by

-- ================================================================
-- Query 4: Check chat_rooms table columns (14 expected)
-- ================================================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'chat_rooms'
ORDER BY ordinal_position;

-- ================================================================
-- Query 5: Check users table columns (17 expected)
-- ================================================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'users'
ORDER BY ordinal_position;

-- ================================================================
-- Query 6: Check projects table columns (11 expected)
-- ================================================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'projects'
ORDER BY ordinal_position;

-- ================================================================
-- Query 7: Check project_members table columns (9 expected)
-- ================================================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'project_members'
ORDER BY ordinal_position;

-- ================================================================
-- Query 8: Check foreign keys for tasks table
-- ================================================================
SELECT
    tc.constraint_name,
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
  AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
  AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_name = 'tasks';

-- ================================================================
-- INSTRUCTIONS:
-- ================================================================
-- 1. Copy this entire file
-- 2. Open Supabase Dashboard → SQL Editor
-- 3. Paste and run each query ONE AT A TIME
-- 4. Copy the results for each query
-- 5. Share the results so I can compare with Kotlin models
-- ================================================================
