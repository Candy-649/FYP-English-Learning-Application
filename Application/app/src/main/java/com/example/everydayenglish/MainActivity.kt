package com.example.everydayenglish

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.everydayenglish.ui.theme.EverydayEnglishTheme
import com.example.everydayenglish.viewmodel.ExerciseViewModel
import com.example.everydayenglish.viewmodel.MainViewModel
import com.example.everydayenglish.viewmodel.ProfileViewModel
import com.example.everydayenglish.viewmodel.SettingViewModel
import com.example.everydayenglish.viewmodel.SplashViewModel
import com.example.everydayenglish.viewmodel.toBubbles

sealed class Screen(val route: String){
    object MainScreen: Screen("main")
    object ExerciseScreen: Screen("exercise")
    object StatisticScreen: Screen("statistic")
    object SettingScreen: Screen("setting")
    object ProfileScreen: Screen("profile")
    object SplashScreen: Screen("splash")

}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PainterDefaults.defaultAvatarUri = Uri.parse("android.resource://${packageName}/${R.drawable.default_avatar}")
        PainterDefaults.defaultProfileBackgroundUri = Uri.parse("android.resource://${packageName}/${R.drawable.default_profile_background}")
        setContent {
            EverydayEnglishTheme {
                AppNavigation()
            }
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

    NavHost(
        navController = navController,
        startDestination = Screen.MainScreen.route
    ){
        composable(Screen.MainScreen.route){
            MainScreen(
                uiState = mainViewModel.uiState.collectAsState().value
            )
        }
        composable(Screen.ExerciseScreen.route){
            val uiState = exerciseViewModel.uiState.collectAsState()
            ExerciseScreen(
                uiState = uiState.value,
                onAnswerChange = {
                    exerciseViewModel.updateUserAnswer(it)
                },
                onSubmit = {
                    exerciseViewModel.submitAnswer()
                },
                onNextExercise = {
                    exerciseViewModel
                        .goToNextExercise()
                },
                onReturn = {
                    navController.popBackStack()
                },
                onRestartSession = {
                    exerciseViewModel.restartSession()
                }
            )
        }
        composable(Screen.ProfileScreen.route) {
            val uiState = profileViewModel.uiState.collectAsState()
            ProfileScreen(
                uiState = uiState.value,
                bubbles = uiState.value.toBubbles()
            )
        }
        composable(Screen.SplashScreen.route){
            SplashScreen(splashViewModel)
        }
        composable(Screen.StatisticScreen.route){}
        composable(Screen.SettingScreen.route){
            SettingScreen(
                uiState = settingViewModel.uiState.collectAsState().value,
                onLanguageClick = {},
                onCacheClick = {},
                onNotificationChange = {
                    settingViewModel
                        .updateNotificationEnabled(it)
                },
                onDarkModeChange = {
                    settingViewModel
                        .updateDarkMode(it)
                },
                onSentenceCountClick = {},
                onDailyGoalClick = {}
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    EverydayEnglishTheme {
    }
}