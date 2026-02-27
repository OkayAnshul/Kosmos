-- Migration: Add settings column to users table
-- This migration adds a JSONB column to store user settings (privacy + notifications)
-- Safe to run multiple times (uses IF NOT EXISTS check)

-- Add settings column if it doesn't exist
DO $$
BEGIN
    -- Check if settings column exists
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
        AND table_name = 'users'
        AND column_name = 'settings'
    ) THEN
        -- Add settings column with default values
        ALTER TABLE users ADD COLUMN settings JSONB DEFAULT '{
            "privacy": {
                "profileVisibility": "PUBLIC",
                "showEmail": false,
                "showLastSeen": true,
                "showOnlineStatus": true,
                "allowDirectMessages": true,
                "allowMentions": true
            },
            "notifications": {
                "enabled": true,
                "messages": true,
                "tasks": true,
                "projectUpdates": true,
                "mentions": true,
                "mentionsOnlyMode": false,
                "sound": true,
                "vibration": true,
                "dnd": {
                    "enabled": false,
                    "startHour": 22,
                    "startMinute": 0,
                    "endHour": 8,
                    "endMinute": 0
                }
            }
        }'::jsonb;

        RAISE NOTICE 'Settings column added to users table';
    ELSE
        RAISE NOTICE 'Settings column already exists';
    END IF;
END $$;

-- Create index on settings for faster queries
CREATE INDEX IF NOT EXISTS idx_users_settings ON users USING GIN (settings);

-- Verify the migration
SELECT
    table_name,
    column_name,
    data_type,
    column_default IS NOT NULL as has_default
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name = 'users'
AND column_name = 'settings';
