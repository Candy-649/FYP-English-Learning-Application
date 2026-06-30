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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profiles ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

        db.execSQL("""
            CREATE TABLE exercise_records_new (
                recordId TEXT NOT NULL PRIMARY KEY,
                promptId INTEGER NOT NULL,
                userId TEXT NOT NULL,
                referId INTEGER NOT NULL,
                userAnswer TEXT NOT NULL,
                isCorrect INTEGER NOT NULL,
                grammar TEXT,
                semanticScore REAL,
                feedback TEXT,
                evaluationPending INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO exercise_records_new
            SELECT 'legacy-' || recordId, promptId, userId, referId, userAnswer, isCorrect,
                   grammar, semanticScore, feedback, evaluationPending, timestamp, timestamp
            FROM exercise_records
        """.trimIndent())
        db.execSQL("DROP TABLE exercise_records")
        db.execSQL("ALTER TABLE exercise_records_new RENAME TO exercise_records")

        db.execSQL("""
            CREATE TABLE question_attempts_new (
                id TEXT NOT NULL PRIMARY KEY,
                promptId INTEGER NOT NULL,
                userId TEXT NOT NULL,
                tense TEXT NOT NULL,
                totalTries INTEGER NOT NULL,
                solved INTEGER NOT NULL,
                accuracy REAL NOT NULL,
                timestamp INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO question_attempts_new
            SELECT 'legacy-' || id, promptId, userId, tense, totalTries, solved, accuracy, timestamp, timestamp
            FROM question_attempts
        """.trimIndent())
        db.execSQL("DROP TABLE question_attempts")
        db.execSQL("ALTER TABLE question_attempts_new RENAME TO question_attempts")
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
    version = 4,
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