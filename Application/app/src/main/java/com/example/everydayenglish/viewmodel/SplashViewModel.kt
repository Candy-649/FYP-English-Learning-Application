package com.example.everydayenglish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.PainterDefaults
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.ExerciseRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.entity.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SplashViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val userProfileRepository: UserProfileRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        viewModelScope.launch {
            initializeApp()
        }
    }

    private suspend fun initializeApp() {
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
