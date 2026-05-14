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
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class ReferenceAnswer(
    @PrimaryKey(autoGenerate = true)
    val answerId: Int = 0,
    val exerciseId: Int,
    val referId: Int,
    val reference: String,
    val tense: String? = null,
    val verbTagsLemmas: String? = null
)