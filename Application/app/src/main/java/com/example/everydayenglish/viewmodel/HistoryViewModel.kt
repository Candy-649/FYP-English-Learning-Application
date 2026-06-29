package com.example.everydayenglish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.ExerciseRepository
import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.entity.ExerciseRecord
import com.example.everydayenglish.domain.CorrectAnswerRewardApplier
import com.example.everydayenglish.grammarChecker.GrammarChecker
import com.example.everydayenglish.onlineEvaluation.FeedbackGenerator
import com.example.everydayenglish.onlineEvaluation.SemanticChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryItem(
    val promptId        : Int,
    val prompt          : String,
    val tenses          : List<String>,       // distinct，来自 ReferenceAnswer
    val records         : List<ExerciseRecord>, // 按时间升序，供展开后展示
    val latestTimestamp : Long,               // 用于列表排序
    val totalAttempts   : Int,
    val correctCount    : Int,                // isCorrect == true 的条数
    val evaluatedCount  : Int,                // !evaluationPending 的条数
) {
    /** 有评估结果时才展示正确率，全部 pending 则 null */
    val accuracyRate: Double?
        get() = if (evaluatedCount > 0) correctCount.toDouble() / evaluatedCount else null

    /** 一次都没做对过 = 放弃 / 卡住的题，History 里可以重做 */
    val canRedo: Boolean
        get() = accuracyRate == 0.0
}

/** 重做一题时，提交答案后的评估反馈状态。结构跟 ExerciseViewModel.FeedbackState 类似，但只服务于单题重做。 */
data class RedoFeedbackState(
    val isEvaluating: Boolean = true,
    val isCorrect: Boolean? = null,
    val feedback: String? = null,
    val grammar: String? = null,
    val evaluationOffline: Boolean = false
)

data class HistoryUiState(
    val items           : List<HistoryItem> = emptyList(),
    val expandedPromptId: Int?              = null,
    val isLoading       : Boolean           = true,
    val errorMessage    : String?           = null,

    // ── 重做放弃的题 ──────────────────────────────────────
    val redoPromptId    : Int?              = null,  // 当前打开重做面板的题，null = 没有
    val redoAnswer      : String            = "",
    val redoFeedback    : RedoFeedbackState? = null
)

class HistoryViewModel(
    private val recordRepository  : RecordRepository,
    private val exerciseRepository: ExerciseRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val grammarChecker: GrammarChecker,
    private val semanticChecker: SemanticChecker,
    private val feedbackGenerator: FeedbackGenerator,
    private val correctAnswerRewardApplier: CorrectAnswerRewardApplier
) : ViewModel() {
    private val userId: String
        get() = appPreferencesRepository.getUserId()

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun refresh() { load() }

    fun toggleExpand(promptId: Int) {
        _uiState.update { state ->
            state.copy(
                expandedPromptId = if (state.expandedPromptId == promptId) null else promptId
            )
        }
    }

    // ── 重做放弃的题 ──────────────────────────────────────

    fun openRedo(promptId: Int) {
        _uiState.update {
            it.copy(
                expandedPromptId = promptId,
                redoPromptId     = promptId,
                redoAnswer       = "",
                redoFeedback     = null
            )
        }
    }

    fun closeRedo() {
        _uiState.update {
            it.copy(redoPromptId = null, redoAnswer = "", redoFeedback = null)
        }
    }

    fun updateRedoAnswer(answer: String) {
        _uiState.update { it.copy(redoAnswer = answer) }
    }

    /** 重做反馈是错的时候，清空重新输入；不算给放弃记录 */
    fun retryRedo() {
        _uiState.update { it.copy(redoAnswer = "", redoFeedback = null) }
    }

    fun submitRedo() {
        viewModelScope.launch {
            val promptId   = _uiState.value.redoPromptId ?: return@launch
            val userAnswer = _uiState.value.redoAnswer.trim()
            if (userAnswer.isBlank()) return@launch

            _uiState.update { it.copy(redoFeedback = RedoFeedbackState(isEvaluating = true)) }

            val exercise        = exerciseRepository.getExercisesWithReferenceAnswersById(promptId)
            val matchedAnswer   = exercise.answers.firstOrNull()
            val referenceTexts  = exercise.answers.map { it.reference }
            val tense           = matchedAnswer?.tense ?: "Unknown"

            val grammarResult = grammarChecker.check(userAnswer)

            // 跟 submitAnswer 一样：先插入一条 pending 记录，评估完再回填
            val recordId = recordRepository.insertExerciseRecord(
                ExerciseRecord(
                    promptId          = promptId,
                    userId            = userId,
                    referId           = matchedAnswer?.referId ?: -1,
                    userAnswer        = userAnswer,
                    isCorrect         = false,
                    grammar           = grammarResult.summary,
                    evaluationPending = true
                )
            )

            val semanticResult = try {
                semanticChecker.evaluate(userAnswer, referenceTexts)
            } catch (e: Exception) {
                null
            }
            val evalResult = try {
                feedbackGenerator.generate(
                    userAnswer       = userAnswer,
                    referenceAnswers = referenceTexts,
                    grammarSummary   = grammarResult.summary,
                    semanticScore    = semanticResult?.score
                )
            } catch (e: Exception) {
                null
            }

            if (evalResult == null && semanticResult == null) {
                // 跟正常做题一样：评估失败就留 pending，等以后联网重试，这次重做不发奖励
                _uiState.update {
                    it.copy(redoFeedback = RedoFeedbackState(
                        isEvaluating      = false,
                        evaluationOffline = true
                    ))
                }
                return@launch
            }

            val isCorrect = evalResult?.isCorrect ?: semanticResult?.isCorrect ?: false

            recordRepository.updateEvaluation(
                recordId  = recordId.toInt(),
                score     = semanticResult?.score ?: 0.0,
                feedback  = evalResult?.feedback ?: "",
                isCorrect = isCorrect
            )

            _uiState.update {
                it.copy(redoFeedback = RedoFeedbackState(
                    isEvaluating = false,
                    isCorrect    = isCorrect,
                    feedback     = evalResult?.feedback,
                    grammar      = grammarResult.summary
                ))
            }

            // 答对了：跟正常做对一题完全一样的奖励（老虎机、todayProgress、todayCorrectCount……）
            if (isCorrect) {
                correctAnswerRewardApplier.apply(
                    promptId   = promptId,
                    userId     = userId,
                    tense      = tense,
                    totalTries = 1
                )
            }

            // 静默刷新列表（不带全屏 loading），让新记录 / 正确率立刻显示出来，
            // 同时不打断用户正在看的重做反馈面板
            load(showLoading = false)
        }
    }

    private fun load(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            try {
                val allRecords = recordRepository.getAllByUser(userId)

                // 按 promptId 分组，保持首次出现顺序（= 最新 record 在前）
                val grouped = allRecords.groupBy { it.promptId }

                val items = grouped.map { (promptId, records) ->
                    val exerciseWithAnswers = exerciseRepository
                        .getExercisesWithReferenceAnswersById(promptId)

                    val tenses = exerciseWithAnswers.answers
                        .mapNotNull { it.tense }
                        .distinct()

                    val evaluatedCount = records.count { !it.evaluationPending }
                    val correctCount   = records.count { it.isCorrect }

                    HistoryItem(
                        promptId        = promptId,
                        prompt          = exerciseWithAnswers.exercise.prompt,
                        tenses          = tenses,
                        records         = records.sortedBy { it.timestamp }, // 展开后时间升序
                        latestTimestamp = records.first().timestamp,          // 已按 DESC 排，first() 最新
                        totalAttempts   = records.size,
                        correctCount    = correctCount,
                        evaluatedCount  = evaluatedCount
                    )
                }
                // 最新有活动的题在最上面（groupBy 保留插入顺序，records 已按 DESC 排）
                _uiState.update { it.copy(items = items, isLoading = false) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load history")
                }
            }
        }
    }
}
