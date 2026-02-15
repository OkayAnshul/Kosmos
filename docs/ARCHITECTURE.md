# Architecture

## Executive Summary
Kosmos follows a pragmatic MVVM + Repository architecture with Room local persistence, Supabase backend integration, and Compose-based UI. The architecture is broad and production-oriented, with implementation debt concentrated in selected deferred features.

## Tech Stack
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- DI: Hilt
- Local DB: Room
- Remote: Supabase (Auth, PostgREST, Realtime, Storage)
- Networking: Ktor + Retrofit/OkHttp
- Background: WorkManager
- Async: Coroutines + Flow

## App Structure
Top-level code modules under `app/src/main/java/com/example/kosmos`:
- `core/` (config, models, db, sync, validators)
- `data/` (datasources, repositories, realtime, sync)
- `features/` (auth/chat/tasks/projects/profile/settings/etc.)
- `shared/` (design system, UI utilities, helpers)
- `navigation/` (route definitions)

## Layering Contract
1. UI layer (`features/*/presentation`)
- Compose screens, wrappers, and view state handling.
- ViewModels own screen state and user actions.

2. Domain-ish orchestration (ViewModels + validators + mappers)
- Permission checks and transformation logic.
- Feature orchestration and command flow.

3. Data layer (`data/repository` + `data/datasource`)
- Repository as source of truth façade.
- Room for local read/write.
- Supabase datasource for remote sync.

4. Infrastructure (`core/*`, `data/sync`, `data/realtime`)
- Database schema/migrations
- Queueing/sync support
- Realtime listener management

## Data Flow
### Online happy path
UI action -> ViewModel -> Repository -> Local DB write + Remote call -> Flow updates UI.

### Offline-first behavior
UI action -> ViewModel -> Repository -> Local DB write -> queue/sync retry pattern -> eventual remote convergence.

## Auth and Deep Link
- OAuth callback handled through `kosmos://auth-callback` intent filter.
- Supabase deeplink handling is routed in `MainActivity` `onCreate` and `onNewIntent`.

## Build and Release Architecture
- `applicationId`: `com.aravya.apps.kosmos`
- `namespace`: `com.example.kosmos` (kept for current code package stability)
- `minSdk=26`, `targetSdk=36`, `compileSdk=36`
- Release build: minify + shrink resources enabled
- Release signing is conditional on local `RELEASE_*` properties

## Known Architecture Debt
- 100+ TODO/FUTURE markers remain in code comments
- Partial feature stubs in settings/profile/task-detail adjacent flows
- Voice feature modules intentionally disabled
- Namespace and applicationId mismatch (acceptable short term, should converge later)

## Architectural Priorities
1. Signing + release reproducibility
2. Resolve high-impact TODOs in settings/profile/task flow
3. Tighten offline conflict and realtime consistency guarantees
4. Modularize large UI files into smaller composables
