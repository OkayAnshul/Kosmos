# Archived Legacy Documentation - December 24, 2025

This folder contains documentation files that have been superseded by the comprehensive documentation in `/documents/`.

## Why These Files Were Archived

As part of a complete codebase analysis and documentation consolidation, all historical documentation has been:
1. Thoroughly analyzed and verified against actual code
2. Consolidated into 6 comprehensive documents
3. Archived here for historical reference

## New Comprehensive Documentation Location

**All project documentation is now in `/documents/`:**

1. **PROJECT_OVERVIEW_STATUS.md** - Project purpose, status, architecture, evolution timeline
2. **CODEBASE_MODULE_DOCS.md** - Complete technical reference for all modules and classes
3. **UI_UX_METHODS_FLOW.md** - Complete UI method inventory enabling full redesign
4. **LOGS_SESSIONS_ANALYSIS.md** - Development timeline reconstructed from all sessions
5. **GAPS_RISKS_VERIFICATION.md** - Issues, contradictions, verification results
6. **IMPROVEMENT_ROADMAP.md** - Prioritized actionable recommendations

## Content Coverage

All information from archived files has been incorporated into the new documentation:

- **Session files** → LOGS_SESSIONS_ANALYSIS.md
- **Phase completion files** → PROJECT_OVERVIEW_STATUS.md
- **Architecture files** → CODEBASE_MODULE_DOCS.md
- **Design system files** → CODEBASE_MODULE_DOCS.md
- **UI phase files** → UI_UX_METHODS_FLOW.md
- **Testing files** → GAPS_RISKS_VERIFICATION.md
- **Planning & fix files** → GAPS_RISKS_VERIFICATION.md & IMPROVEMENT_ROADMAP.md
- **Build logs** → LOGS_SESSIONS_ANALYSIS.md
- **Old SQL migrations** → Superseded by SCHEMA_FIX_COMPLETE_V2.sql

## Active Documentation Remaining in Root

The following files remain active in the project root:

### Active Project Docs
- CLAUDE.md - Development instructions for Claude Code
- DEVELOPMENT_LOGBOOK.md - Ongoing project tracker
- MAIN_SCREENS_POLISH_LOGBOOK.md - Current phase tracker

### Authoritative Schema
- SCHEMA_FIX_COMPLETE_V2.sql - Current database schema
- FIX_NULL_USERNAMES_2025-11-09.sql - Required database fix
- RUN_THIS_IN_SUPABASE.sql - Database setup script

### Setup & Testing
- SUPABASE_SETUP.md - Supabase configuration guide
- SUPABASE_SQL_SETUP_QUICK_START.md - Quick start guide
- quick_test.sh - Test helper script
- run_rbac_tests.sh - RBAC test automation

## Restoration

If you need to restore any archived file:
```bash
cp archive/legacy_docs_2025-12-24/FILENAME.md ./
```

## Archive Date
December 24, 2025

## Verification
All archived content was verified to be covered in the new comprehensive documentation before archival.
