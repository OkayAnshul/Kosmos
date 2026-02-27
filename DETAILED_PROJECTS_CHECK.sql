-- ============================================
-- DETAILED PROJECTS TABLE CHECK
-- ============================================
-- This shows EXACTLY which columns are in projects table
-- and which ones are MISSING
-- ============================================

-- PART 1: Show all current columns in projects table
SELECT
    'CURRENT_COLUMNS' as section,
    ordinal_position as position,
    column_name,
    data_type,
    CASE
        WHEN is_nullable = 'YES' THEN 'NULL'
        ELSE 'NOT NULL'
    END as nullable,
    COALESCE(column_default, 'no default') as default_value
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'projects'
ORDER BY ordinal_position;

-- Copy results above first, then run query below

-- PART 2: Check for missing columns (27 expected)
SELECT
    'MISSING_CHECK' as section,
    expected_column,
    CASE
        WHEN actual_column IS NOT NULL THEN '✅ EXISTS'
        ELSE '❌ MISSING - NEED TO ADD'
    END as status,
    column_category
FROM (
    -- All 27 expected columns
    SELECT 'id' as expected_column, 'CORE' as column_category
    UNION ALL SELECT 'name', 'CORE'
    UNION ALL SELECT 'description', 'CORE'
    UNION ALL SELECT 'owner_id', 'CORE'
    UNION ALL SELECT 'status', 'CORE'
    UNION ALL SELECT 'visibility', 'CORE'
    UNION ALL SELECT 'created_at', 'CORE'
    UNION ALL SELECT 'updated_at', 'CORE'
    UNION ALL SELECT 'image_url', 'CORE'
    UNION ALL SELECT 'color', 'CORE'
    UNION ALL SELECT 'settings', 'CORE'
    -- Project Wizard fields (11)
    UNION ALL SELECT 'category', 'WIZARD'
    UNION ALL SELECT 'deadline', 'WIZARD'
    UNION ALL SELECT 'website_url', 'WIZARD'
    UNION ALL SELECT 'github_url', 'WIZARD'
    UNION ALL SELECT 'project_motive', 'WIZARD'
    UNION ALL SELECT 'tech_stack', 'WIZARD'
    UNION ALL SELECT 'tags', 'WIZARD'
    UNION ALL SELECT 'business_model', 'WIZARD'
    UNION ALL SELECT 'target_audience', 'WIZARD'
    UNION ALL SELECT 'industry_tags', 'WIZARD'
    UNION ALL SELECT 'open_source_license', 'WIZARD'
    -- Metadata caching fields (6)
    UNION ALL SELECT 'member_count', 'METADATA'
    UNION ALL SELECT 'chat_count', 'METADATA'
    UNION ALL SELECT 'task_count', 'METADATA'
    UNION ALL SELECT 'completed_task_count', 'METADATA'
    UNION ALL SELECT 'pending_task_count', 'METADATA'
    UNION ALL SELECT 'last_activity_at', 'METADATA'
    -- Version field
    UNION ALL SELECT 'version', 'VERSION'
) expected
LEFT JOIN (
    SELECT column_name as actual_column
    FROM information_schema.columns
    WHERE table_schema = 'public'
    AND table_name = 'projects'
) actual ON expected.expected_column = actual.actual_column
ORDER BY
    CASE
        WHEN actual_column IS NULL THEN 0  -- Missing columns first
        ELSE 1
    END,
    CASE column_category
        WHEN 'CORE' THEN 1
        WHEN 'WIZARD' THEN 2
        WHEN 'METADATA' THEN 3
        WHEN 'VERSION' THEN 4
    END,
    expected_column;

-- ============================================
-- INSTRUCTIONS
-- ============================================
/*
1. Run PART 1 first:
   - Copy lines 9-23
   - Paste in Supabase SQL Editor
   - Click Run
   - Note how many rows returned (should be 11 or 27)

2. Run PART 2:
   - Copy lines 27-93
   - Paste in Supabase SQL Editor
   - Click Run
   - Any row with ❌ MISSING = column needs to be added

3. Share results with me:
   - Tell me how many columns PART 1 returned
   - Copy the ❌ MISSING rows from PART 2

Example of GOOD result (all columns exist):
| section       | expected_column  | status    | column_category |
|---------------|------------------|-----------|-----------------|
| MISSING_CHECK | id               | ✅ EXISTS | CORE            |
| MISSING_CHECK | name             | ✅ EXISTS | CORE            |
... (all 27 rows show ✅ EXISTS)

Example of BAD result (missing columns):
| section       | expected_column  | status               | column_category |
|---------------|------------------|----------------------|-----------------|
| MISSING_CHECK | category         | ❌ MISSING - NEED TO ADD | WIZARD          |
| MISSING_CHECK | deadline         | ❌ MISSING - NEED TO ADD | WIZARD          |
... (any ❌ MISSING = problem!)
*/
