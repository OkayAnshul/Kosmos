package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kosmos.core.models.ProjectCategory
import com.example.kosmos.core.models.ProjectStatus
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens

/**
 * Edit Project Screen - Full screen form matching TaskEditScreenReact pattern
 *
 * Fields:
 * - Basic: Name, Description, Status, Category
 * - Category-specific: GitHub URL, Website URL, Motive, Tech Stack, Business Model, Target Audience
 * - Details: Deadline, Tags, Color
 * - Danger zone: Delete project
 */

data class ProjectEditFormData(
    val name: String = "",
    val description: String = "",
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val category: ProjectCategory = ProjectCategory.OTHER,
    val color: String = "#6366F1",
    val deadline: String = "",
    val githubUrl: String = "",
    val websiteUrl: String = "",
    val projectMotive: String = "",
    val techStack: String = "",
    val businessModel: String = "",
    val targetAudience: String = "",
    val tags: List<String> = emptyList()
)

@Composable
fun EditProjectScreenReact(
    projectId: String? = null,
    initialData: ProjectEditFormData = ProjectEditFormData(),
    onBack: () -> Unit = {},
    onSave: (ProjectEditFormData) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isNew = projectId == null

    var name by remember { mutableStateOf(initialData.name) }
    var description by remember { mutableStateOf(initialData.description) }
    var status by remember { mutableStateOf(initialData.status) }
    var category by remember { mutableStateOf(initialData.category) }
    var color by remember { mutableStateOf(initialData.color) }
    var deadline by remember { mutableStateOf(initialData.deadline) }
    var githubUrl by remember { mutableStateOf(initialData.githubUrl) }
    var websiteUrl by remember { mutableStateOf(initialData.websiteUrl) }
    var projectMotive by remember { mutableStateOf(initialData.projectMotive) }
    var techStack by remember { mutableStateOf(initialData.techStack) }
    var businessModel by remember { mutableStateOf(initialData.businessModel) }
    var targetAudience by remember { mutableStateOf(initialData.targetAudience) }
    var tags by remember { mutableStateOf(initialData.tags) }
    var tagInput by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = ColorTokens.ReactTheme.card,
        unfocusedContainerColor = ColorTokens.ReactTheme.card,
        focusedBorderColor = ColorTokens.ReactTheme.primary,
        unfocusedBorderColor = ColorTokens.ReactTheme.border,
        cursorColor = ColorTokens.ReactTheme.primary,
        focusedTextColor = ColorTokens.ReactTheme.foreground,
        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
        focusedLabelColor = ColorTokens.ReactTheme.primary,
        unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ColorTokens.ReactTheme.card
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ColorTokens.ReactTheme.foreground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = if (isNew) "New Project" else "Edit Project",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTokens.ReactTheme.foreground
                    )
                }

                Button(
                    onClick = {
                        onSave(
                            ProjectEditFormData(
                                name = name,
                                description = description,
                                status = status,
                                category = category,
                                color = color,
                                deadline = deadline,
                                githubUrl = githubUrl,
                                websiteUrl = websiteUrl,
                                projectMotive = projectMotive,
                                techStack = techStack,
                                businessModel = businessModel,
                                targetAudience = targetAudience,
                                tags = tags
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorTokens.ReactTheme.primary,
                        contentColor = ColorTokens.ReactTheme.primaryForeground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Text("Save", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        HorizontalDivider(color = ColorTokens.ReactTheme.border, thickness = 1.dp)

        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // === BASIC INFO ===
            item {
                SectionHeader("Basic Information")
            }

            // Name
            item {
                FieldLabel("Project Name")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter project name", color = ColorTokens.ReactTheme.mutedForeground) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ColorTokens.ReactTheme.foreground),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Description
            item {
                FieldLabel("Description")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = { Text("Describe your project...", color = ColorTokens.ReactTheme.mutedForeground) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ColorTokens.ReactTheme.foreground),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 6
                )
            }

            // Status selector
            item {
                FieldLabel("Status")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProjectStatus.entries.forEach { s ->
                        val selected = status == s
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { status = s },
                            color = if (selected) ColorTokens.ReactTheme.primary else ColorTokens.ReactTheme.card,
                            shape = RoundedCornerShape(8.dp),
                            border = if (!selected) BorderStroke(1.dp, ColorTokens.ReactTheme.border) else null
                        ) {
                            Text(
                                text = s.name.replace("_", " "),
                                modifier = Modifier.padding(vertical = 10.dp).then(Modifier.fillMaxWidth()),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) ColorTokens.ReactTheme.primaryForeground else ColorTokens.ReactTheme.mutedForeground,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Category selector
            item {
                FieldLabel("Category")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProjectCategory.entries.forEach { c ->
                        val selected = category == c
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { category = c },
                            color = if (selected) ColorTokens.ReactTheme.primary else ColorTokens.ReactTheme.card,
                            shape = RoundedCornerShape(8.dp),
                            border = if (!selected) BorderStroke(1.dp, ColorTokens.ReactTheme.border) else null
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    c.getIcon(),
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selected) ColorTokens.ReactTheme.primaryForeground else ColorTokens.ReactTheme.mutedForeground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = c.getDisplayName(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selected) ColorTokens.ReactTheme.primaryForeground else ColorTokens.ReactTheme.mutedForeground,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // === CATEGORY-SPECIFIC FIELDS ===
            item {
                SectionHeader("Details")
            }

            // GitHub URL (TECH)
            if (category == ProjectCategory.TECH || githubUrl.isNotEmpty()) {
                item {
                    FieldLabel("GitHub URL")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = githubUrl,
                        onValueChange = { githubUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://github.com/...", color = ColorTokens.ReactTheme.mutedForeground) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ColorTokens.ReactTheme.foreground),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Code, null, tint = ColorTokens.ReactTheme.mutedForeground, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            // Tech Stack (TECH)
            if (category == ProjectCategory.TECH || techStack.isNotEmpty()) {
                item {
                    FieldLabel("Tech Stack")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = techStack,
                        onValueChange = { techStack = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Kotlin, Compose, Supabase...", color = ColorTokens.ReactTheme.mutedForeground) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ColorTokens.ReactTheme.foreground),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Website URL (BUSINESS)
            if (category == ProjectCategory.BUSINESS || websiteUrl.isNotEmpty()) {
                item {
                    FieldLabel("Website URL")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://...", color = ColorTokens.ReactTheme.mutedForeground) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ColorTokens.ReactTheme.foreground),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Language, null, tint = ColorTokens.ReactTheme.mutedForeground, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            // Business Model (BUSINESS)
            if (category == ProjectCategory.BUSINESS || businessModel.isNotEmpty()) {
                item {
                    FieldLabel("Business Model")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = businessModel,
                        onValueChange = { businessModel = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        placeholder = { Text("Describe your business model...", color = ColorTokens.ReactTheme.mutedForeground) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ColorTokens.ReactTheme.foreground),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )
                }
            }

            // Project Motive (SOCIAL)
            if (category == ProjectCategory.SOCIAL || projectMotive.isNotEmpty()) {
                item {
                    FieldLabel("Project Motive")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = projectMotive,
                        onValueChange = { projectMotive = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        placeholder = { Text("What drives this project...", color = ColorTokens.ReactTheme.mutedForeground) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ColorTokens.ReactTheme.foreground),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )
                }
            }

            // Target Audience (SOCIAL)
            if (category == ProjectCategory.SOCIAL || targetAudience.isNotEmpty()) {
                item {
                    FieldLabel("Target Audience")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetAudience,
                        onValueChange = { targetAudience = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Who is this for...", color = ColorTokens.ReactTheme.mutedForeground) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ColorTokens.ReactTheme.foreground),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Deadline
            item {
                FieldLabel("Deadline")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("YYYY-MM-DD", color = ColorTokens.ReactTheme.mutedForeground) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ColorTokens.ReactTheme.foreground),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null, tint = ColorTokens.ReactTheme.mutedForeground, modifier = Modifier.size(18.dp)) }
                )
            }

            // Tags
            item {
                FieldLabel("Tags")
                Spacer(modifier = Modifier.height(8.dp))
                if (tags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(tags) { tag ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ColorTokens.ReactTheme.primary.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        tag,
                                        fontSize = 13.sp,
                                        color = ColorTokens.ReactTheme.primary
                                    )
                                    IconButton(
                                        onClick = { tags = tags - tag },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            "Remove",
                                            modifier = Modifier.size(14.dp),
                                            tint = ColorTokens.ReactTheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add tag and press enter...", color = ColorTokens.ReactTheme.mutedForeground) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ColorTokens.ReactTheme.foreground),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            val trimmed = tagInput.trim()
                            if (trimmed.isNotEmpty() && trimmed !in tags) {
                                tags = tags + trimmed
                                tagInput = ""
                            }
                        }
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    )
                )
            }

            // Color picker
            item {
                FieldLabel("Project Color")
                Spacer(modifier = Modifier.height(8.dp))
                val colors = listOf("#6366F1", "#7C3AED", "#EC4899", "#EF4444", "#F59E0B", "#10B981", "#3B82F6", "#8B5CF6")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { c ->
                        val parsed = try { Color(android.graphics.Color.parseColor(c)) } catch (_: Exception) { ColorTokens.ReactTheme.primary }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .then(
                                    if (color == c) Modifier.border(3.dp, ColorTokens.ReactTheme.foreground, CircleShape)
                                    else Modifier
                                )
                                .clickable { color = c },
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == c) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Danger Zone (only for existing projects)
            if (!isNew) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader("Danger Zone")
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, ColorTokens.ReactTheme.destructive.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Delete Project",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorTokens.ReactTheme.destructive
                                )
                                Text(
                                    "This action cannot be undone.",
                                    fontSize = 12.sp,
                                    color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.7f)
                                )
                            }
                            Button(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ColorTokens.ReactTheme.destructive,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Bottom spacer
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Project?", color = ColorTokens.ReactTheme.foreground) },
            text = { Text("This will permanently delete the project and all associated data.", color = ColorTokens.ReactTheme.mutedForeground) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = ColorTokens.ReactTheme.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = ColorTokens.ReactTheme.mutedForeground)
                }
            },
            containerColor = ColorTokens.ReactTheme.card
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = ColorTokens.ReactTheme.foreground,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = ColorTokens.ReactTheme.mutedForeground
    )
}

