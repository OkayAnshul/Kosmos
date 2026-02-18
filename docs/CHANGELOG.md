# Changelog

## 2026-03-06
### Documentation and Release Hardening
- Set production package ID to `com.aravya.apps.kosmos`.
- Removed production routing to internal test-only screens.
- Added signed bundle verification script (`scripts/verify_bundle_signature.sh`).
- Added/updated release gating docs and GO/NO-GO tracking.
- Performed aggressive repository cleanup into `cleanup_nonprod_2026-03-06`.
- Rebuilt production documentation set from archived sources and current code state.

### Current Open Blockers
- Missing release signing properties (`RELEASE_*`).
- AAB remains unsigned until signing is configured.
