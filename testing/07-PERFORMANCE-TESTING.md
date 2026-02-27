# Performance Testing

**Date**: 2026-02-15
**Status**: Index cleanup complete

## Duplicate Index Cleanup

**Before**: 17 duplicate index pairs flagged by Supabase advisor
**After**: 0 duplicate index warnings

### Indexes Dropped (17)
- idx_chat_rooms_project (dup of idx_chat_rooms_project_id)
- idx_messages_chat_room (dup of idx_messages_chat_room_id)
- idx_messages_sender (dup of idx_messages_sender_id)
- idx_project_members_project (dup of idx_project_members_project_id)
- idx_project_members_active (dup of idx_project_members_project_active)
- idx_project_members_user (dup of idx_project_members_user_id)
- project_members_project_id_user_id_key (dup of unique_project_member)
- idx_projects_owner (dup of idx_projects_owner_id)
- idx_task_activity_actor (dup of idx_task_activity_actor_id)
- idx_task_activity_project (dup of idx_task_activity_project_id)
- idx_task_activity_task (dup of idx_task_activity_task_id)
- idx_tasks_assigned_to + idx_tasks_assignee (dup of idx_tasks_assigned_to_id)
- idx_tasks_chat_room (dup of idx_tasks_chat_room_id)
- idx_tasks_project (dup of idx_tasks_project_id)
- idx_tasks_status (dup of idx_tasks_project_status)
- unique_username constraint (dup of users_username_key)

### Missing FK Indexes Added (4)
- idx_messages_reply_to_id (for fk_messages_reply_to)
- idx_messages_reply_to_message_id (for messages_reply_to_id_fkey)
- idx_project_members_invited_by (for fk_project_members_invited_by)
- idx_tasks_source_message_id (for tasks_source_message_id_fkey)

## Remaining Unused Indexes

Many indexes show as "unused" because:
1. App hasn't been exercised with RLS yet (RLS subqueries use these indexes)
2. Low data volume (6 users, 31 projects, 50 tasks)
3. Some are necessary for FK constraints

**Decision**: Keep all remaining indexes — they'll be used by RLS policy subqueries.

## Supabase Advisor Status

- **Security**: 1 INFO (leaked password protection — dashboard setting)
- **Performance**: Only unused_index INFO-level items (expected)
- **No WARN or ERROR level issues**
