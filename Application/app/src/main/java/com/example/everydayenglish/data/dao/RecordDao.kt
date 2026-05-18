package com.example.everydayenglish.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.everydayenglish.data.entity.ExerciseRecord

@Dao
interface ExerciseRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseRecord(exerciseRecord: ExerciseRecord)

    @Query("SELECT * FROM exercise_records ORDER BY timestamp DESC")
    suspend fun getAllExerciseRecords(): List<ExerciseRecord>

    @Query("SELECT * FROM exercise_records WHERE recordId = :id")
    suspend fun getRecordById(id: Int): ExerciseRecord?

    @Delete
    suspend fun deleteExerciseRecord(exerciseRecord: ExerciseRecord)

    @Query("SELECT * FROM exercise_records WHERE promptId = :promptId")
    suspend fun getRecordsByPromptId(promptId: Int): List<ExerciseRecord>

    @Query("SELECT * FROM exercise_records ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentRecords(limit: Int): List<ExerciseRecord>


}