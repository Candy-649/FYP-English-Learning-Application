package com.example.everydayenglish.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "exercise_records")
data class ExerciseRecord(
    @PrimaryKey
    val recordId: String = UUID.randomUUID().toString(),   // 全局唯一，多设备生成不会撞车
    val promptId: Int,
    val userId: String,
    val referId: Int,
    val userAnswer: String,
    val isCorrect: Boolean,
    val grammar: String? = null,
    val semanticScore: Double? = null,
    val feedback: String? = null,
    val evaluationPending: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()       // 第三轮做 last-write-wins 合并用
)
