-- ============================================================================
-- VERIFY AND FIX ARRAY DATA TYPES
-- ============================================================================
-- Purpose: Check if tech_stack, tags, and industry_tags are TEXT[] or TEXT
--          and provide conversion scripts if needed
--
-- Issue: If columns are TEXT instead of TEXT[], array queries won't work
--        Room converters expect JSON strings, Supabase expects arrays
--
-- Run this in: Supabase SQL Editor
-- Estimated time: <5 seconds (verification), ~30 seconds (conversion if needed)
-- ============================================================================

-- Step 1: Check Current Data Types
-- ============================================================================

SELECT
    table_name,
    column_name,
    data_type,
    udt_name,
    CASE
        WHEN data_type = 'ARRAY' THEN '✅ Array type (optimal for queries)'
        WHEN data_type = 'text' THEN '⚠️  Text type (requires conversion for array queries)'
        ELSE '❓ Unknown type'
    END as status
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name IN ('projects', 'tasks')
AND column_name IN ('tech_stack', 'tags', 'industry_tags', 'tags')
ORDER BY table_name, column_name;

-- Expected Output (if already TEXT[]):
-- +------------+---------------+-----------+----------+----------------------------------+
-- | table_name | column_name   | data_type | udt_name | status                           |
-- +------------+---------------+-----------+----------+----------------------------------+
-- | projects   | industry_tags | ARRAY     | _text    | ✅ Array type (optimal)          |
-- | projects   | tags          | ARRAY     | _text    | ✅ Array type (optimal)          |
-- | projects   | tech_stack    | ARRAY     | _text    | ✅ Array type (optimal)          |
-- | tasks      | tags          | ARRAY     | _text    | ✅ Array type (optimal)          |
-- +------------+---------------+-----------+----------+----------------------------------+

-- Step 2: Sample Data Inspection
-- ============================================================================

-- Check how data is currently stored
SELECT
    id,
    name,
    tech_stack,
    pg_typeof(tech_stack) as tech_stack_type,
    tags,
    pg_typeof(tags) as tags_type,
    industry_tags,
    pg_typeof(industry_tags) as industry_tags_type
FROM public.projects
WHERE tech_stack IS NOT NULL
   OR tags IS NOT NULL
   OR industry_tags IS NOT NULL
LIMIT 5;

-- If TEXT[]: Output will show arrays like {"Kotlin","Android"}
-- If TEXT: Output will show strings like "Kotlin,Android" or '["Kotlin","Android"]'

-- Step 3: Test Array Query Capabilities
-- ============================================================================

-- Test 1: Check if array operators work
DO $$
BEGIN
    -- Try array containment query
    PERFORM 1 FROM public.projects
    WHERE tech_stack @> ARRAY['Kotlin']::TEXT[]
    LIMIT 1;

    RAISE NOTICE '✅ Array queries work - columns are TEXT[]';
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE '⚠️  Array queries failed - columns may be TEXT (not TEXT[])';
        RAISE NOTICE 'Error: %', SQLERRM;
END $$;

-- ============================================================================
-- CONVERSION SCRIPTS (Run ONLY if columns are TEXT, not TEXT[])
-- ============================================================================

-- ⚠️  WARNING: BACKUP YOUR DATA FIRST!
-- CREATE TABLE projects_backup AS SELECT * FROM public.projects;

-- Option A: Convert TEXT to TEXT[] (if data is comma-separated)
-- ============================================================================

-- UNCOMMENT ONLY IF VERIFICATION SHOWS TEXT TYPE:

/*
-- Projects: tech_stack
ALTER TABLE public.projects
ALTER COLUMN tech_stack TYPE TEXT[]
USING CASE
    WHEN tech_stack IS NULL OR tech_stack = '' THEN NULL
    WHEN tech_stack LIKE '{%}' THEN tech_stack::TEXT[]  -- Already array format
    WHEN tech_stack LIKE '[%]' THEN (
        -- JSON array format
        SELECT array_agg(value::TEXT)
        FROM json_array_elements_text(tech_stack::JSON)
    )
    ELSE string_to_array(tech_stack, ',')  -- Comma-separated
END;

-- Projects: tags
ALTER TABLE public.projects
ALTER COLUMN tags TYPE TEXT[]
USING CASE
    WHEN tags IS NULL OR tags = '' THEN NULL
    WHEN tags LIKE '{%}' THEN tags::TEXT[]
    WHEN tags LIKE '[%]' THEN (
        SELECT array_agg(value::TEXT)
        FROM json_array_elements_text(tags::JSON)
    )
    ELSE string_to_array(tags, ',')
END;

-- Projects: industry_tags
ALTER TABLE public.projects
ALTER COLUMN industry_tags TYPE TEXT[]
USING CASE
    WHEN industry_tags IS NULL OR industry_tags = '' THEN NULL
    WHEN industry_tags LIKE '{%}' THEN industry_tags::TEXT[]
    WHEN industry_tags LIKE '[%]' THEN (
        SELECT array_agg(value::TEXT)
        FROM json_array_elements_text(industry_tags::JSON)
    )
    ELSE string_to_array(industry_tags, ',')
END;

-- Tasks: tags
ALTER TABLE public.tasks
ALTER COLUMN tags TYPE TEXT[]
USING CASE
    WHEN tags IS NULL OR tags = '' THEN NULL
    WHEN tags LIKE '{%}' THEN tags::TEXT[]
    WHEN tags LIKE '[%]' THEN (
        SELECT array_agg(value::TEXT)
        FROM json_array_elements_text(tags::JSON)
    )
    ELSE string_to_array(tags, ',')
END;
*/

-- Option B: Add GIN Indexes for Fast Array Queries (after conversion)
-- ============================================================================

/*
-- UNCOMMENT AFTER CONVERTING TO TEXT[]:

CREATE INDEX IF NOT EXISTS idx_projects_tech_stack_gin
ON public.projects USING GIN(tech_stack);

CREATE INDEX IF NOT EXISTS idx_projects_tags_gin
ON public.projects USING GIN(tags);

CREATE INDEX IF NOT EXISTS idx_projects_industry_tags_gin
ON public.projects USING GIN(industry_tags);

CREATE INDEX IF NOT EXISTS idx_tasks_tags_gin
ON public.tasks USING GIN(tags);

COMMENT ON INDEX idx_projects_tech_stack_gin IS 'Fast array containment queries for tech stack';
COMMENT ON INDEX idx_projects_tags_gin IS 'Fast array containment queries for project tags';
COMMENT ON INDEX idx_projects_industry_tags_gin IS 'Fast array containment queries for industry tags';
COMMENT ON INDEX idx_tasks_tags_gin IS 'Fast array containment queries for task tags';
*/

-- Step 4: Post-Conversion Verification
-- ============================================================================

/*
-- Run AFTER conversion to confirm:

-- Test array operators
SELECT COUNT(*) FROM public.projects
WHERE tech_stack @> ARRAY['Kotlin']::TEXT[];  -- Should work

SELECT COUNT(*) FROM public.projects
WHERE 'Android' = ANY(tech_stack);  -- Should work

-- Verify data integrity
SELECT
    COUNT(*) as total_projects,
    COUNT(tech_stack) as projects_with_tech_stack,
    COUNT(tags) as projects_with_tags,
    COUNT(industry_tags) as projects_with_industry_tags
FROM public.projects;
*/

-- ============================================================================
-- ANDROID CODE IMPLICATIONS
-- ============================================================================

/*
Current Room Implementation:
```kotlin
@Entity(tableName = "projects")
data class Project(
    val techStack: String? = null,  // Stored as JSON string
    val tags: String? = null         // Stored as JSON string
)
```

If Supabase uses TEXT[]:
- Supabase stores as PostgreSQL array: {"Kotlin", "Android"}
- Room converter must convert between String and Array
- Sync code must handle conversion

Current Converter (Tokens.kt):
```kotlin
class StringListConverter {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun toString(list: List<String>?): String? {
        return list?.let { Json.encodeToString(it) }
    }
}
```

SupabaseProjectDataSource needs to:
1. Receive List<String> from Room
2. Send as TEXT[] to Supabase (automatic conversion)
3. Receive TEXT[] from Supabase
4. Convert back to JSON string for Room

Example:
```kotlin
// Supabase automatically converts List<String> to TEXT[]
set("tech_stack", project.techStack?.let {
    Json.decodeFromString<List<String>>(it)  // String → List
})

// Supabase returns TEXT[] as List<String>
val response = supabase.from("projects").select().single()
val techStack = response["tech_stack"] as? List<String>
project.copy(techStack = Json.encodeToString(techStack))  // List → String
```

RECOMMENDATION:
- Use TEXT[] in Supabase for query performance
- Keep String in Room for local storage
- Handle conversion in data sources
*/

-- ============================================================================
-- DECISION MATRIX
-- ============================================================================

/*
Should you convert TEXT to TEXT[]?

✅ YES - Convert to TEXT[] if:
- You need array queries (e.g., "find projects with Kotlin in tech_stack")
- You want GIN index performance
- Data is consistently formatted (comma-separated or JSON)

❌ NO - Keep as TEXT if:
- Data format is inconsistent
- Only need exact match queries
- Conversion would lose data
- Android app uses JSON strings everywhere

CURRENT RECOMMENDATION:
- Check verification output first
- If already TEXT[], do nothing (optimal)
- If TEXT, decide based on query needs
- Conversion is safe if data is well-formatted
*/
