-- ============================================
-- QUICK SCHEMA CHECK - Run This ONE Query
-- ============================================
-- This combines all critical checks into ONE result
-- Copy entire query, paste in Supabase SQL Editor, click Run
-- Then share the results with me
-- ============================================

WITH
-- Get column counts for all tables
table_counts AS (
    SELECT
        'TABLE_COUNTS' as check_type,
        table_name,
        COUNT(*) as column_count,
        CASE table_name
            WHEN 'users' THEN 17
            WHEN 'projects' THEN 27
            WHEN 'tasks' THEN 23
            WHEN 'messages' THEN 15
            WHEN 'chat_rooms' THEN 18
            WHEN 'project_members' THEN 9
            WHEN 'task_activity' THEN 14
            ELSE 0
        END as expected_count
    FROM information_schema.columns
    WHERE table_schema = 'public'
    GROUP BY table_name
),

-- Check for version columns
version_checks AS (
    SELECT
        'VERSION_COLUMN' as check_type,
        t.table_name,
        CASE
            WHEN c.column_name IS NOT NULL THEN 1
            ELSE 0
        END as column_count,
        1 as expected_count
    FROM (VALUES ('users'), ('projects'), ('tasks')) AS t(table_name)
    LEFT JOIN information_schema.columns c
        ON c.table_schema = 'public'
        AND c.table_name = t.table_name
        AND c.column_name = 'version'
),

-- Check for critical project wizard fields
project_wizard_checks AS (
    SELECT
        'PROJECT_WIZARD' as check_type,
        w.column_name as table_name,
        CASE
            WHEN c.column_name IS NOT NULL THEN 1
            ELSE 0
        END as column_count,
        1 as expected_count
    FROM (VALUES
        ('category'), ('deadline'), ('website_url'), ('github_url'),
        ('project_motive'), ('tech_stack'), ('tags'), ('business_model'),
        ('target_audience'), ('industry_tags'), ('open_source_license')
    ) AS w(column_name)
    LEFT JOIN information_schema.columns c
        ON c.table_schema = 'public'
        AND c.table_name = 'projects'
        AND c.column_name = w.column_name
),

-- Check for metadata caching fields
metadata_checks AS (
    SELECT
        'METADATA_CACHE' as check_type,
        m.column_name as table_name,
        CASE
            WHEN c.column_name IS NOT NULL THEN 1
            ELSE 0
        END as column_count,
        1 as expected_count
    FROM (VALUES
        ('member_count'), ('chat_count'), ('task_count'),
        ('completed_task_count'), ('pending_task_count'), ('last_activity_at')
    ) AS m(column_name)
    LEFT JOIN information_schema.columns c
        ON c.table_schema = 'public'
        AND c.table_name = 'projects'
        AND c.column_name = m.column_name
),

-- Check for other critical columns
critical_checks AS (
    SELECT
        'CRITICAL_COLUMN' as check_type,
        cr.table_name || '.' || cr.column_name as table_name,
        CASE
            WHEN c.column_name IS NOT NULL THEN 1
            ELSE 0
        END as column_count,
        1 as expected_count
    FROM (VALUES
        ('messages', 'sender_name'),
        ('messages', 'reactions'),
        ('chat_rooms', 'participant_ids'),
        ('chat_rooms', 'is_archived'),
        ('tasks', 'comments')
    ) AS cr(table_name, column_name)
    LEFT JOIN information_schema.columns c
        ON c.table_schema = 'public'
        AND c.table_name = cr.table_name
        AND c.column_name = cr.column_name
)

-- Combine all checks
SELECT
    check_type,
    table_name,
    column_count,
    expected_count,
    CASE
        WHEN column_count = expected_count THEN '✅ OK'
        WHEN column_count < expected_count THEN '❌ MISSING'
        ELSE '⚠️ EXTRA'
    END as status,
    CASE
        WHEN column_count = expected_count THEN ''
        WHEN column_count < expected_count THEN
            'Missing ' || (expected_count - column_count)::text || ' column(s)'
        ELSE
            'Has ' || (column_count - expected_count)::text || ' extra column(s)'
    END as details
FROM (
    SELECT * FROM table_counts
    UNION ALL
    SELECT * FROM version_checks
    UNION ALL
    SELECT * FROM project_wizard_checks
    UNION ALL
    SELECT * FROM metadata_checks
    UNION ALL
    SELECT * FROM critical_checks
) combined
ORDER BY
    CASE check_type
        WHEN 'TABLE_COUNTS' THEN 1
        WHEN 'VERSION_COLUMN' THEN 2
        WHEN 'PROJECT_WIZARD' THEN 3
        WHEN 'METADATA_CACHE' THEN 4
        WHEN 'CRITICAL_COLUMN' THEN 5
    END,
    status DESC,
    table_name;

-- ============================================
-- WHAT TO LOOK FOR IN RESULTS
-- ============================================
/*
GOOD RESULTS (All ✅ OK):
- TABLE_COUNTS: All tables match expected counts
- VERSION_COLUMN: users, projects, tasks all have version
- PROJECT_WIZARD: All 11 wizard fields exist
- METADATA_CACHE: All 6 metadata fields exist
- CRITICAL_COLUMN: All critical columns exist

BAD RESULTS (Any ❌ MISSING):
- TABLE_COUNTS with status = ❌ MISSING = Table has fewer columns than expected
- VERSION_COLUMN with status = ❌ MISSING = Need to add version field
- PROJECT_WIZARD with status = ❌ MISSING = Need to add wizard fields
- METADATA_CACHE with status = ❌ MISSING = Need to add metadata fields

AFTER RUNNING THIS:
1. Screenshot the results OR copy the entire table
2. Share with me
3. I'll create exact migration scripts for any ❌ MISSING items
*/
