package com.example.everydayenglish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.adaptiveEngine.TenseCategory
import com.example.everydayenglish.data.Repository.BanditRepository
import com.example.everydayenglish.data.Repository.ExerciseRepository
import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.entity.ExerciseRecord
import com.example.everydayenglish.data.entity.ExerciseWithReferenceAnswers
import com.example.everydayenglish.data.entity.ReferenceAnswer
import com.example.everydayenglish.data.entity.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

// Default user ID for a single-user app
private const val DEFAULT_USER_ID = "1"

class ExerciseViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val banditRepository: BanditRepository,
    private val recordRepository: RecordRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSession()
    }

    // ─────────────────────────────────────────────────────────────
    // Session bootstrap
    // ─────────────────────────────────────────────────────────────

    /**
     * Entry point. Loads the user profile first, then tries to restore
     * an in-progress session. If none exists (or the previous daily goal
     * was fully completed), fetches a fresh batch.
     */
    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val profile = userProfileRepository.getUserProfile(DEFAULT_USER_ID)
                    ?: UserProfile(userId = DEFAULT_USER_ID)

                // A "session" lives in the ViewModel's queue.
                // On a fresh start the queue is empty, so we always fetch.
                fetchExerciseBatch(profile)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Fetches [UserProfile.dailyGoal] exercises via the bandit, shuffles
     * them, and places them in the queue. Then advances to the first one.
     */
    private suspend fun fetchExerciseBatch(profile: UserProfile) {
        val dailyGoal = profile.dailyGoal.coerceAtLeast(1)
        val batch = mutableListOf<ExerciseWithReferenceAnswers>()

        repeat(dailyGoal) {
            val category = banditRepository.selectNextCategory()
            val exercise = exerciseRepository
                .getExercisesByCategory(category)
                .randomOrNull()
            if (exercise != null) {
                val withAnswers =
                    exerciseRepository.getExercisesWithReferenceAnswersById(exercise.id)
                batch.add(withAnswers)
            }
        }

        if (batch.isEmpty()) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "No exercises available.")
            }
            return
        }

        _uiState.update {
            it.copy(
                exerciseQueue    = batch,
                currentIndex     = 0,
                dailyGoal        = dailyGoal,
                todayProgress    = profile.todayProgress,
                totalAnswered    = 0,
                correctCount     = 0,
                feedbackState    = null,
                userAnswer       = "",
                isSessionDone    = false,
                isLoading        = false,
                errorMessage     = null,
                currentExercise  = batch.first()
            )
        }
    }

    // ─────────────────────────────────────────────────────────────
    // User interactions
    // ─────────────────────────────────────────────────────────────

    fun updateUserAnswer(answer: String) {
        _uiState.update { it.copy(userAnswer = answer) }
    }

    /**
     * Evaluates the current answer, persists an [ExerciseRecord], updates
     * the bandit, and surfaces feedback to the UI.
     */
    fun submitAnswer() {
        viewModelScope.launch {
            val state       = _uiState.value
            val exercise    = state.currentExercise ?: return@launch
            val userAnswer  = state.userAnswer.trim()
            if (userAnswer.isBlank()) return@launch

            // ── Evaluation (replace with real NLP when ready) ──────────
            val isCorrect     = Random.nextBoolean()
            val matchedAnswer = exercise.answers.randomOrNull()
            // ────────────────────────────────────────────────────────────

            // Persist record
            val record = ExerciseRecord(
                promptId = exercise.exercise.id,
                userId = DEFAULT_USER_ID.toIntOrNull() ?: 1,
                referId = matchedAnswer?.referId ?: -1,
                userAnswer = userAnswer,
                isCorrect = isCorrect,
                grammar = null,      // fill once NLP is wired
                semanticScore = null,
                feedback = null,
                timestamp = System.currentTimeMillis()
            )
            recordRepository.insertExerciseRecord(record)

            // Update bandit
            val category = matchedAnswer
                ?.tense
                ?.let { TenseCategory.fromTenseString(it) }

            if (category != null) {
                banditRepository.update(
                    category  = category,
                    isCorrect = isCorrect,
                    timestamp = System.currentTimeMillis()
                )
            }

            // Update correct-sentence count in profile if correct
            if (isCorrect) {
                userProfileRepository.incrementSentencesCompleted(DEFAULT_USER_ID)
            }

            _uiState.update {
                it.copy(
                    totalAnswered = it.totalAnswered + 1,
                    correctCount  = if (isCorrect) it.correctCount + 1 else it.correctCount,
                    feedbackState = FeedbackState(
                        isCorrect              = isCorrect,
                        matchedReferenceAnswer = matchedAnswer,
                        feedback               = null,
                        semanticScore          = null,
                        grammar                = null
                    )
                )
            }
        }
    }

    /**
     * Dismisses the feedback dialog, increments [todayProgress] in the
     * profile, and either advances to the next exercise or marks the
     * session as complete (triggering [totalStudyDays] + 1).
     */
    fun goToNextExercise() {
        viewModelScope.launch {
            val state    = _uiState.value
            val nextIndex = state.currentIndex + 1

            // Always increment todayProgress when the user moves on
            val newProgress = state.todayProgress + 1
            userProfileRepository.updateTodayProgress(newProgress, DEFAULT_USER_ID)

            if (nextIndex >= state.exerciseQueue.size) {
                // ── Session complete ────────────────────────────────────
                userProfileRepository.incrementStudyDays(DEFAULT_USER_ID)

                _uiState.update {
                    it.copy(
                        todayProgress = newProgress,
                        feedbackState = null,
                        isSessionDone = true,
                        currentExercise = null
                    )
                }
            } else {
                // ── Advance to next exercise ────────────────────────────
                _uiState.update {
                    it.copy(
                        currentIndex    = nextIndex,
                        currentExercise = it.exerciseQueue[nextIndex],
                        todayProgress   = newProgress,
                        feedbackState   = null,
                        userAnswer      = ""
                    )
                }
            }
        }
    }

    /**
     * Starts a brand-new session (e.g. after completing all exercises or
     * on explicit retry). Re-reads the profile so dailyGoal is fresh.
     */
    fun restartSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val profile = userProfileRepository.getUserProfile(DEFAULT_USER_ID)
                    ?: UserProfile(userId = DEFAULT_USER_ID)
                fetchExerciseBatch(profile)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// State classes
// ─────────────────────────────────────────────────────────────────────────────

data class ExerciseUiState(

    // Current session queue (size == dailyGoal at fetch time)
    val exerciseQueue: List<ExerciseWithReferenceAnswers> = emptyList(),
    val currentIndex: Int = 0,

    // Convenience pointer — always exerciseQueue[currentIndex]
    val currentExercise: ExerciseWithReferenceAnswers? = null,

    // User input
    val userAnswer: String = "",

    // Session-level counters (reset each session)
    val totalAnswered: Int = 0,
    val correctCount: Int = 0,

    // Profile-level progress (persisted)
    val dailyGoal: Int = 10,
    val todayProgress: Int = 0,

    // True when the user has finished all exercises in the queue
    val isSessionDone: Boolean = false,

    // Feedback dialog shown after submitting
    val feedbackState: FeedbackState? = null,

    // Loading / error
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class FeedbackState(
    val isCorrect: Boolean,
    val matchedReferenceAnswer: ReferenceAnswer? = null,
    val feedback: String?,
    val semanticScore: Double?,
    val grammar: String?
)