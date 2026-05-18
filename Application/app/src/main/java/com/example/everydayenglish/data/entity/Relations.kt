package com.example.everydayenglish.data.entity

import androidx.room.Embedded
import androidx.room.Relation

//Relations
data class ExerciseWithReferenceAnswers(
    @Embedded val exercise: Exercise,
    @Relation(
        parentColumn = "promptId",
        entityColumn = "promptId"
    )
    val answers: List<ReferenceAnswer>
)
