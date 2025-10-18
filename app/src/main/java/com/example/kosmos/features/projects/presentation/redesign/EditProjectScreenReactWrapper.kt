package com.example.kosmos.features.projects.presentation.redesign

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.kosmos.core.models.Project
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.ProjectRepository
import com.example.kosmos.features.project.presentation.ProjectViewModel
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EditProjectDataViewModel @Inject constructor(
    val projectRepository: ProjectRepository,
    val authRepository: AuthRepository
) : ViewModel()

@Composable
fun EditProjectScreenReactWrapper(
    projectId: String,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    onDeleteSuccess: () -> Unit = onBack,
    projectViewModel: ProjectViewModel = hiltViewModel(),
    dataViewModel: EditProjectDataViewModel = hiltViewModel()
) {
    val projectRepository = dataViewModel.projectRepository
    val authRepository = dataViewModel.authRepository
    val coroutineScope = rememberCoroutineScope()

    var project by remember { mutableStateOf<Project?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load project data
    LaunchedEffect(projectId) {
        project = projectRepository.getProject(projectId)
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorTokens.ReactTheme.primary)
        }
        return
    }

    val p = project ?: run {
        onBack()
        return
    }

    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val initialData = ProjectEditFormData(
        name = p.name,
        description = p.description,
        status = p.status,
        category = p.category,
        color = p.color,
        deadline = p.deadline?.let { dateFormat.format(Date(it)) } ?: "",
        githubUrl = p.githubUrl ?: "",
        websiteUrl = p.websiteUrl ?: "",
        projectMotive = p.projectMotive ?: "",
        techStack = p.techStack ?: "",
        businessModel = p.businessModel ?: "",
        targetAudience = p.targetAudience ?: "",
        tags = p.tags?.let {
            try {
                Json.decodeFromString<List<String>>(it)
            } catch (_: Exception) {
                it.split(",").map { t -> t.trim() }.filter { t -> t.isNotEmpty() }
            }
        } ?: emptyList()
    )

    EditProjectScreenReact(
        projectId = projectId,
        initialData = initialData,
        onBack = onBack,
        onSave = { formData ->
            coroutineScope.launch {
                val currentUserId = authRepository.getCurrentUser()?.id ?: return@launch
                val updatedProject = p.copy(
                    name = formData.name,
                    description = formData.description,
                    status = formData.status,
                    category = formData.category,
                    color = formData.color,
                    deadline = if (formData.deadline.isNotEmpty()) {
                        try { dateFormat.parse(formData.deadline)?.time } catch (_: Exception) { null }
                    } else null,
                    githubUrl = formData.githubUrl.ifEmpty { null },
                    websiteUrl = formData.websiteUrl.ifEmpty { null },
                    projectMotive = formData.projectMotive.ifEmpty { null },
                    techStack = formData.techStack.ifEmpty { null },
                    businessModel = formData.businessModel.ifEmpty { null },
                    targetAudience = formData.targetAudience.ifEmpty { null },
                    tags = if (formData.tags.isNotEmpty()) {
                        "[" + formData.tags.joinToString(",") { "\"$it\"" } + "]"
                    } else null,
                    updatedAt = System.currentTimeMillis()
                )

                val result = projectRepository.updateProject(updatedProject, currentUserId)
                if (result.isSuccess) {
                    onSaveSuccess()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to update project"
                    Log.e("EditProject", "Update failed", result.exceptionOrNull())
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onDelete = {
            coroutineScope.launch {
                projectViewModel.deleteProject(projectId)
                onDeleteSuccess()
            }
        }
    )
}
