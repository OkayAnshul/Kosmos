-- ============================================
-- Supabase Migration: Add Missing User Fields
-- ============================================
-- Project: Kosmos Android App
-- Date: 2025-10-31
-- Purpose: Fix user registration and search issues
-- ============================================

-- Add missing username column (CRITICAL)
-- This column is required for @username lookups
ALTER TABLE users
ADD COLUMN IF NOT EXISTS username TEXT;

-- Add unique constraint on username
-- Note: Run this AFTER adding default values to existing rows
-- For now, we'll handle uniqueness in application logic
-- CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_unique ON users(username);

-- Add optional profile fields
ALTER TABLE users
ADD COLUMN IF NOT EXISTS age INTEGER,
ADD COLUMN IF NOT EXISTS role TEXT,
ADD COLUMN IF NOT EXISTS bio TEXT,
ADD COLUMN IF NOT EXISTS location TEXT;

-- Add social media URL fields
ALTER TABLE users
ADD COLUMN IF NOT EXISTS github_url TEXT,
ADD COLUMN IF NOT EXISTS twitter_url TEXT,
ADD COLUMN IF NOT EXISTS linkedin_url TEXT,
ADD COLUMN IF NOT EXISTS website_url TEXT,
ADD COLUMN IF NOT EXISTS portfolio_url TEXT;

-- Create index on username for fast lookups
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- ============================================
-- IMPORTANT: Data Migration Steps
-- ============================================
-- 1. For existing users without username, generate one:
--    UPDATE users SET username = LOWER(REPLACE(display_name, ' ', '_')) WHERE username IS NULL;
--
-- 2. After all users have usernames, add NOT NULL constraint:
--    ALTER TABLE users ALTER COLUMN username SET NOT NULL;
--
-- 3. Then add unique constraint:
--    ALTER TABLE users ADD CONSTRAINT users_username_unique UNIQUE (username);
--
-- 4. Add length check constraint:
--    ALTER TABLE users ADD CONSTRAINT users_username_length CHECK (length(username) >= 3);
-- ============================================

-- Verify changes
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'users'
ORDER BY ordinal_position;
