package com.example.kosmos.features.chat.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.User
import com.example.kosmos.shared.ui.components.*
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.utils.ValidationUtils

/**
 * Create Chat Screen - Full-Screen 2-Step Wizard for New Chats
 *
 * Step 1: Select project members to chat with
 * Step 2 (groups only): Enter a group name
 *
 * Flow:
 * - 1 person selected  → "Start Chat" → direct create (no step 2)
 * - 2+ people selected → "Continue"  → step 2 (group name) → "Create Group"
 *
 * P1-07: Includes proper validation using ValidationUtils
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatScreen(
    projectMembers: List<User>,
    isLoading: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onCreate: (chatName: String?, selectedUserIds: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(1) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUsers by remember { mutableStateOf(setOf<User>()) }
    var chatName by remember { mutableStateOf("") }
    var chatNameError by remember { mutableStateOf<String?>(null) }

    // Step 2 is only needed for group chats (2+ members)
    val isGroupChat = selectedUsers.size > 1

    fun validateChatName() {
        chatNameError = ValidationUtils.validateChatName(chatName, required = true)
    }

    // Filter members based on search query
    val filteredMembers = remember(projectMembers, searchQuery) {
        if (searchQuery.isBlank()) {
            projectMembers
        } else {
            projectMembers.filter { user ->
                user.displayName.contains(searchQuery, ignoreCase = true) ||
                user.username.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = ColorTokens.ReactTheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New Chat",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorTokens.ReactTheme.background,
                    titleContentColor = ColorTokens.ReactTheme.foreground,
                    navigationIconContentColor = ColorTokens.ReactTheme.foreground
                )
            )
        },
        bottomBar = {
            Surface(color = ColorTokens.ReactTheme.background) {
                Column {
                    HorizontalDivider(color = ColorTokens.ReactTheme.border)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Tokens.Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                    ) {
                        if (currentStep == 2) {
                            SecondaryButton(
                                text = "Back",
                                onClick = { currentStep = 1 },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        LoadingButton(
                            text = when {
                                currentStep == 1 && !isGroupChat -> "Start Chat"
                                currentStep == 1 -> "Continue"
                                else -> "Create Group"
                            },
                            onClick = {
                                if (currentStep == 1 && isGroupChat) {
                                    // Advance to group name step
                                    currentStep = 2
                                } else {
                                    // Validate group name before creating
                                    if (isGroupChat) {
                                        val nameError = ValidationUtils.validateChatName(chatName, required = true)
                                        chatNameError = nameError
                                        if (nameError != null) return@LoadingButton
                                    }
                                    // Direct chat passes null; group chat passes the entered name
                                    val finalName = if (!isGroupChat) {
                                        null
                                    } else {
                                        chatName.ifBlank {
                                            selectedUsers.joinToString(", ") {
                                                it.displayName.split(" ").first()
                                            }
                                        }
                                    }
                                    onCreate(finalName, selectedUsers.map { it.id })
                                }
                            },
                            isLoading = isLoading,
                            enabled = selectedUsers.isNotEmpty() &&
                                      (currentStep == 1 || (chatName.isNotBlank() && chatNameError == null)),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Error message below buttons
                    if (error != null) {
                        Text(
                            text = error,
                            color = ColorTokens.ReactTheme.destructive,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Tokens.Spacing.sm)
                                .padding(horizontal = Tokens.Spacing.md)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Show step indicator only when there are (or will be) multiple steps
            if (isGroupChat || currentStep == 2) {
                WizardStepIndicator(currentStep = currentStep, totalSteps = 2)
                HorizontalDivider(color = ColorTokens.ReactTheme.border)
            }

            AnimatedContent(
                targetState = currentStep,
                label = "wizard_step"
            ) { step ->
                if (step == 1) {
                    // --- Step 1: Select People ---
                    LazyColumn(
                        contentPadding = PaddingValues(Tokens.Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            SearchBarStandard(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                placeholder = "Search members...",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (selectedUsers.isNotEmpty()) {
                            item {
                                SectionCard(title = "SELECTED (${selectedUsers.size})") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
                                    ) {
                                        selectedUsers.forEach { user ->
                                            FilterChip(
                                                selected = true,
                                                onClick = { selectedUsers = selectedUsers - user },
                                                label = { Text(user.displayName) },
                                                trailingIcon = {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Remove",
                                                        modifier = Modifier.size(Tokens.Size.iconSmall)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            SectionCard(title = "MEMBERS") {
                                if (filteredMembers.isEmpty()) {
                                    EmptyState(
                                        icon = IconSet.Action.search,
                                        title = "No users found",
                                        message = if (searchQuery.isBlank())
                                            "No project members available"
                                        else
                                            "No results for \"$searchQuery\""
                                    )
                                } else {
                                    Column {
                                        filteredMembers.forEach { user ->
                                            UserSelectionItem(
                                                user = user,
                                                isSelected = user in selectedUsers,
                                                onToggle = {
                                                    selectedUsers = if (user in selectedUsers) {
                                                        selectedUsers - user
                                                    } else {
                                                        selectedUsers + user
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // --- Step 2: Group Details ---
                    LazyColumn(
                        contentPadding = PaddingValues(Tokens.Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            SectionCard(title = "GROUP NAME") {
                                OutlinedTextField(
                                    value = chatName,
                                    onValueChange = {
                                        chatName = it
                                        validateChatName()
                                    },
                                    label = { Text("Group Name *") },
                                    placeholder = { Text("Enter a name for the group") },
                                    supportingText = {
                                        if (chatNameError != null) {
                                            Text(
                                                text = chatNameError!!,
                                                color = ColorTokens.ReactTheme.destructive
                                            )
                                        } else {
                                            Text(
                                                text = "${chatName.length}/100",
                                                color = ColorTokens.ReactTheme.mutedForeground
                                            )
                                        }
                                    },
                                    isError = chatNameError != null,
                                    singleLine = true,
                                    colors = KosmosDialogDefaults.textFieldColors(),
                                    shape = RoundedCornerShape(Tokens.CornerRadius.md),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        item {
                            SectionCard(title = "MEMBERS (${selectedUsers.size})") {
                                selectedUsers.forEach { user ->
                                    UserSelectionItem(
                                        user = user,
                                        isSelected = true,
                                        onToggle = {} // read-only in step 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Backwards-compatibility alias. Prefer [CreateChatScreen].
 */
@Deprecated(
    message = "Renamed to CreateChatScreen. Use CreateChatScreen instead.",
    replaceWith = ReplaceWith("CreateChatScreen(projectMembers, isLoading, error, onDismiss, onCreate, modifier)")
)
@Composable
fun CreateChatDialog(
    projectMembers: List<User>,
    isLoading: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onCreate: (chatName: String?, selectedUserIds: List<String>) -> Unit,
    modifier: Modifier = Modifier
) = CreateChatScreen(
    projectMembers = projectMembers,
    isLoading = isLoading,
    error = error,
    onDismiss = onDismiss,
    onCreate = onCreate,
    modifier = modifier
)

/**
 * User Selection Item with Checkbox
 */
@Composable
private fun UserSelectionItem(
    user: User,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.Spacing.md, vertical = Tokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )

            // User avatar placeholder
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = ColorTokens.ReactTheme.secondary,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = ColorTokens.ReactTheme.primaryForeground
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            if (isSelected) {
                Icon(
                    IconSet.Status.checkmark,
                    contentDescription = "Selected",
                    tint = ColorTokens.ReactTheme.primary
                )
            }
        }
    }
}
