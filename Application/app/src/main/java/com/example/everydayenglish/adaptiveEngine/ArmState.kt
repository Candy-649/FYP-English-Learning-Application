package com.example.everydayenglish.adaptiveEngine

data class ArmState(
    val category: TenseCategory,
    val window: ArrayDeque<Pair<Long, Double>> = ArrayDeque()  // Boolean → Double
) {
    fun averageAccuracy(): Double {
        if (window.isEmpty()) return 0.0
        return window.sumOf { it.second } / window.size  // 平均正确率
    }

    fun count(): Int = window.size
}
