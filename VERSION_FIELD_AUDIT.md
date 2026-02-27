# Version Field Audit - Repository vs Supabase Schema

**Date**: 2026-01-24
**Purpose**: Verify which repositories use optimistic locking and if Supabase supports it
**Status**: ❌ **INCOMPLETE IMPLEMENTATION**

---

## 📊 Summary

| Repository | Uses Version? | Entity Has Version? | Supabase Has Version? | Status |
|------------|---------------|---------------------|----------------------|--------|
| **UserRepository** | ✅ Yes | ✅ User.version | ✅ Yes (P0-01) | ✅ **COMPLETE** |
| **ProjectRepository** | ✅ Yes | ✅ Project.version | ❌ **MISSING** | ❌ **BROKEN** |
| **TaskRepository** | ✅ Yes | ✅ Task.version | ❌ **MISSING** | ❌ **BROKEN** |
| **ChatRepository** | ❌ No | ❌ N/A | N/A | ⚠️ **NOT IMPLEMENTED** |
| **AuthRepository** | N/A | N/A | N/A | - |

**Overall**: 1/3 working, 2/3 broken

---

## ✅ WORKING: UserRepository

### Implementation Status: COMPLETE

**Entity: User.kt (line 62)**
```kotlin
data class User(
    // ... other fields
    val version: Int = 1  // ✅ Optimistic locking field
)
```

**Repository: UserRepository.kt**

**Line 126:** Sets initial version
```kotlin
val userWithTimestamp = user.copy(
    createdAt = user.createdAt ?: System.currentTimeMillis(),
    version = 1 // ✅ New user starts at version 1
)
```

**Usage Pattern:**
- Create: Sets version = 1
- Update: NOT SHOWN (need to verify if version is incremented)

**Supabase Schema:**
```sql
-- From P0-01_ADD_USER_VERSION_FIELD.sql
ALTER TABLE public.users
ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;
```

**Status:** ✅ **FULLY ALIGNED**

---

## ❌ BROKEN: ProjectRepository

### Implementation Status: BROKEN (No Supabase version column)

**Entity: Project.kt (lines 97-100)**
```kotlin
data class Project(
    // ... other fields

    /**
     * P1-11: Version field for optimistic locking
     * Incremented on every update to detect conflicts
     */
    val version: Int = 1,
    // ... more fields
)
```

**Repository: ProjectRepository.kt (lines 504-507)**
```kotlin
// P1-11: Update locally with incremented version
val updatedProject = project.copy(
    updatedAt = System.currentTimeMillis(),
    version = project.version + 1  // ✅ Increments version
)
projectDao.updateProject(updatedProject)
```

**Supabase Schema:**
```sql
-- ❌ NO MIGRATION EXISTS
-- Expected location: documents/04-DATABASE/ADD_PROJECT_VERSION_FIELD.sql
-- Status: MISSING
```

**Consequence:**
1. ❌ **Local optimistic locking works** (Room detects conflicts)
2. ❌ **Supabase sync fails** (unknown column 'version')
3. ❌ **Multi-device conflicts use last-write-wins** (data loss risk)
4. ❌ **P1-11 feature incomplete**

**Test Scenario:**
```kotlin
// Device A: Edit project
projectRepository.updateProject(project.copy(name = "New Name"))
// Room: version 1 → 2 ✅
// Supabase: ERROR - column "version" does not exist ❌

// Device B: Edit same project
projectRepository.updateProject(project.copy(description = "New Desc"))
// Room: version 1 → 2 ✅
// Supabase: ERROR - column "version" does not exist ❌

// Result: Both updates fail to sync, data inconsistent
```

**Fix Required:**
```sql
-- Create: documents/04-DATABASE/ADD_PROJECT_VERSION_FIELD.sql

ALTER TABLE public.projects
ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_projects_version
ON public.projects(version);

COMMENT ON COLUMN public.projects.version IS
'Version number for optimistic locking (P1-11). Incremented on each update to detect concurrent modifications.';

-- Verify
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns
WHERE table_name = 'projects' AND column_name = 'version';
```

---

## ❌ BROKEN: TaskRepository

### Implementation Status: BROKEN (No Supabase version column)

**Entity: Task.kt (lines 112-116)**
```kotlin
data class Task(
    // ... other fields

    /**
     * P1-11: Version field for optimistic locking
     * Incremented on every update to detect conflicts
     */
    val version: Int = 1,
    // ... more fields
)
```

**Repository: TaskRepository.kt (lines 336-340)**
```kotlin
// P1-11: Increment version on successful update
val updatedTask = task.copy(
    updatedAt = System.currentTimeMillis(),
    version = task.version + 1  // ✅ Increments version
)
```

**Supabase Schema:**
```sql
-- ❌ NO MIGRATION EXISTS
-- Expected location: documents/04-DATABASE/ADD_TASK_VERSION_FIELD.sql
-- Status: MISSING
```

**Consequence:**
1. ❌ **Local optimistic locking works** (Room detects conflicts)
2. ❌ **Supabase sync fails** (unknown column 'version')
3. ❌ **Multi-device task edits corrupt data** (last-write-wins)
4. ❌ **P1-11 feature incomplete**

**Test Scenario:**
```kotlin
// User A: Change task status TODO → IN_PROGRESS
taskRepository.updateTask(task.copy(status = TaskStatus.IN_PROGRESS))
// Room: version 1 → 2 ✅
// Supabase: ERROR - column "version" does not exist ❌

// User B (same task): Assign to different user
taskRepository.updateTask(task.copy(assignedToId = "user-2"))
// Room: version 1 → 2 ✅
// Supabase: ERROR - column "version" does not exist ❌

// Result: Both updates stuck in sync queue, never reach Supabase
```

**Fix Required:**
```sql
-- Create: documents/04-DATABASE/ADD_TASK_VERSION_FIELD.sql

ALTER TABLE public.tasks
ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_tasks_version
ON public.tasks(version);

COMMENT ON COLUMN public.tasks.version IS
'Version number for optimistic locking (P1-11). Incremented on each update to detect concurrent modifications.';

-- Verify
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns
WHERE table_name = 'tasks' AND column_name = 'version';
```

---

## ⚠️ NOT IMPLEMENTED: ChatRepository

### Implementation Status: No Optimistic Locking

**Entity: ChatRoom.kt**
```kotlin
data class ChatRoom(
    // ... fields
    // ❌ NO version field
)
```

**Repository: ChatRepository.kt**
```kotlin
// ❌ No version increment logic
// Uses simple last-write-wins
```

**Assessment:**
- ChatRoom edits are less critical than Tasks/Projects
- Concurrent edits rare (only admins edit chat settings)
- **Priority**: P3 (nice-to-have)

**Recommendation**: Add version field if chat editing becomes common

---

## ⚠️ NOT IMPLEMENTED: Message Entity

### Implementation Status: No Optimistic Locking

**Entity: Message.kt**
```kotlin
data class Message(
    // ... fields
    // ❌ NO version field
)
```

**Assessment:**
- Messages are append-only (rarely edited)
- Edit conflicts handled by `isEdited` + `editedAt` timestamps
- **Priority**: P4 (not needed)

**Recommendation**: Keep as-is, messages don't need versioning

---

## 🔍 Detailed Version Field Usage Analysis

### UserRepository.kt - Version Usage

**Found References:**
1. **Line 126** - Create user with version = 1
```kotlin
version = 1 // New user starts at version 1
```

**Missing:**
- ❓ **Update logic** - No code found that increments version on user updates
- ⚠️ **Potential bug**: User updates may not increment version

**Action Required:**
Search for user update logic and verify version increment:
```bash
# Check if user updates increment version
grep -n "updateUser\|update(user" UserRepository.kt
```

---

### ProjectRepository.kt - Version Usage

**Found References:**
1. **Lines 504-507** - Update project with version increment
```kotlin
val updatedProject = project.copy(
    updatedAt = System.currentTimeMillis(),
    version = project.version + 1
)
```

**Implementation:** ✅ **CORRECT**
- Increments version on every update
- Follows optimistic locking pattern

**Supabase Support:** ❌ **MISSING**
- Need to create migration

---

### TaskRepository.kt - Version Usage

**Found References:**
1. **Lines 336-340** - Update task with version increment
```kotlin
val updatedTask = task.copy(
    updatedAt = System.currentTimeMillis(),
    version = task.version + 1
)
```

**Implementation:** ✅ **CORRECT**
- Increments version on every update
- Follows optimistic locking pattern

**Supabase Support:** ❌ **MISSING**
- Need to create migration

---

## 🚨 Critical Findings

### 1. Incomplete P1-11 Implementation

**Phase 5 Summary claims:**
> "✅ P1-11: Conflict resolution - Optimistic locking prevents data loss"

**Reality:**
- ✅ User table: WORKING (Supabase migration exists)
- ❌ Project table: BROKEN (no Supabase column)
- ❌ Task table: BROKEN (no Supabase column)

**Impact:**
- **Phase 5 is NOT complete** (only 33% done)
- **Optimistic locking doesn't work across devices**
- **Data loss risk still exists for projects and tasks**

### 2. User Update Missing Version Increment

**Issue:** UserRepository.kt line 126 sets version=1 for new users, but no code found that increments version on updates.

**Test Required:**
```kotlin
// Create user
val user = User(id = "1", email = "test@example.com", username = "test", version = 1)
userRepository.createUser(user)

// Update user
val updated = user.copy(displayName = "New Name")
userRepository.updateUser(updated)

// Check version
val fromDb = userRepository.getUserById("1")
// Expected: fromDb.version == 2
// Actual: fromDb.version == 1 (BUG!)
```

**Action:** Find `updateUser` implementation and add version increment

---

## 📋 Migration Scripts Needed

### Priority 1: Add Task Version Field

**File:** `documents/04-DATABASE/ADD_TASK_VERSION_FIELD.sql`

```sql
-- Add version column to tasks table for optimistic locking
-- Implements P1-11 conflict resolution feature
-- Date: 2026-01-24

ALTER TABLE public.tasks
ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;

-- Create index for conflict detection queries
CREATE INDEX IF NOT EXISTS idx_tasks_version
ON public.tasks(version);

-- Add column comment
COMMENT ON COLUMN public.tasks.version IS
'Version number for optimistic locking (P1-11). Incremented on each update to detect concurrent modifications.';

-- Verify migration
SELECT
    column_name,
    data_type,
    column_default,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'tasks'
  AND column_name = 'version';

-- Expected output:
-- column_name | data_type | column_default | is_nullable
-- version     | integer   | 1              | NO

-- Test conflict detection
-- (Run this after migration to verify it works)
/*
-- Set task version to 5
UPDATE tasks SET version = 5 WHERE id = '<test-task-id>';

-- Try to update with old version (should fail in app logic)
-- Expected: App detects version mismatch (current=5, provided=1)
*/
```

### Priority 2: Add Project Version Field

**File:** `documents/04-DATABASE/ADD_PROJECT_VERSION_FIELD.sql`

```sql
-- Add version column to projects table for optimistic locking
-- Implements P1-11 conflict resolution feature
-- Date: 2026-01-24

ALTER TABLE public.projects
ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;

-- Create index for conflict detection queries
CREATE INDEX IF NOT EXISTS idx_projects_version
ON public.projects(version);

-- Add column comment
COMMENT ON COLUMN public.projects.version IS
'Version number for optimistic locking (P1-11). Incremented on each update to detect concurrent modifications.';

-- Verify migration
SELECT
    column_name,
    data_type,
    column_default,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'projects'
  AND column_name = 'version';

-- Expected output:
-- column_name | data_type | column_default | is_nullable
-- version     | integer   | 1              | NO
```

---

## 🧪 Testing Checklist

**After adding version columns to Supabase:**

### Test 1: Single Device Update
- [ ] Create task with version=1
- [ ] Update task, verify version increments to 2 in Room
- [ ] Verify version=2 synced to Supabase
- [ ] Update again, verify version=3 in both

### Test 2: Conflict Detection (Two Devices)
- [ ] Device A: Load task (version=1)
- [ ] Device B: Load same task (version=1)
- [ ] Device A: Update task → version=2
- [ ] Device B: Attempt update with version=1
- [ ] Verify: Device B gets ConflictException
- [ ] Verify: User shown "Task modified by someone else" message

### Test 3: Offline → Online Sync
- [ ] Disable network
- [ ] Update task offline (version 1→2 in Room)
- [ ] Enable network
- [ ] Verify sync succeeds, version=2 in Supabase

### Test 4: User Update Version Increment
- [ ] Create user (version=1)
- [ ] Update user display name
- [ ] **Verify version increments to 2** ⚠️ (may fail if bug exists)

---

## 📊 Recommendations

### Immediate (Before Production)
1. ✅ **Create task version migration** (Priority 1)
2. ✅ **Create project version migration** (Priority 1)
3. ⚠️ **Verify user update logic** increments version
4. ✅ **Test all three entities** for conflict detection

### Short-term (Post-Launch)
5. ⏭️ **Add ChatRoom version field** (if concurrent edits become common)
6. ⏭️ **Add version to project_members** (if role changes conflict)

### Long-term (Architecture Improvement)
7. ⏭️ **Abstract optimistic locking** into base repository
8. ⏭️ **Add version validation** in RLS policies
9. ⏭️ **Monitor conflict frequency** with analytics

---

## 🎯 Updated Phase 5 Status

**Original Claim:**
> ✅ Phase 5: Complete (100%) - P1-11 Optimistic locking implemented

**Actual Status:**
> ⏳ Phase 5: Partial (33%) - P1-11 INCOMPLETE
> - ✅ User table: Working
> - ❌ Project table: Broken (no Supabase column)
> - ❌ Task table: Broken (no Supabase column)

**Time to Complete:**
- Create 2 SQL migrations: 30 minutes
- Run migrations in Supabase: 10 minutes
- Test conflict detection: 1 hour
- Fix user update bug (if exists): 30 minutes
- **Total: 2-3 hours**

---

**Last Updated**: 2026-01-24
**Next Action**: Run GET_COMPLETE_SCHEMA.sql to verify current Supabase state
**Owner**: Development Team
