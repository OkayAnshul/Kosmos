# Testing and Quality

## Current Verification Baseline (2026-03-06)
- Unit tests: `./gradlew testDebugUnitTest` -> PASS
- Lint: `./gradlew lintRelease` -> PASS
- Bundle: `./gradlew bundleRelease` -> PASS
- Signature: `./scripts/verify_bundle_signature.sh` -> FAIL (unsigned)

## Test Inventory
- `app/src/test`: unit tests for viewmodels, validators, models, and utility logic
- `app/src/androidTest`: UI/integration tests including repository and screen flows

Approximate counts:
- Main Kotlin files: 262
- Unit test files: 11
- Android test files: 11

## Quality Gates
Mandatory for release candidate:
1. `./scripts/preflight_release.sh`
2. `./gradlew testDebugUnitTest`
3. `./gradlew lintRelease`
4. `./gradlew bundleRelease`
5. `./scripts/verify_bundle_signature.sh`

## Coverage and Risk Notes
- Test surface exists but is thin relative to codebase size.
- Highest risk areas:
  - sync conflict paths
  - realtime consistency under reconnect scenarios
  - settings/profile partial TODO paths

## Recommended Additions
### Priority A
- Auth -> Project -> Chat -> Task smoke instrumentation path
- Sync conflict deterministic tests
- Permission and role boundary tests for project/task actions

### Priority B
- Error-state and retry-path UI tests
- Data export/privacy settings behavior tests

## Release QA Checklist
- Fresh install flow
- Upgrade from previous app version
- Offline create/edit then reconnect sync
- Notification permission denied/granted transitions
- Background/foreground state persistence
