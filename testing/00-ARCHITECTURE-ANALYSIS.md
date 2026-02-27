# Architecture Analysis

## What Kosmos Does

Collaborative project management app with real-time communication:
- **Projects** with categories (TECH, SOCIAL, BUSINESS, OTHER)
- **Tasks** with priority, status, assignee, due dates, time tracking
- **Chat** with real-time messaging, typing indicators, voice messages
- **Team Members** with RBAC (ADMIN > MANAGER > MEMBER, 31 permissions)
- **Activity Tracking** with audit logs per task
- **Offline-First** with local caching and automatic sync

## Tech Stack

- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Repository + Offline-First
- **DI**: Dagger Hilt
- **Local DB**: Room v11 (13 tables, 8 DAOs)
- **Remote DB**: Supabase PostgreSQL (6+ tables + RLS)
- **Auth**: Supabase Auth + Google OAuth
- **Real-time**: Supabase Realtime WebSocket
- **Build**: Kotlin 1.9+, MinSDK 26, TargetSDK 36

## Data Flow

```
UI (Compose) → ViewModel (StateFlow) → Repository → Room (immediate) + Supabase (async)
                                                    ↓
                                              Sync Queue (offline retry)
                                                    ↓
                                              Supabase Realtime → Room → ViewModel → UI
```

**Key Principle**: Offline-first — Room updates immediately, Supabase syncs in background. Optimistic UI.

## Key Components

| Layer | Count | Examples |
|-------|-------|---------|
| ViewModels | 16 | TaskViewModel, ChatViewModel, ProjectViewModel, AuthViewModel |
| Repositories | 6 | TaskRepository, ChatRepository, ProjectRepository |
| Data Sources | 10 | SupabaseTaskDataSource, SupabaseRealtimeManager |
| Room DAOs | 8 | TaskDao, MessageDao, ProjectMemberDao |
| Room Tables | 13 | users, projects, tasks, messages, sync_queue, etc. |
| Supabase Tables | 6+ | users, projects, project_members, chat_rooms, messages, tasks |
| Migrations | 11 | V1→V11 (includes FK fix, sync timestamps, time entries) |

## RBAC System

- **ADMIN** (weight 3): All 31 permissions
- **MANAGER** (weight 2): All except project deletion, member removal/role changes
- **MEMBER** (weight 1): View project, edit own tasks, basic chat
- **Custom permissions**: Override default role permissions per member
- **Enforcement**: Repository layer (pre-request) + Supabase RLS (server-side)

## Sync Infrastructure

- `InitialSyncManager` — Project-centric incremental sync
- `SyncQueueManager` — Failed operation retry queue
- `FKRetryQueue` — FK constraint violation retry
- `SyncRetryHelper` — Exponential backoff

## Current State

- Build: Compiles successfully (~1min cached)
- Tests: 0% coverage (2 placeholder files only)
- Room DB: Version 11 with all migrations
- RLS: **CRITICAL** — disabled on 7 core tables (production blocker)
