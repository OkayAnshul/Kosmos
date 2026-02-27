-- ============================================================================
-- ADD METADATA CACHING TRIGGERS
-- ============================================================================
-- Purpose: Auto-update cached count fields in projects table when related
--          entities change (members, tasks, chats)
--
-- Issue: Code comments say "Auto-updated by triggers" but NO triggers exist
--        App must manually update counts = BUG RISK + inconsistency
--
-- Solution: Add database triggers to maintain counts automatically
--
-- Run this in: Supabase SQL Editor
-- Estimated time: ~10 seconds
-- ============================================================================

-- Metadata Fields to Auto-Update:
-- - member_count: Count of active project members
-- - chat_count: Count of active chat rooms
-- - task_count: Total tasks (all statuses)
-- - completed_task_count: Tasks with status = 'DONE'
-- - pending_task_count: Tasks with status NOT IN ('DONE', 'CANCELLED')
-- - last_activity_at: Timestamp of last activity

-- ============================================================================
-- TRIGGER 1: Auto-Update member_count
-- ============================================================================

CREATE OR REPLACE FUNCTION update_project_member_count()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT' AND NEW.is_active = TRUE) THEN
        -- New active member added
        UPDATE public.projects
        SET member_count = member_count + 1,
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = NEW.project_id;
        RETURN NEW;

    ELSIF (TG_OP = 'DELETE' AND OLD.is_active = TRUE) THEN
        -- Active member removed
        UPDATE public.projects
        SET member_count = GREATEST(member_count - 1, 0),
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = OLD.project_id;
        RETURN OLD;

    ELSIF (TG_OP = 'UPDATE' AND OLD.is_active != NEW.is_active) THEN
        -- Member activation status changed
        IF NEW.is_active THEN
            -- Member reactivated
            UPDATE public.projects
            SET member_count = member_count + 1,
                last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
            WHERE id = NEW.project_id;
        ELSE
            -- Member deactivated
            UPDATE public.projects
            SET member_count = GREATEST(member_count - 1, 0),
                last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
            WHERE id = NEW.project_id;
        END IF;
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to project_members table
DROP TRIGGER IF EXISTS project_member_count_trigger ON public.project_members;
CREATE TRIGGER project_member_count_trigger
AFTER INSERT OR UPDATE OR DELETE ON public.project_members
FOR EACH ROW EXECUTE FUNCTION update_project_member_count();

COMMENT ON FUNCTION update_project_member_count() IS
'Auto-updates projects.member_count when members are added/removed/changed';

-- ============================================================================
-- TRIGGER 2: Auto-Update chat_count
-- ============================================================================

CREATE OR REPLACE FUNCTION update_project_chat_count()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        -- New chat room created
        UPDATE public.projects
        SET chat_count = chat_count + 1,
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = NEW.project_id;
        RETURN NEW;

    ELSIF (TG_OP = 'DELETE') THEN
        -- Chat room deleted
        UPDATE public.projects
        SET chat_count = GREATEST(chat_count - 1, 0),
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = OLD.project_id;
        RETURN OLD;

    ELSIF (TG_OP = 'UPDATE' AND OLD.is_archived != NEW.is_archived) THEN
        -- Chat archive status changed (optional: adjust count logic if archived chats shouldn't count)
        UPDATE public.projects
        SET last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = NEW.project_id;
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to chat_rooms table
DROP TRIGGER IF EXISTS project_chat_count_trigger ON public.chat_rooms;
CREATE TRIGGER project_chat_count_trigger
AFTER INSERT OR UPDATE OR DELETE ON public.chat_rooms
FOR EACH ROW EXECUTE FUNCTION update_project_chat_count();

COMMENT ON FUNCTION update_project_chat_count() IS
'Auto-updates projects.chat_count when chat rooms are created/deleted';

-- ============================================================================
-- TRIGGER 3: Auto-Update task_count + completed_task_count + pending_task_count
-- ============================================================================

CREATE OR REPLACE FUNCTION update_project_task_counts()
RETURNS TRIGGER AS $$
DECLARE
    old_status TEXT;
    new_status TEXT;
BEGIN
    IF (TG_OP = 'INSERT') THEN
        -- New task created
        UPDATE public.projects
        SET task_count = task_count + 1,
            pending_task_count = pending_task_count + CASE
                WHEN NEW.status NOT IN ('DONE', 'CANCELLED') THEN 1
                ELSE 0
            END,
            completed_task_count = completed_task_count + CASE
                WHEN NEW.status = 'DONE' THEN 1
                ELSE 0
            END,
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = NEW.project_id;
        RETURN NEW;

    ELSIF (TG_OP = 'DELETE') THEN
        -- Task deleted
        UPDATE public.projects
        SET task_count = GREATEST(task_count - 1, 0),
            pending_task_count = GREATEST(pending_task_count - CASE
                WHEN OLD.status NOT IN ('DONE', 'CANCELLED') THEN 1
                ELSE 0
            END, 0),
            completed_task_count = GREATEST(completed_task_count - CASE
                WHEN OLD.status = 'DONE' THEN 1
                ELSE 0
            END, 0),
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = OLD.project_id;
        RETURN OLD;

    ELSIF (TG_OP = 'UPDATE' AND OLD.status != NEW.status) THEN
        -- Task status changed
        old_status := OLD.status;
        new_status := NEW.status;

        UPDATE public.projects
        SET pending_task_count = pending_task_count
            - CASE WHEN old_status NOT IN ('DONE', 'CANCELLED') THEN 1 ELSE 0 END
            + CASE WHEN new_status NOT IN ('DONE', 'CANCELLED') THEN 1 ELSE 0 END,
            completed_task_count = completed_task_count
            - CASE WHEN old_status = 'DONE' THEN 1 ELSE 0 END
            + CASE WHEN new_status = 'DONE' THEN 1 ELSE 0 END,
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = NEW.project_id;
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to tasks table
DROP TRIGGER IF EXISTS project_task_counts_trigger ON public.tasks;
CREATE TRIGGER project_task_counts_trigger
AFTER INSERT OR UPDATE OR DELETE ON public.tasks
FOR EACH ROW EXECUTE FUNCTION update_project_task_counts();

COMMENT ON FUNCTION update_project_task_counts() IS
'Auto-updates projects.task_count, completed_task_count, pending_task_count when tasks change';

-- ============================================================================
-- TRIGGER 4: Update last_activity_at on Messages
-- ============================================================================

CREATE OR REPLACE FUNCTION update_project_last_activity_on_message()
RETURNS TRIGGER AS $$
DECLARE
    related_project_id UUID;
BEGIN
    IF (TG_OP = 'INSERT') THEN
        -- Get project_id from chat_room
        SELECT project_id INTO related_project_id
        FROM public.chat_rooms
        WHERE id = NEW.chat_room_id;

        IF related_project_id IS NOT NULL THEN
            UPDATE public.projects
            SET last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
            WHERE id = related_project_id;
        END IF;

        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to messages table
DROP TRIGGER IF EXISTS project_activity_on_message_trigger ON public.messages;
CREATE TRIGGER project_activity_on_message_trigger
AFTER INSERT ON public.messages
FOR EACH ROW EXECUTE FUNCTION update_project_last_activity_on_message();

COMMENT ON FUNCTION update_project_last_activity_on_message() IS
'Updates projects.last_activity_at when new messages are sent';

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Check that triggers were created
SELECT
    trigger_name,
    event_manipulation,
    event_object_table,
    action_statement
FROM information_schema.triggers
WHERE trigger_schema = 'public'
AND trigger_name IN (
    'project_member_count_trigger',
    'project_chat_count_trigger',
    'project_task_counts_trigger',
    'project_activity_on_message_trigger'
)
ORDER BY event_object_table, trigger_name;

-- Expected Output: 4 triggers listed

-- ============================================================================
-- INITIAL COUNT SYNCHRONIZATION
-- ============================================================================
-- Run this ONCE after creating triggers to fix any existing count mismatches

-- Fix member_count
UPDATE public.projects p
SET member_count = (
    SELECT COUNT(*)
    FROM public.project_members pm
    WHERE pm.project_id = p.id
    AND pm.is_active = TRUE
);

-- Fix chat_count
UPDATE public.projects p
SET chat_count = (
    SELECT COUNT(*)
    FROM public.chat_rooms cr
    WHERE cr.project_id = p.id
);

-- Fix task counts
UPDATE public.projects p
SET
    task_count = (
        SELECT COUNT(*)
        FROM public.tasks t
        WHERE t.project_id = p.id
    ),
    completed_task_count = (
        SELECT COUNT(*)
        FROM public.tasks t
        WHERE t.project_id = p.id
        AND t.status = 'DONE'
    ),
    pending_task_count = (
        SELECT COUNT(*)
        FROM public.tasks t
        WHERE t.project_id = p.id
        AND t.status NOT IN ('DONE', 'CANCELLED')
    );

-- Verify counts are correct
SELECT
    p.id,
    p.name,
    p.member_count as cached_members,
    (SELECT COUNT(*) FROM project_members pm WHERE pm.project_id = p.id AND pm.is_active = TRUE) as actual_members,
    p.task_count as cached_tasks,
    (SELECT COUNT(*) FROM tasks t WHERE t.project_id = p.id) as actual_tasks,
    p.completed_task_count as cached_completed,
    (SELECT COUNT(*) FROM tasks t WHERE t.project_id = p.id AND t.status = 'DONE') as actual_completed,
    p.chat_count as cached_chats,
    (SELECT COUNT(*) FROM chat_rooms cr WHERE cr.project_id = p.id) as actual_chats
FROM public.projects p
LIMIT 10;

-- All cached_* should match actual_* values

-- ============================================================================
-- TESTING THE TRIGGERS
-- ============================================================================

/*
Test 1: Add a member, verify member_count increments

-- Get initial count
SELECT id, name, member_count FROM projects LIMIT 1;

-- Add test member (replace UUIDs with real values)
INSERT INTO project_members (project_id, user_id, role, is_active)
VALUES ('your-project-id', 'your-user-id', 'MEMBER', TRUE);

-- Check count increased by 1
SELECT id, name, member_count FROM projects WHERE id = 'your-project-id';

-- Cleanup
DELETE FROM project_members WHERE project_id = 'your-project-id' AND user_id = 'your-user-id';


Test 2: Create a task, verify task_count and pending_task_count increment

-- Get initial counts
SELECT id, name, task_count, pending_task_count, completed_task_count
FROM projects LIMIT 1;

-- Create test task
INSERT INTO tasks (project_id, title, status, priority, created_by_id)
VALUES ('your-project-id', 'Test Task', 'TODO', 'MEDIUM', 'your-user-id');

-- Check counts increased
SELECT id, name, task_count, pending_task_count, completed_task_count
FROM projects WHERE id = 'your-project-id';

-- Change task to DONE
UPDATE tasks SET status = 'DONE'
WHERE title = 'Test Task' AND project_id = 'your-project-id';

-- Check pending decreased, completed increased
SELECT id, name, task_count, pending_task_count, completed_task_count
FROM projects WHERE id = 'your-project-id';

-- Cleanup
DELETE FROM tasks WHERE title = 'Test Task' AND project_id = 'your-project-id';


Test 3: Send a message, verify last_activity_at updates

-- Get initial timestamp
SELECT id, name, last_activity_at FROM projects LIMIT 1;

-- Send test message (get chat_room_id from chat_rooms table)
INSERT INTO messages (chat_room_id, sender_id, content)
VALUES ('your-chat-room-id', 'your-user-id', 'Test message');

-- Check timestamp updated
SELECT id, name, last_activity_at FROM projects WHERE id = 'your-project-id';

-- Cleanup
DELETE FROM messages WHERE content = 'Test message' AND chat_room_id = 'your-chat-room-id';
*/

-- ============================================================================
-- ANDROID CODE IMPLICATIONS
-- ============================================================================

/*
After adding these triggers:

1. Remove manual count updates from repositories:
   - ProjectRepository.addMember() - DELETE manual member_count increment
   - TaskRepository.createTask() - DELETE manual task_count increment
   - ChatRepository.createChatRoom() - DELETE manual chat_count increment

2. Rely on Supabase triggers to maintain counts:
   - Insert/update/delete entities normally
   - Counts will auto-update on Supabase side
   - Real-time listener will propagate updates to Room

3. Initial sync should trust Supabase counts:
   - Don't recalculate counts locally
   - Supabase is source of truth for metadata

4. Offline mode considerations:
   - Local count updates still needed when offline
   - When syncing to Supabase, DON'T send counts (let triggers handle it)
   - When receiving from Supabase, overwrite local counts (Supabase wins)

Example - Remove this from ProjectRepository:
```kotlin
// BEFORE (manual count update):
suspend fun addMember(member: ProjectMember) {
    localDataSource.insertMember(member)
    remoteDataSource.insertMember(member)
    // ❌ DELETE THIS:
    remoteDataSource.updateProject(project.copy(memberCount = project.memberCount + 1))
}

// AFTER (trigger handles it):
suspend fun addMember(member: ProjectMember) {
    localDataSource.insertMember(member)
    remoteDataSource.insertMember(member)
    // ✅ Trigger auto-updates member_count
}
```

5. Sync logic:
```kotlin
// When syncing from Supabase
val projectFromSupabase = supabase.from("projects").select().single()
// Trust Supabase counts (they're maintained by triggers)
localDataSource.insertProject(projectFromSupabase)  // Overwrites local counts
```
*/

-- ============================================================================
-- ROLLBACK (if needed)
-- ============================================================================

/*
-- UNCOMMENT ONLY IF YOU NEED TO ROLLBACK

DROP TRIGGER IF EXISTS project_member_count_trigger ON public.project_members;
DROP TRIGGER IF EXISTS project_chat_count_trigger ON public.chat_rooms;
DROP TRIGGER IF EXISTS project_task_counts_trigger ON public.tasks;
DROP TRIGGER IF EXISTS project_activity_on_message_trigger ON public.messages;

DROP FUNCTION IF EXISTS update_project_member_count();
DROP FUNCTION IF EXISTS update_project_chat_count();
DROP FUNCTION IF EXISTS update_project_task_counts();
DROP FUNCTION IF EXISTS update_project_last_activity_on_message();
*/
