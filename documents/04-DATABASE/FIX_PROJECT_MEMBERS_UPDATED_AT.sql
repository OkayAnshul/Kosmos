-- ============================================================================
-- Migration: Add updated_at column to project_members table
-- ============================================================================
-- Purpose: Fix sync error "column project_members.updated_at does not exist"
-- Issue: SupabaseProjectMemberDataSource expects updated_at for incremental sync
-- Solution: Add updated_at column with auto-update trigger (matches tasks, chat_rooms pattern)
--
-- Risk: LOW - Additive only, backwards compatible
-- Rollback: Provided at end of file
-- ============================================================================

-- ============================================================================
-- STEP 1: Add updated_at column with default value
-- ============================================================================

ALTER TABLE project_members
ADD COLUMN updated_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;

-- ============================================================================
-- STEP 2: Backfill existing records (set to joined_at timestamp)
-- ============================================================================

UPDATE project_members
SET updated_at = joined_at
WHERE updated_at IS NULL;

-- ============================================================================
-- STEP 3: Make column NOT NULL after backfill
-- ============================================================================

ALTER TABLE project_members
ALTER COLUMN updated_at SET NOT NULL;

-- ============================================================================
-- STEP 4: Create trigger function to auto-update on modifications
-- ============================================================================

CREATE OR REPLACE FUNCTION update_project_members_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- STEP 5: Create trigger to auto-update updated_at on row changes
-- ============================================================================

CREATE TRIGGER project_members_updated_at_trigger
BEFORE UPDATE ON project_members
FOR EACH ROW
EXECUTE FUNCTION update_project_members_updated_at();

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Verify column was added
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'project_members'
  AND column_name = 'updated_at';

-- Expected: 1 row showing (updated_at, bigint, NO, extract...)

-- Verify all records have updated_at values
SELECT
    COUNT(*) as total_records,
    COUNT(updated_at) as records_with_updated_at,
    COUNT(*) - COUNT(updated_at) as null_count
FROM project_members;

-- Expected: null_count = 0 (all records have updated_at)

-- Verify trigger exists
SELECT
    trigger_name,
    event_manipulation,
    event_object_table,
    action_timing
FROM information_schema.triggers
WHERE event_object_table = 'project_members'
  AND trigger_name = 'project_members_updated_at_trigger';

-- Expected: 1 row showing trigger details

-- Sample data check (verify backfill worked)
SELECT
    id,
    user_id,
    project_id,
    role,
    joined_at,
    updated_at,
    (updated_at = joined_at) as backfilled
FROM project_members
LIMIT 5;

-- Expected: All rows show backfilled = true (updated_at matches joined_at)

-- ============================================================================
-- TEST TRIGGER (Optional - verify auto-update works)
-- ============================================================================

-- Update a record and verify updated_at changes
DO $$
DECLARE
    test_project_id UUID;
    test_user_id UUID;
    old_updated_at BIGINT;
    new_updated_at BIGINT;
BEGIN
    -- Get a test record
    SELECT project_id, user_id, updated_at
    INTO test_project_id, test_user_id, old_updated_at
    FROM project_members
    LIMIT 1;

    -- Wait a moment to ensure timestamp difference
    PERFORM pg_sleep(0.1);

    -- Update the record (no-op update to trigger the trigger)
    UPDATE project_members
    SET role = role
    WHERE project_id = test_project_id AND user_id = test_user_id
    RETURNING updated_at INTO new_updated_at;

    -- Verify updated_at changed
    IF new_updated_at > old_updated_at THEN
        RAISE NOTICE 'SUCCESS: Trigger working correctly (old: %, new: %)', old_updated_at, new_updated_at;
    ELSE
        RAISE WARNING 'FAILED: Trigger did not update timestamp (old: %, new: %)', old_updated_at, new_updated_at;
    END IF;
END $$;

-- ============================================================================
-- ROLLBACK SCRIPT (Run only if migration needs to be reverted)
-- ============================================================================

/*
-- WARNING: This will remove the updated_at column and trigger
-- Only run this if you need to rollback the migration

-- Remove trigger
DROP TRIGGER IF EXISTS project_members_updated_at_trigger ON project_members;

-- Remove trigger function
DROP FUNCTION IF EXISTS update_project_members_updated_at();

-- Remove column
ALTER TABLE project_members DROP COLUMN IF EXISTS updated_at;

-- Verify rollback
SELECT column_name
FROM information_schema.columns
WHERE table_name = 'project_members' AND column_name = 'updated_at';
-- Expected: 0 rows (column removed)
*/

-- ============================================================================
-- POST-MIGRATION INSTRUCTIONS
-- ============================================================================

/*
AFTER RUNNING THIS MIGRATION:

1. VERIFY MIGRATION SUCCESS
   - Run the verification queries above
   - All checks should pass

2. CLEAR APP DATA (REQUIRED!)
   - Android: Settings → Apps → Kosmos → Clear Data
   - This forces a fresh sync with new column

3. TEST APP SYNC
   - Log in to the app
   - Monitor logcat for sync messages
   - Expected: "✅ Synced X members for project <name>"
   - No errors: "column project_members.updated_at does not exist"

4. VERIFY UI
   - Project List: Shows all 27 projects
   - Project Workspace → Members: Shows member list
   - Project Workspace → Tasks: Shows tasks
   - Project Workspace → Chats: Shows chat rooms

5. TEST INCREMENTAL SYNC
   - Add a new member in Supabase
   - Pull to refresh in app
   - Verify new member appears (only fetches new data)

EXPECTED RESULTS:
✅ No code changes needed (SupabaseProjectMemberDataSource already correct)
✅ All 27 projects sync without errors
✅ Members, tasks, and chats populate in UI
✅ Incremental sync works for future updates
*/
