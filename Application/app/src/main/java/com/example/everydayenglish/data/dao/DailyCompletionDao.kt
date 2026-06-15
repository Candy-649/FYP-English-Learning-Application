package com.example.everydayenglish.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.everydayenglish.data.entity.DailyCompletion

@Dao
interface DailyCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: DailyCompletion)

    @Query("SELECT dateEpochDay FROM daily_completions WHERE userId = :userId AND completed = 1 ORDER BY dateEpochDay DESC")
    suspend fun getCompletedDays(userId: String): List<Int>
}