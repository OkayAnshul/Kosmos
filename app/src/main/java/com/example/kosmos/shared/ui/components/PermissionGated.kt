package com.example.kosmos.shared.ui.components

import androidx.compose.runtime.*
import com.example.kosmos.core.models.Permission
import com.example.kosmos.core.models.ProjectMember
import com.example.kosmos.core.validators.PermissionChecker

/**
 * Wraps content with a permission check.
 * If user lacks [permission]: content is hidden entirely (clean, no lock/fade).
 * If user has permission: renders content normally.
 */
@Composable
fun PermissionGated(
    permission: Permission,
    currentMember: ProjectMember?,
    action: String,
    content: @Composable () -> Unit
) {
    val hasPermission = remember(currentMember, permission) {
        if (currentMember == null) false
        else PermissionChecker.hasPermission(currentMember, permission).isGranted()
    }

    if (hasPermission) {
        content()
    }
}
