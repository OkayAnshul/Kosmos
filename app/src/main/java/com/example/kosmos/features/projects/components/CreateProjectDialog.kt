package com.example.kosmos.features.projects.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.utils.ValidationUtils
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit,
    isLoading: Boolean = false
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    KosmosDialogSurface(
        onDismissRequest = { if (!isLoading) onDismiss() }
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            Surface(
                shape = RoundedCornerShape(Tokens.CornerRadius.md),
                color = ColorTokens.ReactTheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = IconSet.Action.add,
                    contentDescription = null,
                    tint = ColorTokens.ReactTheme.primary,
                    modifier = Modifier.padding(Tokens.Spacing.xs)
                )
            }
            Column {
                Text(
                    text = "Create New Project",
                    style = KosmosDialogDefaults.titleStyle,
                    color = KosmosDialogDefaults.titleColor
                )
                Text(
                    text = "Enter project details",
                    style = TypographyTokens.Custom.caption,
                    color = KosmosDialogDefaults.subtitleColor
                )
            }
        }

        HorizontalDivider(color = KosmosDialogDefaults.dividerColor)

        // Project Name
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = ValidationUtils.validateProjectName(it)
            },
            label = { Text("Project Name *") },
            supportingText = {
                if (nameError != null) {
                    Text(text = nameError!!, color = ColorTokens.ReactTheme.destructive)
                } else {
                    Text(text = "${name.length}/100 characters", color = ColorTokens.ReactTheme.mutedForeground)
                }
            },
            isError = nameError != null,
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = KosmosDialogDefaults.textFieldColors(),
            shape = RoundedCornerShape(Tokens.CornerRadius.md)
        )

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                descriptionError = if (it.isNotBlank()) ValidationUtils.validateDescription(it) else null
            },
            label = { Text("Description (optional)") },
            placeholder = { Text("Describe your project") },
            supportingText = {
                if (descriptionError != null) {
                    Text(text = descriptionError!!, color = ColorTokens.ReactTheme.destructive)
                } else if (description.isNotBlank()) {
                    Text(text = "${description.length}/500 characters", color = ColorTokens.ReactTheme.mutedForeground)
                }
            },
            isError = descriptionError != null,
            minLines = 3,
            maxLines = 5,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = KosmosDialogDefaults.textFieldColors(),
            shape = RoundedCornerShape(Tokens.CornerRadius.md)
        )

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            SecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
            LoadingButton(
                text = "Create Project",
                onClick = {
                    val nameValidation = ValidationUtils.validateProjectName(name)
                    val descValidation = if (description.isNotBlank()) ValidationUtils.validateDescription(description) else null
                    nameError = nameValidation
                    descriptionError = descValidation
                    if (nameValidation == null && descValidation == null) {
                        onCreate(name.trim(), description.trim())
                    }
                },
                isLoading = isLoading,
                modifier = Modifier.weight(1f),
                enabled = !isLoading && name.isNotBlank() && nameError == null && descriptionError == null
            )
        }
    }
}
