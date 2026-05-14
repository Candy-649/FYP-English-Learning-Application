package com.example.everydayenglish.data

import android.content.Context
import androidx.room.Room
import com.example.everydayenglish.data.OfflineRepository.OfflineAppPreferencesRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineBanditRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineRecordRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineUserProfileRepository
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.BanditRepository
import com.example.everydayenglish.data.Repository.ExerciseRepository
import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.dataStore.dataStore
import com.example.everydayenglish.data.OfflineRepository.OfflineExerciseRepository

interface AppContainer {
    val exerciseRepository: ExerciseRepository
    val recordRepository: RecordRepository
    val userProfileRepository: UserProfileRepository
    val appPreferencesRepository: AppPreferencesRepository
    val banditRepository: BanditRepository
}

class AppDataContainer(context: Context) : AppContainer {
    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "everyday_english_db"
    ).build()

    override val exerciseRepository: ExerciseRepository =
        OfflineExerciseRepository(database.exerciseDao(), context.applicationContext)

    override val recordRepository: RecordRepository =
        OfflineRecordRepository(database.recordDao())

    override val userProfileRepository: UserProfileRepository =
        OfflineUserProfileRepository(database.profileDao())
    override val appPreferencesRepository: AppPreferencesRepository =
        OfflineAppPreferencesRepository(
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE),
            context.dataStore
        )
    override val banditRepository: BanditRepository =
        OfflineBanditRepository(database.recordDao(), database.referenceAnswerDao())

}