# CLAUDE.md

This file provides guidance to Claude Code when working with the Kosmos Android application.

---

## 🎯 CURRENT FOCUS: UI Design Implementation (Design-Only Phase)

**Goal**: Implement all 24 screens to match React design reference exactly (NO backend wiring).

**Phase**: Design-only implementation using mock data
**Next Phase**: Backend wiring (after all screens designed)

---

## 📚 Documentation Structure

**NEW (2026-01-19):** Documentation reorganized into use case-based folders!

**Quick Start:** Read `/documents/00-START-HERE/README.md` for complete navigation guide.

### Organized Folders

**📊 `/documents/01-ACTIVE-STATUS/`** - Check daily for current state
- `DEVELOPMENT_LOGBOOK.md` - Current phase tracker (also symlinked in root)
- `COMPREHENSIVE_CODEBASE_ANALYSIS_2026-01-19.md` - Latest full analysis
- `PROJECT_OVERVIEW_STATUS.md` - Architecture, tech stack, current state
- `GAPS_RISKS_VERIFICATION.md` - Known issues (P0/P1/P2)
- `IMPROVEMENT_ROADMAP.md` - Prioritized action items

**🔧 `/documents/02-TECHNICAL-REFERENCE/`** - Deep technical docs
- `CODEBASE_MODULE_DOCS.md` - Complete technical reference (94+ files)
- `UI_UX_METHODS_FLOW.md` - UI inventory & user flows
- `UI_INTEGRATION_GUIDE.md` - Component integration patterns

**📖 `/documents/03-GUIDES/`** - Setup & implementation guides
- `SUPABASE_SETUP.md` - Backend setup guide
- `SUPABASE_SQL_SETUP_QUICK_START.md` - Quick database setup
- `RUN_MIGRATION_GUIDE.md` - Database migration instructions
- `DEPLOYMENT_GUIDE.md` - Production deployment steps
- `DESIGN_BRIEF_FOR_FIGMA.md` - Screen specifications (also symlinked in root)

**🗄️ `/documents/04-DATABASE/`** - SQL scripts & schema
- `SCHEMA_FIX_COMPLETE_V2.sql` - Authoritative schema (production)
- `RLS_ENABLE_PRODUCTION.sql` - Row Level Security policies

**📝 `/documents/05-SESSION-LOGS/`** - Historical implementation logs
- `LOGS_SESSIONS_ANALYSIS.md` - Historical timeline summary
- Session completion logs (notifications, task management, race condition fixes)

**🎨 `/documents/Kosmos/`** - React design reference
- `src/app/components/*.tsx` - 69 React components (visual design)
- `src/styles/theme.css` - Design tokens (colors, spacing, typography)

### Context Loading Strategy

**For feature implementation:**
1. Check `documents/01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md` for known issues
2. Check `documents/02-TECHNICAL-REFERENCE/CODEBASE_MODULE_DOCS.md` for technical details
3. Check `documents/04-DATABASE/SCHEMA_FIX_COMPLETE_V2.sql` for database capabilities
4. Read relevant source files directly

**For bug fixing:**
1. Check `documents/01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md` first
2. Review `documents/05-SESSION-LOGS/LOGS_SESSIONS_ANALYSIS.md` for similar past issues
3. Check relevant ViewModel/Repository in codebase

**For UI design work (CURRENT PHASE):**
1. Check `DEVELOPMENT_LOGBOOK.md` for screen status and progress (root symlink)
2. Read `DESIGN_BRIEF_FOR_FIGMA.md` for screen requirements (root symlink)
3. Read `documents/Kosmos/src/app/components/[Screen].tsx` for visual design (PRIMARY)
4. Read `documents/Kosmos/src/styles/theme.css` for design tokens
5. Implement in Jetpack Compose using mock data (NO backend wiring)

---

## 🏗️ Project Overview

**Type**: Project Management System with real-time collaboration
**Core Features**: Projects → Members → Chat Rooms → Tasks + RBAC

### Tech Stack
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Repository + Offline-First
- **DI**: Dagger Hilt
- **Local DB**: Room (8 DAOs, offline caching)
- **Remote DB**: Supabase PostgreSQL (6 tables + RLS)
- **Auth**: Firebase Auth + Google Sign-In
- **Storage**: Supabase Storage (voice, images, files)
- **Real-time**: Supabase Realtime (WebSocket subscriptions)
- **Notifications**: Firebase Cloud Messaging (FCM)

### Architecture Pattern

**Data Flow:**
```
UI (Compose)
  ↓ Events
ViewModels (@HiltViewModel)
  ↓ Calls
Repositories (hybrid: Room + Supabase)
  ↓ Immediate update
Room DB (local cache)
  ↓ Sync (if online)
Supabase (remote sync + real-time listeners)
  ↓ Emit updates
Repository (Flow)
  ↓ Collect
ViewModel (StateFlow)
  ↓ Observe
UI (recompose)
```

**Key Principles:**
- Offline-first (Room updates immediately, Supabase syncs)
- Optimistic updates (UI reflects changes before server confirmation)
- Real-time sync (Supabase listeners update local cache)
- Result pattern for error handling

### File Structure

**Core modules** (see `documents/02-TECHNICAL-REFERENCE/CODEBASE_MODULE_DOCS.md` for details):
- `/models/models.kt` - 11 domain models
- `/Module.kt` - Hilt dependency injection
- `/viewmodels/` - 11 ViewModels
- `/data/repositories/` - 6 repositories
- `/data/datasource/` - 6 Supabase data sources + real-time manager
- `/data/local/` - Room database + 8 DAOs
- `/shared/ui/designsystem/` - Design system (5116 lines)

---

## 🔧 Development Commands

**Build & Run:**
```bash
./gradlew build              # Full build
./gradlew assembleDebug      # Debug APK
./gradlew installDebug       # Install to device
./gradlew clean              # Clean build
```

**Testing:**
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
```

---

## ⚠️ Known Issues (Priority P0-P1)

See `documents/01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md` for full list. Critical items:

**P0 - Critical:**
1. Room migrations missing (destructive fallback = data loss)
2. Photo upload UI exists but Supabase Storage not wired
3. Zero automated tests (0% coverage)

**P1 - High:**
4. Privacy/Notification settings UI exists but doesn't persist
5. Some ViewModels missing error handling
6. No input validation on forms

**Refer to `documents/01-ACTIVE-STATUS/IMPROVEMENT_ROADMAP.md` for implementation steps.**

---

## 🎨 Design System

**Location:** `/app/src/main/java/com/example/kosmos/shared/ui/designsystem/`

**Components:**
- `Tokens.kt` - Spacing, sizing, dimensions
- `Colors.kt` - Color palette, Material 3 theme
- `Typography.kt` - Text styles, font scales
- `Icons.kt` - 100+ Material icons

**Usage:** Always use design system tokens instead of hardcoded values.

---

## 📋 Development Workflow

### Before Starting Work
1. Check `DEVELOPMENT_LOGBOOK.md` for current phase status (root or `documents/01-ACTIVE-STATUS/`)
2. Review relevant docs in `/documents/` organized folders
3. Check `documents/01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md` for known issues in that area

### During Work
1. Use design system tokens (spacing, colors, typography)
2. Follow MVVM pattern (UI → ViewModel → Repository → Data Source)
3. Implement offline-first (update Room first, sync Supabase)
4. Use Result pattern for error handling
5. Write state to ViewModel StateFlow, collect in UI

### After Work
1. Update `DEVELOPMENT_LOGBOOK.md` with progress
2. Test offline behavior (disable network)
3. Verify real-time sync works
4. Check for proper error handling

---

## 🔑 Configuration

**API Keys & Services:**
- Supabase: URL + anon key in `build.gradle.kts`
- Firebase: `google-services.json` (Auth + FCM)
- Google Cloud Speech: API key in build config

**Database Schema:**
- Authoritative source: `documents/04-DATABASE/SCHEMA_FIX_COMPLETE_V2.sql`
- Tables: users, projects, project_members, chat_rooms, messages, tasks
- Features: RBAC (49 permissions), metadata caching, RLS policies

**Build Config:**
- Min SDK: 26 (Android 8.0)
- Target SDK: 36
- KSP for Room + Hilt annotation processing

---

## 🎨 UI IMPLEMENTATION TRACKING

**Primary Log**: `DEVELOPMENT_LOGBOOK.md` (root or `documents/01-ACTIVE-STATUS/`)
- Tracks all screen implementations and current work
- Maps design system (React → Android)
- Records design decisions and session progress

**Design Sources (CRITICAL - Read in Order)**:
1. `DESIGN_BRIEF_FOR_FIGMA.md` (root symlink or `documents/03-GUIDES/`) - **Screen requirements ONLY**
   - What screen does, user actions, states
   - ⛔ DO NOT use for colors, spacing, or styling
2. `documents/Kosmos/src/app/components/[Screen].tsx` - **PRIMARY visual design source**
   - Exact layout structure, component hierarchy
   - Spacing, colors, typography to replicate
3. `documents/Kosmos/src/styles/theme.css` - **Design tokens**
   - Color hex values: `--primary: #7C3AED`, `--background: #0F0F14`, etc.
   - Typography scale, border radius, spacing
4. Backend wiring (FUTURE - NOT NOW) → `documents/02-TECHNICAL-REFERENCE/CODEBASE_MODULE_DOCS.md`, `documents/04-DATABASE/SCHEMA_FIX_COMPLETE_V2.sql`

**Implementation Workflow (5 Steps)**:
1. Design Analysis: Read React component + DESIGN_BRIEF + theme.css
2. Design System Prep: Map colors/spacing to ColorTokens.kt and Tokens.kt
3. Compose Implementation: Build screen with mock data (NO ViewModels/Repositories)
4. Documentation: Update DEVELOPMENT_LOGBOOK.md
5. Verification: Visual comparison with React reference

**Current Phase**: Design-only (use mock/hardcoded data)
- ⛔ NO backend wiring
- ⛔ NO ViewModel implementation
- ⛔ NO navigation logic
- ✅ Match React design EXACTLY
- ✅ Use theme.css for all values

---

## 📖 Quick Reference

**Need to understand:**
- Architecture? → `documents/01-ACTIVE-STATUS/PROJECT_OVERVIEW_STATUS.md`
- Code structure? → `documents/02-TECHNICAL-REFERENCE/CODEBASE_MODULE_DOCS.md`
- UI flows? → `documents/02-TECHNICAL-REFERENCE/UI_UX_METHODS_FLOW.md`
- What's broken? → `documents/01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md`
- What to build next? → `documents/01-ACTIVE-STATUS/IMPROVEMENT_ROADMAP.md`

**Common tasks:**
- **Implement UI screen** → Check `DEVELOPMENT_LOGBOOK.md`, read React component in `documents/Kosmos/`, use mock data
- Add new feature → Check schema capabilities in `documents/04-DATABASE/`, wire Repository → ViewModel → UI
- Fix bug → Check `documents/01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md`, locate ViewModel/Repository, fix with proper error handling
- Database change → Update Room entities + DAOs, create migration in `documents/04-DATABASE/`

---

## 🎯 Current Development Priorities

**Phase: UI Design Implementation (Design-Only)**

**Priority Order**:
1. **Phase 1**: Core screens with React references
   - Project List, Project Details, My Tasks, Task Detail, Task Edit, Chat List, Chat Room
2. **Phase 2**: Secondary screens with partial React references
   - Task Board, Activity Log, Members screens
3. **Phase 3**: Screens from DESIGN_BRIEF only
   - Auth (Login/SignUp), Settings, Profile, Notifications

**Implementation Focus**:
1. Map all design tokens from theme.css to ColorTokens.kt
2. Create reusable components (KosmosCard, StatusBadge, PriorityBadge, etc.)
3. Implement screens with mock data (NO backend wiring)
4. Match React designs EXACTLY
5. Document everything in UI_IMPLEMENTATION_LOG.md

**Critical Rule**: NO backend wiring during this phase (ViewModels/Repositories come later)

**Next Phase: Backend Wiring**
- Wire all UI screens to existing ViewModels/Repositories
- Implement proper error handling
- Add offline-first sync
- Form validation

---

**Last Updated**: January 19, 2026
**Version**: 3.1 (Documentation reorganization + use case-based structure)
