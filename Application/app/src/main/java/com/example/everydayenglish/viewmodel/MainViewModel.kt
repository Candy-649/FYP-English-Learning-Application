package com.example.everydayenglish.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.AttemptRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.util.isNewDay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(

    private val userProfileRepository:
    UserProfileRepository,

    private val appPreferencesRepository:
    AppPreferencesRepository,

    ) : ViewModel() {

    private val _uiState =
        MutableStateFlow(MainUiState())

    val uiState = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun refresh() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val userId = appPreferencesRepository.getUserId()
            if (userId.isBlank()) return@launch
            userProfileRepository.observeUserProfile(userId).collect { profile ->
                if (profile == null) return@collect
                if (isNewDay(profile.lastStudiedDate) && (profile.todayProgress > 0 || profile.todayCorrectCount > 0)) {
                    val now = System.currentTimeMillis()
                    userProfileRepository.updateTodayProgress(0, now, userId)
                    userProfileRepository.updateTodayCorrectCount(0, userId)
                } else {
                    _uiState.update {
                        it.copy(
                            todayProgress     = profile.todayProgress,
                            todayCorrectCount = profile.todayCorrectCount,
                            dailyGoal         = profile.dailyGoal,
                            userAvatar        = profile.avatarUri,
                        )
                    }
                }
            }
        }
    }



}

data class MainUiState(

    val todayProgress: Int = 0,
    val todayCorrectCount: Int = 0,

    val dailyGoal: Int = 10,
    val userAvatar: Uri = Uri.EMPTY
)