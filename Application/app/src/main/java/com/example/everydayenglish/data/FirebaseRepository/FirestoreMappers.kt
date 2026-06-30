package com.example.everydayenglish.data.FirebaseRepository

import androidx.core.net.toUri
import com.example.everydayenglish.data.entity.ExerciseRecord
import com.example.everydayenglish.data.entity.QuestionAttempt
import com.example.everydayenglish.data.entity.UserProfile

fun UserProfile.toFirestoreMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "userName" to userName,
    "avatarUri" to avatarUri.toString(),
    "bio" to bio,
    "profileBackgroundUri" to profileBackgroundUri.toString(),
    "totalStudyDays" to totalStudyDays,
    "totalSentencesCompleted" to totalSentencesCompleted,
    "currentStreak" to currentStreak,
    "dailyGoal" to dailyGoal,
    "todayProgress" to todayProgress,
    "todayCorrectCount" to todayCorrectCount,
    "lastStudiedDate" to lastStudiedDate,
    "recentSentenceCount" to recentSentenceCount,
    "updatedAt" to updatedAt
)

fun Map<String, Any?>.toUserProfile(): UserProfile? = runCatching {
    UserProfile(
        userId = this["userId"] as String,
        userName = this["userName"] as String,
        avatarUri = (this["avatarUri"] as String).toUri(),
        bio = this["bio"] as? String ?: "About me",
        profileBackgroundUri = (this["profileBackgroundUri"] as String).toUri(),
        totalStudyDays = (this["totalStudyDays"] as? Long)?.toInt() ?: 0,
        totalSentencesCompleted = (this["totalSentencesCompleted"] as? Long)?.toInt() ?: 0,
        currentStreak = (this["currentStreak"] as? Long)?.toInt() ?: 0,
        dailyGoal = (this["dailyGoal"] as? Long)?.toInt() ?: 10,
        todayProgress = (this["todayProgress"] as? Long)?.toInt() ?: 0,
        todayCorrectCount = (this["todayCorrectCount"] as? Long)?.toInt() ?: 0,
        lastStudiedDate = this["lastStudiedDate"] as? Long ?: 0L,
        recentSentenceCount = (this["recentSentenceCount"] as? Long)?.toInt() ?: 20,
        updatedAt = this["updatedAt"] as? Long ?: 0L
    )
}.getOrNull()

fun ExerciseRecord.toFirestoreMap(): Map<String, Any?> = mapOf(
    "recordId" to recordId,
    "promptId" to promptId,
    "userId" to userId,
    "referId" to referId,
    "userAnswer" to userAnswer,
    "isCorrect" to isCorrect,
    "grammar" to grammar,
    "semanticScore" to semanticScore,
    "feedback" to feedback,
    "evaluationPending" to evaluationPending,
    "timestamp" to timestamp,
    "updatedAt" to updatedAt
)

fun Map<String, Any?>.toExerciseRecord(): ExerciseRecord? = runCatching {
    ExerciseRecord(
        recordId = this["recordId"] as String,
        promptId = (this["promptId"] as Long).toInt(),
        userId = this["userId"] as String,
        referId = (this["referId"] as Long).toInt(),
        userAnswer = this["userAnswer"] as String,
        isCorrect = this["isCorrect"] as? Boolean ?: false,
        grammar = this["grammar"] as? String,
        semanticScore = this["semanticScore"] as? Double,
        feedback = this["feedback"] as? String,
        evaluationPending = this["evaluationPending"] as? Boolean ?: false,
        timestamp = this["timestamp"] as? Long ?: 0L,
        updatedAt = this["updatedAt"] as? Long ?: 0L
    )
}.getOrNull()

fun QuestionAttempt.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "promptId" to promptId,
    "userId" to userId,
    "tense" to tense,
    "totalTries" to totalTries,
    "solved" to solved,
    "accuracy" to accuracy,
    "timestamp" to timestamp,
    "updatedAt" to updatedAt
)

fun Map<String, Any?>.toQuestionAttempt(): QuestionAttempt? = runCatching {
    QuestionAttempt(
        id = this["id"] as String,
        promptId = (this["promptId"] as Long).toInt(),
        userId = this["userId"] as String,
        tense = this["tense"] as String,
        totalTries = (this["totalTries"] as Long).toInt(),
        solved = this["solved"] as? Boolean ?: false,
        accuracy = this["accuracy"] as? Double ?: 0.0,
        timestamp = this["timestamp"] as? Long ?: 0L,
        updatedAt = this["updatedAt"] as? Long ?: 0L
    )
}.getOrNull()
