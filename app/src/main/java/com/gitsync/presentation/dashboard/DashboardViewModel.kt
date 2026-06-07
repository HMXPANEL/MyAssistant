package com.gitsync.presentation.dashboard

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
import javax.inject.Inject

data class DashboardState(
    val projects: List<Project> = emptyList(),
    val totalProjects: Int = 0,
    val syncedProjects: Int = 0,
    val pendingProjects: Int = 0,
    val errorProjects: Int = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val gitHubUsername: String = "",
    val defaultBranch: String = ""
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val gitRepository: GitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            _state.value = _state.value.copy(
                gitHubUsername = authRepository.getUsername(),
                defaultBranch = authRepository.getDefaultBranch()
            )

            projectRepository.getAllProjects().collect { projects ->
                var synced = 0
                var pending = 0
                var error = 0

                projects.forEach { project ->
                    val status = gitRepository.getSyncStatus(project.localPath)
                    when (status) {
                        SyncStatus.SYNCED -> synced++
                        SyncStatus.ERROR -> error++
                        else -> pending++
                    }
                }

                _state.value = _state.value.copy(
                    projects = projects,
                    totalProjects = projects.size,
                    syncedProjects = synced,
                    pendingProjects = pending,
                    errorProjects = error,
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true)
            loadDashboard()
        }
    }
}
