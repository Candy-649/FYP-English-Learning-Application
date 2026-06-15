package com.example.everydayenglish.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.AttemptRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
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
                if (isNewDay(profile.lastStudiedDate) && profile.todayProgress > 0) {
                    userProfileRepository.updateTodayProgress(0, System.currentTimeMillis(), userId)
                } else {
                    _uiState.update {
                        it.copy(
                            todayProgress = profile.todayProgress,
                            dailyGoal     = profile.dailyGoal,
                            userAvatar    = profile.avatarUri,
                        )
                    }
                }
            }
        }
    }



    private fun isNewDay(lastMs: Long): Boolean {
        if (lastMs == 0L) return true   // 旧数据没记录日期，保守起见也重置
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return lastMs < cal.timeInMillis
    }
}

data class MainUiState(

    val todayProgress: Int = 0,

    val dailyGoal: Int = 10,
    val userAvatar: Uri = Uri.EMPTY
)