# RLS Security Audit - Kosmos Android App

**Date**: 2026-01-13
**Status**: 🔴 CRITICAL - RLS Currently Disabled
**Priority**: P0 - Must fix before production

---

## Executive Summary

**CRITICAL SECURITY ISSUE FOUND**: Row Level Security (RLS) is currently **DISABLED** on all tables in the Supabase database. This means:
- ❌ Any authenticated user can access ANY other user's data
- ❌ No data isolation between users
- ❌ No project membership enforcement at database level
- ❌ Messages, tasks, and projects are accessible to all users

**Impact**: Complete data breach risk. All user data is vulnerable.

**Status per SCHEMA_FIX_COMPLETE_V2.sql line 650**:
> "✅ RLS disabled for all tables (testing mode)"

---

## Current State Analysis

### Tables Without RLS Protection

| Table | RLS Status | Risk Level | Data Exposed |
|-------|-----------|------------|--------------|
| `users` | 🔴 Disabled | HIGH | Email, phone, profile data, settings |
| `projects` | 🔴 Disabled | CRITICAL | All project details regardless of membership |
| `project_members` | 🔴 Disabled | CRITICAL | Membership lists, roles, permissions |
| `chat_rooms` | 🔴 Disabled | CRITICAL | All chats regardless of participation |
| `messages` | 🔴 Disabled | CRITICAL | All messages from all projects |
| `tasks` | 🔴 Disabled | HIGH | All tasks from all projects |

### Verification Query Results

Current state (from schema file line 597-607):
```sql
SELECT tablename,
  CASE WHEN rowsecurity = true
    THEN '🔒 ENABLED'
    ELSE '🔓 DISABLED (testing mode)'
  END as rls_status
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename IN ('users', 'messages', 'chat_rooms', 'tasks', 'projects', 'project_members')
ORDER BY tablename;
```

**Expected Result**: All show "🔓 DISABLED (testing mode)"

---

## Required RLS Policies

### 1. Users Table

**Access Rules**:
- Users can read their own profile
- Users can read profiles of project members (if project visibility allows)
- Users can update their own profile only
- No user can delete any profile

**Policies**:
```sql
-- Enable RLS
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view their own profile
CREATE POLICY "users_select_own" ON users
  FOR SELECT
  USING (auth.uid() = id);

-- Policy: Users can view profiles of users in same projects (for @mentions, assignments)
CREATE POLICY "users_select_project_members" ON users
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM project_members pm1
      JOIN project_members pm2 ON pm1.project_id = pm2.project_id
      WHERE pm1.user_id = auth.uid()
        AND pm2.user_id = users.id
    )
  );

-- Policy: Users can update their own profile
CREATE POLICY "users_update_own" ON users
  FOR UPDATE
  USING (auth.uid() = id)
  WITH CHECK (auth.uid() = id);

-- Policy: New users can insert their own profile (signup)
CREATE POLICY "users_insert_own" ON users
  FOR INSERT
  WITH CHECK (auth.uid() = id);

-- No delete policy (profiles are never deleted, only soft-deleted)
```

---

### 2. Projects Table

**Access Rules**:
- Users can see projects they are members of
- Project owners can update/delete their projects
- Admins can update projects
- Members can only read

**Policies**:
```sql
-- Enable RLS
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view projects they are members of
CREATE POLICY "projects_select_member" ON projects
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = projects.id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Users can create new projects
CREATE POLICY "projects_insert_owner" ON projects
  FOR INSERT
  WITH CHECK (auth.uid() = owner_id);

-- Policy: Owners and Admins can update projects
CREATE POLICY "projects_update_admin" ON projects
  FOR UPDATE
  USING (
    owner_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = projects.id
        AND project_members.user_id = auth.uid()
        AND project_members.role = 'ADMIN'
    )
  )
  WITH CHECK (
    owner_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = projects.id
        AND project_members.user_id = auth.uid()
        AND project_members.role = 'ADMIN'
    )
  );

-- Policy: Only owners can delete projects
CREATE POLICY "projects_delete_owner" ON projects
  FOR DELETE
  USING (owner_id = auth.uid());
```

---

### 3. Project Members Table

**Access Rules**:
- Users can view members of projects they belong to
- Only admins and owners can add/remove members
- Only admins can change roles

**Policies**:
```sql
-- Enable RLS
ALTER TABLE project_members ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view members of their projects
CREATE POLICY "project_members_select" ON project_members
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM project_members pm
      WHERE pm.project_id = project_members.project_id
        AND pm.user_id = auth.uid()
    )
  );

-- Policy: Admins and owners can add members
CREATE POLICY "project_members_insert" ON project_members
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM projects
      WHERE projects.id = project_members.project_id
        AND (
          projects.owner_id = auth.uid()
          OR EXISTS (
            SELECT 1 FROM project_members pm
            WHERE pm.project_id = project_members.project_id
              AND pm.user_id = auth.uid()
              AND pm.role = 'ADMIN'
          )
        )
    )
  );

-- Policy: Admins and owners can remove members
CREATE POLICY "project_members_delete" ON project_members
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM projects
      WHERE projects.id = project_members.project_id
        AND (
          projects.owner_id = auth.uid()
          OR EXISTS (
            SELECT 1 FROM project_members pm
            WHERE pm.project_id = project_members.project_id
              AND pm.user_id = auth.uid()
              AND pm.role = 'ADMIN'
          )
        )
    )
  );

-- Policy: Admins can update member roles
CREATE POLICY "project_members_update" ON project_members
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM projects
      WHERE projects.id = project_members.project_id
        AND (
          projects.owner_id = auth.uid()
          OR EXISTS (
            SELECT 1 FROM project_members pm
            WHERE pm.project_id = project_members.project_id
              AND pm.user_id = auth.uid()
              AND pm.role = 'ADMIN'
          )
        )
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM projects
      WHERE projects.id = project_members.project_id
        AND (
          projects.owner_id = auth.uid()
          OR EXISTS (
            SELECT 1 FROM project_members pm
            WHERE pm.project_id = project_members.project_id
              AND pm.user_id = auth.uid()
              AND pm.role = 'ADMIN'
          )
        )
    )
  );
```

---

### 4. Chat Rooms Table

**Access Rules**:
- Users can see chat rooms they are participants in
- Project members can create chat rooms
- Only project admins can delete chat rooms

**Policies**:
```sql
-- Enable RLS
ALTER TABLE chat_rooms ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view chat rooms they participate in
CREATE POLICY "chat_rooms_select" ON chat_rooms
  FOR SELECT
  USING (
    auth.uid() = ANY(participant_ids)
    OR EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = chat_rooms.project_id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Project members can create chat rooms
CREATE POLICY "chat_rooms_insert" ON chat_rooms
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = chat_rooms.project_id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Project admins can update chat rooms
CREATE POLICY "chat_rooms_update" ON chat_rooms
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = chat_rooms.project_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = chat_rooms.project_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  );

-- Policy: Project admins can delete chat rooms
CREATE POLICY "chat_rooms_delete" ON chat_rooms
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = chat_rooms.project_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  );
```

---

### 5. Messages Table

**Access Rules**:
- Users can only see messages in chat rooms they participate in
- Users can send messages to chat rooms they participate in
- Users can only edit/delete their own messages

**Policies**:
```sql
-- Enable RLS
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view messages in their chat rooms
CREATE POLICY "messages_select" ON messages
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM chat_rooms
      WHERE chat_rooms.id = messages.chat_room_id
        AND (
          auth.uid() = ANY(chat_rooms.participant_ids)
          OR EXISTS (
            SELECT 1 FROM project_members
            WHERE project_members.project_id = chat_rooms.project_id
              AND project_members.user_id = auth.uid()
          )
        )
    )
  );

-- Policy: Users can send messages to chat rooms they participate in
CREATE POLICY "messages_insert" ON messages
  FOR INSERT
  WITH CHECK (
    sender_id = auth.uid()
    AND EXISTS (
      SELECT 1 FROM chat_rooms
      WHERE chat_rooms.id = messages.chat_room_id
        AND (
          auth.uid() = ANY(chat_rooms.participant_ids)
          OR EXISTS (
            SELECT 1 FROM project_members
            WHERE project_members.project_id = chat_rooms.project_id
              AND project_members.user_id = auth.uid()
          )
        )
    )
  );

-- Policy: Users can update their own messages (edit)
CREATE POLICY "messages_update" ON messages
  FOR UPDATE
  USING (sender_id = auth.uid())
  WITH CHECK (sender_id = auth.uid());

-- Policy: Users can delete their own messages
CREATE POLICY "messages_delete" ON messages
  FOR DELETE
  USING (sender_id = auth.uid());
```

---

### 6. Tasks Table

**Access Rules**:
- Users can see tasks in projects they are members of
- Project members can create tasks
- Task assignees and admins can update tasks
- Only admins can delete tasks

**Policies**:
```sql
-- Enable RLS
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;

-- Policy: Users can view tasks in their projects
CREATE POLICY "tasks_select" ON tasks
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM chat_rooms
      JOIN project_members ON chat_rooms.project_id = project_members.project_id
      WHERE chat_rooms.id = tasks.chat_room_id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Project members can create tasks
CREATE POLICY "tasks_insert" ON tasks
  FOR INSERT
  WITH CHECK (
    created_by = auth.uid()
    AND EXISTS (
      SELECT 1 FROM chat_rooms
      JOIN project_members ON chat_rooms.project_id = project_members.project_id
      WHERE chat_rooms.id = tasks.chat_room_id
        AND project_members.user_id = auth.uid()
    )
  );

-- Policy: Assignees and admins can update tasks
CREATE POLICY "tasks_update" ON tasks
  FOR UPDATE
  USING (
    assigned_to_user_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM chat_rooms
      JOIN project_members ON chat_rooms.project_id = project_members.project_id
      WHERE chat_rooms.id = tasks.chat_room_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  )
  WITH CHECK (
    assigned_to_user_id = auth.uid()
    OR EXISTS (
      SELECT 1 FROM chat_rooms
      JOIN project_members ON chat_rooms.project_id = project_members.project_id
      WHERE chat_rooms.id = tasks.chat_room_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  );

-- Policy: Only admins can delete tasks
CREATE POLICY "tasks_delete" ON tasks
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM chat_rooms
      JOIN project_members ON chat_rooms.project_id = project_members.project_id
      WHERE chat_rooms.id = tasks.chat_room_id
        AND project_members.user_id = auth.uid()
        AND project_members.role IN ('ADMIN', 'OWNER')
    )
  );
```

---

## Manual Testing Procedure

### Test 1: User Isolation (CRITICAL)

**Objective**: Verify users cannot access other users' data

**Steps**:
1. Create two test users in Supabase Auth:
   - User A: `test-user-a@example.com`
   - User B: `test-user-b@example.com`

2. User A creates a project (should succeed)
3. User A creates a chat room in project (should succeed)
4. User A sends a message (should succeed)

5. **CRITICAL TEST**: Log in as User B
6. Try to query User A's project directly:
   ```sql
   SELECT * FROM projects WHERE owner_id = '<user-a-id>';
   ```
   - ✅ Expected: 0 rows returned (RLS blocks it)
   - ❌ Current (no RLS): Returns User A's project

7. Try to query User A's messages:
   ```sql
   SELECT * FROM messages WHERE sender_id = '<user-a-id>';
   ```
   - ✅ Expected: 0 rows returned
   - ❌ Current (no RLS): Returns all User A's messages

### Test 2: Project Member Access

**Objective**: Verify project membership enforcement

**Steps**:
1. User A creates Project X
2. User A adds User B as MEMBER
3. User A creates chat room in Project X

4. **Verify User B can access**:
   - ✅ Can see Project X
   - ✅ Can see chat room
   - ✅ Can send messages
   - ❌ Cannot delete project
   - ❌ Cannot remove User A

5. User A removes User B from project

6. **Verify User B loses access**:
   - ❌ Cannot see Project X
   - ❌ Cannot see chat rooms
   - ❌ Cannot see messages

### Test 3: Role-Based Permissions

**Objective**: Verify ADMIN vs MEMBER permissions

**Steps**:
1. User A creates Project Y
2. User A adds User B as MEMBER
3. User A adds User C as ADMIN

4. **Verify MEMBER (User B) restrictions**:
   - ✅ Can view project
   - ✅ Can create tasks
   - ❌ Cannot add new members
   - ❌ Cannot change roles
   - ❌ Cannot delete project

5. **Verify ADMIN (User C) permissions**:
   - ✅ Can view project
   - ✅ Can create tasks
   - ✅ Can add new members
   - ✅ Can change member roles
   - ✅ Can update project settings
   - ❌ Cannot delete project (only owner can)

### Test 4: Message Privacy

**Objective**: Verify message isolation between chat rooms

**Steps**:
1. User A creates Project Z with Chat Room 1
2. User B creates Project W with Chat Room 2
3. User A sends message M1 in Chat Room 1
4. User B sends message M2 in Chat Room 2

5. **Verify isolation**:
   - User A queries messages → should only see M1
   - User B queries messages → should only see M2
   - Neither can see the other's messages

---

## Implementation Steps

### Step 1: Backup Current Database (CRITICAL)

```sql
-- Create backup before enabling RLS
-- Run in Supabase SQL Editor

-- Export all data first using Supabase Dashboard:
-- 1. Go to Table Editor
-- 2. For each table, click "..." → "Export as CSV"
-- 3. Save all CSVs locally
```

### Step 2: Enable RLS (Execute in Order)

**File**: `RLS_ENABLE_PRODUCTION.sql` (to be created)

1. Run users table policies
2. Run projects table policies
3. Run project_members table policies
4. Run chat_rooms table policies
5. Run messages table policies
6. Run tasks table policies

### Step 3: Test with Anon Key

After enabling RLS, test that:
- Unauthenticated requests fail (return no data)
- Authenticated requests only return user's data

### Step 4: Update Android App

Verify that all Supabase queries use `auth.uid()` correctly:
- ✅ Queries already filtered by userId in repository layer
- ✅ RLS provides additional database-level security
- ⚠️ Some queries may fail if RLS is too restrictive

### Step 5: Monitor Logs

After deploying RLS:
- Check Supabase logs for policy violations
- Monitor Android app for "permission denied" errors
- Fix any overly restrictive policies

---

## Security Assessment

### Current Risk Level: 🔴 CRITICAL

**Without RLS**:
- Any authenticated user can access ALL data
- Data breach via API inspection
- No defense against malicious clients
- Violation of data privacy regulations (GDPR, CCPA)

**With RLS Enabled**:
- Database-level security
- Defense in depth (app layer + database layer)
- Compliance with data privacy requirements
- Protection against API abuse

---

## Recommendations

### Immediate (Before Production):
1. ✅ Enable RLS on all tables
2. ✅ Test all policies manually
3. ✅ Update Android app if needed
4. ✅ Document RLS policies

### Follow-up (Post-Deployment):
1. Set up monitoring for RLS policy violations
2. Regular security audits every quarter
3. Penetration testing
4. Add database activity logging

---

## Conclusion

**Status**: RLS is currently DISABLED - this is a critical security vulnerability.

**Action Required**: Execute RLS_ENABLE_PRODUCTION.sql and complete manual testing before any production deployment.

**Estimated Time**: 3-4 hours for implementation + testing

**Next Steps**:
1. Review this audit with team
2. Schedule RLS enablement
3. Prepare test users and data
4. Execute SQL script
5. Run manual tests
6. Document results
