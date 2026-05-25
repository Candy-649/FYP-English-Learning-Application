package com.example.everydayenglish.data.Repository

import com.example.everydayenglish.data.entity.QuestionAttempt

interface AttemptRepository {
    suspend fun insert(attempt: QuestionAttempt)
    suspend fun getRecent(limit: Int): List<QuestionAttempt>
}