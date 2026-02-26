package com.example.kosmos.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.Project
import com.example.kosmos.core.models.ProjectStatus
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import kotlinx.coroutines.CancellationException

/**
 * Project Picker Dialog
 *
 * Reusable dialog for selecting a project from a list.
 * Features:
 * - Search functionality to filter projects
 * - Project color indicators
 * - Member and task counts
 * - Project status display
 * - Empty state when no projects found
 *
 * @param projects List of projects to display
 * @param title Dialog title
 * @param onProjectSelected Callback when project is selected (returns project ID)
 * @param onDismiss Callback when dialog is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectPickerDialog(
    projects: List<Project>,
    title: String = "Select Project",
    onProjectSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter projects by search query
    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) {
            projects
        } else {
            projects.filter { project ->
                project.name.contains(searchQuery, ignoreCase = true) ||
                project.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Only show active projects
    val activeProjects = remember(filteredProjects) {
        filteredProjects.filter { it.status == ProjectStatus.ACTIVE }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(Tokens.CornerRadius.lg),
            color = ColorTokens.ReactTheme.card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.lg)
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

                Spacer(modifier = Modifier.height(Tokens.Spacing.md))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search projects...",
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ColorTokens.ReactTheme.mutedForeground
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = ColorTokens.ReactTheme.mutedForeground
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(Tokens.CornerRadius.md),
                    colors = KosmosDialogDefaults.textFieldColors()
                )

                Spacer(modifier = Modifier.height(Tokens.Spacing.md))

                // Project list
                if (activeProjects.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Tokens.Spacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (searchQuery.isBlank()) "No projects available" else "No projects found",
                                style = TypographyTokens.typography.bodyMedium,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                    ) {
                        items(activeProjects, key = { it.id }) { project ->
                            ProjectPickerItem(
                                project = project,
                                onClick = { onProjectSelected(project.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Project Picker Item
 * Individual project row in the picker dialog
 */
@Composable
private fun ProjectPickerItem(
    project: Project,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Tokens.CornerRadius.md),
        color = ColorTokens.ReactTheme.secondary,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Project color indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(parseProjectColor(project.color)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Project info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
            ) {
                Text(
                    text = project.name,
                    style = TypographyTokens.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ColorTokens.ReactTheme.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (project.description.isNotBlank()) {
                    Text(
                        text = project.description,
                        style = TypographyTokens.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Project stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Member count
                    if (project.memberCount > 0) {
                        Text(
                            text = "${project.memberCount} members",
                            style = TypographyTokens.typography.labelSmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }

                    // Task count
                    if (project.taskCount > 0) {
                        Text(
                            text = "•",
                            style = TypographyTokens.typography.labelSmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                        Text(
                            text = "${project.taskCount} tasks",
                            style = TypographyTokens.typography.labelSmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }
            }
        }
    }
}

/**
 * Parse project color from hex string
 */
@Composable
private fun parseProjectColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        ColorTokens.ReactTheme.primary // Fallback color
    }
}
