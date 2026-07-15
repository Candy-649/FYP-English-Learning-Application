package com.example.everydayenglish.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.everydayenglish.data.entity.GrammarError

/**
 * Renders one grammar error as a card:
 *   - type label (e.g. "Tense error")
 *   - annotated sentence: strikethrough + correction, or omission insertion marker
 *   - rule hint
 *
 * Used by both FeedbackDialog (ExerciseScreen) and RedoPanel (HistoryScreen).
 */
@Composable
fun GrammarErrorCard(userAnswer: String, error: GrammarError) {
    val typeLabel = when (error.type) {
        "TENSE"      -> "Tense error"
        "MORPHOLOGY" -> "Word form"
        "SPELLING"   -> "Spelling"
        "LEXICAL"    -> "Word choice"
        "ADDITION"   -> "Unnecessary word"
        "WORD_ORDER" -> "Word order"
        "OMISSION"   -> "Missing word"
        else         -> error.type
    }

    Card(
        shape  = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier            = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text       = typeLabel,
                style      = MaterialTheme.typography.labelSmall,
                color      = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )

            when (error.type) {
                "OMISSION" -> OmissionAnnotation(userAnswer, error)
                else       -> StrikethroughAnnotation(userAnswer, error)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text  = error.ruleHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Shows the user's answer with the erroneous phrase struck through in red,
 * followed by the correct form in primary colour (if any).
 * Used for TENSE, MORPHOLOGY, SPELLING, LEXICAL, ADDITION, WORD_ORDER errors.
 */
@Composable
private fun StrikethroughAnnotation(userAnswer: String, error: GrammarError) {
    val phrase = error.errorPhrase
    val idx    = if (phrase != null) userAnswer.indexOf(phrase) else -1

    if (phrase == null || idx < 0) {
        Text(userAnswer, style = MaterialTheme.typography.bodyMedium)
        return
    }

    val errorColor      = MaterialTheme.colorScheme.error
    val correctionColor = MaterialTheme.colorScheme.primary

    val annotated = buildAnnotatedString {
        if (idx > 0) append(userAnswer.substring(0, idx))

        withStyle(SpanStyle(color = errorColor, textDecoration = TextDecoration.LineThrough)) {
            append(phrase)
        }

        if (error.correction != null) {
            append(" ")
            withStyle(SpanStyle(color = correctionColor, fontWeight = FontWeight.Bold)) {
                append(error.correction)
            }
        }

        val afterIdx = idx + phrase.length
        if (afterIdx < userAnswer.length) append(userAnswer.substring(afterIdx))
    }

    Text(annotated, style = MaterialTheme.typography.bodyMedium)
}

/**
 * Shows the user's answer with an inline [+word] insertion marker in primary colour
 * at the position where the missing word should be inserted.
 * Used for OMISSION errors.
 */
@Composable
private fun OmissionAnnotation(userAnswer: String, error: GrammarError) {
    val after      = error.afterPhrase
    val correction = error.correction

    if (after == null || correction == null) {
        Text(userAnswer, style = MaterialTheme.typography.bodyMedium)
        return
    }

    val idx = userAnswer.indexOf(after)
    if (idx < 0) {
        Text(userAnswer, style = MaterialTheme.typography.bodyMedium)
        return
    }

    val insertionColor = MaterialTheme.colorScheme.primary
    val insertPoint    = idx + after.length

    val annotated = buildAnnotatedString {
        append(userAnswer.substring(0, insertPoint))
        withStyle(SpanStyle(color = insertionColor, fontWeight = FontWeight.Bold)) {
            append(" [+$correction]")
        }
        if (insertPoint < userAnswer.length) append(userAnswer.substring(insertPoint))
    }

    Text(annotated, style = MaterialTheme.typography.bodyMedium)
}
