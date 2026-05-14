package com.example.everydayenglish.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everydayenglish.R
import com.example.everydayenglish.ui.components.ClickableInfoCard
import com.example.everydayenglish.ui.components.SettingsInfoItem
import com.example.everydayenglish.ui.components.SettingsSection
import com.example.everydayenglish.ui.components.SettingsSwitchItem
import com.example.everydayenglish.ui.theme.EverydayEnglishTheme
import com.example.everydayenglish.viewmodel.AppLanguage
import com.example.everydayenglish.viewmodel.SettingUiState

@Composable
fun SettingScreen(

    uiState: SettingUiState,

    onLanguageClick: () -> Unit,

    onCacheClick: () -> Unit,

    onNotificationChange: (Boolean) -> Unit,

    onDarkModeChange: (Boolean) -> Unit,

    onSentenceCountClick: () -> Unit,

    onDailyGoalClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .padding(
                dimensionResource(
                    R.dimen.padding_medium
                )
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                dimensionResource(
                    R.dimen.padding_medium
                )
            )
    ) {

        Column {

            Text(
                "General Settings",

                modifier =
                    Modifier.padding(4.dp),

                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            SettingsSection {

                SettingsInfoItem(

                    text = "Language",

                    value =
                        uiState.language.toString(),

                    onClick =
                        onLanguageClick
                )

                SettingsInfoItem(

                    text = "Cache size",

                    value =
                        uiState.cacheSizeText,

                    onClick =
                        onCacheClick
                )

                SettingsSwitchItem(

                    text = "Notification",

                    checked =
                        uiState.notificationEnabled,

                    onCheckedChange =
                        onNotificationChange
                )

                SettingsSwitchItem(

                    text = "Dark Mode",

                    checked =
                        uiState.darkModeEnabled,

                    onCheckedChange =
                        onDarkModeChange
                )
            }
        }

        Column {

            Text(
                "Study Settings",

                modifier =
                    Modifier.padding(4.dp),

                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            SettingsSection {

                SettingsInfoItem(

                    text =
                        "Sentences count for analysis",

                    value =
                        uiState
                            .recentSentenceCount
                            .toString(),

                    onClick =
                        onSentenceCountClick
                )

                SettingsInfoItem(

                    text = "Daily goal",

                    value =
                        uiState.dailyGoal.toString(),

                    onClick =
                        onDailyGoalClick
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingScreenPreview(){
    EverydayEnglishTheme() {
        SettingScreen(
            uiState = SettingUiState(
                recentSentenceCount = 30,
                cacheSizeText = "128 MB",
                notificationEnabled = true,
                dailyGoal = 15,
                darkModeEnabled = true,
                language = AppLanguage.CHINESE
            ),
            onLanguageClick = {},
            onCacheClick = {},
            onNotificationChange = {},
            onDarkModeChange = {},
            onSentenceCountClick = {},
            onDailyGoalClick = {}
        )
    }
}