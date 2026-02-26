package com.example.kosmos.features.projects.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectStatus
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

@Composable
fun EditProjectDialog(
    project: Project,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, status: ProjectStatus) -> Unit,
    onDelete: (() -> Unit)? = null,
    isLoading: Boolean = false
) {
    var name by remember { mutableStateOf(project.name) }
    var description by remember { mutableStateOf(project.description) }
    var status by remember { mutableStateOf(project.status) }
    var nameError by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    KosmosDialogSurface(
        onDismissRequest = { if (!isLoading) onDismiss() }
    ) {
        // Header
        Column {
            Text(
                text = "Edit Project",
                style = KosmosDialogDefaults.titleStyle,
                color = KosmosDialogDefaults.titleColor
            )
            Text(
                text = "Update project details",
                style = TypographyTokens.Custom.caption,
                color = KosmosDialogDefaults.subtitleColor
            )
        }

        HorizontalDivider(color = KosmosDialogDefaults.dividerColor)

        // Project Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = { Text("Project Name *") },
            placeholder = { Text("Enter project name") },
            isError = nameError,
            supportingText = if (nameError) { { Text("Project name is required") } } else null,
            enabled = !isLoading,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = KosmosDialogDefaults.textFieldColors(),
            shape = RoundedCornerShape(Tokens.CornerRadius.md)
        )

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            placeholder = { Text("What is this project about?") },
            enabled = !isLoading,
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            colors = KosmosDialogDefaults.textFieldColors(),
            shape = RoundedCornerShape(Tokens.CornerRadius.md)
        )

        HorizontalDivider(color = KosmosDialogDefaults.dividerColor)

        // Status
        Text(
            text = "Project Status",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ColorTokens.ReactTheme.primary
        )

        StatusOption(
            title = "Active",
            description = "Project is active and visible to members",
            icon = Icons.Default.CheckCircle,
            selected = status == ProjectStatus.ACTIVE,
            onClick = { if (!isLoading) status = ProjectStatus.ACTIVE },
            enabled = !isLoading,
            color = ColorTokens.ReactTheme.primary
        )

        StatusOption(
            title = "Archived",
            description = "Project is archived and read-only",
            icon = Icons.Default.Archive,
            selected = status == ProjectStatus.ARCHIVED,
            onClick = { if (!isLoading) status = ProjectStatus.ARCHIVED },
            enabled = !isLoading,
            color = ColorTokens.ReactTheme.accent
        )

        if (status == ProjectStatus.ARCHIVED) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Tokens.CornerRadius.md),
                color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Archive, contentDescription = "", tint = ColorTokens.ReactTheme.destructive, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Archiving will make this project read-only for all members.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTokens.ReactTheme.destructive
                    )
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Delete
        if (onDelete != null) {
            HorizontalDivider(color = KosmosDialogDefaults.dividerColor)
            DestructiveButton(
                text = "Delete Project",
                onClick = { if (!isLoading) showDeleteConfirmation = true },
                fullWidth = true,
                enabled = !isLoading
            )
            if (status != ProjectStatus.ARCHIVED) {
                Text(
                    text = "Tip: Archive the project instead of deleting to preserve data",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.7f)
                )
            }
        }

        // Save/Cancel
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
            PrimaryButton(
                text = "Save Changes",
                onClick = {
                    if (name.isBlank()) nameError = true
                    else onSave(name.trim(), description.trim(), status)
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
        }
    }

    // Delete Confirmation
    if (showDeleteConfirmation) {
        ConfirmationDialog(
            title = "Delete Project?",
            message = "Are you sure you want to permanently delete \"${project.name}\"? This action cannot be undone.",
            onConfirm = {
                onDelete?.invoke()
                showDeleteConfirmation = false
                onDismiss()
            },
            onDismiss = { showDeleteConfirmation = false },
            confirmText = "Delete Forever",
            dismissText = "Cancel",
            icon = Icons.Default.Warning,
            isDestructive = true
        )
    }
}

@Composable
private fun StatusOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) color.copy(alpha = 0.15f) else ColorTokens.ReactTheme.secondary
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, color)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, ColorTokens.ReactTheme.border.copy(alpha = 0.3f))
        },
        shape = RoundedCornerShape(Tokens.CornerRadius.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, contentDescription = "", tint = if (selected) color else ColorTokens.ReactTheme.mutedForeground, modifier = Modifier.size(24.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = if (selected) color else ColorTokens.ReactTheme.foreground)
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = ColorTokens.ReactTheme.mutedForeground)
                }
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = color, modifier = Modifier.size(24.dp))
            }
        }
    }
}
