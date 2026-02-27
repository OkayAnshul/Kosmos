# Project Creation - Schema Mismatch Issue

**Date:** January 7, 2026
**Status:** ⚠️ CRITICAL - Database Migration Required
**Issue:** Projects not appearing after creation

---

## Root Cause Identified

The logs reveal the actual problem:

```
SupabasePr...DataSource: Error inserting project
io.github.jan.supabase.postgrest.exception.PostgrestRestException:
Could not find the 'business_model' column of 'projects' in the schema cache
Code: PGRST204
```

### What's Happening

1. **Room Database (Local):** ✅ Has all wizard fields
   - category, deadline, website_url, github_url, project_motive
   - tech_stack, tags, business_model, target_audience
   - industry_tags, open_source_license

2. **Supabase Database (Remote):** ❌ Missing these fields
   - Only has basic fields: id, name, description, owner_id, etc.
   - Does NOT have any of the wizard fields

3. **When creating a project:**
   ```
   ✅ Project saves to Room successfully
   ✅ Members save to Room successfully
   ❌ Project sync to Supabase FAILS (missing columns)
   ✅ Members sync to Supabase successfully
   ✅ Local project creation "completes"
   ✅ Sync from Supabase fetches 16 projects
   ❌ New project NOT in those 16 (never made it to Supabase!)
   ✅ Flow emits "16 projects" (without the new one)
   ```

4. **Result:**
   - User sees success message
   - But new project isn't in Supabase
   - So it doesn't appear in the synced list
   - Looks like project creation is broken (but it's actually a schema mismatch)

---

## Evidence from Logs

**Project Creation (Line 6):**
```
ProjectRepository: ✅ Project saved to Room: 71a40d58-404e-4fb4-b8b9-d57228ae8b20
```

**Supabase Sync Failure (Line 17-18):**
```
SupabasePr...DataSource: Error inserting project (Ask Gemini)
io.github.jan.supabase.postgrest.exception.PostgrestRestException:
Could not find the 'business_model' column of 'projects' in the schema cache
Code: PGRST204
```

**Project Completion Without Supabase (Line 342):**
```
ProjectRepository: 🎉 Project creation complete: hehw with 6 members
```

**Sync Finds Old Projects Only (Line 344-423):**
```
ProjectRepository: Found 16 projects for user
ProjectRepository: ✅ Project sync complete: 16 succeeded, 0 failed
```

**Note:** The newly created project (71a40d58-404e-4fb4-b8b9-d57228ae8b20) is NOT in the 16 synced projects because it never made it to Supabase!

**Flow Emissions (Lines 345-427):**
```
📦 Projects updated: 16 projects (repeated ~50 times)
```

All showing 16 projects - the new one is missing.

---

## Why The Code Changes Didn't Help

Our fixes were correct for a different issue:

1. ✅ **Added `syncProjectsFromSupabase()`** - Works perfectly, syncs 16 projects
2. ✅ **Refresh Flow after creation** - Works perfectly, re-fetches 16 projects
3. ✅ **Added logging** - Helped us diagnose the real issue!

But the real problem is **the new project never makes it to Supabase** due to schema mismatch.

---

## Solution

### Migration Required

Run `ADD_PROJECT_WIZARD_FIELDS_MIGRATION.sql` in Supabase SQL Editor

This migration adds 11 missing columns to the `projects` table:

1. **category** - Project category enum (TECH, SOCIAL, BUSINESS, OTHER)
2. **deadline** - Optional deadline timestamp
3. **website_url** - Optional website URL
4. **github_url** - Optional GitHub repository URL
5. **project_motive** - Optional project goals/motive
6. **tech_stack** - JSON array of technologies
7. **tags** - JSON array of general tags
8. **business_model** - Optional business model description
9. **target_audience** - Optional target audience
10. **industry_tags** - JSON array of industry tags
11. **open_source_license** - Optional open source license

### After Migration

Once the columns are added:

1. **Existing projects in Room** will sync to Supabase successfully
2. **New projects** will be created in both Room AND Supabase
3. **Sync will fetch** the newly created project
4. **Flow will emit** updated count (17, 18, 19... projects)
5. **UI will display** the new project immediately

---

## How This Happened

**Timeline:**

1. Project wizard was designed with extended fields
2. `ProjectCreationData` class was created with all fields (ProjectRepository.kt:51-69)
3. Room `Project` entity was updated to include these fields
4. **Supabase table was NOT updated** to match
5. Code assumed both databases had same schema
6. Offline-first architecture allowed local creation to "succeed"
7. Silent Supabase sync failure went unnoticed

**Why it was silent:**

Looking at `createProjectWithMembers()` (ProjectRepository.kt:235-256):

```kotlin
try {
    val projectSyncResult = supabaseProjectDataSource.insert(project)
    if (projectSyncResult.isFailure) {
        Log.w(TAG, "⚠️ Failed to sync project to Supabase (will retry)", ...)
    } else {
        Log.d(TAG, "✅ Project synced to Supabase")
    }
} catch (syncException: Exception) {
    Log.w(TAG, "⚠️ Sync to Supabase failed (offline or network error), will retry later", ...)
}
```

The error was caught and logged as a WARNING, not an ERROR. The method still returned `Result.success(project)` because the local save succeeded.

This is **correct behavior for offline-first architecture** - but we need schema parity for it to work!

---

## Testing After Migration

### Before Migration:
1. Create project → Success message ✅
2. Check project list → Not there ❌
3. Check Supabase → Not there ❌
4. Check logs → "Could not find the 'business_model' column" ❌

### After Migration:
1. Create project → Success message ✅
2. Check logs → "✅ Project synced to Supabase" ✅
3. Check project list → Shows new project ✅
4. Check Supabase → Project exists with all fields ✅

---

## Files Created

1. **ADD_PROJECT_WIZARD_FIELDS_MIGRATION.sql**
   - Adds 11 missing columns to Supabase projects table
   - Includes verification queries
   - Includes rollback instructions
   - Safe to run multiple times (uses IF NOT EXISTS)

2. **This document (PROJECT_CREATION_SCHEMA_MISMATCH.md)**
   - Explains the root cause
   - Provides evidence from logs
   - Documents solution

---

## Next Steps

### Immediate (Required):
1. ⚠️ **Run migration in Supabase SQL Editor**
2. Test project creation
3. Verify project appears in list
4. Check Supabase directly to confirm data

### Follow-Up (Recommended):
1. Add schema validation tests
2. Add Supabase sync error handling to UI
3. Consider adding a "Sync Status" indicator
4. Document schema in one place (source of truth)
5. Add migration checklist for future schema changes

---

## Lessons Learned

1. **Keep schemas in sync:** Room and Supabase must have matching schemas
2. **Test sync failures:** Offline-first is great, but silent failures are dangerous
3. **Better error visibility:** Schema errors should be more visible to developers
4. **Migration checklist:** Always update BOTH databases when adding fields
5. **Logs are critical:** Without detailed logging, this would have been impossible to debug

---

**Status:** ⚠️ AWAITING DATABASE MIGRATION
**Action Required:** Run ADD_PROJECT_WIZARD_FIELDS_MIGRATION.sql
**Expected Result:** Projects will sync to Supabase and appear in list

---

*Generated: January 7, 2026*
*Issue Discovered: Log analysis of project creation flow*
*Migration Script: ADD_PROJECT_WIZARD_FIELDS_MIGRATION.sql*
