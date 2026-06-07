package com.gitsync.presentation.projects

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitsync.domain.model.Project
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
    val error: String? = null
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
                    val status = gitRepository.getSyncStatus(project.localPath)
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
        val docFile = DocumentFile.fromTreeUri(application, uri)
        val name = docFile?.name ?: "Untitled"

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

        viewModelScope.launch {
            _state.value = _state.value.copy(isAdding = true, error = null)

            try {
                val token = authRepository.getToken()

                if (s.selectedFolderUri != null) {
                    application.contentResolver.takePersistableUriPermission(
                        s.selectedFolderUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

                val uriString = s.selectedFolderUri?.toString() ?: ""
                val localPath = if (s.selectedFolderUri != null) {
                    try {
                        val docId = DocumentsContract.getTreeDocumentId(s.selectedFolderUri)
                        val parts = docId.split(":")
                        val relative = parts.getOrElse(1) { "" }
                        if (parts[0] == "primary") {
                            "${Environment.getExternalStorageDirectory()}/$relative"
                        } else {
                            resolveSecondaryStoragePath(application, parts[0], relative)
                        }
                    } catch (_: Exception) {
                        uriString
                    }
                } else {
                    ""
                }

                val projectId = projectRepository.addProject(
                    name = s.projectName.trim(),
                    localPath = localPath,
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

    fun removeProject(project: Project) {
        viewModelScope.launch {
            projectRepository.removeProject(project.id)
        }
    }

    fun refresh() {
        loadProjects()
    }
}

private fun resolveSecondaryStoragePath(
    application: Application,
    volumeId: String,
    relative: String
): String {
    val storageManager = application.getSystemService(StorageManager::class.java)
    val volume = storageManager.storageVolumes.firstOrNull { it.uuid == volumeId }
    if (volume != null) {
        try {
            val field = StorageVolume::class.java.getDeclaredField("mPath")
            field.isAccessible = true
            val mountPoint = field.get(volume) as String
            return "$mountPoint/$relative"
        } catch (_: Exception) {
            // fall through
        }
    }
    return "/storage/$volumeId/$relative"
}
