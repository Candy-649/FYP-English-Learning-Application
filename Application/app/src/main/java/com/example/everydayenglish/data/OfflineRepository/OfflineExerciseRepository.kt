package com.example.everydayenglish.data.OfflineRepository

import android.content.Context
import com.example.everydayenglish.adaptiveEngine.TenseCategory
import com.example.everydayenglish.data.ExerciseJson
import com.example.everydayenglish.data.entity.ExerciseWithReferenceAnswers
import com.example.everydayenglish.data.entity.ReferenceAnswer
import com.example.everydayenglish.data.Repository.ExerciseRepository
import com.example.everydayenglish.data.dao.ExerciseDao
import com.example.everydayenglish.data.entity.Exercise
import com.example.everydayenglish.data.mapping.toExercise
import com.example.everydayenglish.data.mapping.toReferenceAnswer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class OfflineExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val context: Context
) : ExerciseRepository {

    override suspend fun insertExercises(exercises: List<Exercise>) =
        exerciseDao.insertExercise(exercises)

    override suspend fun insertReferenceAnswers(referenceAnswers: List<ReferenceAnswer>) =
        exerciseDao.insertReferenceAnswer(referenceAnswers)

    override suspend fun getExercisesWithReferenceAnswers(): List<ExerciseWithReferenceAnswers> =
        exerciseDao.getExercisesWithReferenceAnswers()
    override suspend fun getExercisesWithReferenceAnswersById(id: Int): ExerciseWithReferenceAnswers =
        exerciseDao.getExerciseWithReferenceAnswersById(id)


    override suspend fun getExerciseById(id: Int): Exercise =
        exerciseDao.getExerciseById(id)

    override suspend fun importExercises() {
        val jsonExercises = loadExercises()

        val exercises = jsonExercises
            .distinctBy { it.promptId }
            .map { it.toExercise() }
        exerciseDao.insertExercise(exercises)

        val answers = jsonExercises.map { it.toReferenceAnswer() }
        exerciseDao.insertReferenceAnswer(answers)
    }

    override suspend fun getExercisesByCategory(category: TenseCategory): List<Exercise> =
        exerciseDao.getExercisesByTense(category.displayName)

    private fun loadExercises(): List<ExerciseJson> {
        val jsonString = context.assets
            .open("questions.json")
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<ExerciseJson>>() {}.type
        return Gson().fromJson(jsonString, type)
    }
}