package com.example.kosmos.shared.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Input Components for Kosmos App
 *
 * Form inputs, search bars, and chips optimized for mobile.
 * All inputs follow Material 3 design with consistent styling.
 *
 * Components:
 * - TextFieldStandard: Standard text input
 * - TextFieldPassword: Password input with show/hide
 * - TextFieldMultiline: Multiline text area
 * - SearchBar: Search input with clear button
 * - ChipGroup: Horizontal chip collection
 * - InputChip: Single input chip with remove
 */

/**
 * Standard Text Field
 *
 * Material 3 text field with consistent styling
 *
 * @param value Current text value
 * @param onValueChange Value change handler
 * @param label Field label
 * @param modifier Modifier
 * @param placeholder Placeholder text
 * @param leadingIcon Optional leading icon
 * @param trailingIcon Optional trailing icon
 * @param supportingText Helper/error text below field
 * @param isError Whether field has error
 * @param enabled Whether field is enabled
 * @param readOnly Whether field is read-only
 * @param singleLine Whether field is single line
 * @param maxLines Maximum lines (if not single line)
 * @param keyboardType Keyboard type
 * @param imeAction IME action
 * @param onImeAction IME action handler
 */
@Composable
fun TextFieldStandard(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else null,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null
                )
            }
        } else null,
        trailingIcon = if (trailingIcon != null) {
            {
                if (onTrailingIconClick != null) {
                    IconButton(onClick = onTrailingIconClick) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null
                        )
                    }
                } else {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null
                    )
                }
            }
        } else null,
        supportingText = if (supportingText != null) {
            { Text(supportingText) }
        } else null,
        isError = isError,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = TypographyTokens.Custom.inputField,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction?.invoke() },
            onGo = { onImeAction?.invoke() },
            onSearch = { onImeAction?.invoke() },
            onSend = { onImeAction?.invoke() }
        )
    )
}

/**
 * Password Text Field
 *
 * Password input with show/hide toggle
 *
 * @param value Current password value
 * @param onValueChange Value change handler
 * @param label Field label
 * @param modifier Modifier
 * @param supportingText Helper/error text
 * @param isError Whether field has error
 * @param imeAction IME action
 * @param onImeAction IME action handler
 */
@Composable
fun TextFieldPassword(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = IconSet.Settings.privacy,
                contentDescription = null
            )
        },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible)
                        IconSet.Visibility.visible
                    else
                        IconSet.Visibility.invisible,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        supportingText = if (supportingText != null) {
            { Text(supportingText) }
        } else null,
        isError = isError,
        visualTransformation = if (passwordVisible)
            VisualTransformation.None
        else
            PasswordVisualTransformation(),
        singleLine = true,
        textStyle = TypographyTokens.Custom.inputField,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction?.invoke() }
        )
    )
}

/**
 * Multiline Text Field
 *
 * Text area for longer content (descriptions, messages, etc.)
 *
 * @param value Current text value
 * @param onValueChange Value change handler
 * @param label Field label
 * @param modifier Modifier
 * @param placeholder Placeholder text
 * @param minLines Minimum visible lines
 * @param maxLines Maximum lines before scrolling
 * @param supportingText Helper/error text
 * @param isError Whether field has error
 * @param showCharacterCount Whether to show character count
 * @param maxCharacters Maximum characters allowed
 */
@Composable
fun TextFieldMultiline(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minLines: Int = 3,
    maxLines: Int = 5,
    supportingText: String? = null,
    isError: Boolean = false,
    showCharacterCount: Boolean = false,
    maxCharacters: Int = 500
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.length <= maxCharacters) {
                onValueChange(newValue)
            }
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else null,
        supportingText = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (supportingText != null) {
                    Text(supportingText)
                } else {
                    Spacer(modifier = Modifier.width(0.dp))
                }
                if (showCharacterCount) {
                    Text(
                        "${value.length}/$maxCharacters",
                        style = TypographyTokens.Custom.caption
                    )
                }
            }
        },
        isError = isError,
        singleLine = false,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = TypographyTokens.Custom.inputField
    )
}

/**
 * Search Bar
 *
 * Search input with search icon and clear button
 * Optimized for search UX with instant clear action
 *
 * @param query Current search query
 * @param onQueryChange Query change handler
 * @param modifier Modifier
 * @param placeholder Placeholder text
 * @param onSearch Search submit handler
 * @param active Whether search bar is in active/focused state
 * @param onActiveChange Active state change handler
 */
@Composable
fun SearchBarStandard(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    onSearch: ((String) -> Unit)? = null,
    active: Boolean = false,
    onActiveChange: ((Boolean) -> Unit)? = null
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = IconSet.Action.search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = IconSet.Action.clear,
                        contentDescription = "Clear"
                    )
                }
            }
        },
        singleLine = true,
        textStyle = TypographyTokens.Custom.searchField,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch?.invoke(query)
                onActiveChange?.invoke(false)
            }
        ),
        shape = MaterialTheme.shapes.extraLarge
    )
}

/**
 * Chip Group
 *
 * Horizontal scrollable collection of chips
 * Used for tags, filters, selections
 *
 * @param chips List of chip labels
 * @param selectedChips Set of selected chip labels
 * @param onChipClick Chip click handler
 * @param modifier Modifier
 * @param multiSelect Whether multiple chips can be selected
 */
@Composable
fun ChipGroup(
    chips: List<String>,
    selectedChips: Set<String>,
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = Tokens.Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
    ) {
        chips.forEach { chip ->
            val isSelected = chip in selectedChips
            FilterChip(
                selected = isSelected,
                onClick = { onChipClick(chip) },
                label = {
                    Text(
                        text = chip,
                        style = TypographyTokens.Custom.chipLabel
                    )
                }
            )
        }
    }
}

/**
 * Input Chip
 *
 * Chip with remove button
 * Used for tag input, multi-select displays
 *
 * @param label Chip label
 * @param onRemove Remove button click handler
 * @param modifier Modifier
 * @param leadingIcon Optional leading icon
 */
@Composable
fun InputChipStandard(
    label: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    InputChip(
        selected = false,
        onClick = { },
        label = {
            Text(
                text = label,
                style = TypographyTokens.Custom.chipLabel
            )
        },
        modifier = modifier,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(Tokens.Size.iconSmall)
                )
            }
        } else null,
        trailingIcon = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(Tokens.Size.iconMedium)
            ) {
                Icon(
                    imageVector = IconSet.Action.clear,
                    contentDescription = "Remove",
                    modifier = Modifier.size(Tokens.Size.iconSmall)
                )
            }
        }
    )
}

/**
 * Tag Input Field
 *
 * Text field that creates chips when Enter is pressed
 * Common pattern for tag/keyword input
 *
 * @param tags Current list of tags
 * @param onTagsChange Tags change handler
 * @param label Field label
 * @param modifier Modifier
 * @param placeholder Placeholder text
 */
@Composable
fun TagInputField(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Add tags..."
) {
    var inputValue by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        // Tag chips
        if (tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Tokens.Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
            ) {
                tags.forEach { tag ->
                    InputChipStandard(
                        label = tag,
                        onRemove = {
                            onTagsChange(tags - tag)
                        }
                    )
                }
            }
        }

        // Input field
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (inputValue.isNotBlank()) {
                        onTagsChange(tags + inputValue.trim())
                        inputValue = ""
                    }
                }
            ),
            trailingIcon = {
                if (inputValue.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onTagsChange(tags + inputValue.trim())
                            inputValue = ""
                        }
                    ) {
                        Icon(
                            imageVector = IconSet.Action.add,
                            contentDescription = "Add tag"
                        )
                    }
                }
            }
        )
    }
}

/**
 * Flow Row for wrapping chips
 * Simple implementation - for production use accompanist FlowRow
 */
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simplified flow row - in production, use Accompanist FlowRow
    // or wait for Compose Foundation FlowRow (coming in future versions)
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}

/**
 * Counter Input
 *
 * Numeric input with +/- buttons
 * Common for quantity selection
 *
 * @param value Current value
 * @param onValueChange Value change handler
 * @param modifier Modifier
 * @param min Minimum value
 * @param max Maximum value
 * @param step Increment/decrement step
 * @param label Optional label
 */
@Composable
fun CounterInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 100,
    step: Int = 1,
    label: String? = null
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = TypographyTokens.Custom.inputLabel,
                modifier = Modifier.padding(bottom = Tokens.Spacing.xs)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            IconButton(
                onClick = {
                    val newValue = (value - step).coerceAtLeast(min)
                    onValueChange(newValue)
                },
                enabled = value > min
            ) {
                Icon(
                    imageVector = IconSet.Action.clear,
                    contentDescription = "Decrease"
                )
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .widthIn(min = 60.dp)
                    .heightIn(min = Tokens.TouchTarget.minimum)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = Tokens.Spacing.md)
                ) {
                    Text(
                        text = value.toString(),
                        style = TypographyTokens.typography.titleMedium
                    )
                }
            }

            IconButton(
                onClick = {
                    val newValue = (value + step).coerceAtMost(max)
                    onValueChange(newValue)
                },
                enabled = value < max
            ) {
                Icon(
                    imageVector = IconSet.Action.add,
                    contentDescription = "Increase"
                )
            }
        }
    }
}
