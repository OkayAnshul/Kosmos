package com.example.kosmos.features.projects.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.shared.ui.components.KosmosDialogDefaults
import com.example.kosmos.shared.ui.components.KosmosDialogSurface
import com.example.kosmos.shared.ui.components.PrimaryButton
import com.example.kosmos.shared.ui.components.SecondaryButton
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Dialog for changing a member's role in a project
 * Stitch design: Navy background, role chips with icons
 */
@Composable
fun ChangeRoleDialog(
    member: ProjectMember,
    memberName: String,
    currentRole: ProjectRole,
    onRoleSelected: (ProjectRole) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRole by remember { mutableStateOf(currentRole) }

    KosmosDialogSurface(onDismissRequest = onDismiss) {
        // Title
        Text(
            text = "Change Role",
            style = KosmosDialogDefaults.titleStyle,
            color = KosmosDialogDefaults.titleColor
        )
        Text(
            text = "Select a new role for $memberName",
            style = MaterialTheme.typography.bodyMedium,
            color = KosmosDialogDefaults.subtitleColor
        )

        HorizontalDivider(color = KosmosDialogDefaults.dividerColor)

        // Role options
        RoleOption(
            role = ProjectRole.ADMIN,
            icon = Icons.Default.AdminPanelSettings,
            title = "Admin",
            description = "Full control over project and members",
            isSelected = selectedRole == ProjectRole.ADMIN,
            onClick = { selectedRole = ProjectRole.ADMIN }
        )
        RoleOption(
            role = ProjectRole.MANAGER,
            icon = Icons.Default.Edit,
            title = "Manager",
            description = "Can manage tasks and members",
            isSelected = selectedRole == ProjectRole.MANAGER,
            onClick = { selectedRole = ProjectRole.MANAGER }
        )
        RoleOption(
            role = ProjectRole.MEMBER,
            icon = Icons.Default.Person,
            title = "Member",
            description = "Standard member with basic access",
            isSelected = selectedRole == ProjectRole.MEMBER,
            onClick = { selectedRole = ProjectRole.MEMBER }
        )

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
                    onRoleSelected(selectedRole)
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
                enabled = selectedRole != currentRole
            )
        }
    }
}

/**
 * Single role option with icon and description
 */
@Composable
private fun RoleOption(
    role: ProjectRole,
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
            } else {
                ColorTokens.ReactTheme.secondary
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, ColorTokens.ReactTheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) ColorTokens.ReactTheme.primary else ColorTokens.ReactTheme.mutedForeground,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Title and description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorTokens.ReactTheme.foreground,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            // Radio indicator
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = ColorTokens.ReactTheme.primary,
                    unselectedColor = ColorTokens.ReactTheme.mutedForeground
                )
            )
        }
    }
}
