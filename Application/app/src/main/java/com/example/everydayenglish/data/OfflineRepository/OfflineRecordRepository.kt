package com.example.everydayenglish.data.OfflineRepository

import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.dao.ExerciseRecordDao
import com.example.everydayenglish.data.entity.ExerciseRecord

class OfflineRecordRepository(
    private val exerciseRecordDao: ExerciseRecordDao
) : RecordRepository {

    override suspend fun insertExerciseRecord(exerciseRecord: ExerciseRecord): Long =
        exerciseRecordDao.insertExerciseRecord(exerciseRecord)

    override suspend fun getAllExerciseRecords(): List<ExerciseRecord> =
        exerciseRecordDao.getAllExerciseRecords()

    override suspend fun getRecordById(id: Int): ExerciseRecord? =
        exerciseRecordDao.getRecordById(id)

    override suspend fun deleteExerciseRecord(exerciseRecord: ExerciseRecord) =
        exerciseRecordDao.deleteExerciseRecord(exerciseRecord)

    override suspend fun getRecordsByPromptId(promptId: Int): List<ExerciseRecord> =
        exerciseRecordDao.getRecordsByPromptId(promptId)
    override suspend fun getPendingRecords(): List<ExerciseRecord> =
        exerciseRecordDao.getPendingRecords()
    override suspend fun updateEvaluation(recordId: Int, score: Double, feedback: String, isCorrect: Boolean) =
        exerciseRecordDao.updateEvaluation(recordId, score, feedback, isCorrect)
    override suspend fun getAllByUser(userId: String): List<ExerciseRecord> =
        exerciseRecordDao.getAllByUser(userId)
}