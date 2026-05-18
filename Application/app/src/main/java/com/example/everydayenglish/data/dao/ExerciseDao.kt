package com.example.everydayenglish.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.everydayenglish.data.entity.Exercise
import com.example.everydayenglish.data.entity.ExerciseWithReferenceAnswers
import com.example.everydayenglish.data.entity.ReferenceAnswer

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(
        exercises: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferenceAnswer(
        referenceAnswers: List<ReferenceAnswer>)

    @Transaction
    @Query("SELECT * FROM exercises")
    suspend fun getExercisesWithReferenceAnswers():
            List<ExerciseWithReferenceAnswers>

    @Transaction
    @Query("SELECT * FROM exercises WHERE promptId = :id")
    suspend fun getExerciseWithReferenceAnswersById(id: Int):
            ExerciseWithReferenceAnswers

    @Query("SELECT * FROM exercises WHERE promptId = :id")
    suspend fun getExerciseById(id: Int): Exercise

    @Query(
        """
        SELECT e.* FROM exercises e
        INNER JOIN reference_answers r ON e.promptId = r.promptId
        WHERE r.tense = :tense
        GROUP BY e.promptId
    """
    )
    suspend fun getExercisesByTense(tense: String): List<Exercise>

}