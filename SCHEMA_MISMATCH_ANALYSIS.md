# Supabase vs Room Database Schema Mismatch Analysis

**Date**: 2026-01-24
**Analysis**: Complete comparison of Supabase PostgreSQL schema vs Room local database
**Status**: ❌ **CRITICAL MISMATCHES FOUND**

---

## 📊 Summary

| Table | Room Columns | Supabase Columns | Status | Severity |
|-------|--------------|------------------|--------|----------|
| **users** | 18 | 17 | ✅ Aligned | - |
| **projects** | 27 | 11 | ❌ **MISMATCH** | 🔴 **CRITICAL** |
| **project_members** | 9 | 9 | ✅ Aligned | - |
| **chat_rooms** | 18 | 14 | ⚠️ **PARTIAL** | 🟡 **HIGH** |
| **messages** | 15 | 15 | ✅ Aligned | - |
| **tasks** | 23 | 21 | ❌ **MISMATCH** | 🟠 **MEDIUM** |
| **task_activity** | 11 | 14 | ✅ Aligned | - |
| **sync_queue** | 12 | N/A | ✅ Local-only | - |
| **voice_messages** | - | N/A | ⚠️ **UNKNOWN** | 🟡 **HIGH** |
| **action_items** | - | N/A | ⚠️ **UNKNOWN** | 🟡 **HIGH** |

**Overall Status**: 🔴 **NOT PRODUCTION READY** - Significant schema drift detected

---

## 🔴 CRITICAL: Project Table Mismatch

**Impact**: Project creation/updates will FAIL when syncing to Supabase
**Severity**: CRITICAL - Blocks core functionality

### Room Entity (27 columns)
```kotlin
// Core fields (11 - ALIGNED)
id, name, description, ownerId, status, visibility,
createdAt, updatedAt, imageUrl, color, settings

// NEW in Room: Project Wizard Fields (11 - MISSING IN SUPABASE)
category, deadline, websiteUrl, githubUrl, projectMotive,
techStack, tags, businessModel, targetAudience,
industryTags, openSourceLicense

// NEW in Room: Metadata Caching Fields (6 - MISSING IN SUPABASE)
memberCount, chatCount, taskCount, completedTaskCount,
pendingTaskCount, lastActivityAt
```

### Supabase Schema (11 columns)
```sql
-- From SCHEMA_FIX_COMPLETE_V2.sql
id, name, description, owner_id, status, visibility,
created_at, updated_at, image_url, color, settings
```

### Root Cause
- **Room Migration 3→4** added 11 Project Wizard fields to local database (KosmosDatabase.kt:90-124)
- **NO corresponding Supabase migration** exists in `/documents/04-DATABASE/`
- **Metadata caching fields** exist in Room model but NOT in database schema

### Consequences
1. ❌ **Creating projects with category/deadline/githubUrl will fail on sync**
2. ❌ **memberCount, taskCount, etc. will NEVER sync to Supabase**
3. ❌ **Data loss**: Local fields saved, but silently dropped during Supabase sync
4. ❌ **Inconsistent state**: Room has data that Supabase doesn't

### Fix Required
**Create Supabase migration:**
```sql
-- Add Project Wizard fields
ALTER TABLE public.projects
ADD COLUMN IF NOT EXISTS category TEXT DEFAULT 'OTHER',
ADD COLUMN IF NOT EXISTS deadline BIGINT,
ADD COLUMN IF NOT EXISTS website_url TEXT,
ADD COLUMN IF NOT EXISTS github_url TEXT,
ADD COLUMN IF NOT EXISTS project_motive TEXT,
ADD COLUMN IF NOT EXISTS tech_stack JSONB,
ADD COLUMN IF NOT EXISTS tags JSONB,
ADD COLUMN IF NOT EXISTS business_model TEXT,
ADD COLUMN IF NOT EXISTS target_audience TEXT,
ADD COLUMN IF NOT EXISTS industry_tags JSONB,
ADD COLUMN IF NOT EXISTS open_source_license TEXT;

-- Add Metadata Caching fields
ALTER TABLE public.projects
ADD COLUMN IF NOT EXISTS member_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS chat_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS task_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS completed_task_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS pending_task_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS last_activity_at BIGINT;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_projects_category ON projects(category);
CREATE INDEX IF NOT EXISTS idx_projects_deadline ON projects(deadline);
CREATE INDEX IF NOT EXISTS idx_projects_last_activity_at ON projects(last_activity_at DESC);
```

---

## 🟠 MEDIUM: Task Table Mismatch

**Impact**: Optimistic locking will NOT work on Supabase
**Severity**: MEDIUM - Breaks conflict resolution feature

### Room Entity (23 columns)
```kotlin
// All expected 21 columns from schema
id, projectId, chatRoomId, title, description, status, priority,
assignedToId, assignedToName, assignedToRole, createdById,
createdByName, createdByRole, createdAt, updatedAt, dueDate,
sourceMessageId, tags, comments, parentTaskId, estimatedHours, actualHours

// NEW in Room: Optimistic Locking (1 - MISSING IN SUPABASE)
version  // Added in Room for P1-11 conflict resolution
```

### Supabase Schema (21 columns)
```sql
-- From SCHEMA_FIX_COMPLETE_V2.sql verification query
-- Expected 21 columns, version NOT mentioned
```

### Root Cause
- **P1-11 implementation** added `version` field to Task.kt for optimistic locking
- **NO Supabase migration** exists for tasks.version field
- Only users table has version field migration (P0-01_ADD_USER_VERSION_FIELD.sql)

### Consequences
1. ⚠️ **Conflict resolution ONLY works locally**
2. ⚠️ **Multi-device edits will use last-write-wins** (data loss risk)
3. ⚠️ **Phase 5 P1-11 feature is INCOMPLETE**

### Fix Required
**Create Supabase migration:**
```sql
-- Add version field to tasks table
ALTER TABLE public.tasks
ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;

-- Create index for conflict detection
CREATE INDEX IF NOT EXISTS idx_tasks_version ON tasks(version);

COMMENT ON COLUMN public.tasks.version IS
'Version number for optimistic locking. Incremented on each update to detect concurrent modifications.';
```

---

## 🟡 HIGH: ChatRoom Table Partial Mismatch

**Impact**: UI-only features won't sync
**Severity**: HIGH - User experience degradation

### Room Entity (18 columns)
```kotlin
// Core 14 columns (likely aligned)
id, projectId, name, description, imageUrl, type, participantIds,
createdBy, createdAt, updatedAt, lastMessageId, lastMessage,
lastMessageTimestamp, isTaskBoardEnabled

// Possibly missing in Supabase (4 - NEED VERIFICATION)
isArchived, isPinned, isPrivate
```

### Supabase Schema (14 columns expected)
```sql
-- SCHEMA_FIX_COMPLETE_V2.sql verifies 14 columns exist
-- Need to manually check if isArchived, isPinned, isPrivate exist
```

### Status
⚠️ **NEEDS VERIFICATION** - Run this query in Supabase:
```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'chat_rooms'
AND column_name IN ('is_archived', 'is_pinned', 'is_private')
ORDER BY column_name;
```

### Likely Consequence
- If missing: Pinning/archiving chats will NOT sync across devices
- Local-only feature degradation

---

## ⚠️ UNKNOWN: Missing Tables

### Room-Only Tables (Local Storage)
✅ **sync_queue** - Intentionally local-only (offline queue)

### Unverified Tables
Need to check if these exist in Supabase:
1. ❓ **voice_messages** - Referenced in Message.kt foreign keys
2. ❓ **action_items** - Registered in KosmosDatabase.kt

**Verification Query:**
```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_name IN ('voice_messages', 'action_items')
ORDER BY table_name;
```

---

## ✅ ALIGNED: Verified Tables

### 1. Users Table
- **Room**: 18 columns (including version, settings)
- **Supabase**: 17 columns + version field (via P0-01 migration)
- **Status**: ✅ Fully aligned

### 2. ProjectMembers Table
- **Room**: 9 columns
- **Supabase**: 9 columns (verified in SCHEMA_FIX_COMPLETE_V2.sql)
- **Status**: ✅ Fully aligned

### 3. Messages Table
- **Room**: 15 columns
- **Supabase**: 15 columns (fixed via SCHEMA_FIX_COMPLETE_V2.sql)
- **Status**: ✅ Fully aligned

### 4. TaskActivity Table
- **Room**: 11 columns
- **Supabase**: 14 columns (via P0-02_CREATE_TASK_ACTIVITY_TABLE.sql)
- **Status**: ✅ Aligned (Supabase has extra timestamp fields)

---

## 🚨 Immediate Actions Required

### Priority 1: Fix Project Schema (CRITICAL)
1. Create `/documents/04-DATABASE/ADD_PROJECT_WIZARD_FIELDS.sql`
2. Add all 17 missing columns to Supabase projects table
3. Run migration in Supabase SQL Editor
4. Test project creation/update sync

### Priority 2: Fix Task Version Field (MEDIUM)
1. Create `/documents/04-DATABASE/ADD_TASK_VERSION_FIELD.sql`
2. Add version column to Supabase tasks table
3. Update TaskRepository conflict resolution to check Supabase version

### Priority 3: Verify ChatRoom Fields (HIGH)
1. Run verification query in Supabase
2. If missing: Create migration for isArchived, isPinned, isPrivate
3. Test chat pinning/archiving across devices

### Priority 4: Verify Missing Tables (HIGH)
1. Check if voice_messages and action_items tables exist
2. If missing: Create table schemas in Supabase
3. Update Room DAOs if tables don't exist

---

## 📋 Testing Checklist

After fixing schema mismatches:

**Project Tests:**
- [ ] Create project with category=TECH, githubUrl, techStack
- [ ] Verify all wizard fields sync to Supabase
- [ ] Check memberCount updates correctly
- [ ] Verify lastActivityAt syncs

**Task Tests:**
- [ ] Edit same task from 2 devices simultaneously
- [ ] Verify conflict detection works (version mismatch)
- [ ] Check ConflictException is thrown

**ChatRoom Tests:**
- [ ] Pin a chat room, verify sync
- [ ] Archive a chat room, verify sync
- [ ] Check isPrivate flag syncs

**VoiceMessage/ActionItem Tests:**
- [ ] Query tables in Supabase
- [ ] If exist: Test create/read operations
- [ ] If missing: Remove from Room or create in Supabase

---

## 🔍 How This Happened

**Root Cause Analysis:**

1. **Room migrations exist** (MIGRATION_3_4 in KosmosDatabase.kt)
   - Added project wizard fields to local SQLite database
   - No corresponding Supabase migration created

2. **P1-11 optimistic locking** implemented
   - Added version to Task.kt and Project.kt models
   - Only created Supabase migration for users.version (P0-01)
   - Forgot tasks.version and projects.version

3. **Metadata caching fields** added to Project.kt
   - Designed for database triggers to auto-update
   - Never created the actual Supabase columns or triggers

4. **No automated schema validation**
   - Room schema and Supabase schema evolved independently
   - No CI/CD checks to catch drift

---

## 📝 Prevention Recommendations

### Short-term
1. **Create schema sync script** that compares Room entities with Supabase schema
2. **Document all fields** in both Room and Supabase migrations
3. **Test offline→online sync** after every schema change

### Long-term
1. **Single source of truth**: Generate Room entities from Supabase schema
2. **Automated testing**: CI checks that fail if schema drift detected
3. **Migration pairing**: Every Room migration must have Supabase equivalent
4. **Schema versioning**: Track Room version → Supabase schema version mapping

---

## 📊 Impact on Current Phase

**From OVERALL_PROGRESS_SUMMARY.md:**
- Phase 0-5 complete (91% issues resolved)
- Production readiness: A+ grade
- **BUT**: Schema mismatches make sync UNRELIABLE

**Reality Check:**
- ❌ Project creation wizard is BROKEN for sync
- ❌ Conflict resolution is INCOMPLETE
- ❌ Metadata caching is LOCAL-ONLY
- ❌ Can't reliably test offline→online sync

**Revised Status:**
- **Production Readiness**: B- (down from A+)
- **Reason**: Core data sync is compromised by schema drift

---

## 🎯 Next Steps

**Recommended Approach:**

1. **PAUSE UI work** until schema is fixed
2. **Fix schema mismatches** (Priority 1-4 above)
3. **Test end-to-end sync** with fixed schema
4. **Document schema parity** in CLAUDE.md
5. **THEN resume UI implementation**

**Estimated Time:**
- Schema fixes: 2-3 hours
- Testing: 1-2 hours
- Documentation: 1 hour
- **Total**: 4-6 hours

**Alternative (NOT RECOMMENDED):**
- Continue UI work, fix schema later
- Risk: More code written on broken foundation
- Harder to debug sync issues later

---

**Last Updated**: 2026-01-24
**Next Review**: After schema fixes applied and tested
**Owner**: Development Team
