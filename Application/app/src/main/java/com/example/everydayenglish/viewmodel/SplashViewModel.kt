package com.example.everydayenglish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.BuildConfig
import com.example.everydayenglish.data.PainterDefaults
import com.example.everydayenglish.data.Repository.AuthRepository
import com.example.everydayenglish.data.Repository.ExerciseRepository
import com.example.everydayenglish.data.Repository.SyncRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.entity.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UpdateInfo(val versionName: String, val downloadUrl: String)

class SplashViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val userProfileRepository: UserProfileRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    // Splash 跑完之后，导航层（ScreenContent）读这个字段决定落地到 Gate（AuthScreen）
    // 还是直接进 MainScreen。只在 initializeApp() 跑完之前是有意义的，跑完就定型了。
    private val _needsAuthGate = MutableStateFlow(false)
    val needsAuthGate: StateFlow<Boolean> = _needsAuthGate.asStateFlow()

    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        viewModelScope.launch {
            initializeApp()
        }
    }

    private suspend fun initializeApp() {
        _statusText.value = "Checking for updates..."
        checkForUpdate()
        if (_updateInfo.value != null) return

        // 完全没有 Firebase session（第一次装、或者刚登出）才需要走 Gate；
        // 只要有 session（不管是匿名还是正式账号），都是"回头客"，直接跳过 Gate。
        val hasSession = authRepository.currentUserId != null
        _needsAuthGate.value = !hasSession

        val steps = mutableListOf(
            Step(weight = 2) {
                _statusText.value = "Checking exercises..."
                val exercises = exerciseRepository.getExercisesWithReferenceAnswers()
                if (exercises.isEmpty()) {
                    _statusText.value = "Loading exercises..."
                    exerciseRepository.importExercises()
                }
            }
        )

        // 没有 session 的话，下面这两步（同步、建本地 profile）都没有 userId 可用，
        // 干脆不跑——等用户在 Gate 选完（Continue without account / Login / Sign up）
        // 之后，AuthViewModel 那边会负责把这两件事补上。
        if (hasSession) {
            val uid = authRepository.currentUserId!!
            steps += Step(weight = 1) {
                // Firebase 的登录状态会跨重启自动保留，已登录的话每次开 app 都拉一次最新数据，
                // 这样在另一台设备上做的题，回到这台设备打开 app 就能看到。
                _statusText.value = "Syncing your data..."
                runCatching { syncRepository.pullAndMerge(uid) }
            }
            steps += Step(weight = 1) {
                _statusText.value = "Setting up profile..."
                ensureProfileExists(uid)
            }
        }

        steps += Step(weight = 1) {
            _statusText.value = "Almost ready..."
            delay(300L)
        }

        val totalWeight = steps.sumOf { it.weight }
        var completedWeight = 0

        steps.forEach { step ->
            step.action()
            completedWeight += step.weight
            _progress.value = (completedWeight * 100) / totalWeight
        }

        _statusText.value = "Ready!"
        _isReady.value = true
    }

    private suspend fun ensureProfileExists(userId: String) {
        val profile = userProfileRepository.getUserProfile(userId)
        if (profile == null) {
            val shortId = userId.replace("-", "").take(3).uppercase()
            val defaultName = "user$shortId"
            userProfileRepository.insertUserProfile(
                UserProfile(
                    userId = userId,
                    userName = defaultName,
                    avatarUri = PainterDefaults.defaultAvatarUri,
                    profileBackgroundUri = PainterDefaults.defaultProfileBackgroundUri)
            )
        }
    }

    private suspend fun checkForUpdate() {
        try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.github.com/repos/Candy-649/FYP-English-Learning-Application/releases/latest")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext
                val body = response.body?.string() ?: return@withContext
                val json = JSONObject(body)
                val remoteVersion = json.getString("tag_name").removePrefix("v")
                if (remoteVersion == BuildConfig.VERSION_NAME) return@withContext
                val assets = json.getJSONArray("assets")
                if (assets.length() == 0) return@withContext
                val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                _updateInfo.value = UpdateInfo(remoteVersion, downloadUrl)
            }
        } catch (_: Exception) { } // 网络失败就跳过，不阻断
    }

    private data class Step(
        val weight: Int,
        val action: suspend () -> Unit
    )
}
