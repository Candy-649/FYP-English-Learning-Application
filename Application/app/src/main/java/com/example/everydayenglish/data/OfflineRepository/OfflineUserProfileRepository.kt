package com.example.everydayenglish.data.OfflineRepository

import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.dao.UserProfileDao
import com.example.everydayenglish.data.entity.UserProfile

class OfflineUserProfileRepository(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {

    override suspend fun insertUserProfile(userProfile: UserProfile) =
        userProfileDao.insertUserProfile(userProfile)

    override suspend fun updateUserProfile(userProfile: UserProfile) =
        userProfileDao.updateUserProfile(userProfile)

    override suspend fun getUserProfile(userId: String): UserProfile? =
        userProfileDao.getUserProfile(userId)

    override suspend fun updateTodayProgress(progress: Int, userId: String) =
        userProfileDao.updateTodayProgress(progress, userId)

    override suspend fun updateStreak(streak: Int, date: Long, userId: String) =
        userProfileDao.updateStreak(streak, date, userId)

    override suspend fun incrementSentencesCompleted(userId: String) =
        userProfileDao.incrementSentencesCompleted(userId)

    override suspend fun incrementStudyDays(userId: String) =
        userProfileDao.incrementStudyDays(userId)
}