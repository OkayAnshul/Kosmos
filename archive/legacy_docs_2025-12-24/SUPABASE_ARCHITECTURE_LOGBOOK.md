# Supabase Architecture Logbook

**Project**: Kosmos Android App
**Purpose**: Document schema decisions, migrations, and prevent breaking changes
**Last Updated**: 2025-10-31

---

## 📋 Table of Contents

1. [Current Schema State](#current-schema-state)
2. [Schema Change History](#schema-change-history)
3. [Breaking Change Prevention Guide](#breaking-change-prevention-guide)
4. [Best Practices](#best-practices)
5. [Troubleshooting Guide](#troubleshooting-guide)
6. [Migration Checklist](#migration-checklist)

---

## 🗄️ Current Schema State

### Table Summary

| Table | Columns | Status | Priority | Notes |
|-------|---------|--------|----------|-------|
| users | 17 | ✅ Complete | CRITICAL | All fields populated, username NOT NULL |
| messages | 15 | ⚠️ Fixed | CRITICAL | Added sender_name, sender_photo_url |
| chat_rooms | 14 | ⚠️ Fixed | CRITICAL | Added participant_ids array |
| tasks | 21 | ✅ Complete | CRITICAL | All fields match model |
| projects | 11 | ✅ Complete | CRITICAL | All fields match model |
| project_members | 9 | ✅ Complete | CRITICAL | All fields match model |
| voice_messages | N/A | ❌ Not Created | Phase 2 | Future feature |
| action_items | N/A | ❌ Not Created | Phase 2 | Future feature |

### Critical Schema Requirements

#### users (17 columns)
```sql
CREATE TABLE users (
    id                    TEXT PRIMARY KEY,
    email                 TEXT NOT NULL,
    username              TEXT NOT NULL UNIQUE,
    display_name          TEXT NOT NULL,
    age                   INTEGER,
    role                  TEXT,
    bio                   TEXT,
    location              TEXT,
    github_url            TEXT,
    twitter_url           TEXT,
    linkedin_url          TEXT,
    website_url           TEXT,
    portfolio_url         TEXT,
    photo_url             TEXT,
    is_online             BOOLEAN DEFAULT false,
    last_seen             BIGINT,
    fcm_token             TEXT,
    created_at            BIGINT
);
```

#### messages (15 columns) ⚠️ FIXED
```sql
CREATE TABLE messages (
    id                    TEXT PRIMARY KEY,
    chat_room_id          TEXT NOT NULL REFERENCES chat_rooms(id),
    sender_id             TEXT NOT NULL REFERENCES users(id),
    sender_name           TEXT NOT NULL,              -- ✅ ADDED
    sender_photo_url      TEXT,                       -- ✅ ADDED
    content               TEXT NOT NULL,
    timestamp             BIGINT NOT NULL,
    type                  TEXT NOT NULL DEFAULT 'TEXT',
    voice_message_id      TEXT,
    task_ids              TEXT[],
    reply_to_message_id   TEXT REFERENCES messages(id),
    is_edited             BOOLEAN DEFAULT false,
    edited_at             BIGINT,
    reactions             JSONB DEFAULT '{}'::jsonb,
    read_by               TEXT[]
);
```

#### chat_rooms (14 columns) ⚠️ FIXED
```sql
CREATE TABLE chat_rooms (
    id                      TEXT PRIMARY KEY,
    project_id              TEXT NOT NULL REFERENCES projects(id),
    name                    TEXT NOT NULL,
    description             TEXT,
    image_url               TEXT,
    type                    TEXT NOT NULL DEFAULT 'GENERAL',
    participant_ids         TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],  -- ✅ ADDED
    created_by              TEXT NOT NULL REFERENCES users(id),
    created_at              BIGINT NOT NULL,
    last_message_id         TEXT REFERENCES messages(id),
    last_message            TEXT,
    last_message_timestamp  BIGINT,
    is_task_board_enabled   BOOLEAN DEFAULT true,
    is_archived             BOOLEAN DEFAULT false,
    is_private              BOOLEAN DEFAULT false
);
```

#### tasks (21 columns)
```sql
CREATE TABLE tasks (
    id                   TEXT PRIMARY KEY,
    project_id           TEXT NOT NULL REFERENCES projects(id),
    chat_room_id         TEXT REFERENCES chat_rooms(id),
    title                TEXT NOT NULL,
    description          TEXT,
    status               TEXT NOT NULL DEFAULT 'TODO',
    priority             TEXT NOT NULL DEFAULT 'MEDIUM',
    assigned_to_id       TEXT REFERENCES users(id),
    assigned_to_name     TEXT,
    assigned_to_role     TEXT,
    created_by_id        TEXT NOT NULL REFERENCES users(id),
    created_by_name      TEXT NOT NULL,
    created_by_role      TEXT,
    created_at           BIGINT NOT NULL,
    updated_at           BIGINT NOT NULL,
    due_date             BIGINT,
    source_message_id    TEXT REFERENCES messages(id),
    tags                 TEXT[],
    comments             JSONB DEFAULT '[]'::jsonb,
    parent_task_id       TEXT REFERENCES tasks(id),
    estimated_hours      REAL,
    actual_hours         REAL
);
```

#### projects (11 columns)
```sql
CREATE TABLE projects (
    id           TEXT PRIMARY KEY,
    name         TEXT NOT NULL,
    description  TEXT,
    owner_id     TEXT NOT NULL REFERENCES users(id),
    status       TEXT NOT NULL DEFAULT 'ACTIVE',
    visibility   TEXT NOT NULL DEFAULT 'PRIVATE',
    created_at   BIGINT NOT NULL,
    updated_at   BIGINT NOT NULL,
    image_url    TEXT,
    color        TEXT DEFAULT '#6366F1',
    settings     TEXT
);
```

#### project_members (9 columns)
```sql
CREATE TABLE project_members (
    id                  TEXT PRIMARY KEY,
    project_id          TEXT NOT NULL REFERENCES projects(id),
    user_id             TEXT NOT NULL REFERENCES users(id),
    role                TEXT NOT NULL,
    joined_at           BIGINT NOT NULL,
    invited_by          TEXT REFERENCES users(id),
    is_active           BOOLEAN DEFAULT true,
    last_activity_at    BIGINT,
    custom_permissions  TEXT,
    UNIQUE(project_id, user_id)
);
```

---

## 📜 Schema Change History

### Migration 1: Initial Setup (Date: Unknown)
- Created all 6 core tables
- **Issue**: Created without sender_name/sender_photo_url in messages
- **Issue**: Created without participant_ids in chat_rooms
- **Issue**: Username allowed NULL values

### Migration 2: User Fix (Date: 2025-10-31)
**File**: `SUPABASE_MIGRATION_ADD_USER_FIELDS.sql`, `SUPABASE_INTEGRATION_FIX.md`

**Changes**:
- Added missing user profile fields (age, role, bio, location, social URLs)
- Set username as NOT NULL with UNIQUE constraint
- Populated NULL usernames from display_name

**Reason**: User model had fields not in database

### Migration 3: RLS Fix (Date: 2025-10-31)
**File**: `SUPABASE_FIX_USERNAME_AND_RLS.sql`

**Changes**:
- Disabled Row Level Security on all 6 core tables
- Fixed remaining NULL usernames

**Reason**: RLS was blocking all INSERT/UPDATE operations, data not syncing

### Migration 4: Complete Schema Fix (Date: 2025-10-31)
**File**: `SCHEMA_FIX_COMPLETE.sql`

**Changes**:
- ✅ Added `sender_name TEXT NOT NULL` to messages table
- ✅ Added `sender_photo_url TEXT` to messages table
- ✅ Added `participant_ids TEXT[]` to chat_rooms table
- ✅ Populated existing data from users table
- ✅ Added performance indexes
- ✅ Added foreign key constraints
- ✅ Reloaded PostgREST schema cache

**Reason**: Fixed critical schema mismatches causing sync failures

**Errors Fixed**:
- `Could not find 'sender_name' column of 'messages' (PGRST204)`
- `Could not find 'participant_ids' column of 'chat_rooms' (PGRST204)`

---

## 🛡️ Breaking Change Prevention Guide

### Rule 1: Schema-First Development

**ALWAYS follow this order:**

1. **Design Kotlin Model**
   ```kotlin
   @Serializable
   @Entity(tableName = "example")
   data class Example(
       @PrimaryKey val id: String,
       @SerialName("snake_case_field") val camelCaseField: String
   )
   ```

2. **Create SQL Table to Match**
   ```sql
   CREATE TABLE example (
       id TEXT PRIMARY KEY,
       snake_case_field TEXT NOT NULL
   );
   ```

3. **Verify Match** - Run schema validator (see Rule 5)

4. **Deploy & Test** - Build app, test sync

### Rule 2: Kotlin Model Requirements

**Every model field MUST have:**

✅ `@SerialName("snake_case")` annotation for non-ID fields
✅ Default value or nullable type
✅ Matching column in Supabase table
✅ Correct data type mapping

**Kotlin to PostgreSQL Type Mapping:**

| Kotlin Type | PostgreSQL Type | Notes |
|-------------|----------------|-------|
| `String` | `TEXT` | Use NOT NULL for non-nullable Kotlin String |
| `String?` | `TEXT` | Nullable |
| `Int` | `INTEGER` | |
| `Long` | `BIGINT` | Use for timestamps |
| `Boolean` | `BOOLEAN` | |
| `Float/Double` | `REAL` | |
| `List<String>` | `TEXT[]` | PostgreSQL array |
| `Map<String, Any>` | `JSONB` | Use for complex objects |

### Rule 3: Never Skip @SerialName

**BAD** ❌:
```kotlin
data class User(
    val displayName: String  // Will look for "displayName" in DB
)
```

**GOOD** ✅:
```kotlin
data class User(
    @SerialName("display_name")
    val displayName: String  // Will look for "display_name" in DB
)
```

### Rule 4: Array Fields Are Special

**Kotlin**:
```kotlin
@SerialName("participant_ids")
val participantIds: List<String> = emptyList()
```

**SQL**:
```sql
participant_ids TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[]
```

**Index for Performance**:
```sql
CREATE INDEX idx_chat_rooms_participant_ids
ON chat_rooms USING GIN(participant_ids);
```

### Rule 5: Schema Validation Before Every Release

**Create SchemaValidator.kt** (TODO):
```kotlin
object SchemaValidator {
    suspend fun validateAllTables(): List<SchemaMismatch> {
        // Compare Kotlin models against Supabase schema
        // Return list of mismatches
    }
}
```

**Add Gradle Task** (TODO):
```kotlin
// build.gradle.kts
tasks.register("validateSchema") {
    doLast {
        // Run SchemaValidator
        // Fail build if mismatches found
    }
}
```

### Rule 6: PostgREST Schema Cache

**ALWAYS reload cache after schema changes:**

```sql
NOTIFY pgrst, 'reload schema';
```

**Symptoms of stale cache:**
- Error code `PGRST204`
- "Could not find column X in schema cache"
- Operations work in SQL editor but fail from app

### Rule 7: Migration Checklist

Before any schema change:

- [ ] Read DEVELOPMENT_LOGBOOK.md for current phase
- [ ] Document change in this file (SUPABASE_ARCHITECTURE_LOGBOOK.md)
- [ ] Create migration SQL file with rollback plan
- [ ] Test migration on development database first
- [ ] Verify with diagnostic queries
- [ ] Reload PostgREST cache
- [ ] Update Kotlin models if needed
- [ ] Run schema validator
- [ ] Rebuild app and test sync
- [ ] Update DEVELOPMENT_LOGBOOK.md with results

---

## 💡 Best Practices

### 1. Offline-First Architecture

**Current Pattern**:
```kotlin
suspend fun sendMessage(message: Message) {
    // 1. Save to Room first (instant UI update)
    messageDao.insert(message)

    // 2. Sync to Supabase in background
    val result = supabaseMessageDataSource.insert(message)

    // 3. Log result but don't fail if sync fails
    if (result.isFailure) {
        Log.e(TAG, "Sync failed, will retry later", result.exceptionOrNull())
    }
}
```

**Benefits**:
- ✅ Instant UI updates
- ✅ Works offline
- ✅ Automatic sync when online

**Trade-offs**:
- ⚠️ Data may be temporarily out of sync
- ⚠️ Requires conflict resolution (not implemented yet)
- ⚠️ Silent failures can be hard to debug

### 2. Error Logging Standards

**ALWAYS log detailed errors:**

```kotlin
if (result.isFailure) {
    val error = result.exceptionOrNull()
    Log.e(TAG, "❌ SUPABASE SYNC FAILED for $entity", error)
    Log.e(TAG, "Possible causes: RLS policies, network error, auth token expired, schema mismatch")
    Log.e(TAG, "Data saved locally only. Check Supabase logs and schema.")
} else {
    Log.d(TAG, "✅ $entity synced to Supabase successfully: $id")
}
```

**Never do this:**
```kotlin
// BAD - Silent failure
if (result.isFailure) {
    // Nothing logged
}
```

### 3. Real-time Subscriptions

**Current State**: WebSocket not working due to engine issue

**Fix Needed in SupabaseConfig.kt**:
```kotlin
install(HttpClient) {
    engine = OkHttp.create()  // Or CIO - both support WebSocket
}
```

**Real-time Pattern**:
```kotlin
fun observeMessages(chatRoomId: String): Flow<List<Message>> {
    return supabase.channel("messages:$chatRoomId")
        .postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
            filter = "chat_room_id=eq.$chatRoomId"
        }
        .map { handleChange(it) }
}
```

### 4. RLS in Production

**Current State**: RLS disabled for testing

**Production Requirement**: Enable RLS with proper policies

**Example Policy**:
```sql
-- Users can only insert messages they send
CREATE POLICY "Users can insert own messages"
ON messages FOR INSERT
TO authenticated
WITH CHECK (auth.uid()::text = sender_id);

-- Users can view messages in their chat rooms
CREATE POLICY "Users can view their messages"
ON messages FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM chat_rooms
        WHERE chat_rooms.id = messages.chat_room_id
        AND auth.uid()::text = ANY(chat_rooms.participant_ids)
    )
);
```

### 5. Index Strategy

**Always index:**
- Foreign keys
- Array fields (using GIN index)
- Frequently filtered columns (status, type, etc.)
- Sort columns (timestamp, created_at)

**Example**:
```sql
-- Foreign key
CREATE INDEX idx_messages_chat_room_id ON messages(chat_room_id);

-- Array field
CREATE INDEX idx_chat_rooms_participant_ids ON chat_rooms USING GIN(participant_ids);

-- Filter + Sort
CREATE INDEX idx_messages_chat_room_timestamp ON messages(chat_room_id, timestamp DESC);
```

---

## 🔍 Troubleshooting Guide

### Error: "Could not find column X in schema cache"

**Error Code**: `PGRST204`

**Root Cause**: Database schema changed but PostgREST cache not reloaded

**Solution**:
```sql
NOTIFY pgrst, 'reload schema';
```

**Prevention**: Always reload cache after schema changes

---

### Error: "JSON deserialization failed"

**Symptoms**: App crashes when fetching data from Supabase

**Common Causes**:
1. NULL value in database but non-nullable Kotlin field
2. Missing @SerialName annotation
3. Type mismatch (e.g., TEXT in DB but Int in Kotlin)

**Solution**:
1. Check for NULL values:
```sql
SELECT column_name, COUNT(*) as null_count
FROM your_table
WHERE column_name IS NULL
GROUP BY column_name;
```

2. Add @SerialName if missing:
```kotlin
@SerialName("snake_case_field")
val camelCaseField: String
```

3. Make field nullable or add default:
```kotlin
// Option 1: Nullable
val field: String? = null

// Option 2: Default value
val field: String = ""
```

---

### Error: "new row violates row-level security policy"

**Root Cause**: RLS enabled but no policy allows the operation

**Immediate Fix** (testing only):
```sql
ALTER TABLE your_table DISABLE ROW LEVEL SECURITY;
```

**Production Fix**: Create proper RLS policies (see Best Practices #4)

---

### Error: "Engine doesn't support WebSocketCapability"

**Root Cause**: Ktor HTTP client engine doesn't support WebSocket

**Solution**: Update SupabaseConfig.kt:
```kotlin
import io.ktor.client.engine.okhttp.OkHttp

val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    install(Postgrest)
    install(Auth)
    install(Realtime)
    install(Storage)

    // Add this:
    httpEngine = OkHttp.create()
}
```

---

### Error: Messages/Tasks not syncing

**Diagnostic Steps**:

1. Check logcat for errors:
```bash
adb logcat | grep "SUPABASE SYNC"
```

2. Verify RLS is disabled (for testing):
```sql
SELECT tablename, rowsecurity FROM pg_tables
WHERE schemaname = 'public' AND tablename = 'messages';
```

3. Test direct insert in Supabase:
```sql
INSERT INTO messages (id, chat_room_id, sender_id, sender_name, content, timestamp, type)
VALUES ('test-123', 'room-1', 'user-1', 'Test', 'Hello', 1234567890000, 'TEXT');
```

4. Check auth token:
```kotlin
val session = supabase.auth.currentSessionOrNull()
Log.d(TAG, "Auth token expires at: ${session?.expiresAt}")
```

---

## ✅ Migration Checklist Template

Use this checklist for every schema change:

### Pre-Migration

- [ ] Read DEVELOPMENT_LOGBOOK.md
- [ ] Identify affected tables and columns
- [ ] Document change reason in this file
- [ ] Create backup of production data (if applicable)
- [ ] Write rollback SQL

### Migration

- [ ] Create migration SQL file (name: `MIGRATION_YYYY_MM_DD_description.sql`)
- [ ] Add diagnostic queries at the top
- [ ] Add migration statements
- [ ] Add verification queries at the bottom
- [ ] Include `NOTIFY pgrst, 'reload schema';`

### Testing

- [ ] Test on development database first
- [ ] Run all verification queries
- [ ] Check for NULL values in new columns
- [ ] Verify indexes created
- [ ] Verify foreign keys created
- [ ] Reload PostgREST cache

### Code Updates

- [ ] Update Kotlin models if needed
- [ ] Add/update @SerialName annotations
- [ ] Update Room entities if needed
- [ ] Run schema validator (once implemented)
- [ ] Build app: `./gradlew clean assembleDebug`
- [ ] Fix any compilation errors

### Deployment Testing

- [ ] Install app on device
- [ ] Clear app data for fresh start
- [ ] Test affected features
- [ ] Monitor logcat for errors
- [ ] Verify data syncs to Supabase
- [ ] Check Supabase table editor for new data

### Documentation

- [ ] Update this file (SUPABASE_ARCHITECTURE_LOGBOOK.md)
- [ ] Update DEVELOPMENT_LOGBOOK.md
- [ ] Create/update testing guide if needed
- [ ] Document any issues encountered
- [ ] Update CLAUDE.md if architecture changed

---

## 🎯 Future Improvements

### Phase 2 Tables (voice_messages, action_items)

**When implementing these:**

1. Create tables following schema in SCHEMA_ANALYSIS_COMPLETE.md
2. Add to schema validator
3. Create indexes and foreign keys
4. Disable RLS initially, add policies later
5. Test sync thoroughly before enabling

### Schema Validation Automation

**TODO**: Implement automated schema validation:

1. Create `SchemaValidator.kt` that:
   - Fetches current Supabase schema via API
   - Compares against Kotlin models
   - Generates migration SQL for mismatches
   - Outputs report of differences

2. Add Gradle task:
   ```bash
   ./gradlew validateSchema
   ```

3. Run in CI/CD pipeline before builds

### Conflict Resolution

**Current Issue**: No conflict resolution for offline edits

**Future Solution**:
- Last-write-wins strategy
- Timestamp-based merging
- User-prompted conflict resolution UI

### Sync Status UI

**Current Issue**: Silent sync failures

**Future Solution**:
- Sync status indicator in UI
- Retry failed syncs manually
- Show sync errors to user with actionable messages

---

## 📚 Related Documentation

- `SCHEMA_ANALYSIS_COMPLETE.md` - Complete schema documentation
- `SCHEMA_FIX_COMPLETE.sql` - Complete migration script
- `DEVELOPMENT_LOGBOOK.md` - Project progress tracking
- `CLAUDE.md` - Project architecture overview
- `FIX_SUMMARY_2025-10-31.md` - Recent fix summary
- `TESTING_GUIDE_SUPABASE_FIXES.md` - Testing procedures

---

## 🔄 Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2025-10-31 | Initial creation | Claude Code |
| | | Documented 6 core tables | |
| | | Added migration history | |
| | | Created prevention guide | |
| | | Added troubleshooting section | |

---

**Last Updated**: 2025-10-31
**Next Review**: After Phase 2 implementation
**Maintainer**: Development Team
