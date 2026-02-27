-- ============================================
-- COMPLETE DATABASE SCHEMA EXPORT FOR ANALYSIS
-- ============================================
-- Run this in Supabase SQL Editor to get full schema details
-- Copy all results and provide to Claude for analysis
-- ============================================

-- ============================================
-- PART 1: List All Tables
-- ============================================
SELECT
    'TABLE_LIST' as section,
    table_name,
    table_type,
    (SELECT COUNT(*) FROM information_schema.columns c WHERE c.table_name = t.table_name) as column_count
FROM information_schema.tables t
WHERE table_schema = 'public'
AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- ============================================
-- PART 2: Complete Column Details for ALL Tables
-- ============================================
SELECT
    'COLUMNS' as section,
    table_name,
    ordinal_position,
    column_name,
    data_type,
    character_maximum_length,
    is_nullable,
    column_default,
    udt_name
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name IN (
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = 'public'
    AND table_type = 'BASE TABLE'
)
ORDER BY table_name, ordinal_position;

-- ============================================
-- PART 3: Primary Keys
-- ============================================
SELECT
    'PRIMARY_KEYS' as section,
    tc.table_name,
    kcu.column_name,
    tc.constraint_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
WHERE tc.constraint_type = 'PRIMARY KEY'
AND tc.table_schema = 'public'
ORDER BY tc.table_name;

-- ============================================
-- PART 4: Foreign Keys
-- ============================================
SELECT
    'FOREIGN_KEYS' as section,
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name,
    rc.update_rule,
    rc.delete_rule,
    tc.constraint_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
JOIN information_schema.referential_constraints AS rc
    ON rc.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
AND tc.table_schema = 'public'
ORDER BY tc.table_name, kcu.column_name;

-- ============================================
-- PART 5: Indexes
-- ============================================
SELECT
    'INDEXES' as section,
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;

-- ============================================
-- PART 6: Unique Constraints
-- ============================================
SELECT
    'UNIQUE_CONSTRAINTS' as section,
    tc.table_name,
    tc.constraint_name,
    kcu.column_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
WHERE tc.constraint_type = 'UNIQUE'
AND tc.table_schema = 'public'
ORDER BY tc.table_name, tc.constraint_name;

-- ============================================
-- PART 7: Check Constraints
-- ============================================
SELECT
    'CHECK_CONSTRAINTS' as section,
    tc.table_name,
    tc.constraint_name,
    cc.check_clause
FROM information_schema.table_constraints tc
JOIN information_schema.check_constraints cc
    ON tc.constraint_name = cc.constraint_name
WHERE tc.constraint_type = 'CHECK'
AND tc.table_schema = 'public'
ORDER BY tc.table_name;

-- ============================================
-- PART 8: Row Level Security (RLS) Status
-- ============================================
SELECT
    'RLS_STATUS' as section,
    schemaname,
    tablename,
    rowsecurity as rls_enabled,
    (SELECT COUNT(*)
     FROM pg_policies
     WHERE schemaname = 'public'
     AND tablename = pg_tables.tablename) as policy_count
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;

-- ============================================
-- PART 9: RLS Policies Detail
-- ============================================
SELECT
    'RLS_POLICIES' as section,
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd as command,
    qual as using_expression,
    with_check as with_check_expression
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, policyname;

-- ============================================
-- PART 10: Triggers
-- ============================================
SELECT
    'TRIGGERS' as section,
    trigger_schema,
    trigger_name,
    event_manipulation,
    event_object_table,
    action_statement,
    action_timing
FROM information_schema.triggers
WHERE trigger_schema = 'public'
ORDER BY event_object_table, trigger_name;

-- ============================================
-- PART 11: Table Sizes (Data Volume)
-- ============================================
SELECT
    'TABLE_SIZES' as section,
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) AS indexes_size,
    (SELECT COUNT(*) FROM information_schema.columns c WHERE c.table_name = tablename) as column_count
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- ============================================
-- PART 12: Row Counts (Approximate)
-- ============================================
SELECT
    'ROW_COUNTS' as section,
    schemaname,
    relname as tablename,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    n_live_tup as live_rows,
    n_dead_tup as dead_rows,
    last_vacuum,
    last_autovacuum
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY n_live_tup DESC;

-- ============================================
-- PART 13: Sequences (Auto-increment counters)
-- ============================================
SELECT
    'SEQUENCES' as section,
    sequence_schema,
    sequence_name,
    data_type,
    start_value,
    minimum_value,
    maximum_value,
    increment,
    cycle_option
FROM information_schema.sequences
WHERE sequence_schema = 'public'
ORDER BY sequence_name;

-- ============================================
-- PART 14: Views (if any)
-- ============================================
SELECT
    'VIEWS' as section,
    table_schema,
    table_name as view_name,
    view_definition
FROM information_schema.views
WHERE table_schema = 'public'
ORDER BY table_name;

-- ============================================
-- PART 15: Functions/Procedures
-- ============================================
SELECT
    'FUNCTIONS' as section,
    n.nspname as schema_name,
    p.proname as function_name,
    pg_get_function_arguments(p.oid) as arguments,
    pg_get_function_result(p.oid) as result_type,
    CASE p.prokind
        WHEN 'f' THEN 'FUNCTION'
        WHEN 'p' THEN 'PROCEDURE'
        WHEN 'a' THEN 'AGGREGATE'
        WHEN 'w' THEN 'WINDOW'
    END as function_type,
    l.lanname as language
FROM pg_proc p
LEFT JOIN pg_namespace n ON p.pronamespace = n.oid
LEFT JOIN pg_language l ON p.prolang = l.oid
WHERE n.nspname = 'public'
ORDER BY p.proname;

-- ============================================
-- PART 16: Database Extensions
-- ============================================
SELECT
    'EXTENSIONS' as section,
    extname as extension_name,
    extversion as version,
    extrelocatable as relocatable,
    extnamespace::regnamespace as schema
FROM pg_extension
ORDER BY extname;

-- ============================================
-- PART 17: Specific Check - Version Columns
-- ============================================
SELECT
    'VERSION_COLUMNS' as section,
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default,
    CASE WHEN column_name = 'version' THEN '✅ HAS VERSION' ELSE '❌ NO VERSION' END as status
FROM information_schema.columns
WHERE table_schema = 'public'
AND (column_name = 'version' OR table_name IN ('users', 'projects', 'tasks', 'chat_rooms', 'messages', 'project_members'))
ORDER BY table_name, column_name;

-- ============================================
-- PART 18: Critical Missing Columns Check
-- ============================================
SELECT
    'CRITICAL_CHECKS' as section,
    'users' as table_name,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'version'
    ) THEN '✅ HAS version' ELSE '❌ MISSING version' END as version_field,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'username' AND is_nullable = 'NO'
    ) THEN '✅ username NOT NULL' ELSE '❌ username NULL' END as username_constraint

UNION ALL

SELECT
    'CRITICAL_CHECKS',
    'projects' as table_name,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects' AND column_name = 'version'
    ) THEN '✅ HAS version' ELSE '❌ MISSING version' END,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects' AND column_name = 'category'
    ) THEN '✅ HAS category' ELSE '❌ MISSING category' END

UNION ALL

SELECT
    'CRITICAL_CHECKS',
    'tasks' as table_name,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tasks' AND column_name = 'version'
    ) THEN '✅ HAS version' ELSE '❌ MISSING version' END,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tasks' AND column_name = 'comments'
    ) THEN '✅ HAS comments' ELSE '❌ MISSING comments' END

UNION ALL

SELECT
    'CRITICAL_CHECKS',
    'messages' as table_name,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'messages' AND column_name = 'sender_name'
    ) THEN '✅ HAS sender_name' ELSE '❌ MISSING sender_name' END,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'messages' AND column_name = 'reactions'
    ) THEN '✅ HAS reactions' ELSE '❌ MISSING reactions' END

UNION ALL

SELECT
    'CRITICAL_CHECKS',
    'chat_rooms' as table_name,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'chat_rooms' AND column_name = 'participant_ids'
    ) THEN '✅ HAS participant_ids' ELSE '❌ MISSING participant_ids' END,
    CASE WHEN EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'chat_rooms' AND column_name = 'is_archived'
    ) THEN '✅ HAS is_archived' ELSE '❌ MISSING is_archived' END;

-- ============================================
-- PART 19: Project Wizard Fields Check
-- ============================================
SELECT
    'PROJECT_WIZARD_FIELDS' as section,
    column_name,
    data_type,
    is_nullable,
    column_default,
    CASE
        WHEN column_name IN (
            'category', 'deadline', 'website_url', 'github_url',
            'project_motive', 'tech_stack', 'tags', 'business_model',
            'target_audience', 'industry_tags', 'open_source_license'
        ) THEN '✅ WIZARD FIELD'
        ELSE 'CORE FIELD'
    END as field_type
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'projects'
ORDER BY ordinal_position;

-- ============================================
-- PART 20: Metadata Caching Fields Check
-- ============================================
SELECT
    'METADATA_CACHING_FIELDS' as section,
    column_name,
    data_type,
    is_nullable,
    column_default,
    CASE
        WHEN column_name IN (
            'member_count', 'chat_count', 'task_count',
            'completed_task_count', 'pending_task_count', 'last_activity_at'
        ) THEN '✅ METADATA FIELD'
        ELSE 'OTHER FIELD'
    END as field_type
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'projects'
AND column_name IN (
    'member_count', 'chat_count', 'task_count',
    'completed_task_count', 'pending_task_count', 'last_activity_at',
    'id', 'name', 'description'  -- Include some core fields for context
)
ORDER BY
    CASE
        WHEN column_name IN ('member_count', 'chat_count', 'task_count',
            'completed_task_count', 'pending_task_count', 'last_activity_at')
        THEN 1 ELSE 2
    END,
    ordinal_position;

-- ============================================
-- SUMMARY STATISTICS
-- ============================================
SELECT
    'SUMMARY' as section,
    'Total Tables' as metric,
    COUNT(*)::text as value
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_type = 'BASE TABLE'

UNION ALL

SELECT
    'SUMMARY',
    'Total Columns',
    COUNT(*)::text
FROM information_schema.columns
WHERE table_schema = 'public'

UNION ALL

SELECT
    'SUMMARY',
    'Total Indexes',
    COUNT(*)::text
FROM pg_indexes
WHERE schemaname = 'public'

UNION ALL

SELECT
    'SUMMARY',
    'Total Foreign Keys',
    COUNT(*)::text
FROM information_schema.table_constraints
WHERE constraint_type = 'FOREIGN KEY'
AND table_schema = 'public'

UNION ALL

SELECT
    'SUMMARY',
    'Total RLS Policies',
    COUNT(*)::text
FROM pg_policies
WHERE schemaname = 'public'

UNION ALL

SELECT
    'SUMMARY',
    'Total Triggers',
    COUNT(*)::text
FROM information_schema.triggers
WHERE trigger_schema = 'public';

-- ============================================
-- END OF SCHEMA EXPORT
-- ============================================
-- Copy ALL results from ALL 20 parts above
-- Save to a text file and share with Claude
-- ============================================
