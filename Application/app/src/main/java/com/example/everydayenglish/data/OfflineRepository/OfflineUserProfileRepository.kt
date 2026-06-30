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

    // updateUserProfile 来源有两种：用户自己改资料（要盖新时间戳），
    // 或者同步引擎从云端整条覆盖本地（要保留云端原来的 updatedAt，不能盖成"现在"）。
    // 这里用 insertUserProfile 而不是 updateUserProfile 走合并的话，时间戳天然就不会被动到。
    override suspend fun updateUserProfile(userProfile: UserProfile) =
        userProfileDao.updateUserProfile(userProfile.copy(updatedAt = System.currentTimeMillis()))

    override suspend fun getUserProfile(userId: String): UserProfile? =
        userProfileDao.getUserProfile(userId)

    override suspend fun getUserProfileForToday(userId: String): UserProfile? {
        val profile = userProfileDao.getUserProfile(userId) ?: return null
        if (!isNewDay(profile.lastStudiedDate)) return profile
        if (profile.todayProgress == 0 && profile.todayCorrectCount == 0) return profile

        val now = System.currentTimeMillis()
        userProfileDao.updateTodayProgress(0, now, userId, now)
        userProfileDao.updateTodayCorrectCount(0, userId, now)
        return profile.copy(todayProgress = 0, todayCorrectCount = 0, lastStudiedDate = now, updatedAt = now)
    }

    override suspend fun updateTodayProgress(progress: Int, date: Long, userId: String) =
        userProfileDao.updateTodayProgress(progress, date, userId, System.currentTimeMillis())

    override suspend fun updateTodayCorrectCount(count: Int, userId: String) =
        userProfileDao.updateTodayCorrectCount(count, userId, System.currentTimeMillis())

    override suspend fun updateStreak(streak: Int, date: Long, userId: String) =
        userProfileDao.updateStreak(streak, date, userId, System.currentTimeMillis())

    override suspend fun incrementSentencesCompleted(userId: String) =
        userProfileDao.incrementSentencesCompleted(userId, System.currentTimeMillis())

    override suspend fun incrementStudyDays(userId: String) =
        userProfileDao.incrementStudyDays(userId, System.currentTimeMillis())
    override suspend fun updateSentenceCount(count: Int, userId: String) =
        userProfileDao.updateSentenceCount(count, userId, System.currentTimeMillis())
    override suspend fun updateDailyGoal(goal: Int, userId: String) =
        userProfileDao.updateDailyGoal(goal, userId, System.currentTimeMillis())
    override fun observeUserProfile(userId: String): Flow<UserProfile?> =
        userProfileDao.observeUserProfile(userId)
}
