package com.example.everydayenglish.data.Repository

import com.example.everydayenglish.data.entity.DailyCompletion

interface DailyCompletionRepository {
    suspend fun insert(completion: DailyCompletion)
    suspend fun getCompletedDays(userId: String): List<Int>
}