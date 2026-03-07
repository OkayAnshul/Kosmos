# Release Runbook

Status: Mixed (verified command path + pending operational completion)

## Target
Primary target is Play Store Internal Testing before wider rollout.

## Prerequisites
- Runtime keys configured locally.
- Release signing keys configured locally.
- Keystore file available at configured `RELEASE_STORE_FILE` path.

## Required Commands
```bash
./scripts/preflight_release.sh
./gradlew testDebugUnitTest
./gradlew lintRelease
./gradlew bundleRelease
./scripts/verify_bundle_signature.sh
```

## Pass Criteria
- Preflight reports all required keys as present.
- Unit tests and lint pass.
- AAB exists at `app/build/outputs/bundle/release/app-release.aab`.
- Signature verification reports success.

## Known Failure Modes
- Unsigned bundle due to missing `RELEASE_*` properties.
- Intermittent bundle task collisions from concurrent Gradle processes.

## GO / NO-GO Rule
No Play upload until all pass criteria are met and legal/store metadata are finalized.
