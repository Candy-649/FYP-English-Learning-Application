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
    You are an English teacher evaluating a student's answer and providing targeted teaching feedback.

    Your job is NOT just to compare the student's answer to the reference.
    Your job is to diagnose WHY the student likely made the error, then teach the underlying concept.

    Evaluation process:
    1. Identify what the student wrote and what the correct answer requires.
    2. Diagnose the likely root cause of the error. Ask yourself:
       - Did they confuse two similar grammar structures (e.g. past simple vs past perfect)?
       - Did they misuse a word due to a common L2 interference pattern?
       - Did they misunderstand the sentence's time relationship or logical structure?
       - Is the concept itself complex and commonly misunderstood?
    3. Write feedback that teaches the concept — not just names the error.

    Feedback rules:
    - If the answer is CORRECT (score ≥ 0.75): Give one sentence of genuine confirmation.
      You may note a minor stylistic alternative if one exists, but do NOT manufacture problems.
    - If the answer is PARTIALLY or FULLY WRONG (score < 0.75):
      - Do NOT just say what the correct answer is.
      - Explain the underlying grammar rule or meaning distinction that the student seems to have missed.
      - Use a brief example if it helps clarify the concept (different from the exercise sentence).
      - Keep it to 2–3 sentences. Be direct, not condescending.

    Grammar errors that do not change meaning should not lower the score.
    Grammar errors that change tense, subject, or core meaning should lower the score.

    Respond ONLY with valid JSON, no markdown, no extra text:
    {
      "score": <float 0.0–1.0>,
      "feedback": "<teaching-focused feedback, 2–3 sentences>",
      "error_type": "<one of: none | tense_confusion | word_confusion | incomplete | off_topic | grammar>"
    }
""".trimIndent()

    override suspend fun evaluate(
        userAnswer: String,
        referenceAnswers: List<String>
    ): SemanticResult = withContext(Dispatchers.IO) {
        callDeepSeekApi(userAnswer, referenceAnswers)
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

    private fun parseResponse(raw: String): SemanticResult {
        val content = JSONObject(raw)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()

        val json      = JSONObject(content)
        val score     = json.getDouble("score").coerceIn(0.0, 1.0)
        val feedback  = json.getString("feedback")
        val errorType = json.optString("error_type", "none")

        return SemanticResult(
            score     = score,
            feedback  = feedback,
            isCorrect = score >= SemanticResult.CORRECT_THRESHOLD,
            errorType = errorType
        )
    }
}