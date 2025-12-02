package com.example.kosmos.features.users.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.shared.ui.components.KosmosDialogDefaults
import com.example.kosmos.shared.ui.components.KosmosDialogSurface
import com.example.kosmos.shared.ui.components.LoadingButton
import com.example.kosmos.shared.ui.components.SecondaryButton
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
/**
 * Dialog for adding a user to a project
 * Shows list of user's projects and role selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToProjectDialog(
    projects: List<Project>,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onAddToProject: (projectId: String, role: ProjectRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var selectedRole by remember { mutableStateOf(ProjectRole.MEMBER) }
    var showRolePicker by remember { mutableStateOf(false) }

    KosmosDialogSurface(
        onDismissRequest = onDismiss,
        modifier = modifier,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Title
        Text(
            text = "Add to Project",
            style = KosmosDialogDefaults.titleStyle,
            color = KosmosDialogDefaults.titleColor
        )

        if (error != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Tokens.CornerRadius.md),
                color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(Tokens.Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = "", tint = ColorTokens.ReactTheme.destructive)
                    Text(text = error, style = MaterialTheme.typography.bodySmall, color = ColorTokens.ReactTheme.destructive)
                }
            }
        }

        HorizontalDivider(color = KosmosDialogDefaults.dividerColor)

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorTokens.ReactTheme.primary)
                }
            }
            projects.isEmpty() -> { EmptyProjectsState() }
            else -> {
                Text(text = "Select a project", style = MaterialTheme.typography.titleSmall, color = KosmosDialogDefaults.subtitleColor)

                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                ) {
                    projects.forEach { project ->
                        ProjectItem(
                            project = project,
                            isSelected = selectedProjectId == project.id,
                            onClick = { selectedProjectId = project.id }
                        )
                    }
                }

                if (selectedProjectId != null) {
                    HorizontalDivider(color = KosmosDialogDefaults.dividerColor)
                    Text(text = "Select role", style = MaterialTheme.typography.titleSmall, color = KosmosDialogDefaults.subtitleColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
                        RoleChip(role = ProjectRole.MEMBER, isSelected = selectedRole == ProjectRole.MEMBER, onClick = { selectedRole = ProjectRole.MEMBER })
                        RoleChip(role = ProjectRole.MANAGER, isSelected = selectedRole == ProjectRole.MANAGER, onClick = { selectedRole = ProjectRole.MANAGER })
                    }
                    Text(text = "Note: ADMIN role can only be assigned by other admins in project settings.", style = MaterialTheme.typography.bodySmall, color = KosmosDialogDefaults.subtitleColor)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            SecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
            LoadingButton(
                text = "Add to Project",
                onClick = { selectedProjectId?.let { onAddToProject(it, selectedRole) } },
                isLoading = isLoading,
                modifier = Modifier.weight(1f),
                enabled = selectedProjectId != null && !isLoading
            )
        }
    }
}

/**
 * Project Item in list
 */
@Composable
private fun ProjectItem(
    project: Project,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
            } else {
                ColorTokens.ReactTheme.secondary
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, ColorTokens.ReactTheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) {
                        ColorTokens.ReactTheme.primaryForeground
                    } else {
                        ColorTokens.ReactTheme.foreground
                    }
                )

                if (project.description.isNotBlank()) {
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            ColorTokens.ReactTheme.primaryForeground.copy(alpha = 0.7f)
                        } else {
                            ColorTokens.ReactTheme.mutedForeground
                        },
                        maxLines = 2
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = ColorTokens.ReactTheme.primary
                )
            }
        }
    }
}

/**
 * Role Selection Chip
 */
@Composable
private fun RoleChip(
    role: ProjectRole,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = role.name,
                style = MaterialTheme.typography.labelLarge
            )
        },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "",
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        modifier = modifier
    )
}

/**
 * Empty State when user has no projects
 */
@Composable
private fun EmptyProjectsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.FolderOff,
                contentDescription = "",
                modifier = Modifier.size(64.dp),
                tint = ColorTokens.ReactTheme.mutedForeground
            )
            Text(
                text = "No Projects",
                style = MaterialTheme.typography.titleMedium,
                color = ColorTokens.ReactTheme.mutedForeground
            )
            Text(
                text = "You need to be a member of at least one project to add users.",
                style = MaterialTheme.typography.bodySmall,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
    }
}

