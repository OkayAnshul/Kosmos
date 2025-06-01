# Kosmos

Kosmos is an Android collaboration app focused on project work: team chat, task workflows, project membership, and offline-first sync.

## Current Status (Evidence-Backed)
- `testDebugUnitTest`: passing
- `assembleRelease`: passing
- `bundleRelease`: passing (`app-release.aab` generated)
- `lintRelease`: passing
- Release signing: supported through local properties; not committed in git

See [docs/evidence/BASELINE_STATUS.md](docs/evidence/BASELINE_STATUS.md) for command outputs and artifact paths.

## Problem It Solves
- Teams need project chat + tasks in one mobile flow.
- Network instability breaks many collaboration apps.
- Kosmos emphasizes local-first state and background sync retry.

## Core Features
- Email and Google-based auth
- Project creation, membership, and role-based actions
- Chat rooms and project-scoped messaging
- Task creation/editing/status/assignment
- Notification listener and in-app notification surface
- Room + Supabase hybrid sync with retry queue

## Tech Stack
- Kotlin, Jetpack Compose, Material 3
- Hilt DI
- Room (local persistence + migrations)
- Supabase (Auth, PostgREST, Realtime, Storage)
- Coroutines + Flow

## Build & Verification
```bash
./scripts/preflight_release.sh
./gradlew testDebugUnitTest
./gradlew assembleRelease
./gradlew bundleRelease
./gradlew lintRelease
./gradlew jacocoTestReport
```

## Secrets & Signing
Do not commit runtime keys or keystores.

Set credentials locally in `local.properties` or `~/.gradle/gradle.properties`:
- `SUPABASE_URL`, `SUPABASE_ANON_KEY`
- `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_CLOUD_API_KEY`
- `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`

## Known Limitations
- Codebase still has large files that should be modularized further.
- Feature coverage is broad, but some lower-priority areas remain marked as future work.
- Coverage is currently low at total-code level due to large UI surface and generated artifacts.

## Documentation
- Production docs index: `docs/README.md`
- Legacy and development docs archive: `cleanup_nonprod_2026-03-06/`
