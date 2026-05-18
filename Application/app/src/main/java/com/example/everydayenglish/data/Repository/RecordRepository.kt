package com.example.everydayenglish.data.Repository

import com.example.everydayenglish.data.entity.ExerciseRecord


interface RecordRepository {
    suspend fun insertExerciseRecord(exerciseRecord: ExerciseRecord)
    suspend fun getAllExerciseRecords(): List<ExerciseRecord>
    suspend fun getRecordById(id: Int): ExerciseRecord?
    suspend fun deleteExerciseRecord(exerciseRecord: ExerciseRecord)
    suspend fun getRecordsByPromptId(promptId: Int): List<ExerciseRecord>
}