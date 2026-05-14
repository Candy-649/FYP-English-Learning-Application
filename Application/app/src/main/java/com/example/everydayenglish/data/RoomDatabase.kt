package com.example.everydayenglish.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.everydayenglish.data.dao.ExerciseDao
import com.example.everydayenglish.data.dao.ExerciseRecordDao
import com.example.everydayenglish.data.dao.ReferenceAnswerDao
import com.example.everydayenglish.data.dao.UserProfileDao
import com.example.everydayenglish.data.entity.Exercise
import com.example.everydayenglish.data.entity.ExerciseRecord
import com.example.everydayenglish.data.entity.ReferenceAnswer
import com.example.everydayenglish.data.entity.UserProfile

@Database(
    entities = [
        Exercise::class,
        ExerciseRecord::class,
        UserProfile::class,
        ReferenceAnswer::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun recordDao(): ExerciseRecordDao
    abstract fun profileDao(): UserProfileDao
    abstract fun referenceAnswerDao(): ReferenceAnswerDao
}