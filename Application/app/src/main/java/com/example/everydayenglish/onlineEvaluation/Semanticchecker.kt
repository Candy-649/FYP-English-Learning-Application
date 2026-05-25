package com.example.everydayenglish.onlineEvaluation

data class SemanticResult(
    val score: Double,      // 0.0 – 1.0，与参考答案的语义相似度
    val feedback: String,   // Haiku 生成的自然语言反馈，直接展示给用户
    val isCorrect: Boolean  // score >= CORRECT_THRESHOLD
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