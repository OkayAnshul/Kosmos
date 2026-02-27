-- P0-01 FIX: Add version field to users table for optimistic locking
-- This prevents race conditions when updating user profiles from multiple devices
--
-- Run this migration in Supabase SQL Editor
-- Date: 2026-01-24
-- Phase 0: Critical Data Integrity

-- Add version field to users table
ALTER TABLE public.users
ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;

-- Create index on version for faster conflict detection
CREATE INDEX IF NOT EXISTS idx_users_version
ON public.users(version);

-- Add comment explaining the field
COMMENT ON COLUMN public.users.version IS
'Version number for optimistic locking. Incremented on each update to detect concurrent modifications.';

-- Verify the migration
SELECT
    column_name,
    data_type,
    column_default,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'users'
  AND column_name = 'version';
