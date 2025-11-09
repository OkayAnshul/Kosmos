package com.example.kosmos.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Card Components for Kosmos App
 *
 * Card variants optimized for mobile with gesture support.
 * All cards support swipe actions for power user efficiency.
 *
 * Components:
 * - StandardCard: Basic elevated card
 * - SwipeableCard: Card with left/right swipe actions
 * - CollapsibleCard: Expandable/collapsible card
 * - ActionCard: Card with built-in action buttons
 * - SelectableCard: Card with selection state
 */

/**
 * Standard Card - Basic Elevated Card
 *
 * Material 3 card with consistent elevation and padding
 *
 * @param modifier Modifier
 * @param onClick Optional click handler
 * @param enabled Whether card is enabled
 * @param elevation Card elevation level
 * @param content Card content
 */
@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = Tokens.Elevation.level1
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            elevation = elevation
        ) {
            Column(
                modifier = Modifier.padding(Tokens.Spacing.md),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier,
            elevation = elevation
        ) {
            Column(
                modifier = Modifier.padding(Tokens.Spacing.md),
                content = content
            )
        }
    }
}

/**
 * Swipeable Card - Card with Swipe Actions
 *
 * Supports left and right swipe gestures for quick actions.
 * Perfect for list items that need archive/delete/pin functionality.
 *
 * @param modifier Modifier
 * @param onSwipeLeft Action when swiped left (e.g., delete)
 * @param onSwipeRight Action when swiped right (e.g., archive)
 * @param swipeLeftIcon Icon for left swipe action
 * @param swipeRightIcon Icon for right swipe action
 * @param swipeLeftLabel Label for left swipe
 * @param swipeRightLabel Label for right swipe
 * @param onClick Optional click handler
 * @param content Card content
 */
@Composable
fun SwipeableCard(
    modifier: Modifier = Modifier,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    swipeLeftIcon: ImageVector? = null,
    swipeRightIcon: ImageVector? = null,
    swipeLeftLabel: String = "Delete",
    swipeRightLabel: String = "Archive",
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val threshold = Tokens.Gesture.swipeThreshold.value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val swipeDistance = offsetX.absoluteValue
                        if (swipeDistance > threshold) {
                            // Swipe completed
                            if (offsetX > 0 && onSwipeRight != null) {
                                onSwipeRight()
                            } else if (offsetX < 0 && onSwipeLeft != null) {
                                onSwipeLeft()
                            }
                        }
                        // Reset position
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = offsetX + dragAmount
                        // Limit swipe distance
                        offsetX = newOffset.coerceIn(-threshold * 2, threshold * 2)
                    }
                )
            }
    ) {
        // Background action indicators
        if (offsetX > 0 && onSwipeRight != null) {
            // Right swipe background (left side visible)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .alpha(0.8f),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = Tokens.Spacing.md)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (swipeRightIcon != null) {
                        Icon(
                            imageVector = swipeRightIcon,
                            contentDescription = swipeRightLabel,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                    Text(
                        text = swipeRightLabel,
                        style = TypographyTokens.Custom.chipLabel,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else if (offsetX < 0 && onSwipeLeft != null) {
            // Left swipe background (right side visible)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .alpha(0.8f),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = Tokens.Spacing.md)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = swipeLeftLabel,
                        style = TypographyTokens.Custom.chipLabel,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                    if (swipeLeftIcon != null) {
                        Icon(
                            imageVector = swipeLeftIcon,
                            contentDescription = swipeLeftLabel,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Card content with offset
        StandardCard(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth(),
            onClick = onClick,
            content = content
        )
    }
}

/**
 * Collapsible Card - Expandable Card
 *
 * Card that can be expanded/collapsed with animation.
 * Useful for showing/hiding additional details.
 *
 * @param title Card title (always visible)
 * @param modifier Modifier
 * @param initiallyExpanded Whether card starts expanded
 * @param onExpandChange Callback when expansion state changes
 * @param headerContent Optional content in header (badges, icons)
 * @param content Collapsible content
 */
@Composable
fun CollapsibleCard(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    onExpandChange: ((Boolean) -> Unit)? = null,
    headerContent: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(
            defaultElevation = Tokens.Elevation.level1
        )
    ) {
        Column {
            // Header (always visible, clickable)
            Surface(
                onClick = {
                    expanded = !expanded
                    onExpandChange?.invoke(expanded)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = TypographyTokens.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )

                    if (headerContent != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            content = headerContent
                        )
                    }

                    Icon(
                        imageVector = if (expanded)
                            com.example.kosmos.shared.ui.designsystem.IconSet.Direction.expandLess
                        else
                            com.example.kosmos.shared.ui.designsystem.IconSet.Direction.expandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(Tokens.Duration.medium)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(Tokens.Duration.medium)
                )
            ) {
                Column(
                    modifier = Modifier.padding(Tokens.Spacing.md),
                    content = content
                )
            }
        }
    }
}

/**
 * Action Card - Card with Action Buttons
 *
 * Card with built-in primary and secondary action buttons
 * Common pattern for confirmations, prompts, etc.
 *
 * @param title Card title
 * @param primaryAction Primary button text
 * @param onPrimaryClick Primary button click handler
 * @param modifier Modifier
 * @param secondaryAction Optional secondary button text
 * @param onSecondaryClick Optional secondary button click handler
 * @param icon Optional header icon
 * @param content Card content
 */
@Composable
fun ActionCard(
    title: String,
    primaryAction: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryAction: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(
            defaultElevation = Tokens.Elevation.level2
        )
    ) {
        Column(
            modifier = Modifier.padding(Tokens.Spacing.md)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(Tokens.Size.iconLarge),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = title,
                    style = TypographyTokens.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Tokens.Spacing.md))

            // Content
            Column(content = content)

            Spacer(modifier = Modifier.height(Tokens.Spacing.md))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
            ) {
                if (secondaryAction != null && onSecondaryClick != null) {
                    SecondaryButton(
                        text = secondaryAction,
                        onClick = onSecondaryClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                PrimaryButton(
                    text = primaryAction,
                    onClick = onPrimaryClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Selectable Card - Card with Selection State
 *
 * Card that shows selected state with checkmark and border
 * Useful for multi-select scenarios
 *
 * @param selected Whether card is selected
 * @param onSelectedChange Selection change handler
 * @param modifier Modifier
 * @param showCheckmark Whether to show checkmark when selected
 * @param content Card content
 */
@Composable
fun SelectableCard(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showCheckmark: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = { onSelectedChange(!selected) },
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (selected)
            androidx.compose.foundation.BorderStroke(
                width = Tokens.BorderWidth.medium,
                color = MaterialTheme.colorScheme.primary
            )
        else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) Tokens.Elevation.level2 else Tokens.Elevation.level1
        )
    ) {
        Box {
            Column(
                modifier = Modifier.padding(Tokens.Spacing.md),
                content = content
            )

            if (selected && showCheckmark) {
                Icon(
                    imageVector = com.example.kosmos.shared.ui.designsystem.IconSet.Status.checkmark,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Tokens.Spacing.xs)
                        .size(Tokens.Size.iconMedium)
                )
            }
        }
    }
}

/**
 * Info Card - Card with Icon and Title
 *
 * Card optimized for displaying information with icon
 * Common pattern for metrics, stats, summaries
 *
 * @param title Card title
 * @param value Main value/content
 * @param icon Leading icon
 * @param modifier Modifier
 * @param subtitle Optional subtitle
 * @param onClick Optional click handler
 */
@Composable
fun InfoCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    StandardCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(Tokens.Size.avatarMedium)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(Tokens.Size.iconMedium)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TypographyTokens.Custom.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = TypographyTokens.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = TypographyTokens.Custom.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Compact Card - Minimal Padding Card
 *
 * Card with reduced padding for dense layouts
 * Useful for list items that need less spacing
 *
 * @param modifier Modifier
 * @param onClick Optional click handler
 * @param content Card content
 */
@Composable
fun CompactCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            elevation = CardDefaults.cardElevation(
                defaultElevation = Tokens.Elevation.level1
            )
        ) {
            Column(
                modifier = Modifier.padding(Tokens.Spacing.sm),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(
                defaultElevation = Tokens.Elevation.level1
            )
        ) {
            Column(
                modifier = Modifier.padding(Tokens.Spacing.sm),
                content = content
            )
        }
    }
}
