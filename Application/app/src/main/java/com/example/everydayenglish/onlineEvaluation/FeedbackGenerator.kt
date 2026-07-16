package com.example.everydayenglish.onlineEvaluation

import com.example.everydayenglish.data.entity.EvaluationResult

/**
 * One past turn within the same question's session: the raw inputs used to build that
 * turn's user message, plus the JSON the model returned for it (same format as
 * EvaluationResult.toStorageString()). Passing these back in lets DeepSeek see what it
 * already said earlier in this session, so retries on the same question don't produce
 * contradictory feedback.
 */
data class ConversationTurn(
    val userAnswer      : String,
    val referenceAnswers: List<String>,
    val grammarSummary  : String?,
    val semanticScore   : Double?,
    val tenseCategory   : String?,
    val assistantJson   : String
)

interface FeedbackGenerator {
    suspend fun generate(
        userAnswer          : String,
        referenceAnswers    : List<String>,
        grammarSummary      : String?,
        semanticScore       : Double?,
        tenseCategory       : String? = null,
        conversationHistory : List<ConversationTurn> = emptyList()
    ): EvaluationResult
}