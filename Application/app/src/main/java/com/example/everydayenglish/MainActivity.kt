package com.example.everydayenglish

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.everydayenglish.data.PainterDefaults
import com.example.everydayenglish.navigation.CustomNavController
import com.example.everydayenglish.navigation.ScreenContent
import com.example.everydayenglish.ui.theme.EverydayEnglishTheme
import com.example.everydayenglish.viewmodel.AuthViewModel
import com.example.everydayenglish.viewmodel.DarkModeOption
import com.example.everydayenglish.viewmodel.ExerciseViewModel
import com.example.everydayenglish.viewmodel.HistoryViewModel
import com.example.everydayenglish.viewmodel.MainViewModel
import com.example.everydayenglish.viewmodel.ProfileViewModel
import com.example.everydayenglish.viewmodel.SettingViewModel
import com.example.everydayenglish.viewmodel.SplashViewModel
import com.example.everydayenglish.viewmodel.StatisticViewModel

sealed class Screen(val route: String){
    object MainScreen: Screen("main")
    object ExerciseScreen: Screen("exercise")
    object StatisticScreen: Screen("statistic")
    object SettingScreen: Screen("setting")
    object ProfileScreen: Screen("profile")
    object SplashScreen: Screen("splash")
    object HistoryScreen: Screen("history")
    object AuthScreen: Screen("auth")
    object ImageCropScreen: Screen("image_crop")

}

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        if (!isTablet){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        enableEdgeToEdge()
        PainterDefaults.defaultAvatarUri =
            "android.resource://${packageName}/${R.drawable.default_avatar}".toUri()
        PainterDefaults.defaultProfileBackgroundUri =
            "android.resource://${packageName}/${R.drawable.default_profile_background}".toUri()
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        setContent {
            AppNavigation()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(40)
        }
    }
}

@Composable
fun AppNavigation(){
    val splashViewModel: SplashViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val exerciseViewModel: ExerciseViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settingViewModel: SettingViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val mainViewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val statisticViewModel: StatisticViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)

    val settingUiState by settingViewModel.uiState.collectAsState()
    val darkTheme = when (settingUiState.darkModeOption){
        DarkModeOption.AUTO -> isSystemInDarkTheme()
        DarkModeOption.MANUAL -> settingUiState.darkModeEnabled
    }

    EverydayEnglishTheme(darkTheme = darkTheme) {
        val nav = remember { CustomNavController(Screen.SplashScreen.route) }

        val configuration = LocalConfiguration.current
        val isTablet = configuration.smallestScreenWidthDp >= 600
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val useDualPane = isTablet && isLandscape

        BackHandler(enabled = nav.canGoBack) { nav.popBack() }

        if (useDualPane && nav.previousRoute != null) {

            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    ScreenContent(
                        route = nav.currentRoute,
                        nav = nav,
                        splashViewModel = splashViewModel,
                        exerciseViewModel = exerciseViewModel,
                        profileViewModel = profileViewModel,
                        settingViewModel = settingViewModel,
                        mainViewModel = mainViewModel,
                        statisticViewModel = statisticViewModel,
                        historyViewModel = historyViewModel,
                        authViewModel = authViewModel
                    )
                }

                VerticalDivider()

                Box(Modifier.weight(1f)) {
                    ScreenContent(
                        route = nav.previousRoute!!,
                        nav = nav,
                        splashViewModel = splashViewModel,
                        mainViewModel = mainViewModel,
                        profileViewModel = profileViewModel,
                        exerciseViewModel = exerciseViewModel,
                        statisticViewModel = statisticViewModel,
                        historyViewModel = historyViewModel,
                        settingViewModel = settingViewModel,
                        authViewModel = authViewModel
                    )
                }
            }

        }else if(isTablet && isLandscape){
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
                    ScreenContent(
                        route = nav.currentRoute,
                        nav = nav,
                        splashViewModel = splashViewModel,
                        mainViewModel = mainViewModel,
                        exerciseViewModel = exerciseViewModel,
                        profileViewModel = profileViewModel,
                        historyViewModel = historyViewModel,
                        settingViewModel = settingViewModel,
                        statisticViewModel = statisticViewModel,
                        authViewModel = authViewModel
                    )
                }
            }
        }
        else {
            AnimatedContent(
                targetState = nav.currentRoute,
                transitionSpec = {
                    val enter = slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec  = tween(320)
                    )
                    val exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(320)
                    )
                    enter togetherWith exit
                },
                label = "nav_anim"
            ) { route ->
                ScreenContent(
                    route = route,
                    nav = nav,
                    splashViewModel = splashViewModel,
                    mainViewModel = mainViewModel,
                    exerciseViewModel = exerciseViewModel,
                    profileViewModel = profileViewModel,
                    historyViewModel = historyViewModel,
                    settingViewModel = settingViewModel,
                    statisticViewModel = statisticViewModel,
                    authViewModel = authViewModel
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
