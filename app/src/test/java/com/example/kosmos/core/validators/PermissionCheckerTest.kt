package com.example.kosmos.core.validators

import com.example.kosmos.core.models.Permission
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.testutil.TestFixtures
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for PermissionChecker.
 * Pure JUnit4 — no Android deps required.
 */
@RunWith(JUnit4::class)
class PermissionCheckerTest {

    // ─── Role default permissions ──────────────────────────────────────────────

    @Test
    fun `ADMIN has DELETE_PROJECT permission by default`() {
        val admin = TestFixtures.projectMember(role = ProjectRole.ADMIN)
        assertThat(PermissionChecker.hasPermission(admin, Permission.DELETE_PROJECT).isGranted()).isTrue()
    }

    @Test
    fun `ADMIN has EDIT_PROJECT permission by default`() {
        val admin = TestFixtures.projectMember(role = ProjectRole.ADMIN)
        assertThat(PermissionChecker.hasPermission(admin, Permission.EDIT_PROJECT).isGranted()).isTrue()
    }

    @Test
    fun `MANAGER has CREATE_TASKS permission by default`() {
        val manager = TestFixtures.projectMember(role = ProjectRole.MANAGER)
        assertThat(PermissionChecker.hasPermission(manager, Permission.CREATE_TASKS).isGranted()).isTrue()
    }

    @Test
    fun `MANAGER does not have DELETE_PROJECT permission by default`() {
        val manager = TestFixtures.projectMember(role = ProjectRole.MANAGER)
        assertThat(PermissionChecker.hasPermission(manager, Permission.DELETE_PROJECT).isGranted()).isFalse()
    }

    @Test
    fun `MEMBER has VIEW_PROJECT permission by default`() {
        val member = TestFixtures.projectMember(role = ProjectRole.MEMBER)
        assertThat(PermissionChecker.hasPermission(member, Permission.VIEW_PROJECT).isGranted()).isTrue()
    }

    @Test
    fun `MEMBER does not have DELETE_PROJECT permission by default`() {
        val member = TestFixtures.projectMember(role = ProjectRole.MEMBER)
        assertThat(PermissionChecker.hasPermission(member, Permission.DELETE_PROJECT).isGranted()).isFalse()
    }

    // ─── hasPermission ────────────────────────────────────────────────────────

    @Test
    fun `hasPermission - granted returns Granted`() {
        val member = TestFixtures.projectMember(role = ProjectRole.MEMBER)
        val result = PermissionChecker.hasPermission(member, Permission.VIEW_PROJECT)
        assertThat(result).isInstanceOf(PermissionChecker.PermissionResult.Granted::class.java)
    }

    @Test
    fun `hasPermission - denied returns Denied with reason`() {
        val member = TestFixtures.projectMember(role = ProjectRole.MEMBER)
        val result = PermissionChecker.hasPermission(member, Permission.DELETE_PROJECT)
        assertThat(result).isInstanceOf(PermissionChecker.PermissionResult.Denied::class.java)
        assertThat((result as PermissionChecker.PermissionResult.Denied).reason).isNotEmpty()
    }

    // ─── hasAllPermissions ────────────────────────────────────────────────────

    @Test
    fun `hasAllPermissions - all present returns Granted`() {
        val admin = TestFixtures.projectMember(role = ProjectRole.ADMIN)
        val result = PermissionChecker.hasAllPermissions(
            admin, listOf(Permission.VIEW_PROJECT, Permission.EDIT_PROJECT)
        )
        assertThat(result.isGranted()).isTrue()
    }

    @Test
    fun `hasAllPermissions - one missing returns Denied with missing list`() {
        val member = TestFixtures.projectMember(role = ProjectRole.MEMBER)
        val result = PermissionChecker.hasAllPermissions(
            member, listOf(Permission.VIEW_PROJECT, Permission.DELETE_PROJECT)
        )
        assertThat(result.isGranted()).isFalse()
        assertThat((result as PermissionChecker.PermissionResult.Denied).reason).contains("Delete")
    }

    // ─── hasAnyPermission ────────────────────────────────────────────────────

    @Test
    fun `hasAnyPermission - has one of many returns Granted`() {
        val member = TestFixtures.projectMember(role = ProjectRole.MEMBER)
        val result = PermissionChecker.hasAnyPermission(
            member, listOf(Permission.DELETE_PROJECT, Permission.VIEW_PROJECT)
        )
        assertThat(result.isGranted()).isTrue()
    }

    @Test
    fun `hasAnyPermission - has none returns Denied`() {
        val member = TestFixtures.projectMember(role = ProjectRole.MEMBER)
        val result = PermissionChecker.hasAnyPermission(
            member, listOf(Permission.DELETE_PROJECT, Permission.EDIT_PROJECT)
        )
        assertThat(result.isGranted()).isFalse()
    }

    // ─── getEffectivePermissions ──────────────────────────────────────────────

    @Test
    fun `getEffectivePermissions - valid custom JSON overrides role defaults`() {
        val member = TestFixtures.projectMember(
            role = ProjectRole.MEMBER,
            customPermissions = """["DELETE_PROJECT", "EDIT_PROJECT"]"""
        )
        val perms = PermissionChecker.getEffectivePermissions(member)
        assertThat(perms).contains(Permission.DELETE_PROJECT)
        assertThat(perms).contains(Permission.EDIT_PROJECT)
        // Should NOT contain VIEW_PROJECT (not in custom JSON)
        assertThat(perms).doesNotContain(Permission.VIEW_PROJECT)
    }

    @Test
    fun `getEffectivePermissions - invalid JSON falls back to role defaults`() {
        val member = TestFixtures.projectMember(
            role = ProjectRole.ADMIN,
            customPermissions = "{ not valid json ["
        )
        val perms = PermissionChecker.getEffectivePermissions(member)
        // Should fall back to ADMIN defaults
        assertThat(perms).contains(Permission.DELETE_PROJECT)
    }

    @Test
    fun `getEffectivePermissions - unknown permission name skipped`() {
        val member = TestFixtures.projectMember(
            role = ProjectRole.MEMBER,
            customPermissions = """["VIEW_PROJECT", "DOES_NOT_EXIST"]"""
        )
        val perms = PermissionChecker.getEffectivePermissions(member)
        assertThat(perms).contains(Permission.VIEW_PROJECT)
        assertThat(perms).hasSize(1)
    }

    // ─── requirePermission ────────────────────────────────────────────────────

    @Test
    fun `requirePermission - granted does not throw`() {
        val admin = TestFixtures.projectMember(role = ProjectRole.ADMIN)
        PermissionChecker.requirePermission(admin, Permission.DELETE_PROJECT)
        // No exception = pass
    }

    @Test(expected = PermissionChecker.PermissionDeniedException::class)
    fun `requirePermission - denied throws PermissionDeniedException`() {
        val member = TestFixtures.projectMember(role = ProjectRole.MEMBER)
        PermissionChecker.requirePermission(member, Permission.DELETE_PROJECT)
    }

    // ─── Actions convenience methods ─────────────────────────────────────────

    @Test
    fun `Actions canEditProject - admin returns true`() {
        val admin = TestFixtures.projectMember(role = ProjectRole.ADMIN)
        assertThat(PermissionChecker.Actions.canEditProject(admin)).isTrue()
    }

    @Test
    fun `Actions canEditProject - member returns false`() {
        val member = TestFixtures.projectMember(role = ProjectRole.MEMBER)
        assertThat(PermissionChecker.Actions.canEditProject(member)).isFalse()
    }

    @Test
    fun `Actions canCreateTasks - member returns true`() {
        val member = TestFixtures.projectMember(role = ProjectRole.MEMBER)
        assertThat(PermissionChecker.Actions.canCreateTasks(member)).isTrue()
    }

    @Test
    fun `Actions canDeleteProject - member returns false`() {
        val member = TestFixtures.projectMember(role = ProjectRole.MEMBER)
        assertThat(PermissionChecker.Actions.canDeleteProject(member)).isFalse()
    }
}
