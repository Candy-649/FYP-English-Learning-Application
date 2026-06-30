package com.example.everydayenglish.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.everydayenglish.data.entity.QuestionAttempt

@Dao
interface QuestionAttemptDao {
    @Insert
    suspend fun insert(attempt: QuestionAttempt)

    // 给同步合并用：云端版本覆盖本地同 id 的行。正常做题流程用上面的 insert()，不用这个。
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attempt: QuestionAttempt)

    @Query("SELECT * FROM question_attempts ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<QuestionAttempt>

    @Query("SELECT * FROM question_attempts WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllByUser(userId: String): List<QuestionAttempt>
    @Query("""
    SELECT COUNT(*) FROM question_attempts 
    WHERE userId = :userId AND solved = 1 AND timestamp >= :todayStart
""")
    suspend fun getTodaySolvedCount(userId: String, todayStart: Long): Int
}
