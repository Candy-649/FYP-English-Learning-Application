package com.example.everydayenglish.onlineEvaluation

data class SemanticResult(
    val score: Double,
    val feedback: String,
    val isCorrect: Boolean,
    val errorType: String = "none"
) {
    companion object {
        const val CORRECT_THRESHOLD = 0.75
    }
}

interface SemanticChecker {

    suspend fun evaluate(
        userAnswer: String,
        referenceAnswers: List<String>
    ): SemanticResult
}