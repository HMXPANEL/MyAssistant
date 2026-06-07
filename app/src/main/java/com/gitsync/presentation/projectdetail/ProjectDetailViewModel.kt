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
    val syncStatus: SyncStatus = SyncStatus.UNKNOWN,
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
                    val status = gitRepository.getSyncStatus(project.localPath)
                    val files = gitRepository.getModifiedFiles(project.localPath)
                        .getOrDefault(emptyList())

                    _state.value = _state.value.copy(
                        project = project.copy(
                            syncStatus = status,
                            modifiedFiles = files
                        ),
                        syncStatus = status,
                        modifiedFiles = files,
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Project not found")
                }
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
                    gitRepository.initRepository(project.localPath)
                }

                gitRepository.addAll(project.localPath)
                    .getOrThrow()

                val hash = gitRepository.commit(
                    project.localPath,
                    "Auto-sync from GitSync"
                ).getOrThrow()

                val username = authRepository.getUsername()
                val token = authRepository.getToken()

                gitRepository.push(
                    project.localPath,
                    username,
                    token,
                    project.branch
                ).getOrThrow()

                projectRepository.updateProject(
                    project.copy(
                        lastSyncTime = System.currentTimeMillis(),
                        lastCommitHash = hash.take(7),
                        lastCommitMessage = "Auto-sync from GitSync"
                    )
                )

                _state.value = _state.value.copy(
                    isPushing = false,
                    successMessage = "Successfully pushed to ${project.branch}"
                )
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
                val username = authRepository.getUsername()
                val token = authRepository.getToken()

                gitRepository.pull(
                    project.localPath,
                    username,
                    token,
                    project.branch
                ).getOrThrow()

                _state.value = _state.value.copy(
                    isPulling = false,
                    successMessage = "Successfully pulled from ${project.branch}"
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
