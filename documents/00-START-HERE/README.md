# Kosmos Documentation Guide

**Last Updated:** 2026-01-19

This folder contains all project documentation organized by use case. Start here to understand the project structure and find what you need quickly.

---

## 📚 Documentation Structure

### 🎯 [00-START-HERE/](./00-START-HERE/) (You are here!)
**When to use:** First time exploring the project
- `QUICK_CONTEXT.md` - ⚡ Ultra-fast context (<1000 tokens, <30 seconds read)
- `README.md` - This file, your complete navigation guide
- `LEGACY-README.md` - Archived old README (historical reference)

### 📊 [01-ACTIVE-STATUS/](./01-ACTIVE-STATUS/)
**When to use:** Daily development, checking current progress
- `DEVELOPMENT_LOGBOOK.md` - Current phase tracker (check daily)
- `COMPREHENSIVE_CODEBASE_ANALYSIS_2026-01-19.md` - Latest full analysis
- `PROJECT_OVERVIEW_STATUS.md` - Architecture, tech stack, current state
- `GAPS_RISKS_VERIFICATION.md` - Known issues (P0/P1/P2)
- `IMPROVEMENT_ROADMAP.md` - Prioritized action items

### 🔧 [02-TECHNICAL-REFERENCE/](./02-TECHNICAL-REFERENCE/)
**When to use:** Understanding codebase, implementing features
- `CODEBASE_MODULE_DOCS.md` - Complete technical reference (94+ files)
- `UI_UX_METHODS_FLOW.md` - UI inventory & user flows
- `UI_INTEGRATION_GUIDE.md` - Component integration patterns

### 📖 [03-GUIDES/](./03-GUIDES/)
**When to use:** Setup, deployment, design implementation
- `SUPABASE_SETUP.md` - Backend setup guide
- `SUPABASE_SQL_SETUP_QUICK_START.md` - Quick database setup
- `RUN_MIGRATION_GUIDE.md` - Database migration instructions
- `DEPLOYMENT_GUIDE.md` - Production deployment steps
- `DESIGN_BRIEF_FOR_FIGMA.md` - Screen specifications (functional only)

### 🗄️ [04-DATABASE/](./04-DATABASE/)
**When to use:** Database schema changes, SQL scripts
- `SCHEMA_FIX_COMPLETE_V2.sql` - Authoritative schema (production)
- `RLS_ENABLE_PRODUCTION.sql` - Row Level Security policies

### 📝 [05-SESSION-LOGS/](./05-SESSION-LOGS/)
**When to use:** Understanding past implementations, debugging similar issues
- `LOGS_SESSIONS_ANALYSIS.md` - Historical timeline summary
- Session completion logs (notifications, task management, etc.)
- Race condition fixes and implementation logs

### 🎨 [Kosmos/](./Kosmos/)
**When to use:** UI design implementation (React reference)
- `src/app/components/*.tsx` - 69 React components (visual design reference)
- `src/styles/theme.css` - Design tokens (colors, spacing, typography)

---

## 🚀 Quick Start Paths

### New Developer Onboarding

**⚡ Fast Track (5 minutes):**
1. Read `00-START-HERE/QUICK_CONTEXT.md` (~800 tokens, <1 min)
2. Skim `01-ACTIVE-STATUS/DEVELOPMENT_LOGBOOK.md` (current status)
3. Check `01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md` (known issues)
4. Start coding with context!

**📚 Comprehensive (1 hour):**
1. Read `01-ACTIVE-STATUS/PROJECT_OVERVIEW_STATUS.md` (architecture, tech stack)
2. Read `01-ACTIVE-STATUS/COMPREHENSIVE_CODEBASE_ANALYSIS_2026-01-19.md` (full context)
3. Check `01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md` (known issues)
4. Set up: `03-GUIDES/SUPABASE_SETUP.md`
5. Refer to `02-TECHNICAL-REFERENCE/CODEBASE_MODULE_DOCS.md` as needed

### Implementing a Feature
1. Check `01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md` for known issues in that area
2. Check `02-TECHNICAL-REFERENCE/CODEBASE_MODULE_DOCS.md` for technical details
3. Check `04-DATABASE/SCHEMA_FIX_COMPLETE_V2.sql` for database capabilities
4. Read relevant source files directly

### Fixing a Bug
1. Check `01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md` first
2. Review `05-SESSION-LOGS/LOGS_SESSIONS_ANALYSIS.md` for similar past issues
3. Check relevant ViewModel/Repository in codebase

### UI Design Work (Current Phase)
1. Check root `DEVELOPMENT_LOGBOOK.md` for current screen status
2. Read `03-GUIDES/DESIGN_BRIEF_FOR_FIGMA.md` for screen requirements (functional only)
3. Read `Kosmos/src/app/components/[Screen].tsx` for visual design (PRIMARY)
4. Read `Kosmos/src/styles/theme.css` for design tokens
5. Implement in Jetpack Compose using mock data (NO backend wiring)

### Deploying to Production
1. Read `03-GUIDES/DEPLOYMENT_GUIDE.md`
2. Run scripts from `04-DATABASE/`
3. Address P0 items from `01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md`

---

## 📋 Root Directory Files

**Keep in root for quick access:**
- `/CLAUDE.md` - AI assistant context (instructions for Claude Code)
- `/DEVELOPMENT_LOGBOOK.md` - Active development tracker (current progress)
- `/README.md` - Project README (GitHub/overview)

---

## 🗂️ Archive Structure

Historical documentation is preserved in `/archive/`:
- `/archive/legacy_docs_2025-12-24/` - 75+ files from 15 months of development
- `/archive/context_cleanup_2026-01-11/` - Recent session logs
- `/archive/old_summaries_2026-01-16/` - Development logbook archives
- `/archive/legacy_ui/` - Old UI implementations

**When to use archives:** Researching past decisions, understanding project evolution

---

## 🎯 Finding Information Fast

**"What's the current status?"**
→ `01-ACTIVE-STATUS/DEVELOPMENT_LOGBOOK.md`

**"What's broken/missing?"**
→ `01-ACTIVE-STATUS/GAPS_RISKS_VERIFICATION.md`

**"How does [feature] work?"**
→ `02-TECHNICAL-REFERENCE/CODEBASE_MODULE_DOCS.md`

**"What's the database schema?"**
→ `04-DATABASE/SCHEMA_FIX_COMPLETE_V2.sql`

**"How do I set up Supabase?"**
→ `03-GUIDES/SUPABASE_SETUP.md`

**"How do I implement [UI screen]?"**
→ `Kosmos/src/app/components/[Screen].tsx` + `03-GUIDES/DESIGN_BRIEF_FOR_FIGMA.md`

**"What happened in past sessions?"**
→ `05-SESSION-LOGS/LOGS_SESSIONS_ANALYSIS.md`

**"Complete project context?"**
→ `01-ACTIVE-STATUS/COMPREHENSIVE_CODEBASE_ANALYSIS_2026-01-19.md`

---

## 📊 Documentation Maintenance

**Active documents** (update frequently):
- Root: `DEVELOPMENT_LOGBOOK.md`
- `01-ACTIVE-STATUS/` - All files updated as project evolves

**Reference documents** (update when codebase changes):
- `02-TECHNICAL-REFERENCE/` - Update when files/patterns change

**Static documents** (rarely change):
- `03-GUIDES/` - Update when setup process changes
- `04-DATABASE/` - Update when schema changes
- `05-SESSION-LOGS/` - Append only (new sessions added)

**Archive strategy:**
- When session logs accumulate (>10 files), move oldest to `/archive/`
- When major refactoring happens, archive old analyses
- Preserve, don't delete - maintain complete history

---

## 🏆 Documentation Quality

- **Confidence Level:** 95%
- **Completeness:** 6 comprehensive documents consolidating 75+ historical files
- **Accuracy:** Evidence-based with file paths, line numbers, code snippets
- **Maintainability:** Clear structure, timestamped files, version tracking

---

**For AI Assistants (Claude Code):**
This documentation structure is designed for efficient context loading. Start with `01-ACTIVE-STATUS/` for current context, then drill into specific folders as needed. The root `/CLAUDE.md` file provides overarching guidance.
