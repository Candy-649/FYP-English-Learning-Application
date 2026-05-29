package com.example.everydayenglish.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.everydayenglish.data.dao.ExerciseDao
import com.example.everydayenglish.data.dao.ExerciseRecordDao
import com.example.everydayenglish.data.dao.QuestionAttemptDao
import com.example.everydayenglish.data.dao.ReferenceAnswerDao
import com.example.everydayenglish.data.dao.UserProfileDao
import com.example.everydayenglish.data.entity.Exercise
import com.example.everydayenglish.data.entity.ExerciseRecord
import com.example.everydayenglish.data.entity.QuestionAttempt
import com.example.everydayenglish.data.entity.ReferenceAnswer
import com.example.everydayenglish.data.entity.UserProfile

@Database(
    entities = [
        Exercise::class,
        ExerciseRecord::class,
        UserProfile::class,
        ReferenceAnswer::class,
        QuestionAttempt::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun recordDao(): ExerciseRecordDao
    abstract fun profileDao(): UserProfileDao
    abstract fun referenceAnswerDao(): ReferenceAnswerDao
    abstract fun questionAttemptDao(): QuestionAttemptDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_profiles ADD COLUMN recentSentenceCount INTEGER NOT NULL DEFAULT 20")
            }
        }
    }
}