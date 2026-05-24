package com.example.everydayenglish.data.Repository

import com.example.everydayenglish.adaptiveEngine.TenseCategory

interface BanditRepository {
    suspend fun selectNextCategory(): TenseCategory
    suspend fun updateFromAttempt(tense: String, accuracy: Double, timestamp: Long)
    suspend fun restoreFromAttempts()
    fun getStats(): Map<TenseCategory, Pair<Double, Int>>
    fun getTotalPulls(): Int
    fun getWindowSize(): Int
    fun getExplorationC(): Double
}