-- ============================================================================
-- UI Integration Phase 2 - Database Migration
-- Date: November 2, 2025
-- Purpose: Add performance indexes for project stats calculation
-- ============================================================================

-- Performance indexes for stats queries
-- These indexes optimize the count queries used in ProjectStats calculation

-- Index for project members lookup (used in member count)
CREATE INDEX IF NOT EXISTS idx_project_members_project
ON project_members(project_id);

-- Index for chat rooms lookup by project (used in chat count)
CREATE INDEX IF NOT EXISTS idx_chat_rooms_project
ON chat_rooms(project_id);

-- Index for tasks lookup by project (used in task count)
CREATE INDEX IF NOT EXISTS idx_tasks_project
ON tasks(project_id);

-- Index for task status filtering (used in completed/pending counts)
CREATE INDEX IF NOT EXISTS idx_tasks_status
ON tasks(status);

-- Index for tasks lookup by assignee (used in MyTasks cross-project view)
CREATE INDEX IF NOT EXISTS idx_tasks_assignee
ON tasks(assigned_to_id);

-- Composite index for task status + project (optimal for project task counts by status)
CREATE INDEX IF NOT EXISTS idx_tasks_project_status
ON tasks(project_id, status);

-- ============================================================================
-- Verification Queries
-- ============================================================================
-- Run these to verify indexes were created:
-- SELECT tablename, indexname, indexdef FROM pg_indexes
-- WHERE schemaname = 'public' AND tablename IN ('project_members', 'chat_rooms', 'tasks')
-- ORDER BY tablename, indexname;
-- ============================================================================
