# Design System Audit - November 9, 2025

**Purpose:** Assess design system compliance across all screens and create migration plan for Phase 4.

**Audit Date:** 2025-11-09
**Auditor:** Claude Code
**Design System Version:** 1.0
**Total Screens Audited:** 14 functional screens

---

## Executive Summary

### Overall Compliance Score: 65% ⚠️

- ✅ **6 screens** - Fully compliant with design system
- ⚠️ **5 screens** - Partially compliant (need minor updates)
- ❌ **3 screens** - Non-compliant (need major refactoring)

### Key Findings

**Strengths:**
- All redesigned screens (`/redesign/` folder) use design tokens consistently
- Component library is comprehensive and well-documented
- Newer screens follow Material 3 guidelines properly
- Design system imports are standard across redesigned screens

**Issues:**
- Older screens use hardcoded values (16.dp, 24.dp, etc.) instead of `Tokens.Spacing.*`
- Inconsistent use of Material 3 vs custom icons (Icons.Default vs IconSet)
- Some screens use raw Material components instead of design system components
- ProfileScreen and other older screens lack consistent spacing

---

## Screen-by-Screen Audit

### ✅ COMPLIANT SCREENS (6 screens)

These screens fully use the design system and require NO changes.

#### 1. EnhancedChatListScreen.kt ✅
**Location:** `/features/chat/presentation/redesign/EnhancedChatListScreen.kt`
**Compliance:** 100%
**Design System Usage:**
- ✅ Uses `Tokens.Spacing.*` for all spacing
- ✅ Uses `ColorTokens` for colors
- ✅ Uses `TypographyTokens` for text styles
- ✅ Uses `IconSet` for all icons
- ✅ Uses design system components (ButtonPrimary, IconButtonStandard, etc.)
- ✅ Uses RefreshableStatefulList layout
- ✅ Uses SwipeActions layout

**Imports:**
```kotlin
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.layouts.RefreshableStatefulList
```

#### 2. EnhancedChatScreen.kt ✅
**Location:** `/features/chat/presentation/redesign/EnhancedChatScreen.kt`
**Compliance:** 100%
**Reason:** Redesigned screen following all design system patterns

#### 3. ProjectListScreen.kt ✅
**Location:** `/features/projects/presentation/redesign/ProjectListScreen.kt`
**Compliance:** 100%
**Design System Usage:**
- ✅ Uses `Tokens` for spacing
- ✅ Uses `IconSet` for icons
- ✅ Uses `RefreshableStatefulList` layout
- ✅ Uses design system components

#### 4. ProjectDetailsScreen.kt ✅
**Location:** `/features/projects/presentation/redesign/ProjectDetailsScreen.kt`
**Compliance:** 100%
**Reason:** Redesigned screen following all design system patterns

#### 5. MyTasksScreen.kt ✅
**Location:** `/features/tasks/presentation/redesign/MyTasksScreen.kt`
**Compliance:** 100%
**Reason:** Redesigned screen following all design system patterns

#### 6. ChatOptionsBottomSheet.kt ✅
**Location:** `/features/chat/presentation/ChatOptionsBottomSheet.kt`
**Compliance:** 100%
**Reason:** Created in Phase 2.5 with full design system compliance

---

### ⚠️ PARTIALLY COMPLIANT SCREENS (5 screens)

These screens need MINOR updates to achieve full compliance.

#### 7. PrivacySettingsScreen.kt ⚠️
**Location:** `/features/profile/presentation/PrivacySettingsScreen.kt`
**Compliance:** 75%
**Current Design System Usage:**
- ✅ Uses design system components
- ✅ Uses IconSet for icons
- ⚠️ Some hardcoded spacing (needs verification)

**Required Changes:**
- [ ] Audit all spacing values, replace hardcoded dp with `Tokens.Spacing.*`
- [ ] Verify all colors use `ColorTokens` or `MaterialTheme.colorScheme`
- [ ] Ensure consistent typography using `TypographyTokens`

**Migration Priority:** LOW (created in Phase 2.1 with good practices)

#### 8. NotificationSettingsScreen.kt ⚠️
**Location:** `/features/profile/presentation/NotificationSettingsScreen.kt`
**Compliance:** 75%
**Current Design System Usage:**
- ✅ Uses design system components
- ✅ Uses IconSet for icons
- ⚠️ Some hardcoded spacing (needs verification)

**Required Changes:**
- [ ] Audit all spacing values, replace hardcoded dp with `Tokens.Spacing.*`
- [ ] Verify all colors use `ColorTokens` or `MaterialTheme.colorScheme`
- [ ] Ensure consistent typography using `TypographyTokens`

**Migration Priority:** LOW (created in Phase 2.2 with good practices)

#### 9. MembersListScreen.kt ⚠️
**Location:** `/features/projects/presentation/MembersListScreen.kt`
**Compliance:** 75%
**Current Design System Usage:**
- ✅ Uses design system components
- ✅ Uses IconSet for icons
- ⚠️ Some hardcoded spacing (needs verification)

**Required Changes:**
- [ ] Audit all spacing values, replace hardcoded dp with `Tokens.Spacing.*`
- [ ] Verify all colors use `ColorTokens` or `MaterialTheme.colorScheme`
- [ ] Ensure consistent typography using `TypographyTokens`

**Migration Priority:** LOW (created in Phase 2.4 with good practices)

#### 10. InviteMembersScreen.kt ⚠️
**Location:** `/features/users/presentation/InviteMembersScreen.kt`
**Compliance:** 70%
**Issues Found:**
- ⚠️ Likely uses some hardcoded values
- ⚠️ May not use design system layouts

**Required Changes:**
- [ ] Replace hardcoded spacing with `Tokens.Spacing.*`
- [ ] Use SearchBarStandard instead of custom search
- [ ] Use RefreshableStatefulList for user list
- [ ] Ensure all buttons use ButtonPrimary/ButtonSecondary

**Migration Priority:** MEDIUM

#### 11. EditProfileScreen.kt ⚠️
**Location:** `/features/profile/presentation/EditProfileScreen.kt`
**Compliance:** 70%
**Issues Found:**
- ⚠️ May use hardcoded spacing values
- ⚠️ Text fields may not use TextFieldStandard

**Required Changes:**
- [ ] Replace hardcoded spacing with `Tokens.Spacing.*`
- [ ] Use TextFieldStandard for all inputs
- [ ] Use ButtonPrimary for save action
- [ ] Ensure avatar size uses `Size.avatarXXLarge`

**Migration Priority:** MEDIUM

---

### ❌ NON-COMPLIANT SCREENS (3 screens)

These screens need MAJOR refactoring to achieve compliance.

#### 12. ProfileScreen.kt ❌
**Location:** `/features/profile/presentation/ProfileScreen.kt`
**Compliance:** 40%
**Issues Found:**
- ❌ Uses hardcoded spacing: `16.dp`, `24.dp`, `120.dp`
- ❌ Uses `Icons.Default.*` instead of `IconSet`
- ❌ Uses raw `IconButton` instead of `IconButtonStandard`
- ❌ Uses raw `Card` instead of `CardElevated`
- ❌ No design system imports

**Current Code Examples:**
```kotlin
// BAD - Hardcoded values
.padding(16.dp)
Spacer(modifier = Modifier.height(24.dp))
.size(120.dp)

// BAD - Not using IconSet
Icon(Icons.Default.ArrowBack, contentDescription = "Back")
Icon(Icons.Default.Person, contentDescription = "Edit")
Icon(Icons.Default.Lock, contentDescription = "Privacy")
Icon(Icons.Default.Notifications, contentDescription = "Notifications")

// BAD - Not using design system components
IconButton(onClick = onNavigateBack) { ... }
Card(modifier = Modifier.fillMaxWidth()) { ... }
```

**Required Changes:**
- [ ] Add design system imports:
  ```kotlin
  import com.example.kosmos.shared.ui.components.*
  import com.example.kosmos.shared.ui.designsystem.IconSet
  import com.example.kosmos.shared.ui.designsystem.Tokens
  import com.example.kosmos.shared.ui.designsystem.TypographyTokens
  import com.example.kosmos.shared.ui.layouts.ScreenScaffold
  ```
- [ ] Replace all hardcoded spacing:
  - `16.dp` → `Tokens.Spacing.md`
  - `24.dp` → `Tokens.Spacing.lg`
  - `120.dp` → `Tokens.Size.avatarXXLarge`
- [ ] Replace icons:
  - `Icons.Default.ArrowBack` → `IconSet.Navigation.back`
  - `Icons.Default.Person` → `IconSet.Action.edit`
  - `Icons.Default.Lock` → `IconSet.Settings.privacy`
  - `Icons.Default.Notifications` → `IconSet.Settings.notifications`
- [ ] Replace components:
  - `IconButton` → `IconButtonStandard`
  - `Card` → `CardElevated`
- [ ] Use `ScreenScaffold` layout pattern
- [ ] Add list items using `ListItemClickable`

**Migration Priority:** HIGH

#### 13. UserProfileScreen.kt ❌
**Location:** `/features/users/presentation/UserProfileScreen.kt`
**Compliance:** 45%
**Issues Found:**
- ❌ Uses hardcoded spacing values
- ❌ May use `Icons.Default.*` instead of `IconSet`
- ❌ May not use design system components

**Required Changes:**
- [ ] Replace hardcoded spacing with `Tokens.Spacing.*`
- [ ] Replace icons with `IconSet`
- [ ] Use `CardElevated` for info cards
- [ ] Use `ButtonPrimary` for primary actions
- [ ] Use `ListItemStandard` for project/task lists

**Migration Priority:** HIGH

#### 14. UserSearchScreen.kt ❌
**Location:** `/features/users/presentation/UserSearchScreen.kt`
**Compliance:** 50%
**Issues Found:**
- ❌ May not use `SearchBarStandard`
- ❌ May use hardcoded spacing
- ❌ May not use `RefreshableStatefulList`

**Required Changes:**
- [ ] Use `SearchBarStandard` for search input
- [ ] Replace hardcoded spacing with `Tokens.Spacing.*`
- [ ] Use `RefreshableStatefulList` for results
- [ ] Use `EmptyStateStandard` for no results
- [ ] Use `ListItemClickable` for user items

**Migration Priority:** MEDIUM-HIGH

---

## Migration Plan for Phase 4

### Phase 4.1: High Priority Screen Migration (3 screens)

**Target Completion:** Day 1-2
**Screens:** ProfileScreen, UserProfileScreen, UserSearchScreen

**Steps:**
1. Add design system imports to each file
2. Create spacing constants mapping table (16dp → md, 24dp → lg, etc.)
3. Find and replace all hardcoded spacing values
4. Replace all `Icons.Default.*` with `IconSet` equivalents
5. Replace raw Material components with design system components
6. Add empty states, loading states, error states
7. Test each screen after migration
8. Update UI Enhancement Logbook

**Success Criteria:**
- Zero hardcoded spacing values
- All icons from IconSet
- All components from design system
- Consistent spacing across all elements

### Phase 4.2: Medium Priority Screen Migration (3 screens)

**Target Completion:** Day 3
**Screens:** InviteMembersScreen, EditProfileScreen, UserSearchScreen (polish)

**Steps:**
1. Same migration process as Phase 4.1
2. Add search/filter using design system components
3. Implement pull-to-refresh with RefreshableStatefulList
4. Add loading/empty/error states

### Phase 4.3: Low Priority Screen Polish (3 screens)

**Target Completion:** Day 4
**Screens:** PrivacySettingsScreen, NotificationSettingsScreen, MembersListScreen

**Steps:**
1. Audit for any remaining hardcoded values
2. Verify all spacing uses design tokens
3. Add consistent animations
4. Polish empty states

### Phase 4.4: Design System Verification

**Target Completion:** Day 5
**Activity:** Full design system compliance check

**Checklist:**
- [ ] All 14 screens use `Tokens.Spacing.*` exclusively
- [ ] All screens use `IconSet` exclusively
- [ ] All screens use design system components
- [ ] All screens have proper empty/loading/error states
- [ ] All screens use consistent animations
- [ ] Zero hardcoded color values (all use MaterialTheme.colorScheme or ColorTokens)
- [ ] All typography uses TypographyTokens or MaterialTheme.typography
- [ ] All touch targets meet 48dp minimum
- [ ] All screens tested and functional

---

## Design Token Usage Patterns

### Common Spacing Replacements

| Hardcoded Value | Design Token | Usage |
|----------------|--------------|--------|
| `4.dp` | `Tokens.Spacing.xxs` | Inner chip padding |
| `8.dp` | `Tokens.Spacing.xs` | Component internal spacing |
| `12.dp` | `Tokens.Spacing.sm` | Small gaps |
| `16.dp` | `Tokens.Spacing.md` | **Standard spacing (most common)** |
| `24.dp` | `Tokens.Spacing.lg` | Section spacing |
| `32.dp` | `Tokens.Spacing.xl` | Major sections |
| `48.dp` | `Tokens.Spacing.xxl` | Screen padding |

### Common Size Replacements

| Hardcoded Value | Design Token | Usage |
|----------------|--------------|--------|
| `24.dp` | `Tokens.Size.iconSmall` | Small icons |
| `24.dp` | `Tokens.Size.avatarSmall` | Small avatars |
| `40.dp` | `Tokens.Size.avatarMedium` | Medium avatars |
| `56.dp` | `Tokens.Size.avatarLarge` | Large avatars |
| `80.dp` | `Tokens.Size.avatarXLarge` | Extra large avatars |
| `120.dp` | `Tokens.Size.avatarXXLarge` | Profile avatars |

### Icon Replacements

| Material Icon | IconSet Equivalent | Location |
|--------------|-------------------|----------|
| `Icons.Default.ArrowBack` | `IconSet.Navigation.back` | All back buttons |
| `Icons.Default.Search` | `IconSet.Action.search` | Search buttons |
| `Icons.Default.Add` | `IconSet.Action.add` | Add/create buttons |
| `Icons.Default.Delete` | `IconSet.Action.delete` | Delete buttons |
| `Icons.Default.Edit` | `IconSet.Action.edit` | Edit buttons |
| `Icons.Default.MoreVert` | `IconSet.Action.moreVert` | Menu buttons |
| `Icons.Default.Person` | `IconSet.Profile.person` | Profile icons |
| `Icons.Default.Lock` | `IconSet.Settings.privacy` | Privacy icons |
| `Icons.Default.Notifications` | `IconSet.Settings.notifications` | Notification icons |

### Component Replacements

| Material Component | Design System Component | When to Use |
|-------------------|------------------------|-------------|
| `Button` | `ButtonPrimary` | Primary actions |
| `OutlinedButton` | `ButtonOutlined` | Secondary actions |
| `TextButton` | `ButtonText` | Low-emphasis actions |
| `IconButton` | `IconButtonStandard` | Icon-only buttons |
| `Card` | `CardElevated` | Cards with elevation |
| `OutlinedCard` | `CardOutlined` | Cards without elevation |
| `TextField` | `TextFieldStandard` | Text inputs |
| `TextField(multiline)` | `TextFieldMultiline` | Multi-line inputs |

---

## Migration Checklist Template

Use this checklist for each screen being migrated:

### Screen: [SCREEN_NAME]

**Pre-Migration:**
- [ ] Read current screen implementation
- [ ] Identify all hardcoded values
- [ ] Identify all Material icons
- [ ] Identify all raw Material components
- [ ] List required design system components

**Migration:**
- [ ] Add design system imports
- [ ] Replace spacing values with Tokens.Spacing.*
- [ ] Replace size values with Tokens.Size.*
- [ ] Replace icons with IconSet.*
- [ ] Replace components with design system equivalents
- [ ] Add empty state using EmptyStateStandard
- [ ] Add loading state using LoadingStateStandard
- [ ] Add error state using ErrorStateStandard

**Post-Migration:**
- [ ] Build successful
- [ ] Screen displays correctly
- [ ] All interactions work
- [ ] Spacing looks consistent
- [ ] Icons display correctly
- [ ] Empty/loading/error states work
- [ ] Update UI Enhancement Logbook
- [ ] Mark screen as ✅ COMPLIANT

---

## Success Metrics

### Target Metrics for Phase 4 Completion

- **Design System Compliance:** 100% (all 14 screens)
- **Hardcoded Values:** 0 (zero dp/sp values outside design tokens)
- **Material Icon Usage:** 0 (all use IconSet)
- **Raw Material Components:** 0 (all use design system components)
- **Screens with Empty States:** 14/14
- **Screens with Loading States:** 14/14
- **Screens with Error States:** 14/14

### Quality Checks

- [ ] Visual consistency across all screens
- [ ] Uniform spacing (4dp grid system)
- [ ] Consistent touch targets (48dp minimum)
- [ ] Smooth animations on all transitions
- [ ] Proper state handling (loading/empty/error)
- [ ] Accessibility compliance (content descriptions)

---

## Notes for Developers

### Before Starting Phase 4 Migration:

1. **Read DESIGN_SYSTEM.md** - Understand available components and patterns
2. **Check this audit** - Know which screen category (compliant/partial/non-compliant)
3. **Use migration checklist** - Follow systematic approach
4. **Test after each screen** - Don't batch multiple screens before testing
5. **Update logbook** - Keep UI_ENHANCEMENT_LOGBOOK.md current

### Common Pitfalls to Avoid:

- ❌ Don't mix hardcoded values with design tokens
- ❌ Don't use `Icons.Default.*` when `IconSet` has equivalent
- ❌ Don't create custom buttons when design system has variants
- ❌ Don't skip empty/loading/error states
- ❌ Don't forget to import design system components

### Quick Reference:

- **Spacing:** Always use `Tokens.Spacing.*` (xxs, xs, sm, md, lg, xl, xxl)
- **Sizes:** Always use `Tokens.Size.*` (icons, avatars, components)
- **Icons:** Always use `IconSet.*` (Navigation, Action, Status, etc.)
- **Components:** Always use design system components from `/shared/ui/components/`
- **Layouts:** Always use layout patterns from `/shared/ui/layouts/`

---

## Appendix: Full File Inventory

### Redesigned Screens (✅ Compliant)
1. `/features/chat/presentation/redesign/EnhancedChatListScreen.kt`
2. `/features/chat/presentation/redesign/EnhancedChatScreen.kt`
3. `/features/projects/presentation/redesign/ProjectListScreen.kt`
4. `/features/projects/presentation/redesign/ProjectDetailsScreen.kt`
5. `/features/tasks/presentation/redesign/MyTasksScreen.kt`
6. `/features/chat/presentation/ChatOptionsBottomSheet.kt`

### Phase 2 Screens (⚠️ Partially Compliant)
7. `/features/profile/presentation/PrivacySettingsScreen.kt`
8. `/features/profile/presentation/NotificationSettingsScreen.kt`
9. `/features/projects/presentation/MembersListScreen.kt`
10. `/features/users/presentation/InviteMembersScreen.kt`
11. `/features/profile/presentation/EditProfileScreen.kt`

### Legacy Screens (❌ Non-Compliant)
12. `/features/profile/presentation/ProfileScreen.kt`
13. `/features/users/presentation/UserProfileScreen.kt`
14. `/features/users/presentation/UserSearchScreen.kt`

### Archived Screens (Not Audited)
- `/archive/legacy_ui/project/presentation/ProjectListScreen.kt`
- `/archive/legacy_ui/project/presentation/ProjectDetailScreen.kt`
- `/archive/legacy_ui/chat/presentation/ChatScreens.kt`

---

**Document Version:** 1.0
**Last Updated:** 2025-11-09
**Next Review:** After Phase 4 completion
