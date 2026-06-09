package com.gitsync.presentation.projects

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitsync.core.util.SafUriHelper
import com.gitsync.domain.model.Project
import com.gitsync.domain.model.SyncStatus
import com.gitsync.domain.repository.AuthRepository
import com.gitsync.domain.repository.GitRepository
import com.gitsync.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectsState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val selectedFolderUri: Uri? = null,
    val repoOwner: String = "",
    val repoName: String = "",
    val projectName: String = "",
    val branch: String = "main",
    val isAdding: Boolean = false,
    val error: String? = null,
    val setupStep: String = ""
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val gitRepository: GitRepository,
    private val authRepository: AuthRepository,
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectsState())
    val state: StateFlow<ProjectsState> = _state.asStateFlow()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            projectRepository.getAllProjects().collect { projects ->
                val updatedProjects = mutableListOf<Project>()
                for (project in projects) {
                    val projectDir = java.io.File(project.localPath)
                    val status = if (!projectDir.exists() || !projectDir.canRead()) {
                        SyncStatus.ERROR
                    } else {
                        gitRepository.getSyncStatus(project.localPath)
                    }
                    val files = gitRepository.getModifiedFiles(project.localPath)
                        .getOrDefault(emptyList())
                    updatedProjects.add(project.copy(syncStatus = status, modifiedFiles = files))
                }
                _state.value = _state.value.copy(
                    projects = updatedProjects,
                    isLoading = false
                )
            }
        }
    }

    fun showAddDialog() {
        _state.value = _state.value.copy(
            showAddDialog = true,
            repoOwner = authRepository.getUsername(),
            branch = authRepository.getDefaultBranch()
        )
    }

    fun hideAddDialog() {
        _state.value = _state.value.copy(
            showAddDialog = false,
            selectedFolderUri = null,
            projectName = "",
            repoOwner = authRepository.getUsername(),
            repoName = "",
            branch = authRepository.getDefaultBranch(),
            error = null
        )
    }

    fun onFolderSelected(uri: Uri) {
        val name = SafUriHelper.getDirectoryName(application, uri)

        _state.value = _state.value.copy(
            selectedFolderUri = uri,
            projectName = name,
            repoName = name.lowercase().replace(" ", "-"),
            error = null
        )
    }

    fun onProjectNameChanged(value: String) {
        _state.value = _state.value.copy(projectName = value, error = null)
    }

    fun onRepoOwnerChanged(value: String) {
        _state.value = _state.value.copy(repoOwner = value, error = null)
    }

    fun onRepoNameChanged(value: String) {
        _state.value = _state.value.copy(repoName = value, error = null)
    }

    fun onBranchChanged(value: String) {
        _state.value = _state.value.copy(branch = value, error = null)
    }

    fun addProject() {
        val s = _state.value

        if (s.projectName.isBlank()) {
            _state.value = s.copy(error = "Project name is required")
            return
        }
        if (s.repoOwner.isBlank()) {
            _state.value = s.copy(error = "Repository owner is required")
            return
        }
        if (s.repoName.isBlank()) {
            _state.value = s.copy(error = "Repository name is required")
            return
        }
        if (s.branch.isBlank()) {
            _state.value = s.copy(error = "Branch is required")
            return
        }
        if (s.selectedFolderUri == null) {
            _state.value = s.copy(error = "Please select a folder")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isAdding = true, error = null)

            try {
                if (!SafUriHelper.isValidDirectory(application, s.selectedFolderUri)) {
                    _state.value = _state.value.copy(
                        isAdding = false,
                        error = "Selected folder is not valid or accessible"
                    )
                    return@launch
                }

                SafUriHelper.takePersistablePermissions(application, s.selectedFolderUri)

                val uriString = s.selectedFolderUri.toString()
                val localPath = SafUriHelper.resolveLocalPath(application, s.selectedFolderUri)
                if (localPath == null) {
                    _state.value = _state.value.copy(
                        isAdding = false,
                        error = "Cannot resolve folder path. Please grant 'All Files Access' permission:\nSettings > Apps > GitSync > Permissions > Files and media > Allow management of all files"
                    )
                    return@launch
                }

                projectRepository.addProject(
                    name = s.projectName.trim(),
                    localPath = localPath,
                    safUri = uriString,
                    repoOwner = s.repoOwner.trim(),
                    repoName = s.repoName.trim(),
                    branch = s.branch.trim(),
                    uriPermission = uriString
                )

                _state.value = _state.value.copy(
                    isAdding = false,
                    showAddDialog = false,
                    selectedFolderUri = null,
                    projectName = "",
                    repoName = "",
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isAdding = false,
                    error = e.message ?: "Failed to add project"
                )
            }
        }
    }

    fun addAndSetupProject() {
        val s = _state.value
        if (s.projectName.isBlank()) {
            _state.value = s.copy(error = "Project name is required"); return
        }
        if (s.repoOwner.isBlank()) {
            _state.value = s.copy(error = "Repository owner is required"); return
        }
        if (s.repoName.isBlank()) {
            _state.value = s.copy(error = "Repository name is required"); return
        }
        if (s.selectedFolderUri == null) {
            _state.value = s.copy(error = "Please select a folder"); return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isAdding = true, error = null, setupStep = "Validating folder...")

            try {
                if (!SafUriHelper.isValidDirectory(application, s.selectedFolderUri)) {
                    _state.value = _state.value.copy(
                        isAdding = false, setupStep = "",
                        error = "Selected folder is not valid or accessible"
                    )
                    return@launch
                }

                SafUriHelper.takePersistablePermissions(application, s.selectedFolderUri)

                val localPath = SafUriHelper.resolveLocalPath(application, s.selectedFolderUri)

                // Critical: if we can't resolve a real /storage/... path, JGit will fail
                if (localPath == null || localPath.startsWith("content://")) {
                    _state.value = _state.value.copy(
                        isAdding = false, setupStep = "",
                        error = "Cannot resolve folder path. Please grant 'All Files Access' permission:\nSettings > Apps > GitSync > Permissions > Files and media > Allow management of all files"
                    )
                    return@launch
                }

                val username = authRepository.getUsername()
                val token = authRepository.getToken()
                val branch = s.branch.ifBlank { "main" }
                val repoUrl = "https://github.com/${s.repoOwner.trim()}/${s.repoName.trim()}.git"

                _state.value = _state.value.copy(setupStep = "Initializing git repository...")

                val setupResult = gitRepository.setupAndPushProject(
                    projectPath = localPath,
                    remoteUrl = repoUrl,
                    username = username,
                    token = token,
                    branch = branch
                )

                if (setupResult.isFailure) {
                    _state.value = _state.value.copy(
                        isAdding = false, setupStep = "",
                        error = setupResult.exceptionOrNull()?.message ?: "Setup failed"
                    )
                    return@launch
                }

                _state.value = _state.value.copy(setupStep = "Saving project...")

                val commitHash = setupResult.getOrNull() ?: ""
                val newProjectId = projectRepository.addProject(
                    name = s.projectName.trim(),
                    localPath = localPath,
                    safUri = s.selectedFolderUri.toString(),
                    repoOwner = s.repoOwner.trim(),
                    repoName = s.repoName.trim(),
                    branch = branch,
                    uriPermission = s.selectedFolderUri.toString()
                )
                // Immediately record the successful push so "Last Sync" shows correctly
                val newProject = projectRepository.getProjectByIdOnce(newProjectId)
                if (newProject != null) {
                    projectRepository.updateProject(
                        newProject.copy(
                            lastSyncTime = System.currentTimeMillis(),
                            lastCommitHash = commitHash,
                            lastCommitMessage = "Initial sync via GitSync"
                        )
                    )
                }

                _state.value = _state.value.copy(
                    isAdding = false, setupStep = "",
                    showAddDialog = false,
                    selectedFolderUri = null,
                    projectName = "", repoName = "",
                    error = null
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isAdding = false, setupStep = "",
                    error = "Add failed: ${e.message}"
                )
            }
        }
    }

    fun removeProject(project: Project) {
        viewModelScope.launch {
            if (project.safUri.isNotBlank()) {
                try {
                    SafUriHelper.releasePersistablePermissions(
                        application,
                        android.net.Uri.parse(project.safUri)
                    )
                } catch (_: Exception) { }
            }
            projectRepository.removeProject(project.id)
        }
    }

    fun refresh() {
        loadProjects()
    }
}
