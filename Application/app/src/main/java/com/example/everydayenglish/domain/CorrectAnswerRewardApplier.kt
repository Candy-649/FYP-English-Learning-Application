package com.example.everydayenglish.domain

import com.example.everydayenglish.data.Repository.AttemptRepository
import com.example.everydayenglish.data.Repository.BanditRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.entity.QuestionAttempt

class CorrectAnswerRewardApplier(
    private val attemptRepository: AttemptRepository,
    private val banditRepository: BanditRepository,
    private val userProfileRepository: UserProfileRepository
) {
    suspend fun apply(
        promptId: Int,
        userId: String,
        tense: String,
        totalTries: Int = 1
    ) {
        val now = System.currentTimeMillis()
        val accuracy = 1.0 / totalTries

        attemptRepository.insert(
            QuestionAttempt(
                promptId   = promptId,
                userId     = userId,
                tense      = tense,
                totalTries = totalTries,
                solved     = true,
                accuracy   = accuracy
            )
        )
        banditRepository.updateFromAttempt(tense, accuracy, now)
        userProfileRepository.incrementSentencesCompleted(userId)

        val profile = userProfileRepository.getUserProfileForToday(userId)
        userProfileRepository.updateTodayProgress(
            (profile?.todayProgress ?: 0) + 1, now, userId
        )
        userProfileRepository.updateTodayCorrectCount(
            (profile?.todayCorrectCount ?: 0) + 1, userId
        )
    }
}
