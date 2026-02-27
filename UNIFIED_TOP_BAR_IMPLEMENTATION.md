# Unified Top App Bar Implementation

## Overview

Implemented a single, smart top app bar that handles all workspace screens dynamically instead of having separate conditional top bars.

---

## What Changed

### Before (2 Separate Top Bars)
```kotlin
when (selectedTab) {
    WorkspaceTab.OVERVIEW -> {
        ReactProjectTopBar(projectName, onBackClick, onMoreClick)
    }
    else -> {
        SimpleProjectTopBar(projectName, onBackClick)
    }
}
```

**Problems:**
- Code duplication
- Hard to maintain
- Difficult to add new screen-specific customizations
- No loading state handling
- Static titles

### After (1 Unified Top Bar)
```kotlin
UnifiedWorkspaceTopBar(
    selectedTab = selectedTab,
    projectName = currentProject?.name,
    isLoading = currentProject == null,
    onBackClick = onBackClick,
    onMenuClick = if (selectedTab == WorkspaceTab.OVERVIEW) onEditProject else null
)
```

**Benefits:**
- Single source of truth
- Easy to maintain and extend
- Handles loading states automatically
- Dynamic titles per screen
- Optional menu per screen
- Consistent React design

---

## Features

### 1. Dynamic Screen Titles

The top bar automatically shows the appropriate title based on the selected tab:

| Tab | Title |
|-----|-------|
| **OVERVIEW** | `"Project Details: <Project Name>"` |
| **CHATS** | `"Chats"` |
| **TASKS** | `"Tasks"` |
| **MEMBERS** | `"Members"` |
| **ACTIVITY** | `"Activity"` |

### 2. Loading State Handling

When the project is still loading (`currentProject == null`):
- Shows `"Loading..."` as title
- Prevents showing incomplete data
- Graceful user experience

### 3. Optional Menu Button

Menu button only appears when needed:
- **Overview tab**: Shows menu button (edit/delete project)
- **Other tabs**: No menu button, spacer for alignment
- Extensible: Pass `onMenuClick` for any screen that needs a menu

### 4. Consistent React Design

- Uses `ColorTokens.ReactTheme.card` background
- 1dp border with `ColorTokens.ReactTheme.border`
- React-style typography (SemiBold 600)
- Proper spacing and icon sizing

---

## Implementation Details

### Component Signature

```kotlin
@Composable
private fun UnifiedWorkspaceTopBar(
    selectedTab: WorkspaceTab,      // Current tab (determines title)
    projectName: String?,            // Project name (null when loading)
    isLoading: Boolean,              // Loading state flag
    onBackClick: () -> Unit,         // Back button handler
    onMenuClick: (() -> Unit)? = null // Optional menu handler
)
```

### Title Generation Logic

```kotlin
val title = when {
    isLoading -> "Loading..."
    selectedTab == WorkspaceTab.OVERVIEW && projectName != null ->
        "Project Details: $projectName"
    selectedTab == WorkspaceTab.CHATS -> "Chats"
    selectedTab == WorkspaceTab.TASKS -> "Tasks"
    selectedTab == WorkspaceTab.MEMBERS -> "Members"
    selectedTab == WorkspaceTab.ACTIVITY -> "Activity"
    else -> projectName ?: "Loading..."
}
```

### Menu Button Rendering

```kotlin
if (onMenuClick != null) {
    IconButton(onClick = onMenuClick) {
        Icon(imageVector = IconSet.Action.moreVert, ...)
    }
} else {
    Spacer(modifier = Modifier.width(48.dp)) // Alignment spacer
}
```

---

## How to Extend

### Add Menu to Another Screen

To add a menu button to the Tasks screen, for example:

```kotlin
UnifiedWorkspaceTopBar(
    selectedTab = selectedTab,
    projectName = currentProject?.name,
    isLoading = currentProject == null,
    onBackClick = onBackClick,
    onMenuClick = when (selectedTab) {
        WorkspaceTab.OVERVIEW -> onEditProject
        WorkspaceTab.TASKS -> onTasksMenu      // NEW
        else -> null
    }
)
```

### Custom Title Format

To customize the title format for a specific screen:

```kotlin
// In UnifiedWorkspaceTopBar title generation:
val title = when {
    isLoading -> "Loading..."
    selectedTab == WorkspaceTab.OVERVIEW && projectName != null ->
        "Project Details: $projectName"
    selectedTab == WorkspaceTab.CHATS && projectName != null ->
        "$projectName - Chats"  // Custom format
    selectedTab == WorkspaceTab.TASKS -> "My Tasks"
    // ... etc
}
```

### Add Actions (e.g., Search, Filter)

To add additional action buttons:

```kotlin
@Composable
private fun UnifiedWorkspaceTopBar(
    selectedTab: WorkspaceTab,
    projectName: String?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,  // NEW
    onFilterClick: (() -> Unit)? = null   // NEW
) {
    Surface(...) {
        Row(...) {
            IconButton(onClick = onBackClick) { ... }
            Text(title, ...)

            // Additional actions
            if (onSearchClick != null) {
                IconButton(onClick = onSearchClick) {
                    Icon(IconSet.Action.search, ...)
                }
            }
            if (onFilterClick != null) {
                IconButton(onClick = onFilterClick) {
                    Icon(IconSet.Action.filter, ...)
                }
            }

            // Menu button
            if (onMenuClick != null) { ... }
            else { Spacer(...) }
        }
    }
}
```

### Add Loading Indicator

To show a loading spinner in the top bar:

```kotlin
// In UnifiedWorkspaceTopBar:
Row(...) {
    IconButton(onClick = onBackClick) { ... }

    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = ColorTokens.ReactTheme.primary
        )
    }

    Text(title, ...)
    // ... rest of the bar
}
```

---

## Edge Cases Handled

### 1. Project Not Loaded
- **Scenario**: User navigates to workspace before project data loads
- **Handling**: `isLoading = true` → Shows "Loading..." title
- **Result**: Graceful UX, no null pointer errors

### 2. Project Name Too Long
- **Scenario**: Project name exceeds available space
- **Handling**: `maxLines = 1` + `TextOverflow.Ellipsis`
- **Result**: Text truncates with "..." instead of breaking layout

### 3. Tab Switching
- **Scenario**: User quickly switches between tabs
- **Handling**: Title updates reactively via `selectedTab` parameter
- **Result**: Smooth transitions, no stale titles

### 4. Optional Menu
- **Scenario**: Some screens need menu, others don't
- **Handling**: `onMenuClick: (() -> Unit)? = null`
- **Result**: Menu appears only where needed, spacer maintains alignment

---

## File Changes

### Modified Files

**1. ProjectWorkspaceScreen.kt**
- Lines 75-85: Replaced conditional logic with unified top bar
- Lines 161-262: Replaced 2 components with 1 unified component
- Updated documentation (lines 23-43)

**Changes Summary:**
- ✅ Removed `ReactProjectTopBar` component
- ✅ Removed `SimpleProjectTopBar` component
- ✅ Added `UnifiedWorkspaceTopBar` component
- ✅ Updated call site to use unified component
- ✅ Added loading state handling
- ✅ Added dynamic title generation

---

## Testing Checklist

### Manual Testing

- [ ] **Overview Tab**: Verify title shows "Project Details: <Project Name>"
- [ ] **Overview Tab**: Verify menu button (3 dots) appears
- [ ] **Chats Tab**: Verify title shows "Chats"
- [ ] **Chats Tab**: Verify NO menu button appears
- [ ] **Tasks Tab**: Verify title shows "Tasks"
- [ ] **Tasks Tab**: Verify NO menu button appears
- [ ] **Members Tab**: Verify title shows "Members"
- [ ] **Members Tab**: Verify NO menu button appears
- [ ] **Activity Tab**: Verify title shows "Activity"
- [ ] **Activity Tab**: Verify NO menu button appears
- [ ] **Loading State**: Kill app, reopen → Verify "Loading..." appears briefly
- [ ] **Long Project Name**: Create project with 50+ char name → Verify ellipsis truncation
- [ ] **Back Button**: Verify works from all tabs
- [ ] **Menu Button**: Tap menu → Verify edit/delete dialog opens

### Edge Cases

- [ ] Switch tabs rapidly (5+ times) → Verify no crashes, titles update correctly
- [ ] Navigate to workspace with invalid projectId → Verify "Loading..." persists gracefully
- [ ] Rotate device on each tab → Verify layout maintains correctly
- [ ] Test on small screen (phone) and large screen (tablet) → Verify text doesn't overflow

---

## Future Enhancements

### 1. Tab-Specific Actions

Allow each screen to define its own action buttons:

```kotlin
data class TopBarConfig(
    val title: String,
    val showMenu: Boolean = false,
    val showSearch: Boolean = false,
    val showFilter: Boolean = false,
    val customActions: List<TopBarAction> = emptyList()
)

data class TopBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)
```

### 2. Subtitle Support

Add optional subtitle for additional context:

```kotlin
// Example: "Project Details: Mobile App"
//          "Last updated 2 hours ago"
```

### 3. Breadcrumb Navigation

For nested screens:

```kotlin
// "Projects > Mobile App > Task #42"
```

### 4. Search Integration

Expand/collapse search bar in top bar:

```kotlin
var searchExpanded by remember { mutableStateOf(false) }
if (searchExpanded) {
    SearchBar(...)
} else {
    Text(title, ...)
}
```

---

## Architecture Benefits

### Before
```
ProjectWorkspaceScreen
  ├─ ReactProjectTopBar (Overview only)
  ├─ SimpleProjectTopBar (Other tabs)
  ├─ Content switching
  └─ Bottom navigation
```

**Issues:**
- Duplication
- Hard to extend
- No loading handling
- Static behavior

### After
```
ProjectWorkspaceScreen
  ├─ UnifiedWorkspaceTopBar
  │   ├─ Dynamic title (based on tab)
  │   ├─ Loading state handling
  │   └─ Optional menu (per screen)
  ├─ Content switching
  └─ Bottom navigation
```

**Benefits:**
- DRY (Don't Repeat Yourself)
- Single source of truth
- Easy to extend
- Graceful loading
- Screen-specific customization

---

## Summary

The unified top app bar implementation provides:

✅ **Single Component**: One top bar handles all screens
✅ **Dynamic Titles**: Automatically shows correct title per tab
✅ **Loading States**: Gracefully handles project loading
✅ **Optional Menu**: Shows menu only where needed (Overview)
✅ **Extensible**: Easy to add more customizations per screen
✅ **Consistent Design**: React theme throughout
✅ **Edge Case Handling**: Long names, null states, rapid switching

This architecture is cleaner, more maintainable, and provides a better foundation for future enhancements.
