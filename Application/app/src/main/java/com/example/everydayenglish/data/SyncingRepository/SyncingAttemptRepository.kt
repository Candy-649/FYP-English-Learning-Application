package com.example.everydayenglish.data.SyncingRepository

import com.example.everydayenglish.data.Repository.AttemptRepository
import com.example.everydayenglish.data.Repository.AuthRepository
import com.example.everydayenglish.data.Repository.SyncRepository
import com.example.everydayenglish.data.entity.QuestionAttempt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SyncingAttemptRepository(
    private val offline: AttemptRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository
) : AttemptRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun insert(attempt: QuestionAttempt) {
        offline.insert(attempt)
        if (authRepository.currentUserId == attempt.userId) {
            syncScope.launch { syncRepository.pushAttempt(attempt) }
        }
    }

    // 同步合并专用，不在这里再触发一次 push（pullAndMerge 自己会决定要不要推）
    override suspend fun upsert(attempt: QuestionAttempt) = offline.upsert(attempt)

    override suspend fun getRecent(limit: Int): List<QuestionAttempt> = offline.getRecent(limit)

    override suspend fun getAllByUser(userId: String): List<QuestionAttempt> =
        offline.getAllByUser(userId)

    override suspend fun getTodaySolvedCount(userId: String, todayStart: Long): Int =
        offline.getTodaySolvedCount(userId, todayStart)
}
