package com.example.everydayenglish.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reference_answers",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["promptId"],
            childColumns = ["promptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("promptId")]
)
data class ReferenceAnswer(
    @PrimaryKey(autoGenerate = true)
    val referId: Int,
    val promptId: Int,
    val reference: String,
    val tense: String? = null,
    val verbTagsLemmas: String? = null
)