package com.example.everydayenglish.grammarChecker

data class GrammarResult(
    val issues: List<GrammarIssue>,
    val summary: String
) {
    val hasErrors: Boolean get() = issues.isNotEmpty()
}

data class GrammarIssue(
    val message: String,       // 错误描述，例如 "Possible agreement error"
    val shortMessage: String,  // 简短描述，例如 "Agreement"
    val offset: Int,           // 错误起始字符位置
    val length: Int,           // 错误长度
    val replacements: List<String> // 建议替换词
)

interface GrammarChecker {
    /**
     * 检查文本语法，挂起函数，在 IO 线程调用
     */
    suspend fun check(text: String): GrammarResult
}