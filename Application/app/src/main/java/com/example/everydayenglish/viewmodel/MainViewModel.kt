package com.example.everydayenglish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(

    private val userProfileRepository:
    UserProfileRepository,

    private val appPreferencesRepository:
    AppPreferencesRepository

) : ViewModel() {

    private val _uiState =
        MutableStateFlow(MainUiState())

    val uiState = _uiState.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {

        viewModelScope.launch {

            val userId: String =
                appPreferencesRepository
                    .getUserId()

            val profile =
                userProfileRepository
                    .getUserProfile(userId)

            if (profile != null) {

                _uiState.update {

                    it.copy(

                        userName =
                            profile.userName,

                        todayProgress =
                            profile.todayProgress,

                        dailyGoal =
                            profile.dailyGoal
                    )
                }
            }
        }
    }
    fun refresh(){
        loadUserInfo()
    }
}

data class MainUiState(

    val userName: String = "",

    val todayProgress: Int = 0,

    val dailyGoal: Int = 10
)