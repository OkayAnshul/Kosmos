-- ============================================================================
-- ADD PROJECT WIZARD FIELDS MIGRATION
-- ============================================================================
-- Purpose: Add missing wizard fields to Supabase projects table
-- Date: 2026-01-07
-- Issue: Project creation fails with PGRST204 error - columns don't exist
-- Error: "Could not find the 'business_model' column of 'projects' in the schema cache"
-- ============================================================================

-- These fields were added to the Room database but never added to Supabase
-- This causes project creation to fail when syncing to Supabase

-- Step 1: Add category column (enum type)
-- First, check if the enum type exists, if not create it
DO $$ BEGIN
    CREATE TYPE project_category AS ENUM ('TECH', 'SOCIAL', 'BUSINESS', 'OTHER');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

ALTER TABLE projects
ADD COLUMN IF NOT EXISTS category project_category DEFAULT 'OTHER';

-- Step 2: Add deadline column
ALTER TABLE projects
ADD COLUMN IF NOT EXISTS deadline BIGINT;

-- Step 3: Add website_url column
ALTER TABLE projects
ADD COLUMN IF NOT EXISTS website_url TEXT;

-- Step 4: Add github_url column
ALTER TABLE projects
ADD COLUMN IF NOT EXISTS github_url TEXT;

-- Step 5: Add project_motive column
ALTER TABLE projects
ADD COLUMN IF NOT EXISTS project_motive TEXT;

-- Step 6: Add tech_stack column (stored as JSON string)
ALTER TABLE projects
ADD COLUMN IF NOT EXISTS tech_stack TEXT;

-- Step 7: Add tags column (stored as JSON string)
ALTER TABLE projects
ADD COLUMN IF NOT EXISTS tags TEXT;

-- Step 8: Add business_model column
ALTER TABLE projects
ADD COLUMN IF NOT EXISTS business_model TEXT;

-- Step 9: Add target_audience column
ALTER TABLE projects
ADD COLUMN IF NOT EXISTS target_audience TEXT;

-- Step 10: Add industry_tags column (stored as JSON string)
ALTER TABLE projects
ADD COLUMN IF NOT EXISTS industry_tags TEXT;

-- Step 11: Add open_source_license column
ALTER TABLE projects
ADD COLUMN IF NOT EXISTS open_source_license TEXT;

-- ============================================================================
-- VERIFICATION
-- ============================================================================

-- Check that all columns were added successfully
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
ORDER BY ordinal_position;

-- Show sample of updated table structure
SELECT
    id,
    name,
    category,
    deadline,
    website_url,
    github_url,
    business_model
FROM projects
LIMIT 5;

-- ============================================================================
-- NOTES
-- ============================================================================
--
-- These columns match the fields in:
-- - ProjectCreationData (ProjectRepository.kt lines 51-69)
-- - Project entity (Project.kt)
--
-- Field Mapping:
-- - category: ProjectCategory enum (TECH, SOCIAL, BUSINESS, OTHER)
-- - deadline: Optional timestamp (milliseconds)
-- - website_url: Optional URL (mainly for BUSINESS projects)
-- - github_url: Optional GitHub repo URL (mainly for TECH projects)
-- - project_motive: Optional goals/motive (mainly for SOCIAL/OTHER)
-- - tech_stack: JSON array of technologies (mainly for TECH)
-- - tags: JSON array of general tags
-- - business_model: Optional business model description (mainly for BUSINESS)
-- - target_audience: Optional target audience (mainly for SOCIAL)
-- - industry_tags: JSON array of industry tags (mainly for BUSINESS)
-- - open_source_license: Optional OSS license (mainly for TECH)
--
-- ============================================================================
-- EXECUTION INSTRUCTIONS
-- ============================================================================
-- 1. Open Supabase Dashboard → SQL Editor
-- 2. Paste this entire script
-- 3. Run the script
-- 4. Verify all columns added successfully (check VERIFICATION section output)
-- 5. Test project creation in app - should now sync to Supabase
-- ============================================================================

-- ============================================================================
-- ROLLBACK (if needed)
-- ============================================================================
-- If you need to rollback these changes:
--
-- ALTER TABLE projects DROP COLUMN IF EXISTS category;
-- ALTER TABLE projects DROP COLUMN IF EXISTS deadline;
-- ALTER TABLE projects DROP COLUMN IF EXISTS website_url;
-- ALTER TABLE projects DROP COLUMN IF EXISTS github_url;
-- ALTER TABLE projects DROP COLUMN IF EXISTS project_motive;
-- ALTER TABLE projects DROP COLUMN IF EXISTS tech_stack;
-- ALTER TABLE projects DROP COLUMN IF EXISTS tags;
-- ALTER TABLE projects DROP COLUMN IF EXISTS business_model;
-- ALTER TABLE projects DROP COLUMN IF EXISTS target_audience;
-- ALTER TABLE projects DROP COLUMN IF EXISTS industry_tags;
-- ALTER TABLE projects DROP COLUMN IF EXISTS open_source_license;
-- DROP TYPE IF EXISTS project_category;
-- ============================================================================
