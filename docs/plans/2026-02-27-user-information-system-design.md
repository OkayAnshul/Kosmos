# User Information System — Design Document
**Date**: 2026-02-27
**Status**: Approved

## Problem

The Kosmos app has 20+ silent failure points where exceptions are swallowed (`catch (_: Exception) {}`), RBAC permission denials surface as generic "Failed to…" messages, and role-based UI restrictions are invisible to users. This causes confusion: users don't know why an action failed or what they're allowed to do.

## Solution: 3-Pillar Information System

### Pillar 1 — `UserFeedbackManager` (singleton)

A `@Singleton` that acts as the single bus for user-facing feedback events. ViewModels and repositories post events to it; `KosmosApp` collects them and routes to Snackbar/Dialog.

**Event types (sealed class `FeedbackEvent`):**
- `PermissionDenied(action: String, reason: String, requiredRole: String?)` → Red Snackbar, 8s, "Learn more" opens bottom sheet
- `SyncWarning(message: String)` → Yellow Snackbar, 3s (non-blocking)
- `Error(message: String, retryAction: (() -> Unit)?)` → Red Snackbar, 6s, optional Retry button
- `Success(message: String)` → Green Snackbar, 2s
- `Info(message: String)` → Blue Snackbar, 3s

Backed by `MutableSharedFlow<FeedbackEvent>(extraBufferCapacity = 16)`.

### Pillar 2 — `safeCall { }` ViewModel extension

Extension on `CoroutineScope` that wraps blocks and automatically routes known exceptions:
- `PermissionDeniedException` → `UserFeedbackManager.post(PermissionDenied(...))`
- `CancellationException` → rethrown (never swallowed)
- Generic `Exception` → `UserFeedbackManager.post(SyncWarning(...))` or `Error(...)`

Replaces all `catch (_: Exception) {}` anti-patterns throughout the codebase.

### Pillar 3 — `PermissionGated` composable

Wraps any UI element with a role/permission check:
- If user **has** permission → renders content normally
- If user **lacks** permission → dims element to 40% opacity, adds lock icon overlay, on tap shows bottom sheet:
  > "You don't have permission to [action]. Required: Manager or higher. Your role: Member."

**Signature:**
```kotlin
@Composable
fun PermissionGated(
    permission: Permission,
    currentMember: ProjectMember?,
    action: String,           // human-readable: "delete this task"
    content: @Composable () -> Unit
)
```

## Files to Create

| File | Purpose |
|------|---------|
| `core/feedback/FeedbackEvent.kt` | Sealed class for all event types |
| `core/feedback/UserFeedbackManager.kt` | Singleton, SharedFlow bus |
| `core/feedback/FeedbackExtensions.kt` | `safeCall {}` ViewModel extension |
| `shared/ui/components/PermissionGated.kt` | Composable wrapper for role-gated UI |
| `shared/ui/components/PermissionDeniedBottomSheet.kt` | Explains denied action to user |

## Files to Modify

| File | Change |
|------|--------|
| `Module.kt` | Register `UserFeedbackManager` as `@Singleton @Provides` |
| `MainActivity.kt` (`KosmosApp`) | Collect `UserFeedbackManager.events` flow, route to `globalSnackbarHostState` |
| All ViewModels (10+) | Inject `UserFeedbackManager`, replace `catch (_: Exception) {}` with `safeCall {}` |
| RBAC-enforcing screens | Wrap action buttons with `PermissionGated` |

## UI Behavior

| Event | Visual | Duration |
|-------|--------|----------|
| PermissionDenied | Red Snackbar + "Learn more" bottom sheet | 8s |
| SyncWarning | Yellow/amber Snackbar | 3s |
| Error (with retry) | Red Snackbar + Retry button | 6s |
| Success | Green Snackbar | 2s |
| Info | Blue/neutral Snackbar | 3s |

PermissionGated elements: 40% opacity + lock icon + bottom sheet on tap/long-press.

## Key Screens with Permission-Gated Actions

- **ProjectDetailsScreen** → Edit project (Manager+), Delete project (Admin only)
- **MembersListScreen** → Remove member (Admin+), Change role (Admin)
- **TaskDetailScreen** → Delete task (own task: Member+, any task: Manager+)
- **TaskBoardScreen** → Create task (Member+), Assign to others (Manager+)
- **ChatListScreen** → Archive chat room (Admin+)

## Reuse

- `Feedback.kt:showError()` / `showSuccess()` — kept for local screen-level feedback
- `Feedback.kt:InfoBanner` — for persistent banners (e.g., "Read-only: viewing as Guest")
- `PermissionChecker.PermissionDeniedException` — caught by `safeCall {}`
- `PermissionChecker.hasPermission()` — called inside `PermissionGated`

## Verification

1. Trigger a permission denial (Member tries to delete project) → should see red Snackbar + bottom sheet
2. Go offline → try to sync → should see yellow SyncWarning Snackbar
3. Member sees dimmed "Delete Project" button with lock icon; tap shows explanation
4. Admin sees fully enabled "Delete Project" button
5. `./gradlew compileDebugKotlin` passes with no errors
