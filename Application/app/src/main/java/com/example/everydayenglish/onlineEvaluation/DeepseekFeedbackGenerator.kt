package com.example.everydayenglish.onlineEvaluation

import com.example.everydayenglish.data.entity.EvaluationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class DeepSeekFeedbackGenerator(
    private val apiKey: String
) : FeedbackGenerator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private val SYSTEM_PROMPT = """
    You are an English teacher evaluating a student's answer.
    You will receive:
    - The student's answer
    - One or more reference answers
    - A grammar check result
    - A semantic similarity score (0.0–1.0), or "unavailable" if the scoring service is offline

    First, decide: if the student's answer is mostly correct in meaning with only minor grammar issues, judge it as correct. If the meaning is far from the reference or there are serious grammar errors, judge it as incorrect.

    Then write 1–2 sentences of feedback to guide the student toward better grammar and closer meaning.
    You may reference specific words or phrases from the reference answer as hints to guide the student, but do not reproduce the full reference answer verbatim.
    Use markdown formatting in your feedback, so that important points can stand out.
    
    Respond ONLY with valid JSON in this exact format:
    {"isCorrect": true, "feedback": "..."}
""".trimIndent()

    override suspend fun generate(
        userAnswer: String,
        referenceAnswers: List<String>,
        grammarSummary: String?,
        semanticScore: Double?
    ): EvaluationResult = withContext(Dispatchers.IO) {
        callDeepSeekApi(userAnswer, referenceAnswers, grammarSummary, semanticScore)
    }

    private fun callDeepSeekApi(
        userAnswer       : String,
        referenceAnswers : List<String>,
        grammarSummary   : String?,
        semanticScore    : Double?
    ): EvaluationResult {
        val refBlock = referenceAnswers
            .mapIndexed { i, ref -> "${i + 1}. $ref" }
            .joinToString("\n")

        val meaningLevel = when {
            semanticScore == null -> "Semantic similarity score is unavailable; evaluate based on grammar and reference answers only."
            semanticScore >= 0.90 -> "The meaning is essentially correct."
            semanticScore >= 0.75 -> "The meaning is mostly correct but slightly off."
            semanticScore >= 0.50 -> "The meaning is partially correct."
            else                  -> "The meaning is quite different from the expected answer."
        }

        val userContent = """
        Student's answer: "$userAnswer"

        Reference answer(s):
        $refBlock

        Grammar check: ${grammarSummary ?: "Not available."}
        Meaning assessment: $meaningLevel

        Please provide brief, encouraging teaching feedback.
    """.trimIndent()

        val body = JSONObject().apply {
            put("model", "deepseek-chat")
            put("max_tokens", 120)
            put("temperature", 0.3)
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

        val content = JSONObject(responseText)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()

        val json = JSONObject(content)
        return EvaluationResult(
            isCorrect = json.getBoolean("isCorrect"),
            feedback  = json.getString("feedback")
        )
    }
}