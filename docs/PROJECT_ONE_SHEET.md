# Kosmos Project One Sheet

Status model used in this document:
- **Verified**: directly supported by current repository code/config/scripts.
- **Planned**: approved next improvements not fully complete yet.

Date: 2026-03-07

## 1. Project At A Glance
Kosmos is an Android collaboration platform focused on project execution in mobile-first workflows. It combines project spaces, team chat, tasks, member management, and offline-first data behavior.

### Verified
- Platform: Android (Kotlin + Jetpack Compose)
- Core domains in product flow: auth, projects, chat, tasks, notifications, profile/settings
- App package for release distribution: `com.aravya.apps.kosmos`
- Repository architecture is layered around `core`, `data`, `features`, `shared`, and `navigation`

### Planned
- Expand top-level product narrative in repository media with guided screenshots/video walkthrough in a dedicated docs pass.

## 2. Product Context And Value
Kosmos addresses team coordination friction where communication and task execution are disconnected across tools.

### Verified
- Chat and task flows are both implemented and represented in current feature modules.
- Room + Supabase integration supports local-first behavior with remote convergence.
- Release gating scripts are present for production hygiene.

### Planned
- Stronger task dependency visibility and richer interaction models in subsequent phases.
- Voice and advanced attachment capabilities after stabilization track.

## 3. Technical Baseline (Build + Runtime)

### Verified
- Build config:
  - `compileSdk=36`
  - `targetSdk=36`
  - `minSdk=26`
  - `versionCode=1`, `versionName=1.0`
- IDs:
  - `applicationId=com.aravya.apps.kosmos`
  - `namespace=com.example.kosmos` (intentional transitional mismatch)
- Stack:
  - Kotlin, Jetpack Compose, Material3, Hilt
  - Room (local persistence)
  - Supabase (auth/data/realtime)
  - Coroutines + Flow
- Required release commands:
  - `./scripts/preflight_release.sh`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lintRelease`
  - `./gradlew bundleRelease`
  - `./scripts/verify_bundle_signature.sh`

### Planned
- Namespace convergence from `com.example.kosmos` to organization-aligned package path in a controlled post-stability refactor.

## 4. System Architecture

### Verified
- Presentation layer: Compose screens + ViewModels under feature packages.
- Data layer: repositories + data sources coordinating local and remote operations.
- Infrastructure layer: database entities/DAOs, sync utilities, realtime manager.
- Shared/UI layer: reusable design tokens, components, layout wrappers, and utility helpers.
- Typical flow:
  - UI action -> ViewModel -> Repository -> local DB write + remote sync -> Flow updates UI.

### Planned
- Additional decomposition of large classes/screens into smaller bounded units.
- Stronger consistency contracts in reconnect/retry flows.

## 5. Feature Maturity Matrix

### Verified Stable/Usable
- Authentication entry flow (including Google client integration path)
- Project list/workspace and member-oriented project operations
- Chat list/room messaging surfaces
- Task board, detail, and edit flows
- Notification and settings surfaces

### Verified Partial/Deferred Areas
- Realtime subscription TODO markers remain in select project/member data sources.
- Some retry or settings/profile interactions still include placeholders.
- Voice pipeline remains intentionally deferred.

### Planned
- Prioritized closure of high-impact TODOs in settings/profile/realtime areas before broader rollout.

## 6. Security And Secrets

### Verified
- Secrets are expected via local properties, not committed source.
- `.gitignore` excludes local properties and common key/keystore formats.
- Release preflight script checks presence of required runtime/signing properties.
- Signature verification script validates whether produced AAB is signed.

Required local keys:
- Runtime: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_CLOUD_API_KEY`
- Signing: `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`

### Planned
- Final legal metadata and policy URL verification for store rollout governance.
- Ongoing permission-surface audit against actual runtime feature usage.

## 7. Testing And Quality Reality

### Verified
- Source footprint:
  - Main Kotlin files: **262**
  - Unit test files: **11**
  - Android test files: **11**
- Current quality posture:
  - Mandatory gate command path is defined and script-supported.
  - Test surface exists but is proportionally smaller than feature/code surface.

### Planned
- Add deterministic end-to-end smoke path coverage:
  - auth -> project -> chat -> task critical path
- Expand conflict/retry and reconnect scenario tests.
- Strengthen role/permission boundary test cases.

## 8. Codebase Findings And Risks

### Verified Findings
1. The architecture is broad and production-oriented but concentrated in several very large files.
2. Realtime/retry TODO markers indicate remaining reliability work in specific paths.
3. Feature breadth is strong, while verification depth is the main area needing expansion.
4. Release readiness is process-driven and blocked primarily by signing/legal completion.

### Verified Risk Ranking
- **High**: signing completion and signed artifact verification before distribution.
- **Medium**: reconnect/realtime consistency and large-file regression risk.
- **Lower (important)**: remaining UX consistency cleanup in deferred settings/profile paths.

### Planned
- Sequence mitigation by release gating first, then reliability hardening, then modularity improvements.

## 9. Release Readiness Snapshot

### Verified
- Release pipeline scripts exist and are integrated into docs guidance.
- Store-target package ID is fixed to `com.aravya.apps.kosmos`.
- Internal-track-first release strategy is documented.

### Planned
- Complete local signing setup and verify signed bundle pass.
- Finalize Play listing legal links and metadata completeness.
- Execute controlled internal testing cycle before broader release tracks.

## 10. Decision Timeline Highlights

### Verified Decisions Already Embedded In Project Direction
- Organization-aligned application ID adopted.
- Transitional namespace kept to avoid risky pre-release refactor.
- Aggressive non-production cleanup moved historical material to dedicated archive.
- Signed artifact verification treated as a mandatory release gate.

### Planned Decision Checkpoints
- Namespace migration timing after stability baseline.
- Deferred feature activation order (voice/attachments/admin surfaces).
- Modularization boundaries for largest files based on defect and churn data.

## 11. Six-Week Execution Timeline

### Weeks 1-2 (Release Closure)
- Complete signing configuration.
- Produce verified signed AAB.
- Execute internal distribution readiness checklist.

### Weeks 3-4 (Reliability)
- Close highest-impact realtime/retry TODO paths.
- Add deterministic smoke and conflict scenario tests.

### Weeks 5-6 (Maintainability + UX)
- Refactor top hotspot files into bounded components.
- Complete UX consistency and accessibility-focused cleanup on high-traffic screens.

## 12. Repository Reading Guide
Use this one sheet first, then drill into focused references:
1. `docs/ARCHITECTURE.md`
2. `docs/CODEBASE_FINDINGS.md`
3. `docs/SECURITY.md`
4. `docs/TESTING.md`
5. `docs/RELEASE.md`

Historical context only:
- `docs/ARCHIVE_REFERENCES.md` -> `cleanup_nonprod_2026-03-06/*`

## 13. Current Bottom Line
Kosmos is a substantial production-oriented Android codebase with strong feature breadth and explicit release discipline. The remaining work is concentrated in release-signing finalization, targeted reliability hardening, and incremental modularity improvements rather than foundational architecture gaps.
