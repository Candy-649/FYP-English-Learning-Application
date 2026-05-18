package com.example.everydayenglish.data.Repository

import com.example.everydayenglish.adaptiveEngine.TenseCategory

interface BanditRepository {
    suspend fun selectNextCategory(): TenseCategory
    suspend fun update(category: TenseCategory, isCorrect: Boolean, timestamp: Long)
    suspend fun restoreFromRecords()
    fun getStats(): Map<TenseCategory, Pair<Double, Int>>
    fun getTotalPulls(): Int
    fun getWindowSize(): Int
    fun getExplorationC(): Double
}