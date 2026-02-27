-- ============================================================================
-- Verification Script: project_members.updated_at Column
-- ============================================================================
-- Purpose: Verify the FIX_PROJECT_MEMBERS_UPDATED_AT migration status
-- Usage: Run these queries before and after migration to compare results
-- ============================================================================

-- ============================================================================
-- BEFORE MIGRATION - Expected Results
-- ============================================================================

/*
PRE-MIGRATION STATE:
- Column check: 0 rows (column doesn't exist)
- Trigger check: 0 rows (trigger doesn't exist)
- Sync test: ERROR - column project_members.updated_at does not exist
*/

-- ============================================================================
-- AFTER MIGRATION - Expected Results
-- ============================================================================

/*
POST-MIGRATION STATE:
- Column check: 1 row (column exists, bigint, NOT NULL)
- Trigger check: 1 row (trigger exists)
- Data check: All records have updated_at values
- Sync test: SUCCESS - returns filtered results
*/

-- ============================================================================
-- CHECK 1: Column Existence
-- ============================================================================

SELECT
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'project_members'
  AND column_name = 'updated_at';

-- EXPECTED AFTER MIGRATION:
-- | table_name      | column_name | data_type | is_nullable | column_default              |
-- |-----------------|-------------|-----------|-------------|-----------------------------|
-- | project_members | updated_at  | bigint    | NO          | EXTRACT(EPOCH FROM now()...) |

-- ============================================================================
-- CHECK 2: All Columns in project_members Table
-- ============================================================================

SELECT
    ordinal_position,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'project_members'
ORDER BY ordinal_position;

-- EXPECTED COLUMNS:
-- 1. id (uuid, NO)
-- 2. project_id (uuid, NO)
-- 3. user_id (uuid, NO)
-- 4. role (text, NO)
-- 5. joined_at (bigint, NO)
-- 6. invited_by (uuid, YES)
-- 7. is_active (boolean, NO)
-- 8. last_activity_at (bigint, YES)
-- 9. custom_permissions (text[], YES)
-- 10. updated_at (bigint, NO) ← NEW COLUMN

-- ============================================================================
-- CHECK 3: Trigger Existence
-- ============================================================================

SELECT
    trigger_name,
    event_manipulation,
    event_object_table,
    action_timing,
    action_statement
FROM information_schema.triggers
WHERE event_object_table = 'project_members'
  AND trigger_name = 'project_members_updated_at_trigger';

-- EXPECTED AFTER MIGRATION:
-- | trigger_name                          | event_manipulation | action_timing |
-- |---------------------------------------|-------------------|---------------|
-- | project_members_updated_at_trigger    | UPDATE            | BEFORE        |

-- ============================================================================
-- CHECK 4: Function Existence
-- ============================================================================

SELECT
    routine_name,
    routine_type,
    data_type
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name = 'update_project_members_updated_at';

-- EXPECTED AFTER MIGRATION:
-- | routine_name                      | routine_type | data_type |
-- |-----------------------------------|--------------|-----------|
-- | update_project_members_updated_at | FUNCTION     | trigger   |

-- ============================================================================
-- CHECK 5: Data Integrity Check
-- ============================================================================

SELECT
    COUNT(*) as total_records,
    COUNT(updated_at) as records_with_updated_at,
    COUNT(*) - COUNT(updated_at) as null_count,
    MIN(updated_at) as earliest_updated_at,
    MAX(updated_at) as latest_updated_at
FROM project_members;

-- EXPECTED AFTER MIGRATION:
-- | total_records | records_with_updated_at | null_count | earliest_updated_at | latest_updated_at |
-- |---------------|-------------------------|------------|---------------------|-------------------|
-- | 27 (or more)  | 27 (or more)            | 0          | <timestamp>         | <timestamp>       |

-- ============================================================================
-- CHECK 6: Sample Data (verify backfill worked)
-- ============================================================================

SELECT
    id,
    user_id,
    project_id,
    role,
    joined_at,
    updated_at,
    (updated_at = joined_at) as backfilled,
    (updated_at IS NOT NULL) as has_updated_at
FROM project_members
ORDER BY joined_at DESC
LIMIT 5;

-- EXPECTED AFTER MIGRATION:
-- All rows should have:
-- - backfilled = true (updated_at matches joined_at initially)
-- - has_updated_at = true (no nulls)

-- ============================================================================
-- CHECK 7: Trigger Functionality Test
-- ============================================================================

-- Test 1: Select a member before update
SELECT
    id,
    project_id,
    user_id,
    role,
    updated_at as updated_at_before
FROM project_members
WHERE id = (SELECT id FROM project_members LIMIT 1);

-- Note the updated_at value, then run update:

-- Test 2: Update the member (no-op to trigger the trigger)
UPDATE project_members
SET role = role
WHERE id = (SELECT id FROM project_members LIMIT 1)
RETURNING id, updated_at as updated_at_after;

-- Test 3: Compare before and after
-- updated_at_after should be > updated_at_before (current timestamp)

-- ============================================================================
-- CHECK 8: Simulate Incremental Sync Query
-- ============================================================================

-- This simulates what the Android app does
DO $$
DECLARE
    sync_timestamp BIGINT := EXTRACT(EPOCH FROM NOW() - INTERVAL '1 hour') * 1000;
    result_count INT;
BEGIN
    -- Test incremental sync query (members updated in last hour)
    SELECT COUNT(*)
    INTO result_count
    FROM project_members
    WHERE updated_at > sync_timestamp;

    RAISE NOTICE 'Incremental sync test: % members updated in last hour', result_count;

    -- If trigger test was run (Check 7), this should return at least 1
    IF result_count > 0 THEN
        RAISE NOTICE 'SUCCESS: Incremental sync query works';
    ELSE
        RAISE NOTICE 'NOTE: No members updated recently (expected if no recent changes)';
    END IF;
END $$;

-- EXPECTED AFTER MIGRATION:
-- No errors, query completes successfully

-- ============================================================================
-- CHECK 9: Compare with Other Tables (Consistency Check)
-- ============================================================================

-- Verify project_members now matches the pattern of tasks and chat_rooms
SELECT
    'project_members' as table_name,
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'project_members' AND column_name = 'updated_at'
    ) as has_updated_at,
    EXISTS(
        SELECT 1 FROM information_schema.triggers
        WHERE event_object_table = 'project_members'
          AND trigger_name LIKE '%updated_at%'
    ) as has_trigger

UNION ALL

SELECT
    'tasks' as table_name,
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tasks' AND column_name = 'updated_at'
    ) as has_updated_at,
    EXISTS(
        SELECT 1 FROM information_schema.triggers
        WHERE event_object_table = 'tasks'
          AND trigger_name LIKE '%updated_at%'
    ) as has_trigger

UNION ALL

SELECT
    'chat_rooms' as table_name,
    EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'chat_rooms' AND column_name = 'updated_at'
    ) as has_updated_at,
    EXISTS(
        SELECT 1 FROM information_schema.triggers
        WHERE event_object_table = 'chat_rooms'
          AND trigger_name LIKE '%updated_at%'
    ) as has_trigger;

-- EXPECTED AFTER MIGRATION:
-- | table_name      | has_updated_at | has_trigger |
-- |-----------------|----------------|-------------|
-- | project_members | true           | true        |
-- | tasks           | true           | true/false  |
-- | chat_rooms      | true           | true/false  |

-- All three tables should have updated_at column

-- ============================================================================
-- CHECK 10: Performance Test (Optional)
-- ============================================================================

-- Test query performance with updated_at index
EXPLAIN ANALYZE
SELECT *
FROM project_members
WHERE updated_at > EXTRACT(EPOCH FROM NOW() - INTERVAL '1 day') * 1000;

-- EXPECTED: Query plan shows index scan (if many records)
-- For small datasets (<100 records), seq scan is fine

-- ============================================================================
-- DIAGNOSTIC: Full Migration Status Report
-- ============================================================================

DO $$
DECLARE
    column_exists BOOLEAN;
    trigger_exists BOOLEAN;
    function_exists BOOLEAN;
    total_records INT;
    null_count INT;
    migration_complete BOOLEAN;
BEGIN
    -- Check column
    SELECT EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'project_members' AND column_name = 'updated_at'
    ) INTO column_exists;

    -- Check trigger
    SELECT EXISTS(
        SELECT 1 FROM information_schema.triggers
        WHERE event_object_table = 'project_members'
          AND trigger_name = 'project_members_updated_at_trigger'
    ) INTO trigger_exists;

    -- Check function
    SELECT EXISTS(
        SELECT 1 FROM information_schema.routines
        WHERE routine_name = 'update_project_members_updated_at'
    ) INTO function_exists;

    -- Check data integrity
    SELECT COUNT(*), COUNT(*) - COUNT(updated_at)
    INTO total_records, null_count
    FROM project_members;

    -- Determine overall status
    migration_complete := column_exists AND trigger_exists AND function_exists AND (null_count = 0);

    -- Report
    RAISE NOTICE '========================================';
    RAISE NOTICE 'MIGRATION STATUS REPORT';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Column exists: %', column_exists;
    RAISE NOTICE 'Trigger exists: %', trigger_exists;
    RAISE NOTICE 'Function exists: %', function_exists;
    RAISE NOTICE 'Total records: %', total_records;
    RAISE NOTICE 'Records with NULL updated_at: %', null_count;
    RAISE NOTICE '========================================';

    IF migration_complete THEN
        RAISE NOTICE 'RESULT: ✅ Migration COMPLETE';
    ELSE
        RAISE NOTICE 'RESULT: ❌ Migration INCOMPLETE or NOT RUN';
        IF NOT column_exists THEN
            RAISE NOTICE '  - Missing: updated_at column';
        END IF;
        IF NOT trigger_exists THEN
            RAISE NOTICE '  - Missing: auto-update trigger';
        END IF;
        IF NOT function_exists THEN
            RAISE NOTICE '  - Missing: trigger function';
        END IF;
        IF null_count > 0 THEN
            RAISE NOTICE '  - Issue: % records have NULL updated_at', null_count;
        END IF;
    END IF;

    RAISE NOTICE '========================================';
END $$;

-- ============================================================================
-- QUICK STATUS CHECK (Run this first)
-- ============================================================================

-- Simple one-liner to check if migration has been run
SELECT
    CASE
        WHEN EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'project_members' AND column_name = 'updated_at'
        )
        THEN '✅ Migration complete - updated_at column exists'
        ELSE '❌ Migration not run - updated_at column missing'
    END as status;

-- ============================================================================
-- USAGE INSTRUCTIONS
-- ============================================================================

/*
BEFORE MIGRATION:
1. Run "QUICK STATUS CHECK" above
   - Expected: "❌ Migration not run - updated_at column missing"

2. Run "CHECK 1: Column Existence"
   - Expected: 0 rows (column doesn't exist)

DURING MIGRATION:
1. Open FIX_PROJECT_MEMBERS_UPDATED_AT.sql
2. Run the migration script
3. Migration should complete without errors

AFTER MIGRATION:
1. Run "QUICK STATUS CHECK" above
   - Expected: "✅ Migration complete - updated_at column exists"

2. Run "DIAGNOSTIC: Full Migration Status Report"
   - Expected: "RESULT: ✅ Migration COMPLETE"

3. Run all checks (CHECK 1-9)
   - All should show expected results

4. Clear Android app data
   - Settings → Apps → Kosmos → Clear Data

5. Launch app and verify sync
   - Logcat: "✅ Synced X members"
   - No errors: "column project_members.updated_at does not exist"

TROUBLESHOOTING:
- If CHECK 5 shows null_count > 0:
  → Run: UPDATE project_members SET updated_at = joined_at WHERE updated_at IS NULL;

- If CHECK 3 shows 0 rows (trigger missing):
  → Re-run STEP 4 and STEP 5 from FIX_PROJECT_MEMBERS_UPDATED_AT.sql

- If migration fails:
  → Check Supabase logs for error details
  → Run rollback script from FIX_PROJECT_MEMBERS_UPDATED_AT.sql
*/
