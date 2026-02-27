# RLS Security Audit

**Date**: 2026-02-15
**Status**: PASSED (1 manual action remaining)

## Summary

All 11 public tables now have RLS enabled with correct policies.

## Before (CRITICAL)

| Table | RLS | Policies |
|-------|-----|----------|
| users | OFF | 3 (not enforced!) |
| projects | OFF | 0 |
| project_members | OFF | 0 |
| chat_rooms | OFF | 0 |
| messages | OFF | 0 |
| tasks | OFF | 0 |
| task_comments | OFF | 0 |
| notifications | ON | 4 (INSERT was unrestricted) |
| task_activity | ON | 2 (used auth.uid() directly) |
| time_entries | ON | 4 (used auth.uid() directly) |
| task_dependencies | ON | 3 (used auth.uid() directly) |

## After (FIXED)

| Table | RLS | Policies | Notes |
|-------|-----|----------|-------|
| users | ON | 3 | SELECT all, INSERT/UPDATE own |
| projects | ON | 4 | Members SELECT; owner INSERT; owner/admin UPDATE; owner DELETE |
| project_members | ON | 4 | Co-members SELECT; admin/manager INSERT; admin UPDATE; admin DELETE + self |
| chat_rooms | ON | 4 | Project members CRUD; creator/admin manage |
| messages | ON | 4 | Project members SELECT/INSERT; sender UPDATE/DELETE |
| tasks | ON | 4 | Project members SELECT/INSERT; creator/assignee/admin UPDATE; creator/admin DELETE |
| task_comments | ON | 4 | Project members SELECT/INSERT; author UPDATE/DELETE |
| notifications | ON | 4 | Own user only (fixed INSERT from unrestricted) |
| task_activity | ON | 2 | Project members SELECT/INSERT |
| time_entries | ON | 4 | Project members SELECT; own user INSERT/UPDATE/DELETE |
| task_dependencies | ON | 3 | Via task's project membership |

## Migrations Applied

1. `enable_rls_all_tables` — Enable RLS + create policies for 7 tables
2. `fix_existing_policies_and_functions` — Fix 4 existing tables + 8 function search_paths

## Policy Design Principles

- All policies use `(select auth.uid())` for performance (evaluated once, not per-row)
- Membership checked via `project_members` table with `is_active = true`
- ADMIN gets full access, MANAGER limited, MEMBER most restricted
- Self-referential actions allowed (leave project, edit own messages, etc.)

## Function Search Path

All 8 public functions now have `SET search_path = public`:
- generate_username, update_chat_room_updated_at, update_notification_updated_at
- update_project_chat_count, update_project_last_activity_on_message
- update_project_member_count, update_project_members_updated_at, update_project_task_counts

## Remaining Manual Action

- **Leaked password protection**: Must be enabled in Supabase Dashboard → Auth → Settings
  - URL: https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection

## Post-RLS Advisor Results

Security advisors: Only 1 warning remaining (leaked password protection — dashboard-only setting).
All function search_path warnings: RESOLVED.
All unrestricted policy warnings: RESOLVED.
