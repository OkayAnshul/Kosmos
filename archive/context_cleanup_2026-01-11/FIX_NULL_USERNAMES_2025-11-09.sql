-- ============================================
-- Fix NULL Usernames in Test Users
-- ============================================
-- Project: Kosmos Android App
-- Date: 2025-11-09
-- Purpose: Fix JSON deserialization errors caused by NULL usernames
-- Issue: Test users may have NULL/empty usernames causing app crashes
-- ============================================

-- Step 1: Check current state
-- Show users with NULL or empty usernames
SELECT
    id,
    email,
    display_name,
    username,
    CASE
        WHEN username IS NULL THEN 'NULL'
        WHEN username = '' THEN 'EMPTY'
           ELSE 'OK'
    END as status
FROM users
WHERE username IS NULL OR username = ''
ORDER BY email;

-- Step 2: Generate usernames for users without them
-- Strategy: Generate username from display_name or email
UPDATE users
SET username = CASE
    -- If display_name exists, use it
    WHEN display_name IS NOT NULL AND display_name != '' THEN
        LOWER(
            REGEXP_REPLACE(
                REGEXP_REPLACE(display_name, '[^a-zA-Z0-9]', '_', 'g'),
                '_+', '_', 'g'  -- Replace multiple underscores with single
            )
        )
    -- Otherwise, generate from email (part before @)
    ELSE
        LOWER(
            REGEXP_REPLACE(
                SPLIT_PART(email, '@', 1),
                '[^a-zA-Z0-9]', '_', 'g'
            )
        )
END
WHERE username IS NULL OR username = '';

-- Step 3: Handle duplicates by appending random suffix
-- Check for duplicate usernames
WITH duplicate_usernames AS (
    SELECT username, COUNT(*) as count
    FROM users
    WHERE username IS NOT NULL AND username != ''
    GROUP BY username
    HAVING COUNT(*) > 1
)
SELECT
    u.id,
    u.email,
    u.username,
    d.count as duplicate_count
FROM users u
JOIN duplicate_usernames d ON u.username = d.username
ORDER BY u.username, u.email;

-- Fix duplicates by appending a random number
-- This will need to be run manually for each duplicate if they exist
-- Example:
-- UPDATE users
-- SET username = username || '_' || FLOOR(RANDOM() * 10000)::TEXT
-- WHERE id IN (
--     SELECT id FROM users
--     WHERE username = 'duplicate_username'
--     ORDER BY email
--     OFFSET 1  -- Keep first one, modify the rest
-- );

-- Step 4: Add NOT NULL constraint (optional - only if not already present)
-- First check if constraint exists
SELECT
    conname AS constraint_name,
    contype AS constraint_type
FROM pg_constraint
WHERE conrelid = 'users'::regclass
AND conname LIKE '%username%';

-- If username doesn't have NOT NULL constraint, add it:
-- ALTER TABLE users
-- ALTER COLUMN username SET NOT NULL;

-- Step 5: Verify all users now have valid usernames
SELECT
    COUNT(*) as total_users,
    COUNT(CASE WHEN username IS NULL OR username = '' THEN 1 END) as users_without_username,
    COUNT(CASE WHEN username IS NOT NULL AND username != '' THEN 1 END) as users_with_username
FROM users;

-- Step 6: Show sample of fixed usernames
SELECT
    id,
    email,
    display_name,
    username,
    'FIXED' as status
FROM users
WHERE username IS NOT NULL AND username != ''
ORDER BY email
LIMIT 20;

-- ============================================
-- Additional: Create a function to auto-generate usernames
-- ============================================
-- This function can be used in triggers or manually to ensure
-- all new users get valid usernames

CREATE OR REPLACE FUNCTION generate_username(
    p_display_name TEXT,
    p_email TEXT
) RETURNS TEXT AS $$
DECLARE
    base_username TEXT;
    final_username TEXT;
    counter INTEGER := 1;
BEGIN
    -- Generate base username from display_name or email
    IF p_display_name IS NOT NULL AND p_display_name != '' THEN
        base_username := LOWER(
            REGEXP_REPLACE(
                REGEXP_REPLACE(p_display_name, '[^a-zA-Z0-9]', '_', 'g'),
                '_+', '_', 'g'
            )
        );
    ELSE
        base_username := LOWER(
            REGEXP_REPLACE(
                SPLIT_PART(p_email, '@', 1),
                '[^a-zA-Z0-9]', '_', 'g'
            )
        );
    END IF;

    -- Trim underscores from start and end
    base_username := TRIM(BOTH '_' FROM base_username);

    -- Ensure it's not empty
    IF base_username = '' THEN
        base_username := 'user';
    END IF;

    -- Check for uniqueness and append number if needed
    final_username := base_username;
    WHILE EXISTS (SELECT 1 FROM users WHERE username = final_username) LOOP
        final_username := base_username || '_' || counter;
        counter := counter + 1;
    END LOOP;

    RETURN final_username;
END;
$$ LANGUAGE plpgsql;

-- Example usage:
-- SELECT generate_username('John Doe', 'john.doe@example.com');
-- Result: john_doe

-- ============================================
-- NOTES FOR DEVELOPERS
-- ============================================
-- 1. Run this script in Supabase SQL Editor
-- 2. Check Step 1 output to see which users need fixing
-- 3. Step 2 will auto-fix most cases
-- 4. If Step 3 shows duplicates, fix them manually
-- 5. Verify with Step 5 that all users now have usernames
-- 6. The generate_username() function can be used in app code or triggers
-- ============================================
