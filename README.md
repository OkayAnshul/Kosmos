# Kosmos

Kosmos is an Android collaboration platform built for project execution in mobile-first teams. It combines project workspaces, task execution, team chat, member operations, and offline-first sync in one product.

Status model used in this README:
- **Verified**: directly supported by current repository code/config/scripts.
- **Planned**: approved improvement direction not fully completed yet.

Date: 2026-03-07

## 1. Project Overview
Kosmos solves a common delivery gap: teams discuss work in one tool and execute tasks in another, causing context switching and execution drift. The app is designed to keep communication and execution linked at the project level.

### Verified
- Android app with collaboration-first scope (projects, members, chat, tasks, notifications, settings).
- Release distribution identifier: `com.aravya.apps.kosmos`.
- Implementation organized into production-oriented layers (`core`, `data`, `features`, `shared`, `navigation`).

### Planned
- Add richer repository media walkthroughs (screenshots and short product demo) in a dedicated docs/media pass.

## 2. Unique Product Value (USP)
### Verified
- **Execution + communication in one flow**: task and chat interactions are project-linked.
- **Offline-first posture**: local persistence and sync patterns prioritize continuity in unstable networks.
- **Release discipline**: explicit preflight/build/lint/signature gate commands are integrated into project workflow.

### Planned
- Deepen cross-feature visibility (dependency-aware task context and richer collaboration telemetry).

## 3. Android Engineering Baseline

### Verified
- Build/runtime baseline:
  - `compileSdk=36`
  - `targetSdk=36`
  - `minSdk=26`
  - `versionCode=1`
  - `versionName=1.0`
- App identifiers:
  - `applicationId=com.aravya.apps.kosmos`
  - `namespace=com.example.kosmos` (intentional transitional technical debt)
- Core stack:
  - Kotlin, Jetpack Compose, Material 3
  - Hilt dependency injection
  - Room local database
  - Supabase backend integration
  - Coroutines + Flow

### Planned
- Controlled namespace convergence after stability baseline, avoiding risky pre-release mass refactor.

## 4. Architecture And Data Flow

### Verified
- **Presentation layer**: Compose screens and ViewModels in feature packages.
- **Data layer**: repositories and data sources orchestrate local and remote operations.
- **Infrastructure layer**: database models/DAOs, sync queue/retry helpers, realtime management.
- **Shared layer**: reusable UI tokens/components/layout abstractions and utility services.

Primary runtime flow:
- UI action -> ViewModel -> Repository -> local write + remote sync -> Flow/state updates UI.

Offline-first behavior pattern:
- local state continuity first, eventual backend convergence second.

### Planned
- Additional decomposition of large hotspots (repositories/screens) into bounded modules with clearer ownership.
- Tighten reconnect/retry consistency guarantees in high-churn realtime paths.

## 5. UI/UX Design System And Experience

### Design Direction
Kosmos uses a productivity-centered interface model focused on readability, speed, and low-friction task execution.

### Verified
- Compose + Material 3 implementation with shared UI primitives.
- Reusable design system units in shared UI layers (tokens/components/layout wrappers).
- Feature-level screen wrappers to isolate presentation wiring from reusable UI primitives.

### Interaction Model
- Section-oriented navigation with project-centric screen transitions.
- Action surfaces optimized for high-frequency operations (task status changes, project interactions, messaging).
- Feedback patterns based on explicit UI state transitions and user-visible action confirmations.

### Core UX Flows (high-level)
1. Authentication -> workspace access
2. Project selection/creation -> member/project operations
3. Chat room interaction -> contextual team communication
4. Task board/detail/edit -> execution lifecycle
5. Settings/profile/notifications -> preferences and account-level operations

### Planned
- Broader visual consistency pass for deferred settings/profile areas.
- Follow-up media and UI walkthrough documentation with real app captures.

## 6. Feature Maturity Matrix

### Verified Stable/Usable
- Authentication entry and sign-in flow integration
- Project list/workspace and member-oriented flows
- Chat list and room-level messaging surfaces
- Task board/detail/edit execution paths
- Notification and settings surfaces

### Verified Partial/Deferred
- Select realtime subscription TODO paths remain in project/member areas.
- Some settings/profile and retry UX interactions are still placeholders.
- Voice-related pipeline remains intentionally deferred.

### Planned
- Prioritized closure of high-impact realtime/retry/settings gaps before broader rollout stages.

## 7. Security, Secrets, And Operational Safety

### Verified
- Secret material expected through local properties (not committed source files).
- `.gitignore` excludes local properties and key/keystore artifacts.
- Preflight script validates required runtime/signing property presence.
- Bundle signature verification script checks whether output AAB is signed.

Required local keys:
- Runtime: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_CLOUD_API_KEY`
- Signing: `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`

### Planned
- Final legal metadata verification for store rollout.
- Continued permission-surface audit against active runtime feature usage.

## 8. Testing And Quality Posture

### Verified
- Source footprint (current):
  - Main Kotlin files: **262**
  - Unit test files: **11**
  - Android test files: **11**
- Mandatory release gate command path exists and is scripted.

Release commands:
```bash
./scripts/preflight_release.sh
./gradlew testDebugUnitTest
./gradlew lintRelease
./gradlew bundleRelease
./scripts/verify_bundle_signature.sh
```

### Planned
- Expand deterministic critical-path smoke coverage (auth -> project -> chat -> task).
- Add stronger reconnect/conflict/retry scenario validation.
- Increase role/permission boundary checks for project/task-sensitive operations.

## 9. Production Readiness Snapshot

### Verified
- Internal-track-first release strategy is documented and aligned with current approach.
- Release process has explicit gates for configuration, build quality, artifact generation, and signature checks.

### Planned
- Complete local signing configuration and verify signed bundle pass.
- Finalize legal/store metadata completeness prior to broader rollout.

## 10. Risks And Mitigation Priorities

### Verified risk ranking
- **High**: signing completion and signed artifact verification before distribution.
- **Medium**: reconnect/realtime consistency and hotspot large-file regression risk.
- **Lower (important)**: UX consistency debt in selected deferred settings/profile paths.

### Planned mitigation sequence
1. Close release-signing operational gap
2. Harden reliability-critical realtime/retry paths
3. Modularize hotspot files to improve maintainability and review safety

## 11. Six-Week Execution Timeline

### Weeks 1-2 (Release closure)
- Finalize signing setup
- Produce and verify signed AAB
- Complete internal distribution readiness checklist

### Weeks 3-4 (Reliability hardening)
- Resolve highest-impact realtime/retry TODO paths
- Add deterministic conflict/reconnect test scenarios

### Weeks 5-6 (Maintainability + UX)
- Refactor top hotspot files into bounded components
- Execute UX consistency and accessibility sweep on high-traffic screens

## 12. Documentation Map
For full supporting references:
1. [Project One Sheet](docs/PROJECT_ONE_SHEET.md)
2. [Architecture](docs/ARCHITECTURE.md)
3. [Codebase Findings](docs/CODEBASE_FINDINGS.md)
4. [UI/UX Design](docs/UI_UX_DESIGN.md)
5. [Security](docs/SECURITY.md)
6. [Testing](docs/TESTING.md)
7. [Release Runbook](docs/RELEASE.md)
8. [Production Docs Index](docs/README.md)

Historical/non-production material map:
- [Archive References](docs/ARCHIVE_REFERENCES.md)

## 13. Current Bottom Line
Kosmos is a substantial Android collaboration codebase with strong feature breadth, production-conscious architecture, and explicit release governance. Remaining work is concentrated in release-signing completion, selected reliability hardening, and incremental maintainability improvements rather than foundational architecture gaps.
