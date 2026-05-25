package com.example.everydayenglish.onlineEvaluation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DeepSeekSemanticChecker(
    private val apiKey: String
) : SemanticChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    // ── system prompt（固定内容，DeepSeek 自动缓存）──────────────────────────
    private val SYSTEM_PROMPT = """
        You are an English language teacher evaluating student answers.
        Assess the semantic similarity between a student's answer and reference answers.
 
        Rules:
        - Focus on meaning and communication, not exact wording.
        - A correct paraphrase should score high even if worded differently.
        - Grammar errors alone should NOT lower the score significantly.
        - Score 1.0 = same meaning, 0.0 = completely unrelated.
 
        Respond ONLY with valid JSON, no markdown, no extra text:
        {"score": <float 0.0-1.0>, "feedback": "<one encouraging sentence in English>"}
    """.trimIndent()

    override suspend fun evaluate(
        userAnswer: String,
        referenceAnswers: List<String>
    ): SemanticResult = withContext(Dispatchers.IO) {
        runCatching {
            callDeepSeekApi(userAnswer, referenceAnswers)
        }.getOrElse {
            // API 失败时降级，不阻断用户流程
            SemanticResult(
                score     = 0.5,
                feedback  = "Unable to evaluate semantics right now.",
                isCorrect = false
            )
        }
    }

    private fun callDeepSeekApi(
        userAnswer: String,
        referenceAnswers: List<String>
    ): SemanticResult {

        val refBlock = referenceAnswers
            .mapIndexed { i, ref -> "${i + 1}. $ref" }
            .joinToString("\n")

        val userContent = """
            Reference answer(s):
            $refBlock
 
            Student's answer:
            "$userAnswer"
 
            Evaluate and return JSON.
        """.trimIndent()

        // ── OpenAI-compatible 请求体 ─────────────────────────────────────────
        val body = JSONObject().apply {
            put("model", "deepseek-chat")
            put("max_tokens", 150)
            put("temperature", 0.0)   // 评分任务用 0，输出稳定
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                })
            })
        }.toString()

        val request = Request.Builder()
            .url("https://api.deepseek.com/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_TYPE))
            .build()

        val responseText = client.newCall(request).execute().use { resp ->
            check(resp.isSuccessful) {
                "DeepSeek API error ${resp.code}: ${resp.body?.string()}"
            }
            resp.body!!.string()
        }

        return parseResponse(responseText)
    }

    // ── 解析 OpenAI-format 响应 ───────────────────────────────────────────────
    private fun parseResponse(raw: String): SemanticResult {
        val content = JSONObject(raw)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()

        val json     = JSONObject(content)
        val score    = json.getDouble("score").coerceIn(0.0, 1.0)
        val feedback = json.getString("feedback")

        return SemanticResult(
            score     = score,
            feedback  = feedback,
            isCorrect = score >= SemanticResult.CORRECT_THRESHOLD
        )
    }
}