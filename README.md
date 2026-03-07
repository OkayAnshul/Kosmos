# Kosmos

Kosmos is an Android collaboration platform focused on project execution in mobile-first workflows. It combines team communication and task delivery into one product: projects, members, chat, tasks, notifications, and local-first sync behavior.

## Project Snapshot
- App package ID: `com.aravya.apps.kosmos`
- Kotlin namespace: `com.example.kosmos` (transitional technical debt)
- Build targets: `minSdk=26`, `targetSdk=36`, `compileSdk=36`
- Architecture: MVVM + Repository + Room + Supabase + Compose
- Current stage: production hardening for Play internal testing

## Why This Exists
Most teams split work between chat apps and task apps. Kosmos is built to reduce that context-switching by keeping project conversations and execution states in a single mobile-first flow.

## Core Product Capabilities
### Authentication and Access
- Email-based and Google-based sign-in paths
- Role-aware project actions through permission checks in app logic

### Projects and Members
- Project creation and workspace-centric project views
- Member lists, invite-related flows, and project-scoped collaboration surfaces

### Chat and Communication
- Chat list and room experiences
- Project-linked communication patterns for team coordination

### Tasks and Execution
- Task board and task detail/edit workflows
- Status lifecycle support and activity-oriented task operations

### Profile, Notifications, and Settings
- User profile/settings surfaces
- In-app notification handling and related UX paths

## Architecture Overview
Kosmos is organized in layered packages under `app/src/main/java/com/example/kosmos`:
- `core/`: models, database, DI, config, validators
- `data/`: datasources, repositories, sync and realtime infrastructure
- `features/`: feature UIs and viewmodels
- `shared/`: reusable UI system, layouts, utilities
- `navigation/`: route modeling

Primary runtime flow:
- UI action -> ViewModel -> Repository -> local write + remote sync -> Flow/state update back to UI.

## Offline-First and Sync Model
Kosmos prioritizes local responsiveness first and backend convergence second:
- Room persists local state for continuity
- Repository layer coordinates local/remote operations
- Sync/retry components handle eventual remote reconciliation

## Current Reality (Verified)
- Codebase is broad and feature-rich across core collaboration domains.
- Release gate scripts are present and integrated into docs/release process.
- Several high-value areas remain partially deferred (realtime depth, some settings/profile TODO paths, voice pipeline).

## Quality and Testing
Current repository footprint:
- Main Kotlin files: **262**
- Unit test files: **11**
- Android test files: **11**

Release gate commands:
```bash
./scripts/preflight_release.sh
./gradlew testDebugUnitTest
./gradlew lintRelease
./gradlew bundleRelease
./scripts/verify_bundle_signature.sh
```

Quality focus areas:
- Strengthen deterministic end-to-end smoke path coverage
- Expand reconnect/conflict/retry scenario verification
- Increase boundary tests for role/permission-sensitive operations

## Security and Secret Handling
Do not commit runtime secrets or signing material.

Use local-only properties for:
- Runtime: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_CLOUD_API_KEY`
- Signing: `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`

Current controls include:
- `.gitignore` exclusions for local config and keystore formats
- preflight property checks
- signed bundle verification script

## Release Readiness
Distribution strategy is internal-track-first before broader rollout.

Current primary blockers are operational, not architectural:
- complete release signing configuration
- verify signed AAB pass end-to-end
- finalize legal/store metadata completeness

## Risks and Mitigation Priorities
High priority:
- release signing completion and signed artifact verification

Medium priority:
- realtime behavior consistency under reconnect/network churn
- large-file maintainability and regression risk in hotspot modules

Lower (important):
- deferred UX polish in selected settings/profile and retry paths

## 6-Week Improvement Timeline
Weeks 1-2:
- close signing and release gate loop
- run internal distribution readiness cycle

Weeks 3-4:
- resolve highest-impact realtime/retry TODO paths
- add deterministic critical-flow smoke tests

Weeks 5-6:
- refactor hotspot large files into bounded units
- execute consistency/accessibility sweep on high-traffic screens

## Documentation Map
Start here for complete context in one read:
1. [Project One Sheet](docs/PROJECT_ONE_SHEET.md)

Then drill down:
1. [Architecture](docs/ARCHITECTURE.md)
2. [Codebase Findings](docs/CODEBASE_FINDINGS.md)
3. [Security Model](docs/SECURITY.md)
4. [Testing and Quality](docs/TESTING.md)
5. [Release Runbook](docs/RELEASE.md)
6. [Production Docs Index](docs/README.md)

Historical/non-production archive map:
- [Archive References](docs/ARCHIVE_REFERENCES.md)

## Demo Media
Screenshots and video walkthrough are intentionally deferred and will be added in a dedicated docs pass.
