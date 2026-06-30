package com.example.everydayenglish.data.SyncingRepository

import com.example.everydayenglish.data.Repository.AuthRepository
import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.Repository.SyncRepository
import com.example.everydayenglish.data.entity.ExerciseRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SyncingRecordRepository(
    private val offline: RecordRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository
) : RecordRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun pushIfLoggedIn(record: ExerciseRecord) {
        if (authRepository.currentUserId != record.userId) return
        syncScope.launch { syncRepository.pushRecord(record) }
    }

    override suspend fun insertExerciseRecord(exerciseRecord: ExerciseRecord) {
        offline.insertExerciseRecord(exerciseRecord)
        pushIfLoggedIn(exerciseRecord)
    }

    override suspend fun getAllExerciseRecords(): List<ExerciseRecord> =
        offline.getAllExerciseRecords()

    override suspend fun getRecordById(id: String): ExerciseRecord? =
        offline.getRecordById(id)

    override suspend fun deleteExerciseRecord(exerciseRecord: ExerciseRecord) =
        offline.deleteExerciseRecord(exerciseRecord)

    override suspend fun getRecordsByPromptId(promptId: Int): List<ExerciseRecord> =
        offline.getRecordsByPromptId(promptId)

    override suspend fun getPendingRecords(): List<ExerciseRecord> =
        offline.getPendingRecords()

    override suspend fun updateEvaluation(
        recordId: String,
        score: Double,
        feedback: String,
        isCorrect: Boolean,
        updatedAt: Long
    ) {
        offline.updateEvaluation(recordId, score, feedback, isCorrect, updatedAt)
        offline.getRecordById(recordId)?.let { pushIfLoggedIn(it) }
    }

    override suspend fun getAllByUser(userId: String): List<ExerciseRecord> =
        offline.getAllByUser(userId)
}
