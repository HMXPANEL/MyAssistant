package com.gitsync.presentation.workflow

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitsync.domain.model.WorkflowRun
import com.gitsync.domain.model.WorkflowStatus
import com.gitsync.domain.repository.AuthRepository
import com.gitsync.domain.repository.ProjectRepository
import com.gitsync.domain.repository.WorkflowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkflowState(
    val runs: List<WorkflowRun> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WorkflowViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workflowRepository: WorkflowRepository,
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val projectId: Long = savedStateHandle["projectId"] ?: -1

    private val _state = MutableStateFlow(WorkflowState())
    val state: StateFlow<WorkflowState> = _state.asStateFlow()

    init {
        loadWorkflows()
    }

    private fun loadWorkflows() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            workflowRepository.getStoredWorkflowRuns(projectId).collect { runs ->
                _state.value = _state.value.copy(
                    runs = runs,
                    isLoading = false
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true)

            val project = projectRepository.getProjectByIdOnce(projectId)
            if (project != null) {
                val token = authRepository.getToken()
                val result = workflowRepository.getWorkflowRuns(
                    project.repoOwner,
                    project.repoName,
                    token
                )
                result.onSuccess { runs ->
                    workflowRepository.saveWorkflowRuns(projectId, runs)
                    _state.value = _state.value.copy(
                        isRefreshing = false,
                        error = null
                    )
                }.onFailure { e ->
                    _state.value = _state.value.copy(
                        isRefreshing = false,
                        error = e.message
                    )
                }
            } else {
                _state.value = _state.value.copy(
                    isRefreshing = false,
                    error = "Project not found"
                )
            }
        }
    }

}
