package com.example.everydayenglish.data.Repository

import com.example.everydayenglish.data.entity.ExerciseRecord


interface RecordRepository {
    suspend fun insertExerciseRecord(exerciseRecord: ExerciseRecord): Long
    suspend fun getAllExerciseRecords(): List<ExerciseRecord>
    suspend fun getRecordById(id: Int): ExerciseRecord?
    suspend fun deleteExerciseRecord(exerciseRecord: ExerciseRecord)
    suspend fun getRecordsByPromptId(promptId: Int): List<ExerciseRecord>
    suspend fun getPendingRecords(): List<ExerciseRecord>
    suspend fun updateEvaluation(recordId: Int, score: Double, feedback: String, isCorrect: Boolean)
    suspend fun getAllByUser(userId: String): List<ExerciseRecord>
}