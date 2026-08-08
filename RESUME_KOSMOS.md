# Kosmos — Resume Source Document

> **Purpose:** A pool of pre-written, ATS-optimized content for the Kosmos project. Pick 4–6 lines depending on the role. Every number below is verifiable in the codebase.

---

## 1. Elevator pitches (1-liner under the project name)

- **New-grad:** *Solo-built, full-stack Android collaboration app with offline sync, realtime updates, and a 17-entity Postgres schema.*
- **Mid-level Android / SDE-2:** *Production-grade offline-first Android app with field-level conflict resolution, multi-channel realtime collaboration, and RBAC over a normalized Postgres schema.*
- **Startup / founding engineer:** *Solo-shipped a production-grade Android collaboration platform end-to-end — mobile, schema, realtime sync, RBAC, RLS, CI — in Kotlin + Supabase.*

---

## 2. ATS keyword line (drop verbatim into the project entry)

> **Tech:** Kotlin 2.2, Jetpack Compose, Material 3, MVVM, Hilt, Room 2.8, Coroutines, Flow, supabase-kt 3.2, PostgreSQL, Supabase Realtime (WebSockets), Supabase Auth (OAuth 2.0), Row-Level Security, WorkManager, Coil, JUnit, MockK, Robolectric, Turbine, GitHub Actions, Jacoco, ProGuard/R8.

---

## 3. Verified metrics (use these numbers — they're real)

| Metric | Value |
|---|---|
| Feature modules | **14** (auth, chat, tasks, projects, project, notifications, profile, connections, users, announcements, discover, search, settings, smart) |
| ViewModels | **19** |
| Room entities | **17** |
| Room DB version | **v12** (12-step migration chain) |
| Granular permissions | **28** across 5 categories |
| Role tiers | **3** (Admin / Manager / Member) |
| Unit tests | **112 passing** |
| Total test files | **22** (11 unit + 11 instrumented) |
| Auto-synced entity types | **13+** (exponential-backoff queue, 5 retries) |
| Realtime channel pools | **3** thread-safe `ConcurrentHashMap` pools |
| Realtime broadcast channels | **5+** (typing, presence, task-editing, postgres changes, project events) |
| WorkManager reminder tiers | **4** (1 week / 3 days / 1 day / 1 hour before due) |
| CI jobs | **3** (unit + Jacoco coverage + instrumented build on API 33 emulator) |
| Min / Target SDK | **26 / 36** |

---

## 4. Resume-bullet variants (pick one block, 4–6 lines)

### 4a. New-grad / SDE-1 — emphasizes breadth, ownership, fundamentals

- Designed and shipped a solo full-stack Android collaboration app (Kotlin, Jetpack Compose, Supabase) with MVVM + Repository architecture across **14 feature modules** and **19 ViewModels**.
- Modeled a normalized PostgreSQL / Room schema (**17 entities**, **12 migrations**) with foreign-key-aware migration strategy preventing data loss during `REPLACE` upserts.
- Wrote **112 unit tests** and instrumented Compose UI tests with JUnit, MockK, Robolectric, and Turbine; configured a **3-job GitHub Actions pipeline** (unit + Jacoco coverage + emulator build on API 33).
- Implemented Supabase Auth with OAuth + session persistence and a **28-permission RBAC** system gating UI via reusable composable wrappers.
- Used Kotlin Coroutines and Flow with structured concurrency (`SupervisorJob`, `Mutex`, `SharedFlow`) across realtime, sync, and ViewModel layers.

### 4b. Mid-level Android / SDE-2 — emphasizes hard engineering

- Built an offline-first sync engine (`SyncQueueManager`) with exponential-backoff retry, fresh-state reconciliation on `UPDATE` to avoid stale-version loops, and `NetworkMonitor`-triggered drain across **13+ entity types**.
- Designed a field-level `TaskConflictResolver` using timestamp-based last-write-wins across 9 task fields, auto-merging non-overlapping edits and surfacing a resolution dialog only on true conflict.
- Implemented multi-channel Supabase Realtime collaboration: **3 `ConcurrentHashMap`-backed channel pools** for messages, tasks, and project members; broadcasts for typing, presence, task-editing, and Postgres change streams.
- Authored **12 Room migrations** including a `CASCADE → NO_ACTION` refactor (v10) that eliminated destructive `REPLACE`-upsert side-effects, plus per-resource incremental-sync timestamps (v8) cutting payload size on rejoin.
- Built a **28-permission, 3-role RBAC** enforced both client-side (`PermissionGated` composable + `PermissionChecker`) and server-side via Supabase RLS policies and `SECURITY DEFINER` RPCs for cross-user notification inserts.

### 4c. Startup / founding engineer — emphasizes velocity, breadth, ownership

- Solo-shipped Kosmos, a production-grade offline-first Android team-collaboration app (Kotlin / Compose / Supabase) — owned schema design, RLS policies, sync engine, RBAC, CI, and **14 feature modules** end-to-end.
- Designed the realtime collaboration layer from scratch: typing and presence indicators, live task-editing broadcasts, and Postgres-change subscriptions over a thread-safe multi-pool WebSocket abstraction.
- Engineered offline-first sync with field-level conflict resolution and exponential-backoff queue draining — the hardest problem in mobile collaboration apps.
- Stood up the full backend on Supabase: **17-entity schema**, RLS policies, `SECURITY DEFINER` RPCs for cross-user writes, count-trigger repair migrations, and WorkManager-driven 4-tier reminders.
- Shipped with production discipline: **112 unit + instrumented Compose UI tests**, **3-job GitHub Actions CI** (unit + Jacoco coverage + emulator build), ProGuard/R8 release config, and structured concurrency throughout.

---

## 5. Skills-section keyword bank (paste into the resume "Skills" block)

- **Languages:** Kotlin, SQL, Java
- **Mobile / UI:** Android SDK (26–36), Jetpack Compose, Material 3, Navigation, Coil
- **Architecture:** MVVM, Repository Pattern, Offline-First, Clean Architecture, Dependency Injection (Hilt)
- **Async / Reactive:** Kotlin Coroutines, Flow, StateFlow, SharedFlow, Structured Concurrency
- **Backend / Data:** Supabase, PostgreSQL, Row-Level Security (RLS), Realtime (WebSockets), OAuth 2.0, REST, Room
- **Background / Reliability:** WorkManager, Exponential Backoff, Conflict Resolution, Optimistic Concurrency
- **Testing:** JUnit, MockK, Robolectric, Turbine, Compose UI Testing, Instrumented Tests
- **DevOps:** GitHub Actions, Jacoco, ProGuard/R8, Gradle Kotlin DSL

---

## 6. Cover-letter / LinkedIn flavor lines

- *"Architected and shipped a production-grade Android collaboration app end-to-end — offline-first sync, realtime co-editing, RBAC, 12-step Room schema evolution — solo."*
- *"Built the hard parts of a mobile collab app from scratch: conflict resolution, sync queue, realtime broadcast, RLS — across both the Android client and the Postgres backend."*
- *"Owned every layer of Kosmos: 14 feature modules, 17-entity Postgres schema, multi-channel realtime, and a 3-job CI pipeline."*

---

## 7. Project-link block (for a portfolio / GitHub README pin)

```
Kosmos — Offline-first Android team collaboration app
Kotlin · Jetpack Compose · Supabase · Postgres · Realtime · RBAC
• 14 feature modules · 17-entity Room schema (v12) · 28-permission RBAC
• Offline sync queue with field-level conflict resolution
• Multi-channel realtime: typing / presence / live editing
• 112 unit tests · 3-job GitHub Actions CI
```
