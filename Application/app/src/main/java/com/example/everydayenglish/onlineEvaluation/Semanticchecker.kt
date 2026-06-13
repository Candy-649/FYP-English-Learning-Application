package com.example.everydayenglish.onlineEvaluation

data class SemanticResult(
    val score: Double,       // 0.0 – 1.0，余弦相似度
    val isCorrect: Boolean   // score >= CORRECT_THRESHOLD
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