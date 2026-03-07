# Architecture

Status: Mixed (verified structure + planned hardening)

## Executive Summary
Kosmos uses MVVM + Repository architecture with Room local storage, Supabase backend services, and Compose UI.

## Verified Build and App Configuration
- `applicationId`: `com.aravya.apps.kosmos`
- `namespace`: `com.example.kosmos`
- `minSdk=26`, `targetSdk=36`, `compileSdk=36`

## Layering Model
1. Presentation (`features/*/presentation`): Compose screens + ViewModels
2. Data (`data/repository`, `data/datasource`): local + remote orchestration
3. Infrastructure (`core/*`, `data/realtime`, `data/sync`): DB, realtime, sync mechanics
4. Shared (`shared/*`): design system and reusable UI/utilities

## Data Flow
- Online: UI -> ViewModel -> Repository -> Room + remote -> Flow to UI
- Offline-first: UI -> local write -> queue/retry -> eventual remote convergence

## Verified Strengths
- Clear layered package structure.
- Broad feature coverage across auth/projects/chat/tasks/profile/settings.
- Reusable shared UI system and wrappers across screens.

## Active Debt
- Very large files in repositories/screens increase review and regression cost.
- Namespace/applicationId mismatch is intentionally deferred.
- Some realtime and settings/profile TODO paths remain.

## Hardening Priorities
1. Keep release gate reproducibility (preflight, lint/tests, signed bundle verify).
2. Refactor top hotspot files into smaller bounded units.
3. Close high-impact TODOs in realtime/retry/settings paths.
