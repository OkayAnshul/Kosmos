-- ============================================
-- Supabase Fix: Username & RLS Issues
-- ============================================
-- Project: Kosmos Android App
-- Date: 2025-10-31
-- Purpose: Fix search JSON errors and enable Supabase sync
-- ============================================

-- ============================================
-- FIX 1: Update NULL usernames
-- ============================================
-- This fixes the JSON deserialization error in search
-- Generates username from display_name by:
-- 1. Replacing non-alphanumeric chars with underscore
-- 2. Converting to lowercase
-- 3. Removing duplicate underscores

UPDATE users
SET username = LOWER(
    REPLACE(
        REGEXP_REPLACE(display_name, '[^a-zA-Z0-9]', '_', 'g'),
        '__',
        '_'
    )
)
WHERE username IS NULL OR username = '';

-- Verify the update
SELECT id, email, display_name, username
FROM users
WHERE username IS NOT NULL
LIMIT 10;

-- ============================================
-- FIX 2: Disable Row Level Security (RLS)
-- ============================================
-- IMPORTANT: RLS is blocking INSERT/UPDATE operations
-- This is why messages, tasks, and chat rooms aren't syncing!
--
-- Option A: DISABLE RLS (for development/testing)
-- Option B: CREATE proper RLS policies (for production)
-- ============================================

-- Option A: Disable RLS (Quick Fix for Testing)
-- Run these to allow all authenticated users to read/write
-- NOTE: Only disable RLS for tables that exist in your database
ALTER TABLE users DISABLE ROW LEVEL SECURITY;
ALTER TABLE messages DISABLE ROW LEVEL SECURITY;
ALTER TABLE chat_rooms DISABLE ROW LEVEL SECURITY;
ALTER TABLE tasks DISABLE ROW LEVEL SECURITY;
ALTER TABLE projects DISABLE ROW LEVEL SECURITY;
ALTER TABLE project_members DISABLE ROW LEVEL SECURITY;

-- Optional: Only uncomment these if the tables exist in your database
-- ALTER TABLE voice_messages DISABLE ROW LEVEL SECURITY;
-- ALTER TABLE action_items DISABLE ROW LEVEL SECURITY;

-- Verify RLS is disabled
SELECT tablename, rowsecurity
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'projects', 'project_members')
ORDER BY tablename;

-- ============================================
-- Option B: Enable RLS with Proper Policies
-- ============================================
-- Uncomment these lines if you want to enable RLS with policies
-- (Recommended for production after testing)

/*
-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE voice_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE action_items ENABLE ROW LEVEL SECURITY;

-- DROP existing policies (if any)
DROP POLICY IF EXISTS "Users can view all users" ON users;
DROP POLICY IF EXISTS "Users can update own profile" ON users;
DROP POLICY IF EXISTS "Users can insert messages" ON messages;
DROP POLICY IF EXISTS "Users can view messages in their chat rooms" ON messages;
DROP POLICY IF EXISTS "Users can update own messages" ON messages;
DROP POLICY IF EXISTS "Users can delete own messages" ON messages;

-- USERS table policies
CREATE POLICY "Users can view all users"
ON users FOR SELECT
TO authenticated
USING (true);

CREATE POLICY "Users can insert own profile"
ON users FOR INSERT
TO authenticated
WITH CHECK (auth.uid()::text = id);

CREATE POLICY "Users can update own profile"
ON users FOR UPDATE
TO authenticated
USING (auth.uid()::text = id)
WITH CHECK (auth.uid()::text = id);

-- MESSAGES table policies
CREATE POLICY "Users can view messages in their chat rooms"
ON messages FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM chat_rooms
        WHERE chat_rooms.id = messages.chat_room_id
        AND auth.uid()::text = ANY(chat_rooms.participant_ids)
    )
);

CREATE POLICY "Users can insert messages"
ON messages FOR INSERT
TO authenticated
WITH CHECK (auth.uid()::text = sender_id);

CREATE POLICY "Users can update own messages"
ON messages FOR UPDATE
TO authenticated
USING (auth.uid()::text = sender_id)
WITH CHECK (auth.uid()::text = sender_id);

CREATE POLICY "Users can delete own messages"
ON messages FOR DELETE
TO authenticated
USING (auth.uid()::text = sender_id);

-- CHAT_ROOMS table policies
CREATE POLICY "Users can view their chat rooms"
ON chat_rooms FOR SELECT
TO authenticated
USING (auth.uid()::text = ANY(participant_ids));

CREATE POLICY "Users can insert chat rooms"
ON chat_rooms FOR INSERT
TO authenticated
WITH CHECK (auth.uid()::text = created_by);

CREATE POLICY "Users can update their chat rooms"
ON chat_rooms FOR UPDATE
TO authenticated
USING (auth.uid()::text = ANY(participant_ids))
WITH CHECK (auth.uid()::text = ANY(participant_ids));

-- TASKS table policies
CREATE POLICY "Users can view tasks in their projects"
ON tasks FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM project_members
        WHERE project_members.project_id = tasks.project_id
        AND project_members.user_id = auth.uid()::text
        AND project_members.is_active = true
    )
);

CREATE POLICY "Users can insert tasks in their projects"
ON tasks FOR INSERT
TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1 FROM project_members
        WHERE project_members.project_id = tasks.project_id
        AND project_members.user_id = auth.uid()::text
        AND project_members.is_active = true
    )
);

CREATE POLICY "Users can update tasks in their projects"
ON tasks FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM project_members
        WHERE project_members.project_id = tasks.project_id
        AND project_members.user_id = auth.uid()::text
        AND project_members.is_active = true
    )
);

-- PROJECTS table policies
CREATE POLICY "Users can view their projects"
ON projects FOR SELECT
TO authenticated
USING (
    auth.uid()::text = owner_id OR
    EXISTS (
        SELECT 1 FROM project_members
        WHERE project_members.project_id = projects.id
        AND project_members.user_id = auth.uid()::text
        AND project_members.is_active = true
    )
);

CREATE POLICY "Users can insert projects"
ON projects FOR INSERT
TO authenticated
WITH CHECK (auth.uid()::text = owner_id);

CREATE POLICY "Users can update own projects"
ON projects FOR UPDATE
TO authenticated
USING (auth.uid()::text = owner_id)
WITH CHECK (auth.uid()::text = owner_id);

-- PROJECT_MEMBERS table policies
CREATE POLICY "Users can view members in their projects"
ON project_members FOR SELECT
TO authenticated
USING (
    user_id = auth.uid()::text OR
    EXISTS (
        SELECT 1 FROM project_members pm
        WHERE pm.project_id = project_members.project_id
        AND pm.user_id = auth.uid()::text
        AND pm.is_active = true
    )
);

CREATE POLICY "Project owners can insert members"
ON project_members FOR INSERT
TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1 FROM projects
        WHERE projects.id = project_members.project_id
        AND projects.owner_id = auth.uid()::text
    )
);
*/

-- ============================================
-- Verification Queries
-- ============================================

-- Check if username update worked
SELECT
    id,
    email,
    username,
    display_name,
    CASE
        WHEN username IS NULL THEN '❌ NULL'
        WHEN username = '' THEN '❌ EMPTY'
        ELSE '✅ OK'
    END as status
FROM users
ORDER BY created_at DESC
LIMIT 20;

-- Check RLS status for all tables
SELECT
    tablename,
    CASE
        WHEN rowsecurity = true THEN '🔒 ENABLED'
        ELSE '🔓 DISABLED'
    END as rls_status
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN (
    'users', 'messages', 'chat_rooms', 'tasks',
    'projects', 'project_members', 'voice_messages', 'action_items'
)
ORDER BY tablename;

-- ============================================
-- Testing Insert Operations
-- ============================================
-- After running the fixes above, test if inserts work
-- by trying these queries (replace with real values):

/*
-- Test message insert
INSERT INTO messages (
    id, chat_room_id, sender_id, sender_name,
    content, timestamp, type
) VALUES (
    'test-msg-' || gen_random_uuid()::text,
    'your-chat-room-id',
    'your-user-id',
    'Test User',
    'Test message from SQL',
    EXTRACT(EPOCH FROM NOW())::bigint * 1000,
    'TEXT'
);

-- Verify message was inserted
SELECT id, sender_name, content, timestamp
FROM messages
ORDER BY timestamp DESC
LIMIT 5;
*/

-- ============================================
-- IMPORTANT NOTES
-- ============================================
-- 1. RUN FIX 1 (username update) first
-- 2. RUN FIX 2 Option A (disable RLS) for immediate testing
-- 3. After confirming sync works, consider Option B (proper RLS policies)
-- 4. NEVER disable RLS in production without proper policies!
-- ============================================

-- Expected Results:
-- ✅ All users have valid usernames
-- ✅ Search functionality works without JSON errors
-- ✅ Messages sync to Supabase when sent
-- ✅ Tasks sync to Supabase when created
-- ✅ Chat rooms sync to Supabase when created
