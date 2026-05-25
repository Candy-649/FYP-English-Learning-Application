package com.example.everydayenglish.data.OfflineRepository

import com.example.everydayenglish.data.Repository.AttemptRepository
import com.example.everydayenglish.data.dao.QuestionAttemptDao
import com.example.everydayenglish.data.entity.QuestionAttempt

class OfflineAttemptRepository(
    private val questionAttemptDao: QuestionAttemptDao
) : AttemptRepository {
    override suspend fun insert(attempt: QuestionAttempt) = questionAttemptDao.insert(attempt)
    override suspend fun getRecent(limit: Int): List<QuestionAttempt> = questionAttemptDao.getRecent(limit)
}