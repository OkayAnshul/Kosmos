# RLS Security Audit - Kosmos Android App

**Date**: 2026-01-24
**Phase**: Phase 2 - Database & Security (P0-09)
**Auditor**: Claude Code
**Status**: CRITICAL ISSUES FOUND ⚠️

---

## Executive Summary

**Overall Status**: ⚠️ **NEEDS IMMEDIATE FIX**

- **Tables Audited**: 7 tables
- **Policies Reviewed**: 24 policies
- **Critical Issues**: 4
- **Warnings**: 2
- **Recommendations**: 3

**Key Findings**:
1. ✅ 5/7 tables have correct RLS policies
2. ❌ **Task table policies use WRONG column names** (CRITICAL)
3. ❌ **Task table policies use OUTDATED join logic** (CRITICAL)
4. ⚠️ Task activity policies exist but in separate file (needs consolidation)

---

## Tables Audited

### 1. users Table ✅ PASS
**Status**: Correct
**Policies**: 4 (SELECT x2, INSERT, UPDATE)

#### Policies:
- ✅ `users_select_own` - Users can view their own profile
- ✅ `users_select_project_members` - Users can view project collaborators
- ✅ `users_insert_own` - Users can create their own profile (signup)
- ✅ `users_update_own` - Users can update their own profile

#### Security Level: GOOD
- No cross-user data leaks
- Proper self-access controls
- Project-based visibility working correctly

---

### 2. projects Table ✅ PASS
**Status**: Correct
**Policies**: 4 (SELECT, INSERT, UPDATE, DELETE)

#### Policies:
- ✅ `projects_select_member` - Users can view projects they're members of
- ✅ `projects_insert_owner` - Users can create new projects
- ✅ `projects_update_admin` - Owners and admins can update projects
- ✅ `projects_delete_owner` - Only owners can delete projects

#### Security Level: GOOD
- Proper role-based access control
- Owner-only deletion protection
- Member-based visibility

---

### 3. project_members Table ✅ PASS
**Status**: Correct
**Policies**: 4 (SELECT, INSERT, UPDATE, DELETE)

#### Policies:
- ✅ `project_members_select` - Users can view members of their projects
- ✅ `project_members_insert` - Admins/owners can add members
- ✅ `project_members_update` - Admins/owners can update member roles
- ✅ `project_members_delete` - Admins/owners can remove members

#### Security Level: GOOD
- Admin-only member management
- Project isolation enforced

---

### 4. chat_rooms Table ✅ PASS
**Status**: Correct
**Policies**: 4 (SELECT, INSERT, UPDATE, DELETE)

#### Policies:
- ✅ `chat_rooms_select` - Participants and project members can view
- ✅ `chat_rooms_insert` - Project members can create chat rooms
- ✅ `chat_rooms_update` - Project admins can update chat rooms
- ✅ `chat_rooms_delete` - Project admins can delete chat rooms

#### Security Level: GOOD
- Dual access control (participants OR project members)
- Admin-only modification
- Project-based isolation

---

### 5. messages Table ✅ PASS
**Status**: Correct
**Policies**: 4 (SELECT, INSERT, UPDATE, DELETE)

#### Policies:
- ✅ `messages_select` - Users can view messages in their chat rooms
- ✅ `messages_insert` - Users can send messages to chat rooms they participate in
- ✅ `messages_update` - Users can update their own messages
- ✅ `messages_delete` - Users can delete their own messages

#### Security Level: GOOD
- Chat room-based access control
- Self-edit/delete only
- Proper sender validation

---

### 6. tasks Table ❌ CRITICAL - NEEDS FIX
**Status**: **INCORRECT POLICIES**
**Policies**: 4 (SELECT, INSERT, UPDATE, DELETE)

#### Critical Issues:

**Issue 1: Wrong Column Names**
- ❌ Policy uses `assigned_to_user_id` → Should be `assigned_to_id`
- ❌ Policy uses `created_by` → Should be `created_by_id`

**Evidence**:
```kotlin
// From Task.kt (lines 71-91)
@SerialName("assigned_to_id")
val assignedToId: String? = null,

@SerialName("created_by_id")
val createdById: String = "",
```

**Issue 2: Outdated Join Logic**
- ❌ Policies join through `chat_rooms` table
- ✅ Tasks now have direct `project_id` field

**Evidence**:
```kotlin
// From Task.kt (lines 54-55)
@SerialName("project_id")
val projectId: String = "",
```

**Current (WRONG) Policy**:
```sql
CREATE POLICY "tasks_select" ON tasks
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM chat_rooms
      JOIN project_members ON chat_rooms.project_id = project_members.project_id
      WHERE chat_rooms.id = tasks.chat_room_id  -- ❌ WRONG
        AND project_members.user_id = auth.uid()
    )
  );
```

**Should Be**:
```sql
CREATE POLICY "tasks_select" ON tasks
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM project_members
      WHERE project_members.project_id = tasks.project_id  -- ✅ CORRECT
        AND project_members.user_id = auth.uid()
    )
  );
```

#### Impact: HIGH
- Current policies may **block legitimate access** (users can't see their own tasks)
- Current policies may **allow unauthorized access** (if column names don't match)
- **ALL task operations may be failing** due to these mismatches

---

### 7. task_activity Table ⚠️ WARNING - Needs Consolidation
**Status**: Policies exist but in separate file
**Policies**: 3 (SELECT, INSERT, DELETE)
**File**: `P0-02_CREATE_TASK_ACTIVITY_TABLE.sql`

#### Policies:
- ✅ "Users can view activity in their projects" (SELECT)
- ✅ "Users can create activity records" (INSERT)
- ✅ "Project admins can delete activity" (DELETE)

#### Security Level: GOOD
- Proper project-based isolation
- Actor validation on insert
- Admin-only deletion

#### Recommendation:
- Consolidate these policies into `RLS_ENABLE_PRODUCTION.sql`
- Add UPDATE policy (even if restricted to nobody) for completeness

---

## Security Risks

### Critical Risks (Immediate Action Required)

**RISK-01: Task Access Control Broken**
- **Severity**: CRITICAL
- **Likelihood**: HIGH (100%)
- **Impact**: Users cannot access their own tasks OR unauthorized users can access tasks
- **Affected**: All task operations (SELECT, INSERT, UPDATE, DELETE)
- **Fix**: Update task policies with correct column names and join logic

**RISK-02: Data Leakage via Task API**
- **Severity**: HIGH
- **Likelihood**: MEDIUM (if RLS not enforced)
- **Impact**: Cross-project task visibility
- **Affected**: All users
- **Fix**: Test RLS enforcement, update policies

### Warnings

**WARNING-01: Task Activity Policies Not Centralized**
- **Severity**: LOW
- **Impact**: Harder to audit, maintain, and verify
- **Fix**: Consolidate into main RLS file

**WARNING-02: No UPDATE Policy for Task Activity**
- **Severity**: LOW
- **Impact**: Activity records should be immutable, but policy missing
- **Fix**: Add explicit UPDATE policy denying all

---

## Recommendations

### Immediate (This Session)
1. ✅ **Fix task table policies** - Update column names and join logic
2. ✅ **Test task access** - Verify users can SELECT/INSERT/UPDATE/DELETE their tasks
3. ✅ **Consolidate task_activity policies** - Add to main RLS file

### Short-Term (Within Week)
4. Add RLS policy tests (automated)
5. Add RLS monitoring/alerting
6. Document policy rationale

### Long-Term (Post-Launch)
7. Add audit logging for RLS policy violations
8. Add policy performance monitoring
9. Consider field-level RLS (e.g., hide sensitive fields)

---

## Testing Matrix

### Test Cases

| Test ID | Description | Expected Result | Status |
|---------|-------------|----------------|--------|
| T01 | User A views own tasks | ✅ Success | ⏳ Pending |
| T02 | User A views User B's tasks (different project) | ❌ Denied | ⏳ Pending |
| T03 | User A views User B's tasks (same project) | ✅ Success | ⏳ Pending |
| T04 | User A creates task in own project | ✅ Success | ⏳ Pending |
| T05 | User A creates task in project they're not member of | ❌ Denied | ⏳ Pending |
| T06 | Admin updates any project task | ✅ Success | ⏳ Pending |
| T07 | Member updates own assigned task | ✅ Success | ⏳ Pending |
| T08 | Member updates task assigned to another | ❌ Denied (unless admin) | ⏳ Pending |
| T09 | Owner deletes project task | ✅ Success | ⏳ Pending |
| T10 | Member deletes project task | ❌ Denied (unless admin) | ⏳ Pending |

### Test Procedure
1. Create two test users (User A, User B)
2. Create two projects (Project 1 - A member, Project 2 - B member)
3. Create shared project (Project 3 - both members)
4. Run all 10 test cases
5. Document results
6. Fix any failures

---

## Fixed RLS Policies

See `RLS_ENABLE_PRODUCTION_V2_FIXED.sql` for corrected policies.

---

## Conclusion

**Status**: ❌ **FAIL - Critical Issues Found**

**Action Required**:
1. Apply fixed RLS policies immediately
2. Test all policies with multiple users
3. Monitor Supabase logs for policy violations
4. Re-audit after fixes applied

**Timeline**:
- Fixing policies: 2 hours
- Testing: 2 hours
- Verification: 1 hour
- **Total**: 5 hours (vs 8 hours estimated in plan)

**Production Readiness**:
- **Before Fixes**: ❌ NOT READY
- **After Fixes**: ✅ READY (pending tests)

---

**Audit Complete**: 2026-01-24
**Next Review**: After fixes applied + tests passed
