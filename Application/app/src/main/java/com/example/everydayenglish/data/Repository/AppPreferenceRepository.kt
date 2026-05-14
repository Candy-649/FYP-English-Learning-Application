package com.example.everydayenglish.data.Repository

interface AppPreferencesRepository {
    fun getUserId(): String?
    fun saveUserId(userId: String)
    fun isFirstLaunch(): Boolean
    fun setFirstLaunchDone()
    suspend fun saveDailyGoal(goal: Int)

    suspend fun getDailyGoal(): Int

    suspend fun saveDarkMode(enabled: Boolean)

    suspend fun getDarkMode(): Boolean
}