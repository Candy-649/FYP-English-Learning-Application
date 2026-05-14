package com.example.everydayenglish.data.OfflineRepository

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.everydayenglish.data.dataStore.PreferencesKeys
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import kotlinx.coroutines.flow.first


class OfflineAppPreferencesRepository(
    private val prefs: SharedPreferences,
    private val dataStore: DataStore<Preferences>
) : AppPreferencesRepository {

    override fun getUserId(): String? =
        prefs.getString("user_id", null)

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
}