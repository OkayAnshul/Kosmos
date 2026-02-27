-- ============================================
-- COMPLETE SCHEMA FIX: All Tables Migration
-- ============================================
-- Project: Kosmos Android App
-- Date: 2025-10-31
-- Purpose: Fix ALL schema mismatches between Kotlin models and Supabase database
-- Based on: SCHEMA_ANALYSIS_COMPLETE.md
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

-- Check messages table structure
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'messages'
ORDER BY ordinal_position;

-- Check chat_rooms table structure
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'chat_rooms'
ORDER BY ordinal_position;

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
-- STEP 2: FIX MESSAGES TABLE (CRITICAL)
-- ============================================
-- Error: "Could not find the 'sender_name' column of 'messages'"
-- Root Cause: Missing sender_name and sender_photo_url columns

-- Add missing columns to messages table
ALTER TABLE messages
ADD COLUMN IF NOT EXISTS sender_name TEXT NOT NULL DEFAULT '',
ADD COLUMN IF NOT EXISTS sender_photo_url TEXT;

-- Update existing messages to populate sender_name from users table
-- This ensures no NULL or empty values
UPDATE messages m
SET sender_name = COALESCE(u.display_name, u.username, 'Unknown User')
FROM users u
WHERE m.sender_id = u.id
AND (m.sender_name IS NULL OR m.sender_name = '');

-- Update sender_photo_url from users table
UPDATE messages m
SET sender_photo_url = u.photo_url
FROM users u
WHERE m.sender_id = u.id
AND m.sender_photo_url IS NULL;

-- Remove default constraint after populating data
ALTER TABLE messages
ALTER COLUMN sender_name DROP DEFAULT;

-- Verify messages table fix
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'messages'
AND column_name IN ('sender_name', 'sender_photo_url')
ORDER BY column_name;

-- ============================================
-- STEP 3: FIX CHAT_ROOMS TABLE (CRITICAL)
-- ============================================
-- Error: "Could not find the 'participant_ids' column of 'chat_rooms'"
-- Root Cause: Missing participant_ids array column

-- Add missing participant_ids array column
ALTER TABLE chat_rooms
ADD COLUMN IF NOT EXISTS participant_ids TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[];

-- Populate participant_ids for existing chat rooms
-- Strategy: Add created_by as first participant
UPDATE chat_rooms
SET participant_ids = ARRAY[created_by]::TEXT[]
WHERE participant_ids = ARRAY[]::TEXT[]
AND created_by IS NOT NULL;

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

-- If any columns are missing, add them here:
-- ALTER TABLE tasks ADD COLUMN IF NOT EXISTS [column_name] [data_type];

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

-- Messages table foreign keys
ALTER TABLE messages
DROP CONSTRAINT IF EXISTS fk_messages_chat_room,
DROP CONSTRAINT IF EXISTS fk_messages_sender,
DROP CONSTRAINT IF EXISTS fk_messages_reply_to,
ADD CONSTRAINT fk_messages_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_messages_reply_to FOREIGN KEY (reply_to_message_id) REFERENCES messages(id) ON DELETE SET NULL;

-- Chat rooms table foreign keys
ALTER TABLE chat_rooms
DROP CONSTRAINT IF EXISTS fk_chat_rooms_project,
DROP CONSTRAINT IF EXISTS fk_chat_rooms_created_by,
ADD CONSTRAINT fk_chat_rooms_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_chat_rooms_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE;

-- Tasks table foreign keys
ALTER TABLE tasks
DROP CONSTRAINT IF EXISTS fk_tasks_project,
DROP CONSTRAINT IF EXISTS fk_tasks_chat_room,
DROP CONSTRAINT IF EXISTS fk_tasks_assigned_to,
DROP CONSTRAINT IF EXISTS fk_tasks_created_by,
DROP CONSTRAINT IF EXISTS fk_tasks_parent,
ADD CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_tasks_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id) ON DELETE SET NULL,
ADD CONSTRAINT fk_tasks_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users(id) ON DELETE SET NULL,
ADD CONSTRAINT fk_tasks_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_tasks_parent FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE SET NULL;

-- Projects table foreign keys
ALTER TABLE projects
DROP CONSTRAINT IF EXISTS fk_projects_owner,
ADD CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;

-- Project members table foreign keys
ALTER TABLE project_members
DROP CONSTRAINT IF EXISTS fk_project_members_project,
DROP CONSTRAINT IF EXISTS fk_project_members_user,
DROP CONSTRAINT IF EXISTS fk_project_members_invited_by,
ADD CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_project_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_project_members_invited_by FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL;

-- ============================================
-- STEP 10: ADD UNIQUE CONSTRAINTS
-- ============================================

-- Ensure unique username
ALTER TABLE users
DROP CONSTRAINT IF EXISTS unique_username,
ADD CONSTRAINT unique_username UNIQUE (username);

-- Ensure unique project membership
ALTER TABLE project_members
DROP CONSTRAINT IF EXISTS unique_project_member,
ADD CONSTRAINT unique_project_member UNIQUE (project_id, user_id);

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

-- Verify critical columns exist
SELECT
    'messages.sender_name' as column_path,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'messages' AND column_name = 'sender_name'
    ) THEN '✅ EXISTS' ELSE '❌ MISSING' END as status
UNION ALL
SELECT
    'messages.sender_photo_url',
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'messages' AND column_name = 'sender_photo_url'
    ) THEN '✅ EXISTS' ELSE '❌ MISSING' END
UNION ALL
SELECT
    'chat_rooms.participant_ids',
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
WHERE participant_ids = ARRAY[]::TEXT[]
UNION ALL
SELECT
    'Users with NULL username',
    COUNT(*)
FROM users
WHERE username IS NULL OR username = '';

-- ============================================
-- SUCCESS CRITERIA
-- ============================================
-- After running this script, you should see:
-- ✅ messages table has 15 columns including sender_name and sender_photo_url
-- ✅ chat_rooms table has 14 columns including participant_ids
-- ✅ All critical tables match expected column counts
-- ✅ All indexes created successfully
-- ✅ All foreign keys created successfully
-- ✅ PostgREST schema cache reloaded
-- ✅ No NULL or empty critical fields
-- ✅ RLS disabled for all tables (testing mode)

-- ============================================
-- NEXT STEPS AFTER RUNNING THIS SCRIPT
-- ============================================
-- 1. Verify all verification queries show ✅ status
-- 2. Fix SupabaseConfig.kt to enable WebSocket support
-- 3. Rebuild Android app: ./gradlew clean assembleDebug
-- 4. Test message sync - should see "✅ synced to Supabase" in logcat
-- 5. Test chat room sync - should work without errors
-- 6. Test task sync - should work without errors
-- 7. Update DEVELOPMENT_LOGBOOK.md with results

-- ============================================
-- ROLLBACK PLAN (if something goes wrong)
-- ============================================
-- To remove added columns (DANGEROUS - will lose data):
-- ALTER TABLE messages DROP COLUMN IF EXISTS sender_name;
-- ALTER TABLE messages DROP COLUMN IF EXISTS sender_photo_url;
-- ALTER TABLE chat_rooms DROP COLUMN IF EXISTS participant_ids;

-- ============================================
-- END OF SCHEMA FIX SCRIPT
-- ============================================
