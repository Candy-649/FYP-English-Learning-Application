package com.example.everydayenglish.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.AttemptRepository
import com.example.everydayenglish.data.Repository.ExerciseRepository
import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.entity.QuestionAttempt
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit



private const val MINUTES_PER_EXERCISE = 2

class StatisticViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val attemptRepository: AttemptRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val userId: String
        get() = appPreferencesRepository.getUserId()
    private val _uiState = MutableStateFlow(StatisticUiState())
    val uiState = _uiState.asStateFlow()

    init { loadStatistics() }

    fun refresh(){
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val profileDeferred = async { userProfileRepository.getUserProfileForToday(userId) }
                val attemptsDeferred = async { attemptRepository.getAllByUser(userId) }


                val profile = profileDeferred.await()
                val attempts = attemptsDeferred.await()
                Log.d("StatisticVM", "QuestionAttempt count: ${attempts.size}") // 加这一行
                attempts.forEach {
                    Log.d("StatisticVM", "attempt: promptId=${it.promptId} tense=${it.tense} solved=${it.solved} tries=${it.totalTries} accuracy=${it.accuracy}")
                }
                val todayStudy = profile?.todayProgress ?: 0
                val studyDuration = (profile?.totalSentencesCompleted ?: 0) * MINUTES_PER_EXERCISE

                // tense 统计：直接用 attempt.tense，不用再 join
                val tensesStatistic = attempts
                    .groupBy { it.tense }
                    .mapValues { (_, list) -> list.size }

                // 每日统计：逻辑跟原来一样
                val dailyExercises = buildDailyExerciseMap(attempts)

                _uiState.update {
                    it.copy(
                        todayStudy      = todayStudy,
                        studyDuration   = studyDuration,
                        tensesStatistic = tensesStatistic,
                        dailyExercises  = dailyExercises,
                        isLoading       = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load statistics")
                }
            }
        }
    }

    private fun buildDailyExerciseMap(attempts: List<QuestionAttempt>): Map<Int, Int> {
        val result = (0..6).associateWith { 0 }.toMutableMap()
        val nowMs = System.currentTimeMillis()
        val todayStart = startOfDayMs(nowMs)

        attempts.forEach { attempt ->
            val diffMs = todayStart - startOfDayMs(attempt.timestamp)
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
            if (diffDays in 0..6) {
                val dayIndex = 6 - diffDays
                result[dayIndex] = (result[dayIndex] ?: 0) + 1
            }
        }
        return result
    }

    private fun startOfDayMs(timestampMs: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestampMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

data class StatisticUiState(
    /** Number of exercises completed today (= UserProfile.todayProgress). */
    val todayStudy: Int = 0,

    /**
     * Estimated total study time in minutes.
     * Currently derived as totalSentencesCompleted × MINUTES_PER_EXERCISE.
     * Replace with a real `totalStudyMinutes` field in UserProfile once
     * session-duration tracking is implemented.
     */
    val studyDuration: Int = 0,

    /**
     * Correct-answer count per tense label, e.g.
     * { "Past Simple" → 12, "Present Perfect" → 5 }.
     * Drives the bar chart in StatisticScreen.
     */
    val tensesStatistic: Map<String, Int> = emptyMap(),

    /**
     * Exercise-attempt count per day-offset over the last 7 days.
     * Key 0 = 6 days ago, key 6 = today.
     * Drives the line chart in StatisticScreen.
     */
    val dailyExercises: Map<Int, Int> = emptyMap(),

    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
