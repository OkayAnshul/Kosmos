# Kosmos Android App - Project Structure & Organization Plan

## 🚨 **Current Issues Identified**

### **Critical Problems:**
1. **Mixed Architecture**: Files scattered across root package with no clear domain separation
2. **Duplicate Functionality**: Multiple files handling similar responsibilities
3. **Large Monolithic Files**: Chat.kt (~1400+ lines), Services.kt (~700+ lines)
4. **Broken Features**: Search, Voice Recording, Task Management not working properly
5. **No Clear Module Boundaries**: ViewModels, Repositories, Services mixed together

### **Specific Function Issues:**
- **Search**: Implementation exists but not connected properly to UI
- **Voice Recording**: Helper class exists but MediaRecorder setup may be faulty
- **Task Management**: Only creation works, no status updates/progress tracking

## 📁 **Current Structure Analysis**

```
com.example.kosmos/
├── Auth.kt                              # ❌ Should be in auth domain
├── Chat.kt                              # ❌ Too large, mixed responsibilities
├── MainActivity.kt                      # ✅ OK
├── KosmosApplication.kt                 # ✅ OK
├── Module.kt                            # ❌ Should be split by domain
├── Screens.kt                           # ❌ Too large, mixed UI concerns
├── GlobalRepository.kt                  # ❌ God object anti-pattern
├── Services.kt                          # ❌ Multiple services in one file
├── TaskViewModel.kt                     # ✅ OK placement
├── SpeechRecognitionHelper.kt           # ❌ Should be in voice domain
├── SpeechRecognitionViewModel.kt        # ❌ Should be in voice domain
├── data/repository/                     # ✅ Good structure
│   ├── AuthRepository.kt
│   ├── ChatRepository.kt
│   ├── TaskRepository.kt
│   ├── UserRepository.kt
│   └── VoiceRepository.kt
├── database/                            # ✅ Good structure
│   └── database.kt
├── fcm/                                 # ✅ Good structure
│   └── KosmosFCMService.kt
├── models/                              # ✅ Good structure
│   └── models.kt
└── services/                            # ✅ Good structure
    ├── ActionDetectionWorkerService.kt
    └── TranscriptionWorkerService.kt
```

## 🎯 **Proposed Domain-Driven Structure**

```
com.example.kosmos/
├── core/                                # 🆕 Core application components
│   ├── di/                              # Dependency Injection
│   │   ├── DatabaseModule.kt
│   │   ├── NetworkModule.kt
│   │   ├── RepositoryModule.kt
│   │   └── ServiceModule.kt
│   ├── database/                        # Database setup
│   │   ├── KosmosDatabase.kt
│   │   ├── Converters.kt
│   │   └── dao/
│   │       ├── UserDao.kt
│   │       ├── ChatDao.kt
│   │       ├── MessageDao.kt
│   │       ├── TaskDao.kt
│   │       └── VoiceMessageDao.kt
│   ├── models/                          # Domain models
│   │   ├── User.kt
│   │   ├── ChatRoom.kt
│   │   ├── Message.kt
│   │   ├── Task.kt
│   │   ├── VoiceMessage.kt
│   │   └── ActionItem.kt
│   └── utils/                           # Utility classes
│       ├── Constants.kt
│       └── Extensions.kt
├── features/                            # 🆕 Feature-based modules
│   ├── auth/                            # Authentication domain
│   │   ├── data/
│   │   │   └── AuthRepository.kt
│   │   ├── domain/
│   │   │   └── AuthUseCase.kt
│   │   ├── presentation/
│   │   │   ├── AuthViewModel.kt
│   │   │   ├── LoginScreen.kt
│   │   │   └── SignUpScreen.kt
│   │   └── AuthModule.kt
│   ├── chat/                            # Chat & Messaging domain
│   │   ├── data/
│   │   │   ├── ChatRepository.kt
│   │   │   └── MessageRepository.kt
│   │   ├── domain/
│   │   │   ├── ChatUseCase.kt
│   │   │   └── MessageUseCase.kt
│   │   ├── presentation/
│   │   │   ├── ChatViewModel.kt
│   │   │   ├── ChatListScreen.kt
│   │   │   ├── ChatScreen.kt
│   │   │   └── components/
│   │   │       ├── MessageItem.kt
│   │   │       ├── MessageInput.kt
│   │   │       └── ChatHeader.kt
│   │   └── ChatModule.kt
│   ├── voice/                           # Voice & Audio domain
│   │   ├── data/
│   │   │   └── VoiceRepository.kt
│   │   ├── domain/
│   │   │   └── VoiceUseCase.kt
│   │   ├── presentation/
│   │   │   └── VoiceRecordingViewModel.kt
│   │   ├── service/
│   │   │   ├── VoiceRecordingService.kt
│   │   │   ├── TranscriptionService.kt
│   │   │   └── SpeechRecognitionHelper.kt
│   │   └── VoiceModule.kt
│   ├── tasks/                           # Task Management domain
│   │   ├── data/
│   │   │   └── TaskRepository.kt
│   │   ├── domain/
│   │   │   └── TaskUseCase.kt
│   │   ├── presentation/
│   │   │   ├── TaskViewModel.kt
│   │   │   ├── TaskBoardScreen.kt
│   │   │   ├── TaskDetailScreen.kt
│   │   │   └── components/
│   │   │       ├── TaskCard.kt
│   │   │       ├── CreateTaskDialog.kt
│   │   │       └── TaskStatusChip.kt
│   │   └── TaskModule.kt
│   ├── profile/                         # User Profile domain
│   │   ├── data/
│   │   │   └── UserRepository.kt
│   │   ├── domain/
│   │   │   └── UserUseCase.kt
│   │   ├── presentation/
│   │   │   ├── ProfileViewModel.kt
│   │   │   ├── ProfileScreen.kt
│   │   │   └── SettingsScreen.kt
│   │   └── ProfileModule.kt
│   └── search/                          # Search functionality domain
│       ├── data/
│       │   └── SearchRepository.kt
│       ├── domain/
│       │   └── SearchUseCase.kt
│       ├── presentation/
│       │   ├── SearchViewModel.kt
│       │   └── UserSearchDialog.kt
│       └── SearchModule.kt
├── shared/                              # 🆕 Shared components
│   ├── ui/                              # Reusable UI components
│   │   ├── components/
│   │   │   ├── LoadingButton.kt
│   │   │   ├── ErrorDialog.kt
│   │   │   └── ConfirmationDialog.kt
│   │   ├── theme/
│   │   │   ├── Theme.kt
│   │   │   ├── Color.kt
│   │   │   └── Typography.kt
│   │   └── navigation/
│   │       ├── Screen.kt
│   │       └── Navigation.kt
│   ├── services/                        # Background services
│   │   ├── fcm/
│   │   │   └── KosmosFCMService.kt
│   │   ├── workers/
│   │   │   ├── TranscriptionWorker.kt
│   │   │   └── ActionDetectionWorker.kt
│   │   └── notification/
│   │       └── NotificationManager.kt
│   └── network/                         # Network layer
│       ├── ApiService.kt
│       └── NetworkModule.kt
└── MainActivity.kt                      # Main entry point
```

## 🔧 **Implementation Plan**

### **Phase 1: Create Directory Structure**
1. Create new domain-based folders
2. Move existing files to appropriate domains
3. Split large files into smaller, focused components

### **Phase 2: Fix Core Issues**
1. **Search Functionality**: Create dedicated SearchUseCase and proper UI integration
2. **Voice Recording**: Implement robust MediaRecorder with proper lifecycle
3. **Task Management**: Add complete CRUD operations with status management

### **Phase 3: Improve Architecture**
1. Split DI modules by domain
2. Create Use Cases for business logic
3. Separate UI components into reusable pieces

### **Phase 4: Documentation & Testing**
1. Create comprehensive module documentation
2. Add unit tests for critical functionality
3. Integration testing for end-to-end flows

## 📋 **Next Steps**

1. ✅ **Create this documentation**
2. 🔄 **Start reorganization process**
3. 🔄 **Fix functionality issues**
4. 🔄 **Test thoroughly**
5. 🔄 **Create module documentation**