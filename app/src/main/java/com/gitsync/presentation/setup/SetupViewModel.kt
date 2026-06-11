package com.gitsync.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gitsync.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupState(
    val username: String = "",
    val token: String = "",
    val branch: String = "main",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SetupState())
    val state: StateFlow<SetupState> = _state.asStateFlow()

    fun onUsernameChanged(value: String) {
        _state.value = _state.value.copy(username = value, error = null)
    }

    fun onTokenChanged(value: String) {
        _state.value = _state.value.copy(token = value, error = null)
    }

    fun onBranchChanged(value: String) {
        _state.value = _state.value.copy(branch = value, error = null)
    }

    fun validateAndSave() {
        val s = _state.value
        if (s.username.isBlank()) {
            _state.value = s.copy(error = "GitHub username is required")
            return
        }
        if (s.token.isBlank()) {
            _state.value = s.copy(error = "Personal Access Token is required")
            return
        }
        if (s.branch.isBlank()) {
            _state.value = s.copy(error = "Branch name is required")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val validationResult = authRepository.validateCredentials(
                username = s.username.trim(),
                token = s.token.trim()
            )

            if (validationResult.isFailure) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = validationResult.exceptionOrNull()?.message
                        ?: "Failed to validate credentials"
                )
                return@launch
            }

            val canonicalUsername = validationResult.getOrNull() ?: s.username.trim()
            authRepository.saveCredentials(
                username = canonicalUsername,
                token = s.token.trim(),
                branch = s.branch.trim()
            )

            _state.value = _state.value.copy(
                isLoading = false,
                isSuccess = true
            )
        }
    }
}
