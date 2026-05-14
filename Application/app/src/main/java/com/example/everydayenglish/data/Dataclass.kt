package com.example.everydayenglish.data

import com.google.gson.annotations.SerializedName

data class ExerciseJson(
    @SerializedName("prompt_id")
    val promptId: Int,

    @SerializedName("prompt")
    val prompt: String,

    @SerializedName("refer_id")
    val referId: Int,

    @SerializedName("reference")
    val reference: String,

    @SerializedName("Final_Tense")
    val tense: String?,

    @SerializedName("Verb_Tags_Lemmas")
    val verbTagsLemmas: String?
)

data class AppPreferences(
    val initialized: Boolean,
    val userId: String
)