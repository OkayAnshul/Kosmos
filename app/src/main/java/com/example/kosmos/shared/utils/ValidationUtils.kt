package com.example.kosmos.shared.utils

import android.util.Patterns
import java.util.regex.Pattern

/**
 * Validation utilities for project creation wizard
 * Provides consistent validation logic across all wizard steps
 *
 * Usage: Call validate methods which return null for valid input,
 * or error message string for invalid input
 */
object ValidationUtils {

    // Constants
    private const val MIN_PROJECT_NAME_LENGTH = 3
    private const val MAX_PROJECT_NAME_LENGTH = 100
    private const val MIN_DESCRIPTION_LENGTH = 10
    private const val MAX_DESCRIPTION_LENGTH = 500

    // Regex patterns
    private val GITHUB_URL_PATTERN = Pattern.compile(
        "^(https?://)?(www\\.)?github\\.com/[a-zA-Z0-9_-]+(/[a-zA-Z0-9_-]+)?/?$",
        Pattern.CASE_INSENSITIVE
    )

    private val USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_-]{3,30}$"
    )

    /**
     * Validate project name
     * Requirements: 3-100 characters, alphanumeric + spaces allowed
     *
     * @param name Project name to validate
     * @return Error message or null if valid
     */
    fun validateProjectName(name: String): String? {
        return when {
            name.isBlank() -> "Project name is required"
            name.length < MIN_PROJECT_NAME_LENGTH ->
                "Name must be at least $MIN_PROJECT_NAME_LENGTH characters"
            name.length > MAX_PROJECT_NAME_LENGTH ->
                "Name must be less than $MAX_PROJECT_NAME_LENGTH characters"
            !name.trim().matches(Regex("^[a-zA-Z0-9 _-]+$")) ->
                "Name can only contain letters, numbers, spaces, hyphens, and underscores"
            else -> null
        }
    }

    /**
     * Validate project description
     * Requirements: 10-500 characters
     *
     * @param description Project description to validate
     * @return Error message or null if valid
     */
    fun validateDescription(description: String): String? {
        return when {
            description.isBlank() -> "Description is required"
            description.length < MIN_DESCRIPTION_LENGTH ->
                "Description must be at least $MIN_DESCRIPTION_LENGTH characters"
            description.length > MAX_DESCRIPTION_LENGTH ->
                "Description must be less than $MAX_DESCRIPTION_LENGTH characters"
            else -> null
        }
    }

    /**
     * Validate generic URL
     * Checks for valid URL format
     *
     * @param url URL to validate
     * @return Error message or null if valid
     */
    fun validateUrl(url: String): String? {
        if (url.isBlank()) return null // Optional field

        return when {
            !Patterns.WEB_URL.matcher(url).matches() -> "Invalid URL format"
            !url.startsWith("http://") && !url.startsWith("https://") ->
                "URL must start with http:// or https://"
            else -> null
        }
    }

    /**
     * Validate GitHub URL
     * Checks for valid GitHub repository URL format
     * Accepts: github.com/username or github.com/username/repo
     *
     * @param url GitHub URL to validate
     * @return Error message or null if valid
     */
    fun validateGitHubUrl(url: String): String? {
        if (url.isBlank()) return null // Optional field

        return when {
            !GITHUB_URL_PATTERN.matcher(url).matches() ->
                "Invalid GitHub URL. Format: github.com/username or github.com/username/repo"
            else -> null
        }
    }

    /**
     * Validate email address
     * Uses Android's built-in email pattern
     *
     * @param email Email to validate
     * @return Error message or null if valid
     */
    fun validateEmail(email: String): String? {
        if (email.isBlank()) return null // Optional field

        return when {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
    }

    /**
     * Validate deadline timestamp
     * Checks that deadline is in the future
     *
     * @param deadline Timestamp in milliseconds (nullable)
     * @return Error message or null if valid
     */
    fun validateDeadline(deadline: Long?): String? {
        if (deadline == null) return null // Optional field

        val now = System.currentTimeMillis()
        return when {
            deadline <= now -> "Deadline must be in the future"
            deadline < now + 3600000 -> "Deadline must be at least 1 hour from now"
            else -> null
        }
    }

    /**
     * Validate project motive (for SOCIAL category)
     * Requirements: 20-500 characters
     *
     * @param motive Project motive text
     * @return Error message or null if valid
     */
    fun validateProjectMotive(motive: String): String? {
        return when {
            motive.isBlank() -> "Project motive is required for social projects"
            motive.length < 20 -> "Motive must be at least 20 characters"
            motive.length > 500 -> "Motive must be less than 500 characters"
            else -> null
        }
    }

    /**
     * Validate business model (for BUSINESS category)
     * Requirements: Non-empty, reasonable length
     *
     * @param model Business model text
     * @return Error message or null if valid
     */
    fun validateBusinessModel(model: String): String? {
        return when {
            model.isBlank() -> "Business model is required for business projects"
            model.length < 10 -> "Business model must be at least 10 characters"
            model.length > 300 -> "Business model must be less than 300 characters"
            else -> null
        }
    }

    /**
     * Validate target audience
     * Requirements: Non-empty, reasonable length
     *
     * @param audience Target audience text
     * @return Error message or null if valid
     */
    fun validateTargetAudience(audience: String): String? {
        return when {
            audience.isBlank() -> "Target audience is required"
            audience.length < 5 -> "Target audience must be at least 5 characters"
            audience.length > 200 -> "Target audience must be less than 200 characters"
            else -> null
        }
    }

    /**
     * Validate tech stack selection (for TECH category)
     * Requirements: At least one technology selected
     *
     * @param techStack List of selected technologies
     * @return Error message or null if valid
     */
    fun validateTechStack(techStack: List<String>?): String? {
        return when {
            techStack.isNullOrEmpty() -> "Select at least one technology for tech projects"
            techStack.size > 20 -> "Maximum 20 technologies allowed"
            else -> null
        }
    }

    /**
     * Validate tags
     * Requirements: Optional, but if provided must be reasonable
     *
     * @param tags List of tags
     * @return Error message or null if valid
     */
    fun validateTags(tags: List<String>?): String? {
        if (tags.isNullOrEmpty()) return null // Optional field

        return when {
            tags.size > 10 -> "Maximum 10 tags allowed"
            tags.any { it.length > 30 } -> "Each tag must be less than 30 characters"
            tags.any { it.isBlank() } -> "Tags cannot be empty"
            else -> null
        }
    }

    /**
     * Validate industry tags (for BUSINESS category)
     * Requirements: Optional, but if provided must be reasonable
     *
     * @param industryTags List of industry tags
     * @return Error message or null if valid
     */
    fun validateIndustryTags(industryTags: List<String>?): String? {
        if (industryTags.isNullOrEmpty()) return null // Optional field

        return when {
            industryTags.size > 5 -> "Maximum 5 industry tags allowed"
            industryTags.any { it.isBlank() } -> "Industry tags cannot be empty"
            else -> null
        }
    }

    // ---------- Format Detection Utilities ----------

    /**
     * Check if text is in email format
     * Used for search query detection
     *
     * @param text Text to check
     * @return True if looks like email (contains @)
     */
    fun isEmailFormat(text: String): Boolean {
        return text.contains("@") && Patterns.EMAIL_ADDRESS.matcher(text).matches()
    }

    /**
     * Check if text is in username format
     * Used for search query detection
     * Username format: starts with @, alphanumeric + underscore/hyphen
     *
     * @param text Text to check
     * @return True if looks like username (starts with @)
     */
    fun isUsernameFormat(text: String): Boolean {
        if (!text.startsWith("@")) return false
        val withoutAt = text.substring(1)
        return USERNAME_PATTERN.matcher(withoutAt).matches()
    }

    /**
     * Check if text is a valid display name search
     * Display names can have spaces and more characters
     *
     * @param text Text to check
     * @return True if valid display name search
     */
    fun isDisplayNameFormat(text: String): Boolean {
        return text.isNotBlank() &&
               !text.startsWith("@") &&
               !text.contains("@") &&
               text.length >= 2
    }

    /**
     * Sanitize search query
     * Removes leading @ for username searches
     *
     * @param query Search query
     * @return Sanitized query
     */
    fun sanitizeSearchQuery(query: String): String {
        return query.trim().removePrefix("@")
    }

    // ---------- Comprehensive Validation ----------

    /**
     * Validate all project data based on category
     * Returns map of field names to error messages
     *
     * @param name Project name
     * @param description Project description
     * @param category Project category
     * @param deadline Optional deadline
     * @param websiteUrl Optional website URL
     * @param githubUrl Optional GitHub URL
     * @param projectMotive Optional project motive
     * @param techStack Optional tech stack
     * @param tags Optional tags
     * @param businessModel Optional business model
     * @param targetAudience Optional target audience
     * @param industryTags Optional industry tags
     * @return Map of field errors (empty if all valid)
     */
    fun validateProjectData(
        name: String,
        description: String,
        category: String,
        deadline: Long? = null,
        websiteUrl: String? = null,
        githubUrl: String? = null,
        projectMotive: String? = null,
        techStack: List<String>? = null,
        tags: List<String>? = null,
        businessModel: String? = null,
        targetAudience: String? = null,
        industryTags: List<String>? = null
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        // Always required fields
        validateProjectName(name)?.let { errors["name"] = it }
        validateDescription(description)?.let { errors["description"] = it }

        // Optional common fields
        deadline?.let { validateDeadline(it)?.let { error -> errors["deadline"] = error } }
        websiteUrl?.let { if (it.isNotBlank()) validateUrl(it)?.let { error -> errors["websiteUrl"] = error } }
        githubUrl?.let { if (it.isNotBlank()) validateGitHubUrl(it)?.let { error -> errors["githubUrl"] = error } }
        tags?.let { validateTags(it)?.let { error -> errors["tags"] = error } }

        // Category-specific validation
        when (category.lowercase()) {
            "tech" -> {
                validateTechStack(techStack)?.let { errors["techStack"] = it }
                githubUrl?.let {
                    if (it.isBlank()) errors["githubUrl"] = "GitHub URL recommended for tech projects"
                }
            }
            "social" -> {
                projectMotive?.let {
                    validateProjectMotive(it)?.let { error -> errors["projectMotive"] = error }
                } ?: run { errors["projectMotive"] = "Project motive is required for social projects" }

                targetAudience?.let {
                    if (it.isNotBlank()) validateTargetAudience(it)?.let { error -> errors["targetAudience"] = error }
                }
            }
            "business" -> {
                businessModel?.let {
                    validateBusinessModel(it)?.let { error -> errors["businessModel"] = error }
                } ?: run { errors["businessModel"] = "Business model is required for business projects" }

                industryTags?.let { validateIndustryTags(it)?.let { error -> errors["industryTags"] = error } }

                websiteUrl?.let {
                    if (it.isBlank()) errors["websiteUrl"] = "Website URL recommended for business projects"
                }
            }
            "other" -> {
                // No specific requirements for OTHER category
            }
        }

        return errors
    }

    /**
     * Check if URL is reachable (basic format check only)
     * Does not perform network request
     *
     * @param url URL to check
     * @return True if URL format is valid
     */
    fun isValidUrlFormat(url: String): Boolean {
        return validateUrl(url) == null
    }

    /**
     * Check if GitHub URL is valid (format only)
     *
     * @param url GitHub URL to check
     * @return True if GitHub URL format is valid
     */
    fun isValidGitHubUrlFormat(url: String): Boolean {
        return validateGitHubUrl(url) == null
    }

    // ---------- Authentication & Profile Validation ----------

    /**
     * Validate username for auth/profile
     * Rules: 3-30 chars, alphanumeric + underscore/dash
     *
     * @param username Username to validate
     * @param required Whether username is required
     * @return Error message or null if valid
     */
    fun validateUsername(username: String, required: Boolean = true): String? {
        return when {
            username.isBlank() && !required -> null
            username.isBlank() && required -> "Username is required"
            username.length < 3 -> "Username must be at least 3 characters"
            username.length > 30 -> "Username must be less than 30 characters"
            !USERNAME_PATTERN.matcher(username).matches() ->
                "Username can only contain letters, numbers, underscores, and dashes"
            else -> null
        }
    }

    /**
     * Validate password
     * Rules: 8-128 chars, at least one uppercase, one lowercase, one digit
     *
     * @param password Password to validate
     * @return Error message or null if valid
     */
    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            password.length < 8 -> "Password must be at least 8 characters"
            password.length > 128 -> "Password is too long"
            !password.any { it.isUpperCase() } -> "Password must contain at least one uppercase letter"
            !password.any { it.isLowerCase() } -> "Password must contain at least one lowercase letter"
            !password.any { it.isDigit() } -> "Password must contain at least one number"
            else -> null
        }
    }

    /**
     * Validate display name
     * Rules: 1-50 chars
     *
     * @param name Display name to validate
     * @param required Whether display name is required
     * @return Error message or null if valid
     */
    fun validateDisplayName(name: String, required: Boolean = true): String? {
        return when {
            name.isBlank() && !required -> null
            name.isBlank() && required -> "Display name is required"
            name.length > 50 -> "Display name must be less than 50 characters"
            else -> null
        }
    }

    /**
     * Validate bio/description for user profile
     * Rules: Optional, max 500 chars
     *
     * @param bio Bio text to validate
     * @return Error message or null if valid
     */
    fun validateBio(bio: String): String? {
        return when {
            bio.length > 500 -> "Bio must be less than 500 characters"
            else -> null
        }
    }

    /**
     * Validate phone number
     * Rules: Optional, E.164 format if provided
     *
     * @param phone Phone number to validate
     * @param required Whether phone is required
     * @return Error message or null if valid
     */
    fun validatePhone(phone: String, required: Boolean = false): String? {
        if (phone.isBlank()) {
            return if (required) "Phone number is required" else null
        }

        // E.164 format: +[country code][number], 7-15 digits total
        val phoneRegex = Regex("^\\+?[1-9]\\d{6,14}$")
        return when {
            !phoneRegex.matches(phone) -> "Invalid phone format (use +[country code][number])"
            else -> null
        }
    }

    /**
     * Validate required email (non-blank)
     *
     * @param email Email to validate
     * @param required Whether email is required
     * @return Error message or null if valid
     */
    fun validateRequiredEmail(email: String, required: Boolean = true): String? {
        if (email.isBlank()) {
            return if (required) "Email is required" else null
        }

        return when {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            email.length > 254 -> "Email is too long"
            else -> null
        }
    }

    // ---------- Task Validation ----------

    /**
     * Validate task title
     * Requirements: 1-200 characters
     *
     * @param title Task title to validate
     * @return Error message or null if valid
     */
    fun validateTaskTitle(title: String): String? {
        return when {
            title.isBlank() -> "Task title is required"
            title.length > 200 -> "Title must be less than 200 characters"
            else -> null
        }
    }

    /**
     * Validate task description
     * Requirements: Optional, max 2000 characters
     *
     * @param description Task description to validate
     * @return Error message or null if valid
     */
    fun validateTaskDescription(description: String): String? {
        if (description.isBlank()) return null // Optional field

        return when {
            description.length > 2000 -> "Description must be less than 2000 characters"
            else -> null
        }
    }

    // ---------- Chat Validation ----------

    /**
     * Validate chat/group name
     * Requirements: 1-100 characters for group chats
     *
     * @param name Chat name to validate
     * @param required Whether name is required (true for groups, false for direct chats)
     * @return Error message or null if valid
     */
    fun validateChatName(name: String, required: Boolean = true): String? {
        if (name.isBlank()) {
            return if (required) "Group name is required" else null
        }

        return when {
            name.length > 100 -> "Name must be less than 100 characters"
            else -> null
        }
    }

    /**
     * Validate message content
     * Requirements: Non-empty, max 5000 characters
     *
     * @param content Message content to validate
     * @return Error message or null if valid
     */
    fun validateMessageContent(content: String): String? {
        return when {
            content.isBlank() -> "Message cannot be empty"
            content.length > 5000 -> "Message is too long (max 5000 characters)"
            else -> null
        }
    }
}
