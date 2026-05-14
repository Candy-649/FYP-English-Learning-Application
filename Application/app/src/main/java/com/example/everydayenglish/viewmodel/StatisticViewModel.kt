package com.example.everydayenglish.viewmodel

import androidx.lifecycle.ViewModel

class StatisticViewModel: ViewModel(){

}

data class StatisticUiState(
    val todayStudy: Int = 0,
    val studyDuration: Int = 0,
    val tensesStatistic: Map<String, Int> = emptyMap(),
    val dailyExercises: Map<Int, Int> = emptyMap()
)