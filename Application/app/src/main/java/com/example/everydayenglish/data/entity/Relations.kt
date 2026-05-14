package com.example.everydayenglish.data.entity

import androidx.room.Embedded
import androidx.room.Relation

//Relations
data class ExerciseWithReferenceAnswers(
    @Embedded val exercise: Exercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val answers: List<ReferenceAnswer>
)
