package com.example.kosmos.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * List Components for Kosmos App
 *
 * List items, section headers, and list patterns.
 * All items meet 56dp minimum height for touch accessibility.
 *
 * Components:
 * - ListItemStandard: Standard list item with icon and text
 * - ListItemTwoLine: List item with primary and secondary text
 * - ListItemThreeLine: List item with title, subtitle, and meta
 * - SectionHeader: Section divider with title
 * - ListItemWithSwitch: List item with toggle switch
 * - ListItemWithCheckbox: List item with checkbox
 */

/**
 * Standard List Item - Single Line
 *
 * Basic list item with optional leading/trailing content
 * Minimum height: 56dp
 *
 * @param text Primary text
 * @param onClick Click handler
 * @param modifier Modifier
 * @param leadingIcon Optional leading icon
 * @param leadingContent Optional custom leading content
 * @param trailingIcon Optional trailing icon
 * @param trailingContent Optional custom trailing content
 * @param enabled Whether item is enabled
 */
@Composable
fun ListItemStandard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Tokens.TouchTarget.listItemMinHeight)
                .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Leading content
            when {
                leadingContent != null -> leadingContent()
                leadingIcon != null -> Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(Tokens.Size.iconMedium)
                )
            }

            // Text
            Text(
                text = text,
                style = TypographyTokens.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Trailing content
            when {
                trailingContent != null -> trailingContent()
                trailingIcon != null -> Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(Tokens.Size.iconMedium)
                )
            }
        }
    }
}

/**
 * Two-Line List Item
 *
 * List item with primary and secondary text
 * Minimum height: 56dp
 *
 * @param primaryText Primary text (title)
 * @param secondaryText Secondary text (subtitle)
 * @param onClick Click handler
 * @param modifier Modifier
 * @param leadingIcon Optional leading icon
 * @param leadingContent Optional custom leading content (e.g., avatar)
 * @param trailingIcon Optional trailing icon
 * @param trailingContent Optional custom trailing content
 */
@Composable
fun ListItemTwoLine(
    primaryText: String,
    secondaryText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Tokens.TouchTarget.listItemMinHeight)
                .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Leading content
            when {
                leadingContent != null -> Box(
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    leadingContent()
                }
                leadingIcon != null -> Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(Tokens.Size.iconMedium)
                        .align(Alignment.Top)
                )
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
            ) {
                Text(
                    text = primaryText,
                    style = TypographyTokens.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = secondaryText,
                    style = TypographyTokens.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Trailing content
            when {
                trailingContent != null -> Box(
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    trailingContent()
                }
                trailingIcon != null -> Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(Tokens.Size.iconMedium)
                        .align(Alignment.Top)
                )
            }
        }
    }
}

/**
 * Three-Line List Item
 *
 * List item with title, subtitle, and metadata
 * Minimum height: 88dp
 *
 * @param title Title text
 * @param subtitle Subtitle text
 * @param metadata Metadata text (timestamp, count, etc.)
 * @param onClick Click handler
 * @param modifier Modifier
 * @param leadingContent Optional custom leading content (e.g., large avatar)
 * @param trailingContent Optional custom trailing content
 */
@Composable
fun ListItemThreeLine(
    title: String,
    subtitle: String,
    metadata: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Leading content
            if (leadingContent != null) {
                Box(modifier = Modifier.align(Alignment.Top)) {
                    leadingContent()
                }
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
            ) {
                Text(
                    text = title,
                    style = TypographyTokens.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = TypographyTokens.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metadata,
                    style = TypographyTokens.Custom.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Trailing content
            if (trailingContent != null) {
                Box(modifier = Modifier.align(Alignment.Top)) {
                    trailingContent()
                }
            }
        }
    }
}

/**
 * Section Header
 *
 * Divider with section title
 * Used to group list items
 *
 * @param title Section title
 * @param modifier Modifier
 * @param action Optional action button/icon
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = TypographyTokens.Custom.overline,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (action != null) {
                action()
            }
        }
    }
}

/**
 * List Item with Switch
 *
 * List item with toggle switch on the right
 * Common for settings and preferences
 *
 * @param text Primary text
 * @param checked Whether switch is checked
 * @param onCheckedChange Switch toggle handler
 * @param modifier Modifier
 * @param secondaryText Optional secondary text
 * @param leadingIcon Optional leading icon
 * @param enabled Whether item is enabled
 */
@Composable
fun ListItemWithSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = { if (enabled) onCheckedChange(!checked) },
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Tokens.TouchTarget.listItemMinHeight)
                .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Leading icon
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(Tokens.Size.iconMedium)
                )
            }

            // Text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
            ) {
                Text(
                    text = text,
                    style = TypographyTokens.typography.bodyLarge
                )
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        style = TypographyTokens.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Switch
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

/**
 * List Item with Checkbox
 *
 * List item with checkbox for multi-select
 *
 * @param text Primary text
 * @param checked Whether checkbox is checked
 * @param onCheckedChange Checkbox toggle handler
 * @param modifier Modifier
 * @param secondaryText Optional secondary text
 * @param leadingIcon Optional leading icon
 */
@Composable
fun ListItemWithCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    leadingIcon: ImageVector? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Tokens.TouchTarget.listItemMinHeight)
                .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Checkbox
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )

            // Leading icon
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(Tokens.Size.iconMedium)
                )
            }

            // Text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
            ) {
                Text(
                    text = text,
                    style = TypographyTokens.typography.bodyLarge
                )
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        style = TypographyTokens.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Divider List Item
 *
 * Horizontal divider for lists
 *
 * @param modifier Modifier
 * @param hasInset Whether divider has left inset
 */
@Composable
fun ListDivider(
    modifier: Modifier = Modifier,
    hasInset: Boolean = true
) {
    HorizontalDivider(
        modifier = if (hasInset) {
            modifier.padding(start = Tokens.Spacing.xl)
        } else {
            modifier
        }
    )
}

/**
 * Avatar List Item
 *
 * List item with circular avatar
 * Common for user lists, contacts
 *
 * @param primaryText Primary text (name)
 * @param secondaryText Secondary text (email, status)
 * @param onClick Click handler
 * @param modifier Modifier
 * @param avatarContent Avatar content (image or initials)
 * @param trailingContent Optional trailing content
 * @param showOnlineIndicator Whether to show online status dot
 * @param isOnline Whether user is online
 */
@Composable
fun AvatarListItem(
    primaryText: String,
    secondaryText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarContent: @Composable () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
    showOnlineIndicator: Boolean = false,
    isOnline: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Tokens.TouchTarget.listItemMinHeight)
                .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
        ) {
            // Avatar with optional online indicator
            Box {
                avatarContent()

                if (showOnlineIndicator) {
                    Surface(
                        modifier = Modifier
                            .size(Tokens.Size.statusDotWithBorder)
                            .align(Alignment.BottomEnd),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = if (isOnline)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outlineVariant
                            ) {}
                        }
                    }
                }
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
            ) {
                Text(
                    text = primaryText,
                    style = TypographyTokens.Custom.userDisplayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = secondaryText,
                    style = TypographyTokens.Custom.userSecondaryInfo,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Trailing content
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

/**
 * Expandable List Item
 *
 * List item that expands to show additional content
 *
 * @param title Item title
 * @param onClick Click handler
 * @param modifier Modifier
 * @param expanded Whether item is expanded
 * @param leadingIcon Optional leading icon
 * @param content Expandable content
 */
@Composable
fun ExpandableListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    leadingIcon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Tokens.TouchTarget.listItemMinHeight)
                    .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(Tokens.Size.iconMedium)
                    )
                }

                Text(
                    text = title,
                    style = TypographyTokens.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (expanded)
                        IconSet.Direction.expandLess
                    else
                        IconSet.Direction.expandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (leadingIcon != null) Tokens.Spacing.xxl else Tokens.Spacing.md,
                        end = Tokens.Spacing.md,
                        bottom = Tokens.Spacing.md
                    ),
                content = content
            )
        }
    }
}
