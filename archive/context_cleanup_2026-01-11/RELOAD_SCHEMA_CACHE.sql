-- Reload Supabase PostgREST Schema Cache
-- Run this in Supabase SQL Editor after adding the settings column

-- This command tells PostgREST to reload its schema cache
NOTIFY pgrst, 'reload schema';

-- Verify the settings column exists
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'users'
  AND column_name = 'settings';

-- Expected output: settings | jsonb
