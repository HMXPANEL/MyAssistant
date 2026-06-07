package com.gitsync.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitsync.domain.model.SyncInterval
import com.gitsync.domain.repository.AuthRepository
import com.gitsync.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val syncInterval: SyncInterval = SyncInterval.DISABLED,
    val isDarkTheme: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val githubUsername: String = "",
    val defaultBranch: String = "",
    val branchInput: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.syncInterval.collect { interval ->
                _state.value = _state.value.copy(syncInterval = interval)
            }
        }
        viewModelScope.launch {
            settingsRepository.isDarkTheme.collect { dark ->
                _state.value = _state.value.copy(isDarkTheme = dark)
            }
        }
        viewModelScope.launch {
            settingsRepository.notificationsEnabled.collect { enabled ->
                _state.value = _state.value.copy(notificationsEnabled = enabled)
            }
        }

        _state.value = _state.value.copy(
            githubUsername = authRepository.getUsername(),
            defaultBranch = authRepository.getDefaultBranch(),
            branchInput = authRepository.getDefaultBranch()
        )
    }

    fun setSyncInterval(interval: SyncInterval) {
        viewModelScope.launch {
            settingsRepository.setSyncInterval(interval)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun updateBranch(branch: String) {
        _state.value = _state.value.copy(branchInput = branch)
    }

    fun saveBranch() {
        viewModelScope.launch {
            authRepository.updateBranch(_state.value.branchInput)
            _state.value = _state.value.copy(
                defaultBranch = _state.value.branchInput
            )
        }
    }

    fun logout() {
        authRepository.clearCredentials()
    }
}
