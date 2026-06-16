package com.example.everydayenglish.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.BuildConfig
import com.example.everydayenglish.data.PainterDefaults
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.ExerciseRepository
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
import java.util.UUID

data class UpdateInfo(val versionName: String, val downloadUrl: String)

class SplashViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val userProfileRepository: UserProfileRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val context: Context
) : ViewModel() {

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

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
        val steps = listOf(
            Step(weight = 2) {
                _statusText.value = "Checking exercises..."
                val exercises = exerciseRepository.getExercisesWithReferenceAnswers()
                if (exercises.isEmpty()) {
                    _statusText.value = "Loading exercises..."
                    exerciseRepository.importExercises()
                }
            },
            Step(weight = 1) {
                _statusText.value = "Setting up profile..."
                val userId = getOrCreateUserId()
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
            },
            Step(weight = 1) {
                _statusText.value = "Almost ready..."
                delay(300L)
            }
        )

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

    private fun getOrCreateUserId(): String {
        val existingId = appPreferencesRepository.getUserId()

        if (existingId.isNotBlank()) {
            return existingId
        }

        val newId = UUID.randomUUID().toString()
        appPreferencesRepository.saveUserId(newId)
        return newId
    }

    private data class Step(
        val weight: Int,
        val action: suspend () -> Unit
    )
}
