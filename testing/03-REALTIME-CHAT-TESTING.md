# Real-time Chat Testing

**Date**: 2026-02-15
**Status**: AUDITED - 2 critical, 5 medium, 2 low bugs found

## Critical Bugs

### BUG-001: Typing Broadcast Job Leak
- **File**: `SupabaseRealtimeManager.kt:207-214`
- **Issue**: Typing broadcast flow job launched but never stored/cancelled
- **Impact**: Memory leak, duplicate typing events accumulate after navigating between chat rooms
- **Fix**: Store typing broadcast job, cancel on unsubscribe

### BUG-002: Incomplete Typing Unsubscription
- **File**: `ChatRepository.kt:735-746`
- **Issue**: `stopRealtimeSubscription()` only unsubscribes from messages, NOT typing indicators
- **Impact**: Typing channels persist indefinitely after leaving chat
- **Fix**: Add `realtimeManager.unsubscribeFromTypingIndicators()` to `stopRealtimeSubscription()`

## Medium Bugs

### BUG-003: Race Condition in Reactions/Read Receipts
- **File**: `ChatRepository.kt:621-656`
- **Issue**: Local Room update and Supabase sync not atomic; if sync fails, local state diverges
- **Fix**: Add retry queue for reactions (like message sync)

### BUG-006: Error Events Not Connected to UI
- **File**: `SupabaseRealtimeManager.kt:105-107`
- **Issue**: `_errorEvents` SharedFlow emitted but never collected in ChatViewModel
- **Fix**: Collect error events in ChatViewModel, show toast on parse failures

### BUG-007: Typing Indicator No Timeout
- **File**: `ChatViewModel.kt:515-523`
- **Issue**: If user goes offline while typing, "User typing..." persists forever
- **Fix**: Add 5-second timeout to auto-remove typing indicator

## Architecture Verification

- Channel cleanup in `ChatViewModel.onCleared()`: CORRECT (lines 588-607)
- Message ordering: DESC by timestamp in Room DAO, correct
- Message parsing: Required field validation present (BUG-006 fix)
- Realtime insert/update/delete: Room DAO updated from realtime events correctly

## Low Priority

- BUG-008: Reaction sync fire-and-forget (no retry)
- BUG-009: Task presence/editing broadcast not fully implemented (local-only)
