-- ============================================
-- COMPLETE SCHEMA FIX V2: All Tables Migration (FIXED)
-- ============================================
-- Project: Kosmos Android App
-- Date: 2025-10-31
-- Version: 2.0 (Fixed foreign key issue from V1)
-- Purpose: Fix ALL schema mismatches between Kotlin models and Supabase database
-- Based on: SCHEMA_ANALYSIS_COMPLETE.md
-- ============================================
-- IMPORTANT: This is the CORRECTED version. Do NOT use SCHEMA_FIX_COMPLETE.sql (V1)
-- V1 had a bug where it tried to create foreign keys for columns that don't exist yet.
-- ============================================

-- ============================================
-- STEP 1: DIAGNOSTIC QUERIES (Run First)
-- ============================================
-- These queries show the current state of all tables
-- Copy the output for your records before making changes

-- Check what tables exist
SELECT
table_name,
table_type
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_name IN ('users', 'messages', 'chat_rooms', 'tasks', 'projects', 'project_members', 'voice_messages', 'action_items')
ORDER BY table_name;

-- Check messages table structure (DETAILED)
SELECT
column_name,
data_type,
is_nullable,
column_default
FROM information_schema.columns
WHERE table_name = 'messages'
ORDER BY ordinal_position;

-- Count messages columns
SELECT COUNT(*) as messages_column_count
FROM information_schema.columns
WHERE table_name = 'messages';
-- Expected after fix: 15 columns

-- Check chat_rooms table structure
SELECT
column_name,
data_type,
is_nullable,
column_default
FROM information_schema.columns
WHERE table_name = 'chat_rooms'
ORDER BY ordinal_position;

-- Count chat_rooms columns
SELECT COUNT(*) as chat_rooms_column_count
FROM information_schema.columns
WHERE table_name = 'chat_rooms';
-- Expected after fix: 14 columns

-- Check tasks table structure
SELECT
column_name,
data_type,
is_nullable
FROM information_schema.columns
WHERE table_name = 'tasks'
ORDER BY ordinal_position;

-- Check projects table structure
SELECT
column_name,
data_type,
is_nullable
FROM information_schema.columns
WHERE table_name = 'projects'
ORDER BY ordinal_position;

-- Check project_members table structure
SELECT
column_name,
data_type,
is_nullable
FROM information_schema.columns
WHERE table_name = 'project_members'
ORDER BY ordinal_position;

-- Check users table structure (should already be fixed)
SELECT
column_name,
data_type,
is_nullable
FROM information_schema.columns
WHERE table_name = 'users'
ORDER BY ordinal_position;

-- ============================================
-- STEP 2: FIX MESSAGES TABLE (CRITICAL) - ALL MISSING COLUMNS
-- ============================================
-- Error: "Could not find the 'sender_name' column of 'messages'"
-- Root Cause: Multiple missing columns that Kotlin Message.kt expects

-- Expected 15 columns total:
-- 1. id (PRIMARY KEY)
-- 2. chat_room_id
-- 3. sender_id
-- 4. sender_name ← MISSING
-- 5. sender_photo_url ← MISSING
-- 6. content
-- 7. timestamp
-- 8. type
-- 9. voice_message_id ← POSSIBLY MISSING
-- 10. task_ids ← POSSIBLY MISSING (ARRAY)
-- 11. reply_to_message_id ← POSSIBLY MISSING
-- 12. is_edited ← POSSIBLY MISSING
-- 13. edited_at ← POSSIBLY MISSING
-- 14. reactions ← POSSIBLY MISSING (JSONB)
-- 15. read_by ← POSSIBLY MISSING (ARRAY)

-- Add ALL missing columns to messages table
-- Using IF NOT EXISTS so this is safe to re-run
-- IMPORTANT: ID columns use UUID to match existing schema, not TEXT
ALTER TABLE messages
ADD COLUMN IF NOT EXISTS sender_name TEXT,
ADD COLUMN IF NOT EXISTS sender_photo_url TEXT,
ADD COLUMN IF NOT EXISTS voice_message_id UUID,
ADD COLUMN IF NOT EXISTS task_ids TEXT[],
ADD COLUMN IF NOT EXISTS reply_to_message_id UUID,
ADD COLUMN IF NOT EXISTS is_edited BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS edited_at BIGINT,
ADD COLUMN IF NOT EXISTS reactions JSONB DEFAULT '{}'::jsonb,
ADD COLUMN IF NOT EXISTS read_by TEXT[];

-- Now set sender_name as NOT NULL (after adding it)
-- First populate it, then change constraint
UPDATE messages m
SET sender_name = COALESCE(
m.sender_name,
(SELECT COALESCE(u.display_name, u.username, 'Unknown User')
FROM users u
WHERE u.id = m.sender_id)
)
WHERE sender_name IS NULL OR sender_name = '';

-- Set sender_name as NOT NULL now that all rows have values
ALTER TABLE messages
ALTER COLUMN sender_name SET NOT NULL;

-- Update sender_photo_url from users table for existing messages
UPDATE messages m
SET sender_photo_url = u.photo_url
FROM users u
WHERE m.sender_id = u.id
AND m.sender_photo_url IS NULL;

-- Verify messages table fix - should show 15 rows
SELECT
column_name,
data_type,
is_nullable,
column_default
FROM information_schema.columns
WHERE table_name = 'messages'
ORDER BY ordinal_position;

-- Verify critical columns exist
SELECT
'sender_name' as column_name,
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'sender_name'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END as status
UNION ALL
SELECT 'sender_photo_url',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'sender_photo_url'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT 'reply_to_message_id',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'reply_to_message_id'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT 'task_ids',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'task_ids'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT 'read_by',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'read_by'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END;

-- ============================================
-- STEP 3: FIX CHAT_ROOMS TABLE (CRITICAL)
-- ============================================
-- Error: "Could not find the 'participant_ids' column of 'chat_rooms'"
-- Root Cause: Missing participant_ids array column

-- Add missing participant_ids array column
ALTER TABLE chat_rooms
ADD COLUMN IF NOT EXISTS participant_ids TEXT[];

-- Populate participant_ids for existing chat rooms
-- Strategy: Add created_by as first participant
UPDATE chat_rooms
SET participant_ids = ARRAY[created_by]::TEXT[]
WHERE (participant_ids IS NULL OR participant_ids = ARRAY[]::TEXT[])
AND created_by IS NOT NULL;

-- Set as NOT NULL now that all rows have values
ALTER TABLE chat_rooms
ALTER COLUMN participant_ids SET NOT NULL;

-- Set default for new rows
ALTER TABLE chat_rooms
ALTER COLUMN participant_ids SET DEFAULT ARRAY[]::TEXT[];

-- Verify chat_rooms table fix
SELECT
column_name,
data_type,
is_nullable,
column_default
FROM information_schema.columns
WHERE table_name = 'chat_rooms'
AND column_name = 'participant_ids';

-- Check sample data
SELECT
id,
name,
participant_ids,
array_length(participant_ids, 1) as participant_count
FROM chat_rooms
LIMIT 5;

-- ============================================
-- STEP 4: VERIFY TASKS TABLE
-- ============================================
-- Expected 21 columns (see SCHEMA_ANALYSIS_COMPLETE.md)

-- Check if all required columns exist
SELECT
COUNT(*) as total_columns,
COUNT(CASE WHEN column_name = 'id' THEN 1 END) as has_id,
COUNT(CASE WHEN column_name = 'project_id' THEN 1 END) as has_project_id,
COUNT(CASE WHEN column_name = 'chat_room_id' THEN 1 END) as has_chat_room_id,
COUNT(CASE WHEN column_name = 'title' THEN 1 END) as has_title,
COUNT(CASE WHEN column_name = 'description' THEN 1 END) as has_description,
COUNT(CASE WHEN column_name = 'status' THEN 1 END) as has_status,
COUNT(CASE WHEN column_name = 'priority' THEN 1 END) as has_priority,
COUNT(CASE WHEN column_name = 'assigned_to_id' THEN 1 END) as has_assigned_to_id,
COUNT(CASE WHEN column_name = 'assigned_to_name' THEN 1 END) as has_assigned_to_name,
COUNT(CASE WHEN column_name = 'assigned_to_role' THEN 1 END) as has_assigned_to_role,
COUNT(CASE WHEN column_name = 'created_by_id' THEN 1 END) as has_created_by_id,
COUNT(CASE WHEN column_name = 'created_by_name' THEN 1 END) as has_created_by_name,
COUNT(CASE WHEN column_name = 'created_by_role' THEN 1 END) as has_created_by_role,
COUNT(CASE WHEN column_name = 'created_at' THEN 1 END) as has_created_at,
COUNT(CASE WHEN column_name = 'updated_at' THEN 1 END) as has_updated_at,
COUNT(CASE WHEN column_name = 'due_date' THEN 1 END) as has_due_date,
COUNT(CASE WHEN column_name = 'source_message_id' THEN 1 END) as has_source_message_id,
COUNT(CASE WHEN column_name = 'tags' THEN 1 END) as has_tags,
COUNT(CASE WHEN column_name = 'comments' THEN 1 END) as has_comments,
COUNT(CASE WHEN column_name = 'parent_task_id' THEN 1 END) as has_parent_task_id,
COUNT(CASE WHEN column_name = 'estimated_hours' THEN 1 END) as has_estimated_hours,
COUNT(CASE WHEN column_name = 'actual_hours' THEN 1 END) as has_actual_hours
FROM information_schema.columns
WHERE table_name = 'tasks';

-- If total_columns != 21, you're missing columns. Check the output above to see which ones.

-- ============================================
-- STEP 5: VERIFY PROJECTS TABLE
-- ============================================
-- Expected 11 columns

SELECT
COUNT(*) as total_columns,
COUNT(CASE WHEN column_name = 'id' THEN 1 END) as has_id,
COUNT(CASE WHEN column_name = 'name' THEN 1 END) as has_name,
COUNT(CASE WHEN column_name = 'description' THEN 1 END) as has_description,
COUNT(CASE WHEN column_name = 'owner_id' THEN 1 END) as has_owner_id,
COUNT(CASE WHEN column_name = 'status' THEN 1 END) as has_status,
COUNT(CASE WHEN column_name = 'visibility' THEN 1 END) as has_visibility,
COUNT(CASE WHEN column_name = 'created_at' THEN 1 END) as has_created_at,
COUNT(CASE WHEN column_name = 'updated_at' THEN 1 END) as has_updated_at,
COUNT(CASE WHEN column_name = 'image_url' THEN 1 END) as has_image_url,
COUNT(CASE WHEN column_name = 'color' THEN 1 END) as has_color,
COUNT(CASE WHEN column_name = 'settings' THEN 1 END) as has_settings
FROM information_schema.columns
WHERE table_name = 'projects';

-- ============================================
-- STEP 6: VERIFY PROJECT_MEMBERS TABLE
-- ============================================
-- Expected 9 columns

SELECT
COUNT(*) as total_columns,
COUNT(CASE WHEN column_name = 'id' THEN 1 END) as has_id,
COUNT(CASE WHEN column_name = 'project_id' THEN 1 END) as has_project_id,
COUNT(CASE WHEN column_name = 'user_id' THEN 1 END) as has_user_id,
COUNT(CASE WHEN column_name = 'role' THEN 1 END) as has_role,
COUNT(CASE WHEN column_name = 'joined_at' THEN 1 END) as has_joined_at,
COUNT(CASE WHEN column_name = 'invited_by' THEN 1 END) as has_invited_by,
COUNT(CASE WHEN column_name = 'is_active' THEN 1 END) as has_is_active,
COUNT(CASE WHEN column_name = 'last_activity_at' THEN 1 END) as has_last_activity_at,
COUNT(CASE WHEN column_name = 'custom_permissions' THEN 1 END) as has_custom_permissions
FROM information_schema.columns
WHERE table_name = 'project_members';

-- ============================================
-- STEP 7: VERIFY USERS TABLE
-- ============================================
-- Should already be fixed from previous session
-- Expected 17 columns

SELECT
COUNT(*) as total_columns,
COUNT(CASE WHEN column_name = 'username' AND is_nullable = 'NO' THEN 1 END) as has_username_not_null
FROM information_schema.columns
WHERE table_name = 'users';

-- Check for any NULL usernames (should be 0)
SELECT COUNT(*) as null_username_count
FROM users
WHERE username IS NULL OR username = '';

-- ============================================
-- STEP 8: ADD PERFORMANCE INDEXES
-- ============================================
-- These indexes improve query performance for common operations

-- Messages table indexes
CREATE INDEX IF NOT EXISTS idx_messages_chat_room_id ON messages(chat_room_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_messages_chat_room_timestamp ON messages(chat_room_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_messages_type ON messages(type);

-- Chat rooms table indexes
CREATE INDEX IF NOT EXISTS idx_chat_rooms_project_id ON chat_rooms(project_id);
CREATE INDEX IF NOT EXISTS idx_chat_rooms_created_by ON chat_rooms(created_by);
CREATE INDEX IF NOT EXISTS idx_chat_rooms_participant_ids ON chat_rooms USING GIN(participant_ids);

-- Tasks table indexes
CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_chat_room_id ON tasks(chat_room_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to_id ON tasks(assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_tasks_created_by_id ON tasks(created_by_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority);
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);

-- Projects table indexes
CREATE INDEX IF NOT EXISTS idx_projects_owner_id ON projects(owner_id);
CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);

-- Project members table indexes
CREATE INDEX IF NOT EXISTS idx_project_members_project_id ON project_members(project_id);
CREATE INDEX IF NOT EXISTS idx_project_members_user_id ON project_members(user_id);
CREATE INDEX IF NOT EXISTS idx_project_members_project_user ON project_members(project_id, user_id);

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- ============================================
-- STEP 9: ADD FOREIGN KEY CONSTRAINTS
-- ============================================
-- These ensure referential integrity
-- IMPORTANT: Now safe because all columns exist!

-- Messages table foreign keys
-- Note: We drop existing constraints first to avoid duplicates
DO $$
BEGIN
-- Drop old constraints if they exist
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_messages_chat_room') THEN
ALTER TABLE messages DROP CONSTRAINT fk_messages_chat_room;
END IF;
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_messages_sender') THEN
ALTER TABLE messages DROP CONSTRAINT fk_messages_sender;
END IF;
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_messages_reply_to') THEN
ALTER TABLE messages DROP CONSTRAINT fk_messages_reply_to;
END IF;
END $$;

-- Now add the foreign keys
ALTER TABLE messages
ADD CONSTRAINT fk_messages_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_messages_reply_to FOREIGN KEY (reply_to_message_id) REFERENCES messages(id) ON DELETE SET NULL;

-- Chat rooms table foreign keys
DO $$
BEGIN
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_chat_rooms_project') THEN
ALTER TABLE chat_rooms DROP CONSTRAINT fk_chat_rooms_project;
END IF;
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_chat_rooms_created_by') THEN
ALTER TABLE chat_rooms DROP CONSTRAINT fk_chat_rooms_created_by;
END IF;
END $$;

ALTER TABLE chat_rooms
ADD CONSTRAINT fk_chat_rooms_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_chat_rooms_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE;

-- Tasks table foreign keys
DO $$
BEGIN
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_project') THEN
ALTER TABLE tasks DROP CONSTRAINT fk_tasks_project;
END IF;
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_chat_room') THEN
ALTER TABLE tasks DROP CONSTRAINT fk_tasks_chat_room;
END IF;
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_assigned_to') THEN
ALTER TABLE tasks DROP CONSTRAINT fk_tasks_assigned_to;
END IF;
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_created_by') THEN
ALTER TABLE tasks DROP CONSTRAINT fk_tasks_created_by;
END IF;
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_parent') THEN
ALTER TABLE tasks DROP CONSTRAINT fk_tasks_parent;
END IF;
END $$;

ALTER TABLE tasks
ADD CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_tasks_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE SET NULL,
ADD CONSTRAINT fk_tasks_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users(id) ON DELETE SET NULL,
ADD CONSTRAINT fk_tasks_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_tasks_parent FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE SET NULL;

-- Projects table foreign keys
DO $$
BEGIN
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_projects_owner') THEN
ALTER TABLE projects DROP CONSTRAINT fk_projects_owner;
END IF;
END $$;

ALTER TABLE projects
ADD CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;

-- Project members table foreign keys
DO $$
BEGIN
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_project_members_project') THEN
ALTER TABLE project_members DROP CONSTRAINT fk_project_members_project;
END IF;
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_project_members_user') THEN
ALTER TABLE project_members DROP CONSTRAINT fk_project_members_user;
END IF;
IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_project_members_invited_by') THEN
ALTER TABLE project_members DROP CONSTRAINT fk_project_members_invited_by;
END IF;
END $$;

ALTER TABLE project_members
ADD CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_project_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_project_members_invited_by FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL;

-- ============================================
-- STEP 10: ADD UNIQUE CONSTRAINTS
-- ============================================

-- Ensure unique username (if not already exists)
DO $$
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'unique_username') THEN
ALTER TABLE users ADD CONSTRAINT unique_username UNIQUE (username);
END IF;
END $$;

-- Ensure unique project membership (if not already exists)
DO $$
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'unique_project_member') THEN
ALTER TABLE project_members ADD CONSTRAINT unique_project_member UNIQUE (project_id, user_id);
END IF;
END $$;

-- ============================================
-- STEP 11: RELOAD POSTGREST SCHEMA CACHE
-- ============================================
-- This is CRITICAL - PostgREST must reload its schema cache
-- Otherwise you'll still get PGRST204 errors

NOTIFY pgrst, 'reload schema';

-- ============================================
-- STEP 12: FINAL VERIFICATION
-- ============================================

-- Check all tables have correct column counts
SELECT
table_name,
COUNT(*) as column_count,
CASE table_name
WHEN 'users' THEN 17
WHEN 'messages' THEN 15
WHEN 'chat_rooms' THEN 14
WHEN 'tasks' THEN 21
WHEN 'projects' THEN 11
WHEN 'project_members' THEN 9
ELSE NULL
END as expected_count,
CASE
WHEN COUNT(*) = CASE table_name
    WHEN 'users' THEN 17
    WHEN 'messages' THEN 15
    WHEN 'chat_rooms' THEN 14
    WHEN 'tasks' THEN 21
    WHEN 'projects' THEN 11
    WHEN 'project_members' THEN 9
    ELSE 0
END THEN '✅ CORRECT'
ELSE '❌ MISMATCH'
END as status
FROM information_schema.columns
WHERE table_name IN ('users', 'messages', 'chat_rooms', 'tasks', 'projects', 'project_members')
GROUP BY table_name
ORDER BY table_name;

-- Verify ALL critical columns exist in messages table
SELECT
'messages.sender_name' as column_path,
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'sender_name'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END as status
UNION ALL
SELECT 'messages.sender_photo_url',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'sender_photo_url'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT 'messages.voice_message_id',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'voice_message_id'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT 'messages.task_ids',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'task_ids'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT 'messages.reply_to_message_id',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'reply_to_message_id'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT 'messages.is_edited',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'is_edited'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT 'messages.edited_at',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'edited_at'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT 'messages.reactions',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'reactions'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT 'messages.read_by',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'messages' AND column_name = 'read_by'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT 'chat_rooms.participant_ids',
CASE WHEN EXISTS (
SELECT 1 FROM information_schema.columns
WHERE table_name = 'chat_rooms' AND column_name = 'participant_ids'
) THEN '✅ EXISTS' ELSE '❌ MISSING' END;

-- Check RLS status (should be disabled for testing)
SELECT
tablename,
CASE
WHEN rowsecurity = true THEN '🔒 ENABLED (may block operations)'
ELSE '🔓 DISABLED (testing mode)'
END as rls_status
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'projects', 'project_members')
ORDER BY tablename;

-- Check for any data inconsistencies
SELECT
'Messages with NULL sender_name' as issue,
COUNT(*) as count
FROM messages
WHERE sender_name IS NULL OR sender_name = ''
UNION ALL
SELECT
'Chat rooms with empty participant_ids',
COUNT(*)
FROM chat_rooms
WHERE participant_ids IS NULL OR participant_ids = ARRAY[]::TEXT[]
UNION ALL
SELECT
'Users with NULL username',
COUNT(*)
FROM users
WHERE username IS NULL OR username = '';

-- Check foreign key constraints exist
SELECT
conname as constraint_name,
conrelid::regclass as table_name,
confrelid::regclass as foreign_table
FROM pg_constraint
WHERE contype = 'f'
AND conrelid::regclass::text IN ('messages', 'chat_rooms', 'tasks', 'projects', 'project_members')
ORDER BY table_name, constraint_name;

-- ============================================
-- SUCCESS CRITERIA
-- ============================================
-- After running this script, you should see:
-- ✅ messages table has 15 columns (all required fields present)
-- ✅ chat_rooms table has 14 columns including participant_ids
-- ✅ All critical tables match expected column counts
-- ✅ All columns verification queries show ✅ EXISTS
-- ✅ All indexes created successfully
-- ✅ All foreign keys created successfully
-- ✅ PostgREST schema cache reloaded
-- ✅ No NULL or empty critical fields
-- ✅ RLS disabled for all tables (testing mode)

-- ============================================
-- NEXT STEPS AFTER RUNNING THIS SCRIPT
-- ============================================
-- 1. Verify all verification queries show ✅ status
-- 2. Fix SupabaseConfig.kt to enable WebSocket support (add OkHttp engine)
-- 3. Rebuild Android app: ./gradlew clean assembleDebug
-- 4. Test message sync - should see "✅ synced to Supabase" in logcat
-- 5. Test chat room sync - should work without errors
-- 6. Test task sync - should work without errors
-- 7. Update DEVELOPMENT_LOGBOOK.md with results

-- ============================================
-- ROLLBACK PLAN (if something goes wrong)
-- ============================================
-- To remove added columns (DANGEROUS - will lose data):
/*
ALTER TABLE messages
DROP COLUMN IF EXISTS sender_name,
DROP COLUMN IF EXISTS sender_photo_url,
DROP COLUMN IF EXISTS voice_message_id,
DROP COLUMN IF EXISTS task_ids,
DROP COLUMN IF EXISTS reply_to_message_id,
DROP COLUMN IF EXISTS is_edited,
DROP COLUMN IF EXISTS edited_at,
DROP COLUMN IF EXISTS reactions,
DROP COLUMN IF EXISTS read_by;

ALTER TABLE chat_rooms
DROP COLUMN IF EXISTS participant_ids;

NOTIFY pgrst, 'reload schema';
*/

-- ============================================
-- CHANGES FROM V1
-- ============================================
-- V1 Bug: Tried to create foreign key for reply_to_message_id before creating the column
-- V2 Fix:
-- 1. Adds ALL missing columns from Message.kt model (9 columns total)
-- 2. Creates columns BEFORE creating foreign keys
-- 3. Uses DO blocks for safe constraint dropping
-- 4. More comprehensive verification queries
-- 5. Better documentation and comments

-- ============================================
-- END OF SCHEMA FIX SCRIPT V2
-- ============================================
