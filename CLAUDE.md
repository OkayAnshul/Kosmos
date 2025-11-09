# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 📋 IMPORTANT: Development Logbook

**ALWAYS CHECK THIS FIRST**: Before starting any work on this project, read `/DEVELOPMENT_LOGBOOK.md` to understand:
- Current development phase and progress
- Completed tasks and pending tasks
- Known issues and blockers
- Performance metrics and targets
- Phase completion criteria
- Lessons learned from previous work

The logbook is the **single source of truth** for:
1. What has been implemented
2. What needs to be done
3. Current phase status
4. Testing and review checklists
5. Performance benchmarks
6. Cost/usage tracking

**Workflow**: Read logbook → Update progress → Work on tasks → Update logbook → Review checkpoint

---

## 🎨 CURRENT PHASE: Main Screens Polish & Feature Enhancement

**STATUS: ACTIVE** - The project is now in the **Main Screens Polish Phase** where we focus on user-facing screens (TaskBoard, Chat, Profile, Projects) with feature-rich implementations.

### 🔥 Main Screens Polish Documentation (MUST READ - CURRENT WORK)

**For ALL main screens work**, use this specialized logbook:

1. **`/MAIN_SCREENS_POLISH_LOGBOOK.md`** - Main Screens Polish Tracker ⭐ **PRIMARY DOCUMENT**
   - Read this FIRST before any main screen work
   - Contains 10-day feature-rich enhancement plan (4 phases)
   - Daily progress tracking with checkboxes
   - Schema features to unlock (subtasks, reactions, threading, etc.)
   - Design improvements tracking
   - Context file guide per phase
   - **Use this when:** Working on TaskBoard, Chat, Profile, Projects screens
   - **Update this:** At end of each day with progress, issues, decisions

### Main Screens Polish Workflow

```
START MAIN SCREEN TASK
    ↓
1. Read MAIN_SCREENS_POLISH_LOGBOOK.md → Check current day & phase tasks
    ↓
2. Load phase-specific context files (listed in logbook)
    ↓
3. Check schema (SCHEMA_FIX_COMPLETE_V2.sql) for feature capabilities
    ↓
4. Implement using design system (/shared/ui/designsystem/)
    ↓
5. Update MAIN_SCREENS_POLISH_LOGBOOK.md → Mark complete, log notes
    ↓
6. Build and test → Move to next task
    ↓
END MAIN SCREEN TASK
```

### When to Use Which Document

**Use `/MAIN_SCREENS_POLISH_LOGBOOK.md` for:** (CURRENT PHASE)
- Daily task planning for main screens
- Progress tracking on TaskBoard/Chat/Profile/Projects
- Schema feature unlock tracking
- Phase-specific context file list
- Design improvements log

**Use `/SCHEMA_FIX_COMPLETE_V2.sql` for:**
- Understanding database capabilities
- Planning schema-powered features
- Checking what's possible (subtasks, threading, reactions, etc.)

### 🔥 Previous UI Phase Documentation (REFERENCE ONLY)

**For ALL UI-related work**, use these specialized documents instead of the general development logbook:

1. **`/UI_AUDIT_REPORT_2025-11-08.md`** - Comprehensive UI Analysis
   - Read this FIRST to understand current UI state
   - Contains screen-by-screen functionality analysis
   - Lists all broken/incomplete/working features
   - Identifies critical issues and priorities
   - **Use this when:** You need to understand what's broken, what works, or what needs to be built

2. **`/UI_ENHANCEMENT_LOGBOOK.md`** - UI Phase Project Tracker
   - Read this BEFORE starting each UI task
   - Contains the 10-day UI enhancement plan (6 phases)
   - Daily progress tracking and checklists
   - Task-by-task breakdown with file locations
   - Review checkpoints and success criteria
   - **Use this when:** Planning work, tracking progress, or reviewing completed work
   - **Update this:** Daily with progress, issues, and notes

### UI Phase Workflow

```
START UI TASK
    ↓
1. Read UI_ENHANCEMENT_LOGBOOK.md → Check current phase & today's tasks
    ↓
2. Read UI_AUDIT_REPORT_2025-11-08.md → Understand specific screen/feature details
    ↓
3. Implement the task following the plan
    ↓
4. Update UI_ENHANCEMENT_LOGBOOK.md → Mark tasks complete, log issues, add notes
    ↓
5. At phase completion → Review phase checklist in logbook
    ↓
END UI TASK
```

### When to Use Which Document

**Use `/UI_ENHANCEMENT_LOGBOOK.md` for:**
- Daily task planning
- Progress tracking (checkboxes)
- Recording decisions made
- Logging issues and blockers
- Phase review checkpoints
- Success metrics tracking

**Use `/UI_AUDIT_REPORT_2025-11-08.md` for:**
- Understanding current UI state
- Finding which features are broken
- Checking backend support status
- Getting specific screen details
- Understanding navigation flow
- Reference during implementation

**Use `/DEVELOPMENT_LOGBOOK.md` for:**
- Non-UI backend work
- Database/API changes
- Performance optimizations
- Overall project history

### UI Phase Priorities

Based on user requirements, prioritize in this order:

1. **CRITICAL (Do First):**
   - Privacy Settings Screen
   - Notification Settings Screen
   - Project Edit Dialog
   - Members List Screen
   - Chat Management UI (archive/pin/delete)

2. **HIGH (Do Second):**
   - Design System implementation
   - Component library creation
   - Photo upload to Supabase Storage

3. **MEDIUM (Do Third):**
   - Apply design system to all screens
   - Animations and transitions
   - Empty states with illustrations

4. **NICE TO HAVE (If Time):**
   - Search/filter features
   - Advanced accessibility
   - Performance optimizations

### UI Phase Success Criteria

Before marking UI phase complete, ensure:
- [ ] Zero broken/incomplete UI elements
- [ ] All navigation working end-to-end
- [ ] Consistent design across all screens
- [ ] Smooth animations on all transitions
- [ ] Accessibility score 90%+
- [ ] No duplicate/legacy code in main source
- [ ] Complete design system documentation
- [ ] All features fully tested
- [ ] Both UI logbook and audit report updated

### IMPORTANT NOTES FOR UI WORK

- **Always** check if backend methods exist before creating UI (see audit report)
- **Always** update the UI_ENHANCEMENT_LOGBOOK.md daily log
- **Always** use the design system once it's created (Phase 3)
- **Never** create new screens without checking audit report first
- **Never** skip the review checkpoints at end of each phase
- **Archive** old screen files, don't delete (user wants to keep for reference)

## Project Overview

Kosmos is an Android chat application built with Kotlin and Jetpack Compose. It features real-time messaging, voice transcription, task management, and smart action detection. The app uses a hybrid backend approach with Firebase for authentication and Supabase for database and storage services, plus Room for local data persistence.

## Development Commands

### Building and Running
- `./gradlew build` - Build the entire project
- `./gradlew assembleDebug` - Build debug APK
- `./gradlew assembleRelease` - Build release APK
- `./gradlew installDebug` - Install debug version to connected device
- `./gradlew clean` - Clean build artifacts

### Testing
- `./gradlew test` - Run unit tests
- `./gradlew connectedAndroidTest` - Run instrumented tests on device/emulator

### Code Quality
- The project uses KSP (Kotlin Symbol Processing) for Room database and Hilt dependency injection
- Lint checks are part of the build process

## Architecture Overview

### Tech Stack
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Dagger Hilt
- **Database**: Room (local) + Supabase PostgreSQL (remote)
- **Authentication**: Firebase Auth with Google Sign-In
- **Storage**: Supabase Storage for voice messages and files
- **Push Notifications**: Firebase Cloud Messaging (FCM)
- **Real-time**: Supabase real-time subscriptions
- **Build System**: Gradle with Kotlin DSL

### Key Architectural Components

#### Data Layer
- **Local Database**: Room database with entities for User, ChatRoom, Message, VoiceMessage, Task, ActionItem
- **Remote Database**: Supabase PostgreSQL with real-time listeners
- **Remote Storage**: Supabase Storage for voice messages and file uploads
- **Repository Pattern**: Hybrid approach combining local caching with remote sync
    - Repositories emit local data first, then sync with Supabase
    - All repositories implement Result pattern for error handling

#### Domain Models
Located in `models/models.kt`:
- **User**: User profile and authentication data
- **ChatRoom**: Chat room metadata and participant management
- **Message**: Text, voice, and system messages with reactions and read receipts
- **Task**: Task management with status, priority, and assignment
- **VoiceMessage**: Voice recordings with transcription and waveform data
- **ActionItem**: AI-detected action items from messages

#### Dependency Injection Structure
All DI modules are in `Module.kt`:
- **DatabaseModule**: Room database and DAO providers
- **SupabaseModule**: Supabase client and service providers
- **FirebaseModule**: Firebase Auth and FCM service providers
- **NetworkModule**: Retrofit and OkHttp configuration for Google Cloud Speech API
- **RepositoryModule**: Repository layer providers with hybrid data sources
- **ServiceModule**: Business logic service providers

#### UI Layer
- **MainActivity**: Single activity hosting Compose navigation
- **Navigation**: Sealed class `Screen` with type-safe navigation
- **ViewModels**: Use Hilt injection with `@HiltViewModel`
- **Screens**: All UI implemented with Compose, screens defined in various files

### Core Features Architecture

#### Real-time Messaging
- Supabase real-time subscriptions for live message updates
- Local Room caching with offline support
- Message types: TEXT, VOICE, IMAGE, FILE, SYSTEM, TASK_CREATED
- Firebase FCM for push notifications when app is backgrounded

#### Voice Transcription
- Google Cloud Speech-to-Text integration via Retrofit
- Audio files stored in Supabase Storage
- Asynchronous transcription with confidence scoring
- Waveform visualization support

#### Task Management
- Tasks can be created from messages or voice transcriptions
- Task board view per chat room
- Status tracking: TODO, IN_PROGRESS, DONE, CANCELLED
- Priority levels and due date support

#### Authentication & User Management
- Firebase Auth for user authentication
- Google Sign-In integration
- User profiles stored in Supabase PostgreSQL
- Session management with automatic token refresh

#### Smart Features
- **ActionDetectionService**: Detects actionable items in messages
- **SmartReplyService**: Generates contextual reply suggestions
- Background processing for AI features

### Hybrid Backend Architecture

#### Firebase Services (Keep)
- **Authentication**: Firebase Auth with Google Sign-In
- **Push Notifications**: Firebase Cloud Messaging (FCM)
- **Analytics**: Firebase Analytics and Crashlytics
- **Configuration**: Firebase Remote Config (optional)

#### Supabase Services (New)
- **Database**: PostgreSQL for all app data (users, messages, tasks)
- **Storage**: File storage for voice messages, images, documents
- **Real-time**: Live subscriptions for chat messages and presence
- **Edge Functions**: Server-side logic (if needed)

#### Local Storage
- **Room Database**: Offline caching and fast local queries
- **SharedPreferences**: App settings and user preferences

### Data Flow Pattern
1. UI triggers action via ViewModel
2. ViewModel calls Repository method
3. Repository updates local Room database immediately
4. Repository syncs with Supabase (if online)
5. Supabase real-time listeners update local data
6. Repository emits updated data to UI via Flow
7. Firebase FCM sends push notifications for background events

### Service Configuration

#### Supabase Configuration
- **Database**: PostgreSQL with Row Level Security (RLS)
- **Storage**: Public buckets for voice messages with proper access controls
- **Real-time**: Channel subscriptions for chat rooms and user presence
- **API**: RESTful API with automatic OpenAPI documentation

#### Firebase Configuration
- **Authentication**: Email/password and Google Sign-In providers
- **FCM**: Push notification tokens managed per user device
- **Security Rules**: Configured for user data access control

### Build Configuration
- **Min SDK**: 26 (Android 8.0)
- **Target/Compile SDK**: 36
- **Build Config**: Separate debug/release configurations with API key management
- **API Keys**:
    - Google Cloud Speech API key
    - Supabase URL and Anonymous Key
    - Firebase configuration (google-services.json)
- **Proguard**: Disabled for release builds (consider enabling for production)

## Development Notes

### API Keys & Configuration
- Google Cloud Speech API key configured in build.gradle.kts
- Supabase URL and anonymous key in build config
- Firebase configuration via google-services.json
- Debug builds use development Supabase project
- Production builds require separate Supabase project and API keys

### Database Migrations
- Room database version 1, no migrations implemented yet
- Supabase schema managed via SQL migrations
- Future schema changes will require both Room and Supabase migrations

### Migration Strategy
#### Phase 1 (Current): Hybrid Setup
- Keep Firebase Auth and FCM
- Migrate storage to Supabase Storage
- Set up Supabase database alongside existing local Room

#### Phase 2 (Future): Data Migration
- Migrate message data to Supabase PostgreSQL
- Implement Supabase real-time subscriptions
- Maintain Room for offline caching

#### Phase 3 (Optional): Full Migration
- Consider migrating auth to Supabase (if needed)
- Evaluate Firebase dependency reduction
- Optimize for single backend if beneficial

### Testing Strategy
- Unit tests in `src/test/`
- Instrumented tests in `src/androidTest/`
- Mock both Firebase and Supabase services for testing
- Test coverage currently minimal, consider expanding
- Integration tests for hybrid data flow

### Performance Considerations
- Voice messages use Supabase Storage with local caching
- Message pagination implemented for chat history
- Offline-first architecture with Room database
- Supabase connection pooling for efficient database queries
- Firebase Auth token refresh handled automatically
- Real-time subscription management to prevent memory leaks

### Cost Optimization
- **Free Tiers Used**:
    - Supabase: 1GB storage, 2GB bandwidth, 500MB database
    - Firebase: Authentication, FCM, basic analytics (free forever)
    - Google Cloud Speech: Free tier or pay-per-use
- **Monitoring**: Track usage to stay within free limits
- **Optimization**: Compress voice messages, implement efficient pagination