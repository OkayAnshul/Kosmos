# Supabase SQL Setup - Quick Start Guide

**Your Supabase Project:** https://krbfvekgqbcwjgntepip.supabase.co

This guide helps you set up the database schema for Kosmos with RBAC support.

## Step 1: Access SQL Editor

1. Go to: https://krbfvekgqbcwjgntepip.supabase.co/project/krbfvekgqbcwjgntepip/sql
2. Click **"New query"** button
3. Copy-paste each script below **one at a time**
4. Click **"Run"** after each script
5. Verify "Success. No rows returned" message

---

## Step 2: Run SQL Scripts (Copy-Paste in Order)

### Script 1/7: Users Table ✅
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

-- Indexes for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_online ON users(is_online);
```
**✅ Click "Run" and wait for success message**

---

### Script 2/7: Projects Table (RBAC) ✅
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
**✅ Click "Run" and wait for success message**

---

### Script 3/7: Project Members Table (RBAC) ✅
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
**✅ Click "Run" and wait for success message**

---

### Script 4/7: Chat Rooms Table ✅
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
**✅ Click "Run" and wait for success message**

---

### Script 5/7: Messages Table ✅
```sql
-- Messages table
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_room_id UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id),
    type TEXT DEFAULT 'TEXT', -- TEXT, VOICE, IMAGE, FILE, SYSTEM
    content TEXT,
    voice_message_id UUID,
    timestamp BIGINT DEFAULT extract(epoch from now())::bigint * 1000,
    is_edited BOOLEAN DEFAULT false,
    edited_at BIGINT,
    is_deleted BOOLEAN DEFAULT false,
    deleted_at BIGINT,
    reply_to_id UUID REFERENCES messages(id),
    reactions JSONB DEFAULT '[]',
    metadata JSONB,
    CONSTRAINT messages_type_check CHECK (type IN ('TEXT', 'VOICE', 'IMAGE', 'FILE', 'SYSTEM', 'TASK_CREATED'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_messages_chat_room ON messages(chat_room_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_type ON messages(type);
CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp DESC);
```
**✅ Click "Run" and wait for success message**

---

### Script 6/7: Tasks Table (RBAC-Enabled) ✅
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
**✅ Click "Run" and wait for success message**

---

### Script 7/7: Task Comments Table ✅
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
**✅ Click "Run" and wait for success message**

---

## Step 3: Verify Tables Created

Run this query to check all tables exist:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_type = 'BASE TABLE'
ORDER BY table_name;
```

**Expected Result:** You should see:
- chat_rooms
- messages
- project_members ← NEW (RBAC)
- projects ← NEW (RBAC)
- task_comments
- tasks
- users

---

## Step 4: Enable Row Level Security (RLS) - Optional for MVP

For production, you should enable RLS. For MVP testing, you can skip this.

To enable basic RLS:

```sql
-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_comments ENABLE ROW LEVEL SECURITY;

-- Allow authenticated users to read/write their own data
-- (Add more specific policies as needed)
```

---

## ✅ Database Setup Complete!

Your Supabase database is now configured with:
- ✅ 7 tables created
- ✅ RBAC system (projects, project_members with roles)
- ✅ All indexes for performance
- ✅ Foreign key relationships
- ✅ Check constraints for data integrity

**Next Step:** Test the app! The Android app should now be able to connect to your Supabase backend.

**Troubleshooting:**
- If queries fail, check the error message in the SQL Editor
- Make sure you run scripts in order (tables depend on each other)
- If you need to start over: Delete all tables and run scripts again
