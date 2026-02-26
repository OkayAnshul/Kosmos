package com.example.kosmos.shared.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.Permission
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.validators.PermissionChecker

/**
 * Wraps content with a permission check.
 * If user lacks [permission]: dims content to 40%, shows lock icon overlay,
 * and on tap/long-press shows PermissionDeniedBottomSheet.
 * If user has permission: renders content normally.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PermissionGated(
    permission: Permission,
    currentMember: ProjectMember?,
    action: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val permissionResult = remember(currentMember, permission) {
        if (currentMember == null) {
            PermissionChecker.PermissionResult.Denied("You are not a member of this project.")
        } else {
            PermissionChecker.hasPermission(currentMember, permission)
        }
    }

    val hasPermission = permissionResult.isGranted()
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet && !hasPermission) {
        PermissionDeniedBottomSheet(
            action = action,
            reason = permissionResult.getDeniedReason() ?: "You don't have permission.",
            onDismiss = { showSheet = false }
        )
    }

    Box(
        modifier = modifier.then(
            if (!hasPermission) {
                Modifier
                    .alpha(0.4f)
                    .combinedClickable(
                        onClick = { showSheet = true },
                        onLongClick = { showSheet = true }
                    )
            } else {
                Modifier
            }
        )
    ) {
        content()
        if (!hasPermission) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Permission required",
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}
