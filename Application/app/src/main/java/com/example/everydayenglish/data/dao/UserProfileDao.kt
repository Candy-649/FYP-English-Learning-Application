package com.example.everydayenglish.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.everydayenglish.data.entity.UserProfile

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfile)

    @Update
    suspend fun updateUserProfile(userProfile: UserProfile)

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getUserProfile(userId: String): UserProfile?

    @Query("UPDATE user_profiles SET todayProgress = :progress WHERE userId = :userId")
    suspend fun updateTodayProgress(progress: Int, userId: String)

    @Query("UPDATE user_profiles SET currentStreak = :streak, lastStudiedDate = :date WHERE userId = :userId")
    suspend fun updateStreak(streak: Int, date: Long, userId: String)

    @Query("UPDATE user_profiles SET totalSentencesCompleted = totalSentencesCompleted + 1 WHERE userId = :userId")
    suspend fun incrementSentencesCompleted(userId: String)

    @Query("UPDATE user_profiles SET totalStudyDays = totalStudyDays + 1 WHERE userId = :userId")
    suspend fun incrementStudyDays(userId: String)
}