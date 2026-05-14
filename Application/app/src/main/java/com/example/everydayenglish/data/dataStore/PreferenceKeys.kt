package com.example.everydayenglish.data.dataStore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {

    val DAILY_GOAL =
        intPreferencesKey("daily_goal")

    val DARK_MODE =
        booleanPreferencesKey("dark_mode")

    val NOTIFICATION =
        booleanPreferencesKey("notification")

    val LANGUAGE =
        stringPreferencesKey("language")
}