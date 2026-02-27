-- ============================================
-- RLS ENABLE PRODUCTION V2 - Kosmos Android App
-- ============================================
-- Date: 2026-01-24
-- Version: 2.0 (FIXED)
-- Purpose: Enable Row Level Security on all 7 tables
-- Security Level: CRITICAL
-- Changes from V1:
--   - FIXED: Task table policies (correct column names and join logic)
--   - ADDED: Task activity policies (consolidated from P0-02)
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
  AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'task_activity', 'projects', 'project_members')
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
-- STEP 6: ENABLE RLS ON TASKS TABLE (FIXED)
-- ============================================

ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any
DROP POLICY IF EXISTS "tasks_select" ON tasks;
DROP POLICY IF EXISTS "tasks_insert" ON tasks;
DROP POLICY IF EXISTS "tasks_update" ON tasks;
DROP POLICY IF EXISTS "tasks_delete" ON tasks;

-- Policy: Users can view tasks in their projects
-- FIX: Use direct project_id instead of joining through chat_rooms
CREATE POLICY "tasks_select" ON tasks
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = tasks.project_id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Project members can create tasks
-- FIX: Use correct column name 'created_by_id' instead of 'created_by'
CREATE POLICY "tasks_insert" ON tasks
  FOR INSERT
  WITH CHECK (
    created_by_id = auth.uid()
    AND EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = tasks.project_id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Assignees and admins can update tasks
-- FIX: Use correct column name 'assigned_to_id' instead of 'assigned_to_user_id'
-- FIX: Use direct project_id instead of joining through chat_rooms
CREATE POLICY "tasks_update" ON tasks
  FOR UPDATE
  USING (
    assigned_to_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = tasks.project_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  )
  WITH CHECK (
    assigned_to_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = tasks.project_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  );

-- Policy: Only admins can delete tasks
-- FIX: Use direct project_id instead of joining through chat_rooms
CREATE POLICY "tasks_delete" ON tasks
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = tasks.project_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  );

-- ============================================
-- STEP 7: ENABLE RLS ON TASK_ACTIVITY TABLE
-- ============================================
-- Consolidated from P0-02_CREATE_TASK_ACTIVITY_TABLE.sql

ALTER TABLE task_activity ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any
DROP POLICY IF EXISTS "task_activity_select" ON task_activity;
DROP POLICY IF EXISTS "task_activity_insert" ON task_activity;
DROP POLICY IF EXISTS "task_activity_update" ON task_activity;
DROP POLICY IF EXISTS "task_activity_delete" ON task_activity;

-- Policy: Users can view activity for projects they're members of
CREATE POLICY "task_activity_select" ON task_activity
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = task_activity.project_id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Users can insert activity for their own actions
CREATE POLICY "task_activity_insert" ON task_activity
  FOR INSERT
  WITH CHECK (
    actor_id = auth.uid()
    AND EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = task_activity.project_id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Activity records are immutable (no updates allowed)
CREATE POLICY "task_activity_update" ON task_activity
  FOR UPDATE
  USING (false);  -- Deny all updates to preserve audit trail

-- Policy: Only project admins can delete activity (for moderation)
CREATE POLICY "task_activity_delete" ON task_activity
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = task_activity.project_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  );

-- ============================================
-- STEP 8: VERIFICATION
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
  AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'task_activity', 'projects', 'project_members')
ORDER BY tablename;

-- Count policies per table
SELECT
  tablename,
  COUNT(*) as policy_count
FROM pg_policies
WHERE schemaname = 'public'
  AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'task_activity', 'projects', 'project_members')
GROUP BY tablename
ORDER BY tablename;

-- Expected policy counts:
-- users: 4 policies
-- projects: 4 policies
-- project_members: 4 policies
-- chat_rooms: 4 policies
-- messages: 4 policies
-- tasks: 4 policies (FIXED)
-- task_activity: 4 policies (ADDED)
-- TOTAL: 28 policies (was 24 in V1)

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
  AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'task_activity', 'projects', 'project_members')
ORDER BY tablename, policyname;

-- ============================================
-- STEP 9: CROSS-USER ACCESS TESTS
-- ============================================

-- Test 1: Verify User A can't see User B's projects
-- Run as User A (replace 'user-a-uuid' with actual UUID):
-- Expected: 0 rows (User A not a member)
/*
SELECT COUNT(*) as unauthorized_access_count
FROM projects
WHERE owner_id = 'user-b-uuid';
*/

-- Test 2: Verify User A can see their own tasks
-- Run as User A:
-- Expected: >= 0 rows (their tasks only)
/*
SELECT COUNT(*) as my_tasks_count
FROM tasks t
JOIN project_members pm ON pm.project_id = t.project_id
WHERE pm.user_id = auth.uid();
*/

-- Test 3: Verify User A can see shared project tasks
-- Run as User A in shared project:
-- Expected: All tasks in shared projects
/*
SELECT COUNT(*) as shared_project_tasks_count
FROM tasks t
JOIN project_members pm ON pm.project_id = t.project_id
WHERE pm.user_id = auth.uid();
*/

-- ============================================
-- SUCCESS CRITERIA
-- ============================================
-- After running this script successfully:
-- ✅ All 7 tables show "✅ ENABLED" for RLS
-- ✅ Total of 28 policies created (4 per table)
-- ✅ Each table has SELECT, INSERT, UPDATE, DELETE policies
-- ✅ No errors in execution
-- ✅ Verification queries return expected counts
-- ✅ Cross-user access tests pass (no unauthorized data leaks)

-- ============================================
-- CHANGES FROM V1
-- ============================================
-- 1. FIXED tasks table policies:
--    - Changed 'created_by' to 'created_by_id'
--    - Changed 'assigned_to_user_id' to 'assigned_to_id'
--    - Removed chat_rooms join, use direct project_id
--
-- 2. ADDED task_activity table policies:
--    - 4 policies (SELECT, INSERT, UPDATE, DELETE)
--    - UPDATE policy denies all (immutable audit trail)
--    - Consolidated from P0-02 migration file
--
-- 3. IMPROVED verification:
--    - Added cross-user access tests
--    - Added policy count verification (28 total)
--    - Added detailed test scenarios

-- ============================================
-- NEXT STEPS
-- ============================================
-- 1. Test with authenticated user (should work)
-- 2. Test with unauthenticated request (should fail)
-- 3. Test cross-user access (User A cannot see User B's projects)
-- 4. Update Android app if any queries fail
-- 5. Monitor Supabase logs for policy violations
-- 6. Document test results in RLS_TESTING_RESULTS.md
