package com.example.everydayenglish.data.SyncingRepository

import com.example.everydayenglish.data.Repository.AuthRepository
import com.example.everydayenglish.data.Repository.SyncRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.entity.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SyncingUserProfileRepository(
    private val offline: UserProfileRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository
) : UserProfileRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun pushIfLoggedIn(userId: String) {
        if (authRepository.currentUserId != userId) return
        syncScope.launch {
            offline.getUserProfile(userId)?.let { syncRepository.pushProfile(it) }
        }
    }

    override suspend fun insertUserProfile(userProfile: UserProfile) {
        offline.insertUserProfile(userProfile)
        pushIfLoggedIn(userProfile.userId)
    }

    override suspend fun updateUserProfile(userProfile: UserProfile) {
        offline.updateUserProfile(userProfile)
        pushIfLoggedIn(userProfile.userId)
    }

    override suspend fun getUserProfile(userId: String): UserProfile? =
        offline.getUserProfile(userId)

    override suspend fun getUserProfileForToday(userId: String): UserProfile? =
        offline.getUserProfileForToday(userId)

    override suspend fun updateTodayProgress(progress: Int, date: Long, userId: String) {
        offline.updateTodayProgress(progress, date, userId)
        pushIfLoggedIn(userId)
    }

    override suspend fun updateTodayCorrectCount(count: Int, userId: String) {
        offline.updateTodayCorrectCount(count, userId)
        pushIfLoggedIn(userId)
    }

    override suspend fun updateStreak(streak: Int, date: Long, userId: String) {
        offline.updateStreak(streak, date, userId)
        pushIfLoggedIn(userId)
    }

    override suspend fun incrementSentencesCompleted(userId: String) {
        offline.incrementSentencesCompleted(userId)
        pushIfLoggedIn(userId)
    }

    override suspend fun incrementStudyDays(userId: String) {
        offline.incrementStudyDays(userId)
        pushIfLoggedIn(userId)
    }

    override suspend fun updateSentenceCount(count: Int, userId: String) {
        offline.updateSentenceCount(count, userId)
        pushIfLoggedIn(userId)
    }

    override suspend fun updateDailyGoal(goal: Int, userId: String) {
        offline.updateDailyGoal(goal, userId)
        pushIfLoggedIn(userId)
    }

    override fun observeUserProfile(userId: String): Flow<UserProfile?> =
        offline.observeUserProfile(userId)
}
