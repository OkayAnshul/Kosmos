# Database Migration Guide

**CRITICAL**: This app NO LONGER uses destructive migrations. You MUST create proper migrations for ALL schema changes.

## P0-04 Fix: Destructive Fallback Removed

**What changed:**
- ❌ **REMOVED**: `.fallbackToDestructiveMigration()` from `Module.kt`
- ✅ **RESULT**: App crashes if migration missing → forces proper migration creation
- ✅ **BENEFIT**: User data NEVER silently wiped

**Before (DANGEROUS):**
```kotlin
.addMigrations(...)
.fallbackToDestructiveMigration()  // ⚠️ Wipes all data!
```

**After (SAFE):**
```kotlin
.addMigrations(...)
// No fallback - crash forces proper migration
```

---

## Current Database Status

**Current Version:** 5
**Last Migration:** MIGRATION_4_5 (User version + task_activity table)

### Migration History

| Migration | Changes | Status |
|-----------|---------|--------|
| 1 → 2 | (Unknown - placeholder) | ✅ Defined |
| 2 → 3 | (Unknown - placeholder) | ✅ Defined |
| 3 → 4 | Project wizard fields | ✅ Defined |
| 4 → 5 | User version + task_activity table | ✅ Defined |
| 5 → 6 | *Next migration goes here* | ⏳ Pending |

---

## How to Create a Migration

### Step 1: Update Entity

Modify your entity class with the schema change.

**Example**: Add `isArchived` field to Task entity
```kotlin
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val title: String,
    val isArchived: Boolean = false  // NEW FIELD
)
```

### Step 2: Increment Database Version

Edit `KosmosDatabase.kt`:

```kotlin
@Database(
    entities = [...],
    version = 6,  // INCREMENT THIS
    exportSchema = false
)
```

### Step 3: Create Migration Object

Add migration in `KosmosDatabase.kt`:

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new column with default value
        database.execSQL("ALTER TABLE tasks ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
    }
}
```

### Step 4: Register Migration

Add to `Module.kt`:

```kotlin
.addMigrations(
    KosmosDatabase.MIGRATION_1_2,
    KosmosDatabase.MIGRATION_2_3,
    KosmosDatabase.MIGRATION_3_4,
    KosmosDatabase.MIGRATION_4_5,
    KosmosDatabase.MIGRATION_5_6  // ADD HERE
)
```

### Step 5: Create Supabase Migration

Create SQL file in `documents/04-DATABASE/`:

**File**: `MIGRATION_5_6_ADD_TASK_ARCHIVED.sql`
```sql
-- Migration 5→6: Add isArchived field to tasks
ALTER TABLE public.tasks
ADD COLUMN IF NOT EXISTS is_archived BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_tasks_is_archived
ON public.tasks(is_archived);
```

Run this in Supabase SQL Editor.

### Step 6: Test Migration

1. **Export current database:**
   ```bash
   adb exec-out run-as com.example.kosmos cat databases/kosmos_database > backup.db
   ```

2. **Install new version** (with migration)

3. **Verify data persists:**
   - Check all tables have data
   - Verify new field exists with default values

---

## Migration Patterns

### Adding a Column (Nullable)

```kotlin
database.execSQL("ALTER TABLE users ADD COLUMN bio TEXT")
```

### Adding a Column (Non-Nullable with Default)

```kotlin
database.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
```

### Creating a New Table

```kotlin
database.execSQL("""
    CREATE TABLE IF NOT EXISTS milestones (
        id TEXT PRIMARY KEY NOT NULL,
        project_id TEXT NOT NULL,
        title TEXT NOT NULL,
        due_date INTEGER,
        FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE
    )
""".trimIndent())

// Add indexes
database.execSQL("CREATE INDEX IF NOT EXISTS index_milestones_project_id ON milestones(project_id)")
```

### Dropping a Column (SQLite Workaround)

SQLite doesn't support `DROP COLUMN` directly. Use table recreation:

```kotlin
// 1. Create new table without the column
database.execSQL("""
    CREATE TABLE tasks_new (
        id TEXT PRIMARY KEY NOT NULL,
        title TEXT NOT NULL
        -- old_column REMOVED
    )
""")

// 2. Copy data
database.execSQL("INSERT INTO tasks_new SELECT id, title FROM tasks")

// 3. Drop old table
database.execSQL("DROP TABLE tasks")

// 4. Rename new table
database.execSQL("ALTER TABLE tasks_new RENAME TO tasks")

// 5. Recreate indexes
database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_title ON tasks(title)")
```

### Renaming a Column (SQLite 3.25.0+)

```kotlin
database.execSQL("ALTER TABLE users RENAME COLUMN photo_url TO avatar_url")
```

---

## Common Mistakes

### ❌ WRONG: Forgetting to increment version

```kotlin
@Database(version = 5)  // Still 5!
```
**Result**: Migration never runs, schema mismatch crash

### ❌ WRONG: Not registering migration

```kotlin
// Created MIGRATION_5_6 but forgot to add to Module.kt
.addMigrations(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5
    // MIGRATION_5_6 missing!
)
```
**Result**: App crashes on update

### ❌ WRONG: Non-null column without default

```kotlin
database.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL")
```
**Result**: Fails if table has existing rows

### ✅ CORRECT: Non-null with default

```kotlin
database.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
```

---

## Testing Migrations

### Manual Test Script

```bash
# 1. Install old version
./gradlew installDebug

# 2. Add test data
# (Use app to create projects, tasks, etc.)

# 3. Export database
adb exec-out run-as com.example.kosmos cat databases/kosmos_database > before_migration.db

# 4. Install new version (with migration)
./gradlew installDebug

# 5. Verify app doesn't crash
adb logcat | grep "Migration"

# 6. Export database again
adb exec-out run-as com.example.kosmos cat databases/kosmos_database > after_migration.db

# 7. Compare schemas
sqlite3 before_migration.db ".schema tasks"
sqlite3 after_migration.db ".schema tasks"
```

### Automated Test (TODO)

Create `MigrationTest.kt` in `app/src/androidTest/`:

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KosmosDatabase::class.java
    )

    @Test
    fun migrate5To6() {
        // Create database at version 5
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL("INSERT INTO tasks (id, title) VALUES ('1', 'Test Task')")
            close()
        }

        // Run migration to version 6
        helper.runMigrationsAndValidate(TEST_DB, 6, true, KosmosDatabase.MIGRATION_5_6)

        // Verify migration
        val db = helper.getMigrationDb()
        val cursor = db.query("SELECT isArchived FROM tasks WHERE id = '1'")
        cursor.moveToFirst()
        assertEquals(0, cursor.getInt(0))  // Default value
    }
}
```

---

## Emergency Rollback

If migration causes critical issues in production:

### Option 1: Hotfix Migration

1. Increment version again (e.g., 6 → 7)
2. Create migration that reverts changes
3. Release emergency update

### Option 2: Data Export/Import

1. Export user data via Supabase
2. Release app with destructive migration (emergency only!)
3. Reimport data after migration

**WARNING**: Option 2 causes data loss for offline users!

---

## Supabase Migration Sync

**CRITICAL**: Room migrations must match Supabase schema!

### Workflow:

1. Create Room migration (Android)
2. Create SQL migration (Supabase)
3. Run SQL migration BEFORE releasing Android update
4. Test with Supabase Test Project first

### Verification Query:

```sql
-- Check column exists in Supabase
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'tasks'
  AND column_name = 'is_archived';
```

---

## References

- [Room Migration Docs](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [SQLite ALTER TABLE](https://www.sqlite.org/lang_altertable.html)
- [Supabase Migrations](https://supabase.com/docs/guides/database/migrations)

---

**Last Updated**: 2026-01-24 (P0-04 Fix - Destructive Migrations Removed)
**Next Migration**: 5 → 6 (TBD)
