package com.example.everydayenglish.data.OfflineRepository

import com.example.everydayenglish.adaptiveEngine.SwUcbBandit
import com.example.everydayenglish.adaptiveEngine.TenseCategory
import com.example.everydayenglish.data.Repository.BanditRepository
import com.example.everydayenglish.data.dao.ExerciseRecordDao
import com.example.everydayenglish.data.dao.QuestionAttemptDao
import com.example.everydayenglish.data.dao.ReferenceAnswerDao

class OfflineBanditRepository(
    private val questionAttemptDao: QuestionAttemptDao,
    private val windowSize: Int = 30
) : BanditRepository {

    private val bandit = SwUcbBandit(windowSize = windowSize)
    private var isRestored = false

    override suspend fun selectNextCategory(): TenseCategory {
        if (!isRestored) restoreFromAttempts()
        return bandit.selectArm()
    }

    override suspend fun updateFromAttempt(tense: String, accuracy: Double, timestamp: Long) {
        val category = TenseCategory.fromTenseString(tense) ?: return
        bandit.update(category, accuracy, timestamp)
    }

    override suspend fun restoreFromAttempts() {
        val recent = questionAttemptDao.getRecent(windowSize)
        recent.reversed().forEach { attempt ->
            val category = TenseCategory.fromTenseString(attempt.tense) ?: return@forEach
            bandit.update(category, attempt.accuracy, attempt.timestamp)
        }
        isRestored = true
    }

    override fun getStats() = bandit.getArmStats()
    override fun getTotalPulls() = bandit.totalPulls
    override fun getWindowSize() = bandit.windowSize
    override fun getExplorationC() = bandit.explorationC
}