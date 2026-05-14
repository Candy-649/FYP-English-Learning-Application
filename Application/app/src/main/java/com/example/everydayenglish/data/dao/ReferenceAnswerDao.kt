package com.example.everydayenglish.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.everydayenglish.data.entity.ReferenceAnswer

@Dao
interface ReferenceAnswerDao {
    @Query("SELECT * FROM reference_answers WHERE referId = :id")
    suspend fun getReferenceAnswerByReferId(id: Int): ReferenceAnswer?

    @Query("SELECT * FROM reference_answers WHERE exerciseId = :id")
    suspend fun getReferenceAnswerByPromptId(id: Int): ReferenceAnswer?
}
