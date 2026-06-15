package com.example.everydayenglish.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.PainterDefaults
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.DailyCompletionRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.entity.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ln

class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val dailyCompletionRepository: DailyCompletionRepository
) : ViewModel() {

    fun refresh(){
        loadProfile()
    }

    private val _uiState =
        MutableStateFlow(ProfileUiState())

    val uiState = _uiState.asStateFlow()

    private var isEditing = false

    init {
        loadProfile()
    }

    fun setEditing(editing: Boolean) {
        isEditing = editing
    }
    private fun loadProfile() {
        viewModelScope.launch {
            Log.d("ProfileVM", "loadProfile called")
            try {
                val userId = appPreferencesRepository.getUserId()
                val profile = userProfileRepository.getUserProfile(userId) ?: return@launch
                val completedDays = dailyCompletionRepository.getCompletedDays(userId)
                val streak = calculateStreak(completedDays)
                _uiState.update { current ->
                    Log.d("ProfileVM", "loadProfile updating userAvatar: ${profile.avatarUri}")
                    current.copy(
                        userName = profile.userName,
                        bio = profile.bio,
                        userAvatar = if (isEditing) current.userAvatar else profile.avatarUri,
                        profileBackground = if (isEditing) current.profileBackground else profile.profileBackgroundUri,
                        totalStudyDays = profile.totalStudyDays,
                        totalSentencesCompleted = profile.totalSentencesCompleted,
                        currentStreak = streak,
                        dailyGoal = profile.dailyGoal,
                        todayProgress = profile.todayProgress
                    )
                }
            } catch (_: Exception) { }
        }
    }

    private fun calculateStreak(days: List<Int>): Int {
        if (days.isEmpty()) return 0
        val today = java.time.LocalDate.now().toEpochDay().toInt()
        var streak = 0
        var expected = today
        for (day in days) {
            if (day == expected) { streak++; expected-- } else break
        }
        return streak
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

    fun updateAvatar(uri: Uri) {
        Log.d("ProfileVM", "updateAvatar called: $uri")
        _uiState.update { it.copy(userAvatar = uri) }
    }

    fun updateBackground(uri: Uri) {
        _uiState.update { it.copy(profileBackground = uri) }
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
                            avatarUri = state.userAvatar,
                            profileBackgroundUri = state.profileBackground,

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

fun ProfileUiState.toBubbles(): List<ProfileBubble> {

    val rawList = listOf(
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

    return calculateProportionalBubbles(rawList)
}

/**
 * 按比例分配泡泡大小。
 * - minSize / maxSize 控制泡泡的尺寸范围
 * - 所有值为 0 时平均分配
 * - 值差距极大时，用 sqrt 做阻尼，避免小泡泡被压到最小值
 */
fun calculateProportionalBubbles(
    bubbles: List<ProfileBubble>,
    minSize: Dp = 96.dp,
    maxSize: Dp = 186.dp,
    minFontSize: TextUnit = 16.sp,
    maxFontSize: TextUnit = 32.sp
): List<ProfileBubble> {

    if (bubbles.isEmpty()) return bubbles

    // 用 sqrt 做阻尼，缩小极端差距
    val dampedValues = bubbles.map { kotlin.math.sqrt(it.value.toFloat() + 1f) }
    val minVal = dampedValues.min()
    val maxVal = dampedValues.max()
    val range = maxVal - minVal

    return bubbles.mapIndexed { index, bubble ->
        // 归一化到 0..1，所有值相同时归一化为 0.5（居中）
        val normalized = if (range < 0.001f) {
            0.5f
        } else {
            (dampedValues[index] - minVal) / range
        }

        val sizeDp = minSize + (maxSize - minSize) * normalized
        val fontSize =
            minFontSize.value +
                    (maxFontSize.value - minFontSize.value) * normalized
        val finalFontSize = fontSize.sp
        bubble.copy(
            size = sizeDp,
            fontSize = finalFontSize
        )
    }
}



data class ProfileBubble(
    val id: Int,
    val title: String,
    val value: Int,
    val description: String,
    val size: Dp = 96.dp,
    val fontSize: TextUnit = 16.sp
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