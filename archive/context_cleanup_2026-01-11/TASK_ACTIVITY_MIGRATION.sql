-- ============================================================================
-- TASK ACTIVITY TRACKING MIGRATION
-- ============================================================================
-- Purpose: Git-style activity tracking for all task changes
-- Phase: 1 - Core Activity Tracking System
-- Created: 2025-12-31
-- ============================================================================

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create task_activity table
CREATE TABLE IF NOT EXISTS task_activity (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Foreign key relationships
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,

    -- Actor information (snapshot at time of action for audit trail)
    actor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_name TEXT NOT NULL,
    actor_role TEXT,

    -- Action metadata
    action_type TEXT NOT NULL,
    timestamp BIGINT NOT NULL,

    -- Change tracking (JSONB for flexible schema)
    -- Format: [{"field": "status", "fromValue": "TODO", "toValue": "IN_PROGRESS", "displayFrom": "To Do", "displayTo": "In Progress"}]
    changes JSONB,

    -- User-provided commit message (optional)
    commit_message TEXT,

    -- System-generated description (required)
    auto_description TEXT NOT NULL,

    -- Additional context (extensible metadata)
    metadata JSONB DEFAULT '{}'::jsonb,

    -- Constraint: Valid action types
    CONSTRAINT valid_action_type CHECK (action_type IN (
        'created',
        'updated',
        'status_changed',
        'priority_changed',
        'assigned',
        'unassigned',
        'description_changed',
        'due_date_changed',
        'tags_updated',
        'comment_added',
        'time_logged',
        'dependency_added',
        'dependency_removed',
        'subtask_added',
        'archived',
        'restored',
        'deleted'
    ))
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

-- Most common query: Get all activity for a task, sorted by time (newest first)
CREATE INDEX IF NOT EXISTS idx_task_activity_task_id
ON task_activity(task_id, timestamp DESC);

-- Project-wide activity log
CREATE INDEX IF NOT EXISTS idx_task_activity_project_id
ON task_activity(project_id, timestamp DESC);

-- User activity history
CREATE INDEX IF NOT EXISTS idx_task_activity_actor_id
ON task_activity(actor_id, timestamp DESC);

-- Global activity timeline (for admin/reporting)
CREATE INDEX IF NOT EXISTS idx_task_activity_timestamp
ON task_activity(timestamp DESC);

-- Optional: GIN index for JSONB queries (if filtering by specific field changes)
-- CREATE INDEX IF NOT EXISTS idx_task_activity_changes
-- ON task_activity USING GIN (changes);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

-- Enable RLS on task_activity table
ALTER TABLE task_activity ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view activity for projects they are members of
CREATE POLICY "Users can view task activity in their projects"
ON task_activity FOR SELECT
USING (
    project_id IN (
        SELECT project_id
        FROM project_members
        WHERE user_id = auth.uid()
    )
);

-- Policy: Users can insert activity for projects they are members of
CREATE POLICY "Users can create task activity in their projects"
ON task_activity FOR INSERT
WITH CHECK (
    project_id IN (
        SELECT project_id
        FROM project_members
        WHERE user_id = auth.uid()
    )
);

-- Policy: Activity records are immutable (no updates allowed)
-- This ensures audit trail integrity

-- Policy: Only project admins can delete activity (for cleanup/moderation)
CREATE POLICY "Project admins can delete task activity"
ON task_activity FOR DELETE
USING (
    project_id IN (
        SELECT project_id
        FROM project_members
        WHERE user_id = auth.uid()
        AND role = 'admin'
    )
);

-- ============================================================================
-- OPTIONAL: BACKFILL EXISTING TASKS WITH 'CREATED' ACTIVITY
-- ============================================================================
-- Uncomment this section if you want to create historical activity entries
-- for tasks that already exist in the database

/*
INSERT INTO task_activity (
    task_id,
    project_id,
    actor_id,
    actor_name,
    actor_role,
    action_type,
    timestamp,
    auto_description
)
SELECT
    t.id AS task_id,
    t.project_id,
    t.created_by_id AS actor_id,
    COALESCE(u.display_name, u.username, 'Unknown User') AS actor_name,
    pm.role AS actor_role,
    'created' AS action_type,
    t.created_at AS timestamp,
    'created this task' AS auto_description
FROM tasks t
LEFT JOIN users u ON t.created_by_id = u.id
LEFT JOIN project_members pm ON pm.user_id = t.created_by_id AND pm.project_id = t.project_id
WHERE NOT EXISTS (
    SELECT 1 FROM task_activity ta
    WHERE ta.task_id = t.id AND ta.action_type = 'created'
);
*/

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================
-- Run these queries after migration to verify everything works

-- 1. Check table structure
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'task_activity';

-- 2. Check indexes
-- SELECT indexname, indexdef
-- FROM pg_indexes
-- WHERE tablename = 'task_activity';

-- 3. Check RLS policies
-- SELECT policyname, permissive, roles, cmd, qual
-- FROM pg_policies
-- WHERE tablename = 'task_activity';

-- 4. Test insert (replace UUIDs with actual values from your database)
-- INSERT INTO task_activity (
--     task_id,
--     project_id,
--     actor_id,
--     actor_name,
--     action_type,
--     timestamp,
--     auto_description
-- ) VALUES (
--     'task-uuid-here',
--     'project-uuid-here',
--     'user-uuid-here',
--     'Test User',
--     'created',
--     EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
--     'created this task for testing'
-- );

-- ============================================================================
-- ROLLBACK SCRIPT (if needed)
-- ============================================================================
-- Run this section to completely remove the task_activity table

-- DROP TABLE IF EXISTS task_activity CASCADE;

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================
