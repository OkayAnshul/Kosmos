# Decisions Log

This file records production-relevant decisions derived from current implementation and cleanup planning.

## D-001 Package ID for Play
- Date: 2026-03-06
- Decision: Use `com.aravya.apps.kosmos` as `applicationId`.
- Rationale: Organization-aligned stable ID for Play publishing.
- Status: Implemented.

## D-002 Preserve Kotlin namespace temporarily
- Date: 2026-03-06
- Decision: Keep `namespace` and code package paths as `com.example.kosmos` for now.
- Rationale: Avoid risky mass refactor during release-hardening phase.
- Status: Active technical debt; revisit after first stable release.

## D-003 Aggressive documentation cleanup
- Date: 2026-03-06
- Decision: Move historical development docs/plans/media/logs/sql into `cleanup_nonprod_2026-03-06`.
- Rationale: Reduce repo clutter and create a clean production docs baseline.
- Status: Implemented.

## D-004 Keep automated tests in repository
- Date: 2026-03-06
- Decision: Preserve `app/src/test` and `app/src/androidTest`.
- Rationale: Required for release confidence and quality gates.
- Status: Implemented.

## D-005 Remove internal test route exposure from production source flow
- Date: 2026-03-06
- Decision: Remove production route wiring to internal test launcher screens.
- Rationale: Prevent accidental production exposure of diagnostic surfaces.
- Status: Implemented.

## D-006 Add signed bundle verification gate
- Date: 2026-03-06
- Decision: Add `scripts/verify_bundle_signature.sh` as a mandatory release gate.
- Rationale: `bundleRelease` can pass while artifact remains unsigned when signing props are absent.
- Status: Implemented.

## D-007 Internal testing track before production rollout
- Date: 2026-03-06
- Decision: Prioritize Play internal testing path first.
- Rationale: Validate release operations and user-critical flows before broader rollout.
- Status: Active.

## D-008 Deferred Feature Set
- Date: inherited from prior plans, confirmed 2026-03-06
- Decision: Keep these as post-internal-track roadmap items:
  - F1 Voice recording + storage + playback pipeline
  - F2 Advanced task board drag/drop improvements
  - F3 Attachment module
  - F4 Blocked users complete data model
  - F5 Admin management UI
- Status: Deferred.

## Decision Governance
Any new release-impacting decision must include:
- problem statement,
- alternatives considered,
- risk assessment,
- rollback strategy,
- owner and review date.
