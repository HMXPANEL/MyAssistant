package com.gitsync.presentation.commits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitsync.domain.model.GitCommit
import com.gitsync.domain.repository.GitRepository
import com.gitsync.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommitsState(
    val commits: List<GitCommit> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CommitsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gitRepository: GitRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val projectId: Long = savedStateHandle["projectId"] ?: -1

    private val _state = MutableStateFlow(CommitsState())
    val state: StateFlow<CommitsState> = _state.asStateFlow()

    init {
        loadCommits()
    }

    private fun loadCommits() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val project = projectRepository.getProjectByIdOnce(projectId)
            if (project != null) {
                val result = gitRepository.getRecentCommits(project.localPath, 50)
                result.onSuccess { commits ->
                    _state.value = CommitsState(
                        commits = commits,
                        isLoading = false
                    )
                }.onFailure { e ->
                    _state.value = CommitsState(
                        isLoading = false,
                        error = e.message
                    )
                }
            } else {
                _state.value = CommitsState(
                    isLoading = false,
                    error = "Project not found"
                )
            }
        }
    }
}
