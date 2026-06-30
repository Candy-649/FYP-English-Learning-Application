package com.example.everydayenglish.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.everydayenglish.data.entity.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfile)

    @Update
    suspend fun updateUserProfile(userProfile: UserProfile)

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getUserProfile(userId: String): UserProfile?

    @Query("UPDATE user_profiles SET todayProgress = :progress, lastStudiedDate = :date, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateTodayProgress(progress: Int, date: Long, userId: String, updatedAt: Long)

    @Query("UPDATE user_profiles SET todayCorrectCount = :count, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateTodayCorrectCount(count: Int, userId: String, updatedAt: Long)

    @Query("UPDATE user_profiles SET currentStreak = :streak, lastStudiedDate = :date, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateStreak(streak: Int, date: Long, userId: String, updatedAt: Long)

    @Query("UPDATE user_profiles SET totalSentencesCompleted = totalSentencesCompleted + 1, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun incrementSentencesCompleted(userId: String, updatedAt: Long)

    @Query("UPDATE user_profiles SET totalStudyDays = totalStudyDays + 1, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun incrementStudyDays(userId: String, updatedAt: Long)
    @Query("UPDATE user_profiles SET recentSentenceCount = :count, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateSentenceCount(count: Int, userId: String, updatedAt: Long)
    @Query("UPDATE user_profiles SET dailyGoal = :goal, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateDailyGoal(goal: Int, userId: String, updatedAt: Long)
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun observeUserProfile(userId: String): Flow<UserProfile?>
}
