package com.example.everydayenglish.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.everydayenglish.data.entity.ExerciseRecord
import com.example.everydayenglish.viewmodel.HistoryItem
import com.example.everydayenglish.viewmodel.HistoryUiState
import com.example.everydayenglish.viewmodel.RedoFeedbackState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState           : HistoryUiState,
    onBackClick       : () -> Unit,
    onCardClick       : (Int) -> Unit,      // promptId
    onRedoClick       : (Int) -> Unit = {}, // promptId，打开重做面板
    onRedoAnswerChange: (String) -> Unit = {},
    onSubmitRedo      : () -> Unit = {},
    onRetryRedo       : () -> Unit = {},
    onCloseRedo       : () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study History") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "No exercises yet.\nStart studying to see your history!",
                        style = MaterialTheme.colorScheme.onSurfaceVariant.let {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = uiState.items,
                        key   = { _, item -> item.promptId }
                    ) { _, item ->
                        HistoryCard(
                            item              = item,
                            isExpanded        = uiState.expandedPromptId == item.promptId,
                            isRedoOpen        = uiState.redoPromptId == item.promptId,
                            redoAnswer        = uiState.redoAnswer,
                            redoFeedback      = uiState.redoFeedback,
                            onClick           = { onCardClick(item.promptId) },
                            onRedoClick       = { onRedoClick(item.promptId) },
                            onRedoAnswerChange = onRedoAnswerChange,
                            onSubmitRedo      = onSubmitRedo,
                            onRetryRedo       = onRetryRedo,
                            onCloseRedo       = onCloseRedo
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
//  Card
// ──────────────────────────────────────────────────────────

@Composable
private fun HistoryCard(
    item              : HistoryItem,
    isExpanded        : Boolean,
    isRedoOpen        : Boolean,
    redoAnswer        : String,
    redoFeedback      : RedoFeedbackState?,
    onClick           : () -> Unit,
    onRedoClick       : () -> Unit,
    onRedoAnswerChange: (String) -> Unit,
    onSubmitRedo      : () -> Unit,
    onRetryRedo       : () -> Unit,
    onCloseRedo       : () -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── 题目文本 ──────────────────────────────────────
            Text(
                text       = item.prompt,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── 时态 chips ────────────────────────────────────
            if (item.tenses.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.tenses.forEach { tense ->
                        SuggestionChip(
                            onClick = {},
                            label   = {
                                Text(
                                    text  = tense,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ── 统计行 ────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // 正确率
                AccuracyBadge(
                    accuracyRate   = item.accuracyRate,
                    evaluatedCount = item.evaluatedCount,
                    totalAttempts  = item.totalAttempts
                )

                // 最近时间
                Text(
                    text  = formatTimestamp(item.latestTimestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // ── 一次都没做对：可以重做 ───────────────────────────
            if (item.canRedo && !isRedoOpen) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick  = onRedoClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector        = Icons.Default.Replay,
                        contentDescription = null,
                        modifier           = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Redo this question")
                }
            }

            // ── 展开：历次记录 ──────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text      = "Attempt history",
                        style     = MaterialTheme.typography.labelMedium,
                        color     = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    item.records.forEachIndexed { index, record ->
                        RecordRow(attemptNumber = index + 1, record = record)
                        if (index < item.records.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color    = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }

                    if (isRedoOpen) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        RedoPanel(
                            answer       = redoAnswer,
                            feedback     = redoFeedback,
                            onAnswerChange = onRedoAnswerChange,
                            onSubmit     = onSubmitRedo,
                            onRetry      = onRetryRedo,
                            onClose      = onCloseRedo
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun RedoPanel(
    answer        : String,
    feedback      : RedoFeedbackState?,
    onAnswerChange: (String) -> Unit,
    onSubmit      : () -> Unit,
    onRetry       : () -> Unit,
    onClose       : () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text       = "Redo",
            style      = MaterialTheme.typography.labelMedium,
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        when {
            feedback == null -> {
                OutlinedTextField(
                    value         = answer,
                    onValueChange = onAnswerChange,
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("Enter your answer") }
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClose) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = onSubmit,
                        enabled = answer.isNotBlank()
                    ) { Text("Submit") }
                }
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
                        text  = "Evaluating...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            feedback.evaluationOffline -> {
                Text(
                    text  = "Evaluation failed (possibly a network issue). This attempt won't count toward your progress — please try again later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClose) { Text("Got it") }
                }
            }

            feedback.isCorrect == true -> {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier           = Modifier.size(16.dp),
                        tint               = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text       = "Correct! Counted toward today's progress",
                        style      = MaterialTheme.typography.labelMedium,
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                feedback.feedback?.takeIf { it.isNotBlank() }?.let {
                    MarkdownText(markdown = it, modifier = Modifier.fillMaxWidth())
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onClose) { Text("Done") }
                }
            }

            else -> {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier           = Modifier.size(16.dp),
                        tint               = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text  = "Still incorrect",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                feedback.feedback?.takeIf { it.isNotBlank() }?.let {
                    MarkdownText(markdown = it, modifier = Modifier.fillMaxWidth())
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClose) { Text("Give Up") }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(onClick = onRetry) { Text("Retry") }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
//  正确率 badge
// ──────────────────────────────────────────────────────────

@Composable
private fun AccuracyBadge(
    accuracyRate  : Double?,
    evaluatedCount: Int,
    totalAttempts : Int
) {
    val pendingCount = totalAttempts - evaluatedCount

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when {
            accuracyRate != null -> {
                val color = when {
                    accuracyRate >= 0.75 -> MaterialTheme.colorScheme.primary
                    accuracyRate >= 0.5  -> MaterialTheme.colorScheme.tertiary
                    else                 -> MaterialTheme.colorScheme.error
                }
                Text(
                    text      = "${"%.0f".format(accuracyRate * 100)}%",
                    style     = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color     = color
                )
                Text(
                    text  = "· $totalAttempts attempt${if (totalAttempts > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            pendingCount == totalAttempts -> {
                // 全部待评估
                Icon(
                    imageVector        = Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    modifier           = Modifier.size(14.dp),
                    tint               = MaterialTheme.colorScheme.outline
                )
                Text(
                    text  = "Pending evaluation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            else -> {
                Text(
                    text  = "$totalAttempts attempt${if (totalAttempts > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
//  单条 record 行
// ──────────────────────────────────────────────────────────

@Composable
private fun RecordRow(
    attemptNumber: Int,
    record       : ExerciseRecord
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

        // 第 N 次 + 结果图标 + 时间
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text  = "#$attemptNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                when {
                    record.evaluationPending -> {
                        Icon(
                            imageVector        = Icons.Default.HourglassEmpty,
                            contentDescription = "Pending",
                            modifier           = Modifier.size(14.dp),
                            tint               = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text  = "Pending",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    record.isCorrect -> {
                        Icon(
                            imageVector        = Icons.Default.CheckCircle,
                            contentDescription = "Correct",
                            modifier           = Modifier.size(14.dp),
                            tint               = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text  = "Correct",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {
                        Icon(
                            imageVector        = Icons.Default.Cancel,
                            contentDescription = "Incorrect",
                            modifier           = Modifier.size(14.dp),
                            tint               = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text  = "Incorrect",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Text(
                text  = formatTimestamp(record.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // 用户作答
        Text(
            text  = record.userAnswer,
            style = MaterialTheme.typography.bodyMedium
        )

        // 语义得分（有的话）
        /*record.semanticScore?.let { score ->
            Text(
                text  = "Similarity: ${"%.0f".format(score * 100)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }*/

        // 语法备注
        /*record.grammar?.takeIf { it.isNotBlank() }?.let { grammar ->
            Text(
                text  = "Grammar: $grammar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }*/
        record.feedback?.takeIf { it.isNotBlank() }?.let { feedback ->
            MarkdownText(
                markdown = feedback,
                modifier = Modifier.fillMaxWidth()
            )
        }
        // ── debug 区域 ────────────────────────────────────────────
        /*HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Text(
            text  = "recordId=${record.recordId}  referId=${record.referId}  pending=${record.evaluationPending}  isCorrect=${record.isCorrect}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            fontFamily = FontFamily.Monospace
        )
        record.feedback?.takeIf { it.isNotBlank() }?.let {
            Text(
                text  = "feedback: $it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontFamily = FontFamily.Monospace
            )
        }*/
    }
}

// ──────────────────────────────────────────────────────────
//  Utils
// ──────────────────────────────────────────────────────────

private fun formatTimestamp(timestamp: Long): String {
    val now   = System.currentTimeMillis()
    val delta = now - timestamp
    return when {
        delta < 60_000L              -> "Just now"
        delta < 3_600_000L           -> "${delta / 60_000}m ago"
        delta < 86_400_000L          -> "${delta / 3_600_000}h ago"
        delta < 7 * 86_400_000L      -> "${delta / 86_400_000}d ago"
        else                         -> SimpleDateFormat("MMM d", Locale.getDefault())
            .format(Date(timestamp))
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordRowPreview() {
    RecordRow(
        attemptNumber = 1,
        record = ExerciseRecord(
            recordId = 1,
            promptId = 1,
            userId = "preview",
            referId = 1,
            userAnswer = "She have been study English for three years.",
            isCorrect = false,
            grammar = "Subject-verb agreement error: 'have' should be 'has'.",
            semanticScore = 0.82,
            feedback = "**Issue:** Subject-verb agreement.\n\n- 'She' is third-person singular, so the verb should be 'has', not 'have'.\n- Correct form: *She has been studying English for three years.*",
            evaluationPending = false
        )
    )
}