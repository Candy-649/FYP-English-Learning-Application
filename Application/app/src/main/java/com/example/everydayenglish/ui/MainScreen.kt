package com.example.everydayenglish.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everydayenglish.R
import com.example.everydayenglish.ui.components.JumpCard
import com.example.everydayenglish.ui.components.SettingsItem
import com.example.everydayenglish.ui.components.SettingsSection
import com.example.everydayenglish.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
){
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {/* profile */},
                        modifier = Modifier
                            .padding(dimensionResource(R.dimen.padding_small))
                    ){
                        Image(
                            painter = painterResource(id = R.drawable.default_avatar),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(dimensionResource(R.dimen.avatar_size))
                                .clip(CircleShape)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {/* More */}){
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
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
            Text(
                "Today's progress",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
            )
            CircularProgressIndicator(
                progress = { uiState.todayProgress / uiState.dailyGoal.toFloat() },
                strokeWidth = 16.dp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(dimensionResource(R.dimen.progress_indicator))
            )
            OutlinedButton(
                onClick = { /* Study */ },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            ) {
                Text("Start learning")
            }
            SettingsSection(
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.padding_small)),
            ) {
                JumpCard(
                    icon = Icons.Default.BarChart,
                    text = "Progress Statistic"
                ) {
                    /* Statistic Screen*/
                }
                JumpCard(
                    icon = Icons.Default.History,
                    text = "Progress Statistic"
                ) {
                    /* History Screen */
                }
                JumpCard(
                    icon = Icons.Default.Settings,
                    text = "Study Settings"
                ){
                    /* Setting Screen */
                }
            }
        }
    }


}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview(){
    MainScreen(uiState = MainUiState())
}