package com.example.everydayenglish.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.everydayenglish.viewmodel.ExerciseUiState
import com.example.everydayenglish.viewmodel.FeedbackState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    uiState: ExerciseUiState,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNextExercise: () -> Unit,  // now calls ViewModel.goToNextExercise()
    onReturn: () -> Unit,
    onRestartSession: () -> Unit // calls ViewModel.restartSession()
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
                            text = uiState.currentIndex.toString(),
                            style = MaterialTheme.typography.labelMedium
                        )

                        // Progress bar: fraction of daily goal completed
                        LinearProgressIndicator(
                            progress = {
                                if (uiState.dailyGoal == 0) 0f
                                else uiState.currentIndex.toFloat() / uiState.dailyGoal.toFloat()
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

                // ── Loading ────────────────────────────────────────────────
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // ── Error ──────────────────────────────────────────────────
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

                // ── Session complete ───────────────────────────────────────
                uiState.isSessionDone -> {
                    SessionCompleteContent(
                        totalAnswered = uiState.totalAnswered,
                        correctCount  = uiState.correctCount,
                        dailyGoal     = uiState.dailyGoal,
                        onReturn      = onReturn,
                        onRestart     = onRestartSession
                    )
                }

                // ── Active exercise ────────────────────────────────────────
                exercise != null -> {
                    ExerciseContent(
                        exercise     = exercise,
                        userAnswer   = uiState.userAnswer,
                        onAnswerChange = onAnswerChange,
                        onSubmit     = onSubmit
                    )
                }
            }
        }
    }

    // ── Feedback dialog (shown after submit) ───────────────────────────────
    uiState.feedbackState?.let { feedback ->
        FeedbackDialog(
            feedback    = feedback,
            onNext      = onNextExercise
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composable
// ─────────────────────────────────────────────────────────────────────────────

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
private fun FeedbackDialog(
    feedback: FeedbackState,
    onNext: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* force user to tap Next */ },
        title = {
            Text(if (feedback.isCorrect) "Correct! 🎉" else "Incorrect")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                feedback.matchedReferenceAnswer?.reference?.let { ref ->
                    Text(
                        text  = "Reference: $ref",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                feedback.feedback?.let { Text(it) }

                feedback.grammar?.let {
                    Text(
                        text  = "Grammar: $it",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                feedback.semanticScore?.let {
                    Text(
                        text  = "Similarity: ${"%.0f".format(it * 100)}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNext) {
                Text("Next")
            }
        }
    )
}

/**
 * Full-screen celebration shown once the user finishes all exercises in
 * today's session.
 */
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

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun ExerciseScreenPreview() {
    val mockExercise = ExerciseWithReferenceAnswers(
        exercise = Exercise(
            id = 1, promptId = 1,
            prompt = "Describe what you did yesterday."
        ),
        answers = listOf(
            ReferenceAnswer(
                answerId = 1, exerciseId = 1, referId = 1,
                reference = "I went shopping with my friends.",
                tense = "Past Simple"
            ),
            ReferenceAnswer(
                answerId = 2, exerciseId = 1, referId = 2,
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
            onNextExercise   = {},
            onReturn         = {},
            onRestartSession = {}
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
            onNextExercise   = {},
            onReturn         = {},
            onRestartSession = {}
        )
    }
}



