# Roadmap and Improvement Timeline

## Timeline Model
This roadmap is structured for practical execution from current state to post-launch hardening.

## Phase 0: Release Blocker Closure (1-3 days)
- Configure local signing (`RELEASE_*` properties)
- Generate signed AAB
- Pass signature verification gate
- Confirm internal test upload readiness

Exit criteria:
- `preflight_release.sh` all OK
- `bundleRelease` pass
- `verify_bundle_signature.sh` pass

## Phase 1: Internal Testing Launch (Week 1)
- Upload signed AAB to Play internal track
- Validate install and critical flow smoke tests:
  - auth
  - project load
  - chat room send/receive
  - task create/edit/status change
- Capture defects and triage by severity

Exit criteria:
- Internal track published
- No P0 crash/ANR blockers in smoke path

## Phase 2: Stability and Trust (Weeks 2-3)
- Resolve high-impact TODOs in settings/profile/task flows
- Improve realtime/subscription handling where still marked deferred
- Tighten error surfaces and retry actions
- Reduce key lint warnings related to locale and formatting correctness

Exit criteria:
- Zero release-blocking regressions from internal feedback
- Stable daily build with repeatable release gates

## Phase 3: Architecture and UX Hardening (Weeks 4-6)
- Split oversized composables/viewmodels for maintainability
- Refine state contracts and mapper consistency
- Improve UI consistency across redesigned wrappers and base screens
- Add stronger accessibility and visual QA pass

Exit criteria:
- Reduced UI/logic coupling hotspots
- Improved maintainability metrics and review speed

## Phase 4: Deferred Feature Track (Post v1)
### F1 Voice Pipeline
- MediaRecorder to storage upload
- playback and lifecycle-safe resource handling

### F2 Task Board Interactions
- richer drag/drop and status transition UX

### F3 Attachments
- upload/download/preview lifecycle with permission handling

### F4 Blocked Users
- schema + DAO + repository + UI state integration

### F5 Admin Management
- role-aware admin surfaces and operational controls

## Release Cadence Recommendation
- Patch cadence: weekly during internal/beta
- Minor release cadence: bi-weekly after stabilization
- Hard rule: no release without signed-bundle verification and smoke checklist pass
