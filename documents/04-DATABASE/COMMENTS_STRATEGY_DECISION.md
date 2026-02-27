# Task Comments Storage Strategy

**Status**: 🟡 Decision Needed
**Created**: 2026-01-25
**Priority**: P1 - High (affects data consistency)

---

## 📊 Current Situation

Task comments are currently stored in **TWO places**:

1. **`tasks.comments`** - JSONB column (embedded in tasks table)
   - Type: `JSONB`
   - Structure: Array of comment objects
   - Used by: Room Task model (`val comments: String? = null`)

2. **`task_comments`** - Separate table
   - Columns: `id`, `task_id`, `user_id`, `content`, `created_at`, etc.
   - Used by: Separate query layer (not integrated with Room)

---

## ⚠️ Problem

**Dual storage creates inconsistency risk:**

- Which is the source of truth?
- Are they kept in sync?
- Repository uses embedded comments, but separate table exists
- Confusion for developers on which to use

**Current code behavior:**

```kotlin
// Task.kt model
data class Task(
    val comments: String? = null  // Embedded JSONB
)

// Room uses embedded comments
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTask(id: String): Task  // Returns task with embedded comments
}

// But Supabase has task_comments table too!
CREATE TABLE task_comments (
    id UUID PRIMARY KEY,
    task_id UUID REFERENCES tasks(id),
    user_id UUID REFERENCES users(id),
    content TEXT,
    created_at BIGINT
);
```

---

## 🎯 Decision Options

### Option A: Use Separate Table (RECOMMENDED ✅)

**Advantages:**
- ✅ Scalable (handles 1000+ comments per task)
- ✅ Better query performance (indexed lookups)
- ✅ Cleaner schema (normalized data)
- ✅ Easier to add comment features (edit, delete, reactions)
- ✅ Supports pagination (load comments incrementally)
- ✅ Supports real-time updates (subscribe to comment changes)

**Disadvantages:**
- ❌ Requires JOIN to fetch task with comments
- ❌ More complex repository logic
- ❌ Need to update Room to use separate table

**Implementation effort:** 2-3 hours

**Use this if:**
- Tasks can have many comments (>10 per task)
- You need comment editing/deletion
- You want real-time comment updates
- You plan to add comment reactions/threading

---

### Option B: Use Embedded JSONB (Simple but Limited)

**Advantages:**
- ✅ Simpler queries (no JOIN needed)
- ✅ Atomic updates (comments update with task)
- ✅ Already implemented in Room
- ✅ No migration needed

**Disadvantages:**
- ❌ Limited scalability (<100 comments per task)
- ❌ Inefficient for large comment lists
- ❌ Can't query comments independently
- ❌ Harder to add comment features
- ❌ JSONB updates are all-or-nothing

**Implementation effort:** 0 hours (already working)

**Use this if:**
- Comments are sparse (1-5 per task)
- You don't need comment editing
- You don't need real-time comment updates
- You want simplest possible implementation

---

## 📋 Recommended Approach

### **Use Separate Table (Option A)**

**Rationale:**
1. Task management apps typically have many comments per task
2. Comments need editing, deletion, and reactions (future features)
3. Separate table is industry best practice
4. Supabase already has the table created

**Migration path:**
1. Keep Room Task.comments as **local cache only**
2. Add TaskCommentDao for separate comment queries
3. Repository fetches comments separately and caches in Task.comments
4. When syncing to Supabase:
   - Send comments to `task_comments` table
   - DON'T send embedded comments field
5. When receiving from Supabase:
   - Fetch task + comments separately
   - Cache comments as JSON in Task.comments (Room only)

---

## 🔧 Implementation Plan

### Phase 1: Add TaskCommentDao (30 min)

```kotlin
// app/src/main/java/com/example/kosmos/core/database/dao/TaskCommentDao.kt

@Dao
interface TaskCommentDao {
    @Query("SELECT * FROM task_comments WHERE task_id = :taskId ORDER BY created_at DESC")
    fun getCommentsByTaskId(taskId: String): Flow<List<TaskComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: TaskComment)

    @Delete
    suspend fun deleteComment(comment: TaskComment)

    @Query("DELETE FROM task_comments WHERE task_id = :taskId")
    suspend fun deleteCommentsByTask(taskId: String)
}
```

### Phase 2: Add TaskComment Model (15 min)

```kotlin
// app/src/main/java/com/example/kosmos/core/models/TaskComment.kt

@Entity(
    tableName = "task_comments",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskComment(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val content: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long? = null
)
```

### Phase 3: Update Repository (1 hour)

```kotlin
// TaskRepository changes

suspend fun getTaskWithComments(taskId: String): Result<TaskWithComments> {
    val task = taskDao.getTaskById(taskId).first()
    val comments = taskCommentDao.getCommentsByTaskId(taskId).first()

    // Cache comments in task.comments for offline access
    val commentsJson = Json.encodeToString(comments)
    taskDao.updateTask(task.copy(comments = commentsJson))

    return Result.success(TaskWithComments(task, comments))
}

suspend fun addComment(taskId: String, content: String): Result<TaskComment> {
    val comment = TaskComment(
        taskId = taskId,
        userId = currentUserId,
        content = content
    )

    // Insert locally
    taskCommentDao.insertComment(comment)

    // Sync to Supabase
    supabaseTaskDataSource.insertComment(comment)

    return Result.success(comment)
}
```

### Phase 4: Add SupabaseTaskDataSource Methods (30 min)

```kotlin
// SupabaseTaskDataSource additions

suspend fun insertComment(comment: TaskComment): Result<Unit> {
    return try {
        supabase.from("task_comments").insert(comment)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun getComments(taskId: String): Result<List<TaskComment>> {
    return try {
        val comments = supabase.from("task_comments")
            .select {
                filter { eq("task_id", taskId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<TaskComment>()
        Result.success(comments)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun deleteComment(commentId: String): Result<Unit> {
    return try {
        supabase.from("task_comments").delete {
            filter { eq("id", commentId) }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Phase 5: Update Sync Logic (30 min)

```kotlin
// InitialSyncManager changes

suspend fun syncTasksAndComments(projectId: String) {
    // Sync tasks (WITHOUT embedded comments)
    val tasks = supabaseTaskDataSource.getTasks(projectId).getOrThrow()
    taskDao.insertAll(tasks)

    // Sync comments separately for each task
    tasks.forEach { task ->
        val comments = supabaseTaskDataSource.getComments(task.id).getOrThrow()
        taskCommentDao.insertAll(comments)

        // Cache comments in task.comments field for offline access
        val commentsJson = Json.encodeToString(comments)
        taskDao.updateTask(task.copy(comments = commentsJson))
    }
}
```

---

## 🧪 Testing Plan

### Test 1: Add Comment
1. Create task
2. Add 3 comments
3. Verify comments stored in `task_comments` table (Room + Supabase)
4. Verify Task.comments contains cached JSON

### Test 2: Fetch Task with Comments
1. Fetch task by ID
2. Verify comments loaded from `task_comments` table
3. Verify comments sorted by created_at DESC

### Test 3: Delete Comment
1. Delete a comment
2. Verify deleted from `task_comments` table (Room + Supabase)
3. Verify Task.comments cache updated

### Test 4: Offline Mode
1. Go offline
2. Add comment (stored in Room only)
3. Go online
4. Verify comment synced to Supabase

---

## 🔄 Alternative: Keep Current Dual Storage

**If you want to keep both for compatibility:**

1. **Primary:** Use `task_comments` table for all new comments
2. **Legacy:** Keep `tasks.comments` for old data
3. **Migration:** Write script to move embedded comments to separate table
4. **Future:** Phase out embedded comments completely

**Migration SQL:**
```sql
-- Move embedded comments to task_comments table
INSERT INTO task_comments (id, task_id, user_id, content, created_at)
SELECT
    gen_random_uuid(),
    id,
    created_by_id,
    jsonb_array_elements_text(comments::jsonb),
    created_at
FROM tasks
WHERE comments IS NOT NULL AND comments != '';

-- Clear embedded comments after migration
UPDATE tasks SET comments = NULL;
```

---

## ✅ Decision Required

**Choose one:**

- [ ] **Option A: Separate Table** (Recommended) - Implement Phases 1-5
- [ ] **Option B: Embedded JSONB** - Remove `task_comments` table
- [ ] **Option C: Keep Both** - Migrate embedded → table, phase out embedded

**Recommendation:** Option A (Separate Table)

**Estimated Time:** 2.5-3 hours
**Risk:** Low (data migration is safe with proper testing)
**Impact:** High (better scalability, cleaner architecture)

---

## 📁 Files to Modify (Option A)

### New Files:
1. `app/src/main/java/com/example/kosmos/core/models/TaskComment.kt`
2. `app/src/main/java/com/example/kosmos/core/database/dao/TaskCommentDao.kt`

### Modified Files:
1. `app/src/main/java/com/example/kosmos/data/repository/TaskRepository.kt`
2. `app/src/main/java/com/example/kosmos/data/datasource/SupabaseTaskDataSource.kt`
3. `app/src/main/java/com/example/kosmos/data/sync/InitialSyncManager.kt`
4. `app/src/main/java/com/example/kosmos/core/database/KosmosDatabase.kt` (update version + add DAO)

### SQL Scripts:
1. `documents/04-DATABASE/MIGRATE_COMMENTS_TO_SEPARATE_TABLE.sql` (data migration)

---

**Status**: Awaiting decision from development team
**Next Steps**: Once decided, implement chosen option
**Contact**: Update this document after implementation

---

**Last Updated**: 2026-01-25
