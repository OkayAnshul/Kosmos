package com.example.kosmos.features.projects.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.data.repository.ProjectCreationData
import com.example.kosmos.features.project.presentation.SelectedMember
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens.Spacing

/**
 * Step 3: Review & Create
 *
 * Final wizard step where user reviews all project details and team members
 * before creating the project.
 *
 * Features:
 * - Project summary card (name, description, category, deadline, category-specific fields)
 * - Team members summary card (all selected members with roles)
 * - Edit buttons to jump back to previous steps
 * - Create button (handled by parent WizardNavigationButtons)
 * - Validation warnings if anything is incomplete
 *
 * @param projectData Project creation data to review
 * @param selectedMembers Selected team members to review
 * @param currentUserName Display name of user creating project (owner)
 * @param onEditDetails Callback to edit project details (jump to step 1)
 * @param onEditMembers Callback to edit team members (jump to step 2)
 */
@Composable
fun Step3ReviewCreate(
    projectData: ProjectCreationData?,
    selectedMembers: List<SelectedMember>,
    currentUserName: String,
    onEditDetails: () -> Unit,
    onEditMembers: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Header
        Text(
            text = "Review Your Project",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground
        )

        Text(
            text = "Double-check everything looks good before creating your project. You can edit details later if needed.",
            style = MaterialTheme.typography.bodyMedium,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        HorizontalDivider(color = ColorTokens.ReactTheme.border)

        // Validation warnings (if any)
        if (projectData == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, ColorTokens.ReactTheme.destructive.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.destructive
                    )
                    Column {
                        Text(
                            text = "Missing Project Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ColorTokens.ReactTheme.destructive
                        )
                        Text(
                            text = "Please go back and fill in the required project information.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }
            }
        } else {
            // Project Summary Section
            ProjectSummarySection(
                data = projectData,
                onEdit = onEditDetails
            )
        }

        // Members Summary Section
        MembersSummarySection(
            members = selectedMembers,
            ownerName = currentUserName,
            onEdit = onEditMembers
        )

        HorizontalDivider(color = ColorTokens.ReactTheme.border)

        // Info Cards
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            // What happens next
            InfoCard(
                icon = Icons.Default.RocketLaunch,
                title = "What Happens Next?",
                description = "Your project will be created instantly. Team members will receive invites and can join once they accept.",
                containerColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.1f),
                borderColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.3f),
                iconTint = ColorTokens.ReactTheme.primary
            )

            // Privacy info
            InfoCard(
                icon = Icons.Default.Lock,
                title = "Project Privacy",
                description = "This project is PRIVATE by default. Only you and invited members can access it. You can change visibility settings later.",
                containerColor = WizardColors.amber.copy(alpha = 0.1f),
                borderColor = WizardColors.amber.copy(alpha = 0.3f),
                iconTint = WizardColors.amber
            )

            // Offline mode info (if applicable)
            InfoCard(
                icon = Icons.Default.CloudOff,
                title = "Offline Mode",
                description = "Project will be created locally and synced to cloud when you're back online.",
                containerColor = ColorTokens.ReactTheme.card,
                borderColor = ColorTokens.ReactTheme.border,
                iconTint = ColorTokens.ReactTheme.mutedForeground
            )
        }

        HorizontalDivider(color = ColorTokens.ReactTheme.border)

        // Summary stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryStat(
                icon = Icons.Default.People,
                label = "Team Members",
                value = "${selectedMembers.size + 1}" // +1 for owner
            )

            SummaryStat(
                icon = Icons.Default.Category,
                label = "Category",
                value = projectData?.category?.getDisplayName() ?: "N/A"
            )

            projectData?.deadline?.let { deadline ->
                val daysUntil = ((deadline - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                SummaryStat(
                    icon = Icons.Default.CalendarToday,
                    label = "Deadline",
                    value = if (daysUntil > 0) "$daysUntil days" else "Today"
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // Final reminder
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = WizardColors.emerald.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, WizardColors.emerald.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = WizardColors.emerald,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Ready to Create!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WizardColors.emerald
                    )
                    Text(
                        text = "Everything looks good. Click 'Create Project' below to get started.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }
        }
    }
}

/**
 * Info card component for displaying helpful information
 */
@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    containerColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = iconTint
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }
        }
    }
}

/**
 * Summary stat component for quick project overview
 */
@Composable
private fun SummaryStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ColorTokens.ReactTheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ColorTokens.ReactTheme.foreground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ColorTokens.ReactTheme.mutedForeground
        )
    }
}
