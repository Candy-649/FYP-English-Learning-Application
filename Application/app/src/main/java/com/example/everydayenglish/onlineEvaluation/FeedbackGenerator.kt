package com.example.everydayenglish.onlineEvaluation

import com.example.everydayenglish.data.entity.EvaluationResult

interface FeedbackGenerator {
    suspend fun generate(
        userAnswer: String,
        referenceAnswers: List<String>,
        grammarSummary: String?,
        semanticScore: Double?
    ): EvaluationResult
}