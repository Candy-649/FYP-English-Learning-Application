package com.example.everydayenglish.viewmodel

import android.content.Context
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
import com.example.everydayenglish.data.Repository.AvatarStorageRepository
import com.example.everydayenglish.data.Repository.DailyCompletionRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.entity.UserProfile
import com.example.everydayenglish.util.prefetchImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ln

enum class CropTarget { AVATAR, BACKGROUND }

data class CropRequest(val sourceUri: Uri, val target: CropTarget)

class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val dailyCompletionRepository: DailyCompletionRepository,
    private val avatarStorageRepository: AvatarStorageRepository,
    private val appContext: Context
) : ViewModel() {

    fun refresh(){
        loadProfile()
    }

    private val _uiState =
        MutableStateFlow(ProfileUiState())

    val uiState = _uiState.asStateFlow()
    private var cachedProfile: UserProfile? = null

    // Transient state for the crop screen: which image was just picked and what it's for.
    // Not persisted, not part of ProfileUiState - it only exists for the duration of the
    // pick -> crop -> confirm flow.
    private val _cropRequest = MutableStateFlow<CropRequest?>(null)
    val cropRequest = _cropRequest.asStateFlow()

    init {
        loadProfile()
    }

    fun setEditing(editing: Boolean) {
        _uiState.update { it.copy(isEditing = editing) }
    }

    fun requestCrop(sourceUri: Uri, target: CropTarget) {
        _cropRequest.value = CropRequest(sourceUri, target)
    }

    fun cancelCrop() {
        _cropRequest.value = null
    }

    // Called by the crop screen once it has produced a cropped/resized local file Uri.
    // Routes it to the right field based on what was requested, then clears the request.
    fun consumeCropResult(croppedUri: Uri) {
        when (_cropRequest.value?.target) {
            CropTarget.AVATAR -> updateAvatar(croppedUri)
            CropTarget.BACKGROUND -> updateBackground(croppedUri)
            null -> Log.w("ProfileVM", "consumeCropResult called with no pending crop request")
        }
        _cropRequest.value = null
    }

    private fun loadProfile() {
        viewModelScope.launch {
            Log.d("ProfileVM", "loadProfile called")
            try {
                val userId = appPreferencesRepository.getUserId()
                val profile = userProfileRepository.getUserProfileForToday(userId) ?: return@launch
                cachedProfile = profile
                val completedDays = dailyCompletionRepository.getCompletedDays(userId)
                val streak = calculateStreak(completedDays)
                _uiState.update { current ->
                    Log.d("ProfileVM", "loadProfile updating userAvatar: ${profile.avatarUri}")
                    current.copy(
                        userName = profile.userName,
                        bio = profile.bio,
                        userAvatar = if (current.isEditing) current.userAvatar else profile.avatarUri,
                        profileBackground = if (current.isEditing) current.profileBackground else profile.profileBackgroundUri,
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
                val base = cachedProfile ?: run {
                    Log.w("ProfileVM", "saveProfile: cachedProfile is null, aborting")
                    return@launch
                }
                val userId = appPreferencesRepository.getUserId()

                // Only re-upload when the URI is a freshly picked local file.
                // Already-remote (https) or default (android.resource) URIs are left as-is.
                val finalAvatarUri = uploadIfLocal(userId, state.userAvatar, isAvatar = true)
                val finalBackgroundUri = uploadIfLocal(userId, state.profileBackground, isAvatar = false)
                Log.d("ProfileVM", "saveProfile: about to persist avatarUri=$finalAvatarUri")

                // A freshly-uploaded URL has never actually been downloaded on this device -
                // uploading and downloading are separate network round trips. Without this,
                // the first real fetch happens whenever AsyncImage next renders it (even just
                // navigating away and back), which looks like an unexplained reload.
                prefetchImage(appContext, finalAvatarUri)
                prefetchImage(appContext, finalBackgroundUri)

                userProfileRepository.updateUserProfile(
                    base.copy(
                        userName             = state.userName,
                        bio                  = state.bio,
                        avatarUri            = finalAvatarUri,
                        profileBackgroundUri = finalBackgroundUri,
                        dailyGoal            = state.dailyGoal
                    )
                )
                Log.d("ProfileVM", "saveProfile: updateUserProfile completed")

                _uiState.update { it.copy(userAvatar = finalAvatarUri, profileBackground = finalBackgroundUri) }

            } catch (e: Exception) {
                Log.e("ProfileVM", "saveProfile failed", e)
            }
        }
    }

    // Uploads a locally picked image (file:// URI) to Firebase Storage and returns the
    // resulting https download URL. Already-remote or default URIs are returned unchanged.
    // On upload failure, falls back to the local URI so the save still succeeds -
    // that avatar just won't be visible on other devices until the next successful save.
    private suspend fun uploadIfLocal(userId: String, uri: Uri, isAvatar: Boolean): Uri {
        if (uri.scheme != "file") return uri

        val result = if (isAvatar) {
            avatarStorageRepository.uploadAvatar(userId, uri)
        } else {
            avatarStorageRepository.uploadBackground(userId, uri)
        }

        return result.getOrElse { e ->
            Log.w("ProfileVM", "Image upload failed, keeping local URI: ${e.message}")
            uri
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
 * Proportionally sizes the bubbles.
 * - minSize / maxSize control the bubble size range
 * - if all values are 0, sizes are distributed evenly
 * - when values differ a lot, sqrt damping keeps small bubbles from being
 *   squashed down to the minimum size
 */
fun calculateProportionalBubbles(
    bubbles: List<ProfileBubble>,
    minSize: Dp = 96.dp,
    maxSize: Dp = 186.dp,
    minFontSize: TextUnit = 16.sp,
    maxFontSize: TextUnit = 32.sp
): List<ProfileBubble> {

    if (bubbles.isEmpty()) return bubbles

    // sqrt damping to shrink extreme differences
    val dampedValues = bubbles.map { kotlin.math.sqrt(it.value.toFloat() + 1f) }
    val minVal = dampedValues.min()
    val maxVal = dampedValues.max()
    val range = maxVal - minVal

    return bubbles.mapIndexed { index, bubble ->
        // normalize to 0..1; if all values are equal, normalize to 0.5 (centered)
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
    val todayProgress: Int = 0,
    val isEditing: Boolean = false
)