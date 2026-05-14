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

    override suspend fun getRecordById(id: Int): ExerciseRecord? =
        exerciseRecordDao.getRecordById(id)

    override suspend fun deleteExerciseRecord(exerciseRecord: ExerciseRecord) =
        exerciseRecordDao.deleteExerciseRecord(exerciseRecord)

    override suspend fun getRecordsByExerciseId(exerciseId: Int): List<ExerciseRecord> =
        exerciseRecordDao.getRecordsByExerciseId(exerciseId)
}