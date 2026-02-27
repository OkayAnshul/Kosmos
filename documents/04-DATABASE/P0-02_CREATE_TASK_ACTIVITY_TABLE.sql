-- P0-02 FIX: Create task_activity table for tracking all task changes
-- This enables activity timelines, audit logs, and change history
--
-- Run this migration in Supabase SQL Editor
-- Date: 2026-01-24
-- Phase 0: Critical Data Integrity

-- Create task_activity table
CREATE TABLE IF NOT EXISTS public.task_activity (
    -- Primary identifier
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign keys
    task_id UUID NOT NULL REFERENCES public.tasks(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES public.projects(id) ON DELETE CASCADE,

    -- Actor information (snapshot at time of action)
    actor_id UUID NOT NULL REFERENCES public.users(id) ON DELETE SET NULL,
    actor_name TEXT NOT NULL,
    actor_role TEXT,

    -- Action metadata
    action_type TEXT NOT NULL CHECK (action_type IN (
        'CREATED', 'UPDATED', 'STATUS_CHANGED', 'PRIORITY_CHANGED',
        'ASSIGNED', 'UNASSIGNED', 'DESCRIPTION_CHANGED', 'DUE_DATE_CHANGED',
        'TAGS_UPDATED', 'COMMENT_ADDED', 'TIME_LOGGED', 'DEPENDENCY_ADDED',
        'DEPENDENCY_REMOVED', 'SUBTASK_ADDED', 'ARCHIVED', 'RESTORED', 'DELETED'
    )),
    timestamp BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,

    -- Change tracking (stored as JSONB for flexibility)
    changes JSONB NOT NULL DEFAULT '[]'::JSONB,

    -- User-provided commit message (optional)
    commit_message TEXT,

    -- System-generated description (required)
    auto_description TEXT NOT NULL,

    -- Additional context (JSONB for extensibility)
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,

    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_task_activity_task_id
ON public.task_activity(task_id);

CREATE INDEX IF NOT EXISTS idx_task_activity_project_id
ON public.task_activity(project_id);

CREATE INDEX IF NOT EXISTS idx_task_activity_actor_id
ON public.task_activity(actor_id);

CREATE INDEX IF NOT EXISTS idx_task_activity_timestamp
ON public.task_activity(timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_task_activity_action_type
ON public.task_activity(action_type);

-- Create composite index for common queries
CREATE INDEX IF NOT EXISTS idx_task_activity_project_timestamp
ON public.task_activity(project_id, timestamp DESC);

-- Add table comments
COMMENT ON TABLE public.task_activity IS
'Tracks all changes to tasks with Git-style commit messages and detailed change history';

COMMENT ON COLUMN public.task_activity.changes IS
'JSONB array of FieldChange objects: [{"field":"status","fromValue":"TODO","toValue":"IN_PROGRESS"}]';

COMMENT ON COLUMN public.task_activity.metadata IS
'Extensible metadata field for additional context (JSONB)';

-- Enable Row Level Security
ALTER TABLE public.task_activity ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Users can view activity for projects they're members of
CREATE POLICY "Users can view activity in their projects"
ON public.task_activity
FOR SELECT
USING (
    EXISTS (
        SELECT 1
        FROM public.project_members
        WHERE project_members.project_id = task_activity.project_id
          AND project_members.user_id = auth.uid()
    )
);

-- RLS Policy: Users can insert activity for their own actions
CREATE POLICY "Users can create activity records"
ON public.task_activity
FOR INSERT
WITH CHECK (
    actor_id = auth.uid()
    AND EXISTS (
        SELECT 1
        FROM public.project_members
        WHERE project_members.project_id = task_activity.project_id
          AND project_members.user_id = auth.uid()
    )
);

-- RLS Policy: Only project admins can delete activity (for moderation)
CREATE POLICY "Project admins can delete activity"
ON public.task_activity
FOR DELETE
USING (
    EXISTS (
        SELECT 1
        FROM public.project_members
        WHERE project_members.project_id = task_activity.project_id
          AND project_members.user_id = auth.uid()
          AND project_members.role IN ('ADMIN', 'OWNER')
    )
);

-- Verify the migration
SELECT
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'task_activity'
ORDER BY ordinal_position;

-- Check indexes
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'task_activity';

-- Check RLS policies
SELECT policyname, cmd, qual
FROM pg_policies
WHERE tablename = 'task_activity';
