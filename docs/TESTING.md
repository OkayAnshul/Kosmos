# Testing and Quality

Status: Mixed (verified gates + planned depth expansion)

## Verified Baseline
- Unit tests and instrumentation suites exist in repo.
- Release build/test commands are scripted.
- Signature verification script is present.

## Current Test Surface
- Unit test files: **11** (`app/src/test`)
- Android test files: **11** (`app/src/androidTest`)
- Main Kotlin files: **262** (`app/src/main/java`)

## Mandatory Release Gates
```bash
./scripts/preflight_release.sh
./gradlew testDebugUnitTest
./gradlew lintRelease
./gradlew bundleRelease
./scripts/verify_bundle_signature.sh
```

## Quality Risks
- Test depth is currently thin relative to codebase size.
- High-risk paths: sync conflict handling, reconnect/realtime consistency, large task/project flows.

## Next Test Additions
1. End-to-end instrumentation smoke flow: auth -> project -> chat -> task.
2. Deterministic sync conflict and retry-path tests.
3. Permission/role boundary tests for project/task actions.
