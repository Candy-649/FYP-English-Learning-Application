package com.example.everydayenglish.viewmodel

import android.net.Uri
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.PainterDefaults
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.entity.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ln

class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(ProfileUiState())

    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {

        viewModelScope.launch {

            try {

                val userId =
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

                            bio =
                                profile.bio,

                            totalStudyDays =
                                profile.totalStudyDays,

                            totalSentencesCompleted =
                                profile.totalSentencesCompleted,

                            currentStreak =
                                profile.currentStreak,

                            dailyGoal =
                                profile.dailyGoal,

                            todayProgress =
                                profile.todayProgress
                        )
                    }
                }

            } catch (_: Exception) {

            }
        }
    }

    fun updateUserName(name: String) {

        _uiState.update {
            it.copy(userName = name)
        }
    }

    fun updateBio(bio: String) {

        _uiState.update {
            it.copy(bio = bio)
        }
    }

    fun updateDailyGoal(goal: Int) {

        _uiState.update {
            it.copy(dailyGoal = goal)
        }
    }

    fun saveProfile() {

        viewModelScope.launch {

            try {

                val state = _uiState.value

                val userId =
                    appPreferencesRepository
                        .getUserId()

                userProfileRepository
                    .updateUserProfile(

                        UserProfile(

                            userId = userId,

                            userName =
                                state.userName,

                            bio =
                                state.bio,

                            totalStudyDays =
                                state.totalStudyDays,

                            totalSentencesCompleted =
                                state.totalSentencesCompleted,

                            currentStreak =
                                state.currentStreak,

                            dailyGoal =
                                state.dailyGoal,

                            todayProgress =
                                state.todayProgress
                        )
                    )

            } catch (_: Exception) {

            }
        }
    }
}

fun calculateBubbleSize(value: Int): Dp {
    return (40 + ln(value.toFloat() + 1) * 20).dp
}
fun calculateFontSize(value: Int): TextUnit {
    return (16 + ln(value.toFloat() + 1) * 4).sp
}

fun ProfileUiState.toBubbles(): List<ProfileBubble>{
    return listOf(
        ProfileBubble(
            id = 1,
            title = "Study Days",
            value = totalStudyDays,
            description = "This is perseverance"
        ),
        ProfileBubble(
            id = 2,
            title = "Sentences",
            value = totalSentencesCompleted,
            description = "Quantitative changes bring about qualitative changes."
        ),
        ProfileBubble(
            id = 3,
            title = "Streak",
            value = currentStreak,
            description = "The necessary path to developing good habits?"
        ),
        ProfileBubble(
            id = 4,
            title = "Daily Goal",
            value = dailyGoal,
            description = "Did you complete today's goal?"
        )
    )
}

data class ProfileBubble(
    val id: Int,
    val title: String,
    val value: Int,
    val description: String
)

data class ProfileUiState(
    val userName: String = "",
    val userAvatar: Uri = PainterDefaults.defaultAvatarUri,
    val bio: String = "About me",
    val profileBackground: Uri = PainterDefaults.defaultProfileBackgroundUri,
    val totalStudyDays: Int = 0,
    val totalSentencesCompleted: Int = 0,
    val currentStreak: Int = 0,
    val dailyGoal: Int = 10,
    val todayProgress: Int = 0
)