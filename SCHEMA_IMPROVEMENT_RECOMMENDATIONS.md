# Schema Design Improvement Recommendations

**Date**: 2026-01-24
**Purpose**: Beyond fixing mismatches - optimize schema for performance, scalability, and maintainability
**Priority**: Post-mismatch-fix / Pre-production launch

---

## 🎯 Executive Summary

**Current Schema Grade**: B+ (functional but suboptimal)
**Target Schema Grade**: A+ (production-ready at scale)

**Key Issues Identified:**
1. ❌ Denormalized data (senderName, assignedToName) causes update anomalies
2. ❌ JSONB storage for arrays (tags, techStack) inefficient for queries
3. ❌ Missing composite indexes for common query patterns
4. ❌ No partitioning strategy for high-volume tables (messages, task_activity)
5. ❌ Metadata caching lacks database triggers (manual updates = bugs)
6. ❌ No soft delete pattern (hard deletes lose audit trail)

---

## 🔴 CRITICAL: Data Denormalization Issues

### Problem 1: User Name Duplication

**Current Schema:**
```sql
-- messages table
sender_id UUID REFERENCES users(id),
sender_name TEXT NOT NULL,  -- ❌ Duplicated from users.display_name
sender_photo_url TEXT       -- ❌ Duplicated from users.photo_url

-- tasks table
assigned_to_id UUID REFERENCES users(id),
assigned_to_name TEXT,      -- ❌ Duplicated from users.display_name
created_by_id UUID NOT NULL,
created_by_name TEXT        -- ❌ Duplicated from users.display_name
```

**Issue**: Update Anomaly
- User changes display name: "John" → "John Smith"
- Old messages/tasks still show "John"
- Database inconsistency
- Manual cleanup required across ALL tables

**Impact:**
- 🐛 **Bug reports**: "Why does my old message show wrong name?"
- 📊 **Data integrity**: Stale names everywhere
- 🔧 **Maintenance**: Need migration script on every user rename

### Solution 1A: Normalize (Recommended)

**Remove denormalized fields, use JOINs:**
```sql
-- messages table (normalized)
ALTER TABLE messages
DROP COLUMN sender_name,
DROP COLUMN sender_photo_url;

-- Query with JOIN
SELECT
    m.id, m.content, m.timestamp,
    u.display_name AS sender_name,
    u.photo_url AS sender_photo_url
FROM messages m
INNER JOIN users u ON m.sender_id = u.id
WHERE m.chat_room_id = ?;

-- Create view for backward compatibility
CREATE VIEW messages_with_sender AS
SELECT
    m.*,
    u.display_name AS sender_name,
    u.photo_url AS sender_photo_url
FROM messages m
LEFT JOIN users u ON m.sender_id = u.id;
```

**Pros:**
- ✅ Single source of truth
- ✅ No update anomalies
- ✅ Automatic name updates everywhere
- ✅ Smaller database size

**Cons:**
- ⚠️ Requires JOIN for every query (5-10ms overhead)
- ⚠️ Breaking change for existing code
- ⚠️ Need to update all SupabaseDataSource classes

### Solution 1B: Triggers (Keep Denormalization, Auto-Sync)

**Add database triggers to auto-update:**
```sql
-- Trigger: Update all messages when user display_name changes
CREATE OR REPLACE FUNCTION update_sender_name_in_messages()
RETURNS TRIGGER AS $$
BEGIN
    -- Update all messages from this user
    UPDATE messages
    SET sender_name = NEW.display_name,
        sender_photo_url = NEW.photo_url
    WHERE sender_id = NEW.id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER sync_user_name_to_messages
AFTER UPDATE OF display_name, photo_url ON users
FOR EACH ROW
WHEN (OLD.display_name IS DISTINCT FROM NEW.display_name
      OR OLD.photo_url IS DISTINCT FROM NEW.photo_url)
EXECUTE FUNCTION update_sender_name_in_messages();

-- Similar triggers for tasks table
CREATE OR REPLACE FUNCTION update_user_name_in_tasks()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE tasks
    SET assigned_to_name = NEW.display_name
    WHERE assigned_to_id = NEW.id;

    UPDATE tasks
    SET created_by_name = NEW.display_name
    WHERE created_by_id = NEW.id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER sync_user_name_to_tasks
AFTER UPDATE OF display_name ON users
FOR EACH ROW
WHEN (OLD.display_name IS DISTINCT FROM NEW.display_name)
EXECUTE FUNCTION update_user_name_in_tasks();
```

**Pros:**
- ✅ Keeps fast queries (no JOINs)
- ✅ Automatic consistency
- ✅ No code changes needed

**Cons:**
- ⚠️ Database overhead on user updates
- ⚠️ Trigger complexity
- ⚠️ Still stores duplicate data

**Recommendation**: **Use Solution 1B (Triggers)** for now
- Easier migration path
- Performance benefit outweighs complexity
- Can normalize later if triggers become bottleneck

---

## 🟠 MEDIUM: Inefficient JSONB for Arrays

### Problem 2: Tags/TechStack as JSONB

**Current Schema:**
```sql
-- projects table
tech_stack JSONB,  -- ❌ ["Kotlin", "Android", "Compose"]
tags JSONB,        -- ❌ ["collaboration", "agile"]
industry_tags JSONB

-- tasks table
tags TEXT[]  -- ✅ PostgreSQL array (better)
```

**Issues:**
1. **Inconsistent types**: tasks uses TEXT[], projects uses JSONB
2. **Query inefficiency**: Can't use GIN index properly for JSONB arrays
3. **Schema confusion**: Is it JSONB object or array?

### Solution 2: Standardize on PostgreSQL Arrays

**Migrate JSONB to TEXT[]:**
```sql
-- Backup existing data
CREATE TABLE projects_backup AS SELECT * FROM projects;

-- Convert JSONB to TEXT[]
UPDATE projects
SET tech_stack = (
    SELECT array_agg(value::text)
    FROM jsonb_array_elements_text(tech_stack::jsonb)
)
WHERE tech_stack IS NOT NULL;

-- Change column type
ALTER TABLE projects
ALTER COLUMN tech_stack TYPE TEXT[] USING tech_stack::text[]::text[];

-- Same for tags and industry_tags
ALTER TABLE projects
ALTER COLUMN tags TYPE TEXT[] USING tags::text[]::text[],
ALTER COLUMN industry_tags TYPE TEXT[] USING industry_tags::text[]::text[];

-- Create GIN indexes for fast array queries
CREATE INDEX idx_projects_tech_stack ON projects USING GIN(tech_stack);
CREATE INDEX idx_projects_tags ON projects USING GIN(tags);
CREATE INDEX idx_projects_industry_tags ON projects USING GIN(industry_tags);
```

**Benefits:**
- ✅ 3-5x faster array queries with GIN index
- ✅ Consistent with tasks.tags (TEXT[])
- ✅ Better PostgreSQL integration
- ✅ Native array operators: `@>`, `&&`, `<@`

**Query Examples:**
```sql
-- Find projects with Kotlin tech stack
SELECT * FROM projects
WHERE tech_stack @> ARRAY['Kotlin'];

-- Find projects with ANY of these tags
SELECT * FROM projects
WHERE tags && ARRAY['collaboration', 'agile'];

-- Find projects with EXACTLY these tags
SELECT * FROM projects
WHERE tags = ARRAY['fintech', 'startup'];
```

---

## 🟡 HIGH: Missing Composite Indexes

### Problem 3: Unoptimized Query Patterns

**Current Indexes**: Single-column only
**Common Queries**: Multi-column filters

**Missing Indexes:**

#### 1. Tasks by Project + Status
```sql
-- Common query: Get TODO tasks for a project
SELECT * FROM tasks
WHERE project_id = ? AND status = 'TODO'
ORDER BY priority DESC, created_at DESC;

-- Current: Uses idx_tasks_project_id, then filters status (SLOW)
-- Better: Composite index
CREATE INDEX idx_tasks_project_status_priority
ON tasks(project_id, status, priority DESC, created_at DESC);
```

#### 2. Tasks by Assignee + Status
```sql
-- Common query: My active tasks
SELECT * FROM tasks
WHERE assigned_to_id = ? AND status IN ('TODO', 'IN_PROGRESS')
ORDER BY due_date ASC NULLS LAST;

-- Better: Composite index
CREATE INDEX idx_tasks_assignee_status_due
ON tasks(assigned_to_id, status, due_date NULLS LAST);
```

#### 3. Messages by ChatRoom + Timestamp (Pagination)
```sql
-- Common query: Load recent messages with pagination
SELECT * FROM messages
WHERE chat_room_id = ?
ORDER BY timestamp DESC
LIMIT 50 OFFSET ?;

-- Current: idx_messages_chat_room_timestamp exists ✅
-- But missing: Covering index for SELECT *
CREATE INDEX idx_messages_chat_room_covering
ON messages(chat_room_id, timestamp DESC)
INCLUDE (sender_id, sender_name, content, type);
```

#### 4. ProjectMembers by User (Active projects)
```sql
-- Common query: Get all my active projects
SELECT p.* FROM projects p
INNER JOIN project_members pm ON p.id = pm.project_id
WHERE pm.user_id = ? AND pm.is_active = true
ORDER BY p.last_activity_at DESC NULLS LAST;

-- Better: Composite index
CREATE INDEX idx_project_members_user_active
ON project_members(user_id, is_active, project_id);
```

#### 5. TaskActivity by Project + Timestamp (Activity Feed)
```sql
-- Common query: Project activity timeline
SELECT * FROM task_activity
WHERE project_id = ?
ORDER BY timestamp DESC
LIMIT 100;

-- Current: idx_task_activity_project_timestamp exists ✅
-- But consider partitioning for scale (see Problem 4)
```

**Impact:**
- **Before**: 50-200ms query times on large datasets
- **After**: 5-20ms query times (10x improvement)

---

## 🟠 MEDIUM: No Table Partitioning Strategy

### Problem 4: High-Volume Tables Will Slow Down

**Tables at Risk:**
1. **messages** - Grows infinitely (100K+ rows common)
2. **task_activity** - Every task change adds row (50K+ rows)
3. **sync_queue** - Failed operations accumulate

**Current State**: Single table, no archival strategy

### Solution 4A: Time-Based Partitioning

**Partition messages by month:**
```sql
-- Convert to partitioned table
ALTER TABLE messages RENAME TO messages_old;

-- Create partitioned table
CREATE TABLE messages (
    id UUID NOT NULL,
    chat_room_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    -- ... other columns
    PRIMARY KEY (id, timestamp)  -- timestamp MUST be in key
) PARTITION BY RANGE (timestamp);

-- Create partitions (example: quarterly)
CREATE TABLE messages_2026_q1 PARTITION OF messages
FOR VALUES FROM (1704067200000) TO (1711929600000);  -- Jan-Mar 2026

CREATE TABLE messages_2026_q2 PARTITION OF messages
FOR VALUES FROM (1711929600000) TO (1719792000000);  -- Apr-Jun 2026

-- Create indexes on each partition
CREATE INDEX idx_messages_2026_q1_chat_room
ON messages_2026_q1(chat_room_id, timestamp DESC);

-- Migrate data
INSERT INTO messages SELECT * FROM messages_old;
DROP TABLE messages_old;

-- Auto-create future partitions (cron job or trigger)
CREATE OR REPLACE FUNCTION create_next_message_partition()
RETURNS void AS $$
DECLARE
    next_quarter_start BIGINT;
    next_quarter_end BIGINT;
    partition_name TEXT;
BEGIN
    -- Calculate next quarter timestamps
    -- Create partition
    -- Add indexes
END;
$$ LANGUAGE plpgsql;
```

**Benefits:**
- ✅ Queries only scan relevant partitions (10x faster)
- ✅ Easy archival (detach old partitions)
- ✅ Faster vacuum and maintenance
- ✅ Scales to millions of rows

**Cons:**
- ⚠️ Complex migration
- ⚠️ Requires partition management
- ⚠️ timestamp must be in primary key

### Solution 4B: Archival Strategy (Simpler)

**Create archive tables:**
```sql
-- Archive messages older than 6 months
CREATE TABLE messages_archive (LIKE messages INCLUDING ALL);

-- Move old messages
INSERT INTO messages_archive
SELECT * FROM messages
WHERE timestamp < (EXTRACT(EPOCH FROM NOW() - INTERVAL '6 months') * 1000);

DELETE FROM messages
WHERE timestamp < (EXTRACT(EPOCH FROM NOW() - INTERVAL '6 months') * 1000);

-- Vacuum to reclaim space
VACUUM FULL messages;
```

**Recommendation**: Start with **Solution 4B (Archival)**, upgrade to 4A if performance degrades

---

## 🟡 HIGH: Metadata Caching Lacks Triggers

### Problem 5: Manual Count Updates

**Current Implementation:**
```kotlin
// Project.kt has these fields:
memberCount: Int = 0,
chatCount: Int = 0,
taskCount: Int = 0,
completedTaskCount: Int = 0,
pendingTaskCount: Int = 0,
lastActivityAt: Long? = null
```

**Issue**: Comments say "Auto-updated by triggers" but NO TRIGGERS EXIST
**Reality**: App code must manually update these counts
**Risk**: Bugs, inconsistency, stale data

### Solution 5: Implement Database Triggers

```sql
-- Trigger: Update member_count on project_members changes
CREATE OR REPLACE FUNCTION update_project_member_count()
RETURNS TRIGGER AS $$
BEGIN
    -- Increment on INSERT
    IF (TG_OP = 'INSERT') THEN
        UPDATE projects
        SET member_count = member_count + 1,
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = NEW.project_id;
        RETURN NEW;

    -- Decrement on DELETE
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE projects
        SET member_count = member_count - 1,
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = OLD.project_id;
        RETURN OLD;

    -- Handle is_active toggle
    ELSIF (TG_OP = 'UPDATE' AND OLD.is_active != NEW.is_active) THEN
        UPDATE projects
        SET member_count = member_count + (CASE WHEN NEW.is_active THEN 1 ELSE -1 END),
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = NEW.project_id;
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_project_member_count_trigger
AFTER INSERT OR UPDATE OR DELETE ON project_members
FOR EACH ROW EXECUTE FUNCTION update_project_member_count();

-- Similar triggers for chat_count, task_count, completed_task_count
CREATE OR REPLACE FUNCTION update_project_task_counts()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE projects
        SET task_count = task_count + 1,
            completed_task_count = completed_task_count + (CASE WHEN NEW.status = 'DONE' THEN 1 ELSE 0 END),
            pending_task_count = pending_task_count + (CASE WHEN NEW.status NOT IN ('DONE', 'CANCELLED') THEN 1 ELSE 0 END),
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = NEW.project_id;
        RETURN NEW;

    ELSIF (TG_OP = 'UPDATE' AND OLD.status != NEW.status) THEN
        UPDATE projects
        SET completed_task_count = completed_task_count + (
                CASE WHEN NEW.status = 'DONE' THEN 1 ELSE 0 END -
                CASE WHEN OLD.status = 'DONE' THEN 1 ELSE 0 END
            ),
            pending_task_count = pending_task_count + (
                CASE WHEN NEW.status NOT IN ('DONE', 'CANCELLED') THEN 1 ELSE 0 END -
                CASE WHEN OLD.status NOT IN ('DONE', 'CANCELLED') THEN 1 ELSE 0 END
            ),
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = NEW.project_id;
        RETURN NEW;

    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE projects
        SET task_count = task_count - 1,
            completed_task_count = completed_task_count - (CASE WHEN OLD.status = 'DONE' THEN 1 ELSE 0 END),
            pending_task_count = pending_task_count - (CASE WHEN OLD.status NOT IN ('DONE', 'CANCELLED') THEN 1 ELSE 0 END),
            last_activity_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
        WHERE id = OLD.project_id;
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_project_task_counts_trigger
AFTER INSERT OR UPDATE OR DELETE ON tasks
FOR EACH ROW EXECUTE FUNCTION update_project_task_counts();
```

**Benefits:**
- ✅ Guaranteed consistency (database enforces it)
- ✅ Removes manual count logic from app code
- ✅ Real-time updates
- ✅ Works even if app crashes mid-operation

**Note**: Triggers add 2-5ms overhead per operation, but ensure correctness

---

## 🟠 MEDIUM: No Soft Delete Pattern

### Problem 6: Hard Deletes Lose Audit Trail

**Current Implementation:**
```sql
-- Deleting a task permanently removes it
DELETE FROM tasks WHERE id = ?;

-- Foreign keys CASCADE delete related records
-- ALL task_activity records are LOST
```

**Issues:**
1. ❌ **Can't undo deletions** (user mistakes)
2. ❌ **Lose audit trail** (compliance risk)
3. ❌ **Can't analyze deleted data** (metrics blind spot)
4. ❌ **Breaks references** (if other data points to deleted entity)

### Solution 6: Add Soft Delete Columns

```sql
-- Add soft delete columns to all main tables
ALTER TABLE tasks
ADD COLUMN IF NOT EXISTS deleted_at BIGINT,
ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

ALTER TABLE projects
ADD COLUMN IF NOT EXISTS deleted_at BIGINT,
ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

ALTER TABLE chat_rooms
ADD COLUMN IF NOT EXISTS deleted_at BIGINT,
ADD COLUMN IF NOT EXISTS deleted_by UUID REFERENCES users(id);

-- Create indexes for filtering
CREATE INDEX idx_tasks_deleted_at ON tasks(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_projects_deleted_at ON projects(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_chat_rooms_deleted_at ON chat_rooms(deleted_at) WHERE deleted_at IS NULL;

-- Update queries to filter out deleted records
-- OLD:
SELECT * FROM tasks WHERE project_id = ?;

-- NEW:
SELECT * FROM tasks
WHERE project_id = ? AND deleted_at IS NULL;

-- Create views for convenience
CREATE VIEW tasks_active AS
SELECT * FROM tasks WHERE deleted_at IS NULL;

CREATE VIEW projects_active AS
SELECT * FROM projects WHERE deleted_at IS NULL;
```

**Benefits:**
- ✅ Undo deletions (set deleted_at = NULL)
- ✅ Keep audit trail (task_activity survives)
- ✅ Analyze deletion patterns
- ✅ Compliance-friendly (GDPR right to erasure = hard delete when needed)

**Migration Path:**
1. Add columns (default NULL = not deleted)
2. Update all queries to filter deleted_at IS NULL
3. Update delete operations to set deleted_at timestamp
4. Create cron job to hard-delete records older than 90 days (optional)

---

## 🔵 LOW: Additional Optimizations

### 7. Full-Text Search Support

**Add FTS for messages and tasks:**
```sql
-- Add tsvector column for full-text search
ALTER TABLE messages
ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    to_tsvector('english', coalesce(content, '') || ' ' || coalesce(sender_name, ''))
) STORED;

CREATE INDEX idx_messages_search ON messages USING GIN(search_vector);

-- Fast search query
SELECT * FROM messages
WHERE search_vector @@ to_tsquery('english', 'kotlin & android');
```

### 8. Read Replicas Configuration

**Separate read/write for scale:**
```kotlin
// SupabaseConfig.kt
val supabaseReadOnly = createSupabaseClient(
    supabaseUrl = SUPABASE_READ_REPLICA_URL,
    supabaseKey = SUPABASE_ANON_KEY
) {
    install(Postgrest)
}

// Use in repositories
suspend fun getTasks(projectId: String): List<Task> {
    // Read from replica (lower latency)
    return supabaseReadOnly.from("tasks")...
}

suspend fun createTask(task: Task) {
    // Write to primary
    return supabasePrimary.from("tasks")...
}
```

### 9. Connection Pooling Optimization

**Current**: Default Supabase connection pool
**Better**: Configure for mobile app pattern
```sql
-- Supabase dashboard settings
ALTER DATABASE postgres SET max_connections = 200;
ALTER DATABASE postgres SET shared_buffers = '256MB';
ALTER DATABASE postgres SET effective_cache_size = '1GB';
```

---

## 📊 Implementation Priority Matrix

| Issue | Severity | Effort | ROI | Priority |
|-------|----------|--------|-----|----------|
| User name denormalization | 🔴 Critical | Medium | High | **P0** |
| Missing composite indexes | 🟠 High | Low | Very High | **P0** |
| Metadata caching triggers | 🟡 Medium | Medium | High | **P1** |
| JSONB to TEXT[] arrays | 🟡 Medium | Low | Medium | **P1** |
| Soft delete pattern | 🟠 High | Medium | Medium | **P2** |
| Table partitioning | 🟢 Low | High | Low | **P3** |
| Full-text search | 🟢 Low | Medium | Low | **P3** |
| Read replicas | 🟢 Low | High | Low | **P4** |

---

## 🚀 Recommended Implementation Phases

### Phase 1: Critical Fixes (2-3 hours)
1. ✅ Fix schema mismatches (from SCHEMA_MISMATCH_ANALYSIS.md)
2. ✅ Add composite indexes (immediate 10x query speedup)
3. ✅ Create user name sync triggers (fix update anomalies)

### Phase 2: Consistency (3-4 hours)
1. ✅ Implement metadata caching triggers
2. ✅ Migrate JSONB to TEXT[] for arrays
3. ✅ Test all triggers with integration tests

### Phase 3: Resilience (4-5 hours)
1. ✅ Add soft delete columns
2. ✅ Update all queries and repositories
3. ✅ Create archival strategy for old data

### Phase 4: Scale (Optional, 6-8 hours)
1. ⏭️ Implement table partitioning for messages
2. ⏭️ Add full-text search
3. ⏭️ Configure read replicas

---

## 🧪 Testing Strategy

**After Each Change:**

1. **Unit Tests**: Verify triggers work
```sql
-- Test member_count trigger
INSERT INTO project_members VALUES (...);
SELECT member_count FROM projects WHERE id = ?;
-- Expected: incremented by 1

DELETE FROM project_members WHERE id = ?;
SELECT member_count FROM projects WHERE id = ?;
-- Expected: decremented by 1
```

2. **Integration Tests**: Verify Room ↔ Supabase sync
```kotlin
// Create project with 5 members
// Check memberCount = 5 in both Room and Supabase
// Remove 2 members
// Check memberCount = 3 in both Room and Supabase
```

3. **Performance Tests**: Measure query times
```sql
EXPLAIN ANALYZE SELECT * FROM tasks
WHERE project_id = ? AND status = 'TODO'
ORDER BY priority DESC;
-- Verify uses composite index, not seq scan
```

---

## 📚 Additional Resources

**PostgreSQL Best Practices:**
- [Partitioning Guide](https://www.postgresql.org/docs/current/ddl-partitioning.html)
- [Index Types](https://www.postgresql.org/docs/current/indexes-types.html)
- [Trigger Examples](https://www.postgresql.org/docs/current/trigger-example.html)

**Supabase Specific:**
- [Database Performance](https://supabase.com/docs/guides/database/performance)
- [RLS Policies](https://supabase.com/docs/guides/auth/row-level-security)
- [Realtime Filters](https://supabase.com/docs/guides/realtime/postgres-changes)

---

**Last Updated**: 2026-01-24
**Next Review**: After Phase 1-2 implementation
**Owner**: Database Architecture Team
