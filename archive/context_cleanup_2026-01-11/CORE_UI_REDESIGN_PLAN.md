# Core UI Redesign - Minimal & Efficient

**Date:** 2026-01-09
**Approach:** Core components with minimal enhancement
**Status:** Ready to implement

---

## Philosophy

**Core First:** Perfect 4 essential components used on every screen
**Minimal Default:** Everything starts simple, enhancement only where needed
**Consistent Flow:** Same components behave identically everywhere

---

## Core Components (The Only 4 That Matter)

### 1. CoreCard - Single card component for everything

```kotlin
@Composable
fun CoreCard(
    onClick: (() -> Unit)? = null,
    importance: CardImportance = CardImportance.STANDARD,
    content: @Composable () -> Unit
) {
    // PRIMARY = glass effect (projects, tasks, messages - your main content)
    // STANDARD = minimal flat (sections, lists, metadata)
}

enum class CardImportance { PRIMARY, STANDARD }
```

**Usage:**
- **PRIMARY**: Project cards, task cards, own message bubbles, stat cards
- **STANDARD**: Time tracking section, tags section, other users' messages, lists

### 2. CoreButton - Single button for all actions

```kotlin
@Composable
fun CoreButton(
    onClick: () -> Unit,
    label: String,
    variant: ButtonVariant = ButtonVariant.PRIMARY
) {
    // PRIMARY = filled (create, save, send)
    // SECONDARY = outlined (cancel, back)
    // TERTIARY = text only (edit, delete, show more)
}

enum class ButtonVariant { PRIMARY, SECONDARY, TERTIARY }
```

**Usage:**
- **PRIMARY**: FAB create button, submit forms, send messages
- **SECONDARY**: Cancel dialogs, back navigation
- **TERTIARY**: Inline actions (edit description, show more tasks)

### 3. CoreInput - Single input for all text entry

```kotlin
@Composable
fun CoreInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    leadingIcon: ImageVector? = null
)
```

**Usage:**
- Search bars (with Icons.Search leading icon)
- Form fields (project name, description)
- Message input
- All text entry across app

### 4. CoreNav - Consistent navigation

```kotlin
@Composable
fun CoreTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: List<TopBarAction> = emptyList()
)

@Composable
fun CoreBottomNav(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
)
```

**Usage:**
- CoreTopBar on every screen (title + optional back + actions)
- CoreBottomNav for main navigation (Projects, Tasks, Chats, Profile)

---

## Visual Specification

### Core Card Styling

**PRIMARY Cards (Glass Effect):**
- Background: White with 85% opacity
- Blur: 8dp backdrop blur
- Border: 1dp white (30% opacity)
- Shadow: Soft 2dp shadow (12% black)
- Spacing: 32dp between cards

**STANDARD Cards (Minimal Flat):**
- Background: White 100% solid
- Border: 1dp gray (#E0E0E0)
- No shadow, no blur
- Spacing: 16dp between sections

### Core Button Styling

**PRIMARY:**
- Background: Purple gradient (Midnight Plum)
- Text: White, bold (700 weight)
- Height: 48dp
- Corner: 12dp radius

**SECONDARY:**
- Background: Transparent
- Border: 2dp purple
- Text: Purple, medium (600 weight)
- Height: 48dp

**TERTIARY:**
- Background: Transparent
- No border
- Text: Purple, medium (500 weight)
- Height: auto (text only)

### Core Input Styling

- Background: White 100% solid
- Border: 1dp gray (unfocused), 2dp purple (focused)
- Corner: 12dp radius
- Height: 56dp
- Typography: Body medium (400 weight)

### Core Nav Styling

**TopBar:**
- Background: White 100% solid
- Height: 56dp
- Title: Headline small, bold (700 weight)
- Shadow: 1dp bottom divider (#E0E0E0)

**BottomNav:**
- Background: White 100% solid
- Height: 64dp
- Selected: Purple icon + label
- Unselected: Gray icon + label
- Shadow: 1dp top divider

---

## Design Token Unification

### Single Color System (Remove Stitch/Material Mixing)

```kotlin
object ColorTokens {
    object Primary {
        val default = Color(0xFF6200EA)  // Midnight Plum
        val light = Color(0xFF9C27B0)
        val dark = Color(0xFF4527A0)
    }

    object Surface {
        val default = Color(0xFFFFFFFF)  // White
        val variant = Color(0xFFFAFAFA)  // Light gray
    }

    object Border {
        val default = Color(0xFFE0E0E0)  // Gray
        val focused = Color(0xFF6200EA)  // Purple
    }

    object Text {
        val primary = Color(0xFF1C1B1F)  // Dark gray
        val secondary = Color(0xFF49454F) // Medium gray
    }
}
```

### Single Typography System

```kotlin
object TypographyTokens {
    val headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
    val headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
    val titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    val titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
    val bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
}
```

### Glass Tokens (For PRIMARY Cards Only)

```kotlin
object GlassTokens {
    val alpha = 0.85f
    val blur = 8.dp
    val borderAlpha = 0.3f
    val shadowAlpha = 0.12f
}
```

---

## Screen-by-Screen Core Application

### ProjectListScreen
- **PRIMARY Cards**: Project cards (glass effect)
- **CoreInput**: Search bar (leading icon: search)
- **CoreButton (TERTIARY)**: Filter chips
- **CoreButton (PRIMARY)**: FAB create project
- **CoreTopBar**: "Projects" + notifications icon
- **CoreBottomNav**: Selected on Projects tab

**Whitespace:** 32dp between project cards, 24dp padding

### TaskDetailScreen
- **PRIMARY Card**: Hero section (title + status + priority)
- **STANDARD Cards**: Time tracking, tags, subtasks (flat, minimal)
- **CoreButton (PRIMARY)**: Save changes
- **CoreButton (TERTIARY)**: Edit description, add subtask
- **CoreInput**: Description field, comment input
- **CoreTopBar**: Task title + back + more menu

**Whitespace:** 24dp between sections, 16dp padding

### EnhancedChatScreen
- **PRIMARY Cards**: Own message bubbles (glass)
- **STANDARD Cards**: Others' messages (flat)
- **CoreInput**: Message input (leading icon: attach)
- **CoreButton (PRIMARY)**: Send FAB
- **CoreTopBar**: Chat name + back + search + more
- **CoreBottomNav**: Selected on Chats tab

**Whitespace:** 8dp between messages, 16dp padding

### MyTasksScreen
- **PRIMARY Cards**: Task cards (glass in list view)
- **CoreInput**: Search bar
- **CoreButton (TERTIARY)**: View toggle (list/board), filter, sort
- **CoreButton (PRIMARY)**: FAB create task
- **CoreTopBar**: "My Tasks" + actions
- **CoreBottomNav**: Selected on Tasks tab

**Whitespace:** 24dp between task cards, 16dp padding

### ProjectDetailsScreen
- **PRIMARY Cards**: Overview stat cards (glass)
- **STANDARD Cards**: Member list items, chat list items (flat)
- **CoreButton (TERTIARY)**: Tab navigation (Overview, Chats, Tasks, Members, Activity)
- **CoreButton (PRIMARY)**: Add member, create chat, create task
- **CoreTopBar**: Project name + back + edit + more

**Whitespace:** 24dp between stat cards, 16dp between list items

---

## Implementation Steps

### Week 1: Build Core Components

**Day 1 (Design Tokens)**
- Create unified `ColorTokens.kt` (remove Stitch/Material mixing)
- Create unified `TypographyTokens.kt` (single source)
- Create `GlassTokens.kt` (for PRIMARY cards only)
- Build successful

**Day 2 (CoreCard + CoreButton)**
- Build `CoreCard` with PRIMARY/STANDARD variants
- Build `CoreButton` with PRIMARY/SECONDARY/TERTIARY variants
- Preview composables for testing
- Build successful

**Day 3 (CoreInput + CoreNav)**
- Build `CoreInput` with focus states
- Build `CoreTopBar` with back + actions
- Build `CoreBottomNav` with selection state
- Preview composables
- Build successful

**Day 4 (Testing)**
- Test all 4 core components in isolation
- Verify WCAG AA contrast ratios (4.5:1 minimum)
- Test dark mode (if applicable)
- Performance check (no jank)

### Week 2: Apply to All Screens

**Day 1 (Key Screens 1-3)**
- Update ProjectListScreen with core components
- Update TaskDetailScreen with core components
- Update EnhancedChatScreen with core components
- Build successful, test flow

**Day 2 (Key Screens 4-5 + Wizard)**
- Update MyTasksScreen with core components
- Update ProjectDetailsScreen with core components
- Update ProjectCreationWizard with core components
- Build successful, test flow

**Day 3 (Remaining Screens)**
- Update all auth screens (Login, SignUp)
- Update all profile screens
- Update all task screens
- Update all chat screens
- Build successful

**Day 4 (Final Polish)**
- Fix any inconsistencies
- Verify whitespace consistency (24dp/32dp/16dp)
- Run Android Accessibility Scanner
- Performance profiling (GPU rendering)
- Final build test

---

## File Changes Summary

### New Files (1)
- `/app/src/main/java/com/example/kosmos/shared/ui/components/CoreComponents.kt` (~400 lines)
  - CoreCard
  - CoreButton
  - CoreInput
  - CoreTopBar
  - CoreBottomNav

### Modified Files (4 - Design System)
- `/app/src/main/java/com/example/kosmos/shared/ui/designsystem/ColorTokens.kt`
  - Remove Stitch.* colors
  - Keep only Midnight Plum Primary, Surface, Border, Text

- `/app/src/main/java/com/example/kosmos/shared/ui/designsystem/TypographyTokens.kt`
  - Remove Material.typography.* mixing
  - Single typography scale

- `/app/src/main/java/com/example/kosmos/shared/ui/designsystem/Tokens.kt`
  - Add GlassTokens object

- `/app/src/main/java/com/example/kosmos/shared/ui/designsystem/Gradients.kt`
  - Add primary button gradient

### Modified Files (21+ - Screens)
All screens updated to use CoreCard, CoreButton, CoreInput, CoreTopBar, CoreBottomNav

---

## Success Criteria

**Core Components:**
- [ ] 4 core components built and tested
- [ ] Consistent behavior across all screens
- [ ] WCAG AA contrast compliance
- [ ] 60fps performance on mid-range devices

**Design System:**
- [ ] Single color system (no Stitch/Material mixing)
- [ ] Single typography system
- [ ] GlassTokens for PRIMARY cards only
- [ ] All hardcoded values removed

**Screens:**
- [ ] 21+ screens using core components
- [ ] Consistent whitespace (24dp/32dp/16dp)
- [ ] Visual hierarchy clear (PRIMARY = glass, STANDARD = flat)
- [ ] Build successful with no errors

**Flow Efficiency:**
- [ ] No excess headings or visual noise
- [ ] Navigation feels natural and fast
- [ ] Users can complete tasks without friction
- [ ] Core components feel invisible (good UX)

---

## Decision: Core Approach

**Selected:** Simplified "Essential Core" approach
**Why:** User requested minimal, efficient, core-focused design
**Components:** Only 4 core components (Card, Button, Input, Nav)
**Enhancement:** Glass effect only on PRIMARY importance content
**Timeline:** 2 weeks (1 week core, 1 week application)

**Next Step:** Build CoreComponents.kt with 4 unified components

---

**Last Updated:** 2026-01-09
**Status:** Plan approved, ready to implement
**Reference:** This plan + `UI_REDESIGN_2026_LOGBOOK.md` for session tracking
