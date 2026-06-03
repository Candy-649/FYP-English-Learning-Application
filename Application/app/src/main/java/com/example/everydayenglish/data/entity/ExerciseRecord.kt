package com.example.everydayenglish.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_records")
data class ExerciseRecord(
    @PrimaryKey(autoGenerate = true)
    val recordId: Int = 0,
    val promptId: Int,
    val userId: String,
    val referId: Int,
    val userAnswer: String,
    val isCorrect: Boolean,
    val grammar: String? = null,
    val semanticScore: Double? = null,
    val feedback: String? = null,
    val evaluationPending: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)