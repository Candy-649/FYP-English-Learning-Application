package com.example.everydayenglish

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.everydayenglish.data.Repository.AttemptRepository
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
                everydayEnglishApplication().container.appPreferencesRepository
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
                feedbackGenerator = everydayEnglishApplication().container.feedbackGenerator
            )
        }

        initializer {
            ProfileViewModel(
                everydayEnglishApplication().container.userProfileRepository,
                everydayEnglishApplication().container.appPreferencesRepository
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
                everydayEnglishApplication().container.recordRepository,
                everydayEnglishApplication().container.exerciseRepository,
                everydayEnglishApplication().container.appPreferencesRepository
            )
        }
    }
}

fun CreationExtras.everydayEnglishApplication(): MyApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyApplication)