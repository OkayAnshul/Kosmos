# RBAC Testing Checklist - Quick Reference

**Date:** 2025-10-26
**Supabase Project:** https://krbfvekgqbcwjgntepip.supabase.co
**SQL Editor:** https://krbfvekgqbcwjgntepip.supabase.co/project/krbfvekgqbcwjgntepip/sql

---

## How to Test

### Method 1: All-in-One (Recommended - 5 minutes)
1. Open SQL Editor: https://krbfvekgqbcwjgntepip.supabase.co/project/krbfvekgqbcwjgntepip/sql
2. Open file: `RBAC_MANUAL_TEST_SCRIPT.sql`
3. Copy entire contents
4. Paste into Supabase SQL Editor
5. Click **"Run"**
6. Scroll through results and check each section below

### Method 2: Step-by-Step (15 minutes)
Run each section from `RBAC_MANUAL_TEST_SCRIPT.sql` individually

---

## Verification Checklist

### ✅ Step 1: Database Setup
**Query:** `SELECT table_name FROM information_schema.tables...`

**Expected Result:** 7 tables shown
- [ ] chat_rooms
- [ ] messages
- [ ] project_members ← RBAC table
- [ ] projects ← RBAC table
- [ ] task_comments
- [ ] tasks
- [ ] users

**Status:** ____________________

---

### ✅ Step 2: Create Test User (ADMIN)
**Query:** `INSERT INTO users... admin@rbactest.kosmos`

**Expected Result:** 1 row inserted
- [ ] User ID: user-admin-001
- [ ] Email: admin@rbactest.kosmos
- [ ] Display name: Test Admin User

**Status:** ____________________

---

### ✅ Step 3: Create Project with Auto-ADMIN
**Query:** `INSERT INTO projects... INSERT INTO project_members...`

**Expected Result:** 2 rows inserted (project + membership)
- [ ] Project: RBAC Verification Project
- [ ] Owner: admin@rbactest.kosmos
- [ ] Owner role: ADMIN
- [ ] invited_by: NULL (owner auto-added)
- [ ] is_active: true

**Status:** ____________________

---

### ✅ Step 4: Add MANAGER (Role Hierarchy)
**Query:** `INSERT INTO users... manager@rbactest.kosmos`

**Expected Result:** 2 total members
- [ ] Row 1: admin@rbactest.kosmos, ADMIN, NULL
- [ ] Row 2: manager@rbactest.kosmos, MANAGER, admin@rbactest.kosmos

**Status:** ____________________

---

### ✅ Step 5: Add MEMBER
**Query:** `INSERT INTO users... member@rbactest.kosmos`

**Expected Result:** 3 total members ordered by role weight
- [ ] admin@rbactest.kosmos, ADMIN, weight=3
- [ ] manager@rbactest.kosmos, MANAGER, weight=2
- [ ] member@rbactest.kosmos, MEMBER, weight=1

**Status:** ____________________

---

### ✅ Step 6: Task Creation with Role Tracking
**Query:** `INSERT INTO tasks... (ADMIN creates, assigns to MANAGER)`

**Expected Result:** 1 task created
- [ ] Title: Implement User Authentication
- [ ] created_by_name: Test Admin User
- [ ] created_by_role: ADMIN ← Role tracking field
- [ ] assigned_to_name: Test Manager User
- [ ] assigned_to_role: MANAGER ← Role tracking field
- [ ] Priority: HIGH

**Status:** ____________________

---

### ✅ Step 7: MANAGER Creates Task for MEMBER
**Query:** `INSERT INTO tasks... (MANAGER creates, assigns to MEMBER)`

**Expected Result:** 2 total tasks
- [ ] Task 1: ADMIN → MANAGER (HIGH priority)
- [ ] Task 2: MANAGER → MEMBER (MEDIUM priority)
- [ ] Both tasks have role fields populated correctly

**Status:** ____________________

---

### ✅ Step 8: Business Rule - Cannot Remove Last ADMIN
**Query:** `SELECT... COUNT admin... Add second ADMIN`

**Expected Result:** Business rule verified
- [ ] Initially: 1 active ADMIN
- [ ] Validation: "BLOCKED: Cannot remove last ADMIN"
- [ ] After adding second ADMIN: 2 active ADMINs
- [ ] Validation: "ALLOWED: Another ADMIN exists"

**Status:** ____________________

---

### ✅ Step 9: Permission Verification
**Query:** `SELECT... permission lists for each role`

**Expected Result:** Different permission sets
- [ ] ADMIN: Has all permissions
- [ ] MANAGER: Has ~20 permissions (VIEW, EDIT, INVITE, REMOVE, CREATE, ASSIGN, etc.)
- [ ] MEMBER: Has ~10 permissions (VIEW, CREATE_TASKS, EDIT_OWN_TASKS, etc.)

**Status:** ____________________

---

### ✅ Step 10: Final Summary
**Query:** `SELECT... comprehensive project overview`

**Expected Result:** Complete project stats
- [ ] project_name: RBAC Verification Project
- [ ] status: ACTIVE
- [ ] visibility: PRIVATE
- [ ] owner_email: admin@rbactest.kosmos
- [ ] total_members: 4
- [ ] admin_count: 2
- [ ] manager_count: 1
- [ ] member_count: 1
- [ ] total_tasks: 2

**Status:** ____________________

---

## Success Criteria

### Phase 1A is 100% COMPLETE when:

**Core RBAC Features:**
- [ ] All 7 database tables exist
- [ ] Projects can be created
- [ ] Owner automatically becomes ADMIN
- [ ] Members can be added with different roles (ADMIN, MANAGER, MEMBER)
- [ ] Role hierarchy enforced (ADMIN > MANAGER > MEMBER)

**Role Tracking:**
- [ ] Tasks store `created_by_role` field
- [ ] Tasks store `assigned_to_role` field
- [ ] Role fields populated correctly for all tasks

**Business Rules:**
- [ ] Cannot remove last ADMIN from project
- [ ] Second ADMIN allows first ADMIN removal
- [ ] Permission sets differ by role

**Data Integrity:**
- [ ] Foreign key relationships work (user_id, project_id)
- [ ] Indexes created for performance
- [ ] Constraints prevent invalid data (role must be ADMIN/MANAGER/MEMBER)

---

## Troubleshooting

### Issue: "Relation does not exist"
**Solution:** Run SQL setup scripts from `SUPABASE_SQL_SETUP_QUICK_START.md`

### Issue: "Duplicate key violation"
**Solution:** Test data already exists. Run cleanup section at bottom of test script.

### Issue: "Foreign key violation"
**Solution:** Check that parent records exist (users before project_members, projects before tasks)

### Issue: No results returned
**Solution:** Verify project_id and user_id match in queries

---

## After Testing

### If All Tests Pass:
1. Update `DEVELOPMENT_LOGBOOK.md`:
   - Mark Phase 1A as 100% complete ✅
   - Add "RBAC Backend Testing Complete" section
   - Update overall progress

2. Optional: Clean up test data
   - Run cleanup queries at bottom of `RBAC_MANUAL_TEST_SCRIPT.sql`
   - Or keep for future reference

3. Move to Phase 2:
   - User Discovery (find users by email/name)
   - Chat Room UI with project scope
   - Real-time messaging

### If Tests Fail:
1. Note which step failed
2. Check error message in Supabase
3. Verify SQL scripts from `SUPABASE_SQL_SETUP_QUICK_START.md` were all run
4. Check for typos in queries
5. Ask for help with specific error

---

## Quick Test Command (Alternative)

If you prefer to test from terminal using psql:

```bash
# Install psql if needed
sudo pacman -S postgresql-libs  # For Arch Linux

# Get connection string from Supabase Dashboard > Settings > Database > Connection String

# Run test script
psql "postgresql://postgres:[YOUR-PASSWORD]@[YOUR-HOST]/postgres" -f RBAC_MANUAL_TEST_SCRIPT.sql
```

---

## Time Estimate

- **Method 1 (All-in-One):** 5 minutes
- **Method 2 (Step-by-Step):** 15 minutes
- **Review results:** 5 minutes
- **Update documentation:** 5 minutes

**Total:** 15-30 minutes to complete full RBAC verification

---

**Last Updated:** 2025-10-26
**Status:** Ready to run
**Next Step:** Open Supabase SQL Editor and run the test script!
