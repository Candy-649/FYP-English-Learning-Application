package com.example.everydayenglish.data.Repository

import com.example.everydayenglish.data.entity.ExerciseRecord
import com.example.everydayenglish.data.entity.QuestionAttempt
import com.example.everydayenglish.data.entity.UserProfile

interface SyncRepository {
    suspend fun pullAndMerge(userId: String)

    suspend fun pushProfile(profile: UserProfile)
    suspend fun pushRecord(record: ExerciseRecord)
    suspend fun pushAttempt(attempt: QuestionAttempt)
}
