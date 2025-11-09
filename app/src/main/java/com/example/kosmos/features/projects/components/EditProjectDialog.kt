package com.example.kosmos.features.projects.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectStatus

/**
 * Dialog for editing existing project details
 * Allows updating name, description, and status (Active/Archived)
 */
@Composable
fun EditProjectDialog(
    project: Project,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, status: ProjectStatus) -> Unit,
    isLoading: Boolean = false
) {
    var name by remember { mutableStateOf(project.name) }
    var description by remember { mutableStateOf(project.description) }
    var status by remember { mutableStateOf(project.status) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Column {
                Text("Edit Project")
                Text(
                    text = "Update project details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Project Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Project Name *") },
                    placeholder = { Text("Enter project name") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Project name is required") }
                    } else null,
                    enabled = !isLoading,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Project Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("What is this project about?") },
                    enabled = !isLoading,
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                // Project Status Section
                Text(
                    text = "Project Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Active Status Option
                StatusOption(
                    title = "Active",
                    description = "Project is active and visible to members",
                    icon = Icons.Default.CheckCircle,
                    selected = status == ProjectStatus.ACTIVE,
                    onClick = { if (!isLoading) status = ProjectStatus.ACTIVE },
                    enabled = !isLoading,
                    color = MaterialTheme.colorScheme.primary
                )

                // Archived Status Option
                StatusOption(
                    title = "Archived",
                    description = "Project is archived and read-only",
                    icon = Icons.Default.Archive,
                    selected = status == ProjectStatus.ARCHIVED,
                    onClick = { if (!isLoading) status = ProjectStatus.ARCHIVED },
                    enabled = !isLoading,
                    color = MaterialTheme.colorScheme.tertiary
                )

                if (status == ProjectStatus.ARCHIVED) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Archiving will make this project read-only for all members.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onSave(name.trim(), description.trim(), status)
                    }
                },
                enabled = !isLoading
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Status option card for selecting project status
 */
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
            containerColor = if (selected) {
                color.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                color
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) color else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
