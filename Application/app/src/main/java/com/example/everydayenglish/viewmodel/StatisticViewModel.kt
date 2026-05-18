package com.example.everydayenglish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.Repository.ExerciseRepository
import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val DEFAULT_USER_ID = "1"

/**
 * Estimated average minutes spent per exercise.
 * Replace with real session-duration tracking once available.
 */
private const val MINUTES_PER_EXERCISE = 2

class StatisticViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val recordRepository: RecordRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // ── Fetch all data sources in parallel ────────────────────
                val profileDeferred = async {
                    userProfileRepository.getUserProfile(DEFAULT_USER_ID)
                }
                val recordsDeferred = async {
                    recordRepository.getAllExerciseRecords()
                }

                val profile = profileDeferred.await()
                val records = recordsDeferred.await()

                // ── 1. Today's progress ───────────────────────────────────
                // Read directly from profile (maintained by ExerciseViewModel)
                val todayStudy = profile?.todayProgress ?: 0

                // ── 2. Study duration estimate ────────────────────────────
                // Until real session timing is tracked, derive from total
                // answered sentences × MINUTES_PER_EXERCISE.
                // Swap this line once a `totalStudyMinutes` column is added
                // to UserProfile:
                //   val studyDuration = profile?.totalStudyMinutes ?: 0
                val studyDuration =
                    (profile?.totalSentencesCompleted ?: 0) * MINUTES_PER_EXERCISE

                // ── 3. Tense accuracy statistics ──────────────────────────
                // For each record we need the tense of the matched reference
                // answer (stored as referId). We batch-fetch all exercises
                // with their answers, then build a lookup map.
                //
                // tensesStatistic: Map<tenseLabel, correctCount>
                // tensesTotalMap:  Map<tenseLabel, totalAttempts>  (for future use)
                val tensesStatistic = mutableMapOf<String, Int>()

                if (records.isNotEmpty()) {
                    // Collect all unique exerciseIds touched in the records
                    val exerciseIds = records.map { it.promptId }.distinct()

                    // Build referId → tense lookup from exercise answers
                    // Uses ExerciseRepository.getExercisesWithReferenceAnswersById
                    val referIdToTense = mutableMapOf<Int, String>()
                    exerciseIds.forEach { exId ->
                        val withAnswers =
                            exerciseRepository.getExercisesWithReferenceAnswersById(exId)
                        withAnswers.answers.forEach { answer ->
                            answer.tense?.let { tense ->
                                referIdToTense[answer.referId] = tense
                            }
                        }
                    }

                    // Aggregate: count correct answers per tense
                    records.forEach { record ->
                        val tense = referIdToTense[record.referId] ?: return@forEach
                        if (record.isCorrect) {
                            tensesStatistic[tense] =
                                (tensesStatistic[tense] ?: 0) + 1
                        }
                    }
                }

                // ── 4. Daily exercise counts (last 7 days) ─────────────────
                // key = day-of-month (or offset day index), value = answers
                //
                // We group records by calendar day relative to today, keeping
                // only the last 7 days. The chart x-axis shows day offsets
                // (0 = 7 days ago … 6 = today) so the line always fills
                // a week-wide window even with sparse data.
                val dailyExercises = buildDailyExerciseMap(records)

                // ── Emit final state ──────────────────────────────────────
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
                    it.copy(
                        isLoading    = false,
                        errorMessage = e.message ?: "Failed to load statistics"
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Groups [records] into a day-offset map covering the last 7 days.
     *
     * Key  = 0..6 where 0 is 6 days ago and 6 is today.
     * Value = total exercises attempted on that day.
     *
     * Days with no activity are included as 0 so the line chart always
     * spans a full week.
     */
    private fun buildDailyExerciseMap(
        records: List<com.example.everydayenglish.data.entity.ExerciseRecord>
    ): Map<Int, Int> {

        val result = (0..6).associateWith { 0 }.toMutableMap()

        val nowMs       = System.currentTimeMillis()
        val todayStart  = startOfDayMs(nowMs)

        records.forEach { record ->
            val diffMs   = todayStart - startOfDayMs(record.timestamp)
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()

            // Only keep records from the last 7 days (diffDays 0..6)
            if (diffDays in 0..6) {
                val dayIndex = 6 - diffDays   // 0 = oldest, 6 = today
                result[dayIndex] = (result[dayIndex] ?: 0) + 1
            }
        }

        return result
    }

    /**
     * Returns the Unix timestamp (ms) for the start of the day
     * that contains [timestampMs], in the device's local time zone.
     */
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
