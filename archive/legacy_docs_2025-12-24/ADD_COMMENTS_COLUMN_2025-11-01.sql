-- ================================================================
-- Add Comments Column to Tasks Table
-- Date: 2025-11-01
-- Purpose: Fix PGRST204 error - add missing 'comments' column
-- ================================================================

-- Add comments column to tasks table
-- Type: JSONB to store array of TaskComment objects
-- Default: Empty array to match Kotlin model default
ALTER TABLE tasks
ADD COLUMN IF NOT EXISTS comments JSONB DEFAULT '[]'::jsonb;

-- Add comment for documentation
COMMENT ON COLUMN tasks.comments IS 'Array of TaskComment objects (Phase 2 feature). Structure: [{id, author_id, author_name, content, timestamp}]';

-- Reload Supabase PostgREST schema cache
-- This ensures the API recognizes the new column immediately
NOTIFY pgrst, 'reload schema';

-- ================================================================
-- Verification Query
-- ================================================================
-- Run this after the migration to verify the column was added:

SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'tasks' AND column_name = 'comments';

-- Expected result:
-- column_name | data_type | column_default
-- comments    | jsonb     | '[]'::jsonb

-- ================================================================
-- Testing
-- ================================================================
-- After running this SQL:
-- 1. Clear app data: adb shell pm clear com.example.kosmos
-- 2. Launch app and create a task
-- 3. Check logcat - should see "Task inserted successfully"
-- 4. Verify in Supabase Table Editor - task should appear with comments: []
-- ================================================================
