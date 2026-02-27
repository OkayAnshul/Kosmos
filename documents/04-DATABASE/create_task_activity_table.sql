-- ============================================================================
-- Kosmos: Create task_activity Table
-- ============================================================================
-- Purpose: Add missing task_activity table to Supabase
-- Issue: TaskActivity exists in Room but not in Supabase
-- Impact: Task history never syncs, activity log incomplete
--
-- Run: Execute in Supabase SQL Editor
-- Date: 2026-01-23
-- ============================================================================

-- Create task_activity table
CREATE TABLE IF NOT EXISTS task_activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,  -- 'created', 'updated', 'status_changed', 'completed', etc.
    field_changed VARCHAR(100),   -- 'status', 'assignee', 'priority', 'description', etc.
    old_value TEXT,               -- Previous value (JSON or string)
    new_value TEXT,               -- New value (JSON or string)
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    description TEXT NOT NULL,    -- Human-readable description
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add indexes for common queries
CREATE INDEX IF NOT EXISTS idx_task_activity_task_id ON task_activity(task_id);
CREATE INDEX IF NOT EXISTS idx_task_activity_user_id ON task_activity(user_id);
CREATE INDEX IF NOT EXISTS idx_task_activity_timestamp ON task_activity(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_task_activity_action ON task_activity(action);

-- Add updated_at trigger
CREATE OR REPLACE FUNCTION update_task_activity_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER task_activity_updated_at
    BEFORE UPDATE ON task_activity
    FOR EACH ROW
    EXECUTE FUNCTION update_task_activity_updated_at();

-- Enable Row Level Security (RLS)
ALTER TABLE task_activity ENABLE ROW LEVEL SECURITY;

-- RLS Policy 1: Users can view activities for tasks they have access to
CREATE POLICY "Users can view task activities they have access to"
ON task_activity FOR SELECT
USING (
    -- Can view if they have access to the task
    task_id IN (
        SELECT id FROM tasks
        WHERE created_by_id = auth.uid()
           OR assigned_to_id = auth.uid()
           OR project_id IN (
               SELECT project_id FROM project_members
               WHERE user_id = auth.uid()
           )
    )
);

-- RLS Policy 2: Only authenticated users can insert activities
CREATE POLICY "Authenticated users can insert task activities"
ON task_activity FOR INSERT
WITH CHECK (auth.uid() IS NOT NULL);

-- RLS Policy 3: Users can only update their own activities
CREATE POLICY "Users can update their own task activities"
ON task_activity FOR UPDATE
USING (user_id = auth.uid());

-- RLS Policy 4: Users can only delete their own activities
CREATE POLICY "Users can delete their own task activities"
ON task_activity FOR DELETE
USING (user_id = auth.uid());

-- Grant permissions
GRANT ALL ON task_activity TO authenticated;
GRANT SELECT ON task_activity TO anon;

-- ============================================================================
-- Verification Queries (Run after creation)
-- ============================================================================

-- 1. Verify table exists
-- SELECT table_name, table_type
-- FROM information_schema.tables
-- WHERE table_schema = 'public' AND table_name = 'task_activity';

-- 2. Verify columns
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'task_activity'
-- ORDER BY ordinal_position;

-- 3. Verify indexes
-- SELECT indexname, indexdef
-- FROM pg_indexes
-- WHERE tablename = 'task_activity';

-- 4. Verify RLS policies
-- SELECT policyname, cmd, qual
-- FROM pg_policies
-- WHERE tablename = 'task_activity';

-- ============================================================================
-- Sample Test Data (Optional - for testing)
-- ============================================================================

-- Insert sample activity (replace UUIDs with actual values from your database)
-- INSERT INTO task_activity (task_id, user_id, action, description)
-- VALUES (
--     '00000000-0000-0000-0000-000000000001'::UUID,  -- Replace with actual task_id
--     auth.uid(),
--     'created',
--     'Task created'
-- );

-- ============================================================================
-- Rollback (if needed)
-- ============================================================================

-- DROP TRIGGER IF EXISTS task_activity_updated_at ON task_activity;
-- DROP FUNCTION IF EXISTS update_task_activity_updated_at();
-- DROP TABLE IF EXISTS task_activity CASCADE;

-- ============================================================================
-- Migration Notes
-- ============================================================================
--
-- After running this script:
-- 1. Verify table creation with verification queries above
-- 2. Test activity insertion from Android app
-- 3. Verify RLS policies work correctly
-- 4. Monitor Supabase logs for errors
-- 5. Update Room database version in Android app if needed
--
-- Related Files:
-- - app/src/main/java/com/example/kosmos/core/models/TaskActivity.kt
-- - app/src/main/java/com/example/kosmos/core/database/dao/TaskActivityDao.kt
-- - app/src/main/java/com/example/kosmos/data/datasource/SupabaseTaskActivityDataSource.kt
--
-- ============================================================================
