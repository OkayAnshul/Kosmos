-- P0-05 FIX: Add foreign key constraints to all tables
-- Prevents orphaned data (tasks without projects, messages without chats, etc.)
--
-- Run this migration in Supabase SQL Editor
-- Date: 2026-01-24
-- Phase 0: Critical Data Integrity
--
-- IMPORTANT: This migration adds foreign key constraints to existing tables.
-- Ensure data integrity before running (no orphaned records).

-- =============================================================================
-- STEP 1: Clean up any orphaned data (run checks first!)
-- =============================================================================

-- Check for orphaned tasks (tasks with non-existent projects)
SELECT COUNT(*) AS orphaned_tasks
FROM tasks t
WHERE NOT EXISTS (SELECT 1 FROM projects p WHERE p.id = t.project_id);

-- Check for orphaned messages (messages with non-existent chat rooms)
SELECT COUNT(*) AS orphaned_messages
FROM messages m
WHERE NOT EXISTS (SELECT 1 FROM chat_rooms c WHERE c.id = m.chat_room_id);

-- Check for orphaned chat rooms (chat rooms with non-existent projects)
SELECT COUNT(*) AS orphaned_chat_rooms
FROM chat_rooms cr
WHERE NOT EXISTS (SELECT 1 FROM projects p WHERE p.id = cr.project_id);

-- Check for orphaned project members
SELECT COUNT(*) AS orphaned_members
FROM project_members pm
WHERE NOT EXISTS (SELECT 1 FROM projects p WHERE p.id = pm.project_id)
   OR NOT EXISTS (SELECT 1 FROM users u WHERE u.id = pm.user_id);

-- =============================================================================
-- STEP 2: Add foreign key constraints
-- =============================================================================

-- TASKS table foreign keys
-- =============================================================================

-- FK: task.project_id -> projects.id (CASCADE delete)
ALTER TABLE public.tasks
ADD CONSTRAINT fk_tasks_project_id
FOREIGN KEY (project_id)
REFERENCES public.projects(id)
ON DELETE CASCADE;

-- FK: task.chat_room_id -> chat_rooms.id (SET NULL)
ALTER TABLE public.tasks
ADD CONSTRAINT fk_tasks_chat_room_id
FOREIGN KEY (chat_room_id)
REFERENCES public.chat_rooms(id)
ON DELETE SET NULL;

-- FK: task.assigned_to_id -> users.id (SET NULL)
ALTER TABLE public.tasks
ADD CONSTRAINT fk_tasks_assigned_to_id
FOREIGN KEY (assigned_to_id)
REFERENCES public.users(id)
ON DELETE SET NULL;

-- FK: task.created_by_id -> users.id (CASCADE delete)
ALTER TABLE public.tasks
ADD CONSTRAINT fk_tasks_created_by_id
FOREIGN KEY (created_by_id)
REFERENCES public.users(id)
ON DELETE CASCADE;

-- MESSAGES table foreign keys
-- =============================================================================

-- FK: message.chat_room_id -> chat_rooms.id (CASCADE delete)
ALTER TABLE public.messages
ADD CONSTRAINT fk_messages_chat_room_id
FOREIGN KEY (chat_room_id)
REFERENCES public.chat_rooms(id)
ON DELETE CASCADE;

-- FK: message.sender_id -> users.id (CASCADE delete)
ALTER TABLE public.messages
ADD CONSTRAINT fk_messages_sender_id
FOREIGN KEY (sender_id)
REFERENCES public.users(id)
ON DELETE CASCADE;

-- FK: message.voice_message_id -> voice_messages.id (SET NULL)
ALTER TABLE public.messages
ADD CONSTRAINT fk_messages_voice_message_id
FOREIGN KEY (voice_message_id)
REFERENCES public.voice_messages(id)
ON DELETE SET NULL;

-- FK: message.reply_to_message_id -> messages.id (SET NULL)
ALTER TABLE public.messages
ADD CONSTRAINT fk_messages_reply_to_message_id
FOREIGN KEY (reply_to_message_id)
REFERENCES public.messages(id)
ON DELETE SET NULL;

-- CHAT_ROOMS table foreign keys
-- =============================================================================

-- FK: chat_room.project_id -> projects.id (CASCADE delete)
ALTER TABLE public.chat_rooms
ADD CONSTRAINT fk_chat_rooms_project_id
FOREIGN KEY (project_id)
REFERENCES public.projects(id)
ON DELETE CASCADE;

-- PROJECT_MEMBERS table foreign keys
-- =============================================================================

-- FK: project_member.project_id -> projects.id (CASCADE delete)
ALTER TABLE public.project_members
ADD CONSTRAINT fk_project_members_project_id
FOREIGN KEY (project_id)
REFERENCES public.projects(id)
ON DELETE CASCADE;

-- FK: project_member.user_id -> users.id (CASCADE delete)
ALTER TABLE public.project_members
ADD CONSTRAINT fk_project_members_user_id
FOREIGN KEY (user_id)
REFERENCES public.users(id)
ON DELETE CASCADE;

-- Add unique constraint to prevent duplicate memberships
ALTER TABLE public.project_members
ADD CONSTRAINT unique_project_user
UNIQUE (project_id, user_id);

-- TASK_ACTIVITY table foreign keys
-- =============================================================================

-- FK: task_activity.task_id -> tasks.id (CASCADE delete)
ALTER TABLE public.task_activity
ADD CONSTRAINT fk_task_activity_task_id
FOREIGN KEY (task_id)
REFERENCES public.tasks(id)
ON DELETE CASCADE;

-- FK: task_activity.project_id -> projects.id (CASCADE delete)
ALTER TABLE public.task_activity
ADD CONSTRAINT fk_task_activity_project_id
FOREIGN KEY (project_id)
REFERENCES public.projects(id)
ON DELETE CASCADE;

-- FK: task_activity.actor_id -> users.id (CASCADE delete)
ALTER TABLE public.task_activity
ADD CONSTRAINT fk_task_activity_actor_id
FOREIGN KEY (actor_id)
REFERENCES public.users(id)
ON DELETE CASCADE;

-- VOICE_MESSAGES table foreign keys
-- =============================================================================

-- FK: voice_message.message_id -> messages.id (CASCADE delete)
ALTER TABLE public.voice_messages
ADD CONSTRAINT fk_voice_messages_message_id
FOREIGN KEY (message_id)
REFERENCES public.messages(id)
ON DELETE CASCADE;

-- Add unique constraint (one voice message per message)
ALTER TABLE public.voice_messages
ADD CONSTRAINT unique_voice_message_per_message
UNIQUE (message_id);

-- ACTION_ITEMS table foreign keys
-- =============================================================================

-- FK: action_item.message_id -> messages.id (CASCADE delete)
ALTER TABLE public.action_items
ADD CONSTRAINT fk_action_items_message_id
FOREIGN KEY (message_id)
REFERENCES public.messages(id)
ON DELETE CASCADE;

-- FK: action_item.voice_message_id -> voice_messages.id (CASCADE delete)
ALTER TABLE public.action_items
ADD CONSTRAINT fk_action_items_voice_message_id
FOREIGN KEY (voice_message_id)
REFERENCES public.voice_messages(id)
ON DELETE CASCADE;

-- FK: action_item.chat_room_id -> chat_rooms.id (CASCADE delete)
ALTER TABLE public.action_items
ADD CONSTRAINT fk_action_items_chat_room_id
FOREIGN KEY (chat_room_id)
REFERENCES public.chat_rooms(id)
ON DELETE CASCADE;

-- FK: action_item.task_id -> tasks.id (SET NULL)
ALTER TABLE public.action_items
ADD CONSTRAINT fk_action_items_task_id
FOREIGN KEY (task_id)
REFERENCES public.tasks(id)
ON DELETE SET NULL;

-- =============================================================================
-- STEP 3: Create missing indexes (performance optimization)
-- =============================================================================

-- Tasks indexes
CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON public.tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_chat_room_id ON public.tasks(chat_room_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to_id ON public.tasks(assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_tasks_created_by_id ON public.tasks(created_by_id);

-- Messages indexes
CREATE INDEX IF NOT EXISTS idx_messages_chat_room_id ON public.messages(chat_room_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON public.messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_voice_message_id ON public.messages(voice_message_id);
CREATE INDEX IF NOT EXISTS idx_messages_reply_to_message_id ON public.messages(reply_to_message_id);
CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON public.messages(timestamp DESC);

-- Chat rooms indexes
CREATE INDEX IF NOT EXISTS idx_chat_rooms_project_id ON public.chat_rooms(project_id);

-- Project members indexes
CREATE INDEX IF NOT EXISTS idx_project_members_project_id ON public.project_members(project_id);
CREATE INDEX IF NOT EXISTS idx_project_members_user_id ON public.project_members(user_id);

-- Voice messages indexes (already created in P0-02 but included for completeness)
CREATE INDEX IF NOT EXISTS idx_voice_messages_message_id ON public.voice_messages(message_id);

-- Action items indexes
CREATE INDEX IF NOT EXISTS idx_action_items_message_id ON public.action_items(message_id);
CREATE INDEX IF NOT EXISTS idx_action_items_voice_message_id ON public.action_items(voice_message_id);
CREATE INDEX IF NOT EXISTS idx_action_items_chat_room_id ON public.action_items(chat_room_id);
CREATE INDEX IF NOT EXISTS idx_action_items_task_id ON public.action_items(task_id);

-- =============================================================================
-- STEP 4: Verify foreign keys
-- =============================================================================

-- List all foreign key constraints
SELECT
    tc.table_name,
    tc.constraint_name,
    tc.constraint_type,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_schema = 'public'
ORDER BY tc.table_name, tc.constraint_name;

-- =============================================================================
-- ROLLBACK INSTRUCTIONS (in case of issues)
-- =============================================================================

/*
-- Drop all foreign key constraints (run if migration causes issues)

ALTER TABLE public.tasks DROP CONSTRAINT IF EXISTS fk_tasks_project_id;
ALTER TABLE public.tasks DROP CONSTRAINT IF EXISTS fk_tasks_chat_room_id;
ALTER TABLE public.tasks DROP CONSTRAINT IF EXISTS fk_tasks_assigned_to_id;
ALTER TABLE public.tasks DROP CONSTRAINT IF EXISTS fk_tasks_created_by_id;

ALTER TABLE public.messages DROP CONSTRAINT IF EXISTS fk_messages_chat_room_id;
ALTER TABLE public.messages DROP CONSTRAINT IF EXISTS fk_messages_sender_id;
ALTER TABLE public.messages DROP CONSTRAINT IF EXISTS fk_messages_voice_message_id;
ALTER TABLE public.messages DROP CONSTRAINT IF EXISTS fk_messages_reply_to_message_id;

ALTER TABLE public.chat_rooms DROP CONSTRAINT IF EXISTS fk_chat_rooms_project_id;

ALTER TABLE public.project_members DROP CONSTRAINT IF EXISTS fk_project_members_project_id;
ALTER TABLE public.project_members DROP CONSTRAINT IF EXISTS fk_project_members_user_id;
ALTER TABLE public.project_members DROP CONSTRAINT IF EXISTS unique_project_user;

ALTER TABLE public.task_activity DROP CONSTRAINT IF EXISTS fk_task_activity_task_id;
ALTER TABLE public.task_activity DROP CONSTRAINT IF EXISTS fk_task_activity_project_id;
ALTER TABLE public.task_activity DROP CONSTRAINT IF EXISTS fk_task_activity_actor_id;

ALTER TABLE public.voice_messages DROP CONSTRAINT IF EXISTS fk_voice_messages_message_id;
ALTER TABLE public.voice_messages DROP CONSTRAINT IF EXISTS unique_voice_message_per_message;

ALTER TABLE public.action_items DROP CONSTRAINT IF EXISTS fk_action_items_message_id;
ALTER TABLE public.action_items DROP CONSTRAINT IF EXISTS fk_action_items_voice_message_id;
ALTER TABLE public.action_items DROP CONSTRAINT IF EXISTS fk_action_items_chat_room_id;
ALTER TABLE public.action_items DROP CONSTRAINT IF EXISTS fk_action_items_task_id;
*/
