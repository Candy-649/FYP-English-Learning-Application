package com.example.everydayenglish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.adaptiveEngine.TenseCategory
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
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

data class ArmDebugInfo(
    val category: TenseCategory,
    val mu: Double,
    val n: Int,
    val ucbBonus: Double,
    val ucbScore: Double,
    val isSelected: Boolean
)

data class SelectionStepLog(
    val stepIndex: Int,
    val totalPulls: Int,
    val selectedCategory: TenseCategory,
    val wasUnexplored: Boolean,
    val armDetails: List<ArmDebugInfo>
)

class ExerciseViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val banditRepository: BanditRepository,
    private val recordRepository: RecordRepository,
    private val userProfileRepository: UserProfileRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val userId: String
        get() = appPreferencesRepository.getUserId()


    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSession()
    }


    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val profile = userProfileRepository.getUserProfile(userId = userId)
                    ?: UserProfile(userId = userId)

                fetchExerciseBatch(profile)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }

    private suspend fun fetchExerciseBatch(profile: UserProfile) {
        val dailyGoal = profile.dailyGoal.coerceAtLeast(1)
        val batch = mutableListOf<ExerciseWithReferenceAnswers>()

        repeat(dailyGoal) {
            val armStatesBefore = banditRepository.getStats()
            val totalPulls = banditRepository.getTotalPulls()
            val windowSize = banditRepository.getWindowSize()
            val explorationC = banditRepository.getExplorationC()
            val category = banditRepository.selectNextCategory()
            val anyUnexplored = armStatesBefore.values.any{ (_, n) -> n == 0}
            val armDetails = armStatesBefore.map { (cat, stats) ->
                val (mu, n) = stats
                val ucbBonus = if (n == 0) -1.0 else

            }
            val exercise = exerciseRepository
                .getExercisesByCategory(category)
                .randomOrNull()
            if (exercise != null) {
                val withAnswers =
                    exerciseRepository.getExercisesWithReferenceAnswersById(exercise.promptId)
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


    fun updateUserAnswer(answer: String) {
        _uiState.update { it.copy(userAnswer = answer) }
    }


    fun submitAnswer() {
        viewModelScope.launch {
            val state       = _uiState.value
            val exercise    = state.currentExercise ?: return@launch
            val userAnswer  = state.userAnswer.trim()
            if (userAnswer.isBlank()) return@launch

            val isCorrect     = Random.nextBoolean()
            val matchedAnswer = exercise.answers.randomOrNull()

            val record = ExerciseRecord(
                promptId = exercise.exercise.promptId,
                userId = userId.toIntOrNull() ?: 1,
                referId = matchedAnswer?.referId ?: -1,
                userAnswer = userAnswer,
                isCorrect = isCorrect,
                grammar = null,
                semanticScore = null,
                feedback = null,
                timestamp = System.currentTimeMillis()
            )
            recordRepository.insertExerciseRecord(record)

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

            if (isCorrect) {
                userProfileRepository.incrementSentencesCompleted(userId)
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

    fun goToNextExercise() {
        viewModelScope.launch {
            val state    = _uiState.value
            val nextIndex = state.currentIndex + 1

            val newProgress = state.todayProgress + 1
            userProfileRepository.updateTodayProgress(newProgress, userId)

            if (nextIndex >= state.exerciseQueue.size) {
                userProfileRepository.incrementStudyDays(userId)

                _uiState.update {
                    it.copy(
                        todayProgress = newProgress,
                        feedbackState = null,
                        isSessionDone = true,
                        currentExercise = null
                    )
                }
            } else {
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

    fun restartSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val profile = userProfileRepository.getUserProfile(userId)
                    ?: UserProfile(userId = userId)
                fetchExerciseBatch(profile)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }
    fun toggleDebugPanel() {
        _uiState.update {
            it.copy(showDebugPanel = !it.showDebugPanel)
        }
    }
}

data class ExerciseUiState(

    val exerciseQueue: List<ExerciseWithReferenceAnswers> = emptyList(),
    val currentIndex: Int = 0,

    val currentExercise: ExerciseWithReferenceAnswers? = null,

    val userAnswer: String = "",

    val totalAnswered: Int = 0,
    val correctCount: Int = 0,

    val dailyGoal: Int = 10,
    val todayProgress: Int = 0,

    val isSessionDone: Boolean = false,

    val feedbackState: FeedbackState? = null,

    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectionSteps: List<SelectionStepLog> = emptyList(),
    val showDebugPanel: Boolean = false
)

data class FeedbackState(
    val isCorrect: Boolean,
    val matchedReferenceAnswer: ReferenceAnswer? = null,
    val feedback: String?,
    val semanticScore: Double?,
    val grammar: String?
)