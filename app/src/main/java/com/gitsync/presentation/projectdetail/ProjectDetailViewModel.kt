package com.gitsync.presentation.projectdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.io.File
import javax.inject.Inject

data class ProjectDetailState(
    val project: Project? = null,
    val isLoading: Boolean = true,
    val isPushing: Boolean = false,
    val isPulling: Boolean = false,
    val isInitializing: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.UNKNOWN,
    val detectedBranch: String = "",
    val isGitRepo: Boolean = false,
    val modifiedFiles: List<String> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val gitRepository: GitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val projectId: Long = savedStateHandle["projectId"] ?: -1

    private val _state = MutableStateFlow(ProjectDetailState())
    val state: StateFlow<ProjectDetailState> = _state.asStateFlow()

    init {
        loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            if (projectId == -1L) {
                _state.value = _state.value.copy(isLoading = false, error = "Invalid project")
                return@launch
            }

            projectRepository.getProjectById(projectId).collect { project ->
                if (project != null) {
                    val projectDir = java.io.File(project.localPath)
                    val isRepo = gitRepository.isGitRepository(project.localPath)

                    // Store permission error to show correct message in UI
                    val permissionError = if (!projectDir.canRead())
                        "Storage permission denied. Grant 'All Files Access' in Settings > Apps > GitSync > Permissions."
                    else null

                    val currentBranch = if (isRepo) {
                        gitRepository.getCurrentBranch(project.localPath)
                            .getOrDefault("")
                    } else ""

                    val status = if (isRepo) {
                        gitRepository.getSyncStatus(project.localPath)
                    } else SyncStatus.ERROR

                    val files = if (isRepo) {
                        gitRepository.getModifiedFiles(project.localPath)
                            .getOrDefault(emptyList())
                    } else emptyList()

                    _state.value = _state.value.copy(
                        project = project.copy(
                            syncStatus = status,
                            modifiedFiles = files,
                            branch = currentBranch.ifEmpty { project.branch }
                        ),
                        syncStatus = status,
                        detectedBranch = currentBranch,
                        isGitRepo = isRepo,
                        modifiedFiles = files,
                        isLoading = false,
                        error = permissionError
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Project not found")
                }
            }
        }
    }

    fun initRepo() {
        viewModelScope.launch {
            val project = _state.value.project ?: return@launch
            _state.value = _state.value.copy(isInitializing = true, error = null)

            try {
                gitRepository.initRepository(project.localPath)
                    .getOrThrow()

                _state.value = _state.value.copy(
                    isInitializing = false,
                    isGitRepo = true,
                    successMessage = "Repository initialized"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isInitializing = false,
                    error = e.message ?: "Failed to initialize repository"
                )
            }
        }
    }

    fun pushNow() {
        viewModelScope.launch {
            val project = _state.value.project ?: return@launch
            _state.value = _state.value.copy(isPushing = true, error = null)

            try {
                val repoDir = File(project.localPath)
                if (!repoDir.exists()) {
                    _state.value = _state.value.copy(
                        isPushing = false,
                        error = "Project directory does not exist"
                    )
                    return@launch
                }

                if (!gitRepository.isGitRepository(project.localPath)) {
                    _state.value = _state.value.copy(
                        isPushing = false,
                        error = "Not a Git repository. Initialize it first."
                    )
                    return@launch
                }

                val hasChanges = gitRepository.hasChanges(project.localPath)
                    .getOrThrow()

                if (hasChanges) {
                    gitRepository.addAll(project.localPath)
                        .getOrThrow()

                    val hash = gitRepository.commit(
                        project.localPath,
                        "Auto-sync from GitSync"
                    ).getOrThrow()

                    val currentBranch = gitRepository.getCurrentBranch(project.localPath)
                        .getOrDefault(project.branch)

                    val username = authRepository.getUsername()
                    val token = authRepository.getToken()

                    gitRepository.push(
                        project.localPath,
                        username,
                        token,
                        currentBranch
                    ).getOrThrow()

                    projectRepository.updateProject(
                        project.copy(
                            lastSyncTime = System.currentTimeMillis(),
                            lastCommitHash = hash.take(7),
                            lastCommitMessage = "Auto-sync from GitSync",
                            branch = currentBranch
                        )
                    )

                    _state.value = _state.value.copy(
                        isPushing = false,
                        successMessage = "Successfully pushed to $currentBranch"
                    )
                } else {
                    _state.value = _state.value.copy(
                        isPushing = false,
                        successMessage = "Nothing to push - working tree clean"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isPushing = false,
                    error = e.message ?: "Push failed"
                )
            }
        }
    }

    fun pullNow() {
        viewModelScope.launch {
            val project = _state.value.project ?: return@launch
            _state.value = _state.value.copy(isPulling = true, error = null)

            try {
                if (!gitRepository.isGitRepository(project.localPath)) {
                    _state.value = _state.value.copy(
                        isPulling = false,
                        error = "Not a Git repository. Initialize it first."
                    )
                    return@launch
                }

                val currentBranch = gitRepository.getCurrentBranch(project.localPath)
                    .getOrDefault(project.branch)
                val username = authRepository.getUsername()
                val token = authRepository.getToken()

                gitRepository.pull(
                    project.localPath,
                    username,
                    token,
                    currentBranch
                ).getOrThrow()

                _state.value = _state.value.copy(
                    isPulling = false,
                    successMessage = "Successfully pulled from $currentBranch"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isPulling = false,
                    error = e.message ?: "Pull failed"
                )
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, successMessage = null)
    }
}
