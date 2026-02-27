# Legacy UI Screens - Pre-React Design (Archived 2026-01-11)

This directory contains the old UI screens that were replaced with React-design equivalents.

## Purpose
These files are archived for reference and can be deleted during cleanup. The new React design screens are now wired to the backend and actively used in the app.

## Archived Screens

### Projects
- **ProjectListScreen.kt** - Old project list screen (replaced by ProjectListScreenReact.kt)
- **ProjectListScreenWrapper.kt** - Old wrapper (replaced by ProjectListScreenReactWrapper.kt)

## Migration Status

| Old Screen | New Screen | Status | Notes |
|------------|------------|--------|-------|
| ProjectListScreen.kt | ProjectListScreenReact.kt | ✅ Wired & Active | Backend fully connected via ProjectListScreenReactWrapper.kt |

## Safe to Delete?

**Yes**, these files can be safely deleted after verifying the new React screens work correctly for a few weeks. They are no longer referenced in the codebase.

## How to Restore (if needed)

If you need to restore any of these files:
```bash
cp archive/legacy_ui_pre_react_2026-01-11/projects/ProjectListScreen.kt app/src/main/java/com/example/kosmos/features/projects/presentation/redesign/
```

Then update MainActivity.kt to use the old wrapper instead of ProjectListScreenReactWrapper.
