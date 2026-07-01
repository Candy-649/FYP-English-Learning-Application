package com.example.everydayenglish

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.everydayenglish.viewmodel.AuthViewModel
import com.example.everydayenglish.viewmodel.ExerciseViewModel
import com.example.everydayenglish.viewmodel.HistoryViewModel
import com.example.everydayenglish.viewmodel.MainViewModel
import com.example.everydayenglish.viewmodel.ProfileViewModel
import com.example.everydayenglish.viewmodel.SettingViewModel
import com.example.everydayenglish.viewmodel.SplashViewModel
import com.example.everydayenglish.viewmodel.StatisticViewModel


object AppViewModelProvider {

    val Factory = viewModelFactory {

        initializer {
            SplashViewModel(
                everydayEnglishApplication().container.exerciseRepository,
                everydayEnglishApplication().container.userProfileRepository,
                everydayEnglishApplication().container.authRepository,
                everydayEnglishApplication().container.syncRepository
            )
        }

        initializer {
            ExerciseViewModel(
                exerciseRepository       = everydayEnglishApplication().container.exerciseRepository,
                banditRepository         = everydayEnglishApplication().container.banditRepository,
                recordRepository         = everydayEnglishApplication().container.recordRepository,
                userProfileRepository    = everydayEnglishApplication().container.userProfileRepository,
                appPreferencesRepository = everydayEnglishApplication().container.appPreferencesRepository,
                attemptRepository        = everydayEnglishApplication().container.attemptRepository,
                grammarChecker           = everydayEnglishApplication().container.grammarChecker,
                semanticChecker          = everydayEnglishApplication().container.semanticChecker,
                feedbackGenerator = everydayEnglishApplication().container.feedbackGenerator,
                dailyCompletionRepository = everydayEnglishApplication().container.dailyCompletionRepository,
                correctAnswerRewardApplier = everydayEnglishApplication().container.correctAnswerRewardApplier
            )
        }

        initializer {
            ProfileViewModel(
                everydayEnglishApplication().container.userProfileRepository,
                everydayEnglishApplication().container.appPreferencesRepository,
                dailyCompletionRepository = everydayEnglishApplication().container.dailyCompletionRepository,
                avatarStorageRepository = everydayEnglishApplication().container.avatarStorageRepository,
                appContext = everydayEnglishApplication()
            )
        }

        initializer {
            SettingViewModel(
                everydayEnglishApplication().container.appPreferencesRepository,
                everydayEnglishApplication().container.userProfileRepository
            )
        }

        initializer {
            MainViewModel(
                everydayEnglishApplication().container.userProfileRepository,
                everydayEnglishApplication().container.appPreferencesRepository
            )
        }

        initializer {
            StatisticViewModel(
                everydayEnglishApplication().container.userProfileRepository,
                everydayEnglishApplication().container.attemptRepository,
                everydayEnglishApplication().container.appPreferencesRepository
            )
        }
        initializer {
            HistoryViewModel(
                recordRepository             = everydayEnglishApplication().container.recordRepository,
                exerciseRepository           = everydayEnglishApplication().container.exerciseRepository,
                appPreferencesRepository     = everydayEnglishApplication().container.appPreferencesRepository,
                grammarChecker                = everydayEnglishApplication().container.grammarChecker,
                semanticChecker                = everydayEnglishApplication().container.semanticChecker,
                feedbackGenerator              = everydayEnglishApplication().container.feedbackGenerator,
                correctAnswerRewardApplier     = everydayEnglishApplication().container.correctAnswerRewardApplier
            )
        }

        initializer {
            AuthViewModel(
                everydayEnglishApplication().container.authRepository,
                everydayEnglishApplication().container.syncRepository,
                everydayEnglishApplication().container.userProfileRepository,
                appContext = everydayEnglishApplication()
            )
        }
    }
}

fun CreationExtras.everydayEnglishApplication(): MyApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyApplication)