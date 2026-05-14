package com.example.everydayenglish

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.everydayenglish.viewmodel.ExerciseViewModel
import com.example.everydayenglish.viewmodel.MainViewModel
import com.example.everydayenglish.viewmodel.ProfileViewModel
import com.example.everydayenglish.viewmodel.SettingViewModel
import com.example.everydayenglish.viewmodel.SplashViewModel


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
                everydayEnglishApplication().container.exerciseRepository,
                everydayEnglishApplication().container.banditRepository
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
                everydayEnglishApplication().container.appPreferencesRepository
            )
        }
        initializer {
            MainViewModel(
                everydayEnglishApplication().container.userProfileRepository,
                everydayEnglishApplication().container.appPreferencesRepository
            )
        }
    }
}

fun CreationExtras.everydayEnglishApplication(): MyApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyApplication)