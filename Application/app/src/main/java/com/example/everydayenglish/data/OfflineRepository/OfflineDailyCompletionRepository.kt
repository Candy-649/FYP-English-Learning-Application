package com.example.everydayenglish.data.OfflineRepository

import com.example.everydayenglish.data.Repository.DailyCompletionRepository
import com.example.everydayenglish.data.dao.DailyCompletionDao
import com.example.everydayenglish.data.entity.DailyCompletion

class OfflineDailyCompletionRepository(
    private val dao: DailyCompletionDao
) : DailyCompletionRepository {
    override suspend fun insert(completion: DailyCompletion) = dao.insert(completion)
    override suspend fun getCompletedDays(userId: String) = dao.getCompletedDays(userId)
}