package com.example.everydayenglish.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "question_attempts")
data class QuestionAttempt(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),         // 全局唯一，多设备生成不会撞车
    val promptId: Int,
    val userId: String,
    val tense: String,
    val totalTries: Int,
    val solved: Boolean,
    val accuracy: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()       // 第三轮做 last-write-wins 合并用
)
