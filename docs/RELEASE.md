# Release Runbook

## Release Target
Primary target: Play Store Internal Testing track.

## Prerequisites
- Local runtime keys configured
- Local signing keys configured
- Keystore available at `RELEASE_STORE_FILE`

## Required Commands
```bash
./scripts/preflight_release.sh
./gradlew testDebugUnitTest
./gradlew lintRelease
./gradlew bundleRelease
./scripts/verify_bundle_signature.sh
```

## Expected Outcomes
- Preflight: all `[OK]`
- Tests: pass
- Lint: pass
- Bundle exists: `app/build/outputs/bundle/release/app-release.aab`
- Signature verification: pass (`[OK] Release bundle signature verified`)

## Known Failure Modes
### Unsigned bundle despite bundle success
- Symptom: `verify_bundle_signature.sh` reports unsigned
- Cause: missing `RELEASE_*` values
- Fix: add signing values and rerun bundle

### Bundle packaging race / file already exists
- Symptom: `FileAlreadyExistsException` in intermediary bundle output
- Cause: concurrent Gradle sessions
- Fix: rerun `bundleRelease` alone after other Gradle processes finish

## Artifact Handling
- Keep generated AAB as release candidate artifact.
- Do not modify/re-sign manually outside tracked process.

## Go / No-Go Criteria
GO only when all are true:
- signed AAB verified
- internal smoke suite passes
- privacy/terms URLs verified
- metadata assets complete for target track
