package com.example.everydayenglish.grammarChecker

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.nio.LongBuffer
import kotlin.math.exp

class OnnxGrammarChecker(
    private val context: Context,
    private val modelFile: String = "grammar_model.onnx",
    private val grammaticalThreshold: Float = 0.75f
) : GrammarChecker, Closeable {

    private var envInstance: OrtEnvironment? = null
    private val env: OrtEnvironment
        get() = envInstance ?: OrtEnvironment.getEnvironment()
            .also { envInstance = it }

    private var sessionInstance: OrtSession? = null
    private val session: OrtSession
        get() = sessionInstance ?: run {
            val modelFile = getOrCopyModelFile()
            env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
                .also { sessionInstance = it }
        }

    private fun getOrCopyModelFile(): java.io.File {
        val dest = java.io.File(context.filesDir, modelFile)
        if (!dest.exists()) {
            context.assets.open(modelFile).use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)   // 流式 copy，不会 OOM
                }
            }
        }
        return dest
    }

    private val tokenizer: BertTokenizer by lazy { BertTokenizer(context) }

    override suspend fun check(text: String): GrammarResult =
        withContext(Dispatchers.Default) {
            runCatching { infer(text) }
                .getOrElse { e ->
                    Log.e("OnnxGrammarChecker", "Inference failed", e)  // ← 加这行
                    GrammarResult(issues = emptyList(), summary = "Grammar check unavailable.")
                }
        }

    private fun infer(text: String): GrammarResult {
        val encoding = tokenizer.encode(text, maxLength = 64)

        val inputIds      = encoding.inputIds.map      { it.toLong() }.toLongArray()
        val attentionMask = encoding.attentionMask.map { it.toLong() }.toLongArray()
        val tokenTypeIds  = encoding.tokenTypeIds.map  { it.toLong() }.toLongArray()

        val shape = longArrayOf(1, 64)

        val inputIdsTensor      = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds),      shape)
        val attentionMaskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape)
        val tokenTypeIdsTensor  = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds),  shape)

        val inputs = mapOf(
            "input_ids"      to inputIdsTensor,
            "attention_mask" to attentionMaskTensor,
            "token_type_ids" to tokenTypeIdsTensor,
        )

        // ✅ 修复③：用 orElseThrow 替代裸 .get()，并用 @Suppress 消除 unchecked cast 警告
        @Suppress("UNCHECKED_CAST")
        val logits = session.run(inputs).use { output ->
            (output["logits"]
                .orElseThrow { IllegalStateException("Missing 'logits' output") }
                .value as Array<FloatArray>)[0]
        }

        // ✅ 修复④：max() 已弃用且返回可空，改用 maxOrNull() ?: 0f
        val maxLogit = logits.maxOrNull() ?: 0f
        val exps     = logits.map { exp((it - maxLogit).toDouble()).toFloat() }
        val sumExps  = exps.sum()
        val probs    = exps.map { it / sumExps }

        val grammaticalProb   = probs[1]
        val ungrammaticalProb = probs[0]

        return buildResult(text, grammaticalProb, ungrammaticalProb)
    }

    private fun buildResult(
        text: String,
        grammaticalProb: Float,
        ungrammaticalProb: Float
    ): GrammarResult {
        if (grammaticalProb >= grammaticalThreshold) {
            return GrammarResult(issues = emptyList(), summary = "Grammar looks good!")
        }

        val severity  = when {
            ungrammaticalProb > 0.85f -> Severity.HIGH
            ungrammaticalProb > 0.60f -> Severity.MEDIUM
            else                      -> Severity.LOW
        }
        val errorHint = detectErrorHint(text)
        val summary   = buildSummary(severity, errorHint, grammaticalProb)

        return GrammarResult(
            issues  = listOf(
                GrammarIssue(summary, errorHint ?: severity.label, 0, text.length, emptyList())
            ),
            summary = summary
        )
    }

    private fun detectErrorHint(text: String): String? {
        val t = text.trim()
        if (Regex("""\b(he|she|it)\s+(go|have|do|come|make|want|need|like|get)\b""",
                RegexOption.IGNORE_CASE).containsMatchIn(t))
            return "Subject-verb agreement"
        if (Regex("""\ba\s+[aeiouAEIOU]""").containsMatchIn(t) ||
            Regex("""\ban\s+[^aeiouAEIOU\s]""").containsMatchIn(t))
            return "Article usage (a/an)"
        if (Regex("""\b(don't|doesn't|didn't|can't|won't)\b.*\b(nothing|nobody|never|no one)\b""",
                RegexOption.IGNORE_CASE).containsMatchIn(t))
            return "Double negation"
        if (Regex("""\b(have|has)\s+(went|came|got|ran|saw|did|made|took)\b""",
                RegexOption.IGNORE_CASE).containsMatchIn(t))
            return "Incorrect past participle"
        return null
    }

    private fun buildSummary(severity: Severity, hint: String?, prob: Float): String {
        val confidence = "%.0f%%".format((1f - prob) * 100)
        val hintStr    = if (hint != null) " Possible issue: $hint." else ""
        return when (severity) {
            Severity.HIGH   -> "Grammar issue detected ($confidence confident).$hintStr"
            Severity.MEDIUM -> "Possible grammar issue.$hintStr"
            Severity.LOW    -> "Minor grammar concern.$hintStr"
        }
    }

    private enum class Severity(val label: String) {
        HIGH("Grammar error"), MEDIUM("Grammar issue"), LOW("Minor concern")
    }

    // ✅ 修复②⑤：override Closeable.close()，用 ?. 安全释放，不再需要 isInitialized
    override fun close() {
        sessionInstance?.close()
        envInstance?.close()
    }
}