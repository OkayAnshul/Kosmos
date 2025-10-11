package com.example.kosmos.features.projects.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kosmos.core.models.ProjectRole
import com.example.kosmos.core.models.User
import com.example.kosmos.data.repository.ProjectCreationData
import com.example.kosmos.features.project.presentation.SelectedMember
import com.example.kosmos.shared.ui.components.WizardStepIndicator
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.Tokens.Spacing

/**
 * Multi-step project creation wizard
 *
 * Full-screen Scaffold implementation.
 * Flow: Project Details → Add Members → Review & Create
 *
 * Features:
 * - Dynamic category-based fields
 * - Member search and role assignment
 * - Real-time validation
 * - Animated step transitions
 * - Success animation on completion
 * - Offline-first project creation
 *
 * @param currentStep Current wizard step (1-3)
 * @param projectData Current project data (partial during wizard flow)
 * @param selectedMembers Members selected to add to project
 * @param recentCollaborators Recent collaborators for quick selection
 * @param allUsers All available users for search
 * @param userSearchQuery Current search query
 * @param validationErrors Map of field names to error messages
 * @param isCreating Whether project is being created
 * @param currentUserId ID of user creating the project
 * @param currentUserName Display name of user creating the project
 * @param onStepChange Callback when step changes
 * @param onProjectDataUpdate Callback when project data is updated
 * @param onAddMember Callback to add member
 * @param onRemoveMember Callback to remove member by user ID
 * @param onUpdateMemberRole Callback to update member role
 * @param onSearchQueryChange Callback when search query changes
 * @param onCreate Callback to create project
 * @param onDismiss Callback to dismiss wizard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCreationWizard(
    currentStep: Int,
    projectData: ProjectCreationData?,
    selectedMembers: List<SelectedMember>,
    recentCollaborators: List<User>,
    connectionUsers: List<User> = emptyList(),
    allUsers: List<User>,
    userSearchQuery: String,
    validationErrors: Map<String, String>,
    isCreating: Boolean,
    currentUserId: String,
    currentUserName: String,
    onStepChange: (Int) -> Unit,
    onProjectDataUpdate: (ProjectCreationData) -> Unit,
    onAddMember: (User, ProjectRole) -> Unit,
    onRemoveMember: (String) -> Unit,
    onUpdateMemberRole: (String, ProjectRole) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    // Track creation success for animation
    var showSuccessAnimation by remember { mutableStateOf(false) }

    // Auto-dismiss after success animation
    LaunchedEffect(showSuccessAnimation) {
        if (showSuccessAnimation) {
            kotlinx.coroutines.delay(2000)
            showSuccessAnimation = false
            onDismiss()
        }
    }

    // Step subtitle text shown below TopAppBar
    val stepSubtitle = when (currentStep) {
        1 -> "Tell us about your project"
        2 -> "Add your team members"
        3 -> "Review and create"
        else -> ""
    }

    Scaffold(
        containerColor = ColorTokens.ReactTheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "New Project",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (stepSubtitle.isNotEmpty()) {
                            Text(
                                text = stepSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = if (!isCreating) onDismiss else { {} }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorTokens.ReactTheme.background,
                    titleContentColor = ColorTokens.ReactTheme.foreground,
                    navigationIconContentColor = ColorTokens.ReactTheme.mutedForeground
                )
            )
        },
        bottomBar = {
            Surface(color = ColorTokens.ReactTheme.background) {
                Column {
                    HorizontalDivider(color = ColorTokens.ReactTheme.border)
                    WizardNavigationButtons(
                        currentStep = currentStep,
                        canProceed = validationErrors.isEmpty() && projectData != null,
                        isCreating = isCreating,
                        onBack = { onStepChange(currentStep - 1) },
                        onNext = { onStepChange(currentStep + 1) },
                        onCreate = {
                            onCreate()
                            showSuccessAnimation = true
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Step indicator below TopAppBar
                WizardStepIndicator(
                    currentStep = currentStep,
                    totalSteps = 3
                )

                HorizontalDivider(color = ColorTokens.ReactTheme.border)

                // Step content with animated transitions
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                // Forward navigation
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) + fadeIn() togetherWith
                                        slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> -fullWidth },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) + fadeOut()
                            } else {
                                // Backward navigation
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) + fadeIn() togetherWith
                                        slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> fullWidth },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) + fadeOut()
                            }
                        },
                        label = "step_transition"
                    ) { step ->
                        when (step) {
                            1 -> Step1ProjectDetails(
                                projectData = projectData,
                                validationErrors = validationErrors,
                                onDataUpdate = onProjectDataUpdate
                            )
                            2 -> Step2AddMembers(
                                selectedMembers = selectedMembers,
                                recentCollaborators = recentCollaborators,
                                connectionUsers = connectionUsers,
                                allUsers = allUsers,
                                searchQuery = userSearchQuery,
                                currentUserId = currentUserId,
                                onAddMember = onAddMember,
                                onRemoveMember = onRemoveMember,
                                onUpdateMemberRole = onUpdateMemberRole,
                                onSearchQueryChange = onSearchQueryChange
                            )
                            3 -> Step3ReviewCreate(
                                projectData = projectData,
                                selectedMembers = selectedMembers,
                                currentUserName = currentUserName,
                                onEditDetails = { onStepChange(1) },
                                onEditMembers = { onStepChange(2) }
                            )
                        }
                    }
                }
            }

            // Success animation overlay
            AnimatedVisibility(
                visible = showSuccessAnimation,
                enter = fadeIn() + scaleIn(
                    initialScale = 0.3f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = fadeOut()
            ) {
                SuccessOverlay()
            }

            // Loading overlay (during creation, before success)
            if (isCreating && !showSuccessAnimation) {
                LoadingOverlay()
            }
        }
    }
}

/**
 * Loading overlay shown during project creation
 */
@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(Spacing.lg)
                .widthIn(max = 300.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = ColorTokens.ReactTheme.card,
            border = androidx.compose.foundation.BorderStroke(1.dp, ColorTokens.ReactTheme.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = ColorTokens.ReactTheme.primary
                )
                Text(
                    text = "Creating your project...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = "Setting up workspace and inviting members",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.ReactTheme.mutedForeground,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Success animation overlay
 * Shows checkmark animation after project creation
 */
@Composable
private fun SuccessOverlay() {
    val scale = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        WizardColors.emerald.copy(alpha = 0.9f),
                        WizardColors.emeraldDark.copy(alpha = 0.95f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationZ = rotation.value
                    },
                tint = Color.White
            )

            Text(
                text = "Project Created!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.graphicsLayer {
                    alpha = scale.value
                }
            )

            Text(
                text = "Redirecting to your new project...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.graphicsLayer {
                    alpha = scale.value
                }
            )
        }
    }
}
