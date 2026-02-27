# Deployment Checklist: Members Sync Fix

## ⚡ Quick Reference

**Time Required**: 5-10 minutes
**Risk Level**: LOW (database-only, no code changes)
**Rollback**: Available (see migration script)

---

## 📋 Pre-Deployment Checklist

### ✅ Preparation
- [ ] Read `FIX_MEMBERS_SYNC_QUICKSTART.md` for overview
- [ ] Have Supabase dashboard access ready
- [ ] Have Android device/emulator running
- [ ] Have Android Studio open for logcat monitoring
- [ ] Backup recommended (optional): `pg_dump` or Supabase backup feature

### ✅ Files Ready
- [ ] `documents/04-DATABASE/FIX_PROJECT_MEMBERS_UPDATED_AT.sql` - Migration script
- [ ] `documents/04-DATABASE/VERIFY_MEMBERS_SYNC_FIX.sql` - Verification queries
- [ ] `FIX_MEMBERS_SYNC_QUICKSTART.md` - Deployment guide
- [ ] `MEMBERS_SYNC_FIX_SUMMARY.md` - Technical summary

---

## 🚀 Deployment Steps (Follow in Order)

### Step 1: Pre-Migration Verification (1 minute)

**Action**: Verify current state (migration not run yet)

**Location**: Supabase SQL Editor

**Run**:
```sql
-- Quick status check
SELECT
    CASE
        WHEN EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'project_members' AND column_name = 'updated_at'
        )
        THEN '✅ Migration complete - updated_at column exists'
        ELSE '❌ Migration not run - updated_at column missing'
    END as status;
```

**Expected Result**: `❌ Migration not run - updated_at column missing`

**If Result Shows "Migration complete"**:
- Migration already run (skip to Step 3)
- OR column exists but trigger missing (run diagnostic from `VERIFY_MEMBERS_SYNC_FIX.sql`)

**Checklist**:
- [ ] Ran pre-migration check
- [ ] Confirmed column doesn't exist (expected)
- [ ] Ready to proceed with migration

---

### Step 2: Run Migration (2 minutes)

**Action**: Add `updated_at` column and trigger

**Location**: Supabase SQL Editor

**Run**:
1. Open `documents/04-DATABASE/FIX_PROJECT_MEMBERS_UPDATED_AT.sql`
2. Copy entire file contents
3. Paste into Supabase SQL Editor
4. Click **Run** (or Ctrl+Enter)

**Expected Output**:
```
✅ ALTER TABLE (add column)
✅ UPDATE 27 (or your member count)
✅ ALTER TABLE (set NOT NULL)
✅ CREATE FUNCTION
✅ CREATE TRIGGER
✅ [Verification queries run automatically]
```

**If Errors Occur**:
- Column already exists → See Step 1 troubleshooting
- Permission denied → Check Supabase user has ALTER TABLE rights
- Syntax error → Copy script exactly, no modifications

**Checklist**:
- [ ] Migration script executed successfully
- [ ] No error messages in output
- [ ] Verification queries show expected results
- [ ] All automatic checks passed

---

### Step 3: Post-Migration Verification (2 minutes)

**Action**: Verify migration completed correctly

**Location**: Supabase SQL Editor

**Run**:
```sql
-- Full diagnostic report
DO $$
DECLARE
    column_exists BOOLEAN;
    trigger_exists BOOLEAN;
    function_exists BOOLEAN;
    total_records INT;
    null_count INT;
    migration_complete BOOLEAN;
BEGIN
    -- Check column
    SELECT EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'project_members' AND column_name = 'updated_at'
    ) INTO column_exists;

    -- Check trigger
    SELECT EXISTS(
        SELECT 1 FROM information_schema.triggers
        WHERE event_object_table = 'project_members'
          AND trigger_name = 'project_members_updated_at_trigger'
    ) INTO trigger_exists;

    -- Check function
    SELECT EXISTS(
        SELECT 1 FROM information_schema.routines
        WHERE routine_name = 'update_project_members_updated_at'
    ) INTO function_exists;

    -- Check data integrity
    SELECT COUNT(*), COUNT(*) - COUNT(updated_at)
    INTO total_records, null_count
    FROM project_members;

    -- Determine overall status
    migration_complete := column_exists AND trigger_exists AND function_exists AND (null_count = 0);

    -- Report
    RAISE NOTICE '========================================';
    RAISE NOTICE 'MIGRATION STATUS REPORT';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Column exists: %', column_exists;
    RAISE NOTICE 'Trigger exists: %', trigger_exists;
    RAISE NOTICE 'Function exists: %', function_exists;
    RAISE NOTICE 'Total records: %', total_records;
    RAISE NOTICE 'Records with NULL updated_at: %', null_count;
    RAISE NOTICE '========================================';

    IF migration_complete THEN
        RAISE NOTICE 'RESULT: ✅ Migration COMPLETE';
    ELSE
        RAISE NOTICE 'RESULT: ❌ Migration INCOMPLETE';
    END IF;

    RAISE NOTICE '========================================';
END $$;
```

**Expected Output**:
```
Column exists: true
Trigger exists: true
Function exists: true
Total records: 27 (or your count)
Records with NULL updated_at: 0
RESULT: ✅ Migration COMPLETE
```

**Checklist**:
- [ ] All checks return `true`
- [ ] No NULL `updated_at` values
- [ ] Status shows "Migration COMPLETE"
- [ ] Ready to test app

---

### Step 4: Clear App Data (30 seconds)

**Action**: Force fresh sync with new column

**Location**: Android Device/Emulator

**Steps**:
1. Open **Settings** on device
2. Navigate to **Apps** (or **Applications**)
3. Find and tap **Kosmos**
4. Tap **Storage** (or **Storage & cache**)
5. Tap **Clear Data** (or **Clear Storage**)
6. Confirm deletion

**Why This is Required**:
- Old sync timestamps will filter out all data
- Clearing data forces `since = null` (full sync)
- Without this, UI will still show empty even with fix

**Checklist**:
- [ ] Navigated to app settings
- [ ] Found Kosmos app
- [ ] Cleared data successfully
- [ ] Confirmed deletion prompt

---

### Step 5: Test App Sync (3 minutes)

**Action**: Launch app and verify sync works

**Location**: Android Device + Android Studio Logcat

**Steps**:
1. Launch Kosmos app
2. Log in with your account
3. Wait for sync to complete (10-30 seconds)
4. Monitor logcat for sync messages

**Expected Logcat Output**:
```
✅ Synced X members for project <name>
✅ Synced Y chat rooms for project <name>
✅ Synced Z tasks for project <name>
✅ [1/27] Completed: <project_name>
...
✅ [27/27] Completed: <project_name>
✅ Initial sync completed successfully in XXXms
   Project Data: 27/27 synced (0 errors)
```

**❌ Should NOT See**:
```
❌ column project_members.updated_at does not exist
⚠️ Sync completed with errors - 27/27 projects had errors
```

**Checklist**:
- [ ] App launched successfully
- [ ] Logged in without errors
- [ ] Sync completed (watch logcat)
- [ ] No PostgreSQL errors in logs
- [ ] All projects synced successfully

---

### Step 6: Verify UI (2 minutes)

**Action**: Check all screens show data

**Location**: Kosmos App UI

**Screens to Check**:
1. **Project List Screen**
   - [ ] Shows all 27 projects
   - [ ] Project cards display correctly
   - [ ] Can tap to open project

2. **Project Workspace → Members Tab**
   - [ ] Shows member list (not empty)
   - [ ] Member names and roles display
   - [ ] Avatar/initials show correctly

3. **Project Workspace → Tasks Tab**
   - [ ] Shows project tasks (if any exist)
   - [ ] Task cards display correctly
   - [ ] Status badges show

4. **Project Workspace → Chats Tab**
   - [ ] Shows chat rooms (if any exist)
   - [ ] Chat list not empty
   - [ ] Can tap to open chat

5. **Project Workspace → Activity Tab**
   - [ ] Shows activity log (if any exists)
   - [ ] Timeline displays correctly
   - [ ] Recent activities visible

**If Empty UI**:
- Check Supabase data exists (run `SELECT * FROM project_members LIMIT 5;`)
- Check RLS policies not blocking (run `SELECT * FROM project_members WHERE user_id = '<your_id>';`)
- Clear app data again (force re-sync)

**Checklist**:
- [ ] Project List populated
- [ ] Members tab shows data
- [ ] Tasks tab shows data (if exists)
- [ ] Chats tab shows data (if exists)
- [ ] Activity tab shows data (if exists)

---

### Step 7: Test Incremental Sync (Optional, 2 minutes)

**Action**: Verify incremental sync works correctly

**Location**: Supabase + Kosmos App

**Steps**:
1. **Add new member in Supabase**:
   ```sql
   INSERT INTO project_members (project_id, user_id, role, joined_at, is_active, updated_at)
   VALUES (
       '<existing_project_id>',
       '<existing_user_id>',
       'MEMBER',
       (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
       true,
       (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
   );
   ```

2. **Trigger sync in app**:
   - Pull to refresh in Project List
   - OR wait 30 seconds (real-time sync)

3. **Verify new member appears**:
   - Open project → Members tab
   - New member should be in list
   - Check logcat: `✅ Synced 1 member` (only new one)

**Expected Behavior**:
- Only new member fetched (not full list)
- Incremental sync working efficiently
- Real-time update appears in UI

**Checklist**:
- [ ] Added new member in Supabase
- [ ] Triggered sync in app
- [ ] New member appeared in UI
- [ ] Incremental sync confirmed in logcat

---

## ✅ Success Criteria

### Database Success
- ✅ Column `project_members.updated_at` exists (bigint, NOT NULL)
- ✅ Trigger `project_members_updated_at_trigger` exists
- ✅ All records have non-null `updated_at` values
- ✅ Trigger test passes (updates timestamp on change)

### Sync Success
- ✅ No PostgreSQL error 42703 in logcat
- ✅ All 27 projects sync without errors
- ✅ Sync completes in <30 seconds
- ✅ Logcat shows "✅ Synced X members" for all projects

### UI Success
- ✅ Project List shows all projects
- ✅ Members tab populated
- ✅ Tasks tab populated (if data exists)
- ✅ Chats tab populated (if data exists)
- ✅ Activity tab populated (if data exists)

### Performance Success
- ✅ Incremental sync fetches only changed members
- ✅ Subsequent syncs complete in <5 seconds
- ✅ Real-time updates appear immediately
- ✅ No full re-sync on every launch

---

## 🚨 Troubleshooting

### Issue: Migration fails with "column already exists"
**Solution**: Column was already added. Run diagnostic (Step 3) to verify migration is complete.

### Issue: Still seeing sync errors after migration
**Checklist**:
1. Verified migration completed? (Run Step 3 diagnostic)
2. Cleared app data? (Step 4 - REQUIRED)
3. Logged in again? (Fresh authentication)
4. Waited for sync? (Can take 10-30 seconds)

### Issue: UI still empty after sync
**Possible Causes**:
1. No data in Supabase
   - Run: `SELECT COUNT(*) FROM project_members;`
   - Expected: > 0
2. RLS policies blocking
   - Run: `SELECT * FROM project_members WHERE user_id = '<your_user_id>';`
   - Expected: Returns rows
3. Sync timestamps issue
   - Clear app data again (force full sync)

### Issue: Trigger not working
**Verify**:
```sql
-- Test trigger manually
UPDATE project_members
SET role = role
WHERE id = (SELECT id FROM project_members LIMIT 1)
RETURNING id, updated_at;
-- updated_at should be current timestamp
```

If trigger doesn't work:
- Re-run STEP 4 and STEP 5 from migration script
- Check function exists: `SELECT * FROM pg_proc WHERE proname = 'update_project_members_updated_at';`

---

## 🔄 Rollback Procedure (If Needed)

**When to Rollback**:
- Migration causes data issues
- App behavior worse after migration
- Need to revert to previous state

**Steps**:
1. Open Supabase SQL Editor
2. Run rollback script (from `FIX_PROJECT_MEMBERS_UPDATED_AT.sql`):
   ```sql
   -- Remove trigger
   DROP TRIGGER IF EXISTS project_members_updated_at_trigger ON project_members;

   -- Remove function
   DROP FUNCTION IF EXISTS update_project_members_updated_at();

   -- Remove column
   ALTER TABLE project_members DROP COLUMN IF EXISTS updated_at;
   ```
3. Clear app data again
4. Report issue with details

**After Rollback**:
- App will return to previous state (with sync errors)
- No data loss (column removal doesn't affect other columns)
- Can re-run migration after fixing issues

---

## 📊 Monitoring Post-Deployment

### First 24 Hours
- [ ] Monitor sync success rates (should be 100%)
- [ ] Check sync times (should be <5 seconds after initial)
- [ ] Verify no PostgreSQL errors in logs
- [ ] Confirm incremental sync working (only fetches changes)

### First Week
- [ ] Monitor member updates propagate correctly
- [ ] Verify real-time sync working
- [ ] Check performance (no slowdowns)
- [ ] Confirm offline-to-online recovery works

### Metrics to Track
- Sync success rate: Target 100%
- Sync duration: Target <5s for incremental
- Error rate: Target 0%
- Incremental sync efficiency: Should fetch only changed records

---

## 📚 Reference Documents

**Quick Start**: `FIX_MEMBERS_SYNC_QUICKSTART.md`
**Technical Summary**: `MEMBERS_SYNC_FIX_SUMMARY.md`
**Migration Script**: `documents/04-DATABASE/FIX_PROJECT_MEMBERS_UPDATED_AT.sql`
**Verification Script**: `documents/04-DATABASE/VERIFY_MEMBERS_SYNC_FIX.sql`

**Code References**:
- `SupabaseProjectMemberDataSource.kt:115` - Sync query
- `ProjectMember.kt` - Data model
- `ProjectRepository.kt` - Sync orchestration

---

## ✅ Deployment Complete Checklist

- [ ] Pre-migration verification completed (Step 1)
- [ ] Migration script executed successfully (Step 2)
- [ ] Post-migration verification passed (Step 3)
- [ ] App data cleared (Step 4)
- [ ] App sync tested successfully (Step 5)
- [ ] UI verified showing data (Step 6)
- [ ] Incremental sync tested (Step 7 - optional)
- [ ] All success criteria met
- [ ] Monitoring plan in place
- [ ] Documentation updated

**Sign-off**:
- Date: ______________
- Deployed by: ______________
- Verification by: ______________
- Status: ✅ SUCCESS / ❌ ROLLBACK / ⚠️ PARTIAL

---

**Last Updated**: 2026-01-26
**Next Review**: After 24 hours of production use
**Escalation**: If issues occur, see Troubleshooting section or rollback
