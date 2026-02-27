# Production Audit Fixes — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Close every gap found in the full production audit: fix silent data-loss bugs, wire broken feature paths, remove dead code, and enforce pipeline consistency across all modules.

**Architecture:** Offline-first MVVM. All operations follow UI → ViewModel → Repository → (Room then Supabase). No backend logic in Composables. Wrappers own ViewModel injection; pure Composables own zero state.

**Tech Stack:** Kotlin + Jetpack Compose + Hilt + Room v11 + supabase-kt 3.2.5 (auth-kt, postgrest-kt, storage-kt, realtime-kt) + Firebase Auth

---

## Phase A — P0: Silent Data-Loss Bugs

---

### Task 1: Wire Photo Upload to Supabase Storage (EditProfile + GoogleProfileSetup)

**Context:**
`AuthViewModel.updateProfile()` already receives `photoUri: Uri?` but has a `// TODO` comment at line 443. `supabase-storage-kt` is already declared in `libs.versions.toml` and is available via the injected `supabase: SupabaseClient` in `AuthRepository`. The bucket name to use is `"avatars"` (create if it doesn't exist via Supabase dashboard).

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/data/repository/AuthRepository.kt`
- Modify: `app/src/main/java/com/example/kosmos/features/auth/presentation/AuthViewModel.kt` (lines ~440–448)
- Modify: `app/src/main/java/com/example/kosmos/features/auth/presentation/redesign/GoogleProfileSetupScreen.kt`

**Step 1: Add `uploadProfilePhoto` to `AuthRepository`**

Open `AuthRepository.kt`. Find the class body. Add this method (near the other `private suspend` helpers):

```kotlin
/**
 * Uploads a profile photo to Supabase Storage and returns the public URL.
 * Bucket: "avatars" — create it in Supabase dashboard with public access.
 * Path: "public/{userId}.jpg" — overwrite on re-upload.
 */
suspend fun uploadProfilePhoto(userId: String, photoUri: Uri, context: Context): Result<String> {
    return try {
        val contentResolver = context.contentResolver
        val bytes = contentResolver.openInputStream(photoUri)?.use { it.readBytes() }
            ?: return Result.failure(Exception("Cannot open photo URI"))
        val path = "public/$userId.jpg"
        supabase.storage.from("avatars").upload(path, bytes) {
            upsert = true
        }
        val publicUrl = supabase.storage.from("avatars").publicUrl(path)
        Result.success(publicUrl)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e("AuthRepository", "Photo upload failed", e)
        Result.failure(e)
    }
}
```

Note: `AuthRepository` already has `supabase: SupabaseClient` injected (check constructor). If `Context` is not already injected, add `@ApplicationContext context: Context` to the constructor and register in `Module.kt` `RepositoryModule.provideAuthRepository(...)`.

**Step 2: Call upload in `AuthViewModel.updateProfile()`**

Replace the TODO block (lines ~441–448) with:

```kotlin
// Upload photo if provided
var photoUrl: String? = currentUser.photoUrl
if (photoUri != null) {
    val uploadResult = authRepository.uploadProfilePhoto(currentUser.id, photoUri, context)
    photoUrl = uploadResult.getOrNull() ?: currentUser.photoUrl
    if (uploadResult.isFailure) {
        _uiState.value = _uiState.value.copy(
            error = "Profile saved but photo upload failed: ${uploadResult.exceptionOrNull()?.message}"
        )
    }
}
```

`AuthViewModel` already has `@ApplicationContext context: Context` injected via Hilt (check constructor; if not, add it and update `Module.kt` if `AuthViewModel` is manually provided — but since it's `@HiltViewModel` this is automatic).

**Step 3: Verify `GoogleProfileSetupScreen` also calls `authViewModel.updateProfile` with `photoUri`**

Open `GoogleProfileSetupScreen.kt`. Find where the save/next action calls are made. Verify `photoUri` is threaded through. If the screen has a local `selectedPhotoUri: Uri?` state variable, ensure it's passed to the save call. No new code needed if it already passes `photoUri` — the fix is entirely in Step 1–2.

**Step 4: Build and test**

```bash
./gradlew compileDebugKotlin
```
Expected: Clean compile, no errors.

Manual test: Open app → More → Edit Profile → tap photo → select image → Save. Verify the new photo appears on ProfileScreen after save.

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/kosmos/data/repository/AuthRepository.kt \
        app/src/main/java/com/example/kosmos/features/auth/presentation/AuthViewModel.kt
git commit -m "feat: wire profile photo upload to Supabase Storage avatars bucket"
```

---

### Task 2: Persist Subtask Checkbox State

**Context:**
`TaskRepository.getSubtasksFlow(parentTaskId)` already exists. Subtasks are real `Task` objects with `parentTaskId` set. When the user taps a subtask checkbox in `TaskDetailScreenReact`, it updates a local `remember` state variable only — it never calls the repository.

The fix: add `toggleSubtaskStatus(subtaskId, currentStatus)` to `TaskDetailViewModel`, then wire it from `TaskDetailScreenReactWrapper` down to `TaskDetailScreenReact`.

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/tasks/presentation/TaskDetailViewModel.kt`
- Modify: `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskDetailScreenReactWrapper.kt`
- Modify: `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskDetailScreenReact.kt`

**Step 1: Add method to `TaskDetailViewModel`**

```kotlin
fun toggleSubtaskStatus(subtaskId: String, currentStatus: TaskStatus) {
    val actorId = currentUser?.id ?: return
    viewModelScope.launch {
        val newStatus = if (currentStatus == TaskStatus.DONE) TaskStatus.TODO else TaskStatus.DONE
        taskRepository.updateTaskStatus(subtaskId, newStatus, actorId)
            .onFailure { e ->
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = "Failed to update subtask: ${e.message}") }
            }
    }
}
```

`taskRepository` and `currentUser` are already available in `TaskDetailViewModel` — verify field names from the existing ViewModel body.

**Step 2: Expose callback from `TaskDetailScreenReactWrapper`**

In `TaskDetailScreenReactWrapper`, find where `TaskDetailScreenReact(...)` is called. Add:

```kotlin
onToggleSubtask = { subtaskId, currentStatus ->
    viewModel.toggleSubtaskStatus(subtaskId, currentStatus)
},
```

**Step 3: Update `TaskDetailScreenReact` signature**

Find the `@Composable fun TaskDetailScreenReact(...)` function signature in `TaskDetailScreenReact.kt`. Add:

```kotlin
onToggleSubtask: (subtaskId: String, currentStatus: TaskStatus) -> Unit = { _, _ -> },
```

Then find the subtask checkbox section (search for `Checkbox` or `subtask` in the file). Replace the local state update:

```kotlin
// BEFORE (local state only):
var checked by remember { mutableStateOf(subtask.status == TaskStatus.DONE) }
Checkbox(checked = checked, onCheckedChange = { checked = it })

// AFTER (persisted):
Checkbox(
    checked = subtask.status == TaskStatus.DONE,
    onCheckedChange = { onToggleSubtask(subtask.id, subtask.status) }
)
```

The subtask list must come from real data (Flow from `TaskDetailViewModel.subtasks`) not a local list. Verify that the wrapper already collects a `subtasks: List<Task>` from `TaskDetailViewModel` and passes it down. If not, add:

```kotlin
// In TaskDetailViewModel:
val subtasks: StateFlow<List<Task>> = taskRepository
    .getSubtasksFlow(taskId) // taskId collected from uiState
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

**Step 4: Build and verify**

```bash
./gradlew compileDebugKotlin
```

Manual test: Open a task that has subtasks → toggle a checkbox → navigate back → re-open task → verify checkbox state is preserved.

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/kosmos/features/tasks/presentation/TaskDetailViewModel.kt \
        app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskDetailScreenReactWrapper.kt \
        app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskDetailScreenReact.kt
git commit -m "fix: persist subtask checkbox state via TaskDetailViewModel.toggleSubtaskStatus"
```

---

## Phase B — P1: High-Value Fixes

---

### Task 3: Apply Project Color to Card UI

**Context:**
`Project.color: String?` stores a hex string like `"#7C3AED"`. It is persisted to Supabase. It is never read back into any UI element. The fix is to parse it and apply it as a left-edge accent border on project cards in `ProjectListScreenReact`.

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/projects/presentation/redesign/ProjectListScreenReact.kt`
- Modify: `app/src/main/java/com/example/kosmos/features/projects/presentation/redesign/ProjectListScreenReactWrapper.kt`

**Step 1: Add a helper to parse hex color safely**

In `ProjectListScreenReact.kt`, add at file level (outside any composable):

```kotlin
private fun String?.toComposeColor(fallback: Color = Color(0xFF7C3AED)): Color {
    if (this == null) return fallback
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: IllegalArgumentException) {
        fallback
    }
}
```

**Step 2: Add `accentColor` to `ProjectCardData`**

`ProjectCardData` is a local data class at the top of `ProjectListScreenReact.kt`. Add:

```kotlin
data class ProjectCardData(
    // ... existing fields ...
    val accentColor: String? = null   // hex string from Project.color
)
```

**Step 3: Populate `accentColor` in the Wrapper**

In `ProjectListScreenReactWrapper.kt`, find where `Project` domain objects are mapped to `ProjectCardData`. Add:

```kotlin
accentColor = project.color,
```

**Step 4: Apply accent in project card composable**

Find the project card composable (look for `Card {` or `Surface {` inside the project list item). Add a left border using `drawBehind` or a `Box` overlay:

```kotlin
val accentColor = cardData.accentColor.toComposeColor()

Card(
    modifier = modifier.drawBehind {
        drawRect(
            color = accentColor,
            topLeft = Offset.Zero,
            size = Size(4.dp.toPx(), size.height)
        )
    },
    // ... rest of Card params
) { ... }
```

Import: `import androidx.compose.ui.draw.drawBehind`, `import androidx.compose.ui.geometry.Offset`, `import androidx.compose.ui.geometry.Size`.

**Step 5: Build**

```bash
./gradlew compileDebugKotlin
```

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/kosmos/features/projects/presentation/redesign/ProjectListScreenReact.kt \
        app/src/main/java/com/example/kosmos/features/projects/presentation/redesign/ProjectListScreenReactWrapper.kt
git commit -m "feat: apply project.color as left accent border on project cards"
```

---

### Task 4: Remove Dead Code

**Context:**
Three categories of dead code to remove:
1. `CreateProjectDialog.kt` — standalone file, no usages
2. `ProjectViewModel.createProject(name, description)` simple overload — the wizard uses `createProjectWithWizardData()`
3. `TaskViewModel.syncAllUserTasks()` annotated `@Deprecated`
4. `TaskViewModel.filterTasksByStatus()` and `toggleMyTasksFilter()` — defined but the UI uses its own local state; these are orphaned ViewModel methods

**Files:**
- Delete: `app/src/main/java/com/example/kosmos/features/projects/components/CreateProjectDialog.kt`
- Modify: `app/src/main/java/com/example/kosmos/features/project/presentation/ProjectViewModel.kt` (remove lines ~202–239)
- Modify: `app/src/main/java/com/example/kosmos/features/tasks/presentation/TaskViewModel.kt` (remove deprecated method ~894–908, remove orphaned filter methods ~636–642)

**Step 1: Verify no usages before deletion**

```bash
grep -r "CreateProjectDialog" app/src/main/java/ --include="*.kt"
grep -r "createProject(" app/src/main/java/ --include="*.kt" | grep -v "createProjectWithWizardData"
grep -r "syncAllUserTasks\|filterTasksByStatus\|toggleMyTasksFilter" app/src/main/java/ --include="*.kt"
```

Expected: all grep results show only the definition site, not any call sites. If any call site exists, do NOT delete — file a note instead.

**Step 2: Delete the dead dialog file**

```bash
rm app/src/main/java/com/example/kosmos/features/projects/components/CreateProjectDialog.kt
```

**Step 3: Remove simple `createProject` overload from `ProjectViewModel`**

Open `ProjectViewModel.kt`. Delete the method body at lines ~202–239 (the one that takes `name: String, description: String` directly). Keep `createProjectWithWizardData()`.

**Step 4: Remove deprecated/orphaned methods from `TaskViewModel`**

Open `TaskViewModel.kt`. Delete:
- `syncAllUserTasks()` block at lines ~894–908
- `filterTasksByStatus(status: TaskStatus?)` block at lines ~636–638
- `toggleMyTasksFilter()` block at lines ~640–642

Also remove any `@Deprecated` annotations and `filterStatus` / `showMyTasksOnly` state fields from `TaskUiState` **only if** they are not used anywhere else in the UI (verify first with grep).

**Step 5: Build**

```bash
./gradlew compileDebugKotlin
```
Expected: Zero compile errors. If errors appear, a call site exists — add back the method and add a `// TODO: remove caller first` comment instead.

**Step 6: Commit**

```bash
git add -A
git commit -m "chore: remove dead code (CreateProjectDialog, simple createProject, deprecated sync, orphaned filter methods)"
```

---

### Task 5: Debounce Settings Saves

**Context:**
Both `NotificationSettingsViewModel` and `PrivacySettingsViewModel` call `saveSettings()` (a Supabase write) immediately on every toggle. Rapid toggling causes write amplification. Fix: collect toggle events in a `MutableSharedFlow`, debounce 800ms, then save.

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/profile/presentation/NotificationSettingsViewModel.kt`
- Modify: `app/src/main/java/com/example/kosmos/features/profile/presentation/PrivacySettingsViewModel.kt`

**Step 1: Add debounce save to `NotificationSettingsViewModel`**

In the ViewModel class body, add a save trigger and debounce init:

```kotlin
// At top of class, after _uiState declaration:
private val _saveTrigger = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)

init {
    // existing init block content...

    // Debounced save: waits 800ms after last toggle before writing to Supabase
    viewModelScope.launch {
        _saveTrigger
            .debounce(800)
            .collect { saveSettings() }
    }
}
```

**Step 2: Replace direct `saveSettings()` call in every toggle function**

Every toggle function currently ends with:
```kotlin
viewModelScope.launch { saveSettings() }
```

Replace each with:
```kotlin
_saveTrigger.tryEmit(Unit)
```

`saveSettings()` remains as-is — it reads the current `_uiState.value` and persists it.

**Step 3: Repeat for `PrivacySettingsViewModel`**

Identical pattern. Add `_saveTrigger`, debounce in `init`, replace all `viewModelScope.launch { saveSettings() }` with `_saveTrigger.tryEmit(Unit)`.

Import needed: `import kotlinx.coroutines.flow.debounce`, `import kotlinx.coroutines.flow.MutableSharedFlow`.

**Step 4: Build**

```bash
./gradlew compileDebugKotlin
```

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/kosmos/features/profile/presentation/NotificationSettingsViewModel.kt \
        app/src/main/java/com/example/kosmos/features/profile/presentation/PrivacySettingsViewModel.kt
git commit -m "perf: debounce settings saves to 800ms to avoid Supabase write amplification"
```

---

### Task 6: Fix Project Card "Settings" Dead Menu Item

**Context:**
`ProjectListScreenReact.kt` line 451 renders a "Settings" card menu item that calls `onProjectSettings(project.id)`. The `ProjectListScreenReactWrapper` never passes this callback — it defaults to `{}`. Two options: implement project settings navigation, or remove the menu item if no settings screen is planned.

**Decision:** Remove the menu item (no separate "Project Settings" screen exists; the `EditProjectScreenReact` already covers all editable project fields including danger zone). The wrapper already provides `onEditProject` navigation.

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/projects/presentation/redesign/ProjectListScreenReact.kt`

**Step 1: Find and remove the Settings menu item**

Search for `onProjectSettings` in `ProjectListScreenReact.kt`. Find the `DropdownMenuItem` or menu item composable that calls it. Delete that item. Also remove `onProjectSettings: (String) -> Unit` from the function signature (and its default value `= {}`).

**Step 2: Remove from Wrapper too**

Search `ProjectListScreenReactWrapper.kt` for `onProjectSettings`. If it exists as a parameter being passed (even as `{}`), remove it.

**Step 3: Build**

```bash
./gradlew compileDebugKotlin
```

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/kosmos/features/projects/presentation/redesign/ProjectListScreenReact.kt \
        app/src/main/java/com/example/kosmos/features/projects/presentation/redesign/ProjectListScreenReactWrapper.kt
git commit -m "fix: remove dead 'Settings' menu item from project card (no route exists)"
```

---

### Task 7: Hide Voice Recording Button

**Context:**
The voice recording button in `ChatRoomScreenReact` calls `ChatViewModel.startVoiceRecording()` which immediately returns an error. The full audio pipeline (recording → Supabase Storage upload → transcription) is not implemented. The button is visible to all users and provides a broken affordance.

Fix: hide the button behind a compile-time flag. When `BuildConfig.VOICE_ENABLED` is false, the button does not render.

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/example/kosmos/features/chat/presentation/redesign/ChatRoomScreenReact.kt`

**Step 1: Add build config field**

In `app/build.gradle.kts`, inside `android { defaultConfig { ... } }`:

```kotlin
buildConfigField("Boolean", "VOICE_ENABLED", "false")
```

**Step 2: Conditionally render voice button**

In `ChatRoomScreenReact.kt`, find the voice recording `IconButton`. Wrap it:

```kotlin
if (BuildConfig.VOICE_ENABLED) {
    IconButton(onClick = onVoiceRecord) {
        Icon(/* existing icon */)
    }
}
```

Import: `import com.example.kosmos.BuildConfig`

**Step 3: Build**

```bash
./gradlew compileDebugKotlin
```

**Step 4: Commit**

```bash
git add app/build.gradle.kts \
        app/src/main/java/com/example/kosmos/features/chat/presentation/redesign/ChatRoomScreenReact.kt
git commit -m "fix: hide voice recording button behind VOICE_ENABLED build flag (feature incomplete)"
```

---

### Task 8: Show `industryTags` in Project Details

**Context:**
`Project.industryTags: String?` (a JSON array stored as string) is editable in `EditProjectScreenReact` for the BUSINESS category but is never displayed in `ProjectDetailsScreenReact`. This creates a round-trip where the user sets it and sees nothing.

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/projects/presentation/redesign/ProjectDetailsScreenReact.kt`

**Step 1: Find where other category-specific fields are displayed**

In `ProjectDetailsScreenReact.kt`, search for `websiteUrl` or `githubUrl` — these are already displayed conditionally. Add a similar block for `industryTags`:

```kotlin
// After the existing businessModel display block:
project.industryTags?.takeIf { it.isNotBlank() }?.let { rawTags ->
    val tags = try {
        // industryTags stored as JSON array string: ["Finance","SaaS"]
        rawTags.trim('[', ']').split(",")
            .map { it.trim().trim('"') }
            .filter { it.isNotBlank() }
    } catch (e: Exception) { listOf(rawTags) }

    if (tags.isNotEmpty()) {
        ProjectDetailRow(
            label = "Industry",
            content = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
                    tags.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag, style = TypographyTokens.labelSmall) }
                        )
                    }
                }
            }
        )
    }
}
```

Only show for BUSINESS category: wrap the block with `if (project.category == ProjectCategory.BUSINESS)`.

**Step 2: Build**

```bash
./gradlew compileDebugKotlin
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/kosmos/features/projects/presentation/redesign/ProjectDetailsScreenReact.kt
git commit -m "fix: display industryTags in ProjectDetails for BUSINESS category projects"
```

---

## Phase C — P2: Pipeline & UX Improvements

---

### Task 9: Consolidate Comment Handling to `TaskDetailViewModel`

**Context:**
`TaskViewModel` defines `addComment()`, `editComment()`, `deleteComment()` (lines ~697–742). `TaskDetailScreenReactWrapper` uses `TaskDetailViewModel` for all comment operations instead. The methods in `TaskViewModel` are never called from any UI. Remove them from `TaskViewModel` to eliminate the dual-path confusion.

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/tasks/presentation/TaskViewModel.kt`

**Step 1: Grep for callers**

```bash
grep -rn "taskViewModel\.addComment\|taskViewModel\.editComment\|taskViewModel\.deleteComment" app/src/ --include="*.kt"
```

Expected: zero results.

**Step 2: Remove the dead methods**

Delete `addComment()`, `editComment()`, `deleteComment()` from `TaskViewModel`. Also remove any UI state properties in `TaskUiState` that are used exclusively by these methods (check `currentComment`, `editingCommentId`, etc.).

**Step 3: Build**

```bash
./gradlew compileDebugKotlin
```

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/kosmos/features/tasks/presentation/TaskViewModel.kt
git commit -m "refactor: remove duplicate comment methods from TaskViewModel (canonical: TaskDetailViewModel)"
```

---

### Task 10: Wire Task Status Filter to `TaskViewModel` (or Remove Orphaned Methods)

**Context:**
`MyTasksScreenReactWrapper` maintains its own local `selectedStatus` filter state and filters `uiState.tasks` in the wrapper. `TaskViewModel.filterTasksByStatus()` exists but is never called. Two options:
- **Option A (recommended):** Remove the orphaned ViewModel methods (done in Task 4 above). The local filter in the wrapper is a valid pattern for UI-only state.
- **Option B:** Move filter state into ViewModel for testability.

This task documents that Option A is the correct approach (UI-only filter state belongs in the composable scope, not the ViewModel, when no persistence is needed). No code change needed if Task 4 already removed these methods.

**Verification:**

```bash
./gradlew compileDebugKotlin
```

No new action required if Task 4 is complete.

---

### Task 11: Add Character Count to Text Fields (Description, Bio)

**Context:**
Long text fields (project description, task description, user bio) have no character limit indicator. Users can type indefinitely. Add a character count below each field showing `current/max`.

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/projects/components/ProjectCreationWizard.kt` (description field)
- Modify: `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskEditScreenReact.kt` (description field)
- Modify: `app/src/main/java/com/example/kosmos/features/profile/presentation/redesign/EditProfileScreen.kt` (bio field)

**Step 1: Add helper composable**

Add this reusable composable. Best placed in `shared/ui/components/`:

```kotlin
// File: app/src/main/java/com/example/kosmos/shared/ui/components/CharacterCount.kt
@Composable
fun CharacterCount(current: Int, max: Int, modifier: Modifier = Modifier) {
    val isNearLimit = current > max * 0.85
    val isOverLimit = current > max
    Text(
        text = "$current / $max",
        style = TypographyTokens.labelSmall,
        color = when {
            isOverLimit -> ColorTokens.ReactTheme.error
            isNearLimit -> ColorTokens.ReactTheme.warning
            else -> ColorTokens.ReactTheme.textMuted
        },
        modifier = modifier
    )
}
```

Note: Check `ColorTokens.ReactTheme` for `error` and `warning` — add them if missing (error: `Color(0xFFEF4444)`, warning: `Color(0xFFF59E0B)`).

**Step 2: Apply to each long text field**

Pattern (same for all three files):

```kotlin
// Wrap existing OutlinedTextField + add CharacterCount below
val MAX_DESC = 500 // or 200 for bio
Column {
    OutlinedTextField(
        value = description,
        onValueChange = { if (it.length <= MAX_DESC) description = it },
        // ... existing params
    )
    CharacterCount(
        current = description.length,
        max = MAX_DESC,
        modifier = Modifier
            .align(Alignment.End)
            .padding(end = Tokens.Spacing.xs, top = 2.dp)
    )
}
```

Limits: project description = 500 chars, task description = 500 chars, bio = 200 chars.

**Step 3: Build**

```bash
./gradlew compileDebugKotlin
```

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/kosmos/shared/ui/components/CharacterCount.kt \
        app/src/main/java/com/example/kosmos/features/projects/components/ProjectCreationWizard.kt \
        app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskEditScreenReact.kt \
        app/src/main/java/com/example/kosmos/features/profile/presentation/redesign/EditProfileScreen.kt
git commit -m "feat: add character count indicators to description and bio text fields"
```

---

### Task 12: Inline URL Validation Errors in Project Wizard

**Context:**
URL fields in step 2 of `ProjectCreationWizard` show a generic error message via `errorMap["websiteUrl"]`. The error text is displayed somewhere at the bottom. Move it inline beneath each specific field.

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/projects/components/ProjectCreationWizard.kt`

**Step 1: Identify current error display pattern**

Search for `errorMap` in `ProjectCreationWizard.kt`. The map likely maps field key → error string.

**Step 2: Add inline error text beneath each URL field**

Pattern:

```kotlin
OutlinedTextField(
    value = githubUrl,
    onValueChange = { ... },
    isError = errorMap["githubUrl"] != null,
    // ...
)
errorMap["githubUrl"]?.let { errMsg ->
    Text(
        text = errMsg,
        color = ColorTokens.ReactTheme.error,
        style = TypographyTokens.labelSmall,
        modifier = Modifier.padding(start = Tokens.Spacing.sm, top = 2.dp)
    )
}
```

Apply the same pattern to all URL fields: `websiteUrl`, `githubUrl`, `openSourceLicense`, `businessModel`, `projectMotive`.

**Step 3: Remove the generic error banner at step level (if it was only for URL fields)**

If there was a generic `Text(error)` at the bottom of Step 2, remove it. Keep per-field error text only.

**Step 4: Build**

```bash
./gradlew compileDebugKotlin
```

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/kosmos/features/projects/components/ProjectCreationWizard.kt
git commit -m "ux: move URL validation errors inline beneath each field in project creation wizard"
```

---

### Task 13: Add Estimated Hours + Tags to `QuickTaskCreationSheet`

**Context:**
`QuickTaskCreationSheet` is the most common task creation path (FAB in ProjectWorkspace). It is missing: estimated hours and tags — both available in the full `TaskEditScreenReact`. Users who create tasks via the quick sheet cannot set these fields without going back to edit.

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/QuickTaskCreationSheet.kt`
- Modify: `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/QuickTaskCreationSheetWrapper.kt`

**Step 1: Add state variables to `QuickTaskCreationSheet`**

```kotlin
var estimatedHours by remember { mutableStateOf("") }
var tags by remember { mutableStateOf(listOf<String>()) }
var tagInput by remember { mutableStateOf("") }
```

**Step 2: Add estimated hours field**

After the existing due date field, add:

```kotlin
OutlinedTextField(
    value = estimatedHours,
    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) estimatedHours = it },
    label = { Text("Estimated Hours") },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
```

**Step 3: Add tag input with chip display**

```kotlin
// Tag input row
Row(verticalAlignment = Alignment.CenterVertically) {
    OutlinedTextField(
        value = tagInput,
        onValueChange = { tagInput = it },
        label = { Text("Add Tag") },
        singleLine = true,
        modifier = Modifier.weight(1f)
    )
    Spacer(Modifier.width(Tokens.Spacing.sm))
    IconButton(onClick = {
        val trimmed = tagInput.trim()
        if (trimmed.isNotEmpty() && trimmed !in tags) {
            tags = tags + trimmed
        }
        tagInput = ""
    }) {
        Icon(Icons.Default.Add, contentDescription = "Add tag")
    }
}
// Tag chips
FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
    tags.forEach { tag ->
        InputChip(
            selected = false,
            onClick = { tags = tags - tag },
            label = { Text(tag) },
            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove") }
        )
    }
}
```

**Step 4: Include in `onCreateTask` callback**

Find where the sheet calls its `onCreateTask(title, description, priority, assigneeId, dueDate)` callback. Add `estimatedHours` and `tags`:

```kotlin
onCreateTask(title, description, priority, assigneeId, dueDate, estimatedHours.toDoubleOrNull(), tags)
```

Update the callback signature in both the composable and the wrapper accordingly.

**Step 5: Wire to `TaskViewModel.createTask` in wrapper**

In `QuickTaskCreationSheetWrapper.kt`, find where `taskViewModel.createTask(...)` is called. Pass through `estimatedHours` and `tags`. The `Task` constructor already has both fields.

**Step 6: Build**

```bash
./gradlew compileDebugKotlin
```

**Step 7: Commit**

```bash
git add app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/QuickTaskCreationSheet.kt \
        app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/QuickTaskCreationSheetWrapper.kt
git commit -m "feat: add estimated hours and tags fields to QuickTaskCreationSheet"
```

---

### Task 14: Add Task Dependency Creation UI

**Context:**
`TaskRepository.getDependenciesForTaskFlow(taskId)` exists. `TaskDetailViewModel` fetches and exposes the dependency list. The `TaskDetailScreenReact` displays them but there is no UI to add a new dependency. Fix: add an "Add Dependency" chip/button in the Dependencies section that opens a `TaskPickerBottomSheet` (already exists at `features/tasks/components/TaskPickerBottomSheet.kt`).

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/tasks/presentation/TaskDetailViewModel.kt`
- Modify: `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskDetailScreenReact.kt`
- Modify: `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskDetailScreenReactWrapper.kt`

**Step 1: Add `addDependency` to `TaskDetailViewModel`**

```kotlin
fun addDependency(dependsOnTaskId: String) {
    val taskId = uiState.value.task?.id ?: return
    viewModelScope.launch {
        taskRepository.addTaskDependency(taskId, dependsOnTaskId)
            .onFailure { e ->
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = "Failed to add dependency: ${e.message}") }
            }
    }
}

fun removeDependency(dependsOnTaskId: String) {
    val taskId = uiState.value.task?.id ?: return
    viewModelScope.launch {
        taskRepository.removeTaskDependency(taskId, dependsOnTaskId)
            .onFailure { e ->
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = "Failed to remove dependency: ${e.message}") }
            }
    }
}
```

Verify `addTaskDependency` / `removeTaskDependency` exist in `TaskRepository`. If not, add them — they insert/delete from `task_dependencies` table via `supabase.from("task_dependencies")`.

**Step 2: Add "Add Dependency" button to `TaskDetailScreenReact`**

In the Dependencies section of `TaskDetailScreenReact.kt`, below the existing dependency list:

```kotlin
var showDependencyPicker by remember { mutableStateOf(false) }

// Add button
TextButton(onClick = { showDependencyPicker = true }) {
    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(4.dp))
    Text("Add Dependency")
}

// Picker dialog
if (showDependencyPicker) {
    TaskPickerBottomSheet(
        projectId = task.projectId,
        excludeTaskIds = listOf(task.id) + dependencies.map { it.id },
        onTaskSelected = { selectedTask ->
            onAddDependency(selectedTask.id)
            showDependencyPicker = false
        },
        onDismiss = { showDependencyPicker = false }
    )
}
```

Add `onAddDependency: (String) -> Unit` and `onRemoveDependency: (String) -> Unit` to the function signature.

**Step 3: Wire from Wrapper**

```kotlin
onAddDependency = { depTaskId -> viewModel.addDependency(depTaskId) },
onRemoveDependency = { depTaskId -> viewModel.removeDependency(depTaskId) },
```

**Step 4: Build**

```bash
./gradlew compileDebugKotlin
```

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/kosmos/features/tasks/presentation/TaskDetailViewModel.kt \
        app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskDetailScreenReact.kt \
        app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskDetailScreenReactWrapper.kt
git commit -m "feat: add task dependency creation UI via TaskPickerBottomSheet in TaskDetail"
```

---

### Task 15: Hide Blocked Users Section (Privacy Settings)

**Context:**
`PrivacySettingsScreen` shows an "Blocked Users" section that always renders an empty list. The ViewModel comment reads `// TODO v1.1`. Rather than show a broken empty state, hide the section with a "Coming soon" placeholder or collapse it entirely.

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/profile/presentation/redesign/PrivacySettingsScreen.kt`

**Step 1: Wrap the blocked users section**

Find the blocked users list section in `PrivacySettingsScreen.kt`. Replace with:

```kotlin
// Blocked Users — hidden until v1.1 database schema support is added
// TODO v1.1: un-hide when BlockedUser table is implemented
```

Simply delete (or comment out) the blocked users composable block. The section header and empty list take up screen space and confuse users.

**Step 2: Build**

```bash
./gradlew compileDebugKotlin
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/kosmos/features/profile/presentation/redesign/PrivacySettingsScreen.kt
git commit -m "fix: hide blocked users list (not implemented, always shows empty state)"
```

---

### Task 16: Fix `actualHours` Dual-Path Sync

**Context:**
`Task.actualHours` can be set via:
1. Direct edit in `TaskEditScreenReact` (user types a number)
2. Accumulated from `TimeEntry` records in `TaskDetailViewModel.timeEntries`

These two paths can diverge. Fix: make `actualHours` read-only in `TaskEditScreenReact` — show it as a display field only, with a caption "Tracked automatically via time entries". The authoritative value comes from summing `TimeEntry.durationMinutes / 60.0`.

**Files:**
- Modify: `app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskEditScreenReact.kt`

**Step 1: Find the `actualHours` field in `TaskEditScreenReact`**

Search for `actualHours` or "Actual Hours" in `TaskEditScreenReact.kt`. Convert the editable `OutlinedTextField` to a read-only display:

```kotlin
// REPLACE editable field:
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text("Actual Hours", style = TypographyTokens.bodyMedium, color = ColorTokens.ReactTheme.textMuted)
    Text(
        text = if (task.actualHours != null && task.actualHours > 0) "%.1fh".format(task.actualHours) else "—",
        style = TypographyTokens.bodyMedium,
        color = ColorTokens.ReactTheme.text
    )
}
Text(
    "Tracked automatically via time entries",
    style = TypographyTokens.labelSmall,
    color = ColorTokens.ReactTheme.textMuted
)
```

**Step 2: Build**

```bash
./gradlew compileDebugKotlin
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/kosmos/features/tasks/presentation/redesign/TaskEditScreenReact.kt
git commit -m "fix: make actualHours read-only in TaskEdit (source of truth: time tracker)"
```

---

## Phase D — Final Verification

### Task 17: Full Build + Smoke Test

**Step 1: Full clean build**

```bash
./gradlew clean assembleDebug 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

**Step 2: Run unit tests**

```bash
./gradlew testDebugUnitTest 2>&1 | tail -30
```
Expected: All 112 existing tests pass. Zero new failures.

**Step 3: Manual smoke test checklist**

Run through this on a physical device or emulator:

- [ ] Edit profile → pick photo → save → photo appears on ProfileScreen
- [ ] Open task with subtasks → toggle checkbox → navigate away → return → checkbox state preserved
- [ ] Create project with color = red → project card shows red left border
- [ ] Open project card menu → verify no "Settings" item
- [ ] Chat room → verify voice button is gone
- [ ] Toggle a notification setting 5× rapidly → Supabase write fires once (verify in Supabase dashboard logs)
- [ ] Open BUSINESS project details → verify industry tags appear
- [ ] Privacy Settings → verify no blocked users section
- [ ] QuickTaskCreationSheet → verify estimated hours and tags fields present
- [ ] Task detail → Dependencies section → verify "Add Dependency" button opens task picker

**Step 4: Commit summary**

```bash
git log --oneline -20
```

---

## Phase E — Heavy Features (Planned for Later — NOT in current execution)

The following features are architecturally substantial. They are **deferred** — not part of the current execution run. Each has a stub/hidden state in the UI (see Phase B/C tasks above). When ready to implement, create a dedicated plan file per feature.

---

### [FUTURE] Feature F1: Voice Recording + Supabase Storage Upload

**Scope:** Full audio pipeline — `MediaRecorder` → temp `.m4a` file → `supabase.storage.from("voice-messages").upload(path, bytes)` → `VoiceMessage` record in DB → playback in `ChatRoomScreenReact` via `ExoPlayer`.

**Why deferred:** Requires `RECORD_AUDIO` permission flow, background service for recording state, ExoPlayer dependency, Supabase Storage bucket creation (`voice-messages`), and waveform UI component.

**Current state:** Button hidden behind `BuildConfig.VOICE_ENABLED = false` (Task 7 above). `ChatViewModel.startVoiceRecording()` returns immediate error.

**When implementing:** Create `docs/plans/YYYY-MM-DD-voice-recording.md`. Key files to touch: `ChatViewModel.kt`, `VoiceRepository.kt`, `ChatRoomScreenReact.kt`, `Module.kt`.

---

### [FUTURE] Feature F2: Task Board Drag-and-Drop

**Scope:** Compose drag gesture on `TaskBoardScreen` — long-press card → drag to target column → release → `TaskViewModel.updateTaskStatus()` → optimistic UI update + haptic feedback.

**Why deferred:** Requires `Modifier.pointerInput` drag tracking, column drop-zone detection, cross-column gesture handling, and careful optimistic-update rollback on failure.

**Current state:** Columns render correctly. Status change works via context menu tap (functional workaround exists).

**When implementing:** Create `docs/plans/YYYY-MM-DD-task-board-dnd.md`. Key files: `TaskBoardScreen.kt`, `TaskBoardScreenWrapper.kt`, `TaskViewModel.kt`. Consider `compose-reorderable` library.

---

### [FUTURE] Feature F3: Attachment Module

**Scope:** Full file attach/view — `AttachmentRepository`, `SupabaseAttachmentDataSource` (bucket: `attachments`), `AttachmentDao` (Room), `AttachmentViewModel`, UI in `TaskDetailScreenReact` (file list + picker) and `ChatRoomScreenReact` (file message type).

**Why deferred:** Requires new Room table (migration needed), new Supabase bucket, new DAO, new repository, new ViewModel, and UI in two screens. Largest surface area of all deferred features.

**Current state:** Zero implementation. `IconSet.Files.attachment` icon exists in the design system.

**When implementing:** Create `docs/plans/YYYY-MM-DD-attachments.md`. Plan the Room migration (v12) first.

---

### [FUTURE] Feature F4: Blocked Users List

**Scope:** `blocked_users` table (userId, blockedUserId, createdAt) + RLS policy + `BlockedUserRepository` + `BlockUserViewModel` + UI in `PrivacySettingsScreen` (list with unblock action) and `UserProfileScreen` (block/unblock button).

**Why deferred:** Requires Supabase schema migration, RLS policy update, new repository, and filtering blocked users from search/member results.

**Current state:** Section hidden in `PrivacySettingsScreen` (Task 15 above). ViewModel comment: `// TODO v1.1`.

**When implementing:** Create `docs/plans/YYYY-MM-DD-blocked-users.md`. Run schema migration in Supabase dashboard first.

---

### [FUTURE] Feature F5: Admin Management UI

**Scope:** Admin-only screens accessible from `MoreTabScreen` when `user.isAdmin == true` — user management list, role escalation, broadcast announcement creation, `app_config` editing, system health dashboard.

**Why deferred:** Large UX scope. Requires `is_admin` RLS policies on all admin-gated tables. `AnnouncementScreen` and `AppConfigRepository` already exist as foundation.

**Current state:** `isAdmin` field on `User` is checked but no admin UI screens exist beyond `AnnouncementScreen`.

**When implementing:** Create `docs/plans/YYYY-MM-DD-admin-ui.md`. Reuse existing `AnnouncementViewModel` and `AppConfigRepository` patterns.

---

*Plan saved. Ready to execute.*
