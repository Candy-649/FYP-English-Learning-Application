package com.example.everydayenglish.data.Repository

import com.example.everydayenglish.data.entity.UserProfile

interface UserProfileRepository {
    suspend fun insertUserProfile(userProfile: UserProfile)
    suspend fun updateUserProfile(userProfile: UserProfile)
    suspend fun getUserProfile(userId: String): UserProfile?
    suspend fun updateTodayProgress(progress: Int, userId: String)
    suspend fun updateStreak(streak: Int, date: Long, userId: String)
    suspend fun incrementSentencesCompleted(userId: String)
    suspend fun incrementStudyDays(userId: String)
    suspend fun updateSentenceCount(count: Int, userId: String)
}