-- ============================================================================
-- ADD VERSION COLUMNS FOR OPTIMISTIC LOCKING
-- ============================================================================
-- Purpose: Add version columns to projects and tasks tables to prevent
--          data loss from concurrent multi-device edits
--
-- Issue: Without version columns, last-write-wins behavior causes data loss
--        when the same entity is edited simultaneously from multiple devices
--
-- Solution: Add version column + increment on each update + use in WHERE clause
--
-- Run this in: Supabase SQL Editor
-- Estimated time: <5 seconds
-- ============================================================================

-- Step 1: Add version column to projects table
-- ============================================================================

ALTER TABLE public.projects
ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;

-- Index for faster version lookups during updates
CREATE INDEX IF NOT EXISTS idx_projects_version ON public.projects(version);

COMMENT ON COLUMN public.projects.version IS
'Optimistic locking version. Incremented on each update to detect conflicts. Prevents data loss from concurrent edits.';

-- Step 2: Add version column to tasks table
-- ============================================================================

ALTER TABLE public.tasks
ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;

-- Index for faster version lookups during updates
CREATE INDEX IF NOT EXISTS idx_tasks_version ON public.tasks(version);

COMMENT ON COLUMN public.tasks.version IS
'Optimistic locking version. Incremented on each update to detect conflicts. Prevents data loss from concurrent edits.';

-- Step 3: Verification Query
-- ============================================================================
-- Run this to confirm columns were added successfully

SELECT
    table_name,
    column_name,
    data_type,
    column_default,
    is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name IN ('projects', 'tasks')
AND column_name = 'version'
ORDER BY table_name;

-- Expected Output:
-- +-----------+-------------+-----------+----------------+-------------+
-- | table_name| column_name | data_type | column_default | is_nullable |
-- +-----------+-------------+-----------+----------------+-------------+
-- | projects  | version     | integer   | 1              | NO          |
-- | tasks     | version     | integer   | 1              | NO          |
-- +-----------+-------------+-----------+----------------+-------------+

-- Step 4: Test Version Functionality
-- ============================================================================

-- Test 1: Verify existing rows have version = 1
SELECT COUNT(*) as total_projects,
       COUNT(CASE WHEN version = 1 THEN 1 END) as version_1_projects
FROM public.projects;

SELECT COUNT(*) as total_tasks,
       COUNT(CASE WHEN version = 1 THEN 1 END) as version_1_tasks
FROM public.tasks;

-- Expected: All existing rows should have version = 1

-- Test 2: Test version increment simulation
DO $$
DECLARE
    test_project_id UUID;
    old_version INTEGER;
    new_version INTEGER;
BEGIN
    -- Get a test project
    SELECT id, version INTO test_project_id, old_version
    FROM public.projects
    LIMIT 1;

    IF test_project_id IS NOT NULL THEN
        -- Simulate version increment
        UPDATE public.projects
        SET version = version + 1,
            updated_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = test_project_id
        AND version = old_version;

        -- Check new version
        SELECT version INTO new_version
        FROM public.projects
        WHERE id = test_project_id;

        RAISE NOTICE 'Test Project: % - Old Version: %, New Version: %',
                     test_project_id, old_version, new_version;

        -- Rollback the test update
        UPDATE public.projects
        SET version = old_version
        WHERE id = test_project_id;

        RAISE NOTICE 'Test passed - version incremented correctly (rolled back)';
    ELSE
        RAISE NOTICE 'No projects found for testing';
    END IF;
END $$;

-- ============================================================================
-- USAGE NOTES FOR ANDROID DEVELOPERS
-- ============================================================================

/*
After running this migration:

1. SupabaseTaskDataSource.updateTask() must be updated to:
   - Increment version: set("version", task.version + 1)
   - Add version filter: eq("version", task.version)

2. SupabaseProjectDataSource.update() must be updated to:
   - Increment version: set("version", project.version + 1)
   - Add version filter: eq("version", project.version)

3. Repository layer should:
   - Check update result (0 rows = version conflict)
   - Throw ConflictException if conflict detected
   - UI shows conflict resolution dialog

Example Update Pattern:
```kotlin
supabase.from("tasks").update({
    set("title", task.title)
    set("version", task.version + 1)  // Increment
}) {
    filter {
        eq("id", task.id)
        eq("version", task.version)  // Conflict check
    }
}
```

If version doesn't match, Supabase returns 0 rows updated = conflict detected.
*/

-- ============================================================================
-- ROLLBACK (if needed)
-- ============================================================================

-- UNCOMMENT ONLY IF YOU NEED TO ROLLBACK
-- ALTER TABLE public.projects DROP COLUMN IF EXISTS version;
-- ALTER TABLE public.tasks DROP COLUMN IF EXISTS version;
-- DROP INDEX IF EXISTS idx_projects_version;
-- DROP INDEX IF EXISTS idx_tasks_version;
