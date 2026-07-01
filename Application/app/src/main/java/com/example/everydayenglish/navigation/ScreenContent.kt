package com.example.everydayenglish.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
                onAvatarChange     = profileViewModel::updateAvatar,
                onBackgroundChange = profileViewModel::updateBackground,
                onSetEditing       = profileViewModel::setEditing
            )
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
            // Gate 场景（Splash 直接落地到这里）stack 里只有这一个 route，没有"返回"的地方；
            // 从 Settings 主动点进来的场景 stack 里有上一页。两种场景统一处理：
            // 能 popBack 就 popBack，不能就清栈直接进 MainScreen。
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