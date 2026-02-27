# Kosmos Screen Theme Guide

Reference extracted from: ProjectList, ProjectDetails, MyTasks, TaskDetail, TaskEdit, ChatList, ChatRoom screens.

---

## Color Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `ReactTheme.background` | `#0F0F14` | Screen background, scaffold |
| `ReactTheme.card` | `#18181D` | Cards, top bars, input containers, message bubbles |
| `ReactTheme.secondary` | `#1F1F27` | Search bars, filter chips (unselected), reply preview, system messages |
| `ReactTheme.muted` | `#2A2A32` | Muted backgrounds |
| `ReactTheme.border` | `#2A2A32` | Card borders, dividers, input borders (unfocused) |
| `ReactTheme.primary` | `#7C3AED` | Accent, selected states, FAB, send button, focus borders |
| `ReactTheme.primaryForeground` | `#FFFFFF` | Text on primary backgrounds |
| `ReactTheme.foreground` | `#E8E8ED` | Primary text, titles, body |
| `ReactTheme.mutedForeground` | `#9CA3AF` | Labels, timestamps, placeholders, secondary text |
| `ReactTheme.destructive` | `#EF4444` | Delete, error, overdue |

### Status Colors (hardcoded)

| Status | Color | Usage |
|--------|-------|-------|
| TODO / Default | `#6B7280` (gray) | Badge bg at 0.2f alpha, text solid |
| IN_PROGRESS | `#7C3AED` (purple) | Same as primary |
| DONE | `#10B981` (emerald) | Badge bg at 0.2f alpha, text solid |
| MEDIUM priority | `#F59E0B` (amber) | Badge bg at 0.15f alpha |
| HIGH priority | `#EF4444` (red) | Badge bg at 0.15f alpha |
| Indigo accent | `#6366F1` | Own-message avatars, activity icons |

---

## Corner Radius

| Size | Value | Where |
|------|-------|-------|
| **Cards / Inputs** | `12.dp` (`Tokens.CornerRadius.md`) | All main cards, OutlinedTextFields, message bubbles, send button, search bars |
| **Chips / Small buttons** | `8.dp` | Filter chips, tag chips, reply preview, view toggle buttons |
| **Pill badges** | `16.dp` | Status badges (Active/Archived) |
| **Tiny badges** | `6.dp` | Status/priority inline badges on task detail |
| **Priority badge** | `4.dp` | Small priority indicator |
| **Fully round** | `CircleShape` | Avatars, FAB, unread badge, progress bar track |
| **Top bar** | `RectangleShape` | Top bar surfaces have no rounding (flat edge-to-edge) |

---

## Cards

### Standard Content Card
Used for: task details sections, chat items, project cards, stat cards.

```
Surface/Card {
    color = ReactTheme.card
    shape = RoundedCornerShape(12.dp)     // Tokens.CornerRadius.md
    border = 1.dp, ReactTheme.border
    elevation = Tokens.Elevation.level1    // subtle
    padding = 16.dp (or 12.dp for compact)
}
```

Most screens use `KosmosCard` which wraps this pattern.

### Stat Card (ProjectDetails)
Same as standard card plus:
- Shadow: `4.dp` elevation with `Color.Black.copy(alpha = 0.15f)`
- Internal icon container: `8.dp` rounded, colored bg at `0.15f` alpha

### Input Card (TaskEdit fields)
```
OutlinedTextField {
    containerColor = ReactTheme.card       // both focused and unfocused
    focusedBorder = ReactTheme.primary
    unfocusedBorder = ReactTheme.border
    shape = RoundedCornerShape(12.dp)
    textColor = ReactTheme.foreground
    labelColor = ReactTheme.mutedForeground (unfocused) / ReactTheme.primary (focused)
    placeholderColor = ReactTheme.mutedForeground
}
```

### Selectable Card (TaskEdit status/priority grid)
```
Surface {
    height = 48.dp
    shape = RoundedCornerShape(12.dp)
    // Selected:
    color = ReactTheme.primary
    border = 1.dp, ReactTheme.primary
    textColor = ReactTheme.primaryForeground
    // Unselected:
    color = ReactTheme.card
    border = 1.dp, ReactTheme.border
    textColor = ReactTheme.foreground
}
```

### Chat Message Bubble
```
Surface {
    color = ReactTheme.card
    shape = RoundedCornerShape(12.dp)
    border = 1.dp, ReactTheme.border
    padding = horizontal 12.dp, vertical 8.dp
}
```

### System Message (Chat)
```
Surface {
    color = ReactTheme.secondary
    shape = RoundedCornerShape(12.dp)
    padding = horizontal 12.dp, vertical 6.dp
    // centered in row
}
```

### Task-type Message (Chat)
```
Surface {
    color = ReactTheme.primary.copy(alpha = 0.1f)
    shape = RoundedCornerShape(12.dp)
    border = 1.dp, ReactTheme.primary.copy(alpha = 0.3f)
    padding = 12.dp
}
```

---

## Top Bar

All screens use the same pattern:

```
Surface {
    color = ReactTheme.card
    shadowElevation = 0.dp
    tonalElevation = 0.dp
    padding = horizontal 16.dp, vertical 12.dp
}
// Bottom border:
HorizontalDivider(thickness = 1.dp, color = ReactTheme.border)
```

- Back icon: `22.dp`
- Title: `18-20.sp`, `SemiBold`, `ReactTheme.foreground`
- Subtitle: `12-14.sp`, `ReactTheme.mutedForeground`
- Action icons: `22.dp`

---

## Search Bar

```
Surface {
    height = 48.dp
    color = ReactTheme.secondary
    shape = RoundedCornerShape(12.dp)
    padding = horizontal 12-16.dp
}
// Search icon: 20.dp, mutedForeground
// Placeholder: 15.sp, mutedForeground
// Text: 15.sp, foreground
```

Alternative (OutlinedTextField variant in MyTasks):
```
focusedContainer = ReactTheme.card
unfocusedContainer = ReactTheme.card
focusedBorder = ReactTheme.primary.copy(alpha = 0.5f)
unfocusedBorder = ReactTheme.border
```

---

## Filter Chips

```
// Selected:
color = ReactTheme.primary
textColor = ReactTheme.primaryForeground
shape = RoundedCornerShape(8.dp)
border = none

// Unselected:
color = ReactTheme.secondary
textColor = ReactTheme.foreground
shape = RoundedCornerShape(8.dp)
border = none

// Text: 14.sp, Medium
```

---

## Badges

### Status Badge (pill)
```
Surface {
    shape = RoundedCornerShape(16.dp)
    color = statusColor.copy(alpha = 0.2f)
    padding = horizontal 8-10.dp, vertical 2-4.dp
}
Text { 12.sp, Medium, color = statusColor }
```

### Priority Badge (compact)
```
Surface {
    shape = RoundedCornerShape(4.dp)
    color = priorityColor.copy(alpha = 0.15f)
    padding = horizontal 8.dp, vertical 2.dp
}
Icon { 12.dp, color = priorityColor }
Text { 12.sp, Medium, color = priorityColor }
```

### Unread Count Badge (circle)
```
Surface {
    shape = CircleShape
    color = ReactTheme.primary
    minWidth = 20.dp
    padding = horizontal 6.dp, vertical 2.dp
}
Text { 12.sp, SemiBold, color = primaryForeground }
```

### Count Badge (neutral)
```
Surface {
    shape = CircleShape
    color = ReactTheme.secondary
    padding = horizontal 8.dp, vertical 2.dp
}
Text { 12.sp, Medium, color = mutedForeground }
```

---

## Avatars

| Size | Usage |
|------|-------|
| `32.dp` | Chat message sender, activity items |
| `24.dp` | Member list overlapping avatars |
| `20.dp` | Inline small avatar |

```
Surface {
    shape = CircleShape
    color = ReactTheme.primary  // or Color(0xFF6366F1) for own user
}
Text { 10-12.sp, Medium, primaryForeground }
```

Overlapping avatars: offset by `-8.dp` horizontally.

---

## FAB (Floating Action Button)

```
Surface {
    size = 56.dp
    shape = CircleShape
    color = ReactTheme.primary
    shadowElevation = 8-16.dp
    // optional glow: primary.copy(alpha = 0.5f)
}
Icon { 24.dp, color = primaryForeground }
// Position: bottom-end, padding 24.dp from edges
```

---

## Dividers

| Type | Spec |
|------|------|
| **Section divider** | `HorizontalDivider(thickness = 1.dp, color = ReactTheme.border)` |
| **Subtle divider** | `HorizontalDivider(thickness = 0.5.dp, color = ReactTheme.border.copy(alpha = 0.4f))` |
| **Top bar bottom border** | `HorizontalDivider(thickness = 1.dp, color = ReactTheme.border)` |
| **Vertical divider** (time tracking) | `1.dp` width, `32.dp` height, `ReactTheme.border` |

---

## Spacing

| Token | Value | Usage |
|-------|-------|-------|
| Screen horizontal padding | `16.dp` | All screens use this |
| Between cards/sections | `12-16.dp` | List items, sections |
| Between project groups | `20.dp` | Grouped task lists |
| Card internal padding | `12-16.dp` | 16dp standard, 12dp compact |
| Card internal gap | `8-12.dp` | Between elements inside cards |
| Label to field gap | `8.dp` | Form label → input field |
| Icon to text gap | `8.dp` | Icon + text rows |
| FAB edge padding | `24.dp` | Distance from screen edges |

---

## Icon Sizes

| Size | Usage |
|------|-------|
| `24.dp` | FAB icon |
| `22.dp` | Top bar back/action icons |
| `20.dp` | Secondary action icons, send button, calendar |
| `18.dp` | Tertiary actions, small buttons |
| `16.dp` | Decorative/info icons in cards |
| `14.dp` | Pin indicator, small UI icons |
| `12.dp` | Badge icons (priority), tiny indicators |
| `6.dp` | Dot indicators |

---

## Text Hierarchy

| Role | Size | Weight | Color |
|------|------|--------|-------|
| Screen title | `20.sp` | `SemiBold` | `foreground` |
| Section header | `15-16.sp` | `SemiBold` | `foreground` |
| Card title | `15-16.sp` | `SemiBold` | `foreground` |
| Body text | `14-15.sp` | `Normal` | `foreground` |
| Description | `14.sp` | `Normal` | `foreground` (line height 1.4-1.6x) |
| Label / Caption | `12-14.sp` | `Medium` | `mutedForeground` |
| Timestamp | `12.sp` | `Normal` | `mutedForeground` |
| Badge text | `12.sp` | `Medium` | varies by badge type |
| Stat value | `24.sp` | `Bold` | `foreground` |
| Avatar letter | `10-12.sp` | `Medium` | `primaryForeground` |

Line height multipliers:
- Description text: `1.4x` to `1.6x` (e.g., 14.sp text → 19.6-22.4.sp line height)
- Tight text: `1.3x` (e.g., 24.sp title → 31.2.sp)

---

## Alpha / Opacity Patterns

| Alpha | Usage |
|-------|-------|
| `0.1f` | Tinted backgrounds (task message, delete button bg) |
| `0.15f` | Priority badge bg, icon container tint, card shadows |
| `0.2f` | Status badge bg |
| `0.3f` | Subtle borders (badge outlines, task message border) |
| `0.4f` | Divider transparency |
| `0.5f` | Focus ring borders, FAB glow, empty state bg |

---

## Destructive / Delete Elements

```
// Delete button:
Surface {
    height = 48.dp
    color = ReactTheme.destructive.copy(alpha = 0.1f)
    border = 1.dp, ReactTheme.destructive.copy(alpha = 0.3f)
    shape = RoundedCornerShape(12.dp)
}
Text { 14.sp, Medium, color = ReactTheme.destructive }
```

---

## View Toggle (Segmented Control)

```
// Container:
Surface {
    color = ReactTheme.secondary
    shape = RoundedCornerShape(8.dp)
    padding = 2.dp
}

// Selected button:
Surface {
    size = 32.dp
    color = ReactTheme.card
    shape = RoundedCornerShape(6.dp)
    shadowElevation = 1.dp
}
Icon { 18.dp, color = ReactTheme.primary }

// Unselected button:
Surface { color = Color.Transparent }
Icon { 18.dp, color = ReactTheme.mutedForeground }
```

---

## Progress Bar

```
LinearProgressIndicator {
    height = 6.dp
    shape = RoundedCornerShape(9999.dp)   // fully rounded
    trackColor = ReactTheme.muted
    indicatorColor = ReactTheme.primary
}
```

---

## Send Button (Chat)

```
Surface {
    size = 48.dp
    color = ReactTheme.primary
    shape = RoundedCornerShape(12.dp)
}
Icon { 20.dp, color = primaryForeground }
```

---

## Message Input (Chat)

```
// Container Surface:
color = ReactTheme.card
top border = 1.dp, ReactTheme.border

// Input field:
Surface {
    minHeight = 48.dp
    color = ReactTheme.secondary
    shape = RoundedCornerShape(12.dp)
    padding = horizontal 16.dp, vertical 12.dp
}
// Placeholder: 15.sp, mutedForeground
```

---

## Reply Preview (Chat)

```
Surface {
    color = ReactTheme.secondary
    shape = RoundedCornerShape(8.dp)
    padding = 12.dp
}
// Left accent bar: 3.dp wide, primary color
// Sender: 12.sp, SemiBold, primary
// Message: 14.sp, mutedForeground
```

---

## Tag Chips (TaskEdit)

```
Surface {
    color = ReactTheme.secondary
    shape = RoundedCornerShape(8.dp)
    border = 1.dp, ReactTheme.border
}
Text { 13.sp, foreground }
Icon (remove) { 14.dp, mutedForeground }
```

---

## Task Status Indicator Bar

Left edge of task cards in list/kanban view:
```
Box {
    width = 4.dp
    fillMaxHeight
    color = statusColor  // gray/purple/emerald based on status
    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
}
```

---

## Checklist / Subtask Checkbox

```
Box {
    size = 20.dp
    shape = RoundedCornerShape(4.dp)
    // Unchecked:
    border = 2.dp, ReactTheme.border
    // Checked:
    color = ReactTheme.primary
    icon = Check, 14.dp, primaryForeground
}
```

---

## Kanban Column

```
width = 288.dp  // 18rem equivalent
```

Empty state:
```
Surface {
    color = ReactTheme.card.copy(alpha = 0.5f)
    border = 1.dp, ReactTheme.border
    shape = RoundedCornerShape(12.dp)
    padding = 24.dp
}
Text { 14.sp, mutedForeground, centered }
```

---

## Quick Reference: Element → Style

| Element | BG Color | Border | Corner | Padding |
|---------|----------|--------|--------|---------|
| Screen | background | — | — | horiz 16dp |
| Top bar | card | 1dp border (bottom) | none | 16×12 |
| Content card | card | 1dp border | 12dp | 12-16dp |
| Search bar | secondary | — | 12dp | 12-16dp |
| Filter chip | secondary/primary | — | 8dp | chip default |
| Input field | card | 1dp border/primary | 12dp | field default |
| Message bubble | card | 1dp border | 12dp | 12×8 |
| Badge (status) | status@0.2f | — | 16dp pill | 8-10×2-4 |
| Badge (priority) | color@0.15f | — | 4dp | 8×2 |
| FAB | primary | — | circle | — |
| Delete button | destructive@0.1f | 1dp destr@0.3f | 12dp | 16×8 |
| Tag chip | secondary | 1dp border | 8dp | 8×4 |
