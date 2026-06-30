package com.example.everydayenglish.data.entity

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    val userId: String,                        // single user app, default to 1
    val userName: String,
    val avatarUri: Uri,                 // Uri stored as String
    val bio: String = "About me",
    val profileBackgroundUri: Uri,      // Uri stored as String
    val totalStudyDays: Int = 0,
    val totalSentencesCompleted: Int = 0,
    val currentStreak: Int = 0,
    val dailyGoal: Int = 10,
    val todayProgress: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val todayCorrectCount: Int = 0,
    val lastStudiedDate: Long = 0L,
    val recentSentenceCount: Int = 20,
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis()   // 第三轮做 last-write-wins 合并用
)