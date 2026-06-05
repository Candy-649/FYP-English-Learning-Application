package com.example.everydayenglish.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everydayenglish.R
import com.example.everydayenglish.ui.components.SettingsInfoItem
import com.example.everydayenglish.ui.components.SettingsSection
import com.example.everydayenglish.ui.components.SettingsSwitchItem
import com.example.everydayenglish.ui.components.WheelPicker
import com.example.everydayenglish.ui.theme.EverydayEnglishTheme
import com.example.everydayenglish.viewmodel.DarkModeOption
import com.example.everydayenglish.viewmodel.SettingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(

    uiState: SettingUiState,
    onBackClick: () -> Unit,
    onCacheClick: () -> Unit,

    onNotificationChange: (Boolean) -> Unit,

    onDarkModeChange: (Boolean) -> Unit,
    onDarkModeOptionChange: (DarkModeOption) -> Unit,

    onSentenceCountConfirm: (Int) -> Unit,

    onDailyGoalConfirm: (Int) -> Unit
) {
    var showDarkModeSheet by remember { mutableStateOf(false) }
    var showSentenceSheet    by remember { mutableStateOf(false) }
    var showDailyGoalSheet   by remember { mutableStateOf(false) }
    var pendingSentenceCount by remember {
        mutableStateOf(uiState.recentSentenceCount)
    }
    var pendingDailyGoal by remember {
        mutableStateOf(uiState.dailyGoal)
    }
    var pendingDarkMode by remember { mutableStateOf(uiState.darkModeOption) }

    // Dark Mode Sheet
    if (showDarkModeSheet) {
        val darkModeOptions = DarkModeOption.entries
        val initialIndex = darkModeOptions.indexOf(uiState.darkModeOption).coerceAtLeast(0)
        ModalBottomSheet(onDismissRequest = { showDarkModeSheet = false }) {
            PickerSheetContent(
                title = "Dark Mode",
                onConfirm = {                          // 加 confirm
                    onDarkModeOptionChange(pendingDarkMode)
                    showDarkModeSheet = false
                }
            ) {
                WheelPicker(
                    items = darkModeOptions,
                    initialIndex = initialIndex,
                    selectedValue = { pendingDarkMode = it }
                )
            }
        }
    }

    // Sentence Count Sheet
    if (showSentenceSheet) {
        val sentenceOptions = (20..50 step 5).toList()
        ModalBottomSheet(onDismissRequest = { showSentenceSheet = false }) {
            PickerSheetContent(
                title = "Sentences for Analysis",
                onConfirm = {
                    onSentenceCountConfirm(pendingSentenceCount)
                    showSentenceSheet = false
                }
            ) {
                WheelPicker(
                    items = sentenceOptions,
                    selectedValue = { pendingSentenceCount = it }
                )
            }
        }
    }

    // Daily Goal Sheet
    if (showDailyGoalSheet) {
        val goalOptions = (10..30 step 5).toList()
        var pending by remember {
            mutableStateOf(
                goalOptions.firstOrNull { it == uiState.dailyGoal }
                    ?: goalOptions.first()
            )
        }
        ModalBottomSheet(onDismissRequest = { showDailyGoalSheet = false }) {
            PickerSheetContent(
                title = "Daily Goal",
                onConfirm = {
                    onDailyGoalConfirm(pending)
                    showDailyGoalSheet = false
                }
            ) {
                WheelPicker(
                    items = goalOptions,
                    selectedValue = { pending = it }
                )
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ){paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
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

                    SettingsInfoItem(
                        text = "Dark Mode",
                        value = when (uiState.darkModeOption) {
                            DarkModeOption.AUTO   -> "Auto"
                            DarkModeOption.MANUAL -> if (uiState.darkModeEnabled) "On" else "Off"
                        },
                        onClick = { showDarkModeSheet = true }
                    )
                    AnimatedVisibility(visible = uiState.darkModeOption == DarkModeOption.MANUAL) {
                        SettingsSwitchItem(
                            text = "Enable Dark Mode",
                            checked = uiState.darkModeEnabled,
                            onCheckedChange = onDarkModeChange
                        )
                    }
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

                        onClick = { showSentenceSheet = true }
                    )

                    SettingsInfoItem(

                        text = "Daily goal",

                        value =
                            uiState.dailyGoal.toString(),

                        onClick = { showDailyGoalSheet = true }
                    )
                }
            }
        }
    }



}

@Composable
private fun PickerSheetContent(
    title: String,
    onConfirm: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(start = 4.dp, bottom = 12.dp)
                .align(Alignment.Start)
        )
        content()
        if (onConfirm != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm")
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
            ),
            onCacheClick = {},
            onBackClick = {},
            onNotificationChange = {},
            onDarkModeChange = {},
            onDarkModeOptionChange = {},
            onSentenceCountConfirm = {},
            onDailyGoalConfirm = {}
        )
    }
}