# Cleanup and Refactor Plan

**Date:** January 23, 2026
**Code Quality Grade:** B (85/100)

---

## Executive Summary

The Kosmos codebase is **generally clean** but has:
- 33 dead code files (~8.5MB to remove)
- 91 TODO comments (some critical)
- ProjectViewModel too large (1142 LOC)
- Code duplication in validation logic
- Missing form validation

**Time to Clean:** 20 hours (3-4 days)

---

## 1. Dead Code Removal

### Archive Folders (Safe to Delete)

```bash
# 33 files, ~8.5MB
rm -rf archive/legacy_ui/
rm -rf archive/legacy_ui_pre_react_2026-01-11/
rm -rf archive/old_summaries_2026-01-16/
rm -rf extras/duplicates/
```

### Recently Deleted Files (git status shows as deleted)

Already cleaned up in latest commit:
- `EnhancedChatListScreenWrapper.kt`
- `EnhancedChatScreenWrapper.kt`
- `EditProfileScreen.kt`
- `NotificationSettingsScreen.kt`
- `PrivacySettingsScreen.kt`
- `ProfileScreen.kt`
- Several `*Wrapper.kt` files

**Fix Time:** 5 minutes
**Impact:** Reduces codebase size by ~8MB

---

## 2. TODO Comments Audit

### Critical TODOs (Must Address)

**TaskRepository.kt:**
```kotlin
// TODO: Implement proper sync queue
// TODO: Add conflict resolution
// TODO: Handle offline activity tracking
```

**UserRepository.kt:**
```kotlin
// TODO: Implement photo upload to Supabase Storage
// TODO: Add profile validation
```

**ProjectViewModel.kt:**
```kotlin
// TODO: Split into multiple ViewModels (too large!)
// TODO: Add pagination for large project lists
```

**Total Critical TODOs:** 15

### Non-Critical TODOs

- UI polish (30 TODOs)
- Performance optimization (20 TODOs)
- Feature enhancements (26 TODOs)

**Fix Time:** 20 hours (address critical TODOs)

---

## 3. Large File Refactoring

### ProjectViewModel (1142 LOC - Too Large!)

**Current:**
```kotlin
@HiltViewModel
class ProjectViewModel @Inject constructor(...) : ViewModel() {
    // Project CRUD (300 LOC)
    // Member management (200 LOC)
    // Task management (300 LOC)
    // Stats calculation (150 LOC)
    // Real-time updates (192 LOC)
    // Total: 1142 LOC ❌
}
```

**Recommended Split:**
```kotlin
// 1. ProjectListViewModel (350 LOC)
@HiltViewModel
class ProjectListViewModel @Inject constructor(...) : ViewModel() {
    // Project listing
    // Project creation
    // Project search/filter
}

// 2. ProjectDetailViewModel (400 LOC)
@HiltViewModel
class ProjectDetailViewModel @Inject constructor(...) : ViewModel() {
    // Project details
    // Stats calculation
    // Project updates
}

// 3. ProjectMemberViewModel (350 LOC) - Already exists as MembersListViewModel ✅
```

**Fix Time:** 8 hours
- Split into 2 ViewModels (4 hours)
- Update UI to use new ViewModels (3 hours)
- Test (1 hour)

**File:** `app/src/main/java/com/example/kosmos/features/project/presentation/ProjectViewModel.kt`

---

## 4. Code Duplication

### Validation Logic Duplicated

**Found in 5 places:**
```kotlin
// CreateProjectDialog.kt
if (name.isBlank()) {
    showError("Project name is required")
    return
}

// EditProjectDialog.kt
if (name.isBlank()) {
    showError("Project name is required")
    return
}

// QuickTaskCreationSheet.kt
if (title.isBlank()) {
    showError("Task title is required")
    return
}
```

**Fix: Create ValidationUtils**
```kotlin
object ValidationUtils {
    fun validateProjectName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Project name is required")
            name.length < 3 -> ValidationResult.Error("Name must be at least 3 characters")
            name.length > 50 -> ValidationResult.Error("Name must be less than 50 characters")
            else -> ValidationResult.Success
        }
    }

    fun validateTaskTitle(title: String): ValidationResult {
        return when {
            title.isBlank() -> ValidationResult.Error("Task title is required")
            title.length > 100 -> ValidationResult.Error("Title too long")
            else -> ValidationResult.Success
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
```

**Fix Time:** 4 hours
- Create ValidationUtils (2 hours)
- Replace duplicated validation (2 hours)

---

## 5. Missing Form Validation

### CreateProjectDialog (No Validation!)

**Current:**
```kotlin
Button(onClick = {
    viewModel.createProject(
        name = projectName,  // ❌ No validation
        description = projectDescription
    )
})
```

**Fix:**
```kotlin
Button(onClick = {
    val nameValidation = ValidationUtils.validateProjectName(projectName)
    val descValidation = ValidationUtils.validateDescription(projectDescription)

    if (nameValidation is ValidationResult.Success &&
        descValidation is ValidationResult.Success) {
        viewModel.createProject(name = projectName, description = projectDescription)
    } else {
        showErrors(listOf(nameValidation, descValidation))
    }
})
```

**Forms Missing Validation:**
1. CreateProjectDialog ❌
2. QuickTaskCreationSheet ❌
3. CreateChatDialog ⚠️ Partial
4. InviteMembersScreen ✅ Has validation

**Fix Time:** 4 hours

---

## 6. Error Message Mapping

### Generic Errors Shown to Users

**Current:**
```kotlin
// AuthViewModel.kt
val result = authRepository.signIn(email, password)
if (result.isFailure) {
    _errorMessage.value = result.exceptionOrNull()?.message
    // Shows: "HTTP 500: Internal Server Error" ❌
}
```

**Fix: Create ErrorMapper**
```kotlin
object ErrorMapper {
    fun mapToUserFriendly(exception: Throwable): String {
        return when (exception) {
            is HttpException -> when (exception.code) {
                401 -> "Invalid email or password"
                403 -> "Access denied. Please check your permissions."
                404 -> "Resource not found"
                500 -> "Server error. Please try again later."
                else -> "Network error. Please check your connection."
            }
            is IOException -> "Connection error. Please check your internet connection."
            is CancellationException -> "Operation cancelled"
            else -> "An unexpected error occurred. Please try again."
        }
    }
}

// Usage:
_errorMessage.value = ErrorMapper.mapToUserFriendly(result.exceptionOrNull()!!)
```

**Fix Time:** 6 hours
- Create ErrorMapper (2 hours)
- Update all ViewModels (3 hours)
- Test error scenarios (1 hour)

---

## 7. Missing Loading States

### No Loading Indicators in Some Screens

**MyTasksScreen:**
```kotlin
// Current: Tasks appear instantly or not at all
val tasks by viewModel.tasks.collectAsState()

LazyColumn {
    items(tasks) { task ->
        TaskCard(task)
    }
}
```

**Fix:**
```kotlin
// ✅ Add loading state
val tasks by viewModel.tasks.collectAsState()
val isLoading by viewModel.isLoading.collectAsState()

if (isLoading) {
    LoadingIndicator()  // ✅ Show loading
} else if (tasks.isEmpty()) {
    EmptyState("No tasks yet")  // ✅ Show empty state
} else {
    LazyColumn {
        items(tasks) { task ->
            TaskCard(task)
        }
    }
}
```

**Screens Missing Loading States:**
1. MyTasksScreen ❌
2. ProjectListScreen ⚠️ Partial
3. ChatListScreen ⚠️ Partial
4. UserSearchScreen ❌

**Fix Time:** 4 hours

---

## 8. Missing Empty States

### Empty Lists Show Blank Screen

**Fix: Add EmptyState Components**
```kotlin
@Composable
fun EmptyTasksState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = ColorTokens.textSecondary
        )
        Spacer(modifier = Modifier.height(Tokens.space3))
        Text(
            text = "No tasks yet",
            style = TypographyTokens.heading3,
            color = ColorTokens.textPrimary
        )
        Spacer(modifier = Modifier.height(Tokens.space2))
        Text(
            text = "Create your first task to get started",
            style = TypographyTokens.body2,
            color = ColorTokens.textSecondary
        )
    }
}
```

**Fix Time:** 3 hours

---

## 9. Hardcoded Strings (i18n Blocked)

### All Strings Hardcoded

**Current:**
```kotlin
Text("Create Project")
Text("Task Status")
Text("Due Date")
```

**Fix: Use strings.xml**
```xml
<!-- res/values/strings.xml -->
<string name="create_project">Create Project</string>
<string name="task_status">Task Status</string>
<string name="due_date">Due Date</string>
```

```kotlin
Text(stringResource(R.string.create_project))
```

**Impact:**
- Cannot add internationalization (i18n)
- Cannot change strings without code changes

**Fix Time:** 12 hours (low priority - post-launch)

---

## 10. N+1 Query in ProjectViewModel

### Loading Members One-by-One

**Current:**
```kotlin
fun loadProjectWithMembers(projectId: String) {
    viewModelScope.launch {
        val project = projectRepository.getProject(projectId)  // 1 query

        val members = project.memberIds.map { memberId ->
            userRepository.getUser(memberId)  // N queries! ❌
        }
    }
}
```

**Fix:**
```kotlin
// Add batch query to UserRepository
suspend fun getUsers(userIds: List<String>): List<User> {
    return userDao.getUsersByIds(userIds)  // 1 query ✅
}

// Use it:
fun loadProjectWithMembers(projectId: String) {
    viewModelScope.launch {
        val project = projectRepository.getProject(projectId)
        val members = userRepository.getUsers(project.memberIds)  // 1 query ✅
    }
}
```

**Fix Time:** 2 hours

---

## Refactoring Priority

### P0 - Before Launch (12 hours)

1. ✅ Add form validation (4 hours)
2. ✅ Add error message mapping (6 hours)
3. ✅ Add loading indicators (4 hours)
4. ❌ Delete dead code (5 minutes)

### P1 - Week 2 (16 hours)

5. Split ProjectViewModel (8 hours)
6. Create ValidationUtils (4 hours)
7. Fix N+1 query (2 hours)
8. Add empty states (3 hours)
9. Address critical TODOs (20 hours) - Covered in other docs

### P2 - Post-Launch (12+ hours)

10. Externalize strings (12 hours)
11. Address non-critical TODOs (20+ hours)

---

## Conclusion

**Code Quality Grade: B (85/100)**

**Strengths:**
- ✅ Clean architecture (MVVM)
- ✅ Consistent code style
- ✅ Good naming conventions
- ✅ Proper separation of concerns

**Issues:**
- ⚠️ Dead code (8.5MB)
- ⚠️ Large ViewModel (1142 LOC)
- ⚠️ Missing validation
- ⚠️ Generic error messages
- ⚠️ Code duplication

**Time to Clean:** 40 hours (P0 + P1)

---

**Next:** Read `10-production-readiness-verdict.md` for final assessment and roadmap.
