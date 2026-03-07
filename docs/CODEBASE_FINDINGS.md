# Codebase Findings (Evidence Snapshot)

Date: 2026-03-07

## Scope and Method
This report summarizes current repository facts from code/config inspection.
It is intentionally evidence-first and avoids speculative claims.

## Project Reality Snapshot
- Main Kotlin source files: **262**
- Unit test files: **11**
- Android instrumentation test files: **11**
- Application ID: `com.aravya.apps.kosmos`
- Namespace: `com.example.kosmos`

## Architecture Shape (Observed)
Primary directories under `app/src/main/java/com/example/kosmos`:
- `core/` for models, DB, config, validators, DI
- `data/` for datasources, repositories, realtime, sync
- `features/` for UI and feature-specific viewmodels
- `shared/` for reusable UI and utilities
- `navigation/` for route definitions

## Maintainability Hotspots
Largest files by line count include:
- `data/repository/TaskRepository.kt` (~1589)
- `features/tasks/presentation/redesign/TaskEditScreen.kt` (~1244)
- `data/realtime/SupabaseRealtimeManager.kt` (~1230)
- `MainActivity.kt` (~1124)
- `features/tasks/presentation/redesign/MyTasksScreenReact.kt` (~1128)

Implication:
- Core behavior is implemented, but multiple files exceed practical review/refactor size.

## Concrete Findings
1. Deferred or partial behavior markers exist in production code paths.
2. Realtime subscription TODOs remain in project/member data sources.
3. Some settings/profile and retry UX paths still have placeholders.
4. Architecture is functionally broad, but file-size concentration increases regression risk.
5. Test surface exists but is light relative to source area.

## Production Risk Ranking
### High
- Release signing completeness and signed artifact verification before external rollout.

### Medium
- Realtime consistency under reconnect/network churn.
- Large-file change risk in task/project/chat areas.

### Lower (but important)
- UX consistency gaps in deferred settings/profile interactions.

## Recommended 6-Week Improvement Timeline
### Weeks 1-2
- Close release signing and artifact verification loop.
- Add critical smoke path instrumentation for auth -> project -> chat -> task flow.

### Weeks 3-4
- Refactor top 3-5 largest files into bounded components/use-cases.
- Address highest-impact TODOs in realtime subscription and retry behavior.

### Weeks 5-6
- Expand edge-case tests for offline/online convergence.
- Run UX consistency and accessibility sweep on top-level screens.
