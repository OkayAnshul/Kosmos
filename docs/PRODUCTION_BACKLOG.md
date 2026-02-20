# Production Backlog

## Purpose
Single operational backlog synthesized from archived planning/audit docs and current code-state checks.

## P0 (Must complete before Play internal upload)
1. Configure release signing properties (`RELEASE_*`)
2. Produce signed AAB and pass signature verification
3. Confirm privacy policy and terms URLs are live and final
4. Confirm final store metadata baseline for internal track

## P1 (High priority in first 1-2 sprints)
1. Close high-impact TODOs in settings/profile/task flows
2. Improve unresolved realtime subscription gaps in project/member datasources
3. Remove or complete placeholder retry/time-picker interactions
4. Stabilize sync conflict and retry behavior with stronger test cases

## P2 (Planned hardening)
1. Namespace convergence from `com.example.kosmos` to organization-aligned package path
2. UI refactor of very large screen files into smaller composables
3. Accessibility and visual consistency sweep
4. Dependency/version uplift with regression checks

## Deferred Feature Track (Post-v1)
- F1 Voice recording and transcription pipeline
- F2 Advanced task board interactions
- F3 Attachment module
- F4 Blocked users full data model and UX
- F5 Admin management surfaces

## Acceptance Metrics
- All release gates pass including signed artifact check
- No P0 regressions in internal-track smoke suite
- Zero known crash blockers in critical path
