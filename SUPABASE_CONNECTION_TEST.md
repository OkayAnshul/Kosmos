# Supabase Connection Test Guide

**Project:** Kosmos Android App with RBAC
**Supabase URL:** https://krbfvekgqbcwjgntepip.supabase.co

This guide helps you test that your app successfully connects to Supabase and the RBAC system works.

---

## Prerequisites

✅ **Before testing, ensure you've completed:**

1. **SQL Setup Complete**: Run all 7 SQL scripts from `SUPABASE_SQL_SETUP_QUICK_START.md`
2. **Gradle Configuration**: `gradle.properties` updated with real credentials (DONE ✅)
3. **App Built Successfully**: `./gradlew assembleDebug` completed (DONE ✅)
4. **Device/Emulator Running**: Android device or emulator ready

---

## Test 1: Authentication (Firebase Auth → Supabase User Sync)

### Steps:
1. Install and launch the app
2. Sign in with Google or create account
3. Check if user profile is created

### Expected Behavior:
- ✅ Firebase Auth creates user account
- ✅ App creates corresponding user in Supabase `users` table
- ✅ User profile displays correctly

### Verification in Supabase Dashboard:
1. Go to: https://krbfvekgqbcwjgntepip.supabase.co/project/krbfvekgqbcwjgntepip/editor
2. Click `users` table
3. You should see your user with:
   - `id` (UUID)
   - `email`
   - `display_name`
   - `created_at`

### If it fails:
- Check logcat for errors: `adb logcat | grep Supabase`
- Verify credentials in `build.gradle.kts` match `gradle.properties`

---

## Test 2: Create Project (RBAC Test)

### Steps:
1. In the app, navigate to Projects (or create UI to test)
2. Create a new project with name "Test Project"
3. Check if project is created with you as ADMIN

### Expected Behavior:
- ✅ Project created in `projects` table
- ✅ You're automatically added to `project_members` table with role `ADMIN`
- ✅ Project appears in your project list

### Verification in Supabase Dashboard:

**Check Projects Table:**
```sql
SELECT * FROM projects ORDER BY created_at DESC LIMIT 5;
```
Should show your "Test Project"

**Check Project Members Table:**
```sql
SELECT
    pm.*,
    u.display_name,
    p.name as project_name
FROM project_members pm
JOIN users u ON pm.user_id = u.id
JOIN projects p ON pm.project_id = p.id
ORDER BY pm.joined_at DESC;
```
Should show you as ADMIN of "Test Project"

### If it fails:
- Check permission errors in logcat
- Verify `ProjectRepository` is working
- Check Supabase real-time logs in dashboard

---

## Test 3: Add Member to Project (RBAC Hierarchy Test)

### Steps:
1. Create a second test user (or use another device)
2. As ADMIN, invite User B as MANAGER
3. Verify User B can see the project

### Expected Behavior:
- ✅ User B added to `project_members` with role `MANAGER`
- ✅ User B can view project
- ✅ User B cannot delete project (only ADMIN can)

### Verification in Supabase Dashboard:
```sql
SELECT
    p.name as project,
    u.display_name as member,
    pm.role,
    pm.is_active
FROM project_members pm
JOIN projects p ON pm.project_id = p.id
JOIN users u ON pm.user_id = u.id
WHERE p.name = 'Test Project';
```

### Test RBAC Rules:
- ✅ ADMIN can add/remove any member
- ✅ MANAGER can add MEMBERS but not ADMINS
- ✅ MEMBER cannot add or remove members

---

## Test 4: Create Task with Role Assignment (RBAC Validation)

### Steps:
1. As ADMIN, create a task
2. Assign task to MANAGER user
3. Try to have MEMBER assign task to ADMIN (should fail)

### Expected Behavior:
- ✅ ADMIN can assign to anyone (ADMIN, MANAGER, MEMBER)
- ✅ MANAGER can assign to MANAGER or MEMBER
- ✅ MEMBER can only assign to MEMBER
- ❌ MEMBER assigning to ADMIN/MANAGER should fail with error

### Verification in Supabase Dashboard:
```sql
SELECT
    t.title,
    t.status,
    t.priority,
    creator.display_name as created_by,
    t.created_by_role,
    assignee.display_name as assigned_to,
    t.assigned_to_role,
    p.name as project
FROM tasks t
JOIN users creator ON t.created_by_id = creator.id
LEFT JOIN users assignee ON t.assigned_to_id = assignee.id
JOIN projects p ON t.project_id = p.id
ORDER BY t.created_at DESC;
```

Should show:
- Task with `created_by_role` = 'ADMIN'
- Task with `assigned_to_role` = 'MANAGER'
- Both role fields populated

### If role validation fails:
- Check `RoleValidator.canAssignTask()` logic
- Check `TaskRepository.assignTask()` permission checks
- Review logcat for `PermissionDeniedException`

---

## Test 5: Real-time Sync (Future Phase)

**Status:** Deferred to Phase 2

Real-time subscriptions are currently commented out for MVP. To test when implemented:
1. Have two devices logged in to same project
2. Create task on Device A
3. Task should appear on Device B instantly

---

## Common Issues & Solutions

### Issue: "User not a member of project"
**Solution:** Verify user exists in `project_members` table with `is_active = true`

### Issue: "Permission denied" errors
**Solution:**
1. Check user's role in `project_members`
2. Verify `Permission` enum matches expected permissions
3. Check `PermissionChecker.hasPermission()` logic

### Issue: "Cannot assign to this role"
**Solution:**
1. Check role hierarchy: ADMIN(3) > MANAGER(2) > MEMBER(1)
2. Verify `RoleValidator.canAssignTask()` weight comparison
3. Ensure `ProjectRole.weight` property works correctly

### Issue: Tasks not appearing
**Solution:**
1. Verify `tasks.project_id` matches actual project ID
2. Check foreign key constraints didn't fail
3. Query Supabase directly to see if task exists

---

## Debug Commands

### Check All Your Data:
```sql
-- Your user
SELECT * FROM users WHERE email = 'your@email.com';

-- Your projects
SELECT p.*, pm.role
FROM projects p
JOIN project_members pm ON p.id = pm.project_id
JOIN users u ON pm.user_id = u.id
WHERE u.email = 'your@email.com';

-- Your tasks
SELECT t.*, p.name as project_name
FROM tasks t
JOIN projects p ON t.project_id = p.id
WHERE t.created_by_id IN (SELECT id FROM users WHERE email = 'your@email.com');
```

### Check RBAC Setup:
```sql
-- Projects with member counts
SELECT
    p.name,
    p.status,
    COUNT(DISTINCT pm.user_id) as member_count,
    COUNT(DISTINCT CASE WHEN pm.role = 'ADMIN' THEN pm.user_id END) as admin_count,
    COUNT(DISTINCT CASE WHEN pm.role = 'MANAGER' THEN pm.user_id END) as manager_count,
    COUNT(DISTINCT CASE WHEN pm.role = 'MEMBER' THEN pm.user_id END) as member_count
FROM projects p
LEFT JOIN project_members pm ON p.id = pm.project_id AND pm.is_active = true
GROUP BY p.id, p.name, p.status
ORDER BY p.created_at DESC;
```

---

## Success Criteria

### ✅ Phase 1A + Backend Setup Complete When:

- [x] User can sign in and profile syncs to Supabase
- [ ] User can create project (becomes ADMIN automatically)
- [ ] Admin can add members with roles
- [ ] Role hierarchy enforced (can't assign upward)
- [ ] Tasks store creator and assignee roles
- [ ] Permission checks work for all operations
- [ ] Build succeeds with no RBAC errors

---

## Next Steps After Testing

1. **If all tests pass:** Continue to Phase 2 (User Discovery & Chat)
2. **If issues found:** Fix bugs and re-test
3. **Document any blockers:** Update `DEVELOPMENT_LOGBOOK.md`

---

**Need Help?**
- Check `DEVELOPMENT_LOGBOOK.md` for known issues
- Review Supabase logs: https://krbfvekgqbcwjgntepip.supabase.co/project/krbfvekgqbcwjgntepip/logs/explorer
- Check Android logcat: `adb logcat | grep -E "Kosmos|Supabase|RBAC"`
