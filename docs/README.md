# Kosmos Production Documentation

This is the production-focused documentation set for Kosmos.

## Current Production Readiness (as of 2026-03-06)
- Package ID: `com.aravya.apps.kosmos`
- Build gates:
  - `testDebugUnitTest` -> PASS
  - `lintRelease` -> PASS
  - `bundleRelease` -> PASS
  - `verify_bundle_signature.sh` -> FAIL (unsigned AAB)
- Remaining release blockers:
  - Missing signing properties (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`)
  - Unsigned release bundle

## Docs Index
- [Product and Scope](./PRODUCT.md)
- [Architecture](./ARCHITECTURE.md)
- [Decisions Log](./DECISIONS.md)
- [Roadmap and Timeline](./ROADMAP.md)
- [Production Backlog](./PRODUCTION_BACKLOG.md)
- [UI/UX Design System](./UI_UX_DESIGN.md)
- [Security Model](./SECURITY.md)
- [Testing and Quality](./TESTING.md)
- [Release Runbook](./RELEASE.md)
- [Play Store Submission](./release/PLAY_STORE.md)
- [Archive Reference Map](./ARCHIVE_REFERENCES.md)

## Source Archive
Legacy and development-era documents were moved to:
`cleanup_nonprod_2026-03-06/`

This production set is curated from that archive plus current code/build state.
