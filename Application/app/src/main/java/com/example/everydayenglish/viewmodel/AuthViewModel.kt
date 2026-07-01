package com.example.everydayenglish.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.PainterDefaults
import com.example.everydayenglish.data.Repository.AuthRepository
import com.example.everydayenglish.data.Repository.SyncRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.entity.UserProfile
import com.example.everydayenglish.util.prefetchImage
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
    private val syncRepository: SyncRepository,
    private val userProfileRepository: UserProfileRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(currentUserEmail = authRepository.currentUserEmail)
    )
    val uiState = _uiState.asStateFlow()

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

    /** Gate 页"Continue without an account"：拿一个匿名 Firebase 账号，补上本地 profile。*/
    fun continueAsGuest(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.continueAsGuest()
                .onSuccess { uid ->
                    ensureProfileExists(uid)
                    // No prefetch needed here: a brand new anonymous account always starts
                    // with the local default avatar/background (see ensureProfileExists),
                    // never a remote URL, so there's nothing to warm the cache for yet.
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Something went wrong.")
                    }
                }
        }
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
                    // 登录一个已存在的正式账号才需要拉云端数据合并到本地；
                    // REGISTER 要么是账号升级（本地数据已经在了，不用拉），要么是全新账号（云端本来就是空的）
                    runCatching { syncRepository.pullAndMerge(uid) }

                    // Login is the one path where avatarUri/profileBackgroundUri can suddenly
                    // become a remote https URL this device has never fetched before (set on
                    // another device, just pulled in above). Warm Coil's cache for it now,
                    // while isLoading is still true and the login button already shows a
                    // spinner - by the time we navigate to Main/Profile it should be cached.
                    userProfileRepository.getUserProfile(uid)?.let { profile ->
                        prefetchImage(appContext, profile.avatarUri)
                        prefetchImage(appContext, profile.profileBackgroundUri)
                    }
                }
                // 兜底：不管走哪条路径，确保这个 uid 名下本地有一份 profile
                // （正常情况下 pullAndMerge 或者升级前的匿名 session 已经有了，这里只是兜底）
                ensureProfileExists(uid)
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

    private suspend fun ensureProfileExists(userId: String) {
        if (userProfileRepository.getUserProfile(userId) != null) return
        val shortId = userId.replace("-", "").take(3).uppercase()
        userProfileRepository.insertUserProfile(
            UserProfile(
                userId = userId,
                userName = "user$shortId",
                avatarUri = PainterDefaults.defaultAvatarUri,
                profileBackgroundUri = PainterDefaults.defaultProfileBackgroundUri
            )
        )
    }
}