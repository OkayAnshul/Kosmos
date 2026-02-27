-- Check if the problematic user exists in Supabase
-- Run this in your Supabase SQL Editor

-- 1. Check if user exists
SELECT id, username, display_name, email, created_at
FROM users
WHERE id = 'b4330dd0-1066-4702-bede-2beef09fc847';

-- 2. Find all messages from this missing user
SELECT COUNT(*) as message_count, chat_room_id
FROM messages
WHERE sender_id = 'b4330dd0-1066-4702-bede-2beef09fc847'
GROUP BY chat_room_id;

-- 3. Check if there are other orphaned messages
SELECT DISTINCT m.sender_id
FROM messages m
LEFT JOIN users u ON m.sender_id = u.id
WHERE u.id IS NULL
LIMIT 10;

-- 4. If user doesn't exist, you have two options:
--    Option A: Delete orphaned messages (DESTRUCTIVE)
--    DELETE FROM messages WHERE sender_id = 'b4330dd0-1066-4702-bede-2beef09fc847';
--
--    Option B: Create a placeholder "deleted user" account
--    INSERT INTO users (id, username, display_name, email)
--    VALUES (
--      'b4330dd0-1066-4702-bede-2beef09fc847',
--      'deleted_user',
--      'Deleted User',
--      'deleted@example.com'
--    );
