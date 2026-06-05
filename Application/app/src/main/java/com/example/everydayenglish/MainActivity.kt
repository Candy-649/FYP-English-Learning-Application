package com.example.everydayenglish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.everydayenglish.data.PainterDefaults
import com.example.everydayenglish.ui.ExerciseScreen
import com.example.everydayenglish.ui.MainScreen
import com.example.everydayenglish.ui.ProfileScreen
import com.example.everydayenglish.ui.SettingScreen
import com.example.everydayenglish.ui.SplashScreen
import com.example.everydayenglish.ui.StatisticScreen
import com.example.everydayenglish.ui.theme.EverydayEnglishTheme
import com.example.everydayenglish.viewmodel.ExerciseViewModel
import com.example.everydayenglish.viewmodel.MainViewModel
import com.example.everydayenglish.viewmodel.ProfileViewModel
import com.example.everydayenglish.viewmodel.SettingViewModel
import com.example.everydayenglish.viewmodel.SplashViewModel
import com.example.everydayenglish.viewmodel.StatisticViewModel
import com.example.everydayenglish.viewmodel.toBubbles
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.everydayenglish.ui.HistoryScreen
import com.example.everydayenglish.viewmodel.DarkModeOption
import com.example.everydayenglish.viewmodel.HistoryViewModel

sealed class Screen(val route: String){
    object MainScreen: Screen("main")
    object ExerciseScreen: Screen("exercise")
    object StatisticScreen: Screen("statistic")
    object SettingScreen: Screen("setting")
    object ProfileScreen: Screen("profile")
    object SplashScreen: Screen("splash")
    object HistoryScreen: Screen("history")

}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PainterDefaults.defaultAvatarUri =
            "android.resource://${packageName}/${R.drawable.default_avatar}".toUri()
        PainterDefaults.defaultProfileBackgroundUri =
            "android.resource://${packageName}/${R.drawable.default_profile_background}".toUri()
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
){
    val splashViewModel: SplashViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val exerciseViewModel: ExerciseViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settingViewModel: SettingViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val mainViewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val statisticViewModel: StatisticViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)

    val settingUiState by settingViewModel.uiState.collectAsState()
    val darkTheme = when (settingUiState.darkModeOption){
        DarkModeOption.AUTO -> isSystemInDarkTheme()
        DarkModeOption.MANUAL -> settingUiState.darkModeEnabled
    }

    EverydayEnglishTheme(darkTheme = darkTheme) {
        NavHost(
            navController = navController,
            startDestination = Screen.SplashScreen.route
        ){
            composable(Screen.MainScreen.route){
                LifecycleResumeEffect(Unit){
                    mainViewModel.refresh()
                    onPauseOrDispose{}
                }
                MainScreen(
                    uiState = mainViewModel.uiState.collectAsState().value,
                    onProfileClick = {
                        navController.navigate(Screen.ProfileScreen.route)
                    },
                    onStudyClick = {
                        navController.navigate(Screen.ExerciseScreen.route)
                    },
                    onStatisticClick = {
                        navController.navigate(Screen.StatisticScreen.route)
                    },
                    onHistoryClick = {
                        navController.navigate(Screen.HistoryScreen.route)
                    },
                    onSettingClick = {
                        navController.navigate(Screen.SettingScreen.route)
                    }
                )
            }
            composable(Screen.ExerciseScreen.route){
                val uiState = exerciseViewModel.uiState.collectAsState()
                ExerciseScreen(
                    uiState          = uiState.value,
                    onAnswerChange   = { exerciseViewModel.updateUserAnswer(it) },
                    onSubmit         = { exerciseViewModel.submitAnswer() },
                    onNext           = { exerciseViewModel.finishCurrentQuestion() },
                    onRetry          = { exerciseViewModel.dismissFeedback() },
                    onGiveUp         = { exerciseViewModel.finishCurrentQuestion(gaveUp = true) },
                    onReturn         = { navController.popBackStack() },
                    onRestartSession = { exerciseViewModel.restartSession() },
                    onToggleDebug    = { exerciseViewModel.toggleDebugPanel() }
                )
            }
            composable(Screen.ProfileScreen.route) {
                LifecycleResumeEffect(Unit) {
                    profileViewModel.refresh()
                    onPauseOrDispose {}
                }
                val uiState = profileViewModel.uiState.collectAsState().value
                ProfileScreen(
                    uiState = uiState,
                    bubbles = uiState.toBubbles(),
                    onBackClick = {
                        navController.popBackStack(Screen.MainScreen.route, inclusive = false)
                    },
                    onUserNameChange = profileViewModel::updateUserName,
                    onBioChange = profileViewModel::updateBio,
                    onSaveProfile = profileViewModel::saveProfile,
                    onAvatarChange = profileViewModel::updateAvatar,
                    onBackgroundChange = profileViewModel::updateBackground,
                    onSetEditing = profileViewModel::setEditing
                )
            }
            composable(Screen.SplashScreen.route){
                SplashScreen(
                    onNavigation = {
                        navController.navigate(Screen.MainScreen.route)
                    },
                    viewModel = splashViewModel)
            }
            composable(Screen.StatisticScreen.route){
                LifecycleResumeEffect(Unit) {
                    statisticViewModel.refresh()
                    onPauseOrDispose {}
                }
                StatisticScreen(
                    uiState = statisticViewModel.uiState.collectAsState().value,
                    onBackClick = {
                        navController.popBackStack(
                            Screen.MainScreen.route,
                            inclusive = false
                        )
                    }
                )
            }
            composable(Screen.SettingScreen.route){
                SettingScreen(
                    uiState = settingViewModel.uiState.collectAsState().value,
                    onCacheClick = {
                        settingViewModel.clearCache()
                    },
                    onBackClick = {
                        navController.popBackStack(
                            Screen.MainScreen.route,
                            inclusive = false
                        )
                    },
                    onNotificationChange = {
                        settingViewModel
                            .updateNotificationEnabled(it)
                    },
                    onDarkModeChange = {
                        settingViewModel
                            .updateDarkMode(it)
                    },
                    onDarkModeOptionChange = { settingViewModel.updateDarkModeOption(it) },
                    onSentenceCountConfirm = {
                        settingViewModel
                            .updateSentenceCount(it)
                    },
                    onDailyGoalConfirm = {
                        settingViewModel
                            .updateDailyGoal(it)
                    }
                )
            }
            composable(Screen.HistoryScreen.route) {
                LifecycleResumeEffect(Unit) {
                    historyViewModel.refresh()
                    onPauseOrDispose {}
                }
                HistoryScreen(
                    uiState     = historyViewModel.uiState.collectAsState().value,
                    onBackClick = { navController.popBackStack(Screen.MainScreen.route, inclusive = false) },
                    onCardClick = { historyViewModel.toggleExpand(it) }
                )
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    EverydayEnglishTheme {
    }
}