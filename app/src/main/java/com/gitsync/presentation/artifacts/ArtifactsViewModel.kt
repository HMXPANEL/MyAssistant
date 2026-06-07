package com.gitsync.presentation.artifacts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitsync.domain.model.Artifact
import com.gitsync.domain.repository.AuthRepository
import com.gitsync.domain.repository.ProjectRepository
import com.gitsync.domain.repository.WorkflowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ArtifactsState(
    val artifacts: List<Artifact> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadedFile: File? = null,
    val downloadProgress: String = "",
    val error: String? = null
)

@HiltViewModel
class ArtifactsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workflowRepository: WorkflowRepository,
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val projectId: Long = savedStateHandle["projectId"] ?: -1

    private val _state = MutableStateFlow(ArtifactsState())
    val state: StateFlow<ArtifactsState> = _state.asStateFlow()

    init {
        loadArtifacts()
    }

    private fun loadArtifacts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val project = projectRepository.getProjectByIdOnce(projectId)
            if (project != null) {
                val token = authRepository.getToken()
                val result = workflowRepository.getArtifacts(
                    project.repoOwner,
                    project.repoName,
                    token
                )
                result.onSuccess { artifacts ->
                    _state.value = ArtifactsState(
                        artifacts = artifacts.filter { !it.expired },
                        isLoading = false
                    )
                }.onFailure { e ->
                    _state.value = ArtifactsState(
                        isLoading = false,
                        error = e.message
                    )
                }
            } else {
                _state.value = ArtifactsState(
                    isLoading = false,
                    error = "Project not found"
                )
            }
        }
    }

    fun refresh() {
        loadArtifacts()
    }

    fun downloadArtifact(artifact: Artifact) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isDownloading = true,
                downloadProgress = "Downloading ${artifact.name}..."
            )

            val project = projectRepository.getProjectByIdOnce(projectId)
            if (project != null) {
                val token = authRepository.getToken()
                val downloadDir = File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    ),
                    "GitSync"
                )

                val result = workflowRepository.downloadArtifact(
                    project.repoOwner,
                    project.repoName,
                    artifact.id,
                    token,
                    downloadDir
                )

                result.onSuccess { file ->
                    _state.value = _state.value.copy(
                        isDownloading = false,
                        downloadedFile = file,
                        downloadProgress = "Downloaded to ${file.absolutePath}"
                    )
                }.onFailure { e ->
                    _state.value = _state.value.copy(
                        isDownloading = false,
                        error = e.message ?: "Download failed"
                    )
                }
            }
        }
    }

    fun clearDownloadState() {
        _state.value = _state.value.copy(downloadedFile = null, downloadProgress = "")
    }
}
