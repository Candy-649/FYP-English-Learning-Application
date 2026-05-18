package com.example.everydayenglish.data.OfflineRepository

import com.example.everydayenglish.adaptiveEngine.SwUcbBandit
import com.example.everydayenglish.adaptiveEngine.TenseCategory
import com.example.everydayenglish.data.Repository.BanditRepository
import com.example.everydayenglish.data.dao.ExerciseDao
import com.example.everydayenglish.data.dao.ExerciseRecordDao
import com.example.everydayenglish.data.dao.ReferenceAnswerDao

class OfflineBanditRepository(
    private val exerciseRecordDao: ExerciseRecordDao,
    private val referenceAnswerDao: ReferenceAnswerDao,
    private val windowSize: Int = 30
): BanditRepository {
    private val bandit = SwUcbBandit(windowSize = windowSize)
    private var isRestored = false

    override suspend fun selectNextCategory(): TenseCategory {
        if (!isRestored) restoreFromRecords()
        return bandit.selectArm()
    }

    override suspend fun update(
        category: TenseCategory,
        isCorrect: Boolean,
        timestamp: Long
    ) {
        bandit.update(category, isCorrect, timestamp)
    }
    override suspend fun restoreFromRecords() {
        val recentRecords = exerciseRecordDao.getRecentRecords(windowSize)
        recentRecords.forEach { record ->
            val refAnswer = referenceAnswerDao.getReferenceAnswerByReferId(record.referId)
            val category = TenseCategory.fromTenseString(refAnswer?.tense)
                ?: return@forEach       // Non-Sentential/Other 跳过

            bandit.update(
                category = category,
                isCorrect = record.isCorrect,
                timestamp = record.timestamp
            )
        }
        isRestored = true
    }
    override fun getStats(): Map<TenseCategory, Pair<Double, Int>> =
        bandit.getArmStats()
    override fun getTotalPulls() = bandit.totalPulls
    override fun getWindowSize() = bandit.windowSize
    override fun getExplorationC() = bandit.explorationC
}