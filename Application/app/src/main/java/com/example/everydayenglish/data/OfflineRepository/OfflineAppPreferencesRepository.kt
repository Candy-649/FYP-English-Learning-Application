package com.example.everydayenglish.data.OfflineRepository

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.everydayenglish.data.dataStore.PreferencesKeys
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.AuthRepository
import kotlinx.coroutines.flow.first
import java.io.File


class OfflineAppPreferencesRepository(
    private val prefs: SharedPreferences,
    private val dataStore: DataStore<Preferences>,
    private val cacheDir: File,
    private val authRepository: AuthRepository
) : AppPreferencesRepository {

    override fun getUserId(): String =
        authRepository.currentUserId
            ?: prefs.getString("user_id", "") ?: ""

    override fun saveUserId(userId: String) =
        prefs.edit().putString("user_id", userId).apply()

    override fun isFirstLaunch(): Boolean =
        prefs.getBoolean("is_first_launch", true)

    override fun setFirstLaunchDone() =
        prefs.edit().putBoolean("is_first_launch", false).apply()
    override suspend fun saveDailyGoal(goal: Int) {

        dataStore.edit {

            it[PreferencesKeys.DAILY_GOAL] =
                goal
        }
    }

    override suspend fun getDailyGoal(): Int {

        return dataStore.data.first()[
            PreferencesKeys.DAILY_GOAL
        ] ?: 10
    }

    override suspend fun saveDarkMode(
        enabled: Boolean
    ) {

        dataStore.edit {

            it[PreferencesKeys.DARK_MODE] =
                enabled
        }
    }

    override suspend fun getDarkMode(): Boolean {

        return dataStore.data.first()[
            PreferencesKeys.DARK_MODE
        ] ?: false
    }
    // OfflineAppPreferencesRepository 实现：
    override suspend fun getNotification(): Boolean =
        dataStore.data.first()[PreferencesKeys.NOTIFICATION] ?: true

    override suspend fun saveNotification(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.NOTIFICATION] = enabled }
    }

    fun getCacheSizeText(): String {
        val bytes = cacheDir.walkTopDown().sumOf { it.length() }
        return when {
            bytes < 1024 -> "${bytes} B"
            bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }
    override suspend fun saveDarkModeOption(option: String) {
        dataStore.edit { it[PreferencesKeys.DARK_MODE_OPTION] = option }
    }

    override suspend fun getDarkModeOption(): String =
        dataStore.data.first()[PreferencesKeys.DARK_MODE_OPTION] ?: "AUTO"
    fun clearCache(): Boolean {
        return try {
            cacheDir.walkTopDown()
                .filter { it.isFile }
                .forEach { it.delete() }
            true
        } catch (e: Exception) {
            false
        }
    }
}