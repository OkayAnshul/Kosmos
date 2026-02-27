-- ============================================================================
-- TIME TRACKING MIGRATION
-- ============================================================================
--
-- Purpose: Add time tracking functionality to Kosmos
--
-- Features:
--   - Track time spent on tasks with active timers
--   - Manual time entry support
--   - Billable hours tracking
--   - Auto-calculate task actual_hours field
--
-- Created: 2026-01-01
-- ============================================================================

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- 1. CREATE TIME_ENTRIES TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS time_entries (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Foreign keys
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Time tracking
    start_time BIGINT NOT NULL,
    end_time BIGINT,  -- NULL if timer is currently running
    duration_seconds INTEGER,  -- Calculated when timer stops

    -- Entry details
    description TEXT,
    is_billable BOOLEAN DEFAULT true,
    hourly_rate DECIMAL(10, 2),  -- Stored for history (rate at time of entry)
    is_manual BOOLEAN DEFAULT false,  -- True if added manually, false if from timer

    -- Metadata
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,

    -- Constraints
    CONSTRAINT valid_time_range CHECK (end_time IS NULL OR end_time > start_time),
    CONSTRAINT valid_duration CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
);

-- ============================================================================
-- 2. CREATE INDEXES FOR PERFORMANCE
-- ============================================================================

-- Index for getting all time entries for a task (most common query)
CREATE INDEX IF NOT EXISTS idx_time_entries_task_id
ON time_entries(task_id, start_time DESC);

-- Index for getting all time entries for a user
CREATE INDEX IF NOT EXISTS idx_time_entries_user_id
ON time_entries(user_id, start_time DESC);

-- Partial index for finding running timers (WHERE clause makes it very efficient)
CREATE INDEX IF NOT EXISTS idx_time_entries_running
ON time_entries(user_id, end_time)
WHERE end_time IS NULL;

-- Index for project-wide time reports
CREATE INDEX IF NOT EXISTS idx_time_entries_project_id
ON time_entries(project_id, start_time DESC);

-- ============================================================================
-- 3. ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

-- Enable RLS on time_entries table
ALTER TABLE time_entries ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view time entries for tasks in their projects
CREATE POLICY time_entries_select_policy ON time_entries
    FOR SELECT
    USING (
        project_id IN (
            SELECT project_id
            FROM project_members
            WHERE user_id = auth.uid()
        )
    );

-- Policy: Users can insert their own time entries
CREATE POLICY time_entries_insert_policy ON time_entries
    FOR INSERT
    WITH CHECK (
        user_id = auth.uid()
        AND project_id IN (
            SELECT project_id
            FROM project_members
            WHERE user_id = auth.uid()
        )
    );

-- Policy: Users can update their own time entries
CREATE POLICY time_entries_update_policy ON time_entries
    FOR UPDATE
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- Policy: Users can delete their own time entries
CREATE POLICY time_entries_delete_policy ON time_entries
    FOR DELETE
    USING (user_id = auth.uid());

-- ============================================================================
-- 4. HELPER FUNCTIONS
-- ============================================================================

-- Function to calculate total time for a task (in seconds)
CREATE OR REPLACE FUNCTION get_total_time_for_task(task_uuid UUID)
RETURNS INTEGER AS $$
BEGIN
    RETURN COALESCE(
        (SELECT SUM(duration_seconds)
         FROM time_entries
         WHERE task_id = task_uuid
         AND duration_seconds IS NOT NULL),
        0
    );
END;
$$ LANGUAGE plpgsql;

-- Function to calculate billable time for a task (in seconds)
CREATE OR REPLACE FUNCTION get_billable_time_for_task(task_uuid UUID)
RETURNS INTEGER AS $$
BEGIN
    RETURN COALESCE(
        (SELECT SUM(duration_seconds)
         FROM time_entries
         WHERE task_id = task_uuid
         AND is_billable = true
         AND duration_seconds IS NOT NULL),
        0
    );
END;
$$ LANGUAGE plpgsql;

-- Function to get running timer for a user
CREATE OR REPLACE FUNCTION get_running_timer_for_user(user_uuid UUID)
RETURNS TABLE (
    entry_id UUID,
    task_id UUID,
    start_time BIGINT,
    description TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT id, time_entries.task_id, time_entries.start_time, time_entries.description
    FROM time_entries
    WHERE user_id = user_uuid
    AND end_time IS NULL
    ORDER BY start_time DESC
    LIMIT 1;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 5. TRIGGER TO AUTO-UPDATE updated_at TIMESTAMP
-- ============================================================================

CREATE OR REPLACE FUNCTION update_time_entry_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER time_entries_updated_at_trigger
    BEFORE UPDATE ON time_entries
    FOR EACH ROW
    EXECUTE FUNCTION update_time_entry_updated_at();

-- ============================================================================
-- 6. SAMPLE DATA (OPTIONAL - FOR TESTING)
-- ============================================================================

-- Uncomment to add sample time entries for testing
/*
INSERT INTO time_entries (
    task_id,
    project_id,
    user_id,
    start_time,
    end_time,
    duration_seconds,
    description,
    is_billable,
    hourly_rate,
    is_manual
) VALUES (
    '<task_uuid>',
    '<project_uuid>',
    '<user_uuid>',
    EXTRACT(EPOCH FROM NOW() - INTERVAL '2 hours')::BIGINT * 1000,
    EXTRACT(EPOCH FROM NOW() - INTERVAL '1 hour')::BIGINT * 1000,
    3600,
    'Worked on implementation',
    true,
    75.00,
    false
);
*/

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Verify table was created
SELECT table_name, table_type
FROM information_schema.tables
WHERE table_name = 'time_entries';

-- Verify indexes were created
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'time_entries';

-- Verify RLS policies were created
SELECT policyname, permissive, roles, cmd, qual
FROM pg_policies
WHERE tablename = 'time_entries';

-- Verify functions were created
SELECT routine_name, routine_type
FROM information_schema.routines
WHERE routine_name LIKE '%time%';

-- ============================================================================
-- ROLLBACK (IF NEEDED)
-- ============================================================================

/*
-- Drop table and all dependencies
DROP TABLE IF EXISTS time_entries CASCADE;

-- Drop functions
DROP FUNCTION IF EXISTS get_total_time_for_task(UUID);
DROP FUNCTION IF EXISTS get_billable_time_for_task(UUID);
DROP FUNCTION IF EXISTS get_running_timer_for_user(UUID);
DROP FUNCTION IF EXISTS update_time_entry_updated_at();
*/

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
