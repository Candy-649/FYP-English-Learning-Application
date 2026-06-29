package com.example.everydayenglish.data.OfflineRepository

import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.dao.UserProfileDao
import com.example.everydayenglish.data.entity.UserProfile
import com.example.everydayenglish.util.isNewDay
import kotlinx.coroutines.flow.Flow

class OfflineUserProfileRepository(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {

    override suspend fun insertUserProfile(userProfile: UserProfile) =
        userProfileDao.insertUserProfile(userProfile)

    override suspend fun updateUserProfile(userProfile: UserProfile) =
        userProfileDao.updateUserProfile(userProfile)

    override suspend fun getUserProfile(userId: String): UserProfile? =
        userProfileDao.getUserProfile(userId)

    override suspend fun getUserProfileForToday(userId: String): UserProfile? {
        val profile = userProfileDao.getUserProfile(userId) ?: return null
        if (!isNewDay(profile.lastStudiedDate)) return profile
        if (profile.todayProgress == 0 && profile.todayCorrectCount == 0) return profile

        val now = System.currentTimeMillis()
        userProfileDao.updateTodayProgress(0, now, userId)
        userProfileDao.updateTodayCorrectCount(0, userId)
        return profile.copy(todayProgress = 0, todayCorrectCount = 0, lastStudiedDate = now)
    }

    override suspend fun updateTodayProgress(progress: Int, date: Long, userId: String) =
        userProfileDao.updateTodayProgress(progress, date, userId)

    override suspend fun updateTodayCorrectCount(count: Int, userId: String) =
        userProfileDao.updateTodayCorrectCount(count, userId)

    override suspend fun updateStreak(streak: Int, date: Long, userId: String) =
        userProfileDao.updateStreak(streak, date, userId)

    override suspend fun incrementSentencesCompleted(userId: String) =
        userProfileDao.incrementSentencesCompleted(userId)

    override suspend fun incrementStudyDays(userId: String) =
        userProfileDao.incrementStudyDays(userId)
    override suspend fun updateSentenceCount(count: Int, userId: String) =
        userProfileDao.updateSentenceCount(count, userId)
    override suspend fun updateDailyGoal(goal: Int, userId: String) =
        userProfileDao.updateDailyGoal(goal, userId)
    override fun observeUserProfile(userId: String): Flow<UserProfile?> =
        userProfileDao.observeUserProfile(userId)
}