package com.example.kosmos.shared.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ValidationUtils.
 * Uses Robolectric because validateEmail/isEmailFormat use android.util.Patterns.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ValidationUtilsTest {

    // ─── validateProjectName ───────────────────────────────────────────────────

    @Test
    fun `validateProjectName - blank returns error`() {
        assertThat(ValidationUtils.validateProjectName("")).isNotNull()
        assertThat(ValidationUtils.validateProjectName("   ")).isNotNull()
    }

    @Test
    fun `validateProjectName - too short returns error`() {
        assertThat(ValidationUtils.validateProjectName("AB")).isNotNull()
    }

    @Test
    fun `validateProjectName - minimum boundary 3 chars is valid`() {
        assertThat(ValidationUtils.validateProjectName("ABC")).isNull()
    }

    @Test
    fun `validateProjectName - 101 chars returns error`() {
        assertThat(ValidationUtils.validateProjectName("A".repeat(101))).isNotNull()
    }

    @Test
    fun `validateProjectName - special chars not in allowed set returns error`() {
        assertThat(ValidationUtils.validateProjectName("My@Project!")).isNotNull()
    }

    @Test
    fun `validateProjectName - valid name returns null`() {
        assertThat(ValidationUtils.validateProjectName("My Cool Project")).isNull()
        assertThat(ValidationUtils.validateProjectName("proj_1-test")).isNull()
    }

    // ─── validatePassword ─────────────────────────────────────────────────────

    @Test
    fun `validatePassword - blank returns error`() {
        assertThat(ValidationUtils.validatePassword("")).isNotNull()
    }

    @Test
    fun `validatePassword - length less than 8 returns error`() {
        assertThat(ValidationUtils.validatePassword("Abc12")).isNotNull()
    }

    @Test
    fun `validatePassword - no uppercase returns error`() {
        assertThat(ValidationUtils.validatePassword("abcdefg1")).isNotNull()
    }

    @Test
    fun `validatePassword - no lowercase returns error`() {
        assertThat(ValidationUtils.validatePassword("ABCDEFG1")).isNotNull()
    }

    @Test
    fun `validatePassword - no digit returns error`() {
        assertThat(ValidationUtils.validatePassword("Abcdefgh")).isNotNull()
    }

    @Test
    fun `validatePassword - too long returns error`() {
        assertThat(ValidationUtils.validatePassword("Aa1" + "x".repeat(127))).isNotNull()
    }

    @Test
    fun `validatePassword - valid password returns null`() {
        assertThat(ValidationUtils.validatePassword("SecurePass1")).isNull()
    }

    // ─── validateEmail (optional) ─────────────────────────────────────────────

    @Test
    fun `validateEmail - blank is optional returns null`() {
        assertThat(ValidationUtils.validateEmail("")).isNull()
    }

    @Test
    fun `validateEmail - invalid format returns error`() {
        assertThat(ValidationUtils.validateEmail("not-an-email")).isNotNull()
    }

    @Test
    fun `validateEmail - valid email returns null`() {
        assertThat(ValidationUtils.validateEmail("user@example.com")).isNull()
    }

    // ─── validateRequiredEmail ────────────────────────────────────────────────

    @Test
    fun `validateRequiredEmail - blank returns error`() {
        assertThat(ValidationUtils.validateRequiredEmail("")).isNotNull()
    }

    @Test
    fun `validateRequiredEmail - invalid format returns error`() {
        assertThat(ValidationUtils.validateRequiredEmail("notanemail")).isNotNull()
    }

    @Test
    fun `validateRequiredEmail - email over 254 chars returns error`() {
        // 246 + "@test.com" (9) = 255 chars, which is > 254
        val longEmail = "a".repeat(246) + "@test.com"
        assertThat(ValidationUtils.validateRequiredEmail(longEmail)).isNotNull()
    }

    @Test
    fun `validateRequiredEmail - valid email returns null`() {
        assertThat(ValidationUtils.validateRequiredEmail("valid@example.com")).isNull()
    }

    @Test
    fun `validateRequiredEmail - blank with required=false returns null`() {
        assertThat(ValidationUtils.validateRequiredEmail("", required = false)).isNull()
    }

    // ─── validateGitHubUrl ────────────────────────────────────────────────────

    @Test
    fun `validateGitHubUrl - blank is optional returns null`() {
        assertThat(ValidationUtils.validateGitHubUrl("")).isNull()
    }

    @Test
    fun `validateGitHubUrl - user only path is valid`() {
        assertThat(ValidationUtils.validateGitHubUrl("github.com/octocat")).isNull()
    }

    @Test
    fun `validateGitHubUrl - user+repo path is valid`() {
        assertThat(ValidationUtils.validateGitHubUrl("https://github.com/octocat/hello-world")).isNull()
    }

    @Test
    fun `validateGitHubUrl - non-github domain returns error`() {
        assertThat(ValidationUtils.validateGitHubUrl("https://gitlab.com/user/repo")).isNotNull()
    }

    // ─── validateDeadline ────────────────────────────────────────────────────

    @Test
    fun `validateDeadline - null returns null`() {
        assertThat(ValidationUtils.validateDeadline(null)).isNull()
    }

    @Test
    fun `validateDeadline - past timestamp returns error`() {
        assertThat(ValidationUtils.validateDeadline(System.currentTimeMillis() - 1000)).isNotNull()
    }

    @Test
    fun `validateDeadline - within 1 hour returns error`() {
        assertThat(ValidationUtils.validateDeadline(System.currentTimeMillis() + 1800000)).isNotNull()
    }

    @Test
    fun `validateDeadline - more than 1 hour future returns null`() {
        assertThat(ValidationUtils.validateDeadline(System.currentTimeMillis() + 7200000)).isNull()
    }

    // ─── validateTags ────────────────────────────────────────────────────────

    @Test
    fun `validateTags - null returns null`() {
        assertThat(ValidationUtils.validateTags(null)).isNull()
    }

    @Test
    fun `validateTags - empty list returns null`() {
        assertThat(ValidationUtils.validateTags(emptyList())).isNull()
    }

    @Test
    fun `validateTags - more than 10 tags returns error`() {
        assertThat(ValidationUtils.validateTags(List(11) { "tag$it" })).isNotNull()
    }

    @Test
    fun `validateTags - tag over 30 chars returns error`() {
        assertThat(ValidationUtils.validateTags(listOf("a".repeat(31)))).isNotNull()
    }

    @Test
    fun `validateTags - blank tag returns error`() {
        assertThat(ValidationUtils.validateTags(listOf("valid", "  "))).isNotNull()
    }

    @Test
    fun `validateTags - valid list returns null`() {
        assertThat(ValidationUtils.validateTags(listOf("kotlin", "android", "compose"))).isNull()
    }

    // ─── validateProjectData integration ─────────────────────────────────────

    @Test
    fun `validateProjectData - tech category missing tech stack has error`() {
        val errors = ValidationUtils.validateProjectData(
            name = "My Tech Project",
            description = "A great tech project description",
            category = "tech",
            techStack = emptyList()
        )
        assertThat(errors).containsKey("techStack")
    }

    @Test
    fun `validateProjectData - social category missing motive has error`() {
        val errors = ValidationUtils.validateProjectData(
            name = "My Social Project",
            description = "A social project description here",
            category = "social",
            projectMotive = null
        )
        assertThat(errors).containsKey("projectMotive")
    }

    @Test
    fun `validateProjectData - business category missing business model has error`() {
        val errors = ValidationUtils.validateProjectData(
            name = "My Business Project",
            description = "A business project description here",
            category = "business",
            businessModel = null
        )
        assertThat(errors).containsKey("businessModel")
    }

    @Test
    fun `validateProjectData - valid inputs returns empty map`() {
        val errors = ValidationUtils.validateProjectData(
            name = "My Project",
            description = "A project with a nice description",
            category = "other"
        )
        assertThat(errors).isEmpty()
    }

    // ─── Format detection helpers ─────────────────────────────────────────────

    @Test
    fun `isUsernameFormat - starts with @ and valid chars returns true`() {
        assertThat(ValidationUtils.isUsernameFormat("@johndoe")).isTrue()
    }

    @Test
    fun `isUsernameFormat - no @ prefix returns false`() {
        assertThat(ValidationUtils.isUsernameFormat("johndoe")).isFalse()
    }

    @Test
    fun `isEmailFormat - valid email returns true`() {
        assertThat(ValidationUtils.isEmailFormat("user@example.com")).isTrue()
    }

    @Test
    fun `isEmailFormat - no @ returns false`() {
        assertThat(ValidationUtils.isEmailFormat("notanemail")).isFalse()
    }

    @Test
    fun `sanitizeSearchQuery - removes leading @ and trims`() {
        assertThat(ValidationUtils.sanitizeSearchQuery("  @johndoe  ")).isEqualTo("johndoe")
    }

    @Test
    fun `sanitizeSearchQuery - plain query just trimmed`() {
        assertThat(ValidationUtils.sanitizeSearchQuery("  hello  ")).isEqualTo("hello")
    }
}
