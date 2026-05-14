package com.example.everydayenglish.data.Repository

import android.content.Context
import com.example.everydayenglish.adaptiveEngine.TenseCategory
import com.example.everydayenglish.data.entity.Exercise
import com.example.everydayenglish.data.entity.ExerciseWithReferenceAnswers
import com.example.everydayenglish.data.entity.ReferenceAnswer

interface ExerciseRepository {
    suspend fun insertExercises(exercises: List<Exercise>)
    suspend fun insertReferenceAnswers(referenceAnswers: List<ReferenceAnswer>)
    suspend fun getExercisesWithReferenceAnswers(): List<ExerciseWithReferenceAnswers>
    suspend fun getExercisesWithReferenceAnswersById(id: Int): ExerciseWithReferenceAnswers
    suspend fun getExerciseById(id: Int): Exercise
    suspend fun importExercises()
    suspend fun getExercisesByCategory(category: TenseCategory): List<Exercise>
}