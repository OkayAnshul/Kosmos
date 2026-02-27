-- ============================================
-- RLS ENABLE PRODUCTION - Kosmos Android App
-- ============================================
-- Date: 2026-01-13
-- Purpose: Enable Row Level Security on all tables
-- Security Level: CRITICAL
-- ============================================
-- ⚠️ WARNING: This script will enable RLS on all tables.
-- After running this, only users with proper permissions can access data.
-- Make sure you have backups before running!
-- ============================================

-- ============================================
-- STEP 0: VERIFY CURRENT STATE
-- ============================================

-- Check current RLS status
SELECT
  tablename,
  CASE
    WHEN rowsecurity = true THEN '🔒 ENABLED'
    ELSE '🔓 DISABLED'
  END as current_status
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'projects', 'project_members')
ORDER BY tablename;

-- ============================================
-- STEP 1: ENABLE RLS ON USERS TABLE
-- ============================================

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any
DROP POLICY IF EXISTS "users_select_own" ON users;
DROP POLICY IF EXISTS "users_select_project_members" ON users;
DROP POLICY IF EXISTS "users_update_own" ON users;
DROP POLICY IF EXISTS "users_insert_own" ON users;

-- Policy: Users can view their own profile
CREATE POLICY "users_select_own" ON users
  FOR SELECT
  USING (auth.uid() = id);

-- Policy: Users can view profiles of users in same projects
CREATE POLICY "users_select_project_members" ON users
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM project_members pm1
      JOIN project_members pm2 ON pm1.project_id = pm2.project_id
      WHERE pm1.user_id = auth.uid()
        AND pm2.user_id = users.id
    )
  );

-- Policy: Users can update their own profile
CREATE POLICY "users_update_own" ON users
  FOR UPDATE
  USING (auth.uid() = id)
  WITH CHECK (auth.uid() = id);

-- Policy: New users can insert their own profile (signup)
CREATE POLICY "users_insert_own" ON users
  FOR INSERT
  WITH CHECK (auth.uid() = id);

-- ============================================
-- STEP 2: ENABLE RLS ON PROJECTS TABLE
-- ============================================

ALTER TABLE projects ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any
DROP POLICY IF EXISTS "projects_select_member" ON projects;
DROP POLICY IF EXISTS "projects_insert_owner" ON projects;
DROP POLICY IF EXISTS "projects_update_admin" ON projects;
DROP POLICY IF EXISTS "projects_delete_owner" ON projects;

-- Policy: Users can view projects they are members of
CREATE POLICY "projects_select_member" ON projects
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = projects.id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Users can create new projects
CREATE POLICY "projects_insert_owner" ON projects
  FOR INSERT
  WITH CHECK (auth.uid() = owner_id);

-- Policy: Owners and Admins can update projects
CREATE POLICY "projects_update_admin" ON projects
  FOR UPDATE
  USING (
    owner_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = projects.id
        AND project_members.user_id = auth.uid()
        AND project_members.role = 'ADMIN'
    )
  )
  WITH CHECK (
    owner_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = projects.id
        AND project_members.user_id = auth.uid()
        AND project_members.role = 'ADMIN'
    )
  );

-- Policy: Only owners can delete projects
CREATE POLICY "projects_delete_owner" ON projects
  FOR DELETE
  USING (owner_id = auth.uid());

-- ============================================
-- STEP 3: ENABLE RLS ON PROJECT_MEMBERS TABLE
-- ============================================

ALTER TABLE project_members ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any
DROP POLICY IF EXISTS "project_members_select" ON project_members;
DROP POLICY IF EXISTS "project_members_insert" ON project_members;
DROP POLICY IF EXISTS "project_members_delete" ON project_members;
DROP POLICY IF EXISTS "project_members_update" ON project_members;

-- Policy: Users can view members of their projects
CREATE POLICY "project_members_select" ON project_members
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM project_members pm
      WHERE pm.project_id = project_members.project_id
        AND pm.user_id = auth.uid()
    )
  );

-- Policy: Admins and owners can add members
CREATE POLICY "project_members_insert" ON project_members
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM projects
      WHERE projects.id = project_members.project_id
        AND (
          projects.owner_id = auth.uid()
          OR EXISTS (
            SELECT 1 FROM project_members pm
            WHERE pm.project_id = project_members.project_id
              AND pm.user_id = auth.uid()
              AND pm.role = 'ADMIN'
          )
        )
    )
  );

-- Policy: Admins and owners can remove members
CREATE POLICY "project_members_delete" ON project_members
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM projects
      WHERE projects.id = project_members.project_id
        AND (
          projects.owner_id = auth.uid()
          OR EXISTS (
            SELECT 1 FROM project_members pm
            WHERE pm.project_id = project_members.project_id
              AND pm.user_id = auth.uid()
              AND pm.role = 'ADMIN'
          )
        )
    )
  );

-- Policy: Admins can update member roles
CREATE POLICY "project_members_update" ON project_members
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM projects
      WHERE projects.id = project_members.project_id
        AND (
          projects.owner_id = auth.uid()
          OR EXISTS (
            SELECT 1 FROM project_members pm
            WHERE pm.project_id = project_members.project_id
              AND pm.user_id = auth.uid()
              AND pm.role = 'ADMIN'
          )
        )
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM projects
      WHERE projects.id = project_members.project_id
        AND (
          projects.owner_id = auth.uid()
          OR EXISTS (
            SELECT 1 FROM project_members pm
            WHERE pm.project_id = project_members.project_id
              AND pm.user_id = auth.uid()
              AND pm.role = 'ADMIN'
          )
        )
    )
  );

-- ============================================
-- STEP 4: ENABLE RLS ON CHAT_ROOMS TABLE
-- ============================================

ALTER TABLE chat_rooms ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any
DROP POLICY IF EXISTS "chat_rooms_select" ON chat_rooms;
DROP POLICY IF EXISTS "chat_rooms_insert" ON chat_rooms;
DROP POLICY IF EXISTS "chat_rooms_update" ON chat_rooms;
DROP POLICY IF EXISTS "chat_rooms_delete" ON chat_rooms;

-- Policy: Users can view chat rooms they participate in
CREATE POLICY "chat_rooms_select" ON chat_rooms
  FOR SELECT
  USING (
    auth.uid() = ANY(participant_ids)
    OR EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = chat_rooms.project_id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Project members can create chat rooms
CREATE POLICY "chat_rooms_insert" ON chat_rooms
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = chat_rooms.project_id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Project admins can update chat rooms
CREATE POLICY "chat_rooms_update" ON chat_rooms
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = chat_rooms.project_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = chat_rooms.project_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  );

-- Policy: Project admins can delete chat rooms
CREATE POLICY "chat_rooms_delete" ON chat_rooms
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = chat_rooms.project_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  );

-- ============================================
-- STEP 5: ENABLE RLS ON MESSAGES TABLE
-- ============================================

ALTER TABLE messages ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any
DROP POLICY IF EXISTS "messages_select" ON messages;
DROP POLICY IF EXISTS "messages_insert" ON messages;
DROP POLICY IF EXISTS "messages_update" ON messages;
DROP POLICY IF EXISTS "messages_delete" ON messages;

-- Policy: Users can view messages in their chat rooms
CREATE POLICY "messages_select" ON messages
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM chat_rooms
      WHERE chat_rooms.id = messages.chat_room_id
        AND (
          auth.uid() = ANY(chat_rooms.participant_ids)
          OR EXISTS (
            SELECT 1 FROM project_members
            WHERE project_members.project_id = chat_rooms.project_id
              AND project_members.user_id = auth.uid()
          )
        )
    )
  );

-- Policy: Users can send messages to chat rooms they participate in
CREATE POLICY "messages_insert" ON messages
  FOR INSERT
  WITH CHECK (
    sender_id = auth.uid()
    AND EXISTS (
      SELECT 1 FROM chat_rooms
      WHERE chat_rooms.id = messages.chat_room_id
        AND (
          auth.uid() = ANY(chat_rooms.participant_ids)
          OR EXISTS (
            SELECT 1 FROM project_members
            WHERE project_members.project_id = chat_rooms.project_id
              AND project_members.user_id = auth.uid()
          )
        )
    )
  );

-- Policy: Users can update their own messages (edit)
CREATE POLICY "messages_update" ON messages
  FOR UPDATE
  USING (sender_id = auth.uid())
  WITH CHECK (sender_id = auth.uid());

-- Policy: Users can delete their own messages
CREATE POLICY "messages_delete" ON messages
  FOR DELETE
  USING (sender_id = auth.uid());

-- ============================================
-- STEP 6: ENABLE RLS ON TASKS TABLE
-- ============================================

ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any
DROP POLICY IF EXISTS "tasks_select" ON tasks;
DROP POLICY IF EXISTS "tasks_insert" ON tasks;
DROP POLICY IF EXISTS "tasks_update" ON tasks;
DROP POLICY IF EXISTS "tasks_delete" ON tasks;

-- Policy: Users can view tasks in their projects
CREATE POLICY "tasks_select" ON tasks
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM chat_rooms
      JOIN project_members ON chat_rooms.project_id = project_members.project_id
      WHERE chat_rooms.id = tasks.chat_room_id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Project members can create tasks
CREATE POLICY "tasks_insert" ON tasks
  FOR INSERT
  WITH CHECK (
    created_by = auth.uid()
    AND EXISTS (
      SELECT 1 FROM chat_rooms
      JOIN project_members ON chat_rooms.project_id = project_members.project_id
      WHERE chat_rooms.id = tasks.chat_room_id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Assignees and admins can update tasks
CREATE POLICY "tasks_update" ON tasks
  FOR UPDATE
  USING (
    assigned_to_user_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM chat_rooms
      JOIN project_members ON chat_rooms.project_id = project_members.project_id
      WHERE chat_rooms.id = tasks.chat_room_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  )
  WITH CHECK (
    assigned_to_user_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM chat_rooms
      JOIN project_members ON chat_rooms.project_id = project_members.project_id
      WHERE chat_rooms.id = tasks.chat_room_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  );

-- Policy: Only admins can delete tasks
CREATE POLICY "tasks_delete" ON tasks
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM chat_rooms
      JOIN project_members ON chat_rooms.project_id = project_members.project_id
      WHERE chat_rooms.id = tasks.chat_room_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  );

-- ============================================
-- STEP 7: VERIFICATION
-- ============================================

-- Verify RLS is enabled on all tables
SELECT
  tablename,
  CASE
    WHEN rowsecurity = true THEN '✅ ENABLED'
    ELSE '❌ STILL DISABLED'
  END as rls_status
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'projects', 'project_members')
ORDER BY tablename;

-- Count policies per table
SELECT
  schemaname,
  tablename,
  COUNT(*) as policy_count
FROM pg_policies
WHERE schemaname = 'public'
  AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'projects', 'project_members')
GROUP BY schemaname, tablename
ORDER BY tablename;

-- Expected policy counts:
-- users: 4 policies
-- projects: 4 policies
-- project_members: 4 policies
-- chat_rooms: 4 policies
-- messages: 4 policies
-- tasks: 4 policies
-- TOTAL: 24 policies

-- List all policies by table
SELECT
  tablename,
  policyname,
  cmd as operation,
  CASE
    WHEN qual IS NOT NULL THEN 'Has USING clause'
    ELSE 'No USING clause'
  END as using_check,
  CASE
    WHEN with_check IS NOT NULL THEN 'Has WITH CHECK clause'
    ELSE 'No WITH CHECK clause'
  END as with_check_status
FROM pg_policies
WHERE schemaname = 'public'
  AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'projects', 'project_members')
ORDER BY tablename, policyname;

-- ============================================
-- SUCCESS CRITERIA
-- ============================================
-- After running this script successfully:
-- ✅ All 6 tables show "✅ ENABLED" for RLS
-- ✅ Total of 24 policies created (4 per table)
-- ✅ Each table has SELECT, INSERT, UPDATE, DELETE policies
-- ✅ No errors in execution
-- ✅ Verification queries return expected counts

-- ============================================
-- NEXT STEPS
-- ============================================
-- 1. Test with authenticated user (should work)
-- 2. Test with unauthenticated request (should fail)
-- 3. Test cross-user access (User A cannot see User B's projects)
-- 4. Update Android app if any queries fail
-- 5. Monitor Supabase logs for policy violations
-- 6. Document any issues in RLS_SECURITY_AUDIT.md
