package com.example.everydayenglish.data.OfflineRepository

import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.dao.ExerciseRecordDao
import com.example.everydayenglish.data.entity.ExerciseRecord

class OfflineRecordRepository(
    private val exerciseRecordDao: ExerciseRecordDao
) : RecordRepository {

    override suspend fun insertExerciseRecord(exerciseRecord: ExerciseRecord) =
        exerciseRecordDao.insertExerciseRecord(exerciseRecord)

    override suspend fun getAllExerciseRecords(): List<ExerciseRecord> =
        exerciseRecordDao.getAllExerciseRecords()

    override suspend fun getRecordById(id: String): ExerciseRecord? =
        exerciseRecordDao.getRecordById(id)

    override suspend fun deleteExerciseRecord(exerciseRecord: ExerciseRecord) =
        exerciseRecordDao.deleteExerciseRecord(exerciseRecord)

    override suspend fun getRecordsByPromptId(promptId: Int): List<ExerciseRecord> =
        exerciseRecordDao.getRecordsByPromptId(promptId)

    override suspend fun getPendingRecords(): List<ExerciseRecord> =
        exerciseRecordDao.getPendingRecords()

    override suspend fun updateEvaluation(
        recordId: String,
        score: Double,
        feedback: String,
        isCorrect: Boolean,
        updatedAt: Long
    ) = exerciseRecordDao.updateEvaluation(recordId, score, feedback, isCorrect, updatedAt)

    override suspend fun getAllByUser(userId: String): List<ExerciseRecord> =
        exerciseRecordDao.getAllByUser(userId)
}
