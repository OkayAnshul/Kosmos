# Kosmos

Kosmos is an Android collaboration app for project teams: chat, tasks, membership, and offline-first sync.

## Why This Project Matters
- Combines project communication and execution into one mobile flow.
- Prioritizes local-first behavior with eventual backend convergence.
- Built as a production-oriented codebase with explicit release gates.

## Current Verified Snapshot (March 7, 2026)
- Android app package ID: `com.aravya.apps.kosmos`
- Kotlin namespace in source: `com.example.kosmos` (intentional transitional debt)
- Build config: `minSdk=26`, `targetSdk=36`, `compileSdk=36`
- Core stack: Kotlin, Compose, Hilt, Room, Supabase, Coroutines/Flow
- Release blockers still open: signed release configuration + final Play Console/legal metadata checks

## Read First
1. [Project One Sheet](docs/PROJECT_ONE_SHEET.md)
2. [Architecture](docs/ARCHITECTURE.md)
3. [Codebase Findings](docs/CODEBASE_FINDINGS.md)
4. [Security Model](docs/SECURITY.md)
5. [Testing and Quality](docs/TESTING.md)
6. [Release Runbook](docs/RELEASE.md)

## Planned Improvements (Post Internal Track)
- Close deferred realtime and settings/profile TODO paths.
- Reduce oversized files and improve modularity in high-churn areas.
- Expand end-to-end instrumentation and conflict/retry test coverage.

## Build and Release Commands
```bash
./scripts/preflight_release.sh
./gradlew testDebugUnitTest
./gradlew lintRelease
./gradlew bundleRelease
./scripts/verify_bundle_signature.sh
```

## Secrets and Safety
Do not commit runtime secrets or signing materials.
Use local-only properties for:
- `SUPABASE_URL`, `SUPABASE_ANON_KEY`
- `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_CLOUD_API_KEY`
- `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`

## Documentation
- Production docs index: [docs/README.md](docs/README.md)
- Historical/non-production archive: [docs/ARCHIVE_REFERENCES.md](docs/ARCHIVE_REFERENCES.md)

## Demo Media
Screenshots and video walkthrough are intentionally deferred and will be added in a dedicated docs pass.
