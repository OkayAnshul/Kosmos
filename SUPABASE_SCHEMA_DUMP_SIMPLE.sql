-- ============================================
-- SIMPLIFIED SCHEMA DUMP - Run Each Query Separately
-- ============================================
-- Copy each query one at a time into Supabase SQL Editor
-- Save the results of each query
-- ============================================

-- ============================================
-- QUERY 1: TABLE LIST WITH COLUMN COUNTS
-- ============================================
-- Run this first to see all tables
SELECT
    table_name,
    (SELECT COUNT(*) FROM information_schema.columns c
     WHERE c.table_name = t.table_name AND c.table_schema = 'public') as column_count
FROM information_schema.tables t
WHERE table_schema = 'public'
AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- EXPECTED TABLES:
-- users (should have 17-18 columns)
-- projects (should have 11 or 27 columns - THIS IS CRITICAL)
-- tasks (should have 21-23 columns)
-- messages (should have 15 columns)
-- chat_rooms (should have 14-18 columns)
-- project_members (should have 9 columns)
-- task_activity (should have 14 columns)


-- ============================================
-- QUERY 2: USERS TABLE COLUMNS
-- ============================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'users'
ORDER BY ordinal_position;

-- CRITICAL: Check if 'version' column exists


-- ============================================
-- QUERY 3: PROJECTS TABLE COLUMNS ⭐ MOST IMPORTANT
-- ============================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'projects'
ORDER BY ordinal_position;

-- CRITICAL COLUMNS TO CHECK:
-- ✅ Must have: id, name, description, owner_id, status, visibility
-- ❓ Should have: category, deadline, website_url, github_url, project_motive
-- ❓ Should have: tech_stack, tags, business_model, target_audience
-- ❓ Should have: member_count, chat_count, task_count, version


-- ============================================
-- QUERY 4: TASKS TABLE COLUMNS
-- ============================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'tasks'
ORDER BY ordinal_position;

-- CRITICAL: Check if 'version' column exists


-- ============================================
-- QUERY 5: CHAT_ROOMS TABLE COLUMNS
-- ============================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'chat_rooms'
ORDER BY ordinal_position;

-- CRITICAL: Check if participant_ids, is_archived, is_pinned, is_private exist


-- ============================================
-- QUERY 6: MESSAGES TABLE COLUMNS
-- ============================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'messages'
ORDER BY ordinal_position;

-- CRITICAL: Check if sender_name, sender_photo_url, reactions, read_by exist


-- ============================================
-- QUERY 7: PROJECT_MEMBERS TABLE COLUMNS
-- ============================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'project_members'
ORDER BY ordinal_position;


-- ============================================
-- QUERY 8: TASK_ACTIVITY TABLE COLUMNS
-- ============================================
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'task_activity'
ORDER BY ordinal_position;


-- ============================================
-- QUERY 9: CHECK FOR VOICE_MESSAGES AND ACTION_ITEMS TABLES
-- ============================================
SELECT
    table_name,
    (SELECT COUNT(*) FROM information_schema.columns c
     WHERE c.table_name = t.table_name) as column_count
FROM information_schema.tables t
WHERE table_schema = 'public'
AND table_name IN ('voice_messages', 'action_items')
ORDER BY table_name;

-- If this returns 0 rows, these tables don't exist


-- ============================================
-- QUERY 10: FOREIGN KEYS (All Tables)
-- ============================================
SELECT
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table,
    ccu.column_name AS foreign_column,
    rc.delete_rule
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
JOIN information_schema.referential_constraints AS rc
    ON rc.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
AND tc.table_schema = 'public'
ORDER BY tc.table_name, kcu.column_name;


-- ============================================
-- QUERY 11: INDEXES (All Tables)
-- ============================================
SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;


-- ============================================
-- QUERY 12: RLS POLICIES
-- ============================================
SELECT
    tablename,
    policyname,
    cmd as command,
    SUBSTRING(qual::text, 1, 100) as using_clause
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, policyname;


-- ============================================
-- QUERY 13: TRIGGERS
-- ============================================
SELECT
    trigger_name,
    event_object_table as table_name,
    action_timing,
    event_manipulation,
    SUBSTRING(action_statement, 1, 100) as action
FROM information_schema.triggers
WHERE trigger_schema = 'public'
ORDER BY event_object_table, trigger_name;


-- ============================================
-- QUERY 14: CRITICAL CHECKS - VERSION COLUMNS
-- ============================================
SELECT
    'users' as table_name,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'version'
    ) THEN '✅ HAS version' ELSE '❌ MISSING version' END as version_status

UNION ALL

SELECT
    'projects',
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'projects' AND column_name = 'version'
    ) THEN '✅ HAS version' ELSE '❌ MISSING version' END

UNION ALL

SELECT
    'tasks',
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'tasks' AND column_name = 'version'
    ) THEN '✅ HAS version' ELSE '❌ MISSING version' END;


-- ============================================
-- QUERY 15: CRITICAL CHECKS - PROJECT WIZARD FIELDS
-- ============================================
SELECT
    column_name,
    CASE
        WHEN column_name IN (
            'category', 'deadline', 'website_url', 'github_url', 'project_motive',
            'tech_stack', 'tags', 'business_model', 'target_audience',
            'industry_tags', 'open_source_license'
        ) THEN '✅ WIZARD FIELD'
        WHEN column_name IN (
            'member_count', 'chat_count', 'task_count',
            'completed_task_count', 'pending_task_count', 'last_activity_at'
        ) THEN '✅ METADATA FIELD'
        WHEN column_name = 'version' THEN '✅ VERSION FIELD'
        ELSE 'CORE FIELD'
    END as field_category
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'projects'
ORDER BY
    CASE
        WHEN column_name IN ('category', 'deadline', 'website_url', 'github_url', 'project_motive',
            'tech_stack', 'tags', 'business_model', 'target_audience', 'industry_tags', 'open_source_license')
        THEN 1
        WHEN column_name IN ('member_count', 'chat_count', 'task_count',
            'completed_task_count', 'pending_task_count', 'last_activity_at')
        THEN 2
        WHEN column_name = 'version' THEN 3
        ELSE 4
    END,
    ordinal_position;


-- ============================================
-- QUERY 16: MISSING COLUMNS DETECTION
-- ============================================
-- Expected columns vs actual columns
WITH expected_projects AS (
    SELECT unnest(ARRAY[
        'id', 'name', 'description', 'owner_id', 'status', 'visibility',
        'created_at', 'updated_at', 'image_url', 'color', 'settings',
        -- Project Wizard fields
        'category', 'deadline', 'website_url', 'github_url', 'project_motive',
        'tech_stack', 'tags', 'business_model', 'target_audience',
        'industry_tags', 'open_source_license',
        -- Metadata caching fields
        'member_count', 'chat_count', 'task_count',
        'completed_task_count', 'pending_task_count', 'last_activity_at',
        -- Version field
        'version'
    ]) as expected_column
),
actual_projects AS (
    SELECT column_name as actual_column
    FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'projects'
)
SELECT
    e.expected_column,
    CASE
        WHEN a.actual_column IS NOT NULL THEN '✅ EXISTS'
        ELSE '❌ MISSING'
    END as status
FROM expected_projects e
LEFT JOIN actual_projects a ON e.expected_column = a.actual_column
ORDER BY
    CASE WHEN a.actual_column IS NULL THEN 1 ELSE 2 END,
    e.expected_column;


-- ============================================
-- INSTRUCTIONS FOR RUNNING
-- ============================================
/*
1. Copy QUERY 1, paste in Supabase SQL Editor, click Run
   → Note the column counts

2. Copy QUERY 3 (projects table), paste, click Run
   → Count how many rows returned
   → Should be 27 rows, if less = MISSING COLUMNS

3. Copy QUERY 14 (version checks), paste, click Run
   → Should see ✅ for all three tables
   → If ❌ = need to add version column

4. Copy QUERY 16 (missing columns), paste, click Run
   → Shows exactly which columns are missing
   → This is the KEY query

5. Share results of queries 1, 3, 14, and 16 with me

That's enough to know exactly what migrations you need!
*/
