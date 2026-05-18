package com.example.everydayenglish.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey
    val promptId: Int,
    val prompt: String
)
