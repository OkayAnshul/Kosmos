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