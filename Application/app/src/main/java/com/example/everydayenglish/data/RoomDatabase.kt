package com.example.everydayenglish.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.everydayenglish.data.dao.DailyCompletionDao
import com.example.everydayenglish.data.dao.ExerciseDao
import com.example.everydayenglish.data.dao.ExerciseRecordDao
import com.example.everydayenglish.data.dao.QuestionAttemptDao
import com.example.everydayenglish.data.dao.ReferenceAnswerDao
import com.example.everydayenglish.data.dao.UserProfileDao
import com.example.everydayenglish.data.entity.DailyCompletion
import com.example.everydayenglish.data.entity.Exercise
import com.example.everydayenglish.data.entity.ExerciseRecord
import com.example.everydayenglish.data.entity.QuestionAttempt
import com.example.everydayenglish.data.entity.ReferenceAnswer
import com.example.everydayenglish.data.entity.UserProfile
import com.example.everydayenglish.data.typeConverter.UriTypeConverter

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE user_profiles ADD COLUMN todayCorrectCount INTEGER NOT NULL DEFAULT 0"
        )
    }
}

@Database(
    entities = [
        Exercise::class,
        ExerciseRecord::class,
        UserProfile::class,
        ReferenceAnswer::class,
        QuestionAttempt::class,
        DailyCompletion::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(UriTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun recordDao(): ExerciseRecordDao
    abstract fun profileDao(): UserProfileDao
    abstract fun referenceAnswerDao(): ReferenceAnswerDao
    abstract fun questionAttemptDao(): QuestionAttemptDao
    abstract fun dailyCompletionDao(): DailyCompletionDao

}