-- ============================================================================
-- POPULATE NULL USERNAMES MIGRATION
-- ============================================================================
-- Purpose: Fix user search issue caused by NULL username fields
-- Date: 2026-01-07
-- Issue: JSON deserialization crashes when username is NULL, causing search
--        to return empty results
-- ============================================================================

-- Step 1: Show how many users have NULL or empty usernames (for logging)
SELECT COUNT(*) as null_username_count
FROM users
WHERE username IS NULL OR username = '';

-- Step 2: Populate NULL usernames from display_name
-- Strategy: Convert display name to lowercase and replace spaces with underscores
-- Example: "John Doe" → "john_doe"
UPDATE users
SET username = LOWER(REPLACE(TRIM(display_name), ' ', '_'))
WHERE username IS NULL OR username = '';

-- Step 3: Handle edge cases - if display_name is also NULL/empty
-- Fallback to email local part (before @)
UPDATE users
SET username = LOWER(SPLIT_PART(email, '@', 1))
WHERE username IS NULL OR username = '' OR username = LOWER(REPLACE(TRIM(''), ' ', '_'));

-- Step 4: Handle remaining edge cases - generate from user ID
-- This should rarely happen, but ensures no NULL values remain
-- Cast UUID to text first, then extract first 8 characters
UPDATE users
SET username = CONCAT('user_', SUBSTRING(id::text, 1, 8))
WHERE username IS NULL OR username = '';

-- Step 5: Verify no NULL usernames remain
SELECT COUNT(*) as remaining_null_count
FROM users
WHERE username IS NULL OR username = '';

-- Step 6: Add NOT NULL constraint to prevent future NULL insertions
-- Note: This will fail if any NULL values remain (which is good - it's a safeguard)
ALTER TABLE users
ALTER COLUMN username SET NOT NULL;

-- Step 7: Optional - Add unique constraint if usernames should be unique
-- Uncomment if you want to enforce unique usernames
-- CREATE UNIQUE INDEX idx_users_username_unique ON users(username);

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Show sample of updated usernames
SELECT id, display_name, username, email
FROM users
ORDER BY created_at DESC
LIMIT 10;

-- Check for any duplicate usernames (if unique constraint not added)
SELECT username, COUNT(*) as count
FROM users
GROUP BY username
HAVING COUNT(*) > 1;

-- ============================================================================
-- ROLLBACK (if needed)
-- ============================================================================
-- If you need to rollback, you cannot restore NULL values once constraint
-- is added. Instead, you'd need to:
-- 1. Remove the NOT NULL constraint:
--    ALTER TABLE users ALTER COLUMN username DROP NOT NULL;
-- 2. Manually update specific usernames if needed
-- ============================================================================

-- ============================================================================
-- EXECUTION INSTRUCTIONS
-- ============================================================================
-- 1. Run this script in Supabase SQL Editor
-- 2. Review the "null_username_count" result first
-- 3. Execute the UPDATE statements
-- 4. Verify "remaining_null_count" is 0
-- 5. Apply the NOT NULL constraint
-- 6. Test user search in the app
-- ============================================================================
