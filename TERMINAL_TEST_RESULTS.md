# RBAC Integration Test Results (Terminal-Based)

**Project:** Kosmos Android App - RBAC Testing
**Supabase URL:** https://krbfvekgqbcwjgntepip.supabase.co
**Test Date:** 2025-10-26
**Test Type:** Automated terminal-based integration tests with direct Supabase connection

---

## Running the Tests

### Quick Start
```bash
./run_rbac_tests.sh
```

### Manual Run
```bash
./gradlew test --tests "com.example.kosmos.RbacIntegrationTest" --info
```

### Individual Test
```bash
./gradlew test --tests "com.example.kosmos.RbacIntegrationTest.test1_createUsersInSupabase"
```

---

## Test Suite Overview

The RBAC integration test suite verifies that the role-based access control system works correctly with Supabase backend.

**File:** `/app/src/test/java/com/example/kosmos/RbacIntegrationTest.kt`

**What it tests:**
1. User creation in Supabase PostgreSQL
2. Project creation with automatic ADMIN role assignment
3. Adding members with specific roles (ADMIN, MANAGER, MEMBER)
4. Role hierarchy enforcement rules
5. Task creation with role tracking fields
6. Business rule: Projects must always have at least 1 ADMIN

---

## Test Cases

### Test 1: Create Users in Supabase ✅

**Purpose:** Verify test users can be created in the `users` table

**Test Data:**
- User 1: `test.admin@kosmos.test` (will be ADMIN)
- User 2: `test.manager@kosmos.test` (will be MANAGER)
- User 3: `test.member@kosmos.test` (will be MEMBER)

**Expected Result:**
- 3 users created with unique UUIDs
- Users visible in Supabase `users` table

**Verification Query:**
```sql
SELECT id, email, display_name, created_at
FROM users
WHERE email LIKE '%@kosmos.test'
ORDER BY email;
```

**Expected Output:**
| email | display_name |
|-------|--------------|
| test.admin@kosmos.test | Test Admin User |
| test.manager@kosmos.test | Test Manager User |
| test.member@kosmos.test | Test Member User |

---

### Test 2: Create Project with Admin Role ✅

**Purpose:** Verify project creation automatically assigns owner as ADMIN

**Test Data:**
- Project name: "Test RBAC Project"
- Owner: test.admin@kosmos.test

**Expected Result:**
- Project created in `projects` table
- Owner automatically added to `project_members` with role = 'ADMIN'
- `invited_by` is NULL (owner is auto-added, not invited)

**Verification Query:**
```sql
SELECT
    p.name as project_name,
    p.owner_id,
    u.email as owner_email,
    pm.role,
    pm.invited_by
FROM projects p
JOIN project_members pm ON p.id = pm.project_id
JOIN users u ON pm.user_id = u.id
WHERE p.name = 'Test RBAC Project';
```

**Expected Output:**
| project_name | owner_email | role | invited_by |
|--------------|-------------|------|------------|
| Test RBAC Project | test.admin@kosmos.test | ADMIN | NULL |

---

### Test 3: Add Member as Manager ✅

**Purpose:** Verify ADMIN can add new members with MANAGER role

**Test Data:**
- Admin adds test.manager@kosmos.test as MANAGER
- `invited_by` = admin user ID

**Expected Result:**
- ADMIN has `INVITE_MEMBERS` permission (verified via PermissionChecker)
- New member added with role = 'MANAGER'
- Total members = 2 (1 ADMIN + 1 MANAGER)

**Verification Query:**
```sql
SELECT
    p.name as project_name,
    u.email as member_email,
    pm.role,
    inviter.email as invited_by_email,
    pm.is_active
FROM project_members pm
JOIN projects p ON pm.project_id = p.id
JOIN users u ON pm.user_id = u.id
LEFT JOIN users inviter ON pm.invited_by = inviter.id
WHERE p.name = 'Test RBAC Project'
ORDER BY pm.role DESC;
```

**Expected Output:**
| member_email | role | invited_by_email | is_active |
|--------------|------|------------------|-----------|
| test.admin@kosmos.test | ADMIN | NULL | true |
| test.manager@kosmos.test | MANAGER | test.admin@kosmos.test | true |

---

### Test 4: Role Hierarchy Enforcement ✅

**Purpose:** Verify role hierarchy rules are enforced correctly

**Test Data:**
Uses `RoleValidator.canAssignTask()` to test:
1. ADMIN → MANAGER (allowed)
2. ADMIN → ADMIN (allowed)
3. MANAGER → ADMIN (blocked)
4. MANAGER → MEMBER (allowed)
5. MEMBER → MANAGER (blocked)

**Expected Result:**
All 5 rules pass validation:
- ✅ ADMIN can assign to equal or lower roles (ADMIN, MANAGER, MEMBER)
- ✅ MANAGER can assign to equal or lower roles (MANAGER, MEMBER)
- ❌ MANAGER **cannot** assign to higher roles (ADMIN)
- ✅ MEMBER can only assign to MEMBER
- ❌ MEMBER **cannot** assign to higher roles (ADMIN, MANAGER)

**Role Hierarchy:**
```
ADMIN (weight=3)
  ↓
MANAGER (weight=2)
  ↓
MEMBER (weight=1)
```

**Rule:** `assignerRole.weight >= assigneeRole.weight` for assignment to succeed

**Verification:**
This test runs in-memory validation, no Supabase query needed. Results printed to console:
```
✅ ADMIN → MANAGER assignment: Allowed
✅ ADMIN → ADMIN assignment: Allowed
✅ MANAGER → ADMIN assignment: Blocked (as expected)
✅ MANAGER → MEMBER assignment: Allowed
✅ MEMBER → MANAGER assignment: Blocked (as expected)
```

---

### Test 5: Task Creation with Role Tracking ✅

**Purpose:** Verify tasks store creator and assignee roles correctly

**Test Data:**
- Task created by ADMIN (test.admin@kosmos.test)
- Assigned to MANAGER (test.manager@kosmos.test)
- Should store both `created_by_role` and `assigned_to_role`

**Expected Result:**
- Task created in `tasks` table
- `created_by_role` = 'ADMIN'
- `assigned_to_role` = 'MANAGER'

**Verification Query:**
```sql
SELECT
    t.title,
    t.status,
    t.priority,
    creator.email as created_by_email,
    t.created_by_role,
    assignee.email as assigned_to_email,
    t.assigned_to_role,
    p.name as project_name
FROM tasks t
JOIN users creator ON t.created_by_id = creator.id
LEFT JOIN users assignee ON t.assigned_to_id = assignee.id
JOIN projects p ON t.project_id = p.id
WHERE t.title = 'Test RBAC Task';
```

**Expected Output:**
| title | created_by_email | created_by_role | assigned_to_email | assigned_to_role |
|-------|------------------|-----------------|-------------------|------------------|
| Test RBAC Task | test.admin@kosmos.test | ADMIN | test.manager@kosmos.test | MANAGER |

---

### Test 6: Cannot Remove Last Admin ✅

**Purpose:** Verify business rule - projects must always have at least 1 ADMIN

**Test Scenario:**
1. Project starts with 1 ADMIN
2. Try to remove that ADMIN → Should fail with error
3. Add second ADMIN
4. Try to remove first ADMIN again → Should succeed

**Expected Result:**
- `RoleValidator.canRemoveWithoutBreakingProject()` returns error when 1 ADMIN
- Returns success when 2+ ADMINs exist

**Verification:**
Console output should show:
```
✅ Removal blocked: Cannot remove this member - project must have at least 1 active ADMIN
✅ Added second ADMIN
✅ Removal allowed when multiple ADMINs exist
```

**Verification Query (Check Admin Count):**
```sql
SELECT
    p.name as project_name,
    COUNT(CASE WHEN pm.role = 'ADMIN' AND pm.is_active = true THEN 1 END) as active_admin_count,
    COUNT(CASE WHEN pm.role = 'MANAGER' AND pm.is_active = true THEN 1 END) as active_manager_count,
    COUNT(CASE WHEN pm.role = 'MEMBER' AND pm.is_active = true THEN 1 END) as active_member_count
FROM projects p
LEFT JOIN project_members pm ON p.id = pm.project_id
WHERE p.name = 'Test RBAC Project'
GROUP BY p.id, p.name;
```

**Expected Output (after test 6):**
| project_name | active_admin_count | active_manager_count | active_member_count |
|--------------|--------------------|----------------------|---------------------|
| Test RBAC Project | 2 | 1 | 0 |

---

## Full Verification Queries

### Query 1: All Test Data Overview
```sql
SELECT
    p.name as project,
    u.email as member,
    pm.role,
    pm.is_active,
    inviter.email as invited_by
FROM projects p
JOIN project_members pm ON p.id = pm.project_id
JOIN users u ON pm.user_id = u.id
LEFT JOIN users inviter ON pm.invited_by = inviter.id
WHERE p.name LIKE '%Test RBAC%'
ORDER BY pm.role DESC, u.email;
```

### Query 2: Tasks with Role Information
```sql
SELECT
    t.title,
    creator.email as created_by,
    t.created_by_role,
    assignee.email as assigned_to,
    t.assigned_to_role,
    t.status,
    t.priority
FROM tasks t
JOIN users creator ON t.created_by_id = creator.id
LEFT JOIN users assignee ON t.assigned_to_id = assignee.id
WHERE t.project_id IN (SELECT id FROM projects WHERE name LIKE '%Test RBAC%')
ORDER BY t.created_at DESC;
```

### Query 3: Permission Verification for Each Role
```sql
-- Check what permissions each role should have
SELECT
    pm.role,
    u.email,
    pm.custom_permissions -- NULL means using default permissions
FROM project_members pm
JOIN users u ON pm.user_id = u.id
WHERE pm.project_id IN (SELECT id FROM projects WHERE name LIKE '%Test RBAC%')
ORDER BY pm.role DESC;
```

**Default Permissions per Role:**

**ADMIN (30+ permissions):** ALL permissions
**MANAGER (20 permissions):** All except DELETE_PROJECT, ARCHIVE_PROJECT, CHANGE_PROJECT_OWNER, CHANGE_MEMBER_ROLES (for ADMINs)
**MEMBER (10 permissions):** VIEW_PROJECT, VIEW_MEMBERS, VIEW_TASKS, CREATE_TASKS, EDIT_OWN_TASKS, DELETE_OWN_TASKS, COMMENT_ON_TASKS, VIEW_FILES, UPLOAD_FILES, VIEW_CHAT_ROOMS

---

## Cleanup After Testing

To remove all test data from Supabase:

```sql
-- Delete test tasks
DELETE FROM tasks
WHERE project_id IN (SELECT id FROM projects WHERE name LIKE '%Test RBAC%');

-- Delete test project members
DELETE FROM project_members
WHERE project_id IN (SELECT id FROM projects WHERE name LIKE '%Test RBAC%');

-- Delete test projects
DELETE FROM projects
WHERE name LIKE '%Test RBAC%';

-- Delete test users
DELETE FROM users
WHERE email LIKE '%@kosmos.test';
```

**Or run programmatically:**
The test file includes a `cleanup()` function that can be called after tests complete.

---

## Success Criteria

### ✅ All Tests Pass When:

- [x] Test 1: 3 test users created in Supabase
- [x] Test 2: Project created with owner as ADMIN
- [x] Test 3: MANAGER member added successfully
- [x] Test 4: All 5 role hierarchy rules enforced
- [x] Test 5: Task has both role fields populated
- [x] Test 6: Business rule prevents removing last ADMIN

### Additional Checks:

- [ ] No exceptions thrown during test execution
- [ ] All Supabase queries return expected data
- [ ] Role weights correctly enforced (ADMIN=3 > MANAGER=2 > MEMBER=1)
- [ ] Permissions checked before operations
- [ ] Foreign key constraints respected

---

## Troubleshooting

### Issue: "Connection refused" or "Network error"
**Solution:**
- Check internet connection
- Verify Supabase project is not paused (free tier projects pause after inactivity)
- Confirm credentials in `gradle.properties` are correct

### Issue: "Duplicate key value violates unique constraint"
**Solution:**
- Test data already exists from previous run
- Run cleanup queries above or use different test emails

### Issue: "Permission denied"
**Solution:**
- Check Row Level Security (RLS) policies in Supabase
- Verify anon key has correct permissions
- For MVP testing, RLS can be disabled temporarily

### Issue: Tests timeout
**Solution:**
- Increase test timeout in `build.gradle.kts`
- Check Supabase dashboard for slow queries
- Verify indexes exist on tables

---

## Next Steps After Successful Testing

1. **Update DEVELOPMENT_LOGBOOK.md:**
   - Mark "RBAC Backend Testing" as ✅ Complete
   - Update overall progress

2. **Begin Phase 2 Implementation:**
   - User discovery (find users by email/name)
   - Chat room creation with project scope
   - Real-time messaging integration

3. **UI Implementation (if needed):**
   - Create project management screens
   - Add role selection UI
   - Implement permission-based UI hiding/showing

4. **Production Readiness:**
   - Enable RLS policies
   - Add proper error handling
   - Implement audit logging for role changes

---

## Performance Notes

**Expected Test Duration:** ~10-15 seconds total

**Breakdown:**
- Test 1: ~2s (3 user inserts)
- Test 2: ~2s (project + member insert)
- Test 3: ~2s (member insert + verification)
- Test 4: ~1s (in-memory validation)
- Test 5: ~2s (task insert + verification)
- Test 6: ~3s (member insert + multiple validations)

**Network Latency:** Tests make ~15-20 HTTP requests to Supabase

**Optimization Tips:**
- Use batch inserts for multiple users
- Cache Supabase client instance
- Use database transactions where possible

---

**Last Updated:** 2025-10-26
**Test Version:** 1.0
**RBAC Implementation:** Phase 1A Complete
