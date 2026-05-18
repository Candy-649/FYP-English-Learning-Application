package com.example.everydayenglish.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everydayenglish.R
import com.example.everydayenglish.ui.theme.EverydayEnglishTheme
import com.example.everydayenglish.viewmodel.StatisticUiState
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticScreen(
    uiState: StatisticUiState,
    onBackClick: () -> Unit
){
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Statistic",
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier
                                .padding(dimensionResource(R.dimen.padding_small))
                        )
                    }
                },
                actions = {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        modifier = Modifier.padding(dimensionResource(R.dimen.padding_small))
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(dimensionResource(R.dimen.padding_large))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
        ){
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
            ){
                Card(
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(dimensionResource(R.dimen.padding_small))
                    ) {
                        Text("Today's Progress:")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
                        ) {
                            Text(
                                "${uiState.todayStudy}",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Done",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(dimensionResource(R.dimen.padding_small))
                    ) {
                        Text("Total study Time:")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
                        ) {
                            Text(
                                "${uiState.studyDuration}",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "mins",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    .padding(dimensionResource(R.dimen.padding_small))
            ) {
                Text(
                    "Practice Statistic",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                TenseBarChart(uiState.tensesStatistic)
            }
            Column(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    .padding(dimensionResource(R.dimen.padding_small))
            ) {
                Text(
                    "Daily Exercise Statistic",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                DailyExerciseChart(uiState.dailyExercises)
            }
        }
    }
}

@Composable
fun TenseBarChart(
    data: Map<String, Int>
){
    val modelProducer = remember { CartesianChartModelProducer() }
    val labels = data.keys.toList()

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries {
                series(
                    data.values.map { it.toFloat()}
                )
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = {_, value, _ ->
                    labels.getOrNull(value.toInt()) ?: ""
                },
                itemPlacer = remember {
                    HorizontalAxis.ItemPlacer.segmented(
                        shiftExtremeLines = true
                    )
                }
            )
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(),
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

@Composable
fun DailyExerciseChart(
    data: Map<Int, Int>
){
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data){
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = data.keys.toList(),
                    y = data.values.toList()
                )
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom()
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun StatisticScreenPreview(){
    val sampleStatistic = StatisticUiState(

        todayStudy = 12,

        studyDuration = 95,

        tensesStatistic = mapOf(
            "Present" to 25,
            "Past" to 18,
            "Future" to 10,
            "Present Perfect" to 7
        ),

        dailyExercises = mapOf(
            1 to 5,
            2 to 8,
            3 to 6,
            4 to 10,
            5 to 7,
            6 to 12,
            7 to 4
        )
    )
    EverydayEnglishTheme() {
        StatisticScreen(
            sampleStatistic,
            onBackClick = {}
        )
    }
}