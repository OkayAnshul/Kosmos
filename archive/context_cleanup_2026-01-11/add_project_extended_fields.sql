-- ============================================
-- PROJECT CREATION WIZARD - DATABASE MIGRATION
-- ============================================
-- Feature: Multi-Step Project Creation Wizard
-- Date: 2026-01-06
-- Purpose: Add extended fields to projects table for category-based project creation
-- ============================================

-- ============================================
-- STEP 1: ADD NEW COLUMNS TO PROJECTS TABLE
-- ============================================

ALTER TABLE projects
ADD COLUMN IF NOT EXISTS category TEXT DEFAULT 'other',
ADD COLUMN IF NOT EXISTS deadline BIGINT,
ADD COLUMN IF NOT EXISTS website_url TEXT,
ADD COLUMN IF NOT EXISTS github_url TEXT,
ADD COLUMN IF NOT EXISTS project_motive TEXT,
ADD COLUMN IF NOT EXISTS tech_stack JSONB DEFAULT '[]'::jsonb,
ADD COLUMN IF NOT EXISTS tags TEXT[] DEFAULT '{}',
ADD COLUMN IF NOT EXISTS business_model TEXT,
ADD COLUMN IF NOT EXISTS target_audience TEXT,
ADD COLUMN IF NOT EXISTS industry_tags TEXT[] DEFAULT '{}',
ADD COLUMN IF NOT EXISTS open_source_license TEXT;

-- ============================================
-- STEP 2: ADD CHECK CONSTRAINT FOR CATEGORY
-- ============================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'check_project_category'
    ) THEN
        ALTER TABLE projects
        ADD CONSTRAINT check_project_category
        CHECK (category IN ('tech', 'social', 'business', 'other'));
    END IF;
END $$;

-- ============================================
-- STEP 3: CREATE INDEXES FOR PERFORMANCE
-- ============================================

-- Index on category for filtering projects by type
CREATE INDEX IF NOT EXISTS idx_projects_category
ON projects(category);

-- Index on deadline for sorting/filtering by due date
CREATE INDEX IF NOT EXISTS idx_projects_deadline
ON projects(deadline)
WHERE deadline IS NOT NULL;

-- GIN index on tags for efficient array searches
CREATE INDEX IF NOT EXISTS idx_projects_tags
ON projects USING GIN(tags);

-- GIN index on industry_tags for efficient array searches
CREATE INDEX IF NOT EXISTS idx_projects_industry_tags
ON projects USING GIN(industry_tags);

-- Index on tech_stack for JSON searches
CREATE INDEX IF NOT EXISTS idx_projects_tech_stack
ON projects USING GIN(tech_stack);

-- ============================================
-- STEP 4: VERIFY MIGRATION
-- ============================================

-- Check all new columns exist
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'projects'
AND column_name IN (
    'category',
    'deadline',
    'website_url',
    'github_url',
    'project_motive',
    'tech_stack',
    'tags',
    'business_model',
    'target_audience',
    'industry_tags',
    'open_source_license'
)
ORDER BY column_name;

-- Verify indexes created
SELECT
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'projects'
AND indexname LIKE 'idx_projects_%'
ORDER BY indexname;

-- Check constraint exists
SELECT
    conname,
    pg_get_constraintdef(oid) as constraint_definition
FROM pg_constraint
WHERE conname = 'check_project_category';

-- ============================================
-- STEP 5: RELOAD POSTGREST SCHEMA CACHE
-- ============================================
-- This is CRITICAL - PostgREST must reload its schema cache
-- Otherwise API requests will fail with PGRST204 errors

NOTIFY pgrst, 'reload schema';

-- ============================================
-- MIGRATION COMPLETE
-- ============================================

-- Expected column count: 22 (11 original + 11 new)
SELECT
    COUNT(*) as total_columns,
    CASE
        WHEN COUNT(*) >= 22 THEN '✅ MIGRATION SUCCESSFUL'
        ELSE '❌ MIGRATION INCOMPLETE'
    END as status
FROM information_schema.columns
WHERE table_name = 'projects';

-- ============================================
-- ROLLBACK SCRIPT (EMERGENCY USE ONLY)
-- ============================================
-- Uncomment and run if you need to revert this migration

/*
-- WARNING: This will DELETE all data in the new columns
-- Make sure you have a backup before running this!

ALTER TABLE projects
DROP COLUMN IF EXISTS category,
DROP COLUMN IF EXISTS deadline,
DROP COLUMN IF EXISTS website_url,
DROP COLUMN IF EXISTS github_url,
DROP COLUMN IF EXISTS project_motive,
DROP COLUMN IF EXISTS tech_stack,
DROP COLUMN IF EXISTS tags,
DROP COLUMN IF EXISTS business_model,
DROP COLUMN IF EXISTS target_audience,
DROP COLUMN IF EXISTS industry_tags,
DROP COLUMN IF EXISTS open_source_license;

DROP INDEX IF EXISTS idx_projects_category;
DROP INDEX IF EXISTS idx_projects_deadline;
DROP INDEX IF EXISTS idx_projects_tags;
DROP INDEX IF EXISTS idx_projects_industry_tags;
DROP INDEX IF EXISTS idx_projects_tech_stack;

ALTER TABLE projects DROP CONSTRAINT IF EXISTS check_project_category;

NOTIFY pgrst, 'reload schema';
*/

-- ============================================
-- USAGE NOTES
-- ============================================

-- Category values:
--   'tech'     - Technology/Software projects (has github_url, tech_stack, open_source_license)
--   'social'   - Social/Community projects (has project_motive, target_audience)
--   'business' - Business projects (has website_url, business_model, industry_tags)
--   'other'    - General projects (has project_motive, tags)

-- Tech Stack (JSONB array example):
--   ["Kotlin", "Android", "Jetpack Compose", "Supabase"]

-- Tags (TEXT array example):
--   {"project-management", "collaboration", "mobile-app"}

-- Industry Tags (TEXT array example):
--   {"fintech", "healthcare", "education"}

-- Deadline (BIGINT timestamp):
--   Unix timestamp in milliseconds (e.g., 1704153600000 for 2024-01-02)

-- ============================================
