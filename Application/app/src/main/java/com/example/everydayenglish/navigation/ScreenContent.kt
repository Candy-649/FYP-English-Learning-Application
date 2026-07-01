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
    node: NavNode,
    nav: TreeNavController,
    mainViewModel: MainViewModel,
    profileViewModel: ProfileViewModel,
    exerciseViewModel: ExerciseViewModel,
    statisticViewModel: StatisticViewModel,
    historyViewModel: HistoryViewModel,
    settingViewModel: SettingViewModel,
    splashViewModel: SplashViewModel,
    authViewModel: AuthViewModel,
) {
    // Every nav.navigate()/popBack()/popBackTo() call below passes `from = node` explicitly.
    // `node` is whichever tree node this particular pane is rendering - which in dual-pane
    // may be current.parent, not current itself. Letting these default to `current` would
    // reintroduce the bug where a tap in the parent's pane grew a child off the wrong node.
    val route = node.route

    when (route) {

        Screen.SplashScreen.route -> {
            val needsAuthGate = splashViewModel.needsAuthGate.collectAsState().value
            SplashScreen(
                onNavigation = {
                    nav.reset(if (needsAuthGate) Screen.AuthScreen.route else Screen.MainScreen.route)
                },
                viewModel = splashViewModel
            )
        }

        Screen.MainScreen.route -> {
            LaunchedEffect(node) { mainViewModel.refresh() }
            MainScreen(
                uiState          = mainViewModel.uiState.collectAsState().value,
                onProfileClick   = { nav.navigate(Screen.ProfileScreen.route, from = node) },
                onStudyClick     = { nav.navigate(Screen.ExerciseScreen.route, from = node) },
                onStatisticClick = { nav.navigate(Screen.StatisticScreen.route, from = node) },
                onHistoryClick   = { nav.navigate(Screen.HistoryScreen.route, from = node) },
                onSettingClick   = { nav.navigate(Screen.SettingScreen.route, from = node) }
            )
        }

        Screen.ProfileScreen.route -> {
            LaunchedEffect(node) { profileViewModel.refresh() }
            val uiState = profileViewModel.uiState.collectAsState().value
            ProfileScreen(
                uiState            = uiState,
                bubbles            = uiState.toBubbles(),
                onBackClick        = { nav.popBackTo(Screen.MainScreen.route, from = node) },
                onUserNameChange   = profileViewModel::updateUserName,
                onBioChange        = profileViewModel::updateBio,
                onSaveProfile      = profileViewModel::saveProfile,
                onRequestCrop      = { uri, target ->
                    profileViewModel.requestCrop(uri, target)
                    nav.navigate(Screen.ImageCropScreen.route, from = node)
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
                LaunchedEffect(Unit) { nav.popBack(from = node) }
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
                        nav.popBack(from = node)
                    },
                    onCancel           = {
                        profileViewModel.cancelCrop()
                        nav.popBack(from = node)
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
                onReturn         = { nav.popBackTo(Screen.MainScreen.route, from = node) },
                onRestartSession = { exerciseViewModel.restartSession() },
                onToggleDebug    = { exerciseViewModel.toggleDebugPanel() }
            )
        }

        Screen.StatisticScreen.route -> {
            LaunchedEffect(node) { statisticViewModel.refresh() }
            StatisticScreen(
                uiState     = statisticViewModel.uiState.collectAsState().value,
                onBackClick = { nav.popBackTo(Screen.MainScreen.route, from = node) }
            )
        }

        Screen.HistoryScreen.route -> {
            LaunchedEffect(node) { historyViewModel.refresh() }
            HistoryScreen(
                uiState            = historyViewModel.uiState.collectAsState().value,
                onBackClick        = { nav.popBackTo(Screen.MainScreen.route, from = node) },
                onCardClick        = { historyViewModel.toggleExpand(it) },
                onRedoClick        = { historyViewModel.openRedo(it) },
                onRedoAnswerChange = { historyViewModel.updateRedoAnswer(it) },
                onSubmitRedo       = { historyViewModel.submitRedo() },
                onRetryRedo        = { historyViewModel.retryRedo() },
                onCloseRedo        = { historyViewModel.closeRedo() }
            )
        }

        Screen.SettingScreen.route -> {
            LaunchedEffect(node) { authViewModel.refresh() }
            val authUiState = authViewModel.uiState.collectAsState().value
            SettingScreen(
                uiState                 = settingViewModel.uiState.collectAsState().value,
                onBackClick             = { nav.popBackTo(Screen.MainScreen.route, from = node) },
                onCacheClick            = { settingViewModel.clearCache() },
                onNotificationChange    = { settingViewModel.updateNotificationEnabled(it) },
                onDarkModeChange        = { settingViewModel.updateDarkMode(it) },
                onDarkModeOptionChange  = { settingViewModel.updateDarkModeOption(it) },
                onSentenceCountConfirm  = { settingViewModel.updateSentenceCount(it) },
                onDailyGoalConfirm      = { settingViewModel.updateDailyGoal(it) },
                isLoggedIn              = authUiState.currentUserEmail != null,
                currentUserEmail        = authUiState.currentUserEmail,
                onAccountClick          = { nav.navigate(Screen.AuthScreen.route, from = node) },
                onLogoutClick           = {
                    authViewModel.logout()
                    nav.reset(Screen.AuthScreen.route)
                }
            )
        }

        Screen.AuthScreen.route -> {
            LaunchedEffect(node) { authViewModel.refresh() }
            // Gate scenario (Splash lands directly here): stack only has this one route,
            // nowhere to "go back" to. Coming in from Settings: stack has a previous page.
            // Handle both the same way: popBack if possible, otherwise clear and go to Main.
            val landOnMain: () -> Unit = {
                if (node.parent != null) nav.popBack(from = node)
                else nav.reset(Screen.MainScreen.route)
            }
            AuthScreen(
                uiState          = authViewModel.uiState.collectAsState().value,
                onBackClick      = { nav.popBack(from = node) },
                onSkipClick      = { authViewModel.continueAsGuest(onSuccess = landOnMain) },
                onModeChange     = { authViewModel.switchMode(it) },
                onEmailChange    = { authViewModel.updateEmail(it) },
                onPasswordChange = { authViewModel.updatePassword(it) },
                onSubmit         = { authViewModel.submit(onSuccess = landOnMain) },
                onLogoutClick    = {
                    authViewModel.logout()
                    nav.reset(Screen.AuthScreen.route)
                }
            )
        }
    }
}