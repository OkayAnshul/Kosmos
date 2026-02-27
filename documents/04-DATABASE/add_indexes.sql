-- ============================================================================
-- Kosmos: Add Performance Indexes
-- ============================================================================
-- Purpose: Add missing indexes for common query patterns
-- Issue: Queries without indexes cause slow performance at scale
-- Impact: App slowdown with 1000+ tasks, messages, or projects
--
-- Run: Execute in Supabase SQL Editor
-- Date: 2026-01-23
-- ============================================================================

-- ============================================================================
-- TASKS TABLE: Add indexes for common queries
-- ============================================================================

-- Index 1: Tasks by project (most common query)
CREATE INDEX IF NOT EXISTS idx_tasks_project_id
ON tasks(project_id);

-- Index 2: Tasks by assignee
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to_id
ON tasks(assigned_to_id);

-- Index 3: Tasks by creator
CREATE INDEX IF NOT EXISTS idx_tasks_created_by_id
ON tasks(created_by_id);

-- Index 4: Tasks by status (for filtering)
CREATE INDEX IF NOT EXISTS idx_tasks_status
ON tasks(status);

-- Index 5: Tasks by priority (for sorting)
CREATE INDEX IF NOT EXISTS idx_tasks_priority
ON tasks(priority);

-- Index 6: Tasks by due date (for deadlines)
CREATE INDEX IF NOT EXISTS idx_tasks_due_date
ON tasks(due_date);

-- Index 7: Tasks by creation date (for sorting)
CREATE INDEX IF NOT EXISTS idx_tasks_created_at
ON tasks(created_at DESC);

-- Index 8: Composite index for project + status (common filter)
CREATE INDEX IF NOT EXISTS idx_tasks_project_status
ON tasks(project_id, status);

-- Index 9: Composite index for project + assignee (common filter)
CREATE INDEX IF NOT EXISTS idx_tasks_project_assignee
ON tasks(project_id, assigned_to_id);

-- ============================================================================
-- MESSAGES TABLE: Add indexes for common queries
-- ============================================================================

-- Index 1: Messages by chat room (most common query)
CREATE INDEX IF NOT EXISTS idx_messages_chat_room_id
ON messages(chat_room_id);

-- Index 2: Messages by user
CREATE INDEX IF NOT EXISTS idx_messages_user_id
ON messages(user_id);

-- Index 3: Messages by timestamp (for ordering)
CREATE INDEX IF NOT EXISTS idx_messages_timestamp
ON messages(timestamp DESC);

-- Index 4: Composite index for chat room + timestamp (common pagination)
CREATE INDEX IF NOT EXISTS idx_messages_chat_timestamp
ON messages(chat_room_id, timestamp DESC);

-- Index 5: Messages search (if using full-text search)
-- Note: Only add if content search is implemented
-- CREATE INDEX IF NOT EXISTS idx_messages_content_search
-- ON messages USING GIN(to_tsvector('english', content));

-- ============================================================================
-- PROJECTS TABLE: Add indexes for common queries
-- ============================================================================

-- Index 1: Projects by owner
CREATE INDEX IF NOT EXISTS idx_projects_owner_id
ON projects(owner_id);

-- Index 2: Projects by status
CREATE INDEX IF NOT EXISTS idx_projects_status
ON projects(status);

-- Index 3: Projects by creation date (for sorting)
CREATE INDEX IF NOT EXISTS idx_projects_created_at
ON projects(created_at DESC);

-- Index 4: Projects by name (for search)
CREATE INDEX IF NOT EXISTS idx_projects_name
ON projects(name);

-- ============================================================================
-- PROJECT_MEMBERS TABLE: Add indexes for common queries
-- ============================================================================

-- Index 1: Members by project (most common query)
CREATE INDEX IF NOT EXISTS idx_project_members_project_id
ON project_members(project_id);

-- Index 2: Projects by user (reverse lookup)
CREATE INDEX IF NOT EXISTS idx_project_members_user_id
ON project_members(user_id);

-- Index 3: Members by role (for filtering)
CREATE INDEX IF NOT EXISTS idx_project_members_role
ON project_members(role);

-- Index 4: Composite index for project + user (uniqueness check)
CREATE UNIQUE INDEX IF NOT EXISTS idx_project_members_project_user
ON project_members(project_id, user_id);

-- ============================================================================
-- CHAT_ROOMS TABLE: Add indexes for common queries
-- ============================================================================

-- Index 1: Chat rooms by project
CREATE INDEX IF NOT EXISTS idx_chat_rooms_project_id
ON chat_rooms(project_id)
WHERE project_id IS NOT NULL;

-- Index 2: Chat rooms by type
CREATE INDEX IF NOT EXISTS idx_chat_rooms_type
ON chat_rooms(type);

-- Index 3: Chat rooms by creation date
CREATE INDEX IF NOT EXISTS idx_chat_rooms_created_at
ON chat_rooms(created_at DESC);

-- Index 4: DM chat lookup (for finding existing DM)
-- Note: Uses GIN index for JSONB metadata
CREATE INDEX IF NOT EXISTS idx_chat_rooms_metadata
ON chat_rooms USING GIN(metadata);

-- ============================================================================
-- USERS TABLE: Add indexes for common queries
-- ============================================================================

-- Index 1: Users by email (login lookup)
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email
ON users(email);

-- Index 2: Users by full name (search)
CREATE INDEX IF NOT EXISTS idx_users_full_name
ON users(full_name);

-- Index 3: Users by creation date
CREATE INDEX IF NOT EXISTS idx_users_created_at
ON users(created_at DESC);

-- Index 4: Full-text search on name + email (advanced search)
-- CREATE INDEX IF NOT EXISTS idx_users_search
-- ON users USING GIN(
--     to_tsvector('english', COALESCE(full_name, '') || ' ' || COALESCE(email, ''))
-- );

-- ============================================================================
-- TASK_ACTIVITY TABLE: Add indexes (if table exists)
-- ============================================================================

-- Note: These indexes were already added in create_task_activity_table.sql
-- Including here for completeness

CREATE INDEX IF NOT EXISTS idx_task_activity_task_id
ON task_activity(task_id);

CREATE INDEX IF NOT EXISTS idx_task_activity_user_id
ON task_activity(user_id);

CREATE INDEX IF NOT EXISTS idx_task_activity_timestamp
ON task_activity(timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_task_activity_action
ON task_activity(action);

-- ============================================================================
-- Verification Queries (Run after adding indexes)
-- ============================================================================

-- 1. List all indexes on tasks table
-- SELECT
--     indexname,
--     indexdef
-- FROM pg_indexes
-- WHERE tablename = 'tasks'
-- ORDER BY indexname;

-- 2. Check index sizes (identify large indexes)
-- SELECT
--     schemaname,
--     tablename,
--     indexname,
--     pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'public'
-- ORDER BY pg_relation_size(indexrelid) DESC;

-- 3. Check index usage statistics
-- SELECT
--     schemaname,
--     tablename,
--     indexname,
--     idx_scan AS index_scans,
--     idx_tup_read AS tuples_read,
--     idx_tup_fetch AS tuples_fetched
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'public'
-- ORDER BY idx_scan DESC;

-- 4. Find missing indexes (queries without index usage)
-- SELECT
--     schemaname,
--     tablename,
--     seq_scan,
--     seq_tup_read,
--     idx_scan,
--     seq_tup_read / seq_scan AS avg_rows_per_scan
-- FROM pg_stat_user_tables
-- WHERE schemaname = 'public'
--   AND seq_scan > 0
-- ORDER BY seq_tup_read DESC;

-- ============================================================================
-- Performance Testing Queries
-- ============================================================================

-- Test 1: Tasks by project (should use idx_tasks_project_id)
-- EXPLAIN ANALYZE
-- SELECT * FROM tasks WHERE project_id = '00000000-0000-0000-0000-000000000001';

-- Test 2: Messages in chat room (should use idx_messages_chat_timestamp)
-- EXPLAIN ANALYZE
-- SELECT * FROM messages
-- WHERE chat_room_id = '00000000-0000-0000-0000-000000000001'
-- ORDER BY timestamp DESC
-- LIMIT 50;

-- Test 3: Tasks by assignee + status (should use indexes)
-- EXPLAIN ANALYZE
-- SELECT * FROM tasks
-- WHERE assigned_to_id = auth.uid()
--   AND status = 'in_progress';

-- Test 4: Project members lookup (should use idx_project_members_project_user)
-- EXPLAIN ANALYZE
-- SELECT * FROM project_members
-- WHERE project_id = '00000000-0000-0000-0000-000000000001'
--   AND user_id = auth.uid();

-- ============================================================================
-- Index Maintenance (Run periodically)
-- ============================================================================

-- 1. Reindex all tables (fixes index bloat)
-- REINDEX DATABASE postgres;

-- 2. Vacuum and analyze (updates statistics)
-- VACUUM ANALYZE tasks;
-- VACUUM ANALYZE messages;
-- VACUUM ANALYZE projects;
-- VACUUM ANALYZE project_members;

-- 3. Check for unused indexes (consider dropping)
-- SELECT
--     schemaname,
--     tablename,
--     indexname,
--     pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'public'
--   AND idx_scan = 0
--   AND indexrelid NOT IN (
--       SELECT indexrelid FROM pg_index WHERE indisunique OR indisprimary
--   )
-- ORDER BY pg_relation_size(indexrelid) DESC;

-- ============================================================================
-- Rollback (if needed)
-- ============================================================================

-- To drop all indexes (WARNING: This will hurt performance!)
-- DROP INDEX IF EXISTS idx_tasks_project_id CASCADE;
-- DROP INDEX IF EXISTS idx_tasks_assigned_to_id CASCADE;
-- DROP INDEX IF EXISTS idx_tasks_created_by_id CASCADE;
-- ... etc (drop all indexes listed above)

-- ============================================================================
-- Migration Notes
-- ============================================================================
--
-- After running this script:
-- 1. Run verification queries to confirm indexes exist
-- 2. Test query performance with EXPLAIN ANALYZE
-- 3. Monitor index usage with pg_stat_user_indexes
-- 4. Check index sizes to ensure no excessive bloat
-- 5. Run VACUUM ANALYZE on large tables
--
-- Expected Performance Improvements:
-- - Task queries: 10-100x faster (depends on dataset size)
-- - Message pagination: 50-500x faster
-- - Project member lookups: 5-50x faster
-- - User search: 20-100x faster
--
-- When to Reindex:
-- - After bulk inserts/updates (>10k rows)
-- - Monthly maintenance schedule
-- - If queries become slow despite indexes
-- - After major database cleanup
--
-- Related Files:
-- - app/src/main/java/com/example/kosmos/core/database/dao/TaskDao.kt
-- - app/src/main/java/com/example/kosmos/core/database/dao/MessageDao.kt
-- - app/src/main/java/com/example/kosmos/core/database/dao/ProjectDao.kt
--
-- ============================================================================
