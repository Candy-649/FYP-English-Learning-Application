package com.example.everydayenglish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.Repository.AuthRepository
import com.example.everydayenglish.data.Repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUserEmail: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(currentUserEmail = authRepository.currentUserEmail)
    )
    val uiState = _uiState.asStateFlow()

    val isLoggedIn: Boolean get() = authRepository.isLoggedIn

    fun refresh() {
        _uiState.update { it.copy(currentUserEmail = authRepository.currentUserEmail) }
    }

    fun switchMode(mode: AuthMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    // onSuccess 由调用方负责导航，ViewModel 不碰 nav
    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password are required.") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = when (state.mode) {
                AuthMode.LOGIN    -> authRepository.login(state.email.trim(), state.password)
                AuthMode.REGISTER -> authRepository.register(state.email.trim(), state.password)
            }
            result.onSuccess { uid ->
                if (state.mode == AuthMode.LOGIN) {
                    // 登录（不是新注册）才需要拉云端数据合并到本地，新注册账号云端本来就是空的
                    runCatching { syncRepository.pullAndMerge(uid) }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUserEmail = authRepository.currentUserEmail,
                        password = ""
                    )
                }
                onSuccess()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Something went wrong.")
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update { AuthUiState(currentUserEmail = null) }
    }
}
