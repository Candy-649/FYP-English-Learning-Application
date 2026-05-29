package com.example.everydayenglish.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    val userId: String,                        // single user app, default to 1
    val userName: String = "",
    val avatarUri: String = "",                 // Uri stored as String
    val bio: String = "About me",
    val profileBackgroundUri: String = "",      // Uri stored as String
    val totalStudyDays: Int = 0,
    val totalSentencesCompleted: Int = 0,
    val currentStreak: Int = 0,
    val dailyGoal: Int = 10,
    val todayProgress: Int = 0,
    val lastStudiedDate: Long = 0L,              // for streak calculation
    val recentSentenceCount: Int = 20
)