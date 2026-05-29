package com.example.everydayenglish.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.everydayenglish.data.entity.QuestionAttempt

@Dao
interface QuestionAttemptDao {
    @Insert
    suspend fun insert(attempt: QuestionAttempt)

    @Query("SELECT * FROM question_attempts ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<QuestionAttempt>

    @Query("SELECT * FROM question_attempts WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllByUser(userId: String): List<QuestionAttempt>
}