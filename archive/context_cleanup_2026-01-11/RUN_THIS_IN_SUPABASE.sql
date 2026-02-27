-- ============================================
-- QUICK FIX: Run this in Supabase SQL Editor
-- ============================================
-- This will fix all NULL usernames in your test users
-- Copy and paste this entire script into Supabase SQL Editor and click "Run"
-- ============================================

-- Step 1: Check which users need fixing
SELECT
    id,
    email,
    display_name,
    username,
    CASE
        WHEN username IS NULL THEN '❌ NULL'
        WHEN username = '' THEN '❌ EMPTY'
        ELSE '✅ OK'
    END as status
FROM users
WHERE username IS NULL OR username = ''
ORDER BY email;

-- Step 2: Fix all NULL/empty usernames
UPDATE users
SET username = CASE
    -- If display_name exists, use it
    WHEN display_name IS NOT NULL AND display_name != '' THEN
        LOWER(
            REGEXP_REPLACE(
                REGEXP_REPLACE(display_name, '[^a-zA-Z0-9]', '_', 'g'),
                '_+', '_', 'g'
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

-- Step 3: Verify - all users should now have usernames
SELECT
    COUNT(*) as total_users,
    COUNT(CASE WHEN username IS NULL OR username = '' THEN 1 END) as users_without_username,
    COUNT(CASE WHEN username IS NOT NULL AND username != '' THEN 1 END) as users_with_username
FROM users;

-- Step 4: Show updated users
SELECT
    id,
    email,
    display_name,
    username,
    '✅ FIXED' as status
FROM users
WHERE username IS NOT NULL AND username != ''
ORDER BY email
LIMIT 20;
