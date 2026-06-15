package com.example.everydayenglish.data.entity

import androidx.room.Entity

@Entity(tableName = "daily_completions", primaryKeys = ["userId", "dateEpochDay"])
data class DailyCompletion(
    val userId: String,
    val dateEpochDay: Int,
    val completed: Boolean
)
