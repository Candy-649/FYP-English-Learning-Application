package com.example.everydayenglish.data.mapping

import com.example.everydayenglish.data.ExerciseJson
import com.example.everydayenglish.data.entity.Exercise
import com.example.everydayenglish.data.entity.ReferenceAnswer

fun ExerciseJson.toExercise() =
    Exercise(
        promptId = promptId,
        prompt = prompt
    )

fun ExerciseJson.toReferenceAnswer() =
    ReferenceAnswer(
        exerciseId = promptId,
        referId = referId,
        reference = reference,
        tense = tense,
        verbTagsLemmas = verbTagsLemmas
    )