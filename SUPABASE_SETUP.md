# Supabase Setup Guide for Kosmos MVP

This guide walks you through setting up the Supabase backend for the Kosmos Android application.

## Prerequisites
- A Supabase account (sign up at https://supabase.com)
- Basic SQL knowledge
- Access to this project's `gradle.properties` file

## Step 1: Create Supabase Project

1. Go to https://supabase.com and sign in/sign up
2. Click "New Project"
3. Fill in the project details:
   - **Name**: `kosmos-dev` (or your preferred name)
   - **Database Password**: Generate a strong password (save this securely!)
   - **Region**: Choose closest to your users (e.g., `us-east-1`)
   - **Pricing Plan**: Select **Free tier**
4. Click "Create new project"
5. Wait 2-5 minutes for provisioning to complete

## Step 2: Get API Credentials

1. Once the project is ready, go to **Settings** → **API**
2. Copy the following values:
   - **Project URL**: `https://[your-project-id].supabase.co`
   - **anon/public key**: A long JWT token (starts with `eyJ...`)

3. Add these to your `gradle.properties` file:
   ```properties
   SUPABASE_URL=https://[your-project-id].supabase.co
   SUPABASE_ANON_KEY=eyJhbGc...your-key-here
   ```

## Step 3: Create Database Schema

Navigate to **SQL Editor** in the Supabase dashboard and run the following SQL scripts **in order**:

### 3.1 Create Users Table
```sql
-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    photo_url TEXT,
    fcm_token TEXT,
    is_online BOOLEAN DEFAULT false,
    last_seen BIGINT DEFAULT 0,
    created_at BIGINT DEFAULT extract(epoch from now())::bigint * 1000,
    CONSTRAINT users_email_check CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

-- Index for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_online ON users(is_online);
```

### 3.2 Create Projects Table (RBAC System)
```sql
-- Projects table - Core entity for project management
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT DEFAULT '',
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'ACTIVE', -- ACTIVE, ARCHIVED, COMPLETED, ON_HOLD
    visibility TEXT DEFAULT 'PRIVATE', -- PRIVATE, INTERNAL, PUBLIC
    created_at BIGINT DEFAULT extract(epoch from now())::bigint * 1000,
    updated_at BIGINT DEFAULT extract(epoch from now())::bigint * 1000,
    image_url TEXT,
    color TEXT DEFAULT '#6366F1',
    settings JSONB,
    CONSTRAINT projects_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED', 'COMPLETED', 'ON_HOLD')),
    CONSTRAINT projects_visibility_check CHECK (visibility IN ('PRIVATE', 'INTERNAL', 'PUBLIC'))
);

-- Indexes for project queries
CREATE INDEX IF NOT EXISTS idx_projects_owner ON projects(owner_id);
CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);
CREATE INDEX IF NOT EXISTS idx_projects_updated ON projects(updated_at DESC);
```

### 3.3 Create Project Members Table (RBAC System)
```sql
-- Project members table - Manages user roles and permissions in projects
CREATE TABLE IF NOT EXISTS project_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'MEMBER', -- ADMIN, MANAGER, MEMBER
    joined_at BIGINT DEFAULT extract(epoch from now())::bigint * 1000,
    invited_by UUID REFERENCES users(id),
    is_active BOOLEAN DEFAULT true,
    last_activity_at BIGINT DEFAULT extract(epoch from now())::bigint * 1000,
    custom_permissions JSONB, -- Optional custom permissions override
    UNIQUE(project_id, user_id),
    CONSTRAINT project_members_role_check CHECK (role IN ('ADMIN', 'MANAGER', 'MEMBER'))
);

-- Indexes for member queries
CREATE INDEX IF NOT EXISTS idx_project_members_project ON project_members(project_id);
CREATE INDEX IF NOT EXISTS idx_project_members_user ON project_members(user_id);
CREATE INDEX IF NOT EXISTS idx_project_members_role ON project_members(project_id, role);
CREATE INDEX IF NOT EXISTS idx_project_members_active ON project_members(project_id, is_active);
```

### 3.5 Create Chat Rooms Table
```sql
-- Chat rooms table - Now linked to projects
CREATE TABLE IF NOT EXISTS chat_rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    type TEXT DEFAULT 'GENERAL', -- GENERAL, DIRECT, CHANNEL, TASK_DISCUSSION, ANNOUNCEMENTS
    created_by UUID NOT NULL REFERENCES users(id),
    created_at BIGINT DEFAULT extract(epoch from now())::bigint * 1000,
    last_message_timestamp BIGINT DEFAULT extract(epoch from now())::bigint * 1000,
    last_message TEXT,
    last_message_id UUID,
    is_task_board_enabled BOOLEAN DEFAULT true,
    is_archived BOOLEAN DEFAULT false,
    is_private BOOLEAN DEFAULT false,
    image_url TEXT,
    CONSTRAINT chat_rooms_type_check CHECK (type IN ('GENERAL', 'DIRECT', 'CHANNEL', 'TASK_DISCUSSION', 'ANNOUNCEMENTS'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_chat_rooms_project ON chat_rooms(project_id);
CREATE INDEX IF NOT EXISTS idx_chat_rooms_created_by ON chat_rooms(created_by);
CREATE INDEX IF NOT EXISTS idx_chat_rooms_timestamp ON chat_rooms(last_message_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_chat_rooms_type ON chat_rooms(project_id, type);
```

### 3.3 Create Chat Room Participants Junction Table
```sql
-- Junction table for many-to-many relationship between users and chat rooms
CREATE TABLE IF NOT EXISTS chat_room_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_room_id UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role TEXT DEFAULT 'MEMBER', -- ADMIN, MEMBER
    joined_at BIGINT DEFAULT extract(epoch from now())::bigint * 1000,
    UNIQUE(chat_room_id, user_id)
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_participants_chat_room ON chat_room_participants(chat_room_id);
CREATE INDEX IF NOT EXISTS idx_participants_user ON chat_room_participants(user_id);
```

### 3.4 Create Messages Table
```sql
-- Messages table
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_room_id UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id),
    sender_name TEXT NOT NULL,
    sender_avatar TEXT,
    type TEXT DEFAULT 'TEXT', -- TEXT, VOICE, IMAGE, FILE, SYSTEM, TASK_CREATED
    content TEXT NOT NULL,
    timestamp BIGINT DEFAULT extract(epoch from now())::bigint * 1000,
    read_by UUID[] DEFAULT '{}',
    reactions JSONB DEFAULT '{}',
    metadata JSONB DEFAULT '{}',
    reply_to_message_id UUID REFERENCES messages(id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_messages_chat_room ON messages(chat_room_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_type ON messages(type);
CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp DESC);
```

### 3.7 Create Tasks Table (RBAC-Enabled)
```sql
-- Tasks table - Now linked to projects with role-based assignment
CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    chat_room_id UUID REFERENCES chat_rooms(id) ON DELETE SET NULL, -- Optional discussion room
    title TEXT NOT NULL,
    description TEXT,
    status TEXT DEFAULT 'TODO', -- TODO, IN_PROGRESS, DONE, CANCELLED
    priority TEXT DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
    assigned_to_id UUID REFERENCES users(id),
    assigned_to_name TEXT,
    assigned_to_role TEXT, -- Role at time of assignment (ADMIN, MANAGER, MEMBER)
    created_by_id UUID NOT NULL REFERENCES users(id),
    created_by_name TEXT NOT NULL,
    created_by_role TEXT, -- Role at time of creation
    due_date BIGINT,
    source_message_id UUID REFERENCES messages(id),
    parent_task_id UUID REFERENCES tasks(id) ON DELETE CASCADE, -- For subtasks (Phase 2)
    estimated_hours REAL,
    actual_hours REAL,
    tags TEXT[] DEFAULT '{}',
    created_at BIGINT DEFAULT extract(epoch from now())::bigint * 1000,
    updated_at BIGINT DEFAULT extract(epoch from now())::bigint * 1000,
    CONSTRAINT tasks_status_check CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    CONSTRAINT tasks_priority_check CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT tasks_assigned_role_check CHECK (assigned_to_role IS NULL OR assigned_to_role IN ('ADMIN', 'MANAGER', 'MEMBER')),
    CONSTRAINT tasks_created_role_check CHECK (created_by_role IS NULL OR created_by_role IN ('ADMIN', 'MANAGER', 'MEMBER'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_tasks_project ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_chat_room ON tasks(chat_room_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(project_id, status);
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to ON tasks(assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(project_id, priority);
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);
CREATE INDEX IF NOT EXISTS idx_tasks_parent ON tasks(parent_task_id); -- For subtasks
```

### 3.6 Create Task Comments Table
```sql
-- Task comments table (separate from messages for better normalization)
CREATE TABLE IF NOT EXISTS task_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id),
    author_name TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at BIGINT DEFAULT extract(epoch from now())::bigint * 1000
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_task_comments_task ON task_comments(task_id, created_at);
CREATE INDEX IF NOT EXISTS idx_task_comments_author ON task_comments(author_id);
```

### 3.7 Create Voice Messages Table
```sql
-- Voice messages table
CREATE TABLE IF NOT EXISTS voice_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    audio_file_path TEXT NOT NULL,
    duration_seconds INTEGER DEFAULT 0,
    transcription TEXT,
    transcription_confidence REAL,
    waveform_data TEXT, -- JSON array of amplitude values
    is_transcribed BOOLEAN DEFAULT false,
    created_at BIGINT DEFAULT extract(epoch from now())::bigint * 1000
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_voice_messages_message ON voice_messages(message_id);
CREATE INDEX IF NOT EXISTS idx_voice_messages_transcribed ON voice_messages(is_transcribed);
```

### 3.8 Create Action Items Table
```sql
-- Action items table (AI-detected tasks/action items from messages)
CREATE TABLE IF NOT EXISTS action_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    chat_room_id UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    detected_action TEXT NOT NULL,
    confidence REAL DEFAULT 0.0,
    suggested_assignee_id UUID REFERENCES users(id),
    is_converted_to_task BOOLEAN DEFAULT false,
    task_id UUID REFERENCES tasks(id),
    created_at BIGINT DEFAULT extract(epoch from now())::bigint * 1000
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_action_items_message ON action_items(message_id);
CREATE INDEX IF NOT EXISTS idx_action_items_chat_room ON action_items(chat_room_id);
CREATE INDEX IF NOT EXISTS idx_action_items_converted ON action_items(is_converted_to_task);
```

### 3.9 Create Trigger Functions
```sql
-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = extract(epoch from now())::bigint * 1000;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for tasks table
CREATE TRIGGER update_tasks_updated_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger to update chat room last_message_timestamp
CREATE OR REPLACE FUNCTION update_chat_room_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE chat_rooms
    SET
        last_message_timestamp = NEW.timestamp,
        last_message = LEFT(NEW.content, 100)
    WHERE id = NEW.chat_room_id;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_chat_room_on_message
    AFTER INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION update_chat_room_timestamp();
```

## Step 4: Enable Row Level Security (RLS)

Run these commands to enable RLS and create policies:

```sql
-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_room_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE voice_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE action_items ENABLE ROW LEVEL SECURITY;

-- RLS Policies for users table
CREATE POLICY "Users can view all profiles"
    ON users FOR SELECT
    USING (true);

CREATE POLICY "Users can update own profile"
    ON users FOR UPDATE
    USING (auth.uid()::text = id::text);

CREATE POLICY "Users can insert own profile"
    ON users FOR INSERT
    WITH CHECK (auth.uid()::text = id::text);

-- RLS Policies for chat_rooms table
CREATE POLICY "Users can view chat rooms they're participants in"
    ON chat_rooms FOR SELECT
    USING (
        id IN (
            SELECT chat_room_id FROM chat_room_participants
            WHERE user_id::text = auth.uid()::text
        )
    );

CREATE POLICY "Users can create chat rooms"
    ON chat_rooms FOR INSERT
    WITH CHECK (auth.uid()::text = created_by);

CREATE POLICY "Chat room admins can update rooms"
    ON chat_rooms FOR UPDATE
    USING (
        id IN (
            SELECT chat_room_id FROM chat_room_participants
            WHERE user_id::text = auth.uid()::text AND role = 'ADMIN'
        )
    );

-- RLS Policies for chat_room_participants
CREATE POLICY "Users can view participants in their chat rooms"
    ON chat_room_participants FOR SELECT
    USING (
        chat_room_id IN (
            SELECT chat_room_id FROM chat_room_participants
            WHERE user_id::text = auth.uid()::text
        )
    );

CREATE POLICY "Admins can manage participants"
    ON chat_room_participants FOR ALL
    USING (
        chat_room_id IN (
            SELECT chat_room_id FROM chat_room_participants
            WHERE user_id::text = auth.uid()::text AND role = 'ADMIN'
        )
    );

-- RLS Policies for messages table
CREATE POLICY "Users can view messages in their chat rooms"
    ON messages FOR SELECT
    USING (
        chat_room_id IN (
            SELECT chat_room_id FROM chat_room_participants
            WHERE user_id::text = auth.uid()::text
        )
    );

CREATE POLICY "Users can send messages in their chat rooms"
    ON messages FOR INSERT
    WITH CHECK (
        chat_room_id IN (
            SELECT chat_room_id FROM chat_room_participants
            WHERE user_id::text = auth.uid()::text
        ) AND
        sender_id::text = auth.uid()::text
    );

CREATE POLICY "Users can update own messages"
    ON messages FOR UPDATE
    USING (sender_id::text = auth.uid()::text);

CREATE POLICY "Users can delete own messages"
    ON messages FOR DELETE
    USING (sender_id::text = auth.uid()::text);

-- RLS Policies for tasks table
CREATE POLICY "Users can view tasks in their chat rooms"
    ON tasks FOR SELECT
    USING (
        chat_room_id IN (
            SELECT chat_room_id FROM chat_room_participants
            WHERE user_id::text = auth.uid()::text
        )
    );

CREATE POLICY "Users can create tasks in their chat rooms"
    ON tasks FOR INSERT
    WITH CHECK (
        chat_room_id IN (
            SELECT chat_room_id FROM chat_room_participants
            WHERE user_id::text = auth.uid()::text
        )
    );

CREATE POLICY "Users can update tasks in their chat rooms"
    ON tasks FOR UPDATE
    USING (
        chat_room_id IN (
            SELECT chat_room_id FROM chat_room_participants
            WHERE user_id::text = auth.uid()::text
        )
    );

-- Similar policies for task_comments, voice_messages, action_items...
-- (Add as needed - above are the critical ones for MVP)
```

## Step 5: Configure Storage Buckets

1. Go to **Storage** in the Supabase dashboard
2. Click **New bucket**

### Bucket 1: voice-messages
- **Name**: `voice-messages`
- **Public**: Yes (for easy playback)
- **File size limit**: 5 MB
- **Allowed MIME types**: `audio/*`

### Bucket 2: profile-photos
- **Name**: `profile-photos`
- **Public**: Yes
- **File size limit**: 2 MB
- **Allowed MIME types**: `image/*`

### Bucket 3: chat-files
- **Name**: `chat-files`
- **Public**: Yes
- **File size limit**: 10 MB
- **Allowed MIME types**: `image/*, application/pdf, application/msword, application/vnd.openxmlformats-officedocument.*`

### Storage Policies
For each bucket, create policies:

```sql
-- Example for voice-messages bucket
CREATE POLICY "Users can upload voice messages"
    ON storage.objects FOR INSERT
    WITH CHECK (
        bucket_id = 'voice-messages' AND
        auth.role() = 'authenticated'
    );

CREATE POLICY "Anyone can view voice messages"
    ON storage.objects FOR SELECT
    USING (bucket_id = 'voice-messages');

CREATE POLICY "Users can delete own voice messages"
    ON storage.objects FOR DELETE
    USING (
        bucket_id = 'voice-messages' AND
        auth.uid()::text = owner
    );
```

## Step 6: Enable Realtime

1. Go to **Database** → **Replication**
2. Enable replication for these tables:
   - ✅ `messages`
   - ✅ `tasks`
   - ✅ `chat_rooms`
   - ✅ `users` (for online status)
   - ✅ `task_comments`

3. Click **Save** on each table

## Step 7: Configure Authentication

1. Go to **Authentication** → **Providers**
2. Enable **Email** provider (enabled by default)
3. Enable **Google** provider:
   - Get Google OAuth credentials from Google Cloud Console
   - Add Redirect URL: `kosmos://auth-callback`
   - Paste Client ID and Client Secret
   - Click **Save**

## Step 8: Verify Setup

Run this query in **SQL Editor** to verify all tables exist:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

You should see:
- action_items
- chat_room_participants
- chat_rooms
- messages
- task_comments
- tasks
- users
- voice_messages

## Step 9: Test Connection from Android

1. Ensure `gradle.properties` has correct values
2. Build and run the app
3. Try signing up with email
4. Go to **Authentication** → **Users** in Supabase dashboard
5. You should see your new user

## Free Tier Limits

Monitor usage in **Settings** → **Billing**:

| Resource | Limit | Current Usage |
|----------|-------|---------------|
| Database Size | 500 MB | Check dashboard |
| Storage | 1 GB | Check dashboard |
| Bandwidth | 2 GB/month | Check dashboard |
| Realtime Connections | 200 concurrent | - |
| Edge Functions Invocations | 500K/month | - |

## Troubleshooting

### Build Config Fields Not Found
- Ensure `gradle.properties` has `SUPABASE_URL` and `SUPABASE_ANON_KEY`
- Run `./gradlew clean` and rebuild

### RLS Policy Errors
- Temporarily disable RLS during development: `ALTER TABLE [table_name] DISABLE ROW LEVEL SECURITY;`
- Re-enable before production

### Can't Insert Data
- Check RLS policies
- Verify user is authenticated
- Check table constraints

## Next Steps

After setup:
1. ✅ Build and test authentication
2. ✅ Test creating chat rooms and sending messages
3. ✅ Test creating and updating tasks
4. ✅ Monitor usage in Supabase dashboard
5. ✅ Set up monitoring alerts for approaching limits

## Support

- Supabase Docs: https://supabase.com/docs
- Supabase Discord: https://discord.supabase.com
- Project Issues: https://github.com/anthropics/kosmos/issues
