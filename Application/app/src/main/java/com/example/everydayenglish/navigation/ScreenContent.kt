package com.example.everydayenglish.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import com.example.everydayenglish.Screen
import com.example.everydayenglish.ui.*
import com.example.everydayenglish.viewmodel.*

@Composable
fun ScreenContent(
    route: String,
    nav: CustomNavController,
    mainViewModel: MainViewModel,
    profileViewModel: ProfileViewModel,
    exerciseViewModel: ExerciseViewModel,
    statisticViewModel: StatisticViewModel,
    historyViewModel: HistoryViewModel,
    settingViewModel: SettingViewModel,
    splashViewModel: SplashViewModel,
    authViewModel: AuthViewModel,
) {
    when (route) {

        Screen.SplashScreen.route -> {
            val needsAuthGate = splashViewModel.needsAuthGate.collectAsState().value
            SplashScreen(
                onNavigation = {
                    nav.stack.clear()
                    nav.navigate(
                        if (needsAuthGate) Screen.AuthScreen.route else Screen.MainScreen.route
                    )
                },
                viewModel = splashViewModel
            )
        }

        Screen.MainScreen.route -> {
            LaunchedEffect(route) { mainViewModel.refresh() }
            MainScreen(
                uiState          = mainViewModel.uiState.collectAsState().value,
                onProfileClick   = { nav.navigate(Screen.ProfileScreen.route) },
                onStudyClick     = { nav.navigate(Screen.ExerciseScreen.route) },
                onStatisticClick = { nav.navigate(Screen.StatisticScreen.route) },
                onHistoryClick   = { nav.navigate(Screen.HistoryScreen.route) },
                onSettingClick   = { nav.navigate(Screen.SettingScreen.route) }
            )
        }

        Screen.ProfileScreen.route -> {
            LaunchedEffect(route) { profileViewModel.refresh() }
            val uiState = profileViewModel.uiState.collectAsState().value
            ProfileScreen(
                uiState            = uiState,
                bubbles            = uiState.toBubbles(),
                onBackClick        = { nav.popBackTo(Screen.MainScreen.route) },
                onUserNameChange   = profileViewModel::updateUserName,
                onBioChange        = profileViewModel::updateBio,
                onSaveProfile      = profileViewModel::saveProfile,
                onRequestCrop      = { uri, target ->
                    profileViewModel.requestCrop(uri, target)
                    nav.navigate(Screen.ImageCropScreen.route)
                },
                onSetEditing       = profileViewModel::setEditing
            )
        }

        Screen.ImageCropScreen.route -> {
            // remember, not collectAsState: we only need this route's OWN request, captured
            // once on entry. If this kept observing the live StateFlow, onConfirm/onCancel
            // clearing cropRequest to null (as part of normal completion) would make this
            // branch see request==null while still composed and fire the null-guard below
            // a second time, double-popping the nav stack.
            val request = remember { profileViewModel.cropRequest.value }
            if (request == null) {
                // Nothing pending (e.g. process death mid-crop) - bail back rather than
                // rendering a crop screen with no source image.
                LaunchedEffect(Unit) { nav.popBack() }
            } else {
                val isAvatar = request.target == CropTarget.AVATAR
                ImageCropScreen(
                    sourceUri          = request.sourceUri,
                    shape              = if (isAvatar) CropShape.CIRCLE else CropShape.RECT,
                    // null for background = derive from this device's actual screen ratio
                    // instead of guessing a fixed one (see ImageCropScreen for why).
                    aspectRatio        = if (isAvatar) 1f else null,
                    outputFileName     = if (isAvatar)
                        "user_avatar_${System.currentTimeMillis()}.jpg"
                    else
                        "profile_background_${System.currentTimeMillis()}.jpg",
                    maxOutputDimension = if (isAvatar) 512 else 1024,
                    onConfirm          = { croppedUri ->
                        profileViewModel.consumeCropResult(croppedUri)
                        nav.popBack()
                    },
                    onCancel           = {
                        profileViewModel.cancelCrop()
                        nav.popBack()
                    }
                )
            }
        }

        Screen.ExerciseScreen.route -> {
            ExerciseScreen(
                uiState          = exerciseViewModel.uiState.collectAsState().value,
                onAnswerChange   = { exerciseViewModel.updateUserAnswer(it) },
                onSubmit         = { exerciseViewModel.submitAnswer() },
                onNext           = { exerciseViewModel.finishCurrentQuestion() },
                onRetry          = { exerciseViewModel.dismissFeedback() },
                onGiveUp         = { exerciseViewModel.finishCurrentQuestion(gaveUp = true) },
                onReturn         = { nav.popBackTo(Screen.MainScreen.route) },
                onRestartSession = { exerciseViewModel.restartSession() },
                onToggleDebug    = { exerciseViewModel.toggleDebugPanel() }
            )
        }

        Screen.StatisticScreen.route -> {
            LaunchedEffect(route) { statisticViewModel.refresh() }
            StatisticScreen(
                uiState     = statisticViewModel.uiState.collectAsState().value,
                onBackClick = { nav.popBackTo(Screen.MainScreen.route) }
            )
        }

        Screen.HistoryScreen.route -> {
            LaunchedEffect(route) { historyViewModel.refresh() }
            HistoryScreen(
                uiState            = historyViewModel.uiState.collectAsState().value,
                onBackClick        = { nav.popBackTo(Screen.MainScreen.route) },
                onCardClick        = { historyViewModel.toggleExpand(it) },
                onRedoClick        = { historyViewModel.openRedo(it) },
                onRedoAnswerChange = { historyViewModel.updateRedoAnswer(it) },
                onSubmitRedo       = { historyViewModel.submitRedo() },
                onRetryRedo        = { historyViewModel.retryRedo() },
                onCloseRedo        = { historyViewModel.closeRedo() }
            )
        }

        Screen.SettingScreen.route -> {
            LaunchedEffect(route) { authViewModel.refresh() }
            val authUiState = authViewModel.uiState.collectAsState().value
            SettingScreen(
                uiState                 = settingViewModel.uiState.collectAsState().value,
                onBackClick             = { nav.popBackTo(Screen.MainScreen.route) },
                onCacheClick            = { settingViewModel.clearCache() },
                onNotificationChange    = { settingViewModel.updateNotificationEnabled(it) },
                onDarkModeChange        = { settingViewModel.updateDarkMode(it) },
                onDarkModeOptionChange  = { settingViewModel.updateDarkModeOption(it) },
                onSentenceCountConfirm  = { settingViewModel.updateSentenceCount(it) },
                onDailyGoalConfirm      = { settingViewModel.updateDailyGoal(it) },
                isLoggedIn              = authUiState.currentUserEmail != null,
                currentUserEmail        = authUiState.currentUserEmail,
                onAccountClick          = { nav.navigate(Screen.AuthScreen.route) },
                onLogoutClick           = {
                    authViewModel.logout()
                    nav.stack.clear()
                    nav.navigate(Screen.AuthScreen.route)
                }
            )
        }

        Screen.AuthScreen.route -> {
            LaunchedEffect(route) { authViewModel.refresh() }
            // Gate scenario (Splash lands directly here): stack only has this one route,
            // nowhere to "go back" to. Coming in from Settings: stack has a previous page.
            // Handle both the same way: popBack if possible, otherwise clear and go to Main.
            val landOnMain: () -> Unit = {
                if (nav.canGoBack) nav.popBack()
                else {
                    nav.stack.clear()
                    nav.navigate(Screen.MainScreen.route)
                }
            }
            AuthScreen(
                uiState          = authViewModel.uiState.collectAsState().value,
                onBackClick      = { nav.popBack() },
                onSkipClick      = { authViewModel.continueAsGuest(onSuccess = landOnMain) },
                onModeChange     = { authViewModel.switchMode(it) },
                onEmailChange    = { authViewModel.updateEmail(it) },
                onPasswordChange = { authViewModel.updatePassword(it) },
                onSubmit         = { authViewModel.submit(onSuccess = landOnMain) },
                onLogoutClick    = {
                    authViewModel.logout()
                    nav.stack.clear()
                    nav.navigate(Screen.AuthScreen.route)
                }
            )
        }
    }
}