package com.example.everydayenglish.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.OfflineRepository.OfflineAppPreferencesRepository
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingViewModel(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val userProfileRepository: UserProfileRepository      // 新增
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingUiState())
    val uiState = _uiState.asStateFlow()

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch {
            try {

                val darkMode     = appPreferencesRepository.getDarkMode()
                val darkModeOpt  = runCatching {
                    DarkModeOption.valueOf(appPreferencesRepository.getDarkModeOption())
                }.getOrDefault(DarkModeOption.AUTO)
                val notification = appPreferencesRepository.getNotification()

                // sentenceCount 来自 UserProfile
                val userId       = appPreferencesRepository.getUserId()
                val profile      = userProfileRepository.getUserProfile(userId)
                val dailyGoal    = profile?.dailyGoal ?: 10
                val sentenceCount = profile?.recentSentenceCount ?: 20

                val cacheText = (appPreferencesRepository
                        as? OfflineAppPreferencesRepository)
                    ?.getCacheSizeText() ?: "0 MB"

                _uiState.update {
                    it.copy(
                        dailyGoal           = dailyGoal,
                        darkModeEnabled     = darkMode,
                        darkModeOption = darkModeOpt,
                        notificationEnabled = notification,
                        recentSentenceCount = sentenceCount,
                        cacheSizeText       = cacheText
                    )
                }
            } catch (_: Exception) { }
        }
    }

    fun updateDailyGoal(goal: Int) {
        _uiState.update { it.copy(dailyGoal = goal) }
        viewModelScope.launch {
            val userId = appPreferencesRepository.getUserId()
            userProfileRepository.updateDailyGoal(goal, userId)
        }
    }


    fun updateDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(darkModeEnabled = enabled) }
        viewModelScope.launch {
            appPreferencesRepository.saveDarkMode(enabled)
        }
    }

    fun updateDarkModeOption(option: DarkModeOption) {
        _uiState.update { it.copy(darkModeOption = option) }
        viewModelScope.launch {
            appPreferencesRepository.saveDarkModeOption(option.name)
            val readBack = appPreferencesRepository.getDarkModeOption()
            Log.d("DarkMode", "saved=${option.name}, readBack=$readBack")
        }
    }



    fun updateNotificationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationEnabled = enabled) }
        viewModelScope.launch {
            appPreferencesRepository.saveNotification(enabled)
        }
    }


    fun updateSentenceCount(count: Int) {
        _uiState.update { it.copy(recentSentenceCount = count) }
        viewModelScope.launch {
            val userId = appPreferencesRepository.getUserId()
            userProfileRepository.updateSentenceCount(count, userId)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val repo = appPreferencesRepository as? OfflineAppPreferencesRepository ?: return@launch
            repo.clearCache()
            val newSize = repo.getCacheSizeText()
            _uiState.update { it.copy(cacheSizeText = newSize) }
        }
    }
}

data class SettingUiState(
    val recentSentenceCount: Int = 20,
    val cacheSizeText: String = "0 MB",
    val notificationEnabled: Boolean = true,
    val dailyGoal: Int = 10,
    val darkModeEnabled: Boolean = false,
    val darkModeOption: DarkModeOption = DarkModeOption.AUTO,
)
enum class DarkModeOption {
    AUTO, MANUAL;
    override fun toString() = when (this) {
        AUTO   -> "Auto"
        MANUAL -> "Manual"
    }
}