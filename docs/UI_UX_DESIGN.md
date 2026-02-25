# UI and UX Design System

## Design Direction
Kosmos uses a productivity-first visual language optimized for high-frequency team workflows.

Core goals:
- fast scanning,
- low interaction friction,
- strong status visibility,
- consistent component behavior.

## UI Stack
- Jetpack Compose + Material 3
- Shared tokens in `shared/ui/designsystem`
- Reusable wrappers and components in `shared/ui/components` and `shared/ui/layouts`

## Design System Building Blocks
- Color tokens: semantic color mapping for status and emphasis
- Typography tokens: consistent hierarchy for headings/body/meta labels
- Icon set abstraction: centralized icon references
- Component primitives: cards, badges, dialogs, skeletons, status chips

## Interaction Patterns
- Bottom navigation for major app sections
- Dedicated wrappers for feature-specific state and routing
- Snackbar-based global feedback path
- Permission-gated actions for role-based UX safety

## Status Semantics
Task lifecycle states and visual semantics are standardized around:
- TODO
- IN_PROGRESS
- DONE
- CANCELLED

## Current UX Debt and Fix Targets
- Some settings panels still contain placeholder interactions
- Time picker and minor retry UX are partially stubbed in places
- Mixed legacy/new screen styles remain in parts of the codebase

## Accessibility and Usability Baseline
Production checklist for UI before broader rollout:
- minimum touch target compliance
- contrast review for badges and status chips
- keyboard/navigation behavior in forms
- loading/error/empty states on every top-level screen
- deterministic back navigation behavior

## UX Quality Bar for Release
No screen should ship if it lacks:
- loading state,
- recoverable error state,
- explicit empty state,
- clear primary action.
