# Product and Scope

## Product Vision
Kosmos is a mobile-first collaboration app for project teams combining:
- project management,
- real-time chat,
- task execution,
- offline-first synchronization.

## Primary User Flows
1. Authentication (email + Google OAuth)
2. Project creation and member management
3. Project chat and messaging
4. Task lifecycle (create, assign, update status, track activity)
5. Profile/settings and notifications

## Current Feature Status
### Stable for production hardening
- Auth flows (email + Google callback)
- Project list/workspace patterns
- Task board/detail/edit flows
- Chat hub + room workflows
- Member/invite/connectivity surfaces
- Notification list + in-app notification handling

### Intentionally deferred or partial
- Voice recording upload/transcription pipeline (feature-flagged off)
- Blocked users full backend flow
- Some advanced task dependency display wiring
- Fully implemented preferences/time picker UX in settings

## Explicit Out-of-Scope for first Play release
- Voice AI/transcription expansion
- Attachment module end-to-end
- Admin management UI
- Deep analytics/telemetry pipeline

## Production Objective
First target is Play Store Internal Testing with signed AAB and reproducible quality gates.
