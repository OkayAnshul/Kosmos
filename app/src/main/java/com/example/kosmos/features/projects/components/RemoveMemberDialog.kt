package com.example.kosmos.features.projects.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.shared.ui.components.DestructiveButton
import com.example.kosmos.shared.ui.components.KosmosDialogDefaults
import com.example.kosmos.shared.ui.components.KosmosDialogSurface
import com.example.kosmos.shared.ui.components.SecondaryButton
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Confirmation dialog for removing a member from a project
 * Stitch design: Navy background, destructive action warning
 */
@Composable
fun RemoveMemberDialog(
    member: ProjectMember,
    memberName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    KosmosDialogSurface(onDismissRequest = onDismiss) {
        // Warning icon
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = ColorTokens.ReactTheme.destructive,
                modifier = Modifier.size(48.dp)
            )
        }

        Text(
            text = "Remove Member?",
            style = KosmosDialogDefaults.titleStyle,
            color = KosmosDialogDefaults.titleColor
        )

        Text(
            text = "Are you sure you want to remove $memberName from this project?\n\nThey will lose access to all project resources, tasks, and conversations.",
            style = MaterialTheme.typography.bodyMedium,
            color = KosmosDialogDefaults.subtitleColor,
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            SecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            DestructiveButton(
                text = "Remove",
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
