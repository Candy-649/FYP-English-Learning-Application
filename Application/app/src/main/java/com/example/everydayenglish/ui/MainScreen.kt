package com.example.everydayenglish.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.everydayenglish.R
import com.example.everydayenglish.ui.components.JumpCard
import com.example.everydayenglish.ui.components.SettingsSection
import com.example.everydayenglish.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onProfileClick: () -> Unit,
    onStudyClick: () -> Unit,
    onStatisticClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingClick: () -> Unit
) {
    var showTutorial by remember { mutableStateOf(false) }

    var avatarBounds    by remember { mutableStateOf<Rect?>(null) }
    var progressBounds  by remember { mutableStateOf<Rect?>(null) }
    var startBtnBounds  by remember { mutableStateOf<Rect?>(null) }
    var statisticBounds by remember { mutableStateOf<Rect?>(null) }
    var historyBounds   by remember { mutableStateOf<Rect?>(null) }
    var settingBounds   by remember { mutableStateOf<Rect?>(null) }

    val tutorialSteps = remember(
        avatarBounds, progressBounds, startBtnBounds,
        statisticBounds, historyBounds, settingBounds
    ) {
        listOf(
            TutorialStep(avatarBounds,    "Your Profile",        "Tap your avatar to view and edit your profile."),
            TutorialStep(progressBounds,  "Daily Progress",      "This circle tracks how many exercises you've completed today."),
            TutorialStep(startBtnBounds,  "Start Learning",      "Tap here to begin today's English grammar exercises."),
            TutorialStep(statisticBounds, "Progress Statistics", "Check your learning trends and accuracy across different tenses."),
            TutorialStep(historyBounds,   "Study History",       "Review all your past exercise attempts and AI feedback."),
            TutorialStep(settingBounds,   "Study Settings",      "Adjust your daily goal and app preferences here."),
        )
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = onProfileClick,
                            modifier = Modifier
                                .padding(dimensionResource(R.dimen.padding_small))
                                .onGloballyPositioned { avatarBounds = it.boundsInRoot() }
                        ) {
                            AsyncImage(
                                model = uiState.userAvatar,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(dimensionResource(R.dimen.avatar_size))
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(
                                        2.dp,
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = CircleShape
                                    )
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTutorial = true }) {
                            Icon(Icons.AutoMirrored.Outlined.Help, contentDescription = "Help")
                        }
                    },
                    title = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(dimensionResource(R.dimen.padding_medium))
            ) {
                Text("Today's progress", style = MaterialTheme.typography.titleLarge)

                CircularProgressIndicator(
                    progress = { uiState.todayProgress / uiState.dailyGoal.toFloat() },
                    strokeWidth = 16.dp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(dimensionResource(R.dimen.progress_indicator))
                        .onGloballyPositioned { progressBounds = it.boundsInRoot() }
                )

                OutlinedButton(
                    onClick = onStudyClick,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .onGloballyPositioned { startBtnBounds = it.boundsInRoot() }
                ) {
                    Text("Start learning")
                }

                SettingsSection(
                    modifier = Modifier.padding(dimensionResource(R.dimen.padding_small))
                ) {
                    Box(Modifier.onGloballyPositioned { statisticBounds = it.boundsInRoot() }) {
                        JumpCard(icon = Icons.Default.BarChart, text = "Progress Statistic") {
                            onStatisticClick()
                        }
                    }
                    Box(Modifier.onGloballyPositioned { historyBounds = it.boundsInRoot() }) {
                        JumpCard(icon = Icons.Default.History, text = "Study History") {
                            onHistoryClick()
                        }
                    }
                    Box(Modifier.onGloballyPositioned { settingBounds = it.boundsInRoot() }) {
                        JumpCard(icon = Icons.Default.Settings, text = "Study Settings") {
                            onSettingClick()
                        }
                    }
                }
            }
        }

        if (showTutorial) {
            TutorialOverlay(
                steps = tutorialSteps,
                onDismiss = { showTutorial = false }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview(){
    MainScreen(
        uiState = MainUiState(),
        onProfileClick = {},
        onStudyClick = {},
        onStatisticClick = {},
        onSettingClick = {},
        onHistoryClick = {}
    )
}