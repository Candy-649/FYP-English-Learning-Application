package com.example.everydayenglish.data

import com.example.everydayenglish.BuildConfig
import android.content.Context
import androidx.room.Room
import com.example.everydayenglish.data.OfflineRepository.OfflineAppPreferencesRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineAttemptRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineBanditRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineExerciseRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineRecordRepository
import com.example.everydayenglish.data.OfflineRepository.OfflineUserProfileRepository
import com.example.everydayenglish.data.Repository.AppPreferencesRepository
import com.example.everydayenglish.data.Repository.AttemptRepository
import com.example.everydayenglish.data.Repository.BanditRepository
import com.example.everydayenglish.data.Repository.ExerciseRepository
import com.example.everydayenglish.data.Repository.RecordRepository
import com.example.everydayenglish.data.Repository.UserProfileRepository
import com.example.everydayenglish.data.dataStore.dataStore
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
}

class AppDataContainer(context: Context) : AppContainer {

    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "everyday_english_db"
    )
        .build()

    override val exerciseRepository: ExerciseRepository =
        OfflineExerciseRepository(database.exerciseDao(), context.applicationContext)

    override val recordRepository: RecordRepository =
        OfflineRecordRepository(database.recordDao())

    override val userProfileRepository: UserProfileRepository =
        OfflineUserProfileRepository(database.profileDao())

    override val appPreferencesRepository: AppPreferencesRepository =
        OfflineAppPreferencesRepository(
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE),
            context.dataStore,
            context.cacheDir
        )

    override val banditRepository: BanditRepository =
        OfflineBanditRepository(database.questionAttemptDao())

    override val attemptRepository: AttemptRepository =
        OfflineAttemptRepository(database.questionAttemptDao())

    // 语法：有网 → LanguageTool，无网 → ONNX
    override val grammarChecker =
        SmartGrammarChecker(context.applicationContext)

    // 语义：Claude Haiku（API Key 从 local.properties → BuildConfig 注入）
    override val semanticChecker: SemanticChecker =
        HuggingFaceSemanticChecker(apiToken = BuildConfig.HF_API_TOKEN)
    override val feedbackGenerator: FeedbackGenerator =
        DeepSeekFeedbackGenerator(apiKey = BuildConfig.DEEPSEEK_API_KEY)
}