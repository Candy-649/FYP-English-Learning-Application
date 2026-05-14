package com.example.everydayenglish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingViewModel(
    private val appPreferencesRepository: AppPreferencesRepository
): ViewModel(){
    private val _uiState =
        MutableStateFlow(SettingUiState())

    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {

        viewModelScope.launch {

            try {

                val dailyGoal =
                    appPreferencesRepository
                        .getDailyGoal()

                val darkMode =
                    appPreferencesRepository
                        .getDarkMode()

                _uiState.update {

                    it.copy(
                        dailyGoal = dailyGoal,
                        darkModeEnabled = darkMode
                    )
                }

            } catch (_: Exception) {

            }
        }
    }

    fun updateDailyGoal(goal: Int) {

        _uiState.update {
            it.copy(dailyGoal = goal)
        }
    }

    fun updateDarkMode(enabled: Boolean) {

        _uiState.update {
            it.copy(darkModeEnabled = enabled)
        }
    }

    fun updateNotificationEnabled(
        enabled: Boolean
    ) {

        _uiState.update {
            it.copy(notificationEnabled = enabled)
        }
    }

    fun updateLanguage(language: AppLanguage) {

        _uiState.update {
            it.copy(language = language)
        }
    }

    fun saveSettings() {

        viewModelScope.launch {

            try {

                val state = _uiState.value

                appPreferencesRepository
                    .saveDailyGoal(
                        state.dailyGoal
                    )

                appPreferencesRepository
                    .saveDarkMode(
                        state.darkModeEnabled
                    )

            } catch (_: Exception) {

            }
        }
    }
}

data class SettingUiState(
    val recentSentenceCount: Int = 20,
    val cacheSizeText: String = "0 MB",
    val notificationEnabled: Boolean = true,
    val dailyGoal: Int = 10,
    val darkModeEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH
)

enum class AppLanguage {
    ENGLISH,
    CHINESE
}