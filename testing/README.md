# Kosmos Pre-Production Testing & Hardening

## Overview

Comprehensive testing plan to prepare Kosmos for Play Store release. Covers security (RLS), sync verification, real-time chat, RBAC, code quality, and performance.

**Supabase Project**: `krbfvekgqbcwjgntepip` (ap-south-1)

## Test Documents

| File | Phase | Description |
|------|-------|-------------|
| `00-ARCHITECTURE-ANALYSIS.md` | 1 | Architecture summary, data flow, key components |
| `01-RLS-SECURITY-AUDIT.md` | 2 | RLS policies, security fixes, verification |
| `02-SUPABASE-SYNC-TESTING.md` | 3 | CRUD sync verification per entity |
| `03-REALTIME-CHAT-TESTING.md` | 4 | Real-time messaging, typing, reactions |
| `04-ACTIVITY-TRACKING-TESTING.md` | 5 | Activity log sync verification |
| `05-MULTI-USER-TESTING.md` | 6 | Multi-user RBAC enforcement |
| `06-CODE-QUALITY-AUDIT.md` | 7 | Code gaps, dead code, bugs |
| `07-PERFORMANCE-TESTING.md` | 8 | Index cleanup, query performance |
| `08-PRODUCTION-CHECKLIST.md` | 9 | Final go/no-go checklist |

## Session Logs

Per-session findings go in `session-logs/YYYY-MM-DD-session-N.md`.

## Execution Order

1. Phase 1: Setup (this folder)
2. Phase 2+3: RLS + Sync testing (per table)
3. Phase 4: Real-time chat
4. Phase 5: Activity tracking
5. Phase 6: Multi-user RBAC
6. Phase 7: Code quality audit
7. Phase 8: Performance cleanup
8. Phase 9: Production checklist

## Verification After Each Phase

- `./gradlew compileDebugKotlin` — no build breaks
- Supabase `get_advisors` — security + performance improvements
- Update session log + production checklist
