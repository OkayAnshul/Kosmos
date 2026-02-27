# Kosmos - Quick Context (< 1000 tokens)

**Last Updated:** 2026-01-19
**Purpose:** Ultra-fast context loading for AI assistants and quick reference

---

## 🎯 What is Kosmos?

**Project Management System** (NOT just chat) with real-time collaboration
- **Core Model:** Projects → Members → Chat Rooms → Tasks + RBAC
- **Users:** Small-medium dev teams (5-50 members), remote async communication
- **Stack:** Kotlin + Jetpack Compose + Room + Supabase + Firebase Auth

---

## 📊 Current Status (2026-01-19)

- **Build:** ✅ Successful
- **Completion:** 78% (22/22 screens functional, some features incomplete)
- **Phase:** Bug Fixes & TODO Resolution (Phases 2-4)
- **Next:** UI redesign (design-only, no backend wiring)

---

## ⚠️ Critical Issues (P0 - Blocks Production)

1. ❌ **Destructive migrations** → Data loss risk on schema changes
2. ❌ **Photo upload incomplete** → UI works, Supabase Storage not wired
3. ❌ **Zero automated tests** → No safety net
4. ⚠️ **RLS policies unaudited** → Potential data leaks

---

## 🏗️ Architecture

**Pattern:** MVVM + Repository + Offline-First

**Data Flow:**
```
UI → ViewModel → Repository
  ├─ Room (immediate update + cache)
  └─ Supabase (async sync + real-time)
```

**Key Components:**
- 11 Domain Models (User, Project, Task, Message, ChatRoom, etc.)
- 6 Repositories (Auth, User, Project, Chat, Message, Task)
- 8 Room DAOs + 6 Supabase DataSources
- 11 ViewModels (@HiltViewModel)
- Design System: 5116 lines (ColorTokens, Tokens, Typography, Icons)

---

## 📁 File Locations (Quick Reference)

**Current Status:**
- Daily tracker → `/DEVELOPMENT_LOGBOOK.md` (root or `01-ACTIVE-STATUS/`)
- Full analysis → `01-ACTIVE-STATUS/COMPREHENSIVE_CODEBASE_ANALYSIS_2026-01-19.md`
- Known issues → `01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md`
- What to build → `01-ACTIVE-STATUS/IMPROVEMENT_ROADMAP.md`

**Technical Details:**
- All code docs → `02-TECHNICAL-REFERENCE/CODEBASE_MODULE_DOCS.md` (94+ files)
- UI flows → `02-TECHNICAL-REFERENCE/UI_UX_METHODS_FLOW.md`
- Integration → `02-TECHNICAL-REFERENCE/UI_INTEGRATION_GUIDE.md`

**Setup & Guides:**
- Database schema → `04-DATABASE/SCHEMA_FIX_COMPLETE_V2.sql`
- Backend setup → `03-GUIDES/SUPABASE_SETUP.md`
- Deployment → `03-GUIDES/DEPLOYMENT_GUIDE.md`
- UI specs → `/DESIGN_BRIEF_FOR_FIGMA.md` (root or `03-GUIDES/`)

**Design Reference:**
- React components → `documents/Kosmos/src/app/components/*.tsx` (69 files)
- Design tokens → `documents/Kosmos/src/styles/theme.css`

---

## 🎨 UI Design Work (Current Phase)

**Goal:** Implement 24 screens matching React design (design-only, NO backend wiring)

**Sources (in order):**
1. `DESIGN_BRIEF_FOR_FIGMA.md` - Screen requirements (functional ONLY)
2. `Kosmos/src/app/components/[Screen].tsx` - Visual design (PRIMARY)
3. `Kosmos/src/styles/theme.css` - Design tokens (#7C3AED primary, #0F0F14 background)

**Progress:** 7/24 screens complete (Phase 1)

**Rules:**
- ⛔ NO backend wiring
- ⛔ NO ViewModel implementation
- ✅ Match React design EXACTLY
- ✅ Use mock data

---

## 🔧 Tech Stack Details

**UI:** Jetpack Compose + Material 3
**Architecture:** MVVM + Repository + Offline-First
**DI:** Dagger Hilt
**Local DB:** Room (8 DAOs, offline cache)
**Remote DB:** Supabase PostgreSQL (6 tables: users, projects, project_members, chat_rooms, messages, tasks)
**Auth:** Firebase Auth + Google Sign-In
**Storage:** Supabase Storage
**Real-time:** Supabase Realtime (WebSocket)
**Notifications:** Firebase Cloud Messaging (FCM)
**Build:** Min SDK 26, Target SDK 36

---

## 📋 Common Tasks → Context to Load

**"Implement feature X"**
1. Check `01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md` (known issues)
2. Read `02-TECHNICAL-REFERENCE/CODEBASE_MODULE_DOCS.md` (technical details)
3. Check `04-DATABASE/SCHEMA_FIX_COMPLETE_V2.sql` (database capabilities)
4. Read relevant source files

**"Fix bug Y"**
1. Check `01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md` (known issues)
2. Check `05-SESSION-LOGS/LOGS_SESSIONS_ANALYSIS.md` (past similar issues)
3. Read relevant ViewModel/Repository

**"Implement UI screen Z"**
1. Check `/DEVELOPMENT_LOGBOOK.md` (screen status)
2. Read `/DESIGN_BRIEF_FOR_FIGMA.md` (requirements)
3. Read `Kosmos/src/app/components/[Screen].tsx` (design)
4. Read `Kosmos/src/styles/theme.css` (tokens)
5. Implement with mock data (NO backend)

**"Set up project"**
→ `03-GUIDES/SUPABASE_SETUP.md`

**"Deploy to production"**
→ `03-GUIDES/DEPLOYMENT_GUIDE.md` + fix P0 issues first

---

## 🎯 Development Principles

1. **Offline-first:** Room updates immediately, Supabase syncs
2. **Optimistic updates:** UI reflects changes before server confirmation
3. **Real-time sync:** Supabase listeners update local cache
4. **Result pattern:** Error handling everywhere
5. **Design system:** Always use tokens (spacing, colors, typography)
6. **MVVM:** UI → ViewModel → Repository → Data Source

---

## 📊 Project Evolution (15 months)

**Aug 2024:** Chat MVP (Firebase)
**Oct 2024:** Firebase → Supabase migration
**Oct-Nov 2024:** Schema fixes, offline-first sync, RBAC (49 permissions)
**Nov 2024:** 25x performance boost (metadata caching), UI redesign (5116-line design system)
**Dec 2025:** Task management enhancements, UX polish
**Jan 2026:** Context cleanup, UI redesign (React parity)

---

## 💡 Key Insights

**Misconception:** Kosmos is a chat app
**Reality:** Project management system (chat is one feature)

**Strengths:**
- ✅ Solid architecture (MVVM, offline-first)
- ✅ Modern stack (Compose, Material 3)
- ✅ Exceptional documentation
- ✅ Production-grade design system

**Weaknesses:**
- ❌ Zero automated tests
- ❌ Destructive migrations (data loss risk)
- ⚠️ Incomplete features (photo upload, settings persistence)

---

## 🚀 Next Steps

**Immediate (2 weeks):**
1. Implement Room migrations (P0)
2. Complete photo upload (P0)
3. Add basic unit tests (P0)

**Short-term (1 month):**
1. Continue UI redesign (17 screens remaining)
2. Wire redesigned screens to backend
3. Fix P1 issues (settings persistence, validation)

**Medium-term (3 months):**
1. Complete all 24 screens
2. Integration tests
3. Performance benchmarks
4. RLS audit

---

## 📖 Full Context

For comprehensive understanding, read:
1. `01-ACTIVE-STATUS/COMPREHENSIVE_CODEBASE_ANALYSIS_2026-01-19.md` (~7K tokens)
2. `01-ACTIVE-STATUS/PROJECT_OVERVIEW_STATUS.md` (~8K tokens)
3. `02-TECHNICAL-REFERENCE/CODEBASE_MODULE_DOCS.md` (~25K tokens)

**Total:** ~40K tokens for complete project understanding

---

**This quick context: ~800 tokens**
**Full comprehensive docs: ~40K tokens**
**Savings: 98% for daily tasks**
