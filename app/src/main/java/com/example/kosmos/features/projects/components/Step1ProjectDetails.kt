package com.example.kosmos.features.projects.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.core.models.ProjectCategory
import com.example.kosmos.data.repository.ProjectCreationData
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens.Spacing

/**
 * Step 1: Project Details
 *
 * Features:
 * - Basic info: name, description, category
 * - Dynamic fields based on selected category:
 *   - TECH: GitHub URL, tech stack, license
 *   - SOCIAL: Motive, target audience, impact tags
 *   - BUSINESS: Website, business model, industry tags
 *   - OTHER: General motive, custom tags
 * - Common optional fields: deadline, color, tags
 * - Real-time validation with inline errors
 *
 * @param projectData Current project data (may be null or partial)
 * @param validationErrors Map of field names to error messages
 * @param onDataUpdate Callback when any field changes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step1ProjectDetails(
    projectData: ProjectCreationData?,
    validationErrors: Map<String, String>,
    onDataUpdate: (ProjectCreationData) -> Unit
) {
    // Local state for form fields
    var projectName by remember { mutableStateOf(projectData?.name ?: "") }
    var description by remember { mutableStateOf(projectData?.description ?: "") }
    var category by remember { mutableStateOf(projectData?.category ?: ProjectCategory.OTHER) }
    var deadline by remember { mutableStateOf(projectData?.deadline) }
    var websiteUrl by remember { mutableStateOf(projectData?.websiteUrl ?: "") }
    var githubUrl by remember { mutableStateOf(projectData?.githubUrl ?: "") }
    var projectMotive by remember { mutableStateOf(projectData?.projectMotive ?: "") }
    var techStack by remember { mutableStateOf(projectData?.techStack ?: emptyList()) }
    var tags by remember { mutableStateOf(projectData?.tags ?: emptyList()) }
    var businessModel by remember { mutableStateOf(projectData?.businessModel ?: "") }
    var targetAudience by remember { mutableStateOf(projectData?.targetAudience ?: "") }
    var industryTags by remember { mutableStateOf(projectData?.industryTags ?: emptyList()) }
    var openSourceLicense by remember { mutableStateOf(projectData?.openSourceLicense) }
    var color by remember { mutableStateOf(projectData?.color ?: "#6366F1") }

    // Shared text field colors
    val textFieldColors = wizardTextFieldColors(
    )
    val textFieldShape = RoundedCornerShape(12.dp)

    // Update parent whenever local state changes
    val updateData = {
        onDataUpdate(
            ProjectCreationData(
                name = projectName,
                description = description,
                ownerId = "", // Will be set by ViewModel
                category = category,
                deadline = deadline,
                websiteUrl = websiteUrl.ifBlank { null },
                githubUrl = githubUrl.ifBlank { null },
                projectMotive = projectMotive.ifBlank { null },
                techStack = techStack.takeIf { it.isNotEmpty() },
                tags = tags.takeIf { it.isNotEmpty() },
                businessModel = businessModel.ifBlank { null },
                targetAudience = targetAudience.ifBlank { null },
                industryTags = industryTags.takeIf { it.isNotEmpty() },
                openSourceLicense = openSourceLicense,
                color = color
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Section 1: Basic Information
        Text(
            text = "Basic Information",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground
        )

        // Project Name
        OutlinedTextField(
            value = projectName,
            onValueChange = {
                projectName = it
                updateData()
            },
            label = { Text("Project Name *") },
            isError = validationErrors.containsKey("name"),
            supportingText = {
                if (validationErrors.containsKey("name")) {
                    Text(
                        text = validationErrors["name"] ?: "",
                        color = ColorTokens.ReactTheme.destructive
                    )
                } else {
                    Text(
                        text = "3-100 characters, alphanumeric + spaces",
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            },
            singleLine = true,
            colors = textFieldColors,
            shape = textFieldShape,
            modifier = Modifier.fillMaxWidth()
        )

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                updateData()
            },
            label = { Text("Description *") },
            isError = validationErrors.containsKey("description"),
            supportingText = {
                if (validationErrors.containsKey("description")) {
                    Text(
                        text = validationErrors["description"] ?: "",
                        color = ColorTokens.ReactTheme.destructive
                    )
                } else {
                    Text(
                        text = "${description.length}/500 characters (min 10)",
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            },
            minLines = 3,
            maxLines = 5,
            colors = textFieldColors,
            shape = textFieldShape,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(color = ColorTokens.ReactTheme.border)

        // Section 2: Category Selection
        Text(
            text = "Project Category",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground
        )

        CategorySelector(
            selectedCategory = category,
            onCategorySelect = {
                category = it
                updateData()
            }
        )

        Text(
            text = "Category determines which fields are required",
            style = MaterialTheme.typography.bodySmall,
            color = ColorTokens.ReactTheme.mutedForeground
        )

        HorizontalDivider(color = ColorTokens.ReactTheme.border)

        // Section 3: Category-Specific Fields (Animated)
        AnimatedContent(
            targetState = category,
            transitionSpec = {
                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
            },
            label = "category_fields"
        ) { currentCategory ->
            when (currentCategory) {
                ProjectCategory.TECH -> TechCategoryFields(
                    githubUrl = githubUrl,
                    onGithubUrlChange = {
                        githubUrl = it
                        updateData()
                    },
                    techStack = techStack,
                    onTechStackChange = {
                        techStack = it
                        updateData()
                    },
                    openSourceLicense = openSourceLicense,
                    onLicenseChange = {
                        openSourceLicense = it
                        updateData()
                    },
                    validationErrors = validationErrors
                )

                ProjectCategory.SOCIAL -> SocialCategoryFields(
                    projectMotive = projectMotive,
                    onMotiveChange = {
                        projectMotive = it
                        updateData()
                    },
                    targetAudience = targetAudience,
                    onTargetAudienceChange = {
                        targetAudience = it
                        updateData()
                    },
                    validationErrors = validationErrors
                )

                ProjectCategory.BUSINESS -> BusinessCategoryFields(
                    websiteUrl = websiteUrl,
                    onWebsiteUrlChange = {
                        websiteUrl = it
                        updateData()
                    },
                    businessModel = businessModel,
                    onBusinessModelChange = {
                        businessModel = it
                        updateData()
                    },
                    industryTags = industryTags,
                    onIndustryTagsChange = {
                        industryTags = it
                        updateData()
                    },
                    validationErrors = validationErrors
                )

                ProjectCategory.OTHER -> OtherCategoryFields(
                    projectMotive = projectMotive,
                    onMotiveChange = {
                        projectMotive = it
                        updateData()
                    }
                )
            }
        }

        HorizontalDivider(color = ColorTokens.ReactTheme.border)

        // Section 4: Common Optional Fields
        Text(
            text = "Additional Details (Optional)",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground
        )

        // Deadline Picker
        DeadlinePicker(
            deadline = deadline,
            onDeadlineSelect = {
                deadline = it
                updateData()
            }
        )

        // General Tags
        TagInputField(
            tags = tags,
            onTagsUpdate = {
                tags = it
                updateData()
            },
            label = "General Tags",
            maxTags = 10
        )

        // Color indicator (future enhancement)
        Text(
            text = "More customization options coming soon",
            style = MaterialTheme.typography.bodySmall,
            color = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.padding(vertical = Spacing.sm)
        )
    }
}

// ---------- Category-Specific Field Sections ----------

/**
 * Fields specific to TECH category
 */
@Composable
private fun TechCategoryFields(
    githubUrl: String,
    onGithubUrlChange: (String) -> Unit,
    techStack: List<String>,
    onTechStackChange: (List<String>) -> Unit,
    openSourceLicense: String?,
    onLicenseChange: (String?) -> Unit,
    validationErrors: Map<String, String>
) {
    val textFieldColors = wizardTextFieldColors(
    )
    val textFieldShape = RoundedCornerShape(12.dp)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = "Technology Project Details",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground
        )

        // GitHub URL
        OutlinedTextField(
            value = githubUrl,
            onValueChange = onGithubUrlChange,
            label = { Text("GitHub Repository URL") },
            isError = validationErrors.containsKey("githubUrl"),
            supportingText = {
                if (validationErrors.containsKey("githubUrl")) {
                    Text(
                        text = validationErrors["githubUrl"] ?: "",
                        color = ColorTokens.ReactTheme.destructive
                    )
                } else {
                    Text(
                        text = "Format: github.com/username/repo",
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            },
            placeholder = { Text("https://github.com/username/repo") },
            singleLine = true,
            colors = textFieldColors,
            shape = textFieldShape,
            modifier = Modifier.fillMaxWidth()
        )

        // Tech Stack Selector
        TechStackSelector(
            selectedTech = techStack,
            onTechUpdate = onTechStackChange
        )
        if (validationErrors.containsKey("techStack")) {
            Text(
                text = validationErrors["techStack"] ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = ColorTokens.ReactTheme.destructive
            )
        }

        // License Dropdown
        LicenseDropdown(
            selectedLicense = openSourceLicense,
            onLicenseSelect = onLicenseChange
        )
    }
}

/**
 * Fields specific to SOCIAL category
 */
@Composable
private fun SocialCategoryFields(
    projectMotive: String,
    onMotiveChange: (String) -> Unit,
    targetAudience: String,
    onTargetAudienceChange: (String) -> Unit,
    validationErrors: Map<String, String>
) {
    val textFieldColors = wizardTextFieldColors(
    )
    val textFieldShape = RoundedCornerShape(12.dp)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = "Social/Community Project Details",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground
        )

        // Project Motive
        OutlinedTextField(
            value = projectMotive,
            onValueChange = onMotiveChange,
            label = { Text("Project Motive / Social Impact *") },
            isError = validationErrors.containsKey("projectMotive"),
            supportingText = {
                if (validationErrors.containsKey("projectMotive")) {
                    Text(
                        text = validationErrors["projectMotive"] ?: "",
                        color = ColorTokens.ReactTheme.destructive
                    )
                } else {
                    Text(
                        text = "Describe the social impact and goals (20-500 characters)",
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            },
            placeholder = { Text("e.g., Helping local communities access clean water...") },
            minLines = 3,
            maxLines = 6,
            colors = textFieldColors,
            shape = textFieldShape,
            modifier = Modifier.fillMaxWidth()
        )

        // Target Audience
        OutlinedTextField(
            value = targetAudience,
            onValueChange = onTargetAudienceChange,
            label = { Text("Target Audience / Beneficiaries") },
            isError = validationErrors.containsKey("targetAudience"),
            supportingText = {
                if (validationErrors.containsKey("targetAudience")) {
                    Text(
                        text = validationErrors["targetAudience"] ?: "",
                        color = ColorTokens.ReactTheme.destructive
                    )
                }
            },
            placeholder = { Text("e.g., Rural communities in Southeast Asia") },
            singleLine = true,
            colors = textFieldColors,
            shape = textFieldShape,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Fields specific to BUSINESS category
 */
@Composable
private fun BusinessCategoryFields(
    websiteUrl: String,
    onWebsiteUrlChange: (String) -> Unit,
    businessModel: String,
    onBusinessModelChange: (String) -> Unit,
    industryTags: List<String>,
    onIndustryTagsChange: (List<String>) -> Unit,
    validationErrors: Map<String, String>
) {
    val textFieldColors = wizardTextFieldColors(
    )
    val textFieldShape = RoundedCornerShape(12.dp)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = "Business Project Details",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground
        )

        // Website URL
        OutlinedTextField(
            value = websiteUrl,
            onValueChange = onWebsiteUrlChange,
            label = { Text("Company Website") },
            isError = validationErrors.containsKey("websiteUrl"),
            supportingText = {
                if (validationErrors.containsKey("websiteUrl")) {
                    Text(
                        text = validationErrors["websiteUrl"] ?: "",
                        color = ColorTokens.ReactTheme.destructive
                    )
                } else {
                    Text(
                        text = "Your company or product website",
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            },
            placeholder = { Text("https://example.com") },
            singleLine = true,
            colors = textFieldColors,
            shape = textFieldShape,
            modifier = Modifier.fillMaxWidth()
        )

        // Business Model
        OutlinedTextField(
            value = businessModel,
            onValueChange = onBusinessModelChange,
            label = { Text("Business Model *") },
            isError = validationErrors.containsKey("businessModel"),
            supportingText = {
                if (validationErrors.containsKey("businessModel")) {
                    Text(
                        text = validationErrors["businessModel"] ?: "",
                        color = ColorTokens.ReactTheme.destructive
                    )
                } else {
                    Text(
                        text = "e.g., SaaS, Marketplace, Subscription, Freemium",
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            },
            placeholder = { Text("Describe your revenue model") },
            minLines = 2,
            maxLines = 4,
            colors = textFieldColors,
            shape = textFieldShape,
            modifier = Modifier.fillMaxWidth()
        )

        // Industry Tags
        TagInputField(
            tags = industryTags,
            onTagsUpdate = onIndustryTagsChange,
            label = "Industry Tags",
            maxTags = 5
        )
        if (validationErrors.containsKey("industryTags")) {
            Text(
                text = validationErrors["industryTags"] ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = ColorTokens.ReactTheme.destructive
            )
        }
    }
}

/**
 * Fields specific to OTHER category
 */
@Composable
private fun OtherCategoryFields(
    projectMotive: String,
    onMotiveChange: (String) -> Unit
) {
    val textFieldColors = wizardTextFieldColors(
    )
    val textFieldShape = RoundedCornerShape(12.dp)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(
            text = "General Project Details",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground
        )

        OutlinedTextField(
            value = projectMotive,
            onValueChange = onMotiveChange,
            label = { Text("Additional Details (Optional)") },
            placeholder = { Text("Describe your project goals and objectives...") },
            minLines = 3,
            maxLines = 6,
            colors = textFieldColors,
            shape = textFieldShape,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
