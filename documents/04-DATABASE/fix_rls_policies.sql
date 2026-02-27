-- ============================================================================
-- Kosmos: Fix RLS Policies Column Names
-- ============================================================================
-- Purpose: Fix RLS policies that use incorrect column names
-- Issue: Policies reference 'created_by' instead of 'created_by_id'
--        Policies reference 'assigned_to_user_id' instead of 'assigned_to_id'
-- Impact: Access denied errors, tasks may be inaccessible
--
-- Run: Execute in Supabase SQL Editor
-- Date: 2026-01-23
-- ============================================================================

-- ============================================================================
-- TASKS TABLE: Fix RLS Policies
-- ============================================================================

-- Drop existing incorrect policies
DROP POLICY IF EXISTS "Users can view their own tasks" ON tasks;
DROP POLICY IF EXISTS "Users can view assigned tasks" ON tasks;
DROP POLICY IF EXISTS "Users can view project tasks" ON tasks;
DROP POLICY IF EXISTS "Users can insert tasks in their projects" ON tasks;
DROP POLICY IF EXISTS "Users can update their own tasks" ON tasks;
DROP POLICY IF EXISTS "Users can update assigned tasks" ON tasks;
DROP POLICY IF EXISTS "Users can delete their own tasks" ON tasks;

-- ============================================================================
-- Create CORRECT policies with proper column names
-- ============================================================================

-- Policy 1: Users can view tasks they created
CREATE POLICY "Users can view their own tasks"
ON tasks FOR SELECT
USING (created_by_id = auth.uid());  -- ✅ CORRECT: created_by_id (not created_by)

-- Policy 2: Users can view tasks assigned to them
CREATE POLICY "Users can view assigned tasks"
ON tasks FOR SELECT
USING (assigned_to_id = auth.uid());  -- ✅ CORRECT: assigned_to_id (not assigned_to_user_id)

-- Policy 3: Users can view tasks in projects they're members of
CREATE POLICY "Users can view project tasks"
ON tasks FOR SELECT
USING (
    project_id IN (
        SELECT project_id
        FROM project_members
        WHERE user_id = auth.uid()
    )
);

-- Policy 4: Users can insert tasks in projects where they have permission
CREATE POLICY "Users can insert tasks in their projects"
ON tasks FOR INSERT
WITH CHECK (
    -- Must be a member of the project
    project_id IN (
        SELECT project_id
        FROM project_members
        WHERE user_id = auth.uid()
          AND (
              -- Must have task creation permission
              permissions ? 'task_create'
              OR role IN ('owner', 'admin')  -- Owners/admins can always create
          )
    )
);

-- Policy 5: Task creators can update their own tasks
CREATE POLICY "Task creators can update their own tasks"
ON tasks FOR UPDATE
USING (created_by_id = auth.uid())  -- ✅ CORRECT: created_by_id
WITH CHECK (created_by_id = auth.uid());

-- Policy 6: Assigned users can update task status/progress
CREATE POLICY "Assigned users can update task status"
ON tasks FOR UPDATE
USING (
    assigned_to_id = auth.uid()  -- ✅ CORRECT: assigned_to_id
    AND project_id IN (
        SELECT project_id
        FROM project_members
        WHERE user_id = auth.uid()
          AND (
              permissions ? 'task_edit'
              OR role IN ('owner', 'admin', 'member')
          )
    )
)
WITH CHECK (
    assigned_to_id = auth.uid()
    AND project_id IN (
        SELECT project_id
        FROM project_members
        WHERE user_id = auth.uid()
    )
);

-- Policy 7: Project members with permissions can update any task
CREATE POLICY "Project members can update tasks"
ON tasks FOR UPDATE
USING (
    project_id IN (
        SELECT project_id
        FROM project_members
        WHERE user_id = auth.uid()
          AND (
              permissions ? 'task_edit'
              OR role IN ('owner', 'admin')
          )
    )
)
WITH CHECK (
    project_id IN (
        SELECT project_id
        FROM project_members
        WHERE user_id = auth.uid()
          AND (
              permissions ? 'task_edit'
              OR role IN ('owner', 'admin')
          )
    )
);

-- Policy 8: Task creators can delete their own tasks
CREATE POLICY "Task creators can delete their own tasks"
ON tasks FOR DELETE
USING (
    created_by_id = auth.uid()  -- ✅ CORRECT: created_by_id
    OR project_id IN (
        SELECT project_id
        FROM project_members
        WHERE user_id = auth.uid()
          AND (
              permissions ? 'task_delete'
              OR role IN ('owner', 'admin')
          )
    )
);

-- ============================================================================
-- PROJECTS TABLE: Verify and fix if needed
-- ============================================================================

-- Drop existing policies if they have incorrect column names
DROP POLICY IF EXISTS "Users can view their own projects" ON projects;
DROP POLICY IF EXISTS "Users can view projects they're members of" ON projects;

-- Create correct policies
CREATE POLICY "Users can view their own projects"
ON projects FOR SELECT
USING (owner_id = auth.uid());  -- ✅ CORRECT: owner_id

CREATE POLICY "Users can view projects they're members of"
ON projects FOR SELECT
USING (
    id IN (
        SELECT project_id
        FROM project_members
        WHERE user_id = auth.uid()
    )
);

-- ============================================================================
-- MESSAGES TABLE: Verify and fix if needed
-- ============================================================================

-- Drop existing policies if they have incorrect column names
DROP POLICY IF EXISTS "Users can view messages in their chats" ON messages;
DROP POLICY IF EXISTS "Users can insert messages in their chats" ON messages;

-- Create correct policies
CREATE POLICY "Users can view messages in their chats"
ON messages FOR SELECT
USING (
    chat_room_id IN (
        SELECT id FROM chat_rooms
        WHERE type = 'dm' AND (
            (metadata->>'user1_id')::UUID = auth.uid()
            OR (metadata->>'user2_id')::UUID = auth.uid()
        )
        OR type = 'project' AND project_id IN (
            SELECT project_id FROM project_members WHERE user_id = auth.uid()
        )
    )
);

CREATE POLICY "Users can insert messages in their chats"
ON messages FOR INSERT
WITH CHECK (
    user_id = auth.uid()  -- ✅ CORRECT: user_id
    AND chat_room_id IN (
        SELECT id FROM chat_rooms
        WHERE type = 'dm' AND (
            (metadata->>'user1_id')::UUID = auth.uid()
            OR (metadata->>'user2_id')::UUID = auth.uid()
        )
        OR type = 'project' AND project_id IN (
            SELECT project_id FROM project_members WHERE user_id = auth.uid()
        )
    )
);

-- ============================================================================
-- Verification Queries (Run after applying fixes)
-- ============================================================================

-- 1. Verify tasks policies
-- SELECT policyname, cmd, qual
-- FROM pg_policies
-- WHERE tablename = 'tasks'
-- ORDER BY policyname;

-- 2. Verify projects policies
-- SELECT policyname, cmd, qual
-- FROM pg_policies
-- WHERE tablename = 'projects'
-- ORDER BY policyname;

-- 3. Verify messages policies
-- SELECT policyname, cmd, qual
-- FROM pg_policies
-- WHERE tablename = 'messages'
-- ORDER BY policyname;

-- 4. Test task access (replace UUID with actual user ID)
-- SET LOCAL my.user_id = '00000000-0000-0000-0000-000000000001'::UUID;
-- SELECT * FROM tasks WHERE created_by_id = current_setting('my.user_id')::UUID;

-- ============================================================================
-- Test Scenarios (Run after verification)
-- ============================================================================

-- Test 1: User can view their own tasks
-- SELECT COUNT(*) FROM tasks WHERE created_by_id = auth.uid();

-- Test 2: User can view assigned tasks
-- SELECT COUNT(*) FROM tasks WHERE assigned_to_id = auth.uid();

-- Test 3: User can view project tasks
-- SELECT COUNT(*) FROM tasks
-- WHERE project_id IN (
--     SELECT project_id FROM project_members WHERE user_id = auth.uid()
-- );

-- Test 4: User can create task in their project
-- INSERT INTO tasks (title, project_id, created_by_id)
-- VALUES ('Test Task', 'valid-project-id', auth.uid());

-- ============================================================================
-- Rollback (if needed)
-- ============================================================================

-- To rollback, drop all policies and recreate with old (incorrect) names
-- WARNING: This will restore the bug! Only use for emergency rollback.

-- DROP POLICY IF EXISTS "Users can view their own tasks" ON tasks;
-- DROP POLICY IF EXISTS "Users can view assigned tasks" ON tasks;
-- ... etc

-- ============================================================================
-- Migration Notes
-- ============================================================================
--
-- After running this script:
-- 1. Run verification queries to confirm policies are correct
-- 2. Test task CRUD operations from Android app
-- 3. Verify no "permission denied" errors
-- 4. Monitor Supabase logs for RLS-related errors
-- 5. Test multi-user scenarios (task assignment, project access)
--
-- Common Issues After Fix:
-- - If users still can't access tasks, verify project_members table
-- - Check that permissions JSONB column has correct structure
-- - Verify auth.uid() returns correct user ID
--
-- Related Files:
-- - documents/04-DATABASE/SCHEMA_FIX_COMPLETE_V2.sql (original schema)
-- - app/src/main/java/com/example/kosmos/data/datasource/SupabaseTaskDataSource.kt
--
-- ============================================================================
