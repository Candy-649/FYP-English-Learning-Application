package com.example.everydayenglish.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_attempts")
data class QuestionAttempt(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val promptId: Int,
    val userId: String,
    val tense: String,
    val totalTries: Int,
    val solved: Boolean,
    val accuracy: Double,
    val timestamp: Long = System.currentTimeMillis()
)
