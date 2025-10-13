package com.example.kosmos.features.projects.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.ProjectCategory
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.User
import com.example.kosmos.features.project.presentation.SelectedMember
import com.example.kosmos.features.users.presentation.components.UserAvatar
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens.Spacing
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reusable components for Project Creation Wizard
 * All wizard steps use these components for consistency
 */

/** Shared wizard color tokens to avoid hardcoded hex values */
object WizardColors {
    val emerald = Color(0xFF10B981)
    val emeraldDark = Color(0xFF059669)
    val amber = Color(0xFFF59E0B)
}

/** Shared text field colors for all wizard steps */
@Composable
fun wizardTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = ColorTokens.ReactTheme.card,
    unfocusedContainerColor = ColorTokens.ReactTheme.card,
    focusedBorderColor = ColorTokens.ReactTheme.primary,
    unfocusedBorderColor = ColorTokens.ReactTheme.border,
    focusedTextColor = ColorTokens.ReactTheme.foreground,
    unfocusedTextColor = ColorTokens.ReactTheme.foreground,
    focusedLabelColor = ColorTokens.ReactTheme.primary,
    unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground,
    focusedPlaceholderColor = ColorTokens.ReactTheme.mutedForeground,
    unfocusedPlaceholderColor = ColorTokens.ReactTheme.mutedForeground,
    errorBorderColor = ColorTokens.ReactTheme.destructive,
    cursorColor = ColorTokens.ReactTheme.primary
)

// ---------- Progress Indicator ----------

/**
 * Wizard progress indicator with animated dots
 * Shows current step and allows navigation to previous steps
 *
 * @param currentStep Current step (1-3)
 * @param totalSteps Total number of steps (default 3)
 * @param onStepClick Callback when a completed step is clicked
 */
@Composable
fun WizardProgressIndicator(
    currentStep: Int,
    totalSteps: Int = 3,
    onStepClick: ((Int) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val stepNumber = index + 1
            val isCompleted = stepNumber < currentStep
            val isCurrent = stepNumber == currentStep
            val isClickable = isCompleted && onStepClick != null

            // Step dot
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 16.dp else 12.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> ColorTokens.ReactTheme.primary
                            isCompleted -> ColorTokens.ReactTheme.primary.copy(alpha = 0.7f)
                            else -> ColorTokens.ReactTheme.secondary
                        }
                    )
                    .then(
                        if (isClickable) {
                            Modifier.clickable { onStepClick?.invoke(stepNumber) }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        modifier = Modifier.size(8.dp),
                        tint = Color.White
                    )
                }
            }

            // Connecting line (except after last step)
            if (stepNumber < totalSteps) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(
                            if (isCompleted) ColorTokens.ReactTheme.primary.copy(alpha = 0.5f)
                            else ColorTokens.ReactTheme.secondary
                        )
                )
            }
        }
    }

    // Step label
    Text(
        text = "Step $currentStep of $totalSteps",
        style = MaterialTheme.typography.bodySmall,
        color = ColorTokens.ReactTheme.mutedForeground,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

// ---------- Category Selector ----------

/**
 * Category selection chips
 * Displays 4 categories: Tech, Social, Business, Other
 *
 * @param selectedCategory Currently selected category
 * @param onCategorySelect Callback when category is selected
 */
@Composable
fun CategorySelector(
    selectedCategory: ProjectCategory,
    onCategorySelect: (ProjectCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Project Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground,
            modifier = Modifier.padding(bottom = Spacing.sm)
        )

        val categories = ProjectCategory.values()
        // 2x2 grid layout for better fit on phone screens
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            for (rowIndex in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    for (colIndex in 0..1) {
                        val index = rowIndex * 2 + colIndex
                        if (index < categories.size) {
                            val category = categories[index]
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCategorySelect(category) },
                                label = {
                                    Text(
                                        text = category.getDisplayName(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = category.getIcon(),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColorTokens.ReactTheme.primary,
                                    selectedLabelColor = ColorTokens.ReactTheme.primaryForeground,
                                    selectedLeadingIconColor = ColorTokens.ReactTheme.primaryForeground,
                                    containerColor = ColorTokens.ReactTheme.secondary,
                                    labelColor = ColorTokens.ReactTheme.foreground,
                                    iconColor = ColorTokens.ReactTheme.mutedForeground
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = ColorTokens.ReactTheme.border,
                                    selectedBorderColor = ColorTokens.ReactTheme.primary,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Tech Stack Selector ----------

/**
 * Multi-select tech stack chips
 * Popular technologies with custom entry option
 *
 * @param selectedTech Currently selected technologies
 * @param onTechUpdate Callback when selection changes
 */
@Composable
fun TechStackSelector(
    selectedTech: List<String>,
    onTechUpdate: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val popularTech = listOf(
        "Kotlin", "Java", "Python", "JavaScript", "TypeScript",
        "React", "Vue", "Angular", "Node.js", "Django",
        "Spring Boot", "FastAPI", "PostgreSQL", "MongoDB", "Redis",
        "Docker", "Kubernetes", "AWS", "GCP", "Firebase"
    )

    var showCustomDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Tech Stack",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground
        )
        Text(
            text = "Select technologies you'll use (max 20)",
            style = MaterialTheme.typography.bodySmall,
            color = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.padding(bottom = Spacing.sm)
        )

        // Selected count
        Text(
            text = "${selectedTech.size}/20 selected",
            style = MaterialTheme.typography.labelMedium,
            color = ColorTokens.ReactTheme.primary,
            modifier = Modifier.padding(bottom = Spacing.xs)
        )

        // Tech chips (scrollable)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Add custom button
            item {
                AssistChip(
                    onClick = { showCustomDialog = true },
                    label = { Text("+ Custom") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            // Popular tech chips
            items(popularTech) { tech ->
                val isSelected = selectedTech.contains(tech)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onTechUpdate(
                            if (isSelected) selectedTech - tech
                            else if (selectedTech.size < 20) selectedTech + tech
                            else selectedTech
                        )
                    },
                    label = { Text(tech) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColorTokens.ReactTheme.primary,
                        selectedLabelColor = ColorTokens.ReactTheme.primaryForeground,
                        containerColor = ColorTokens.ReactTheme.secondary,
                        labelColor = ColorTokens.ReactTheme.foreground
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = ColorTokens.ReactTheme.border,
                        selectedBorderColor = ColorTokens.ReactTheme.primary,
                        enabled = true,
                        selected = isSelected
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Custom tech chips
            items(selectedTech.filter { !popularTech.contains(it) }) { tech ->
                FilterChip(
                    selected = true,
                    onClick = { onTechUpdate(selectedTech - tech) },
                    label = { Text(tech) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ColorTokens.ReactTheme.primary,
                        selectedLabelColor = ColorTokens.ReactTheme.primaryForeground,
                        selectedTrailingIconColor = ColorTokens.ReactTheme.primaryForeground
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }

    // Custom tech dialog
    if (showCustomDialog) {
        TagInputDialog(
            title = "Add Custom Technology",
            onDismiss = { showCustomDialog = false },
            onAdd = { customTech ->
                if (selectedTech.size < 20 && !selectedTech.contains(customTech)) {
                    onTechUpdate(selectedTech + customTech)
                }
                showCustomDialog = false
            }
        )
    }
}

// ---------- License Dropdown ----------

/**
 * Open source license dropdown
 * Common OSS licenses for tech projects
 *
 * @param selectedLicense Currently selected license
 * @param onLicenseSelect Callback when license is selected
 */
@Composable
fun LicenseDropdown(
    selectedLicense: String?,
    onLicenseSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val licenses = listOf(
        "MIT License",
        "Apache License 2.0",
        "GNU GPL v3.0",
        "BSD 3-Clause License",
        "Mozilla Public License 2.0",
        "GNU LGPL v3.0",
        "The Unlicense",
        "Proprietary"
    )

    var expanded by remember { mutableStateOf(false) }

    val textFieldColors = wizardTextFieldColors()

    Column(modifier = modifier) {
        Text(
            text = "Open Source License (Optional)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground,
            modifier = Modifier.padding(bottom = Spacing.sm)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedLicense ?: "Select License",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.mutedForeground
                    )
                },
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                licenses.forEach { license ->
                    DropdownMenuItem(
                        text = { Text(license, color = ColorTokens.ReactTheme.foreground) },
                        onClick = {
                            onLicenseSelect(license)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// ---------- Tag Input Field ----------

/**
 * Dialog for adding a custom tag
 *
 * @param title Dialog title
 * @param onDismiss Callback when dialog is dismissed
 * @param onAdd Callback when tag is added with tag value
 */
@Composable
fun TagInputDialog(
    title: String,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var tagValue by remember { mutableStateOf("") }

    val textFieldColors = wizardTextFieldColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorTokens.ReactTheme.card,
        titleContentColor = ColorTokens.ReactTheme.foreground,
        textContentColor = ColorTokens.ReactTheme.foreground,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = tagValue,
                onValueChange = { tagValue = it },
                label = { Text("Tag name") },
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tagValue.isNotBlank()) {
                        onAdd(tagValue.trim())
                    }
                },
                enabled = tagValue.isNotBlank()
            ) {
                Text("Add", color = ColorTokens.ReactTheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ColorTokens.ReactTheme.mutedForeground)
            }
        }
    )
}

/**
 * Multi-tag input field with chip display
 *
 * @param tags Currently selected tags
 * @param onTagsUpdate Callback when tags change
 * @param label Field label
 * @param maxTags Maximum tags allowed
 */
@Composable
fun TagInputField(
    tags: List<String>,
    onTagsUpdate: (List<String>) -> Unit,
    label: String = "Tags",
    maxTags: Int = 10,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ColorTokens.ReactTheme.foreground
            )
            Text(
                text = "${tags.size}/$maxTags",
                style = MaterialTheme.typography.bodySmall,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Tag chips
        if (tags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tags) { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onTagsUpdate(tags - tag) },
                        label = { Text(tag, color = ColorTokens.ReactTheme.foreground) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp),
                                tint = ColorTokens.ReactTheme.mutedForeground
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = ColorTokens.ReactTheme.secondary
                        ),
                        border = InputChipDefaults.inputChipBorder(
                            borderColor = ColorTokens.ReactTheme.border,
                            enabled = true,
                            selected = false
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        // Add tag button
        if (tags.size < maxTags) {
            OutlinedButton(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ColorTokens.ReactTheme.foreground
                ),
                border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Add Tag")
            }
        }
    }

    if (showDialog) {
        TagInputDialog(
            title = "Add $label",
            onDismiss = { showDialog = false },
            onAdd = { tag ->
                if (tags.size < maxTags && !tags.contains(tag)) {
                    onTagsUpdate(tags + tag)
                }
                showDialog = false
            }
        )
    }
}

// ---------- Deadline Picker ----------

/**
 * Deadline picker with formatted display
 * Uses Material 3 DatePickerDialog
 *
 * @param deadline Current deadline timestamp (nullable)
 * @param onDeadlineSelect Callback when deadline is selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlinePicker(
    deadline: Long?,
    onDeadlineSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Column(modifier = modifier) {
        Text(
            text = "Project Deadline (Optional)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.foreground,
            modifier = Modifier.padding(bottom = Spacing.sm)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ColorTokens.ReactTheme.foreground
                ),
                border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = deadline?.let { dateFormatter.format(Date(it)) } ?: "Select Date",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (deadline != null) {
                IconButton(onClick = { onDeadlineSelect(null) }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear deadline",
                        tint = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadline ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDeadlineSelect(it) }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ---------- Member Card ----------

/**
 * Member card with role selection and remove action
 * Used in member selection and review steps
 *
 * @param member Selected member with user and role
 * @param isOwner Whether this member is the project owner (cannot change role/remove)
 * @param onRoleChange Callback when role is changed
 * @param onRemove Callback when member is removed
 */
@Composable
fun MemberCard(
    member: SelectedMember,
    isOwner: Boolean = false,
    onRoleChange: (ProjectRole) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        shape = RoundedCornerShape(12.dp),
        color = ColorTokens.ReactTheme.card,
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User info
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                UserAvatar(
                    photoUrl = member.user.photoUrl,
                    displayName = member.user.displayName,
                    isOnline = member.user.isOnline,
                    size = 40.dp,
                    showOnlineIndicator = false
                )

                Column {
                    Text(
                        text = member.user.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = ColorTokens.ReactTheme.foreground
                    )
                    Text(
                        text = "@${member.user.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }

            // Role dropdown or badge
            if (isOwner) {
                AssistChip(
                    onClick = {},
                    label = { Text("ADMIN") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    enabled = false
                )
            } else {
                var roleMenuExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = roleMenuExpanded,
                    onExpandedChange = { roleMenuExpanded = !roleMenuExpanded }
                ) {
                    AssistChip(
                        onClick = { roleMenuExpanded = true },
                        label = { Text(member.role.name) },
                        modifier = Modifier.menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = roleMenuExpanded,
                        onDismissRequest = { roleMenuExpanded = false }
                    ) {
                        listOf(ProjectRole.MANAGER, ProjectRole.MEMBER).forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name, color = ColorTokens.ReactTheme.foreground) },
                                onClick = {
                                    onRoleChange(role)
                                    roleMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Remove button
            if (!isOwner) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove member",
                        tint = ColorTokens.ReactTheme.destructive
                    )
                }
            }
        }
    }
}

// ---------- Recent Collaborator Chip ----------

/**
 * Compact chip for recent collaborators
 * Used in horizontal scrolling list
 *
 * @param user User to display
 * @param isSelected Whether user is already selected
 * @param onClick Callback when chip is clicked
 */
@Composable
fun RecentCollaboratorChip(
    user: User,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(
                    photoUrl = user.photoUrl,
                    displayName = user.displayName,
                    isOnline = user.isOnline,
                    size = 32.dp,
                    showOnlineIndicator = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.displayName.split(" ").firstOrNull() ?: user.username,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        enabled = !isSelected,
        modifier = modifier,
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}

// ---------- Navigation Buttons ----------

/**
 * Wizard navigation buttons (Back, Next, Create)
 * Handles button states and visibility
 *
 * @param currentStep Current step (1-3)
 * @param canProceed Whether Next/Create is enabled
 * @param isCreating Whether project creation is in progress
 * @param onBack Callback for Back button
 * @param onNext Callback for Next button
 * @param onCreate Callback for Create button (step 3)
 */
@Composable
fun WizardNavigationButtons(
    currentStep: Int,
    canProceed: Boolean,
    isCreating: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back button (hidden on step 1)
        if (currentStep > 1) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isCreating,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ColorTokens.ReactTheme.foreground
                ),
                border = BorderStroke(1.dp, ColorTokens.ReactTheme.border),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Back")
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp)) // Keep layout balanced
        }

        // Next/Create button
        if (currentStep < 3) {
            Button(
                onClick = onNext,
                enabled = canProceed && !isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorTokens.ReactTheme.primary,
                    contentColor = ColorTokens.ReactTheme.primaryForeground,
                    disabledContainerColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.5f),
                    disabledContentColor = ColorTokens.ReactTheme.primaryForeground.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next")
                Spacer(modifier = Modifier.width(Spacing.sm))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Button(
                onClick = onCreate,
                enabled = canProceed && !isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorTokens.ReactTheme.primary,
                    contentColor = ColorTokens.ReactTheme.primaryForeground,
                    disabledContainerColor = ColorTokens.ReactTheme.primary.copy(alpha = 0.5f),
                    disabledContentColor = ColorTokens.ReactTheme.primaryForeground.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                }
                Text(if (isCreating) "Creating..." else "Create Project")
            }
        }
    }
}

// ---------- Summary Sections ----------

/**
 * Project summary section for review step
 * Displays all project details
 *
 * @param data Project creation data
 * @param onEdit Callback to edit (jumps to step 1)
 */
@Composable
fun ProjectSummarySection(
    data: com.example.kosmos.data.repository.ProjectCreationData,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ColorTokens.ReactTheme.card,
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Project Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTokens.ReactTheme.foreground
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit details",
                        tint = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Project name
            SummaryField(
                label = "Name",
                value = data.name,
                icon = data.category.getIcon()
            )

            // Description
            SummaryField(
                label = "Description",
                value = data.description
            )

            // Category
            SummaryField(
                label = "Category",
                value = data.category.getDisplayName()
            )

            // Deadline
            data.deadline?.let { deadline ->
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                SummaryField(
                    label = "Deadline",
                    value = formatter.format(Date(deadline)),
                    icon = Icons.Default.CalendarToday
                )
            }

            // Category-specific fields
            when (data.category) {
                ProjectCategory.TECH -> {
                    data.githubUrl?.let { SummaryField("GitHub", it, Icons.Default.Code) }
                    data.techStack?.let { SummaryField("Tech Stack", it.joinToString(", ")) }
                    data.openSourceLicense?.let { SummaryField("License", it) }
                }
                ProjectCategory.SOCIAL -> {
                    data.projectMotive?.let { SummaryField("Motive", it) }
                    data.targetAudience?.let { SummaryField("Target Audience", it) }
                }
                ProjectCategory.BUSINESS -> {
                    data.websiteUrl?.let { SummaryField("Website", it, Icons.Default.Language) }
                    data.businessModel?.let { SummaryField("Business Model", it) }
                    data.industryTags?.let { SummaryField("Industries", it.joinToString(", ")) }
                }
                ProjectCategory.OTHER -> {
                    data.projectMotive?.let { SummaryField("Description", it) }
                }
            }

            // Tags
            data.tags?.let { tags ->
                if (tags.isNotEmpty()) {
                    SummaryField("Tags", tags.joinToString(", "))
                }
            }
        }
    }
}

/**
 * Members summary section for review step
 * Displays all selected members
 *
 * @param members Selected members list
 * @param ownerName Owner's display name
 * @param onEdit Callback to edit (jumps to step 2)
 */
@Composable
fun MembersSummarySection(
    members: List<SelectedMember>,
    ownerName: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ColorTokens.ReactTheme.card,
        border = BorderStroke(1.dp, ColorTokens.ReactTheme.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Team Members (Invites)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTokens.ReactTheme.foreground
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit members",
                        tint = ColorTokens.ReactTheme.mutedForeground
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "You + ${members.size} invite${if (members.size != 1) "s" else ""} (members join after accepting)",
                style = MaterialTheme.typography.bodySmall,
                color = ColorTokens.ReactTheme.mutedForeground
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Owner
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = ColorTokens.Priority.medium,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "$ownerName (You) - ADMIN",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = ColorTokens.ReactTheme.foreground
                )
            }

            // Other members
            members.forEach { member ->
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        photoUrl = member.user.photoUrl,
                        displayName = member.user.displayName,
                        isOnline = member.user.isOnline,
                        size = 20.dp,
                        showOnlineIndicator = false
                    )
                    Text(
                        text = "${member.user.displayName} - ${member.role.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorTokens.ReactTheme.foreground
                    )
                }
            }
        }
    }
}

/**
 * Summary field row
 * Displays label and value with optional icon
 */
@Composable
private fun SummaryField(
    label: String,
    value: String,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = ColorTokens.ReactTheme.mutedForeground
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ColorTokens.ReactTheme.mutedForeground
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorTokens.ReactTheme.foreground,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
