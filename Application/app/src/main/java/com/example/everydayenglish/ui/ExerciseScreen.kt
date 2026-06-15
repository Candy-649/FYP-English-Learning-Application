package com.example.everydayenglish.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everydayenglish.R
import com.example.everydayenglish.data.entity.Exercise
import com.example.everydayenglish.data.entity.ExerciseWithReferenceAnswers
import com.example.everydayenglish.data.entity.ReferenceAnswer
import com.example.everydayenglish.ui.theme.EverydayEnglishTheme
import com.example.everydayenglish.viewmodel.ArmDebugInfo
import com.example.everydayenglish.viewmodel.ExerciseUiState
import com.example.everydayenglish.viewmodel.FeedbackState
import com.example.everydayenglish.viewmodel.SelectionStepLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    uiState: ExerciseUiState,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,  // now calls ViewModel.goToNextExercise()
    onRetry: () -> Unit,
    onGiveUp: () -> Unit,
    onReturn: () -> Unit,
    onRestartSession: () -> Unit,
    onToggleDebug: () -> Unit
) {
    val exercise = uiState.currentExercise

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.padding_small),
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left label: questions answered this session
                        Text(
                            text = uiState.todayProgress.toString(),
                            style = MaterialTheme.typography.labelMedium
                        )

                        // Progress bar: fraction of daily goal completed
                        LinearProgressIndicator(
                            progress = {
                                if (uiState.dailyGoal == 0) 0f
                                else uiState.todayProgress.toFloat() / uiState.dailyGoal.toFloat()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(dimensionResource(R.dimen.padding_medium))
                        )

                        // Right label: daily goal
                        Text(
                            text = uiState.dailyGoal.toString(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onReturn) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            modifier = Modifier.padding(
                                dimensionResource(R.dimen.padding_small)
                            )
                        )
                    }
                },
                /*actions = {
                    IconButton(onClick = onToggleDebug) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Debug",
                            tint = if (uiState.showDebugPanel)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                },*/
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {

                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(dimensionResource(R.dimen.padding_medium)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(onClick = onRestartSession) {
                            Text("Retry")
                        }
                    }
                }

                uiState.isSessionDone -> {
                    SessionCompleteContent(
                        totalAnswered = uiState.totalAnswered,
                        correctCount  = uiState.correctCount,
                        dailyGoal     = uiState.dailyGoal,
                        onReturn      = onReturn,
                        onRestart     = onRestartSession
                    )
                }

                exercise != null -> {
                    ExerciseContent(
                        exercise     = exercise,
                        userAnswer   = uiState.userAnswer,
                        onAnswerChange = onAnswerChange,
                        onSubmit     = onSubmit
                    )
                }
            }

            /*AnimatedVisibility(
                visible = uiState.showDebugPanel,
                modifier = Modifier.align(Alignment.BottomEnd),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                DebugPanel(
                    steps = uiState.selectionSteps,
                    currentArmStats = uiState.currentArmStats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.45f)
                )
            }*/
        }
    }

    uiState.feedbackState?.let { feedback ->
        FeedbackDialog(
            feedback = feedback,
            onNext   = onNext,
            onRetry  = onRetry,
            onGiveUp = onGiveUp
        )
    }
}

@Composable
private fun FeedbackDialog(
    feedback: FeedbackState,
    onNext  : () -> Unit,
    onRetry : () -> Unit,
    onGiveUp: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                when {
                    feedback.evaluationOffline  -> "Pending Evaluation ⏳"
                    feedback.isEvaluating       -> "Evaluating…"
                    feedback.isCorrect == true  -> "Correct! 🎉"
                    feedback.isCorrect == false -> "Incorrect"
                    else                        -> "Submitted"
                }
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // Reference
                feedback.matchedReferenceAnswer?.reference?.let { ref ->
                    Text(
                        text  = "Reference: $ref",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when {
                    feedback.evaluationOffline -> {
                        Text(
                            text  = "AI evaluation will resume when you're back online.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    feedback.isEvaluating -> {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text  = "Evaluating your answer…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    feedback.isFeedbackLoading -> {
                        feedback.semanticScore?.let { score ->
                            Text(
                                text = "Similarity: ${"%.0f".format(score * 100)}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Generating feedback...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    else -> {
                        feedback.feedback?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyLarge
                                )
                        }
                        feedback.semanticScore?.let { score ->
                            Text(
                                text = "Similarity: ${"%.0f".format(score * 100)}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Grammar — 始终显示（SmartGrammarChecker 离线也能跑）
                feedback.grammar?.let {
                    Text(
                        text  = "Grammar: $it",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            when {
                feedback.evaluationOffline || feedback.isEvaluating || feedback.isFeedbackLoading -> {
                    TextButton(onClick = onNext) {
                        Text("Next")
                    }
                }
                feedback.isCorrect == true -> {
                    TextButton(onClick = onNext) { Text("Next") }
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onGiveUp) {
                            Text("Give Up", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                }
            }
        }
    )
}
@Composable
private fun DebugPanel(
    steps: List<SelectionStepLog>,
    currentArmStats: List<ArmDebugInfo>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "🔧 Bandit Debug Panel",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxSize()) {

                item {
                    Text(
                        text = "Current Arm Stats",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        Text("Category", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold)
                        Text("μ",        style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f),   fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        Text("n",        style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        Text("UCB",      style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    }
                }
                if (currentArmStats.isEmpty()) {
                    item {
                        Text(
                            text = "No data yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    items(currentArmStats) { arm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text(arm.category.displayName, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2.5f), maxLines = 1)
                            Text("%.2f".format(arm.mu),    style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f),   textAlign = TextAlign.End)
                            Text(arm.n.toString(),         style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                            Text(
                                text = if (arm.ucbScore == Double.MAX_VALUE) "∞" else "%.2f".format(arm.ucbScore),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Text(
                        text = "Selection History",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                if (steps.isEmpty()) {
                    item {
                        Text(
                            text = "No steps yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    items(steps) { step ->
                        DebugStepItem(step = step)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugStepItem(step: SelectionStepLog) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // 头部：Step 概览
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step ${step.stepIndex + 1}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(52.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "▶ ${step.selectedCategory.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pulls: ${step.totalPulls}  ${if (step.wasUnexplored) "🔍 Exploring" else "🎯 UCB"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        // 展开：各 arm 的详细数据
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 表头
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Category",   style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold)
                    Text("μ",          style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f),   fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Text("n",          style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Text("UCB",        style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                }

                step.armDetails
                    .sortedByDescending { it.ucbScore }
                    .forEach { arm ->
                        val isSelected = arm.isSelected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else
                                        Color.Transparent
                                )
                                .padding(vertical = 1.dp)
                        ) {
                            Text(
                                text = arm.category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(2.5f),
                                maxLines = 1
                            )
                            Text(
                                text = "%.2f".format(arm.mu),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = arm.n.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(0.7f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = if (arm.ucbScore == Double.MAX_VALUE) "∞"
                                else "%.2f".format(arm.ucbScore),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.End,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun ExerciseContent(
    exercise: ExerciseWithReferenceAnswers,
    userAnswer: String,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_medium)),
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.padding_small)
        )
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.padding_medium)),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text  = exercise.exercise.prompt,
                    style = MaterialTheme.typography.titleLarge
                )

                // Tense chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    exercise.answers
                        .mapNotNull { it.tense }
                        .distinct()
                        .forEach { tense ->
                            Surface(
                                shape  = RoundedCornerShape(20.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text  = tense,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp, vertical = 5.dp
                                    )
                                )
                            }
                        }
                }

                OutlinedTextField(
                    value       = userAnswer,
                    onValueChange = onAnswerChange,
                    modifier    = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("Type your answer...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = onSubmit) {
                Text("Submit")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Some responses are generated by AI and may contain mistakes.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SessionCompleteContent(
    totalAnswered: Int,
    correctCount: Int,
    dailyGoal: Int,
    onReturn: () -> Unit,
    onRestart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector        = Icons.Default.CheckCircle,
            contentDescription = "Done",
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text       = "Session Complete!",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text      = "You finished all $dailyGoal exercises for today.",
            style     = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Score card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text       = "$correctCount / $totalAnswered",
                    style      = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Text(
                    text  = "correct answers",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val accuracy =
                    if (totalAnswered == 0) 0
                    else (correctCount * 100) / totalAnswered

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { accuracy / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )

                Text(
                    text  = "Accuracy: $accuracy%",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Primary CTA
        Button(
            onClick  = onReturn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Home")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary: do another round
        OutlinedButton(
            onClick  = onRestart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Practice More")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ExerciseScreenPreview() {
    val mockExercise = ExerciseWithReferenceAnswers(
        exercise = Exercise(
            promptId = 1,
            prompt = "Describe what you did yesterday."
        ),
        answers = listOf(
            ReferenceAnswer(
                promptId = 1, referId = 1,
                reference = "I went shopping with my friends.",
                tense = "Past Simple"
            ),
            ReferenceAnswer(
                promptId = 1, referId = 2,
                reference = "Yesterday I watched a movie at home.",
                tense = "Past Simple"
            )
        )
    )

    val mockState = ExerciseUiState(
        exerciseQueue   = listOf(mockExercise),
        currentIndex    = 0,
        currentExercise = mockExercise,
        userAnswer      = "I watched TV yesterday.",
        totalAnswered   = 3,
        correctCount    = 2,
        dailyGoal       = 10,
        todayProgress   = 3,
        feedbackState   = FeedbackState(
            isCorrect              = true,
            matchedReferenceAnswer = mockExercise.answers[1],
            feedback               = "Your answer correctly uses the past tense.",
            semanticScore          = 0.87,
            grammar                = "Grammar looks good."
        )
    )

    EverydayEnglishTheme {
        ExerciseScreen(
            uiState          = mockState,
            onAnswerChange   = {},
            onSubmit         = {},
            onNext   = {},
            onReturn         = {},
            onRestartSession = {},
            onToggleDebug = {},
            onRetry          = {},
            onGiveUp         = {}
        )
    }
}

@Preview(showBackground = true, name = "Session Complete")
@Composable
fun ExerciseScreenDonePreview() {
    val mockState = ExerciseUiState(
        dailyGoal    = 10,
        totalAnswered = 10,
        correctCount  = 8,
        isSessionDone = true
    )
    EverydayEnglishTheme {
        ExerciseScreen(
            uiState          = mockState,
            onAnswerChange   = {},
            onSubmit         = {},
            onNext   = {},
            onReturn         = {},
            onRestartSession = {},
            onToggleDebug = {},
            onRetry          = {},
            onGiveUp         = {}
        )
    }
}



