package com.example.everydayenglish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.adaptiveEngine.TenseCategory
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.AttemptRepository
import com.example.everydayenglish.data.Repository.BanditRepository
import com.example.everydayenglish.data.Repository.ExerciseRepository
import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.entity.ExerciseRecord
import com.example.everydayenglish.data.entity.ExerciseWithReferenceAnswers
import com.example.everydayenglish.data.entity.QuestionAttempt
import com.example.everydayenglish.data.entity.ReferenceAnswer
import com.example.everydayenglish.data.entity.UserProfile
import com.example.everydayenglish.grammarChecker.GrammarChecker
import com.example.everydayenglish.onlineEvaluation.FeedbackGenerator
import com.example.everydayenglish.onlineEvaluation.SemanticChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.sqrt

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
    private val appPreferencesRepository: AppPreferencesRepository,
    private val attemptRepository: AttemptRepository,
    private val grammarChecker: GrammarChecker,
    private val semanticChecker: SemanticChecker,
    private val feedbackGenerator: FeedbackGenerator
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
                fetchExerciseBatch(
                    dailyGoal     = profile?.dailyGoal ?: 10,
                    todayProgress = profile?.todayProgress ?: 0
                )

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }

    private suspend fun fetchExerciseBatch(dailyGoal: Int, todayProgress: Int) {
        val dailyGoal = dailyGoal.coerceAtLeast(1)
        val batch = mutableListOf<ExerciseWithReferenceAnswers>()
        val steps = mutableListOf<SelectionStepLog>()

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
                    explorationC * sqrt(ln(minOf(totalPulls, windowSize).toDouble()) / n)
                val ucbScore = if (n == 0) Double.MAX_VALUE else mu + ucbBonus
                ArmDebugInfo(
                    category = cat,
                    mu = mu,
                    n = n,
                    ucbBonus = ucbBonus,
                    ucbScore = ucbScore,
                    isSelected = cat == category
                )
            }
            steps.add(
                SelectionStepLog(
                    stepIndex = batch.size,
                    totalPulls = totalPulls,
                    selectedCategory = category,
                    wasUnexplored = anyUnexplored,
                    armDetails = armDetails
                )
            )
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
                todayProgress    = todayProgress,
                totalAnswered    = 0,
                correctCount     = 0,
                feedbackState    = null,
                userAnswer       = "",
                isSessionDone    = false,
                isLoading        = false,
                errorMessage     = null,
                currentExercise  = batch.first(),
                selectionSteps = steps
            )
        }
        refreshArmStats()
    }


    fun updateUserAnswer(answer: String) {
        _uiState.update { it.copy(userAnswer = answer) }
    }


    fun submitAnswer() {
        viewModelScope.launch {
            val state      = _uiState.value
            val exercise   = state.currentExercise ?: return@launch
            val userAnswer = state.userAnswer.trim()
            if (userAnswer.isBlank()) return@launch

            val newTries      = state.currentTries + 1
            //Grammar is the fastest one
            val grammarResult = grammarChecker.check(userAnswer)
            val matchedAnswer = exercise.answers.firstOrNull()
            val referenceTexts = exercise.answers.map { it.reference }

            //Start Semantic
            val recordId = recordRepository.insertExerciseRecord(
                ExerciseRecord(
                    promptId          = exercise.exercise.promptId,
                    userId            = userId,
                    referId           = matchedAnswer?.referId ?: -1,
                    userAnswer        = userAnswer,
                    isCorrect         = false,
                    grammar           = grammarResult.summary,
                    evaluationPending = true,
                )
            )

            _uiState.update {
                it.copy(
                    currentTries  = newTries,
                    userAnswer    = "",
                    feedbackState = FeedbackState(
                        isCorrect              = null,
                        matchedReferenceAnswer = matchedAnswer,
                        feedback               = null,
                        semanticScore          = null,
                        grammar                = grammarResult.summary,
                        isEvaluating           = true
                    )
                )
            }
            //HuggingFace Semantic
            val semanticResult = try {
                semanticChecker.evaluate(userAnswer, referenceTexts)
            } catch (e: Exception){
                android.util.Log.e("HF_DEBUG", "Semantic check failed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        feedbackState = it.feedbackState?.copy(
                            isEvaluating = false,
                            evaluationOffline = true
                        )
                    )
                }
                return@launch
            }
            //Deepseek Feedback
            val feedbackText = try {
                feedbackGenerator.generate(
                    userAnswer = userAnswer,
                    referenceAnswers = referenceTexts,
                    grammarSummary = grammarResult.summary,
                    semanticScore = semanticResult.score
                )
            } catch (e: Exception){
                null
            }
            recordRepository.updateEvaluation(
                recordId = recordId.toInt(),
                score = semanticResult.score,
                feedback = feedbackText ?: "",
                isCorrect = semanticResult.isCorrect
            )

            _uiState.update {
                it.copy(
                    feedbackState = it.feedbackState?.copy(
                        isCorrect = semanticResult.isCorrect,
                        semanticScore = semanticResult.score,
                        feedback = feedbackText,
                        isFeedbackLoading = false,
                        isEvaluating = false
                    )
                )
            }
        }
    }

    // 用户答对了，或者主动放弃，调这个
    fun finishCurrentQuestion(gaveUp: Boolean = false) {
        viewModelScope.launch {
            val state    = _uiState.value
            val feedback = state.feedbackState ?: return@launch
            val exercise = state.currentExercise ?: return@launch
            val tense    = feedback.matchedReferenceAnswer?.tense ?: "Unknown"
            val now      = System.currentTimeMillis()

            // 只有已拿到评估结果才更新 Bandit / sentencesCompleted
            if (!feedback.evaluationOffline && feedback.isCorrect != null) {
                val solved   = !gaveUp && feedback.isCorrect
                val accuracy = if (solved) 1.0 / state.currentTries else 0.0
                attemptRepository.insert(
                    QuestionAttempt(
                        promptId   = exercise.exercise.promptId,
                        userId     = userId,
                        tense      = tense,
                        totalTries = state.currentTries,
                        solved     = solved,
                        accuracy   = accuracy
                    )
                )
                banditRepository.updateFromAttempt(tense, accuracy, now)
                if (solved) userProfileRepository.incrementSentencesCompleted(userId)
            }
            // evaluationOffline / isEvaluating 中用户跳走：Bandit 跳过，进度照常推进

            val solved      = !gaveUp && feedback.isCorrect == true && !feedback.evaluationOffline
            val newProgress = if (solved) state.todayProgress + 1 else state.todayProgress
            val newAnswered = state.totalAnswered + 1
            val newCorrect  = if (solved) state.correctCount + 1 else state.correctCount

            userProfileRepository.updateTodayProgress(newProgress, System.currentTimeMillis(), userId)

            if (newAnswered >= state.dailyGoal) {
                userProfileRepository.incrementStudyDays(userId)
                _uiState.update {
                    it.copy(
                        todayProgress   = newProgress,
                        totalAnswered   = newAnswered,
                        correctCount    = newCorrect,
                        feedbackState   = null,
                        isSessionDone   = true,
                        currentExercise = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        todayProgress = newProgress,
                        totalAnswered = newAnswered,
                        correctCount  = newCorrect,
                        currentIndex  = state.currentIndex + 1,
                        currentTries  = 0,
                        feedbackState = null
                    )
                }
                loadNextExercise()
            }

            refreshArmStats()
        }
    }

    fun loadNextExercise() {
        viewModelScope.launch {
            val state    = _uiState.value
            val nextIndex = state.currentIndex + 1


            if (nextIndex >= state.exerciseQueue.size) {
                userProfileRepository.incrementStudyDays(userId)

                _uiState.update {
                    it.copy(
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
                fetchExerciseBatch(
                    dailyGoal     = profile?.dailyGoal ?: 10,
                    todayProgress = profile?.todayProgress ?: 0
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }
    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackState = null) }
    }
    fun toggleDebugPanel() {
        _uiState.update {
            it.copy(showDebugPanel = !it.showDebugPanel)
        }
    }
    private fun refreshArmStats() {
        val stats = banditRepository.getStats()
        val totalPulls = banditRepository.getTotalPulls()
        val windowSize = banditRepository.getWindowSize()
        val explorationC = banditRepository.getExplorationC()

        val armDetails = stats.map { (cat, statsPair) ->
            val (mu, n) = statsPair
            val ucbBonus = if (n == 0) -1.0 else
                explorationC * sqrt(
                    ln(minOf(totalPulls, windowSize).toDouble()) / n
                )
            val ucbScore = if (n == 0) Double.MAX_VALUE else mu + ucbBonus
            ArmDebugInfo(
                category = cat,
                mu = mu,
                n = n,
                ucbBonus = ucbBonus,
                ucbScore = ucbScore,
                isSelected = false
            )
        }.sortedByDescending { it.ucbScore }

        _uiState.update { it.copy(currentArmStats = armDetails) }
    }
}

data class ExerciseUiState(

    val exerciseQueue: List<ExerciseWithReferenceAnswers> = emptyList(),
    val currentIndex: Int = 0,

    val currentExercise: ExerciseWithReferenceAnswers? = null,

    val userAnswer: String = "",
    val currentTries: Int = 0,
    val totalAnswered: Int = 0,
    val correctCount: Int = 0,

    val dailyGoal: Int = 10,
    val todayProgress: Int = 0,

    val isSessionDone: Boolean = false,

    val feedbackState: FeedbackState? = null,

    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectionSteps: List<SelectionStepLog> = emptyList(),
    val showDebugPanel: Boolean = false,
    val currentArmStats: List<ArmDebugInfo> = emptyList()
)

data class FeedbackState(
    val isCorrect              : Boolean?,
    val matchedReferenceAnswer : ReferenceAnswer? = null,
    val feedback               : String?,
    val semanticScore          : Double?,
    val grammar                : String?,
    val isEvaluating           : Boolean = false,
    val evaluationOffline      : Boolean = false,
    val isFeedbackLoading : Boolean = false
)