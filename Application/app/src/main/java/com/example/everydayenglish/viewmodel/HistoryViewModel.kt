package com.example.everydayenglish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.ExerciseRepository
import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.entity.ExerciseRecord
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
}

data class HistoryUiState(
    val items           : List<HistoryItem> = emptyList(),
    val expandedPromptId: Int?              = null,
    val isLoading       : Boolean           = true,
    val errorMessage    : String?           = null
)

class HistoryViewModel(
    private val recordRepository  : RecordRepository,
    private val exerciseRepository: ExerciseRepository,
    private val appPreferencesRepository: AppPreferencesRepository
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

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
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