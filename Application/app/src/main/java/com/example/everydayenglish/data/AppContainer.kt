package com.example.everydayenglish.data

import com.example.everydayenglish.BuildConfig
import android.content.Context
import androidx.room.Room
import com.example.everydayenglish.data.FirebaseRepository.FirebaseAuthRepository
import com.example.everydayenglish.data.FirebaseRepository.FirebaseAvatarStorageRepository
import com.example.everydayenglish.data.FirebaseRepository.FirestoreSyncRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineAppPreferencesRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineAttemptRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineBanditRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineDailyCompletionRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineExerciseRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineRecordRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineUserProfileRepository
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.AttemptRepository
import com.example.everydayenglish.data.Repository.AuthRepository
import com.example.everydayenglish.data.Repository.AvatarStorageRepository
import com.example.everydayenglish.data.Repository.BanditRepository
import com.example.everydayenglish.data.Repository.DailyCompletionRepository
import com.example.everydayenglish.data.Repository.ExerciseRepository
import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.Repository.SyncRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.SyncingRepository.SyncingAttemptRepository
import com.example.everydayenglish.data.SyncingRepository.SyncingRecordRepository
import com.example.everydayenglish.data.SyncingRepository.SyncingUserProfileRepository
import com.example.everydayenglish.data.dataStore.dataStore
import com.example.everydayenglish.domain.CorrectAnswerRewardApplier
import com.example.everydayenglish.grammarChecker.GrammarChecker
import com.example.everydayenglish.onlineEvaluation.DeepSeekFeedbackGenerator
import com.example.everydayenglish.onlineEvaluation.FeedbackGenerator
import com.example.everydayenglish.onlineEvaluation.HuggingFaceSemanticChecker
import com.example.everydayenglish.onlineEvaluation.SemanticChecker
import com.example.everydayenglish.onlineEvaluation.SmartGrammarChecker

interface AppContainer {
    val exerciseRepository      : ExerciseRepository
    val recordRepository        : RecordRepository
    val userProfileRepository   : UserProfileRepository
    val appPreferencesRepository: AppPreferencesRepository
    val banditRepository        : BanditRepository
    val attemptRepository       : AttemptRepository
    val grammarChecker          : GrammarChecker
    val semanticChecker         : SemanticChecker
    val feedbackGenerator: FeedbackGenerator
    val dailyCompletionRepository: DailyCompletionRepository
    val correctAnswerRewardApplier: CorrectAnswerRewardApplier
    val authRepository: AuthRepository
    val syncRepository: SyncRepository
    val avatarStorageRepository: AvatarStorageRepository
}

class AppDataContainer(context: Context) : AppContainer {

    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "everyday_english_db"
    )
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
        .fallbackToDestructiveMigration(true) // fallback: if a future version forgets a migration, this avoids a hard crash
        .build()

    override val authRepository: AuthRepository =
        FirebaseAuthRepository()

    override val avatarStorageRepository: AvatarStorageRepository =
        FirebaseAvatarStorageRepository()

    // These are pure local implementations, only used internally by syncRepository and the
    // Syncing* decorators below - not exposed directly to ViewModels, which get the
    // Syncing-wrapped versions instead.
    private val offlineRecordRepository = OfflineRecordRepository(database.recordDao())
    private val offlineUserProfileRepository = OfflineUserProfileRepository(database.profileDao())
    private val offlineAttemptRepository = OfflineAttemptRepository(database.questionAttemptDao())

    override val syncRepository: SyncRepository =
        FirestoreSyncRepository(
            userProfileRepository = offlineUserProfileRepository,
            recordRepository = offlineRecordRepository,
            attemptRepository = offlineAttemptRepository
        )

    override val exerciseRepository: ExerciseRepository =
        OfflineExerciseRepository(database.exerciseDao(), context.applicationContext)

    override val recordRepository: RecordRepository =
        SyncingRecordRepository(offlineRecordRepository, syncRepository, authRepository)

    override val userProfileRepository: UserProfileRepository =
        SyncingUserProfileRepository(offlineUserProfileRepository, syncRepository, authRepository)

    override val appPreferencesRepository: AppPreferencesRepository =
        OfflineAppPreferencesRepository(
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE),
            context.dataStore,
            context.cacheDir,
            authRepository
        )

    override val banditRepository: BanditRepository =
        OfflineBanditRepository(database.questionAttemptDao())

    override val attemptRepository: AttemptRepository =
        SyncingAttemptRepository(offlineAttemptRepository, syncRepository, authRepository)

    // Grammar: online -> LanguageTool, offline -> ONNX
    override val grammarChecker =
        SmartGrammarChecker(context.applicationContext)

    // Semantic scoring (API key injected via local.properties -> BuildConfig)
    override val semanticChecker: SemanticChecker =
        HuggingFaceSemanticChecker(apiToken = BuildConfig.HF_API_TOKEN)
    override val feedbackGenerator: FeedbackGenerator =
        DeepSeekFeedbackGenerator(apiKey = BuildConfig.DEEPSEEK_API_KEY)
    override val dailyCompletionRepository: DailyCompletionRepository =
        OfflineDailyCompletionRepository(database.dailyCompletionDao())

    override val correctAnswerRewardApplier: CorrectAnswerRewardApplier =
        CorrectAnswerRewardApplier(attemptRepository, banditRepository, userProfileRepository)
}
