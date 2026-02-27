# Kosmos Design System

**Version:** 1.0
**Last Updated:** November 9, 2025
**Status:** ✅ Implemented and In Use

---

## Overview

The Kosmos design system is a comprehensive set of reusable components, design tokens, and patterns that ensure visual consistency across the entire application. Built on **Material Design 3** principles, it provides a cohesive, modern, and accessible user experience.

### Key Principles

1. **Consistency** - Same patterns across all screens
2. **Accessibility** - Minimum 48dp touch targets, 4.5:1 contrast
3. **Responsiveness** - Adapts to different screen sizes
4. **Performance** - Optimized composables with minimal recomposition
5. **Developer Experience** - Easy to use, well-documented components

---

## 📐 Design Tokens

### Location
`/shared/ui/designsystem/Tokens.kt`

### Spacing Scale (4dp Grid)

| Token | Value | Usage |
|-------|-------|-------|
| `Spacing.xxs` | 4dp | Inner chip padding, icon margins |
| `Spacing.xs` | 8dp | Component internal spacing |
| `Spacing.sm` | 12dp | Small gaps between related items |
| `Spacing.md` | 16dp | **Standard spacing (default, most common)** |
| `Spacing.lg` | 24dp | Section spacing |
| `Spacing.xl` | 32dp | Major section breaks |
| `Spacing.xxl` | 48dp | Screen top/bottom padding |

**Usage Example:**
```kotlin
Column(
    modifier = Modifier.padding(Tokens.Spacing.md),
    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
) {
    // Content
}
```

### Touch Target Sizes

| Token | Value | Usage |
|-------|-------|-------|
| `TouchTarget.minimum` | 48dp | Absolute minimum for accessibility |
| `TouchTarget.recommended` | 56dp | Recommended for primary actions |
| `TouchTarget.iconButton` | 48dp | Standard icon button |
| `TouchTarget.fab` | 56dp | Floating action button |
| `TouchTarget.listItemMinHeight` | 56dp | Minimum list item height |

### Component Sizes

**Avatars:**
- `Size.avatarSmall` - 24dp
- `Size.avatarMedium` - 40dp
- `Size.avatarLarge` - 56dp
- `Size.avatarXLarge` - 80dp
- `Size.avatarXXLarge` - 120dp

**Icons:**
- `Size.iconSmall` - 16dp
- `Size.iconMedium` - 24dp
- `Size.iconLarge` - 32dp

**Other:**
- `Size.chipHeight` - 32dp
- `Size.statusDot` - 8dp
- `Size.dividerThickness` - 1dp

### Elevation Levels

| Level | Value | Usage |
|-------|-------|-------|
| `level0` | 0dp | Flat surfaces |
| `level1` | 1dp | Cards, subtle elevation |
| `level2` | 3dp | Buttons, chips |
| `level3` | 6dp | FABs, modals |
| `level4` | 8dp | Navigation drawer |
| `level5` | 12dp | Dialogs, pickers |

---

## 🎨 Color System

### Location
`/shared/ui/designsystem/ColorTokens.kt`

### Color Palette

**Primary Colors:**
- `Primary.light` - Main brand color (light theme)
- `Primary.dark` - Main brand color (dark theme)
- `Primary.container` - Container backgrounds

**Status Colors:**
- `Status.online` - Green for online status
- `Status.offline` - Gray for offline
- `Status.busy` - Red for busy
- `Status.away` - Yellow for away

**Error Colors:**
- `Error.light` - Error states in light theme
- `Error.dark` - Error states in dark theme

**Surface Colors:**
- `Surface.elevated0` through `elevated5` - Different elevation levels

### Usage Example
```kotlin
Surface(
    color = ColorTokens.Primary.light,
    contentColor = ColorTokens.OnPrimary.light
) {
    Text("Primary Button")
}
```

---

## ✍️ Typography

### Location
`/shared/ui/designsystem/TypographyTokens.kt`

### Type Scale

| Style | Font Size | Weight | Usage |
|-------|-----------|--------|-------|
| `Display` | 57sp | Regular | Hero titles |
| `Headline Large` | 32sp | Regular | Page titles |
| `Headline Medium` | 28sp | Regular | Section headers |
| `Title Large` | 22sp | Medium | Screen titles |
| `Title Medium` | 16sp | Medium | Card titles |
| `Body Large` | 16sp | Regular | Primary body text |
| `Body Medium` | 14sp | Regular | Default body text |
| `Body Small` | 12sp | Regular | Secondary info |
| `Label Large` | 14sp | Medium | Button labels |
| `Label Small` | 11sp | Medium | Chip labels |

### Custom Typography
- `Custom.badgeNumber` - Badge text styling
- `Custom.caption` - Timestamp captions
- `Custom.code` - Monospace code blocks

### Usage Example
```kotlin
Text(
    text = "Section Title",
    style = TypographyTokens.typography.titleLarge,
    color = MaterialTheme.colorScheme.onSurface
)
```

---

## 🧩 Component Library

### Location
`/shared/ui/components/`

### Button Components (`Buttons.kt`)

**ButtonPrimary** - Main call-to-action buttons
```kotlin
ButtonPrimary(
    text = "Save",
    onClick = { /* action */ },
    enabled = true,
    loading = false
)
```

**ButtonSecondary** - Secondary actions
**ButtonOutlined** - Low-emphasis actions
**ButtonText** - Minimal actions
**IconButtonStandard** - Icon-only buttons
**FABStandard** - Floating action buttons

### Card Components (`Cards.kt`)

**CardElevated** - Standard cards with elevation
```kotlin
CardElevated(
    modifier = Modifier.fillMaxWidth(),
    onClick = { /* action */ }
) {
    // Card content
}
```

**CardOutlined** - Cards with border, no elevation
**CardClickable** - Interactive cards
**SectionHeader** - Section dividers with labels

### Input Components (`Inputs.kt`)

**TextFieldStandard** - Standard text input
```kotlin
TextFieldStandard(
    value = text,
    onValueChange = { text = it },
    label = "Username",
    error = errorMessage,
    leadingIcon = Icons.Default.Person
)
```

**SearchBarStandard** - Search input with clear button
**PasswordField** - Text field with show/hide toggle
**TextFieldMultiline** - Multi-line text input

### List Components (`Lists.kt`)

**ListItemStandard** - Standard list item
**ListItemClickable** - Clickable list item
**ListDivider** - List separators
**ChipGroup** - Horizontal chip list
**ChipFilter** - Selectable filter chips

### Dialog Components (`Dialogs.kt`)

**DialogStandard** - Basic alert dialog
**BottomSheetStandard** - Bottom sheet modal
**ConfirmationDialog** - Yes/No confirmations
**LoadingDialog** - Full-screen loading overlay

### Feedback Components (`Feedback.kt`)

**EmptyStateStandard** - Empty list states
**ErrorStateStandard** - Error states with retry
**LoadingStateStandard** - Loading indicators
**SnackbarStandard** - Toast notifications

---

## 🎬 Animations

### Location
`/shared/ui/animations/Transitions.kt`

### Available Animations

**Screen Transitions:**
- `slideInFromRight()` - Enter from right
- `slideInFromBottom()` - Enter from bottom
- `fadeIn()` - Fade in
- `scaleIn()` - Scale from center

**List Animations:**
- `fadeInListItem()` - Fade in with delay
- `staggeredFadeIn()` - Staggered list animation

**State Animations:**
- `shimmerEffect()` - Loading shimmer
- `bounceAnimation()` - Bounce feedback
- `expandCollapse()` - Expand/collapse content

### Usage Example
```kotlin
AnimatedVisibility(
    visible = isVisible,
    enter = slideInFromRight() + fadeIn(),
    exit = slideOutToLeft() + fadeOut()
) {
    // Content
}
```

---

## 🎯 Icon System

### Location
`/shared/ui/designsystem/IconSet.kt`

### Icon Categories

**Navigation:**
- `IconSet.Navigation.back` - Back arrow
- `IconSet.Navigation.menu` - Hamburger menu
- `IconSet.Navigation.close` - Close X

**Actions:**
- `IconSet.Action.add` - Plus icon
- `IconSet.Action.delete` - Trash icon
- `IconSet.Action.edit` - Pencil icon
- `IconSet.Action.search` - Search magnifying glass
- `IconSet.Action.moreVert` - Vertical dots menu

**Status:**
- `IconSet.Status.online` - Online indicator
- `IconSet.Status.offline` - Offline indicator
- `IconSet.Status.checkCircle` - Success check

**Message:**
- `IconSet.Message.send` - Send arrow
- `IconSet.Message.pin` - Pin icon
- `IconSet.Message.archive` - Archive box

### Usage Example
```kotlin
Icon(
    imageVector = IconSet.Action.delete,
    contentDescription = "Delete",
    tint = MaterialTheme.colorScheme.error
)
```

---

## 📱 Layout Patterns

### Location
`/shared/ui/layouts/`

### Screen Scaffold (`ScreenScaffold.kt`)

Standard screen layout with top bar, content, and optional FAB:

```kotlin
ScreenScaffold(
    title = "Screen Title",
    onBackClick = { /* navigate back */ },
    actions = {
        IconButtonStandard(
            icon = IconSet.Action.search,
            onClick = { /* search */ }
        )
    },
    floatingActionButton = {
        FABStandard(
            icon = IconSet.Action.add,
            onClick = { /* add */ }
        )
    }
) { padding ->
    // Screen content with padding applied
}
```

### Swipeable Layout (`SwipeableLayout.kt`)

Swipe-to-reveal actions (used in chat list, etc.):

```kotlin
SwipeActions(
    onSwipeLeft = { /* delete */ },
    onSwipeRight = { /* archive */ },
    leftIcon = IconSet.Action.delete,
    rightIcon = IconSet.Message.archive
) {
    // Item content
}
```

### Refreshable List (`ListLayouts.kt`)

Pull-to-refresh list with loading, empty, and error states:

```kotlin
RefreshableStatefulList(
    state = listState,
    isRefreshing = isRefreshing,
    onRefresh = { /* refresh */ },
    emptyTitle = "No items",
    errorTitle = "Failed to load"
) { items ->
    items(items) { item ->
        // List item composable
    }
}
```

---

## ✅ Usage Guidelines

### DO's ✅

1. **Always use design tokens** instead of hardcoded values
   ```kotlin
   // Good
   padding(Tokens.Spacing.md)

   // Bad
   padding(16.dp)
   ```

2. **Use standard components** from the library
   ```kotlin
   // Good
   ButtonPrimary(text = "Save", onClick = { })

   // Bad
   Button(onClick = { }) { Text("Save") }
   ```

3. **Follow naming conventions**
   - Components: `ComponentNameVariant` (e.g., `ButtonPrimary`)
   - Tokens: `Category.property` (e.g., `Spacing.md`)

4. **Add content descriptions** for accessibility
   ```kotlin
   Icon(
       imageVector = icon,
       contentDescription = "Descriptive text"
   )
   ```

### DON'Ts ❌

1. **Don't hardcode colors**
   ```kotlin
   // Bad
   color = Color(0xFF0000FF)

   // Good
   color = ColorTokens.Primary.light
   ```

2. **Don't create one-off components** without checking if a standard component exists

3. **Don't skip accessibility** requirements
   - Minimum 48dp touch targets
   - Content descriptions
   - Semantic properties

4. **Don't use deprecated components** from old screens

---

## 🚀 Migration Guide

### Updating Existing Screens

When refactoring screens to use the design system:

1. **Replace hardcoded spacings** with `Tokens.Spacing.*`
2. **Replace custom buttons** with `ButtonPrimary`, `ButtonSecondary`, etc.
3. **Replace custom colors** with `ColorTokens.*`
4. **Replace Material components** with Kosmos components where available
5. **Add consistent loading/error/empty states**

### Example Migration

**Before:**
```kotlin
Button(
    onClick = { },
    modifier = Modifier.padding(16.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF6200EE)
    )
) {
    Text("Save")
}
```

**After:**
```kotlin
ButtonPrimary(
    text = "Save",
    onClick = { },
    modifier = Modifier.padding(Tokens.Spacing.md)
)
```

---

## 📊 Component Status

### ✅ Complete and In Use
- Buttons (all variants)
- Cards (all variants)
- Text inputs (standard, search, password)
- Lists (standard items, dividers, chips)
- Dialogs (standard, bottom sheets, confirmations)
- Feedback states (empty, error, loading)
- Layout scaffolds (screen, swipeable, refreshable)
- Design tokens (spacing, sizing, elevation)
- Color system
- Typography system
- Icon set
- Animations

### 🎯 Needs Review
- Consistency across ALL screens (Phase 4 goal)
- Dark mode support
- Accessibility compliance verification

---

## 📚 Additional Resources

- **Component Files:** `/shared/ui/components/`
- **Design Tokens:** `/shared/ui/designsystem/`
- **Theme:** `/shared/ui/theme/`
- **Layouts:** `/shared/ui/layouts/`
- **Animations:** `/shared/ui/animations/`

---

## 🔄 Changelog

**Version 1.0 (November 9, 2025)**
- Initial documentation created
- All components documented
- Usage guidelines established
- Migration guide added

---

**For questions or contributions, see the main README.md**
