-- ============================================
-- ADD updated_at COLUMN TO chat_rooms TABLE
-- Run this in Supabase SQL Editor
-- ============================================

-- Add missing updated_at column to chat_rooms
ALTER TABLE public.chat_rooms
ADD COLUMN IF NOT EXISTS updated_at BIGINT
DEFAULT (EXTRACT(EPOCH FROM NOW())::BIGINT * 1000);

-- Create trigger function to auto-update timestamp on changes
CREATE OR REPLACE FUNCTION update_chat_room_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = (EXTRACT(EPOCH FROM NOW())::BIGINT * 1000);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to auto-update updated_at before each UPDATE
DROP TRIGGER IF EXISTS set_chat_room_updated_at ON chat_rooms;
CREATE TRIGGER set_chat_room_updated_at
    BEFORE UPDATE ON chat_rooms
    FOR EACH ROW
    EXECUTE FUNCTION update_chat_room_updated_at();

-- Reload PostgREST schema cache to clear any cached errors
NOTIFY pgrst, 'reload schema';

-- ============================================
-- VERIFICATION QUERIES
-- ============================================

-- Verify the column was added
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'chat_rooms'
AND column_name = 'updated_at';

-- Verify the trigger was created
SELECT trigger_name, event_manipulation, event_object_table
FROM information_schema.triggers
WHERE event_object_table = 'chat_rooms'
AND trigger_name = 'set_chat_room_updated_at';

-- Show all chat_rooms columns
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'chat_rooms'
ORDER BY ordinal_position;
