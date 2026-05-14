package com.example.everydayenglish.adaptiveEngine

data class ArmState(
    val category: TenseCategory,
    val window: ArrayDeque<Pair<Long, Boolean>> = ArrayDeque()
){
    fun successRate(): Double {
        if (window.isEmpty()) return 0.0
        return window.count { it.second } / window.size.toDouble()
    }

    fun count(): Int = window.size
}
