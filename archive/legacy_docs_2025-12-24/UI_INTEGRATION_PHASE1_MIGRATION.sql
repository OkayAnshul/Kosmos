-- ============================================
-- UI INTEGRATION PHASE 1: Database Migration
-- ============================================
-- Project: Kosmos Android App
-- Date: 2025-11-02
-- Purpose: Add archive and pin support for UI redesign integration
-- Affects: chat_rooms, projects tables
-- ============================================

-- ============================================
-- STEP 1: ADD ARCHIVE AND PIN SUPPORT TO CHAT_ROOMS
-- ============================================

-- Add is_pinned column for pinning chats to top of list
ALTER TABLE chat_rooms
ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT false;

-- Add is_archived column for archiving chats
ALTER TABLE chat_rooms
ADD COLUMN IF NOT EXISTS is_archived BOOLEAN DEFAULT false;

-- Add indexes for efficient filtering
CREATE INDEX IF NOT EXISTS idx_chat_rooms_pinned
ON chat_rooms(is_pinned)
WHERE is_pinned = true;

CREATE INDEX IF NOT EXISTS idx_chat_rooms_archived
ON chat_rooms(is_archived);

-- Verify chat_rooms columns added
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'chat_rooms'
AND column_name IN ('is_pinned', 'is_archived')
ORDER BY column_name;

-- ============================================
-- STEP 2: ADD ARCHIVE SUPPORT TO PROJECTS
-- ============================================

-- Add is_archived column for archiving projects
ALTER TABLE projects
ADD COLUMN IF NOT EXISTS is_archived BOOLEAN DEFAULT false;

-- Add index for efficient filtering
CREATE INDEX IF NOT EXISTS idx_projects_archived
ON projects(is_archived);

-- Verify projects column added
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'projects'
AND column_name = 'is_archived';

-- ============================================
-- STEP 3: RELOAD POSTGREST SCHEMA CACHE
-- ============================================
-- This is CRITICAL - PostgREST must reload its schema cache
-- Otherwise new columns won't be available via API

NOTIFY pgrst, 'reload schema';

-- ============================================
-- STEP 4: VERIFICATION
-- ============================================

-- Verify all chat rooms have default values
SELECT
    COUNT(*) as total_chat_rooms,
    COUNT(CASE WHEN is_pinned = false THEN 1 END) as unpinned_count,
    COUNT(CASE WHEN is_pinned = true THEN 1 END) as pinned_count,
    COUNT(CASE WHEN is_archived = false THEN 1 END) as active_count,
    COUNT(CASE WHEN is_archived = true THEN 1 END) as archived_count
FROM chat_rooms;

-- Verify all projects have default values
SELECT
    COUNT(*) as total_projects,
    COUNT(CASE WHEN is_archived = false THEN 1 END) as active_count,
    COUNT(CASE WHEN is_archived = true THEN 1 END) as archived_count
FROM projects;

-- Check indexes were created
SELECT
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
AND tablename IN ('chat_rooms', 'projects')
AND indexname IN ('idx_chat_rooms_pinned', 'idx_chat_rooms_archived', 'idx_projects_archived')
ORDER BY tablename, indexname;

-- ============================================
-- SUCCESS CRITERIA
-- ============================================
-- After running this script, you should see:
-- ✅ chat_rooms table has is_pinned and is_archived columns
-- ✅ projects table has is_archived column
-- ✅ All indexes created successfully
-- ✅ All existing rows have default value false
-- ✅ PostgREST schema cache reloaded

-- ============================================
-- TESTING QUERIES
-- ============================================

-- Test pinning a chat
/*
UPDATE chat_rooms
SET is_pinned = true
WHERE id = 'YOUR_CHAT_ROOM_ID';
*/

-- Test archiving a chat
/*
UPDATE chat_rooms
SET is_archived = true
WHERE id = 'YOUR_CHAT_ROOM_ID';
*/

-- Test archiving a project
/*
UPDATE projects
SET is_archived = true
WHERE id = 'YOUR_PROJECT_ID';
*/

-- Query pinned chats
/*
SELECT id, name, is_pinned, is_archived
FROM chat_rooms
WHERE is_pinned = true
ORDER BY created_at DESC;
*/

-- Query active (non-archived) chats
/*
SELECT id, name, is_pinned, is_archived
FROM chat_rooms
WHERE is_archived = false
ORDER BY
    is_pinned DESC,  -- Pinned first
    created_at DESC;
*/

-- Query archived chats
/*
SELECT id, name, is_archived
FROM chat_rooms
WHERE is_archived = true
ORDER BY created_at DESC;
*/

-- Query active projects
/*
SELECT id, name, is_archived
FROM projects
WHERE is_archived = false
ORDER BY created_at DESC;
*/

-- ============================================
-- ROLLBACK PLAN (if something goes wrong)
-- ============================================
-- To remove added columns (DANGEROUS - will lose pin/archive data):
/*
ALTER TABLE chat_rooms DROP COLUMN IF EXISTS is_pinned;
ALTER TABLE chat_rooms DROP COLUMN IF EXISTS is_archived;
ALTER TABLE projects DROP COLUMN IF EXISTS is_archived;
DROP INDEX IF EXISTS idx_chat_rooms_pinned;
DROP INDEX IF EXISTS idx_chat_rooms_archived;
DROP INDEX IF EXISTS idx_projects_archived;
NOTIFY pgrst, 'reload schema';
*/

-- ============================================
-- END OF MIGRATION SCRIPT
-- ============================================
