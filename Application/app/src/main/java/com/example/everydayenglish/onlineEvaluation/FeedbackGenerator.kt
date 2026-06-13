package com.example.everydayenglish.onlineEvaluation

interface FeedbackGenerator {
    suspend fun generate(
        userAnswer       : String,
        referenceAnswers : List<String>,
        grammarSummary   : String?,
        semanticScore    : Double
    ): String
}