package com.example.kosmos.shared.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Tag Input Dialog
 *
 * Reusable dialog for managing tags (add/remove).
 * Features:
 * - Display current tags as chips with remove button
 * - Input field to add new tags
 * - Press Enter or click Add to add tag
 * - Click X on chip to remove tag
 * - Empty state message
 *
 * @param currentTags Current list of tags
 * @param title Dialog title
 * @param placeholder Input field placeholder
 * @param maxTags Maximum number of tags allowed (0 = unlimited)
 * @param onTagsUpdated Callback when tags are updated
 * @param onDismiss Callback when dialog is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagInputDialog(
    currentTags: List<String>,
    title: String = "Manage Tags",
    placeholder: String = "Enter tag...",
    maxTags: Int = 0,
    onTagsUpdated: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tags by remember { mutableStateOf(currentTags.toMutableList()) }
    var inputText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Add tag function
    val addTag: () -> Unit = {
        val trimmedTag = inputText.trim()
        when {
            trimmedTag.isBlank() -> {
                errorMessage = "Tag cannot be empty"
            }
            trimmedTag.length < 2 -> {
                errorMessage = "Tag must be at least 2 characters"
            }
            trimmedTag.length > 20 -> {
                errorMessage = "Tag must be less than 20 characters"
            }
            tags.contains(trimmedTag) -> {
                errorMessage = "Tag already exists"
            }
            maxTags > 0 && tags.size >= maxTags -> {
                errorMessage = "Maximum $maxTags tags allowed"
            }
            else -> {
                tags.add(trimmedTag)
                inputText = ""
                errorMessage = null
            }
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(Tokens.CornerRadius.lg),
            color = ColorTokens.ReactTheme.card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = TypographyTokens.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ColorTokens.ReactTheme.foreground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }

                // Tag count info
                if (maxTags > 0) {
                    Text(
                        text = "${tags.size} / $maxTags tags",
                        style = TypographyTokens.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }

                // Current tags display
                if (tags.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                    ) {
                        items(tags.toList(), key = { it }) { tag ->
                            TagChip(
                                tag = tag,
                                onRemove = {
                                    tags.remove(tag)
                                    errorMessage = null
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No tags added yet",
                        style = TypographyTokens.typography.bodyMedium,
                        color = ColorTokens.ReactTheme.mutedForeground,
                        modifier = Modifier.padding(vertical = Tokens.Spacing.sm)
                    )
                }

                // Input field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                placeholder,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        },
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let {
                            {
                                Text(
                                    text = it,
                                    color = ColorTokens.Error.light
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { addTag() }
                        ),
                        shape = RoundedCornerShape(Tokens.CornerRadius.md),
                        colors = KosmosDialogDefaults.textFieldColors()
                    )

                    // Add button
                    IconButton(
                        onClick = addTag,
                        enabled = inputText.isNotBlank() && (maxTags == 0 || tags.size < maxTags),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add tag",
                            tint = if (inputText.isNotBlank() && (maxTags == 0 || tags.size < maxTags))
                                ColorTokens.ReactTheme.primary
                            else
                                ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Tokens.Spacing.sm))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                ) {
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "Save",
                        onClick = {
                            onTagsUpdated(tags)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Tag Chip Component
 * Displays a tag with remove button
 */
@Composable
private fun TagChip(
    tag: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        color = ColorTokens.ReactTheme.primary.copy(alpha = 0.15f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(
                start = Tokens.Spacing.md,
                end = Tokens.Spacing.sm,
                top = Tokens.Spacing.sm,
                bottom = Tokens.Spacing.sm
            ),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag,
                style = TypographyTokens.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = ColorTokens.ReactTheme.primary
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove tag",
                    modifier = Modifier.size(16.dp),
                    tint = ColorTokens.ReactTheme.primary
                )
            }
        }
    }
}
