-- ============================================================================
-- TASK DEPENDENCIES & MILESTONES MIGRATION
-- ============================================================================
--
-- Purpose: Add task dependency tracking and milestone grouping to Kosmos
--
-- Features:
--   - Task dependencies (blocks, blocked_by, related_to)
--   - Milestones for grouping tasks
--   - Cycle detection support
--   - Critical path calculation support
--
-- Created: 2026-01-01
-- ============================================================================

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- 1. CREATE MILESTONES TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS milestones (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Foreign keys
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,

    -- Milestone details
    name TEXT NOT NULL,
    description TEXT,
    due_date BIGINT,
    status TEXT NOT NULL DEFAULT 'active',  -- 'active', 'completed', 'archived'
    color TEXT,  -- Hex color for UI display
    sort_order INTEGER DEFAULT 0,

    -- Metadata
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,

    -- Constraints
    CONSTRAINT valid_milestone_status CHECK (status IN ('active', 'completed', 'archived'))
);

-- ============================================================================
-- 2. CREATE TASK_DEPENDENCIES TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS task_dependencies (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- The task that has the dependency
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,

    -- The task that is depended upon
    depends_on_task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,

    -- Type of dependency
    dependency_type TEXT NOT NULL DEFAULT 'blocks',  -- 'blocks', 'blocked_by', 'related_to'

    -- Metadata
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    created_by UUID NOT NULL REFERENCES users(id),

    -- Constraints
    CONSTRAINT no_self_dependency CHECK (task_id != depends_on_task_id),
    CONSTRAINT unique_dependency UNIQUE (task_id, depends_on_task_id),
    CONSTRAINT valid_dependency_type CHECK (dependency_type IN ('blocks', 'blocked_by', 'related_to'))
);

-- ============================================================================
-- 3. ADD MILESTONE_ID COLUMN TO TASKS TABLE
-- ============================================================================

ALTER TABLE tasks
ADD COLUMN IF NOT EXISTS milestone_id UUID REFERENCES milestones(id) ON DELETE SET NULL;

-- ============================================================================
-- 4. CREATE INDEXES FOR PERFORMANCE
-- ============================================================================

-- Milestones indexes
CREATE INDEX IF NOT EXISTS idx_milestones_project_id
ON milestones(project_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_milestones_status
ON milestones(status);

CREATE INDEX IF NOT EXISTS idx_milestones_due_date
ON milestones(due_date);

-- Task dependencies indexes
CREATE INDEX IF NOT EXISTS idx_task_dependencies_task_id
ON task_dependencies(task_id);

CREATE INDEX IF NOT EXISTS idx_task_dependencies_depends_on
ON task_dependencies(depends_on_task_id);

CREATE INDEX IF NOT EXISTS idx_task_dependencies_type
ON task_dependencies(dependency_type);

-- Tasks milestone index
CREATE INDEX IF NOT EXISTS idx_tasks_milestone_id
ON tasks(milestone_id);

-- ============================================================================
-- 5. ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

-- Enable RLS on milestones table
ALTER TABLE milestones ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view milestones for their projects
CREATE POLICY milestones_select_policy ON milestones
    FOR SELECT
    USING (
        project_id IN (
            SELECT project_id
            FROM project_members
            WHERE user_id = auth.uid()
        )
    );

-- Policy: Users can insert milestones in their projects
CREATE POLICY milestones_insert_policy ON milestones
    FOR INSERT
    WITH CHECK (
        created_by = auth.uid()
        AND project_id IN (
            SELECT project_id
            FROM project_members
            WHERE user_id = auth.uid()
        )
    );

-- Policy: Users can update milestones in their projects
CREATE POLICY milestones_update_policy ON milestones
    FOR UPDATE
    USING (
        project_id IN (
            SELECT project_id
            FROM project_members
            WHERE user_id = auth.uid()
        )
    );

-- Policy: Users can delete milestones in their projects
CREATE POLICY milestones_delete_policy ON milestones
    FOR DELETE
    USING (
        project_id IN (
            SELECT project_id
            FROM project_members
            WHERE user_id = auth.uid()
        )
    );

-- Enable RLS on task_dependencies table
ALTER TABLE task_dependencies ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view dependencies for tasks in their projects
CREATE POLICY task_dependencies_select_policy ON task_dependencies
    FOR SELECT
    USING (
        task_id IN (
            SELECT tasks.id
            FROM tasks
            JOIN project_members ON tasks.project_id = project_members.project_id
            WHERE project_members.user_id = auth.uid()
        )
    );

-- Policy: Users can insert dependencies for tasks in their projects
CREATE POLICY task_dependencies_insert_policy ON task_dependencies
    FOR INSERT
    WITH CHECK (
        created_by = auth.uid()
        AND task_id IN (
            SELECT tasks.id
            FROM tasks
            JOIN project_members ON tasks.project_id = project_members.project_id
            WHERE project_members.user_id = auth.uid()
        )
    );

-- Policy: Users can delete dependencies for tasks in their projects
CREATE POLICY task_dependencies_delete_policy ON task_dependencies
    FOR DELETE
    USING (
        task_id IN (
            SELECT tasks.id
            FROM tasks
            JOIN project_members ON tasks.project_id = project_members.project_id
            WHERE project_members.user_id = auth.uid()
        )
    );

-- ============================================================================
-- 6. HELPER FUNCTIONS
-- ============================================================================

-- Function to get all tasks that block a given task
CREATE OR REPLACE FUNCTION get_blocking_tasks(target_task_id UUID)
RETURNS TABLE (
    task_id UUID,
    task_title TEXT,
    task_status TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT t.id, t.title, t.status
    FROM tasks t
    JOIN task_dependencies td ON t.id = td.depends_on_task_id
    WHERE td.task_id = target_task_id
    AND td.dependency_type = 'blocks';
END;
$$ LANGUAGE plpgsql;

-- Function to get all tasks blocked by a given task
CREATE OR REPLACE FUNCTION get_blocked_tasks(target_task_id UUID)
RETURNS TABLE (
    task_id UUID,
    task_title TEXT,
    task_status TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT t.id, t.title, t.status
    FROM tasks t
    JOIN task_dependencies td ON t.id = td.task_id
    WHERE td.depends_on_task_id = target_task_id
    AND td.dependency_type = 'blocks';
END;
$$ LANGUAGE plpgsql;

-- Function to check if task can start (all blocking dependencies are done)
CREATE OR REPLACE FUNCTION can_task_start(target_task_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    blocking_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO blocking_count
    FROM task_dependencies td
    JOIN tasks t ON t.id = td.depends_on_task_id
    WHERE td.task_id = target_task_id
    AND td.dependency_type = 'blocks'
    AND t.status != 'DONE';

    RETURN blocking_count = 0;
END;
$$ LANGUAGE plpgsql;

-- Function to get milestone progress (% of tasks completed)
CREATE OR REPLACE FUNCTION get_milestone_progress(milestone_uuid UUID)
RETURNS NUMERIC AS $$
DECLARE
    total_tasks INTEGER;
    completed_tasks INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO total_tasks
    FROM tasks
    WHERE milestone_id = milestone_uuid;

    IF total_tasks = 0 THEN
        RETURN 0;
    END IF;

    SELECT COUNT(*)
    INTO completed_tasks
    FROM tasks
    WHERE milestone_id = milestone_uuid
    AND status = 'DONE';

    RETURN (completed_tasks::NUMERIC / total_tasks::NUMERIC) * 100;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 7. TRIGGER TO AUTO-UPDATE updated_at TIMESTAMP
-- ============================================================================

CREATE OR REPLACE FUNCTION update_milestones_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER milestones_updated_at_trigger
    BEFORE UPDATE ON milestones
    FOR EACH ROW
    EXECUTE FUNCTION update_milestones_updated_at();

-- ============================================================================
-- 8. SAMPLE DATA (OPTIONAL - FOR TESTING)
-- ============================================================================

-- Uncomment to add sample milestone
/*
INSERT INTO milestones (
    project_id,
    name,
    description,
    due_date,
    status,
    color,
    created_by
) VALUES (
    '<project_uuid>',
    'MVP Release',
    'Minimum viable product launch',
    EXTRACT(EPOCH FROM NOW() + INTERVAL '30 days')::BIGINT * 1000,
    'active',
    '#3B82F6',
    '<user_uuid>'
);
*/

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Verify tables were created
SELECT table_name, table_type
FROM information_schema.tables
WHERE table_name IN ('milestones', 'task_dependencies');

-- Verify indexes were created
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('milestones', 'task_dependencies');

-- Verify column was added to tasks
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'tasks' AND column_name = 'milestone_id';

-- Verify RLS policies were created
SELECT tablename, policyname, permissive, roles, cmd
FROM pg_policies
WHERE tablename IN ('milestones', 'task_dependencies');

-- Verify functions were created
SELECT routine_name, routine_type
FROM information_schema.routines
WHERE routine_name LIKE '%milestone%' OR routine_name LIKE '%blocking%' OR routine_name LIKE '%blocked%';

-- ============================================================================
-- ROLLBACK (IF NEEDED)
-- ============================================================================

/*
-- Drop tables and all dependencies
DROP TABLE IF EXISTS task_dependencies CASCADE;
DROP TABLE IF EXISTS milestones CASCADE;

-- Remove column from tasks
ALTER TABLE tasks DROP COLUMN IF EXISTS milestone_id;

-- Drop functions
DROP FUNCTION IF EXISTS get_blocking_tasks(UUID);
DROP FUNCTION IF EXISTS get_blocked_tasks(UUID);
DROP FUNCTION IF EXISTS can_task_start(UUID);
DROP FUNCTION IF EXISTS get_milestone_progress(UUID);
DROP FUNCTION IF EXISTS update_milestones_updated_at();
*/

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
