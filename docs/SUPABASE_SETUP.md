# Supabase Setup Guide for Kosmos

This guide walks you through setting up Supabase for the Kosmos application. Supabase provides the backend infrastructure including PostgreSQL database, file storage, and real-time subscriptions.

## Prerequisites

- Supabase account (free tier is sufficient)
- Android Studio with Kosmos project opened
- Basic understanding of SQL and PostgreSQL

---

## Step 1: Create Supabase Project

### 1.1 Sign Up / Login to Supabase

1. Go to [https://supabase.com](https://supabase.com)
2. Click "Start your project" or "Sign In"
3. Sign in with GitHub (recommended) or email

### 1.2 Create New Project

1. Click "New Project" in the dashboard
2. Fill in project details:
   - **Name**: `kosmos-dev` (or your preferred name)
   - **Database Password**: Generate a strong password (save this securely!)
   - **Region**: Choose closest to your location (e.g., `us-east-1`, `eu-central-1`)
   - **Pricing Plan**: Free (includes 500MB database, 1GB storage, 2GB bandwidth)
3. Click "Create new project"
4. Wait 2-5 minutes for project provisioning

### 1.3 Get Project Credentials

Once your project is ready:

1. Go to **Settings** → **API** in the left sidebar
2. You'll need these two values:
   - **Project URL**: `https://[project-ref].supabase.co`
   - **anon public** key: A long JWT token (starts with `eyJ...`)

**⚠️ IMPORTANT**: Never commit these credentials to Git!

---

## Step 2: Configure Android Project

### 2.1 Create gradle.properties File

1. In the root of your Kosmos project, find or create `gradle.properties`
2. Add your Supabase credentials:

```properties
# Supabase Configuration (Development)
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_ANON_KEY=your-anon-key-here

# Optional: Production credentials (use different Supabase project)
SUPABASE_URL_PROD=https://your-prod-project.supabase.co
SUPABASE_ANON_KEY_PROD=your-prod-anon-key-here

# Google Cloud API Key (for Speech-to-Text - Phase 5)
GOOGLE_CLOUD_API_KEY=
```

### 2.2 Update .gitignore

Ensure `gradle.properties` is in your `.gitignore`:

```gitignore
# API Keys and secrets
gradle.properties
local.properties
```

### 2.3 Verify Build Configuration

The `app/build.gradle.kts` file already has BuildConfig fields configured:

```kotlin
buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_URL") ?: ""}\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.findProperty("SUPABASE_ANON_KEY") ?: ""}\"")
```

This allows you to access credentials in code via:
```kotlin
BuildConfig.SUPABASE_URL
BuildConfig.SUPABASE_ANON_KEY
```

---

## Step 3: Set Up Database Schema

### 3.1 Access SQL Editor

1. In Supabase dashboard, go to **SQL Editor** in the left sidebar
2. Click "New query"

### 3.2 Run Migration Scripts

Execute the SQL scripts in order from the `/supabase/migrations/` directory:

1. **01_create_tables.sql** - Creates all database tables
   - users
   - chat_rooms
   - chat_room_participants
   - messages
   - tasks
   - task_comments
   - voice_messages
   - action_items

2. **02_create_indexes.sql** - Creates indexes for performance

3. **03_create_rls_policies.sql** - Sets up Row Level Security

4. **04_create_triggers.sql** - Creates triggers for auto-updating timestamps

**How to run**:
- Copy the contents of each SQL file
- Paste into SQL Editor
- Click "Run" (or press Ctrl/Cmd + Enter)
- Verify "Success" message
- Repeat for each file in order

### 3.3 Verify Tables Created

1. Go to **Database** → **Tables** in the left sidebar
2. You should see all 8 tables listed
3. Click on any table to view its schema and data

---

## Step 4: Set Up Storage Buckets

### 4.1 Access Storage

1. Go to **Storage** in the left sidebar
2. Click "New bucket"

### 4.2 Create Buckets

Create three storage buckets:

#### Bucket 1: voice-messages
- **Name**: `voice-messages`
- **Public bucket**: ✅ Yes (allows public read access)
- **File size limit**: 5MB
- **Allowed MIME types**: `audio/*`

#### Bucket 2: profile-photos
- **Name**: `profile-photos`
- **Public bucket**: ✅ Yes
- **File size limit**: 2MB
- **Allowed MIME types**: `image/*`

#### Bucket 3: chat-files
- **Name**: `chat-files`
- **Public bucket**: ✅ Yes
- **File size limit**: 10MB
- **Allowed MIME types**: `*/*` (all file types)

### 4.3 Configure Storage Policies

For each bucket, set up access policies:

1. Click on bucket name
2. Go to **Policies** tab
3. Run the SQL script from `/supabase/storage/setup_buckets.sql`

This ensures:
- Anyone can read public files
- Only authenticated users can upload
- Users can only delete their own files

---

## Step 5: Configure Row Level Security (RLS)

RLS ensures users can only access data they're authorized to see.

### 5.1 Why RLS?

Without RLS, any user with the anon key could access all data. RLS policies filter data based on the authenticated user's ID.

### 5.2 Verify RLS Enabled

1. Go to **Database** → **Tables**
2. For each table, verify RLS is enabled:
   - **users**: ✅ RLS enabled
   - **chat_rooms**: ✅ RLS enabled
   - **messages**: ✅ RLS enabled
   - **tasks**: ✅ RLS enabled
   - etc.

### 5.3 Test RLS Policies

The RLS policies allow:
- Users can read their own profile
- Users can read chat rooms they're a participant in
- Users can read messages from their chat rooms
- Users can read tasks from their chat rooms
- Users can insert/update their own data

Test by:
1. Creating a test user in Auth
2. Using that user's JWT to query the API
3. Verifying they can only see authorized data

---

## Step 6: Enable Realtime

Realtime allows instant updates across all connected clients.

### 6.1 Enable Realtime for Tables

1. Go to **Database** → **Replication**
2. Enable realtime for these tables:
   - ✅ messages
   - ✅ tasks
   - ✅ chat_rooms
   - ✅ users (for online status)
   - ✅ task_comments

3. Click "Save changes"

### 6.2 Test Realtime

You can test realtime in the SQL Editor:

```sql
-- Insert a test message
INSERT INTO messages (id, chat_room_id, sender_id, content)
VALUES ('test-1', 'room-1', 'user-1', 'Hello realtime!');
```

If realtime is working, subscribed clients will receive this immediately.

---

## Step 7: Verify Setup

### 7.1 Checklist

- [ ] Supabase project created and active
- [ ] Project URL and anon key saved in gradle.properties
- [ ] .gitignore includes gradle.properties
- [ ] All 8 database tables created
- [ ] Indexes created
- [ ] RLS policies enabled and configured
- [ ] Triggers created
- [ ] 3 storage buckets created with policies
- [ ] Realtime enabled for key tables
- [ ] App builds successfully with credentials

### 7.2 Test Connection

Once you've implemented the `SupabaseConfig.kt` file (next step), you can test the connection:

```kotlin
// In MainActivity or a test screen
lifecycleScope.launch {
    try {
        val supabase = SupabaseConfig.client
        // Try a simple query
        val response = supabase.from("users").select().limit(1).execute()
        Log.d("Supabase", "Connection successful: $response")
    } catch (e: Exception) {
        Log.e("Supabase", "Connection failed", e)
    }
}
```

---

## Step 8: Monitor Usage (Free Tier Limits)

### 8.1 Access Usage Dashboard

1. Go to **Settings** → **Usage** in Supabase dashboard
2. Monitor your usage:
   - **Database**: 500 MB limit
   - **Storage**: 1 GB limit
   - **Bandwidth**: 2 GB/month limit

### 8.2 Set Up Usage Alerts

1. Go to **Settings** → **Notifications**
2. Enable alerts for:
   - Database size at 80%
   - Storage at 80%
   - Bandwidth at 80%

### 8.3 Optimization Tips

To stay within free tier:
- **Compress images** before uploading (max 512x512 for profiles)
- **Implement pagination** (load 20-50 items at a time)
- **Use Room caching** to minimize Supabase queries
- **Delete old messages** after 90 days (optional retention policy)
- **Limit voice message length** to 5 minutes max

---

## Troubleshooting

### Issue: "Invalid API Key" error

**Solution**:
- Verify your `SUPABASE_ANON_KEY` is correct
- Check it's the **anon public** key, not the service role key
- Ensure no extra spaces or quotes in gradle.properties
- Run `./gradlew clean build` to refresh BuildConfig

### Issue: "Network request failed"

**Solution**:
- Verify `SUPABASE_URL` is correct (should include `https://`)
- Check internet connection
- Ensure Supabase project is active (not paused)
- Check Supabase status page: https://status.supabase.com

### Issue: "Row Level Security policy violation"

**Solution**:
- Ensure user is authenticated before querying
- Verify RLS policies are configured correctly
- Check user has permission to access the data
- Review `/supabase/migrations/03_create_rls_policies.sql`

### Issue: Tables not visible in Supabase dashboard

**Solution**:
- Verify SQL scripts ran without errors
- Check SQL Editor history for error messages
- Try running scripts one at a time
- Check PostgreSQL logs in Supabase dashboard

### Issue: "Storage bucket not found"

**Solution**:
- Verify bucket names match exactly (case-sensitive)
- Check bucket was created in correct project
- Ensure storage policies are configured
- Restart app after creating buckets

---

## Security Best Practices

### 1. Never Commit Credentials
- Always use `gradle.properties` for secrets
- Keep `gradle.properties` in `.gitignore`
- Use environment variables in CI/CD

### 2. Use Different Projects for Dev/Prod
- Create separate Supabase projects:
  - `kosmos-dev` for development
  - `kosmos-prod` for production
- Use different credentials for each
- Never test with production data

### 3. Implement Proper RLS
- Test RLS policies thoroughly
- Ensure users can only access their data
- Use service role key only in backend (never in app)
- Audit RLS policies regularly

### 4. Monitor Usage
- Set up usage alerts
- Review logs regularly
- Implement rate limiting if needed
- Clean up old data periodically

### 5. Secure Storage
- Use signed URLs for sensitive files
- Set appropriate expiration times
- Implement file size limits
- Validate file types server-side

---

## Next Steps

After completing this setup:

1. ✅ **Implement SupabaseConfig.kt** - Initialize Supabase client in Android app
2. ✅ **Create Data Sources** - Implement CRUD operations for each entity
3. ✅ **Update Repositories** - Integrate Supabase with existing repository pattern
4. ✅ **Test Integration** - Verify data flows correctly
5. ✅ **Implement Realtime** - Add realtime subscriptions for live updates

Refer to `DEVELOPMENT_LOGBOOK.md` for detailed implementation steps.

---

## Additional Resources

- [Supabase Documentation](https://supabase.com/docs)
- [Supabase Kotlin Client](https://github.com/supabase-community/supabase-kt)
- [Row Level Security Guide](https://supabase.com/docs/guides/auth/row-level-security)
- [Storage Guide](https://supabase.com/docs/guides/storage)
- [Realtime Guide](https://supabase.com/docs/guides/realtime)

---

**Setup Guide Version**: 1.0
**Last Updated**: 2025-10-23
**Kosmos Version**: 1.0.0 (MVP)
