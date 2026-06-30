package com.example.everydayenglish.data.Repository

import com.example.everydayenglish.data.entity.ExerciseRecord


interface RecordRepository {
    suspend fun insertExerciseRecord(exerciseRecord: ExerciseRecord)
    suspend fun getAllExerciseRecords(): List<ExerciseRecord>
    suspend fun getRecordById(id: String): ExerciseRecord?
    suspend fun deleteExerciseRecord(exerciseRecord: ExerciseRecord)
    suspend fun getRecordsByPromptId(promptId: Int): List<ExerciseRecord>
    suspend fun getPendingRecords(): List<ExerciseRecord>
    suspend fun updateEvaluation(
        recordId: String,
        score: Double,
        feedback: String,
        isCorrect: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )
    suspend fun getAllByUser(userId: String): List<ExerciseRecord>
}
